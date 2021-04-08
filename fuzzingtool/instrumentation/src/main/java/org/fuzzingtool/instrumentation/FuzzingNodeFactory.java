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
