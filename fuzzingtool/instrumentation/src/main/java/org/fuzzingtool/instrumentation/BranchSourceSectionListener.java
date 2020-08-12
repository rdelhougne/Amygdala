package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.instrumentation.LoadSourceSectionEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceSectionListener;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.control.IfNode;
import com.oracle.truffle.js.nodes.control.WhileNode;
import org.fuzzingtool.core.components.Amygdala;

public class BranchSourceSectionListener implements LoadSourceSectionListener {
	private final Amygdala amygdala;

	BranchSourceSectionListener(Amygdala amy) {
		this.amygdala = amy;
	}

	@Override
	public void onLoad(LoadSourceSectionEvent event) {
		final SourceSection source_section = event.getSourceSection();
		final Node node = event.getNode();
		if (node instanceof IfNode || node instanceof WhileNode) {
			amygdala.coverage.registerBranch(FuzzingNode.getSourceRelativeIdentifier(source_section, node));
		}
	}
}
