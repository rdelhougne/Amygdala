/*
 * Copyright 2021 Robert Delhougne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fuzzingtool.wrapper;

import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.core.components.TimeProbe;
import org.fuzzingtool.instrumentation.FuzzingTool;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Fuzzer {
	private Engine engine;
	private Context context;
	private Source source = null;
	private Amygdala amygdala = null;
	private Logger logger = null;
	private final TimeProbe probe;
	private boolean initialization_successful;
	private final Timer timeout_timer;

	private static final String TERMINATE_FILE_NAME = "terminate-1bfa427b-a460-4088-b578-e388a6bce94d";
	private String runtime_complete_output = null;
	private String runtime_fractional_output = null;

	public Fuzzer(String fuzzing_config) {
		this.probe = new TimeProbe(false);
		this.timeout_timer = new Timer();
		try {
			init(fuzzing_config);
			this.initialization_successful = true;
		} catch (Throwable th) {
			System.out.println("ERROR: Cannot initialize Fuzzer. Reason: " + th.getMessage());
			this.initialization_successful = false;
		}
	}

	private void init(String fuzzing_config) throws Exception {
		this.engine = Engine.newBuilder().option(FuzzingTool.ID, "true").build();
		if (!this.engine.getLanguages().containsKey("js")) {
			throw new Exception("JS Language context not available");
		}
		this.context = Context.newBuilder("js").engine(this.engine).build();
		FuzzingTool fuzzing_instrument = context.getEngine().getInstruments().get(FuzzingTool.ID).lookup(FuzzingTool.class);
		if (fuzzing_instrument == null) {
			throw new Exception("Cannot communicate with Truffle Instrument, perhaps classpath-isolation is enabled");
		}
		this.amygdala = fuzzing_instrument.getAmygdala();
		this.logger = fuzzing_instrument.getLogger();
		this.amygdala.setTimeProbe(this.probe);

		Map<String, Object> configuration = loadConfigurationFile(fuzzing_config);
		this.amygdala.loadOptions(configuration, new File(fuzzing_config).getAbsoluteFile().getParent());

		this.source = loadSource(amygdala.getProgramPath());

		if (configuration.containsKey("runtime_complete_output") && configuration.get("runtime_complete_output") instanceof String) {
			this.runtime_complete_output = (String) configuration.get("runtime_complete_output");
		}
		if (configuration.containsKey("runtime_fractional_output") && configuration.get("runtime_fractional_output") instanceof String) {
			this.runtime_fractional_output = (String) configuration.get("runtime_fractional_output");
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadConfigurationFile(String file_path) throws Exception {
		FileInputStream fis;
		Map<String, Object> map;
		try {
			Load load = new Load(LoadSettings.builder().build());
			fis = new FileInputStream(file_path);
			map = (Map<String, Object>) load.loadFromInputStream(fis);
		} catch (FileNotFoundException e) {
			throw new Exception("File '" + file_path + "' not found");
		}
		return map;
	}

	private Source loadSource(String source_path) throws Exception {
		File source_file = new File(source_path);
		if (source_file.getName().endsWith(".js")) {
			try {
				String language = Source.findLanguage(source_file);
				if (!language.equals("js")) {
					throw new Exception("Source file " + source_file.getName() + " is not a JavaScript source file");
				}
				return Source.newBuilder(language, source_file).build();
			} catch (IOException e) {
				throw new Exception("Cannot open file " + source_file.getName());
			}
		} else {
			throw new Exception("Source file " + source_file.getName() + " does not end with '.js'");
		}
	}

	public void fuzz() {
		boolean one_more = true;
		while (one_more) {
			logger.info("Running iteration " + (amygdala.getIteration() + 1));
			boolean run_successful = true;
			String error_reason = "UNKNOWN";
			TimeoutTask task = new TimeoutTask(amygdala);
			this.timeout_timer.schedule(task, amygdala.getTimeoutMillis());
			try {
				probe.switchStateAndStartIteration(TimeProbe.ProgramState.EXECUTION);
				context.eval(source);
				probe.switchStateAndEndIteration(TimeProbe.ProgramState.MANAGE);
			} catch (PolyglotException pe) {
				probe.switchStateAndEndIteration(TimeProbe.ProgramState.MANAGE);
				String message = pe.getMessage();
				if (message.startsWith("org.fuzzingtool.core.components.CustomError$EscalatedException:")) {
					error_reason = message.replace("org.fuzzingtool.core.components.CustomError$EscalatedException: ", "");
				} else if (message.startsWith("SyntaxError")) {
					timeout_timer.cancel();
					logger.critical("Syntax error found, cannot proceed. Message:");
					logger.log(message);
					return;
				} else {
					error_reason = message;
				}
				run_successful = false;
			}
			task.cancel();
			amygdala.setTimeoutReached(false);

			if (run_successful) {
				amygdala.terminateEvent(probe.getIterationDuration());
			} else {
				amygdala.errorEvent(error_reason, probe.getIterationDuration());
			}

			amygdala.snapshot();

			// TODO hackyyy...
			File f = new File(TERMINATE_FILE_NAME);
			if(f.exists()) {
				boolean delete_successful = f.delete();
				amygdala.logger.info("User requested shutdown");
				if (!delete_successful) {
					amygdala.logger.warning("Cannot delete termination indicator file, should be deleted manually");
				}
				timeout_timer.cancel();
				return;
			}

			one_more = amygdala.calculateNextPath();
		}
		timeout_timer.cancel();
	}

	public void printResults() {
		logger.info("Printing results\n");
		amygdala.printStatistics();
		amygdala.printInstrumentation();
		amygdala.coverage.printCoverage();
		logger.printStatistics();
	}

	public boolean usable() {
		return this.initialization_successful;
	}

	public void saveResults() {
		if (amygdala.isBranchingVisEnabled()) {
			amygdala.visualizeProgramFlow("trace_tree_explored.svg");
		}

		Map<String, Object> map = amygdala.getResults();
		DumpSettings settings =
				DumpSettings.builder().setDefaultFlowStyle(FlowStyle.BLOCK).setDefaultScalarStyle(ScalarStyle.PLAIN)
						.setExplicitStart(true).setExplicitEnd(true).build();
		Dump dump = new Dump(settings);
		String result_yaml_path = Paths.get(amygdala.getResultsPath(), "result.yaml").toString();
		try {
			FileWriter file_writer = new FileWriter(result_yaml_path);
			file_writer.write(dump.dumpToString(map));
			file_writer.close();
			logger.info("Results written to '" + result_yaml_path + "'");
		} catch (IOException ioe) {
			logger.critical("Cannot write results to " + result_yaml_path + ". Reason: " + ioe.getMessage());
		}
	}

	public void saveAndPrintRuntimeInformation() {
		if (this.runtime_complete_output != null) {
			String type = "instrumentation";
			List<Long> durations = amygdala.getRuntimes();
			Collection<Object> values = amygdala.getVariableValues().get(0).values();

			if (values.size() == 1) {
				String input_val = String.valueOf(values.iterator().next());
				try {
					FileWriter fw = new FileWriter(this.runtime_complete_output, true);
					BufferedWriter bw = new BufferedWriter(fw);
					for (int i = 0; i < durations.size(); i++) {
						String measure_string = type + "," + input_val + "," + (i + 1) + "," + durations.get(i);
						bw.write(measure_string);
						bw.newLine();
					}
					bw.close();
				} catch (IOException e) {
					logger.critical("Cannot write runtime information to file '" + this.runtime_complete_output + "'");
				}
			} else {
				logger.critical("Cannot obtain locked input value");
			}
		}
		probe.switchState(TimeProbe.ProgramState.STOP);
		if (this.runtime_fractional_output != null) {
			try {
				FileWriter fw = new FileWriter(this.runtime_fractional_output, true);
				BufferedWriter bw = new BufferedWriter(fw);
				String measure_string = probe.getDuration(TimeProbe.ProgramState.MANAGE) + "," +
										probe.getDuration(TimeProbe.ProgramState.EXECUTION) + "," +
										probe.getDuration(TimeProbe.ProgramState.INSTRUMENTATION) + "," +
										probe.getDuration(TimeProbe.ProgramState.SOLVE) + "," +
										probe.getDuration(TimeProbe.ProgramState.TACTIC);
										// vm measurement and linebreak will be added by python script
				bw.write(measure_string);
				bw.close();
			} catch (IOException e) {
				logger.critical("Cannot write runtime information to file '" + this.runtime_fractional_output + "'");
			}
		}
		logger.log(probe.toString());
	}

	class TimeoutTask extends TimerTask {
		private final Amygdala amygdala;

		public TimeoutTask(Amygdala amy) {
			this.amygdala = amy;
		}

		@Override
		public void run() {
			this.amygdala.setTimeoutReached(true);
		}
	}
}
