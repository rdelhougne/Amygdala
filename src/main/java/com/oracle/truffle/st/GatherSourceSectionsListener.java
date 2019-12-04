package com.oracle.truffle.st;

import com.oracle.truffle.api.instrumentation.LoadSourceSectionEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceSectionListener;
import com.oracle.truffle.api.source.SourceSection;

final class GatherSourceSectionsListener implements LoadSourceSectionListener {

    private final FuzzingTool instrument;

    GatherSourceSectionsListener(FuzzingTool instrument) {
        this.instrument = instrument;
    }

    @Override
    public void onLoad(LoadSourceSectionEvent event) {
        final SourceSection sourceSection = event.getSourceSection();
        instrument.addLoaded(sourceSection);
    }
}
