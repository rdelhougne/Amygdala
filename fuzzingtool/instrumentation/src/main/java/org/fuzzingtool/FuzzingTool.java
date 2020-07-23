package org.fuzzingtool;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import org.fuzzingtool.components.Amygdala;
import org.graalvm.options.*;

import java.nio.file.Paths;

@Registration(id = FuzzingTool.ID, name = "Fuzzing Tool", version = "1.0-SNAPSHOT", services = FuzzingTool.class)
public final class FuzzingTool extends TruffleInstrument {
	@Option(name = "", help = "Enable Fuzzing (default: false).", category = OptionCategory.USER, stability =
            OptionStability.STABLE)
	static final OptionKey<Boolean> optionFuzzingEnabled = new OptionKey<>(false);

	@Option(name = "mainLoopLineNumber", help = "Line number of the (generated) main loop", category =
            OptionCategory.USER, stability = OptionStability.STABLE)
	static final OptionKey<Integer> optionLoopLineNum = new OptionKey<>(1);

	@Option(name = "mainLoopIdentString", help = "String to identify the main loop", category = OptionCategory.USER,
            stability = OptionStability.STABLE)
	static final OptionKey<String> optionLoopIdentString = new OptionKey<>("fuzzing_main_loop");

	@Option(name = "errorLineNumber", help = "Line number of the global error catch", category = OptionCategory.USER,
            stability = OptionStability.STABLE)
	static final OptionKey<Integer> optionErrorLineNum = new OptionKey<>(7);

	@Option(name = "errorIdentString", help = "String to identify the global error catch", category =
            OptionCategory.USER, stability = OptionStability.STABLE)
	static final OptionKey<String> optionErrorIdentString = new OptionKey<>("fuzzing_error");

	@Option(name = "optionFile", help = "Path to fuzzing options yaml file", category = OptionCategory.USER,
            stability = OptionStability.STABLE)
	static final OptionKey<String> optionOptionFile = new OptionKey<>("fuzzing.yaml");

	@Option(name = "sourceCodeLineOffset", help = "The number of lines added by the preprocessing step", category =
            OptionCategory.USER, stability = OptionStability.STABLE)
	static final OptionKey<Integer> optionSourceCodeLineOffset = new OptionKey<>(3);

	public static final String ID = "fuzzingtool";

	@Override
	protected OptionDescriptors getOptionDescriptors() {
		return new FuzzingToolOptionDescriptors();
	}

	Logger logger;
	Amygdala amygdala;

	@Override
	protected void onCreate(final Env env) {
		this.logger = new Logger(env.out());
		final OptionValues options = env.getOptions();
		if (optionFuzzingEnabled.getValue(options)) {
			this.amygdala = new Amygdala(this.logger);

			if (!options.hasBeenSet(optionOptionFile)) {
				logger.critical(
						"Option --fuzzingtool.optionFile=<String> has not been set, defaulting to 'fuzzing.yaml'.");
			}
			if (!options.hasBeenSet(optionSourceCodeLineOffset)) {
				logger.critical(
						"Option --fuzzingtool.sourceCodeLineOffset=<Integer> has not been set, defaulting to 3.");
			}
			this.amygdala.loadOptions(optionOptionFile.getValue(options),
                                      optionSourceCodeLineOffset.getValue(options));

			if (!options.hasBeenSet(optionLoopLineNum)) {
				logger.critical("Option --fuzzingtool.mainLoopLineNumber=<Integer> has not been set, defaulting to 1" +
                                        ".");
			}
			this.amygdala.main_loop_line_num = optionLoopLineNum.getValue(options);

			if (!options.hasBeenSet(optionLoopIdentString)) {
				logger.critical(
						"Option --fuzzingtool.mainLoopIdentString=<String> has not been set, defaulting to " +
                                "'fuzzing_main_loop'.");
			}
			this.amygdala.main_loop_identifier_string = optionLoopIdentString.getValue(options);

			if (!options.hasBeenSet(optionErrorLineNum)) {
				logger.critical("Option --fuzzingtool.errorLineNumber=<Integer> has not been set, defaulting to 7.");
			}
			this.amygdala.error_line_num = optionErrorLineNum.getValue(options);

			if (!options.hasBeenSet(optionErrorIdentString)) {
				logger.critical(
						"Option --fuzzingtool.errorIdentString=<String> has not been set, defaulting to " +
                                "'fuzzing_error'.");
			}
			this.amygdala.error_identifier_string = optionErrorIdentString.getValue(options);

			init(env);
			env.registerService(this);
		}
	}

	private void init(final Env env) {
		Instrumenter instrumenter = env.getInstrumenter();

		// What source sections are we interested in?
		SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().build();
		// What generates input data to track?
		SourceSectionFilter inputGeneratingLocations = SourceSectionFilter.newBuilder().build();
		instrumenter.attachExecutionEventFactory(sourceSectionFilter, inputGeneratingLocations,
												 new FuzzingNodeWrapperFactory(env, this.amygdala));
	}

	@Override
	protected void onDispose(Env env) {
		printResults();
	}

	private synchronized void printResults() {
		amygdala.visualizeProgramFlow(Paths.get(".").toAbsolutePath().normalize().toString() + "/program_flow");
		amygdala.printStatistics();
		amygdala.printInstrumentation(false);
		logger.printStatistics();
	}
}
