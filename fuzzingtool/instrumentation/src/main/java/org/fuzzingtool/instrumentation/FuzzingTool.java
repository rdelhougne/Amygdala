package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceFilter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.Amygdala;
import org.graalvm.options.*;

@Registration(id = FuzzingTool.ID, name = "Fuzzing Tool", version = "1.0-SNAPSHOT", services = FuzzingTool.class)
public final class FuzzingTool extends TruffleInstrument {
	@Option(name = "", help = "Enable Fuzzing (default: false).", category = OptionCategory.USER, stability =
            OptionStability.STABLE)
	static final OptionKey<Boolean> optionFuzzingEnabled = new OptionKey<>(false);

	public static final String ID = "fuzzingtool";

	@Override
	protected OptionDescriptors getOptionDescriptors() {
		return new FuzzingToolOptionDescriptors();
	}

	private Logger logger;
	private Amygdala amygdala;

	@Override
	protected void onCreate(final Env env) {
		final OptionValues options = env.getOptions();
		if (optionFuzzingEnabled.getValue(options)) {
			this.logger = new Logger(env.out());
			this.amygdala = new Amygdala(this.logger);
			init(env);
			env.registerService(this);
		}
	}

	private void init(final Env env) {
		SourceSectionFilter execution_filter = SourceSectionFilter.newBuilder().includeInternal(true).build();
		SourceSectionFilter section_coverage_filter = SourceSectionFilter.newBuilder().includeInternal(false).build();
		SourceFilter source_filter = SourceFilter.newBuilder().includeInternal(false).build();

		Instrumenter instrumenter = env.getInstrumenter();
		instrumenter.attachExecutionEventFactory(execution_filter, execution_filter, new FuzzingNodeFactory(env, this.amygdala));
		instrumenter.attachLoadSourceListener(source_filter, new FuzzingSourceListener(amygdala), false);
		instrumenter.attachLoadSourceSectionListener(section_coverage_filter, new FuzzingSourceSectionListener(amygdala), false);
	}

	@Override
	protected void onDispose(Env env) {
		//logger.info("FuzzingTool shutting down.");
	}


	public Amygdala getAmygdala() {
		return this.amygdala;
	}

	public Logger getLogger() {
		return this.logger;
	}
}
