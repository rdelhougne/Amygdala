package org.fuzzingtool;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.fuzzingtool.components.Amygdala;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;
import org.fuzzingtool.symbolic.arithmetic.Addition;
import org.fuzzingtool.symbolic.arithmetic.Multiplication;
import org.fuzzingtool.symbolic.arithmetic.Subtraction;
import org.fuzzingtool.symbolic.basic.ConstantInt;
import org.fuzzingtool.symbolic.basic.ConstantString;
import org.fuzzingtool.symbolic.basic.ConstantVoid;
import org.fuzzingtool.symbolic.basic.SymbolicName;
import org.fuzzingtool.symbolic.logical.And;
import org.fuzzingtool.symbolic.logical.Equal;
import org.fuzzingtool.symbolic.logical.LessThan;
import org.fuzzingtool.symbolic.logical.Not;
import org.fuzzingtool.visualization.ASTVisualizer;
import org.graalvm.collections.Pair;
import org.graalvm.options.*;

import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

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
    Amygdala amygdala;

    @Override
    protected void onCreate(final Env env) {
        final OptionValues options = env.getOptions();
        if (optionFuzzingEnabled.getValue(options)) {
            init(env);
            init_constraints();
            env.registerService(this);
        }
    }

    private void init(final Env env) {
        this.outStream = new PrintStream(env.out());
        this.amygdala = new Amygdala();
        Instrumenter instrumenter = env.getInstrumenter();

        // What source sections are we interested in?
        SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().build();
        // What generates input data to track?
        SourceSectionFilter inputGeneratingLocations = SourceSectionFilter.newBuilder().build();
        instrumenter.attachExecutionEventFactory(sourceSectionFilter, inputGeneratingLocations,  new FuzzingNodeWrapperFactory(env, this.amygdala, this.outStream));
    }

    //Limits (for testing)
    ArrayList<String> constraints_scopes = new ArrayList<>();

    // Nur für testzwecke!
    public void init_constraints() {
        constraints_scopes.add("factorial");
    }

    @Override
    protected void onDispose(Env env) {
        printResults();
    }

    private synchronized void printResults() {
        outStream.println("==Fuzzing Finished==");
        outStream.println("Human Readable Expressions:");
        outStream.println(amygdala.lastRunToHumanReadableExpr());

        outStream.println("SMT2 Expression Format:");
        outStream.println(amygdala.lastRunToSMTExpr());
    }

}
