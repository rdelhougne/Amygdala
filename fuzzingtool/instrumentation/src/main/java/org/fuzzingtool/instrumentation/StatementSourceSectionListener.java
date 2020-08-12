package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.instrumentation.LoadSourceSectionEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceSectionListener;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.fuzzingtool.core.components.Amygdala;

public class StatementSourceSectionListener implements LoadSourceSectionListener {
	private final Amygdala amygdala;

	StatementSourceSectionListener(Amygdala amy) {
		this.amygdala = amy;
	}

	@Override
	public void onLoad(LoadSourceSectionEvent event) {
		final SourceSection source_section = event.getSourceSection();
		final Node node = event.getNode();
		amygdala.coverage.registerStatement(FuzzingNode.getSourceRelativeIdentifier(source_section, node));
	}
}
