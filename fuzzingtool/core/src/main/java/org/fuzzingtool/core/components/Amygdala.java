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
import java.util.*;

/**
 * Central class for managing execution flow events and fuzzing attempts
 * The class is designed as a state-machine. Methods
 * like {@link #branching_event(Integer, BranchingNodeAttribute, Integer, Boolean, String)} modify said machine.
 */
public class Amygdala {
	public final Logger logger;
	public final Tracer tracer;
	public final Coverage coverage;
	public final CustomError custom_error;
	private FuzzingTactic tactic;
	private final BranchingNode branchingRootNode;
	private BranchingNode currentBranch;
	private final Context z3_ctx;
	private Queue<Pair<Integer, Boolean>> next_program_path = new LinkedList<>();
	private final List<Map<VariableIdentifier, Object>> variable_values;
	private final List<Pair<Boolean, String>> iteration_information;
	private final List<Long> runtime_nanos;
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

	// Experimental
	// This option advises JSReadCurrent/ScopeFrameSlotNodeGen to fill in values if they are not found.
	public boolean experimental_frameslot_fill_in_nonexistent = false;

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
		this.iteration_information = new ArrayList<>();

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
	public void terminate_event(Long runtime) {
		currentBranch.setBranchingNodeAttribute(BranchingNodeAttribute.TERMINATE);
		currentBranch = branchingRootNode;
		iteration_information.add(Pair.create(true, ""));
		runtime_nanos.add(runtime);
		fuzzing_iterations += 1;
	}

	/**
	 * A call to this method indicates an error while executing the program.
	 * The function suppresses the next terminate-event.
	 */
	public void error_event(String reason, Long runtime) {
		currentBranch.setBranchingNodeAttribute(BranchingNodeAttribute.ERROR);
		currentBranch = branchingRootNode;
		iteration_information.add(Pair.create(false, reason));
		runtime_nanos.add(runtime);
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

	public String getResultsPath() {
		return this.results_path;
	}

	public String getProgramPath() {
		return this.program_path;
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
				variable_values.add(this.tactic.getNextValues());
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
		if (variable_values.get(variable_values.size() - 1).containsKey(var_id)) {
			Object next_input = variable_values.get(variable_values.size() - 1).get(var_id);
			if (var_id.getVariableType() == ExpressionType.STRING) {
				logger.info("Next input value for variable \"" + variable_names.get(var_id) + "\": \"" + next_input + "\" [STRING]");
			} else {
				logger.info("Next input value for variable \"" + variable_names.get(var_id) + "\": " + next_input + " [" + var_id.getVariableType().name() + "]");
			}
			return next_input;
		} else {
			logger.info("No new value for variable: \"" + variable_names.get(var_id) + "\"");
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
					logger.critical("Variable \"" + variable_names.get(var_id) + "\" has not allowed type '" +
											var_id.getVariableType().toString() + "'.");
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
			BranchingVisualizer bv = new BranchingVisualizer(branchingRootNode, this.logger);
			bv.save_image(save_path);
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
			logger.critical("No attribute 'program_path' in configuration file.");
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
		logger.info("Results are written to '" + this.results_path + "'.");

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

		if (map.containsKey("visualization") && map.get("visualization") instanceof Map) {
			loadVisualizationParameters((Map<String, Object>) map.get("visualization"));
		} else {
			logger.info("No visualization configuration found, visualization disabled.");
		}

		if (map.containsKey("custom_errors") && map.get("custom_errors") instanceof Map) {
			loadCustomErrorParameters((Map<String, Object>) map.get("custom_errors"));
		} else {
			logger.info("No custom error configuration found.");
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
					initial_values.put(new_identifier, (Boolean) var_declaration.get("sample"));
					break;
				case STRING:
					initial_values.put(new_identifier, (String) var_declaration.get("sample"));
					break;
				case BIGINT:
				case NUMBER_INTEGER:
					initial_values.put(new_identifier, (Integer) var_declaration.get("sample"));
					break;
				case NUMBER_REAL:
					initial_values.put(new_identifier, (Double) var_declaration.get("sample"));
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

		SymbolicNode.PARTIAL_EVALUATION_ON_CAST = (boolean) parameters.getOrDefault("partial_evaluation_on_cast", false);
		if (SymbolicNode.PARTIAL_EVALUATION_ON_CAST) {
			logger.info("Option partial_evaluation_on_cast enabled");
		}

		String tactic_string = (String) parameters.getOrDefault("tactic", "DEPTH_SEARCH");
		switch (tactic_string) {
			case "IN_ORDER_SEARCH":
				logger.info("Using tactic IN_ORDER_SEARCH");
				this.tactic = new InOrderSearchTactic(this.branchingRootNode, this.z3_ctx, this.logger);
				if (parameters.containsKey("tactic_in_order_search")) {
					Map<String, Object> ds_params = (Map<String, Object>) parameters.get("tactic_in_order_search");
					if (ds_params.containsKey("max_loop_unrolling")) {
						this.tactic.setOption("max_loop_unrolling", ds_params.get("max_loop_unrolling"));
					}
					if (ds_params.containsKey("max_depth")) {
						this.tactic.setOption("max_depth", ds_params.get("max_depth"));
					}
				}
				break;
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
			case "RANDOM_SEARCH":
				logger.info("Using tactic RANDOM_SEARCH");
				this.tactic = new RandomSearchTactic(this.branchingRootNode, this.z3_ctx, this.logger);
				if (parameters.containsKey("tactic_random_search")) {
					Map<String, Object> ds_params = (Map<String, Object>) parameters.get("tactic_random_search");
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
				break;
			default:
				logger.warning("Unknown tactic '" + tactic_string +
									   "', using tactic IN_ORDER_SEARCH with default " + "params");
				this.tactic = new InOrderSearchTactic(this.branchingRootNode, this.z3_ctx, this.logger);
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
			logger.info("Function visualization enabled.");
		}
		this.branching_visualization = (boolean) parameters.getOrDefault("branching_visualization", this.branching_visualization);
		if (this.branching_visualization) {
			logger.info("Branching visualization enabled.");
		}
		this.event_logging = (boolean) parameters.getOrDefault("event_logging", this.event_logging);
		if (this.event_logging) {
			logger.info("Event logging enabled.");
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

			List<Map<String, Object>> variables = new ArrayList<>();
			for (Map.Entry<VariableIdentifier, Object> entry: variable_values.get(i).entrySet()) {
				VariableIdentifier key = entry.getKey();
				Object value = entry.getValue();
				Map<String, Object> variable = new HashMap<>();
				variable.put("name", variable_names.get(key));
				variable.put("line", variable_lines.get(key));
				variable.put("value", value);
				variables.add(variable);
			}
			iteration.put("assignments", variables);

			iteration.put("coverage", coverage.getCoverageObject(i));

			iterations.add(iteration);
		}
		result_map.put("iterations", iterations);

		return result_map;
	}
}
