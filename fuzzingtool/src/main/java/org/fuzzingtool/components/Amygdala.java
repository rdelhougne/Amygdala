package org.fuzzingtool.components;

import com.microsoft.z3.Context;
import org.fuzzingtool.Logger;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.tactics.DepthSearchTactic;
import org.fuzzingtool.tactics.FuzzingException;
import org.fuzzingtool.visualization.BranchingVisualizer;
import org.graalvm.collections.Pair;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central class for managing execution flow events and fuzzing attempts
 * The class is designed as a state-machine. Methods like {@link #branching_event(Integer, BranchingNodeAttribute, Integer, Boolean, String)} modify said machine.
 */
public class Amygdala {
    public Tracer tracer;
    public Logger logger;
    public BranchingNode branchingRootNode;
    private BranchingNode currentBranch;
    public Context z3_ctx;
    public HashMap<VariableIdentifier, Object> variable_values;
    public HashMap<VariableIdentifier, ExpressionType> variable_types;
    public HashMap<Integer, VariableIdentifier> variable_line_to_identifier;
    public boolean fuzzing_finished = false;
    private int fuzzing_iterations = 1;
    private boolean suppress_next_terminate = false;
    private boolean is_first_run = true;

    //Fuzzing Configuration TODO
    public int main_loop_line_num = 1;
    public String main_loop_identifier_string = "fuzzing_main_loop"; // Needed if the program happens to be combined of several files
    public int error_line_num = 7;
    public String error_identifier_string = "fuzzing_error"; // Needed if the program happens to be combined of several files
    private int max_iterations = 1024;

    public Amygdala(Logger lgr) {
        this.tracer = new Tracer(lgr);
        this.tracer.initialize_program_context(LanguageSemantic.JAVASCRIPT);
        this.logger = lgr;
        this.variable_values = new HashMap<>();
        this.variable_types = new HashMap<>();
        this.variable_line_to_identifier = new HashMap<>();

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
     * @param branching_node_hash Hash-code of the Truffle node
     * @param bt Kind of the statement (BRANCH or LOOP)
     * @param predicate_interim_key Key to the boolean expression
     * @param taken Result of the evaluated expression
     * @param vis_predicate_string String representation of the expression for visualization
     */
    public void branching_event(Integer branching_node_hash, BranchingNodeAttribute bt, Integer predicate_interim_key, Boolean taken, String vis_predicate_string) {
        // TODO Hier Konsistenz prüfen: Ist dies wirklich der Pfad den ich gehen wollte?
        if (currentBranch.getBranchingNodeAttribute() == BranchingNodeAttribute.BRANCH // "Hier bin ich schonmal gewesen" -> einfach weiter durchlaufen
                || currentBranch.getBranchingNodeAttribute() == BranchingNodeAttribute.LOOP) {
            currentBranch = currentBranch.getChildBranch(taken);
        } else { // unbekannter pfad -> updaten
            currentBranch.setProperties(tracer.get_interim(predicate_interim_key), branching_node_hash, bt);
            currentBranch.setSourceCodeExpression(vis_predicate_string);
            currentBranch.initializeChildren();
            currentBranch = currentBranch.getChildBranch(taken);
        }
    }

    /**
     * A call to this method indicates that the fuzzed
     * program has been terminated under normal circumstances.
     */
    public void terminate_event() {
        if (suppress_next_terminate) {
            suppress_next_terminate = false;
            return;
        }
        currentBranch.setBranchingNodeAttribute(BranchingNodeAttribute.TERMINATE);
        currentBranch = branchingRootNode;
        tracer.clearAll();
        this.fuzzing_iterations += 1;
    }

    /**
     * A call to this method indicates an error while executing the program.
     * The function suppresses the next terminate-event.
     */
    public void error_event() {
        currentBranch.setBranchingNodeAttribute(BranchingNodeAttribute.ERROR);
        currentBranch = branchingRootNode;
        tracer.clearAll();
        this.fuzzing_iterations += 1;
        suppress_terminate();
    }

    /**
     * This function suppresses the next terminate event. Useful for the first run and error events.
     * This function has side effects.
     */
    public void suppress_terminate() {
        suppress_next_terminate = true;
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
     * This function returns if the fuzzing has just started.
     * The only purpose is to prevent the global fuzzing loop to execute a terminate()
     * signal at the beginning of the fuzzing. This function has a side-effect.
     *
     * @return A boolean, true if it is the first run, false otherwise
     */
    public boolean isFirstRun() {
        if (this.is_first_run) {
            this.is_first_run = false;
            return true;
        }
        return false;
    }

    /**
     * This Method uses a specified tactic to find the next path in the program flow.
     * If the tactic cannot find another path, the global fuzzing-loop has to be terminated.
     *
     * @return The Boolean value that is DIRECTLY fed into the JSConstantBooleanNode, which is the child of the global_while_node
     */
    public Boolean calculateNextPath() {
        if (fuzzing_iterations <= max_iterations) {
            DepthSearchTactic tac = new DepthSearchTactic(this.branchingRootNode, this.z3_ctx, this.logger);
            try {
                variable_values = tac.getNextValues(variable_types);
            } catch (FuzzingException.NoMorePaths noMorePaths) {
                fuzzing_finished = true;
                return false;
            }
            return true;
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
            return variable_values.get(var_id);
        } else {
            logger.warning("No new value for variable: " + var_id.getIdentifierString());
            switch (variable_types.get(var_id)) {
                case BOOLEAN:
                    return true;
                case STRING:
                    return "abc";
                case BIGINT:
                    return 1;
                case NUMBER_INTEGER:
                    return 1;
                case NUMBER_REAL:
                    return 1.5;
                default:
                    logger.critical("Variable " + var_id.getIdentifierString() + " has not allowed type '" + variable_types.get(var_id).toString() + "'.");
                    return null;
            }
        }
    }

    /**
     * Returns the last run as SMT-Lib 2 formatted expression.
     *
     * @return A String, containing the expression in SMT-Lib 2 Format.
     */
    public String lastRunToSMTExpr() {
        ArrayList<String> exp = currentBranch.getSymbolicPathSMTExpression();
        StringBuilder exp_str = new StringBuilder();
        exp_str.append("(declare-const n Int)\n"); // TODO
        exp_str.append("(assert\n");
        exp_str.append("\t(and\n");
        for (String str: exp) {
            exp_str.append("\t\t").append(str).append("\n");
        }
        exp_str.append("\t)\n");
        exp_str.append(")");
        return exp_str.toString();
    }

    /**
     * Returns the last run as human readable expression.
     *
     * @return A String, containing the expression in human readable Format.
     */
    public String lastRunToHumanReadableExpr() throws SymbolicException.WrongParameterSize {
        ArrayList<String> exp = currentBranch.getSymbolicPathHRExpression();
        StringBuilder exp_str = new StringBuilder();
        for (String str: exp) {
            exp_str.append(str).append("\n");
        }
        return exp_str.toString();
    }

    /**
     * Returns the last run as Z3 expression.
     */
    /*public void lastRunToZ3Expr() {
        BoolExpr path_expr = currentBranch.getSymbolicPathZ3Expression(z3_ctx);
        Solver s = z3_ctx.mkSolver();
        s.add(path_expr);
        if (s.check() == Status.SATISFIABLE) {
            Model m = s.getModel();
            logger.log("n = " + m.evaluate(z3_ctx.mkIntConst("n"), false));
        }
        logger.log(path_expr.toString());
    }*/

    /**
     * Visualize and save the complete program-flow tree to a file.
     *
     * @param path Filepath of the .svg file
     */
    public void visualizeProgramFlow(String path) {
        BranchingVisualizer bv = new BranchingVisualizer(branchingRootNode);
        bv.save_image(path);
    }

    public void loadOptions(String path, Integer lineOffset) {
        FileInputStream fis = null;
        Map<String, Object> map;
        try {
            Load load = new Load(LoadSettings.builder().build());
            fis = new FileInputStream(path);
            map = (Map<String, Object>) load.loadFromInputStream(fis);
        } catch (FileNotFoundException e) {
            logger.critical("File not found: " + path);
            return;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        if (map.containsKey("variables") && map.get("variables") instanceof List) {
            loadVariables((List<Map<String, Object>>) map.get("variables"), lineOffset);
        } else {
            logger.warning("No variable configuration found in file " + path);
        }

        if (map.containsKey("fuzzing_parameters") && map.get("fuzzing_parameters") instanceof Map) {
            loadFuzzingParameters((Map<String, Object>) map.get("fuzzing_parameters"));
        } else {
            logger.warning("No general configuration found in file " + path);
        }
    }

    /**
     * This Method initializes all given variables and sample-inputs.
     */
    private void loadVariables(List<Map<String, Object>> variable_list, Integer lineOffset) {
        for (Map<String, Object> var_declaration : variable_list) {
            Integer line_num = (Integer) var_declaration.get("line_num") + lineOffset;
            String name = (String) var_declaration.get("name");
            String var_type = (String) var_declaration.get("type");

            ExpressionType var_type_enum;
            switch (var_type) {
                case "BIGINT":
                    var_type_enum = ExpressionType.BIGINT;
                    break;
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
                    logger.warning("Unknown variable type '" + var_type + "' for variable " + name);
                    continue;
            }

            VariableIdentifier new_identifier = VariableIdentifier.fromString(name);
            variable_line_to_identifier.put(line_num, new_identifier);
            variable_types.put(new_identifier, var_type_enum);

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

    private void loadFuzzingParameters(Map<String, Object> parameters) {
        this.max_iterations = (int) parameters.getOrDefault("max_iterations", this.max_iterations);
    }

    public Pair<Boolean, VariableIdentifier> getInputNodeConfiguration(Integer line_num) {
        if (variable_line_to_identifier.containsKey(line_num)) {
            return Pair.create(true, variable_line_to_identifier.get(line_num));
        } else {
            return Pair.create(false, null);
        }
    }
}
