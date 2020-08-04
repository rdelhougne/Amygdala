package org.fuzzingtool.wrapper;

import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.instrumentation.FuzzingTool;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class Fuzzer {
	private Context context;
	private Source source = null;
	private Amygdala amygdala = null;
	private Logger logger = null;
	private boolean initialization_successful;

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
		if (!Engine.create().getLanguages().containsKey("js")) {
			throw new Exception("JS Language context not available.");
		}
		this.context = Context.newBuilder("js").option(FuzzingTool.ID, "true").build();
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
			try {
				context.eval(source);
			} catch (PolyglotException th) {
				run_successful = false;
			}
			if (run_successful) {
				amygdala.terminate_event();
			} else {
				amygdala.error_event();
			}
			one_more = amygdala.calculateNextPath();
		}
	}

	public void print_results() {
		if (amygdala.isBranchingVisEnabled()) {
			amygdala.visualizeProgramFlow(Paths.get(".").toAbsolutePath().normalize().toString() + "/program_flow");
		}
		amygdala.printStatistics();
		amygdala.printInstrumentation(false);
		logger.printStatistics();
	}

	public boolean usable() {
		return this.initialization_successful;
	}
}
