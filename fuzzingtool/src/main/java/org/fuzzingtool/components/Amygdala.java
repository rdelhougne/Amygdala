package org.fuzzingtool.components;

import org.fuzzingtool.Logger;
import org.fuzzingtool.visualization.BranchingVisualizer;

import java.util.ArrayList;

public class Amygdala {
    public Tracer tracer;
    public Logger logger;
    public BranchingNode branchingRootNode;
    private BranchingNode currentBranch;

    public Amygdala(Logger lgr) {
        this.tracer = new Tracer();
        this.logger = lgr;
    }

    public void branching_event(Integer branching_node_hash, BranchingNodeAttribute bt, Integer predicate_interim_key, Boolean taken) {
        boolean update_existing_path = false; // TODO
        if (update_existing_path) {

        } else {
            if (branchingRootNode == null) {
                branchingRootNode = new BranchingNode(tracer.get_interim(predicate_interim_key), branching_node_hash, bt);
                currentBranch = branchingRootNode.getChildBranch(taken);
            } else {
                currentBranch.setProperties(tracer.get_interim(predicate_interim_key), branching_node_hash, bt);
                currentBranch.initializeChildren();
                currentBranch = currentBranch.getChildBranch(taken);
            }
        }
    }

    public void terminate() {
        currentBranch.setBranchingNodeAttribute(BranchingNodeAttribute.TERMINATE);
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

    public void visualizeProgramFlow(String path) {
        BranchingVisualizer bv = new BranchingVisualizer(branchingRootNode);
        bv.save_image(path);
    }
}
