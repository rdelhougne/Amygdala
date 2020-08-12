package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.instrumentation.*;
import org.fuzzingtool.core.components.Amygdala;

class FuzzingNodeFactory implements ExecutionEventNodeFactory {
	private final TruffleInstrument.Env env;
	private final Amygdala amygdala;

	FuzzingNodeFactory(final TruffleInstrument.Env env, Amygdala amy) {
		this.env = env;
		this.amygdala = amy;
	}

	@Override
	public ExecutionEventNode create(final EventContext ec) {
		return new FuzzingNode(this.env, this.amygdala, ec);
	}
}