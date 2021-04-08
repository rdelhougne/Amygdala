/*
 * Copyright 2021 Robert Delhougne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fuzzingtool.core.components;

import com.microsoft.z3.Context;
import com.microsoft.z3.Version;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.fuzzingtool.core.tactics.DepthSearchTactic;
import org.fuzzingtool.core.tactics.FuzzingTactic;
import org.fuzzingtool.core.tactics.InOrderSearchTactic;
import org.fuzzingtool.core.tactics.RandomSearchTactic;
import org.fuzzingtool.core.visualization.BranchingVisualizer;
import org.graalvm.collections.Pair;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Central class for managing execution flow events and fuzzing attempts
 * The class is designed as a state-machine. Methods
 * like {@link #branchingEvent(Integer, BranchingNodeAttribute, Integer, Boolean, String)} modify said machine.
 */
public class Amygdala {
	public final Logger logger;
	public final Tracer tracer;
	public final Coverage coverage;
	public final CustomError custom_error;
	public TimeProbe probe;
	private FuzzingTactic tactic;
	private final BranchingNode branching_root_node;
	private BranchingNode current_branch;
	private final Context z3_ctx;
	private Queue<Pair<Integer, Boolean>> next_program_path = new LinkedList<>();
	private final List<Map<VariableIdentifier, Object>> variable_values;
	private final List<Pair<Boolean, String>> iteration_information;
	private final List<Long> runtime_nanos;
	private final List<Pair<Integer, Integer>> execution_graph_statistics;
	private final List<Map<BranchingNodeAttribute, Integer>> execution_graph_component_statistics;
	private final BidiMap<VariableIdentifier, Integer> variable_lines;
	private final Map<VariableIdentifier, String> variable_names;
	private Boolean fuzzing_finished = false;
	private int fuzzing_iterations = 0;
	private int max_iterations = 1024;
	private boolean function_visualization = false;
	private boolean branching_visualization = false;
	private boolean event_logging = true;
	private String program_path = "";
	private String results_path = "";
	private double min_coverage_root = 100.0;
	private double min_coverage_statement = 100.0;
	private double min_coverage_branch = 100.0;
	private boolean timeout_reached = false;
	private long timeout_millis = 10000;

	// Experimental
	// This option advises JSReadCurrent/ScopeFrameSlotNodeGen to fill in values if they are not found.
	public static final boolean EXPERIMENTAL_FRAMESLOT_FILL_IN_NONEXISTENT = false;
	// This option disables computation of new input values and instead
	// locks them to the values specified in the configuration YAML-file.
	public static final boolean LOCK_VALUES = false;
	// This option measures size and depth of the execution graph after every iteration
	public static final boolean EXECUTION_GRAPH_STATISTICS = true;

	// Debugging Bits: is node executed, onEnter (E), onInputValue (I), OnReturn (R), onReturnExceptional (X), onUnwind (U), onDispose (D)
	public final HashMap<String, BitSet> node_type_instrumented = new HashMap<>();

	public Amygdala(Logger lgr) {
		this.tracer = new Tracer(lgr);
		this.coverage = new Coverage(lgr);
		this.custom_error = new CustomError(lgr);
		this.logger = lgr;
		this.variable_values = new ArrayList<>();
		this.variable_names = new HashMap<>();
		this.variable_lines = new DualHashBidiMap<>();
		this.runtime_nanos = new ArrayList<>();
		this.execution_graph_statistics = new ArrayList<>();
		this.execution_graph_component_statistics = new ArrayList<>();
		this.iteration_information = new ArrayList<>();

		com.microsoft.z3.Global.ToggleWarningMessages(true);
		com.microsoft.z3.Global.setParameter("smt.string_solver", "z3str3");
		com.microsoft.z3.Global.setParameter("timeout", "10000");
		logger.info("Using Z3 " + Version.getString() + " © Copyright 2006-2016 Microsoft Corp.");

		HashMap<String, String> cfg = new HashMap<>();
		cfg.put("model", "true");
		z3_ctx = new Context(cfg);

		branching_root_node = new BranchingNode();
		current_branch = branching_root_node;
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
	public void branchingEvent(Integer branching_node_hash, BranchingNodeAttribute bt, Integer predicate_interim_key,
							   Boolean taken, String vis_predicate_string) {
		if (next_program_path.peek() != null) {
			Pair<Integer, Boolean> expected_behavior = next_program_path.poll();
			if (!expected_behavior.getLeft().equals(branching_node_hash) || !expected_behavior.getRight().equals(taken)) {
				logger.info("Diverging program path detected");
				logger.alert("Diverging program path detected");
				current_branch.setDiverging();
			}
		}
		if (current_branch.getBranchingNodeAttribute() != BranchingNodeAttribute.BRANCH &&
				current_branch.getBranchingNodeAttribute() != BranchingNodeAttribute.LOOP) {
			current_branch.setProperties(tracer.getIntermediate(predicate_interim_key), branching_node_hash, bt);
			current_branch.setSourceCodeExpression(vis_predicate_string);
			current_branch.initializeChildren();
		}
		current_branch = current_branch.getChildBranch(taken);
	}

	/**
	 * A call to this method indicates that the fuzzed
	 * program has been terminated under normal circumstances.
	 */
	public void terminateEvent(Long runtime) {
		logger.info("Program terminated without error");
		current_branch.setBranchingNodeAttribute(BranchingNodeAttribute.TERMINATE);
		current_branch = branching_root_node;
		iteration_information.add(Pair.create(true, ""));
		runtime_nanos.add(runtime);
		fuzzing_iterations += 1;
	}

	/**
	 * A call to this method indicates an error while executing the program.
	 * The function suppresses the next terminate-event.
	 */
	public void errorEvent(String reason, Long runtime) {
		logger.info("Program fault detected: " + reason);
		current_branch.setBranchingNodeAttribute(BranchingNodeAttribute.ERROR);
		current_branch = branching_root_node;
		iteration_information.add(Pair.create(false, reason));
		runtime_nanos.add(runtime);
		this.fuzzing_iterations += 1;
	}

	/**
	 * This function returns the number of completed fuzzing iterations.
	 *
	 * @return An int, specifying the number of completed runs, effectively the count of terminate() signals.
	 */
	public int getIteration() {
		return this.fuzzing_iterations;
	}

	public List<Long> getRuntimes() {
		return this.runtime_nanos;
	}

	public List<Map<VariableIdentifier, Object>> getVariableValues() {
		return this.variable_values;
	}

	public String getResultsPath() {
		return this.results_path;
	}

	public String getProgramPath() {
		return this.program_path;
	}

	public void setTimeProbe(TimeProbe tp) {
		this.probe = tp;
	}

	public void setTimeoutReached(boolean value) {
		this.timeout_reached = value;
	}

	public boolean timeoutReached() {
		return this.timeout_reached;
	}

	public long getTimeoutMillis() {
		return this.timeout_millis;
	}

	/**
	 * This Method uses a specified tactic to find the next path in the program flow.
	 * If the tactic cannot find another path, the global fuzzing-loop has to be terminated.
	 *
	 * @return The Boolean value for the wrapper
	 */
	public Boolean calculateNextPath() {
		if (fuzzing_iterations < max_iterations) {
			if (!coverage.coverageReached(this.min_coverage_root, this.min_coverage_statement, this.min_coverage_branch)) {
				if (!LOCK_VALUES) {
					probe.switchState(TimeProbe.ProgramState.TACTIC);
					logger.info("Finding next path...");
					boolean res = this.tactic.calculate();
					probe.switchState(TimeProbe.ProgramState.MANAGE);
					if (res) {
						variable_values.add(this.tactic.getNextValues());
						next_program_path = this.tactic.getNextPath();
						return true;
					} else {
						fuzzing_finished = true;
						return false;
					}
				} else {
					return true;
				}
			} else {
				logger.info("Required coverage reached.");
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
		if (variable_values.get(variable_values.size() - 1).containsKey(var_id)) {
			Object next_input = variable_values.get(variable_values.size() - 1).get(var_id);
			if (var_id.getVariableType() == ExpressionType.STRING) {
				logger.info("Next input value for variable '" + variable_names.get(var_id) + "': '" + next_input + "' [STRING]");
			} else {
				logger.info("Next input value for variable '" + variable_names.get(var_id) + "': " + next_input + " [" + var_id.getVariableType().name() + "]");
			}
			return next_input;
		} else {
			logger.info("No new value for variable: '" + variable_names.get(var_id) + "'");
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
					logger.critical("Variable '" + variable_names.get(var_id) + "' has not allowed type '" +
											var_id.getVariableType().toString() + "'");
					return null;
			}
		}
	}

	/**
	 * Visualize and save the complete program-flow tree to a file.
	 *
	 * @param name Name of the .svg file, saved into results directory
	 */
	public void visualizeProgramFlow(String name) {
		String save_name = Paths.get(this.results_path, "trace_tree", name).toString();
		File save_path = new File(save_name);
		if (!save_path.exists()) {
			BranchingVisualizer bv = new BranchingVisualizer(branching_root_node, this.logger);
			bv.saveImage(save_path);
		}
	}

	/**
	 * Load fuzzing options from YAML file.
	 *
	 * @param map Java Object containing the options
	 */
	@SuppressWarnings("unchecked")
	public void loadOptions(Map<String, Object> map, String config_file_path_abs) {
		if (map.containsKey("program_path") && map.get("program_path") instanceof String) {
			File program_file_path = new File((String) map.get("program_path"));
			if (program_file_path.isAbsolute()) {
				this.program_path = program_file_path.getAbsolutePath();
			} else {
				this.program_path = Paths.get(config_file_path_abs, program_file_path.getPath()).toString();
			}
		} else {
			logger.critical("No attribute 'program_path' in configuration file");
		}
		this.program_path = Paths.get(this.program_path).normalize().toString();

		if (map.containsKey("results") && map.get("results") instanceof String) {
			File results_file_path = new File((String) map.get("results"));
			if (results_file_path.isAbsolute()) {
				this.results_path = results_file_path.getAbsolutePath();
			} else {
				this.results_path = Paths.get(config_file_path_abs, results_file_path.getPath()).toString();
			}
		} else {
			this.results_path = Paths.get(config_file_path_abs, "results").toString();
		}
		this.results_path = Paths.get(this.results_path).normalize().toString();
		logger.info("Results are written to '" + this.results_path + "'");

		if (map.containsKey("variables") && map.get("variables") instanceof List) {
			loadVariables((List<Map<String, Object>>) map.get("variables"));
		} else {
			logger.warning("No variable configuration found");
		}

		if (map.containsKey("fuzzing_parameters") && map.get("fuzzing_parameters") instanceof Map) {
			loadFuzzingParameters((Map<String, Object>) map.get("fuzzing_parameters"));
		} else {
			logger.warning("No general configuration found");
		}

		if (map.containsKey("visualization") && map.get("visualization") instanceof Map) {
			loadVisualizationParameters((Map<String, Object>) map.get("visualization"));
		} else {
			logger.info("No visualization configuration found, visualization disabled");
		}

		if (map.containsKey("custom_errors") && map.get("custom_errors") instanceof Map) {
			loadCustomErrorParameters((Map<String, Object>) map.get("custom_errors"));
		} else {
			logger.info("No custom error configuration found");
		}
	}

	/**
	 * This Method initializes all given variables and sample-inputs.
	 *
	 * @param variable_list YAML-Map of te variables
	 */
	private void loadVariables(List<Map<String, Object>> variable_list) {
		Map<VariableIdentifier, Object> initial_values = new HashMap<>();
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
			variable_lines.put(new_identifier, line_num);
			if (var_declaration.containsKey("name")) {
				variable_names.put(new_identifier, var_declaration.get("name").toString());
			} else {
				variable_names.put(new_identifier, var_gid);
			}

			switch (var_type_enum) {
				case BOOLEAN:
				case STRING:
				case BIGINT:
				case NUMBER_INTEGER:
				case NUMBER_REAL:
					initial_values.put(new_identifier, var_declaration.get("sample"));
					break;
			}
		}
		variable_values.add(initial_values);
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

		SymbolicNode.partial_evaluation_on_cast = (boolean) parameters.getOrDefault("partial_evaluation_on_cast", false);
		if (SymbolicNode.partial_evaluation_on_cast) {
			logger.info("Option partial_evaluation_on_cast enabled");
		}

		if (parameters.containsKey("required_coverage") && parameters.get("required_coverage") instanceof Map) {
			Map<String, Object> minc = (Map<String, Object>) parameters.get("required_coverage");
			this.min_coverage_root = Double.parseDouble(minc.getOrDefault("root", 100.0).toString());
			this.min_coverage_statement = Double.parseDouble(minc.getOrDefault("statement", 100.0).toString());
			this.min_coverage_branch = Double.parseDouble(minc.getOrDefault("branch", 100.0).toString());
		}
		logger.info("Minimum coverage set to " + this.min_coverage_root + " (root), " +
							this.min_coverage_statement + " (statement), " +
							this.min_coverage_branch + " (branch)");

		String tactic_string = (String) parameters.getOrDefault("tactic", "DEPTH_SEARCH");
		switch (tactic_string) {
			case "IN_ORDER_SEARCH":
				logger.info("Using tactic IN_ORDER_SEARCH");
				this.tactic = new InOrderSearchTactic(this.branching_root_node, this.z3_ctx, this.logger);
				break;
			case "DEPTH_SEARCH":
				logger.info("Using tactic DEPTH_SEARCH");
				this.tactic = new DepthSearchTactic(this.branching_root_node, this.z3_ctx, this.logger);
				break;
			case "RANDOM_SEARCH":
				logger.info("Using tactic RANDOM_SEARCH");
				this.tactic = new RandomSearchTactic(this.branching_root_node, this.z3_ctx, this.logger);
				break;
			default:
				logger.warning("Unknown tactic '" + tactic_string +
									   "', using tactic IN_ORDER_SEARCH");
				this.tactic = new InOrderSearchTactic(this.branching_root_node, this.z3_ctx, this.logger);
		}
		this.tactic.setTimeProbe(this.probe);
		if (parameters.containsKey("tactic_options")) {
			Map<String, Object> ds_params = (Map<String, Object>) parameters.get("tactic_options");
			if (ds_params.containsKey("max_loop_unrolling")) {
				this.tactic.setOption("max_loop_unrolling", ds_params.get("max_loop_unrolling"));
			}
			if (ds_params.containsKey("max_depth")) {
				this.tactic.setOption("max_depth", ds_params.get("max_depth"));
			}
			if (ds_params.containsKey("seed")) {
				this.tactic.setOption("seed", ds_params.get("seed"));
			}
		}
	}

	/**
	 * Load visualization options from the YAML file.
	 *
	 * @param parameters YAML-Map of the options
	 */
	private void loadVisualizationParameters(Map<String, Object> parameters) {
		this.function_visualization = (boolean) parameters.getOrDefault("function_visualization", this.function_visualization);
		if (this.function_visualization) {
			logger.info("Function visualization enabled");
		}
		this.branching_visualization = (boolean) parameters.getOrDefault("branching_visualization", this.branching_visualization);
		if (this.branching_visualization) {
			logger.info("Branching visualization enabled");
		}
		this.event_logging = (boolean) parameters.getOrDefault("event_logging", this.event_logging);
		if (this.event_logging) {
			logger.info("Event logging enabled");
		}
	}

	/**
	 * Load custom error options from the YAML file.
	 *
	 * @param parameters YAML-Map of the options
	 */
	private void loadCustomErrorParameters(Map<String, Object> parameters) {
		for (Map.Entry<String, Object> entry : parameters.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			custom_error.setOption(key, value);
		}
	}

	/**
	 * Checks if the node at the given line is an input node (called at construction of the node).
	 *
	 * @param line_num Line number in the source code
	 * @return A pair, true if it is input node, false if not. If true, the Variable identifier is also returned
	 */
	public Pair<Boolean, VariableIdentifier> getInputNodeConfiguration(Integer line_num) {
		VariableIdentifier id = variable_lines.getKey(line_num);
		return Pair.create(id != null, id);
	}

	/**
	 * @return true, if function visualization is enabled, false otherwise
	 */
	public boolean isFunctionVisEnabled() {
		return this.function_visualization;
	}

	/**
	 * @return true, if branching visualization is enabled, false otherwise
	 */
	public boolean isBranchingVisEnabled() {
		return this.branching_visualization;
	}

	/**
	 * @return true, if event logging is enabled, false otherwise
	 */
	public boolean isEventLoggingEnabled() {
		return this.event_logging;
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
	public void printInstrumentation() {
		logger.log(getInstrumentationString());
	}

	public void snapshot() {
		coverage.saveSnapshot();
		if (this.branching_visualization) {
			visualizeProgramFlow("trace_tree_" + getIteration() + ".svg");
		}
		if (EXECUTION_GRAPH_STATISTICS) {
			int graph_size = branching_root_node.getTreeSize();
			int graph_height = branching_root_node.getTreeHeight();
			execution_graph_statistics.add(Pair.create(graph_size, graph_height));
			Map<BranchingNodeAttribute, Integer> components = new HashMap<>();
			components.put(BranchingNodeAttribute.BRANCH, 0);
			components.put(BranchingNodeAttribute.LOOP, 0);
			components.put(BranchingNodeAttribute.UNKNOWN, 0);
			components.put(BranchingNodeAttribute.UNREACHABLE, 0);
			components.put(BranchingNodeAttribute.TERMINATE, 0);
			components.put(BranchingNodeAttribute.ERROR, 0);
			branching_root_node.getComponents(components);
			execution_graph_component_statistics.add(components);
		}
	}

	/**
	 * Returns a string-representation of the instrumentation statistics
	 *
	 * @return A string-representation of the statistics
	 */
	public String getInstrumentationString() {
		int NAME_WIDTH = 36;
		StringBuilder stat_str = new StringBuilder();
		stat_str.append("===NODE INSTRUMENTATION INSIGHT===\n");
		stat_str.append(String.format("%-" + NAME_WIDTH + "s", "NODE NAME")).append("E I R X  U D\n");
		for (String key : node_type_instrumented.keySet()) {
			BitSet curr_set = node_type_instrumented.get(key);
			boolean executed = curr_set.get(0);
			boolean instrumented = curr_set.get(1) || curr_set.get(2) || curr_set.get(3) || curr_set.get(4);

			String node_name = key;
			node_name = Logger.capBack(node_name, NAME_WIDTH - 1);
			node_name = String.format("%-" + NAME_WIDTH + "s", node_name);

			if (executed) {
				if (instrumented) {
					stat_str.append(node_name);
				} else {
					stat_str.append("\033[41m").append(node_name).append("\033[0m");
				}
			} else {
				stat_str.append("\033[43m").append(node_name).append("\033[0m");
			}

			if (curr_set.get(1)) {
				stat_str.append("✔ ");
			} else {
				stat_str.append("✗ ");
			}
			if (curr_set.get(2)) {
				stat_str.append("✔ ");
			} else {
				stat_str.append("✗ ");
			}
			if (curr_set.get(3)) {
				stat_str.append("✔ ");
			} else {
				stat_str.append("✗ ");
			}
			if (curr_set.get(4)) {
				stat_str.append("✔  ");
			} else {
				stat_str.append("✗  ");
			}
			if (curr_set.get(5)) {
				stat_str.append("✔ ");
			} else {
				stat_str.append("✗ ");
			}
			if (curr_set.get(6)) {
				stat_str.append("✔");
			} else {
				stat_str.append("✗");
			}
			stat_str.append("\n");
		}
		return stat_str.toString();
	}

	public Map<String, Object> getResults() {
		Map<String, Object> result_map = new HashMap<>();
		result_map.put("fuzzing_finished", fuzzing_finished);
		result_map.put("num_iterations", fuzzing_iterations);
		List<Map<String, Object>> iterations = new ArrayList<>();
		for (int i = 0; i < fuzzing_iterations; i++) {
			Map<String, Object> iteration = new HashMap<>();

			iteration.put("successful", iteration_information.get(i).getLeft());
			if (!iteration_information.get(i).getLeft()) {
				iteration.put("error_message", iteration_information.get(i).getRight());
			}
			iteration.put("runtime", runtime_nanos.get(i) / 1000000);

			if (EXECUTION_GRAPH_STATISTICS) {
				Pair<Integer, Integer> graph_stat = execution_graph_statistics.get(i);
				iteration.put("execution_graph_height", graph_stat.getRight());
				iteration.put("execution_graph_size", graph_stat.getLeft());
				Map<String, Integer> converted_components = new HashMap<>();
				for (Map.Entry<BranchingNodeAttribute, Integer> entry: execution_graph_component_statistics.get(i).entrySet()) {
					converted_components.put(String.valueOf(entry.getKey()), entry.getValue());
				}
				iteration.put("execution_graph_components", converted_components);
			}

			List<Map<String, Object>> variables = new ArrayList<>();
			if (LOCK_VALUES) {
				for (Map.Entry<VariableIdentifier, Object> entry: variable_values.get(0).entrySet()) {
					addVariableResult(variables, entry);
				}
			} else {
				for (Map.Entry<VariableIdentifier, Object> entry: variable_values.get(i).entrySet()) {
					addVariableResult(variables, entry);
				}
			}
			iteration.put("assignments", variables);

			iteration.put("coverage", coverage.getCoverageObject(i));

			iterations.add(iteration);
		}
		result_map.put("iterations", iterations);

		return result_map;
	}

	private void addVariableResult(List<Map<String, Object>> variables, Map.Entry<VariableIdentifier, Object> entry) {
		VariableIdentifier key = entry.getKey();
		Object value = entry.getValue();
		Map<String, Object> variable = new HashMap<>();
		variable.put("name", variable_names.get(key));
		variable.put("line", variable_lines.get(key));
		variable.put("value", value);
		variables.add(variable);
	}
}
