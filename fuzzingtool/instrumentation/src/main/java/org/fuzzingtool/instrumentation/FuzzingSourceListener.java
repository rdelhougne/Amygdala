package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.instrumentation.LoadSourceEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceListener;
import org.fuzzingtool.core.components.Amygdala;

public class FuzzingSourceListener implements LoadSourceListener {
	private final Amygdala amygdala;

	FuzzingSourceListener(Amygdala amy) {
		this.amygdala = amy;
	}

	@Override
	public void onLoad(LoadSourceEvent event) {
		amygdala.coverage.registerSource(event.getSource());
	}
}
