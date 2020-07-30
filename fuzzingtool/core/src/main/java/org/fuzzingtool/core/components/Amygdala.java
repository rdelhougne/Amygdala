package org.fuzzingtool.core.components;

import com.microsoft.z3.Context;
import com.microsoft.z3.Version;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.tactics.DepthSearchTactic;
import org.fuzzingtool.core.tactics.FuzzingTactic;
import org.fuzzingtool.core.visualization.BranchingVisualizer;
import org.graalvm.collections.Pair;

import java.util.*;

/**
 * Central class for managing execution flow events and fuzzing attempts
 * The class is designed as a state-machine. Methods
 * like {@link #branching_event(Integer, BranchingNodeAttribute, Integer, Boolean, String)} modify said machine.
 */
public class Amygdala {
	public final Tracer tracer;
	public final Logger logger;
	public final BranchingNode branchingRootNode;
	public final Context z3_ctx;
	public Queue<Pair<Integer, Boolean>> next_program_path = new LinkedList<>();
	public HashMap<VariableIdentifier, Object> variable_values;
	public final HashMap<Integer, VariableIdentifier> variable_line_to_identifier;
	public final HashMap<VariableIdentifier, String> variable_names;
	public boolean fuzzing_finished = false;
	private BranchingNode currentBranch;
	private FuzzingTactic tactic;
	private int fuzzing_iterations = 0;
	private int max_iterations = 1024;

	//Debugging
	public HashMap<String, BitSet> node_type_instrumented = new HashMap<>();

	public Amygdala(Logger lgr) {
		this.tracer = new Tracer(lgr);
		this.logger = lgr;
		this.variable_values = new HashMap<>();
		this.variable_names = new HashMap<>();
		this.variable_line_to_identifier = new HashMap<>();

		com.microsoft.z3.Global.ToggleWarningMessages(true);
		com.microsoft.z3.Global.setParameter("smt.string_solver", "z3str3");
		com.microsoft.z3.Global.setParameter("timeout", "1000");
		logger.info("Using Z3 " + Version.getString() + " © Copyright 2006-2016 Microsoft Corp.");

		HashMap<String, String> cfg = new HashMap<>();
		cfg.put("model", "true");
		z3_ctx = new Context(cfg);

		branchingRootNode = new BranchingNode();
		currentBranch = branchingRootNode;
	}

	/**
	 * This method should be called when a branching-event happens in the program execution.
	 * Branching events are all events where a boolean expression is evaluated to make a decision about
	 * the program flow. This includes if- while- for- and switch instructions.
	 * This method modifies the state machine.
	 *
	 * @param branching_node_hash   Hash-code of the Truffle node
	 * @param bt                    Kind of the statement (BRANCH or LOOP)
	 * @param predicate_interim_key Key to the boolean expression
	 * @param taken                 Result of the evaluated expression
	 * @param vis_predicate_string  String representation of the expression for visualization
	 */
	public void branching_event(Integer branching_node_hash, BranchingNodeAttribute bt, Integer predicate_interim_key,
								Boolean taken, String vis_predicate_string) {
		if (next_program_path.peek() != null) {
			Pair<Integer, Boolean> expected_behavior = next_program_path.poll();
			if (!expected_behavior.getLeft().equals(branching_node_hash) || !expected_behavior.getRight().equals(taken)) {
				logger.info("Diverging program path detected.");
				currentBranch.setDiverging();
			}
		}
		if (currentBranch.getBranchingNodeAttribute() != BranchingNodeAttribute.BRANCH &&
				currentBranch.getBranchingNodeAttribute() != BranchingNodeAttribute.LOOP) {
			currentBranch.setProperties(tracer.getIntermediate(predicate_interim_key), branching_node_hash, bt);
			currentBranch.setSourceCodeExpression(vis_predicate_string);
			currentBranch.initializeChildren();
		}
		currentBranch = currentBranch.getChildBranch(taken);
	}

	/**
	 * A call to this method indicates that the fuzzed
	 * program has been terminated under normal circumstances.
	 */
	public void terminate_event() {
		currentBranch.setBranchingNodeAttribute(BranchingNodeAttribute.TERMINATE);
		currentBranch = branchingRootNode;
		this.fuzzing_iterations += 1;
	}

	/**
	 * A call to this method indicates an error while executing the program.
	 * The function suppresses the next terminate-event.
	 */
	public void error_event() {
		currentBranch.setBranchingNodeAttribute(BranchingNodeAttribute.ERROR);
		currentBranch = branchingRootNode;
		this.fuzzing_iterations += 1;
	}

	/**
	 * This function returns the number of fuzzing iterations.
	 *
	 * @return An int, specifying the number of completed runs, effectively the count of terminate() signals.
	 */
	public int getIterations() {
		return this.fuzzing_iterations;
	}

	/**
	 * This Method uses a specified tactic to find the next path in the program flow.
	 * If the tactic cannot find another path, the global fuzzing-loop has to be terminated.
	 *
	 * @return The Boolean value for the wrapper
	 */
	public Boolean calculateNextPath() {
		if (fuzzing_iterations < max_iterations) {
			boolean res = this.tactic.calculate();
			if (res) {
				variable_values = this.tactic.getNextValues();
				next_program_path = this.tactic.getNextPath();
				return true;
			} else {
				fuzzing_finished = true;
				return false;
			}
		} else {
			logger.info("Max iterations reached (" + max_iterations + ")");
			return false;
		}
	}

	/**
	 * The method returns the next fuzzing-input value for a given identifier.
	 *
	 * @param var_id The variable identifier
	 * @return An object which contains the next input value
	 */
	public Object getNextInputValue(VariableIdentifier var_id) {
		if (variable_values.containsKey(var_id)) {
			Object next_input = variable_values.get(var_id);
			if (var_id.getVariableType() == ExpressionType.STRING) {
				logger.info("Next input value for variable " + variable_names.get(var_id) + ": \"" + next_input + "\" [STRING]");
			} else {
				logger.info("Next input value for variable " + variable_names.get(var_id) + ": " + next_input + " [" + var_id.getVariableType().name() + "]");
			}
			return next_input;
		} else {
			logger.info("No new value for variable: " + variable_names.get(var_id));
			switch (var_id.getVariableType()) {
				case BOOLEAN:
					return true;
				case STRING:
					return "abc";
				case BIGINT:
				case NUMBER_INTEGER:
					return 1;
				case NUMBER_REAL:
					return 1.5;
				default:
					logger.critical("Variable " + variable_names.get(var_id) + " has not allowed type '" +
											var_id.getVariableType().toString() + "'.");
					return null;
			}
		}
	}

	/**
	 * Visualize and save the complete program-flow tree to a file.
	 *
	 * @param path Filepath of the .svg file
	 */
	public void visualizeProgramFlow(String path) {
		BranchingVisualizer bv = new BranchingVisualizer(branchingRootNode, this.logger);
		bv.save_image(path);
	}

	/**
	 * Load fuzzing options from YAML file.
	 *
	 * @param map Java Object containing the options
	 */
	@SuppressWarnings("unchecked")
	public void loadOptions(Map<String, Object> map) {
		if (map.containsKey("variables") && map.get("variables") instanceof List) {
			loadVariables((List<Map<String, Object>>) map.get("variables"));
		} else {
			logger.warning("No variable configuration found.");
		}

		if (map.containsKey("fuzzing_parameters") && map.get("fuzzing_parameters") instanceof Map) {
			loadFuzzingParameters((Map<String, Object>) map.get("fuzzing_parameters"));
		} else {
			logger.warning("No general configuration found.");
		}
	}

	/**
	 * This Method initializes all given variables and sample-inputs.
	 *
	 * @param variable_list YAML-Map of te variables
	 */
	private void loadVariables(List<Map<String, Object>> variable_list) {
		for (Map<String, Object> var_declaration: variable_list) {
			Integer line_num = (Integer) var_declaration.get("line_num");
			String var_type = (String) var_declaration.get("type");

			ExpressionType var_type_enum;
			switch (var_type) {
				case "BIGINT":
				case "INTEGER":
					var_type_enum = ExpressionType.NUMBER_INTEGER;
					break;
				case "REAL":
					var_type_enum = ExpressionType.NUMBER_REAL;
					break;
				case "BOOLEAN":
					var_type_enum = ExpressionType.BOOLEAN;
					break;
				case "STRING":
					var_type_enum = ExpressionType.STRING;
					break;
				default:
					logger.warning("Unknown variable type '" + var_type + "' for variable in line " + line_num);
					continue;
			}

			String var_gid = tracer.getNewGID();
			VariableIdentifier new_identifier = new VariableIdentifier(var_type_enum, var_gid);
			variable_line_to_identifier.put(line_num, new_identifier);
			StringBuilder builder = new StringBuilder();
			builder.append("\"");
			if (var_declaration.containsKey("name")) {
				builder.append(var_declaration.get("name").toString());
			} else {
				builder.append(var_gid);
			}
			builder.append("\" (line ").append(line_num).append(")");
			variable_names.put(new_identifier, builder.toString());


			switch (var_type_enum) {
				case BOOLEAN:
					variable_values.put(new_identifier, (Boolean) var_declaration.get("sample"));
					break;
				case STRING:
					variable_values.put(new_identifier, (String) var_declaration.get("sample"));
					break;
				case BIGINT:
				case NUMBER_INTEGER:
					variable_values.put(new_identifier, (Integer) var_declaration.get("sample"));
					break;
				case NUMBER_REAL:
					variable_values.put(new_identifier, (Double) var_declaration.get("sample"));
					break;
			}
		}
	}

	/**
	 * Load fuzzing options from the YAML file.
	 *
	 * @param parameters YAML-Map of the options
	 */
	@SuppressWarnings("unchecked")
	private void loadFuzzingParameters(Map<String, Object> parameters) {
		this.max_iterations = (int) parameters.getOrDefault("max_iterations", this.max_iterations);
		logger.info("Option max_iterations set to " + this.max_iterations);

		String tactic_string = (String) parameters.getOrDefault("tactic", "DEPTH_SEARCH");
		switch (tactic_string) {
			case "DEPTH_SEARCH":
				logger.info("Using tactic DEPTH_SEARCH");
				this.tactic = new DepthSearchTactic(this.branchingRootNode, this.z3_ctx, this.logger);
				if (parameters.containsKey("tactic_depth_search")) {
					Map<String, Object> ds_params = (Map<String, Object>) parameters.get("tactic_depth_search");
					if (ds_params.containsKey("max_loop_unrolling")) {
						this.tactic.setOption("max_loop_unrolling", ds_params.get("max_loop_unrolling"));
					}
					if (ds_params.containsKey("max_depth")) {
						this.tactic.setOption("max_depth", ds_params.get("max_depth"));
					}
				}
				break;
			default:
				logger.warning("Unknown tactic '" + tactic_string +
									   "', using tactic DEPTH_SEARCH with default " + "params");
				this.tactic = new DepthSearchTactic(this.branchingRootNode, this.z3_ctx, this.logger);
		}
	}

	/**
	 * Checks if the node at the given line is an input node (called at construction of the node).
	 *
	 * @param line_num Line number in the source code
	 * @return A pair, true if it is input node, false if not. If true, the Variable identifier is also returned
	 */
	public Pair<Boolean, VariableIdentifier> getInputNodeConfiguration(Integer line_num) {
		if (variable_line_to_identifier.containsKey(line_num)) {
			return Pair.create(true, variable_line_to_identifier.get(line_num));
		} else {
			return Pair.create(false, null);
		}
	}

	/**
	 * Print fuzzing statistics to logger.
	 */
	public void printStatistics() {
		logger.log(getStatisticsString());
	}

	/**
	 * Returns a string-representation of the statistics
	 *
	 * @return A string-representation of the statistics
	 */
	public String getStatisticsString() {
		StringBuilder stat_str = new StringBuilder();
		stat_str.append("===FUZZING STATISTICS===\n");
		if (fuzzing_finished) {
			stat_str.append("Finished: yes\n");
		} else {
			stat_str.append("Finished: no\n");
		}
		stat_str.append("Iterations: ").append(this.fuzzing_iterations).append(" of ").append(this.max_iterations).append("\n");
		return stat_str.toString();
	}

	/**
	 * Print instrumentation statistics
	 */
	public void printInstrumentation(boolean only_non_instrumented) {
		logger.log(getInstrumentationString(only_non_instrumented));
	}

	/**
	 * Returns a string-representation of the instrumentation statistics
	 *
	 * @return A string-representation of the statistics
	 */
	public String getInstrumentationString(boolean only_non_instrumented) {
		int NAME_WIDTH = 36;
		StringBuilder stat_str = new StringBuilder();
		stat_str.append("===NODE INSTRUMENTATION INSIGHT===\n");
		stat_str.append(String.format("%-" + NAME_WIDTH + "s", "NODE NAME")).append("E I R\n");
		for (String key : node_type_instrumented.keySet()) {
			BitSet curr_set = node_type_instrumented.get(key);
			boolean not_instrumented = curr_set.cardinality() == 0;

			String node_name = key;
			if (node_name.length() > NAME_WIDTH - 1) {
				node_name = node_name.substring(0, NAME_WIDTH - 4) + "...";
			}
			node_name = String.format("%-" + NAME_WIDTH + "s", node_name);

			if (not_instrumented) {
				stat_str.append("\033[41m").append(node_name).append("\033[0m");
			} else {
				if (!only_non_instrumented) {
					stat_str.append(node_name);
				}
			}

			if (not_instrumented || !only_non_instrumented) {
				if (curr_set.get(0)) {
					stat_str.append("✔ ");
				} else {
					stat_str.append("✗ ");
				}
				if (curr_set.get(1)) {
					stat_str.append("✔ ");
				} else {
					stat_str.append("✗ ");
				}
				if (curr_set.get(2)) {
					stat_str.append("✔");
				} else {
					stat_str.append("✗");
				}
				stat_str.append("\n");
			}
		}
		return stat_str.toString();
	}
}
