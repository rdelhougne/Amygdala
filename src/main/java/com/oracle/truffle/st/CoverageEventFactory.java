package com.oracle.truffle.st;

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;

final class CoverageEventFactory implements ExecutionEventNodeFactory {

    private FuzzingTool fuzzer;

    CoverageEventFactory(FuzzingTool fuzzer) {
        this.fuzzer = fuzzer;
    }

    public ExecutionEventNode create(final EventContext ec) {
        return new CoverageNode(fuzzer, ec.getInstrumentedSourceSection());
    }
}
