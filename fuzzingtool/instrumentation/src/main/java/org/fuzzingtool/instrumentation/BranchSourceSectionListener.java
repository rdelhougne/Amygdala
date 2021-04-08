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

import com.oracle.truffle.api.instrumentation.LoadSourceSectionEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceSectionListener;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.control.IfNode;
import com.oracle.truffle.js.nodes.control.WhileNode;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.core.components.TimeProbe;

public class BranchSourceSectionListener implements LoadSourceSectionListener {
	private final Amygdala amygdala;

	BranchSourceSectionListener(Amygdala amy) {
		this.amygdala = amy;
	}

	@Override
	public void onLoad(LoadSourceSectionEvent event) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		final SourceSection source_section = event.getSourceSection();
		final Node node = event.getNode();
		if (node instanceof IfNode || node instanceof WhileNode) {
			amygdala.coverage.registerBranch(FuzzingNode.getSourceRelativeIdentifier(source_section, node));
		}
		amygdala.probe.switchState(TimeProbe.ProgramState.MANAGE);
	}
}
