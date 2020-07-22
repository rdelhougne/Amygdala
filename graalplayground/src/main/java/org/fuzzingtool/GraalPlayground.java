package org.fuzzingtool;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.LibraryFactory;
import org.graalvm.options.*;

@Registration(id = GraalPlayground.ID, name = "Graal Playground", version = "1.0-SNAPSHOT", services = GraalPlayground.class)
public final class GraalPlayground extends TruffleInstrument {
	public static final String ID = "graalplayground";

	@Option(name = "", help = "Enable Playground (default: false).", category = OptionCategory.USER, stability = OptionStability.STABLE)
	static final OptionKey<Boolean> optionPlaygroundEnabled = new OptionKey<>(false);

	@Override
	protected OptionDescriptors getOptionDescriptors() {
		return new GraalPlaygroundOptionDescriptors();
	}

	Logger logger;
	static final InteropLibrary INTEROP = LibraryFactory.resolve(InteropLibrary.class).getUncached();

	@Override
	protected void onCreate(final Env env) {
		this.logger = new Logger(env.out());
		final OptionValues options = env.getOptions();
		if (optionPlaygroundEnabled.getValue(options)) {
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
												 new FuzzingNodeWrapperFactory(env, this.logger));
	}

	@Override
	protected void onDispose(Env env) {
		printResults();
	}

	private synchronized void printResults() {
		logger.printStatistics();
	}
}
