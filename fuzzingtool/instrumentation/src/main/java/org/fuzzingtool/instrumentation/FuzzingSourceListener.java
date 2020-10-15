package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.instrumentation.LoadSourceEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceListener;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.core.components.TimeProbe;

public class FuzzingSourceListener implements LoadSourceListener {
	private final Amygdala amygdala;

	FuzzingSourceListener(Amygdala amy) {
		this.amygdala = amy;
	}

	@Override
	public void onLoad(LoadSourceEvent event) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		amygdala.coverage.registerSource(event.getSource());
		amygdala.probe.switchState(TimeProbe.ProgramState.MANAGE);
	}
}
