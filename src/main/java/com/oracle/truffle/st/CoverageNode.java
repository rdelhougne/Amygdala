package com.oracle.truffle.st;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.source.SourceSection;

final class CoverageNode extends ExecutionEventNode {

    private final FuzzingTool instrument;
    @CompilerDirectives.CompilationFinal private boolean covered;

    private final SourceSection instrumentedSourceSection;

    CoverageNode(FuzzingTool instrument, SourceSection instrumentedSourceSection) {
        this.instrument = instrument;
        this.instrumentedSourceSection = instrumentedSourceSection;
    }

    @Override
    public void onReturnValue(VirtualFrame vFrame, Object result) {
        if (!covered) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            covered = true;
            instrument.addCovered(instrumentedSourceSection);
        }
    }

}
