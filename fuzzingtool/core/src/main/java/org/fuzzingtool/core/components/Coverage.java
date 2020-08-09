package org.fuzzingtool.core.components;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.fuzzingtool.core.Logger;

import java.util.*;

public class Coverage {
	public final Logger logger;
	private final MultiValuedMap<Source, SourceSection> loaded_sections = new HashSetValuedHashMap<>();
	private final MultiValuedMap<Source, SourceSection> covered_sections = new HashSetValuedHashMap<>();
	private final Map<Integer, Boolean> covered_statements = new HashMap<>();
	// BitSet[0] -> branch taken, BitSet[1] -> branch not taken
	private final Map<Integer, BitSet> covered_branches = new HashMap<>();

	// Snaphots
	private final List<Double> line_coverage = new ArrayList<>();
	private final List<Double> statement_coverage = new ArrayList<>();
	private final List<Double> branch_coverage = new ArrayList<>();

	public Coverage(Logger lgr) {
		this.logger = lgr;
	}

	public void registerSource(Source src) {
		return;
	}

	public void registerStatement(Integer id) {
		covered_statements.putIfAbsent(id, false);
	}

	public void addStatementCovered(Integer id) {
		covered_statements.put(id, true);
	}

	public void registerBranch(Integer id) {
		covered_branches.putIfAbsent(id, new BitSet(2));
	}

	public void addBranchTaken(Integer id, Boolean taken) {
		if (taken) {
			covered_branches.get(id).set(0);
		} else {
			covered_branches.get(id).set(1);
		}
	}

	public void registerSourceSection(SourceSection src_sec) {
		loaded_sections.put(src_sec.getSource(), src_sec);
	}

	public void addSourceSectionCovered(SourceSection src_sec) {
		covered_sections.put(src_sec.getSource(), src_sec);
	}

	public void saveSnapshot() {
		// Line Coverage
		MultiSet<Source> sources = loaded_sections.keys();
		int covered = 0;
		int loaded = 0;
		for (Source source: sources) {
			Set<Integer> covered_line_numbers = coveredLineNumbers(source);
			Set<Integer> loaded_line_numbers = loadedLineNumbers(source);
			covered += covered_line_numbers.size();
			loaded += loaded_line_numbers.size();
		}
		line_coverage.add(100.0 * ((double) covered / (double) loaded));

		// Statement Coverage
		covered = 0;
		loaded = 0;
		for (Map.Entry<Integer, Boolean> entry: covered_statements.entrySet()) {
			loaded++;
			if (entry.getValue()) {
				covered++;
			}
		}
		statement_coverage.add(100.0 * ((double) covered / (double) loaded));

		// Branch Coverage
		covered = 0;
		loaded = 0;
		for (Map.Entry<Integer, BitSet> entry: covered_branches.entrySet()) {
			loaded += 2;
			covered += entry.getValue().cardinality();
		}
		branch_coverage.add(100.0 * ((double) covered / (double) loaded));
	}

	private Set<SourceSection> nonCoveredSections(Source source) {
		final Set<SourceSection> non_covered = new HashSet<>();
		non_covered.addAll(loaded_sections.get(source));
		non_covered.removeAll(covered_sections.get(source));
		return non_covered;
	}

	private Set<Integer> nonCoveredLineNumbers(Source source) {
		final Set<Integer> lines_not_covered = new HashSet<>();
		for (SourceSection ss : nonCoveredSections(source)) {
			for (int i = ss.getStartLine(); i <= ss.getEndLine(); i++) {
				lines_not_covered.add(i);
			}
		}
		return lines_not_covered;
	}

	private Set<Integer> coveredLineNumbers(Source source) {
		return getLines(covered_sections.get(source));
	}

	private Set<Integer> loadedLineNumbers(Source source) {
		return getLines(loaded_sections.get(source));
	}

	private Set<Integer> getLines(Collection<SourceSection> sections) {
		final Set<Integer> lines = new HashSet<>();
		for (SourceSection ss : sections) {
			for (int i = ss.getStartLine(); i <= ss.getEndLine(); i++) {
				lines.add(i);
			}
		}
		return lines;
	}

	/**
	 * Print coverage information.
	 */
	public void printCoverage() {
		logger.log(getCoverageString());
	}

	/**
	 * Returns a string-representation of the coverage statistics.
	 *
	 * @return A string-representation of the statistics
	 */
	public String getCoverageString() {
		StringBuilder cov_str = new StringBuilder();
		cov_str.append("===COVERAGE INFORMATION===\n");
		cov_str.append("-------------|---------|---------|----------|\n");
		cov_str.append(" # Iteration | % Lines | % Stmts | % Branch |\n");
		cov_str.append("-------------|---------|---------|----------|\n");
		for (int i = 0; i < line_coverage.size(); i++) {
			String iter_str = String.format("%11d", i + 1);
			String line_str = String.format("%7.1f", line_coverage.get(i));
			String stmt_str = String.format("%7.1f", statement_coverage.get(i));
			String branch_str = String.format("%8.1f", branch_coverage.get(i));
			cov_str.append(" ").append(iter_str).append(" |");
			cov_str.append(" ").append(line_str).append(" |");
			cov_str.append(" ").append(stmt_str).append(" |");
			cov_str.append(" ").append(branch_str).append(" |\n");
		}
		cov_str.append("-------------|---------|---------|----------|\n");
		return cov_str.toString();
	}


	/**
	 * Returns an object representing a coverage snapshot.
	 *
	 * @param index Number of the iteration
	 * @return A map representing coverage
	 */
	public Map<String, Object> getCoverageObject(int index) {
		Map<String, Object> coverage_map = new HashMap<>();
		if (index < line_coverage.size()) {
			coverage_map.put("line", line_coverage.get(index));
		}
		if (index < statement_coverage.size()) {
			coverage_map.put("statement", statement_coverage.get(index));
		}
		if (index < branch_coverage.size()) {
			coverage_map.put("branch", branch_coverage.get(index));
		}
		return coverage_map;
	}
}
