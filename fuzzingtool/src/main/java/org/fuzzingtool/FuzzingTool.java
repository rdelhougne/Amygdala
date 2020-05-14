package org.fuzzingtool;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptorKeys;
import org.graalvm.options.*;

import com.oracle.truffle.api.interop.InteropLibrary;

@Registration(id = FuzzingTool.ID, name = "Fuzzing Tool", version = "1.0-SNAPSHOT", services = FuzzingTool.class)
public final class FuzzingTool extends TruffleInstrument {
    @Option(name = "", help = "Enable Fuzzing (default: false).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> optionFuzzingEnabled = new OptionKey<>(false);
    public static final String ID = "fuzzing-tool";

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new FuzzingToolOptionDescriptors();
    }

    PrintStream outStream;
    static final InteropLibrary INTEROP = LibraryFactory.resolve(InteropLibrary.class).getUncached();

    @Override
    protected void onCreate(final Env env) {
        final OptionValues options = env.getOptions();
        if (optionFuzzingEnabled.getValue(options)) {
            init(env);
            env.registerService(this);
        }
    }

    private void init(final Env env) {
        this.outStream = new PrintStream(env.out());

        SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
        SourceSectionFilter filter = builder.build();
        Instrumenter instrumenter = env.getInstrumenter();
        instrumenter.attachExecutionEventFactory(filter, new CoverageExampleEventFactory(env));
    }

    private class CoverageExampleEventFactory implements ExecutionEventNodeFactory {
        private final Env env;

        CoverageExampleEventFactory(final Env env) {
            this.env = env;
        }

        public ExecutionEventNode create(final EventContext ec) {
            return new ExecutionEventNode() {

                @Override
                public void onReturnValue(VirtualFrame vFrame, Object result) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    outStream.println("-----------------------");
                    Node n = ec.getInstrumentedNode();
                    for (Scope s: env.findLocalScopes(n, vFrame)) {
                        try {
                            outStream.println(s.getName() + ":::" + s.getNode().hashCode());
                        } catch (java.lang.Exception ex) {
                            outStream.println("not good");
                        }
                    }
                }
            };
        }
    }

    @Override
    protected void onDispose(Env env) {
        printResults();
    }

    private synchronized void printResults() {
        outStream.println("==Fuzzing Finished==");
    }

}
