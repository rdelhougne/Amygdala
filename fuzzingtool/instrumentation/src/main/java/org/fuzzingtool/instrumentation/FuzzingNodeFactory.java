package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.core.components.TimeProbe;

class FuzzingNodeFactory implements ExecutionEventNodeFactory {
	private final TruffleInstrument.Env env;
	private final Amygdala amygdala;

	FuzzingNodeFactory(final TruffleInstrument.Env env, Amygdala amy) {
		this.env = env;
		this.amygdala = amy;
	}

	@Override
	public ExecutionEventNode create(final EventContext ec) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		FuzzingNode wrapper_node = new FuzzingNode(this.env, this.amygdala, ec);
		amygdala.probe.switchState(TimeProbe.ProgramState.EXECUTION);
		return wrapper_node;
	}
}