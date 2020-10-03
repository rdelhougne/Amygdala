package org.fuzzingtool.core.components;

import com.oracle.truffle.api.source.Source;
import org.fuzzingtool.core.Logger;

import java.util.*;

public class Coverage {
	public final Logger logger;
	private final Map<Integer, Boolean> covered_statements = new HashMap<>();
	private final Map<Integer, Boolean> covered_roots = new HashMap<>();
	// BitSet[0] -> branch taken, BitSet[1] -> branch not taken
	private final Map<Integer, BitSet> covered_branches = new HashMap<>();

	// Snaphots
	private final List<Double> root_coverage = new ArrayList<>();
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

	public void registerRoot(Integer id) {
		covered_roots.putIfAbsent(id, false);
	}

	public void addRootCovered(Integer id) {
		covered_roots.put(id, true);
	}

	public void saveSnapshot() {
		// Cleanup
		covered_roots.remove(0);
		covered_statements.remove(0);
		covered_branches.remove(0);

		// Root Coverage
		int covered = 0;
		int loaded = 0;
		for (Map.Entry<Integer, Boolean> entry: covered_roots.entrySet()) {
			loaded++;
			if (entry.getValue()) {
				covered++;
			}
		}
		root_coverage.add(100.0 * ((double) covered / (double) loaded));

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
		cov_str.append(" # Iteration | % Roots | % Stmts | % Branch |\n");
		cov_str.append("-------------|---------|---------|----------|\n");
		for (int i = 0; i < statement_coverage.size(); i++) {
			String iter_str = String.format("%11d", i + 1);
			String root_str = String.format("%7.1f", root_coverage.get(i));
			String stmt_str = String.format("%7.1f", statement_coverage.get(i));
			String branch_str = String.format("%8.1f", branch_coverage.get(i));
			cov_str.append(" ").append(iter_str).append(" |");
			cov_str.append(" ").append(root_str).append(" |");
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
		if (index < root_coverage.size()) {
			coverage_map.put("root", root_coverage.get(index));
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
