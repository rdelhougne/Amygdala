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

package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.Amygdala;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionValues;

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
	}

	public Amygdala getAmygdala() {
		return this.amygdala;
	}

	public Logger getLogger() {
		return this.logger;
	}
}
