package com.oracle.truffle.st;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

@Registration(id = FuzzingTool.ID, name = "Fuzzing Tool", version = "0.0.1", services = FuzzingTool.class)
public final class FuzzingTool extends TruffleInstrument {

    @Option(name = "", help = "Enable Simple Coverage (default: false).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);

    @Option(name = "PrintCoverage", help = "Print coverage to stdout on process exit (default: true).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> PRINT_COVERAGE = new OptionKey<>(true);

    public static final String ID = "fuzzing-tool";

    final Map<Source, Coverage> coverageMap = new HashMap<>();

    public synchronized Map<Source, Coverage> getCoverageMap() {
        return Collections.unmodifiableMap(coverageMap);
    }

    @Override
    protected void onCreate(final Env env) {
        final OptionValues options = env.getOptions();
        if (ENABLED.getValue(options)) {
            enable(env);
            env.registerService(this);
        }
    }

    private void enable(final Env env) {
        SourceSectionFilter filter = SourceSectionFilter.newBuilder().tagIs(ExpressionTag.class).includeInternal(false).build();
        Instrumenter instrumenter = env.getInstrumenter();
        instrumenter.attachLoadSourceSectionListener(filter, new GatherSourceSectionsListener(this), true);
        instrumenter.attachExecutionEventFactory(filter, new CoverageEventFactory(this));
    }

    @Override
    protected void onDispose(Env env) {
        if (PRINT_COVERAGE.getValue(env.getOptions())) {
            printResults(env);
        }
    }

    private synchronized void printResults(final Env env) {
        final PrintStream printStream = new PrintStream(env.out());
        for (Source source : coverageMap.keySet()) {
            printResult(printStream, source);
        }
    }

    private void printResult(PrintStream printStream, Source source) {
        String path = source.getPath();
        int lineCount = source.getLineCount();
        Set<Integer> nonCoveredLineNumbers = nonCoveredLineNumbers(source);
        Set<Integer> loadedLineNumbers = coverageMap.get(source).loadedLineNumbers();
        double coveredPercentage = 100 * ((double) loadedLineNumbers.size() - nonCoveredLineNumbers.size()) / lineCount;
        printStream.println("==");
        printStream.println("Coverage of " + path + " is " + String.format("%.2f%%", coveredPercentage));
        for (int i = 1; i <= source.getLineCount(); i++) {
            char covered = getCoverageCharacter(nonCoveredLineNumbers, loadedLineNumbers, i);
            printStream.println(String.format("%s %s", covered, source.getCharacters(i)));
        }
    }

    private static char getCoverageCharacter(Set<Integer> nonCoveredLineNumbers, Set<Integer> loadedLineNumbers, int i) {
        if (loadedLineNumbers.contains(i)) {
            return nonCoveredLineNumbers.contains(i) ? '-' : '+';
        } else {
            return ' ';
        }
    }

    public synchronized Set<Integer> nonCoveredLineNumbers(final Source source) {
        return coverageMap.get(source).nonCoveredLineNumbers();
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new FuzzingToolOptionDescriptors();
    }

    synchronized void addLoaded(SourceSection sourceSection) {
        final Coverage coverage = coverageMap.computeIfAbsent(sourceSection.getSource(), s -> new Coverage());
        coverage.addLoaded(sourceSection);
    }

    synchronized void addCovered(SourceSection sourceSection) {
        final Coverage coverage = coverageMap.get(sourceSection.getSource());
        coverage.addCovered(sourceSection);
    }

}
