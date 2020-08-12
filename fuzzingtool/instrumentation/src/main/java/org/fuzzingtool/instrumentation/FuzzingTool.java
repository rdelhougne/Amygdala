package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.*;
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
		final Instrumenter instrumenter = env.getInstrumenter();

		// Variable execution tracing
		final SourceSectionFilter execution_filter = SourceSectionFilter.newBuilder().includeInternal(true).build();
		instrumenter.attachExecutionEventFactory(execution_filter, execution_filter, new FuzzingNodeFactory(env, this.amygdala));

		// Coverage information
		SourceSectionFilter branch_coverage_filter = SourceSectionFilter.newBuilder().includeInternal(false).build(); // Apparently WhileNode is not a statement...
		SourceSectionFilter statement_coverage_filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.StatementTag.class).includeInternal(false).build();
		SourceSectionFilter root_coverage_filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootTag.class).includeInternal(false).build();
		instrumenter.attachLoadSourceSectionListener(branch_coverage_filter, new BranchSourceSectionListener(amygdala), false);
		instrumenter.attachLoadSourceSectionListener(statement_coverage_filter, new StatementSourceSectionListener(amygdala), false);
		instrumenter.attachLoadSourceSectionListener(root_coverage_filter, new RootSourceSectionListener(amygdala), false);

		// Load source listener
		SourceFilter source_filter = SourceFilter.newBuilder().build();
		instrumenter.attachLoadSourceListener(source_filter, new FuzzingSourceListener(amygdala), false);
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
