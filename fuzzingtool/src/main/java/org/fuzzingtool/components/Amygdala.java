package org.fuzzingtool.components;

import com.microsoft.z3.*;
import org.fuzzingtool.Logger;
import org.fuzzingtool.symbolic.Type;
import org.fuzzingtool.tactics.DepthSearchTactic;
import org.fuzzingtool.tactics.FuzzingException;
import org.fuzzingtool.visualization.BranchingVisualizer;

import java.util.ArrayList;
import java.util.HashMap;

public class Amygdala {
    public Tracer tracer;
    public Logger logger;
    public BranchingNode branchingRootNode;
    private BranchingNode currentBranch;
    public Context z3_ctx;
    public HashMap<VariableIdentifier, Object> new_variable_values;
    public boolean fuzzing_finished = false;
    private int fuzzing_iterations = 1;
    private boolean is_first_run = true;

    //Fuzzing Configuration TODO
    public int input_line_num = 22;
    public int global_fuzzing_loop_line_num = 2; // Kann automatisch bestimmt werden
    public String global_fuzzing_loop_identifier = "fuzzing1234"; // Needed if the program happens to be combined of several files

    public Amygdala(Logger lgr) {
        this.tracer = new Tracer();
        this.logger = lgr;
        this.new_variable_values = new HashMap<>();

        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        z3_ctx = new Context(cfg);

        branchingRootNode = new BranchingNode();
        currentBranch = branchingRootNode;

        initialize_sample_inputs();
    }

    public void initialize_sample_inputs() { // TODO
        new_variable_values.put(new VariableIdentifier("n", Type.INT), (Integer) 5);
    }

    public void branching_event(Integer branching_node_hash, BranchingNodeAttribute bt, Integer predicate_interim_key, Boolean taken) {
        // TODO Hier Konsistenz prüfen: Ist dies wirklich der Pfad den ich gehen wollte?
        if (currentBranch.getBranchingNodeAttribute() == BranchingNodeAttribute.BRANCH // "Hier bin ich schonmal gewesen" -> einfach weiter durchlaufen
                || currentBranch.getBranchingNodeAttribute() == BranchingNodeAttribute.LOOP) {
            currentBranch = currentBranch.getChildBranch(taken);
        } else { // unbekannter pfad -> updaten
            currentBranch.setProperties(tracer.get_interim(predicate_interim_key), branching_node_hash, bt);
            currentBranch.initializeChildren();
            currentBranch = currentBranch.getChildBranch(taken);
        }
    }

    public void terminate() {
        currentBranch.setBranchingNodeAttribute(BranchingNodeAttribute.TERMINATE);
        currentBranch = branchingRootNode;
        tracer.clearAll();
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
        DepthSearchTactic tac = new DepthSearchTactic(branchingRootNode, z3_ctx);
        try {
            new_variable_values = tac.getNextValues();
        } catch (FuzzingException.NoMorePaths noMorePaths) {
            fuzzing_finished = true;
            return false;
        }
        return true;
    }

    public Object getNextInputValue(VariableIdentifier var_id) {
        return new_variable_values.get(var_id);
    }

    public String lastRunToSMTExpr() {
        ArrayList<String> exp = currentBranch.getSymbolicPathSMTExpression();
        StringBuilder exp_str = new StringBuilder();
        exp_str.append("(declare-const n Int)\n");
        exp_str.append("(assert\n");
        exp_str.append("\t(and\n");
        for (String str: exp) {
            exp_str.append("\t\t").append(str).append("\n");
        }
        exp_str.append("\t)\n");
        exp_str.append(")");
        return exp_str.toString();
    }

    public String lastRunToHumanReadableExpr() {
        ArrayList<String> exp = currentBranch.getSymbolicPathHRExpression();
        StringBuilder exp_str = new StringBuilder();
        for (String str: exp) {
            exp_str.append(str).append("\n");
        }
        return exp_str.toString();
    }

    public void lastRunToZ3Expr() {
        BoolExpr path_expr = currentBranch.getSymbolicPathZ3Expression(z3_ctx);
        Solver s = z3_ctx.mkSolver();
        s.add(path_expr);
        if (s.check() == Status.SATISFIABLE) {
            Model m = s.getModel();
            logger.log("n = " + m.evaluate(z3_ctx.mkIntConst("n"), false));
        }
        logger.log(path_expr.toString());
    }

    public void visualizeProgramFlow(String path) {
        BranchingVisualizer bv = new BranchingVisualizer(branchingRootNode);
        bv.save_image(path);
    }
}
