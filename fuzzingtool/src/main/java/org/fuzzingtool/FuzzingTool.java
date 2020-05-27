package org.fuzzingtool;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import org.fuzzingtool.components.Amygdala;
import org.graalvm.options.*;

import java.nio.file.Paths;
import java.util.ArrayList;

@Registration(id = FuzzingTool.ID, name = "Fuzzing Tool", version = "1.0-SNAPSHOT", services = FuzzingTool.class)
public final class FuzzingTool extends TruffleInstrument {
    @Option(name = "", help = "Enable Fuzzing (default: false).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> optionFuzzingEnabled = new OptionKey<>(false);
    public static final String ID = "fuzzing-tool";

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new FuzzingToolOptionDescriptors();
    }

    Logger logger;
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
        this.logger = new Logger(env.out());
        this.amygdala = new Amygdala(this.logger);
        Instrumenter instrumenter = env.getInstrumenter();

        // What source sections are we interested in?
        SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().build();
        // What generates input data to track?
        SourceSectionFilter inputGeneratingLocations = SourceSectionFilter.newBuilder().build();
        instrumenter.attachExecutionEventFactory(sourceSectionFilter, inputGeneratingLocations,  new FuzzingNodeWrapperFactory(env, this.amygdala));
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
        amygdala.terminate();
        logger.log("==Fuzzing Finished==");
        logger.log("Human Readable Expressions:");
        logger.log(amygdala.lastRunToHumanReadableExpr());

        logger.log("SMT2 Expression Format:");
        logger.log(amygdala.lastRunToSMTExpr());

        amygdala.visualizeProgramFlow(Paths.get(".").toAbsolutePath().normalize().toString() + "/program_flow");
    }

}
