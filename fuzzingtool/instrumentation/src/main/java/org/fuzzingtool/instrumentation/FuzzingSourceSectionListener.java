package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.instrumentation.LoadSourceSectionEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceSectionListener;
import com.oracle.truffle.api.source.SourceSection;
import org.fuzzingtool.core.components.Amygdala;

public class FuzzingSourceSectionListener implements LoadSourceSectionListener {
	private final Amygdala amygdala;

	FuzzingSourceSectionListener(Amygdala amy) {
		this.amygdala = amy;
	}

	@Override
	public void onLoad(LoadSourceSectionEvent event) {
		final SourceSection source_section = event.getSourceSection();
		amygdala.coverage.registerSourceSection(source_section);
	}
}
