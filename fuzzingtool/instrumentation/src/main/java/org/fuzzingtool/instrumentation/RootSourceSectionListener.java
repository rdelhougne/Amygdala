package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.instrumentation.LoadSourceSectionEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceSectionListener;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.core.components.TimeProbe;

public class RootSourceSectionListener implements LoadSourceSectionListener {
	private final Amygdala amygdala;

	RootSourceSectionListener(Amygdala amy) {
		this.amygdala = amy;
	}

	@Override
	public void onLoad(LoadSourceSectionEvent event) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		final SourceSection source_section = event.getSourceSection();
		final Node node = event.getNode();
		amygdala.coverage.registerRoot(FuzzingNode.getSourceRelativeIdentifier(source_section, node));
		amygdala.probe.switchState(TimeProbe.ProgramState.MANAGE);
	}
}
