package org.fuzzingtool.components;

import com.microsoft.z3.*;
import org.fuzzingtool.Logger;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.tactics.DepthSearchTactic;
import org.fuzzingtool.tactics.FuzzingException;
import org.fuzzingtool.visualization.BranchingVisualizer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Central class for managing execution flow events and fuzzing attempts
 * The class is contructed as a state-machine. Methods like {@link #branching_event(Integer, BranchingNodeAttribute, Integer, Boolean, String)} modify said machine.
 */
public class Amygdala {
    public Tracer tracer;
    public Logger logger;
    public BranchingNode branchingRootNode;
    private BranchingNode currentBranch;
    public Context z3_ctx;
    public HashMap<VariableIdentifier, Object> variable_values;
    public HashMap<VariableIdentifier, ExpressionType> variable_types;
    public boolean fuzzing_finished = false;
    private int fuzzing_iterations = 1;
    private boolean suppress_next_terminate = false;
    private boolean is_first_run = true;

    //Fuzzing Configuration TODO
    public int input_line_num = 26;
    public int main_loop_line_num = 1;
    public String main_loop_identifier_string = "fuzzing_main_loop"; // Needed if the program happens to be combined of several files
    public int error_line_num = 7;
    public String error_identifier_string = "fuzzing_error"; // Needed if the program happens to be combined of several files

    public Amygdala(Logger lgr) {
        this.tracer = new Tracer(lgr);
        this.logger = lgr;
        this.variable_values = new HashMap<>();
        this.variable_types = new HashMap<>();

        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        z3_ctx = new Context(cfg);

        branchingRootNode = new BranchingNode();
        currentBranch = branchingRootNode;

        initialize_sample_inputs();
    }

    /**
     * This Method initializes all given sample-inputs.
     */
    public void initialize_sample_inputs() { // TODO
        variable_values.put(new VariableIdentifier("n"), (Integer) 5);
        variable_types.put(new VariableIdentifier("n"), ExpressionType.NUMBER_INTEGER);
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
        DepthSearchTactic tac = new DepthSearchTactic(this.branchingRootNode, this.z3_ctx, this.logger);
        try {
            variable_values = tac.getNextValues(variable_types);
        } catch (FuzzingException.NoMorePaths noMorePaths) {
            fuzzing_finished = true;
            return false;
        }
        return true;
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
}
