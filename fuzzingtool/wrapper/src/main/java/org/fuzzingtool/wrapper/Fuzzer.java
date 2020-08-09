package org.fuzzingtool.wrapper;

import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.instrumentation.FuzzingTool;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.snakeyaml.engine.v2.api.*;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;

import java.io.*;
import java.nio.file.Paths;
import java.util.Map;

public class Fuzzer {
	private Engine engine;
	private Context context;
	private Source source = null;
	private Amygdala amygdala = null;
	private Logger logger = null;
	private boolean initialization_successful;

	private static final String TERMINATE_FILE_NAME = "terminate-1bfa427b-a460-4088-b578-e388a6bce94d";

	public Fuzzer(String fuzzing_config) {
		try {
			init(fuzzing_config);
			this.initialization_successful = true;
		} catch (Throwable th) {
			System.out.println("ERROR: Cannot initialize Fuzzer. Reason: " + th.getMessage());
			this.initialization_successful = false;
		}
	}

	private void init(String fuzzing_config) throws Exception {
		this.engine = Engine.newBuilder().allowExperimentalOptions(true).option("engine.Compilation", "false").option(FuzzingTool.ID, "true").build();
		if (!this.engine.getLanguages().containsKey("js")) {
			throw new Exception("JS Language context not available.");
		}
		this.context = Context.newBuilder("js").engine(this.engine).build();
		FuzzingTool fuzzing_instrument = context.getEngine().getInstruments().get(FuzzingTool.ID).lookup(FuzzingTool.class);
		if (fuzzing_instrument == null) {
			throw new Exception("Cannot communicate with Truffle Instrument, perhaps classpath-isolation is enabled.");
		}
		this.amygdala = fuzzing_instrument.getAmygdala();
		this.logger = fuzzing_instrument.getLogger();

		Map<String, Object> configuration = loadConfigurationFile(fuzzing_config);
		this.amygdala.loadOptions(configuration);

		if (configuration.containsKey("program_path") && configuration.get("program_path") instanceof String) {
			String config_file_path_abs = new File(fuzzing_config).getAbsoluteFile().getParent();
			File program_file_path = new File((String) configuration.get("program_path"));
			if (program_file_path.isAbsolute()) {
				this.source = loadSource(program_file_path.getAbsoluteFile());
			} else {
				this.source = loadSource(new File(Paths.get(config_file_path_abs, program_file_path.getPath()).toString()));
			}
		} else {
			logger.critical("No attribute 'program_path' in configuration file.");
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
			throw new Exception("File '" + file_path + "' not found.");
		}
		return map;
	}

	private Source loadSource(File source_file) throws Exception {
		if (source_file.getName().endsWith(".js")) {
			try {
				String language = Source.findLanguage(source_file);
				if (!language.equals("js")) {
					throw new Exception("Source file " + source_file.getName() + " is not a JavaScript source file.");
				}
				return Source.newBuilder(language, source_file).build();
			} catch (IOException e) {
				throw new Exception("Cannot open file " + source_file.getName() + ".");
			}
		} else {
			throw new Exception("Source file " + source_file.getName() + " does not end with '.js'.");
		}
	}

	public void fuzz() {
		boolean one_more = true;
		while (one_more) {
			boolean run_successful = true;
			long eval_start_time = 0;
			long eval_finish_time = 0;
			String error_reason = "UNKNOWN";
			try {
				eval_start_time = System.nanoTime();
				context.eval(source);
				eval_finish_time = System.nanoTime();
			} catch (PolyglotException pe) {
				eval_finish_time = System.nanoTime();
				String message = pe.getMessage();
				if (message.startsWith("org.fuzzingtool.core.components.CustomError$EscalatedException:")) {
					error_reason = message.replace("org.fuzzingtool.core.components.CustomError$EscalatedException: ", "");
				} else {
					error_reason = message;
				}
				run_successful = false;
			}
			long runtime = eval_finish_time - eval_start_time;

			if (run_successful) {
				amygdala.terminate_event(runtime);
			} else {
				amygdala.error_event(error_reason, runtime);
			}
			amygdala.coverage.saveSnapshot();

			// TODO hackyyy...
			File f = new File(TERMINATE_FILE_NAME);
			if(f.exists()) {
				boolean delete_successful = f.delete();
				amygdala.logger.info("User requested shutdown.");
				if (!delete_successful) {
					amygdala.logger.warning("Cannot delete termination indicator file, should be deleted manually.");
				}
				return;
			}

			one_more = amygdala.calculateNextPath();
		}
	}

	public void print_results() {
		logger.info("Printing results\n");
		amygdala.printStatistics();
		amygdala.printInstrumentation();
		amygdala.coverage.printCoverage();
		logger.printStatistics();
	}

	public boolean usable() {
		return this.initialization_successful;
	}

	public void saveResults(String path) {
		if (amygdala.isBranchingVisEnabled()) {
			amygdala.visualizeProgramFlow(Paths.get(".").toAbsolutePath().normalize().toString() + "/program_flow");
		}

		Map<String, Object> map = amygdala.getResults();
		DumpSettings settings =
				DumpSettings.builder().setDefaultFlowStyle(FlowStyle.BLOCK).setDefaultScalarStyle(ScalarStyle.PLAIN)
						.setExplicitStart(true).setExplicitEnd(true).build();
		Dump dump = new Dump(settings);
		try {
			FileWriter file_writer = new FileWriter(path);
			file_writer.write(dump.dumpToString(map));
			file_writer.close();
			logger.info("Results written to " + path);
		} catch (IOException ioe) {
			logger.critical("Cannot write results to " + path + ". Reason: " + ioe.getMessage());
		}
	}
}
