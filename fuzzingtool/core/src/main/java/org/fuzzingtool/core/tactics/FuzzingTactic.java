package org.fuzzingtool.core.tactics;

import org.fuzzingtool.core.components.VariableIdentifier;
import org.graalvm.collections.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public abstract class FuzzingTactic {
	// Results
	protected boolean has_next_path = false;
	protected final HashMap<VariableIdentifier, Object> next_values = new HashMap<>();
	protected Queue<Pair<Integer, Boolean>> next_path = new LinkedList<>();


	/**
	 * Set an option value.
	 *
	 * @param option_name name of the option
	 * @param value new option value
	 */
	public abstract void setOption(String option_name, Object value);

	/**
	 * Returns tactic options.
	 *
	 * @param option_name name of the option
	 * @return value for given option
	 */
	public abstract Object getOption(String option_name);

	public abstract String getTactic();

	/**
	 * Calculates the next program path.
	 *
	 * @return true, if a path is found, false otherwise
	 */
	public abstract boolean calculate();

	/**
	 * Check if a path is found and the parameters exist.
	 *
	 * @return true, if a unexplored path is found, false otherwise
	 */
	public boolean hasNextPath() {
		return has_next_path;
	}

	/**
	 * Returns the new variable-value-map.
	 *
	 * @return values for the variables, empty if {@link #hasNextPath()} returns false
	 */
	public HashMap<VariableIdentifier, Object> getNextValues() {
		return next_values;
	}

	/**
	 * Returns the new program path as a queue.
	 *
	 * @return new program path, empty if {@link #hasNextPath()} returns false
	 */
	public Queue<Pair<Integer, Boolean>> getNextPath() {
		return next_path;
	}
}
