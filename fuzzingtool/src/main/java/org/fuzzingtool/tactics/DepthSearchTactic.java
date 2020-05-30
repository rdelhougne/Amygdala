package org.fuzzingtool.tactics;

import com.microsoft.z3.*;
import org.fuzzingtool.components.BranchingNode;
import org.fuzzingtool.components.BranchingNodeAttribute;
import org.fuzzingtool.components.VariableIdentifier;

import java.util.HashMap;
import java.util.HashSet;

public class DepthSearchTactic extends FuzzingTactic {
    BranchingNode root_node;
    Context ctx;

    public final Integer max_loop_unrolling = 16;
    public final Integer max_depth = 32;

    HashMap<Integer, Integer> loop_unrolls;

    public DepthSearchTactic(BranchingNode n, Context c) {
        this.root_node = n;
        this.ctx = c;
        loop_unrolls = new HashMap<>();
    }

    public HashMap<VariableIdentifier, Object> getNextValues() throws FuzzingException.NoMorePaths {
        boolean path_found = true;
        while (path_found) {
            BranchingNode new_target = find_unexplored(root_node, 1);
            if (new_target == null) {
                path_found = false;
            } else {
                BoolExpr expr = new_target.getSymbolicPathZ3Expression(ctx);
                HashSet<VariableIdentifier> vars = new_target.getSymbolicPathIdentifiers();
                Solver s = ctx.mkSolver();
                s.add(expr);
                if (s.check() == Status.SATISFIABLE) {
                    Model m = s.getModel();
                    HashMap<VariableIdentifier, Object> variable_values = new HashMap<>();
                    for (VariableIdentifier var: vars) {
                        switch (var.getVariableType()) {
                            case BOOLEAN:
                                // TODO
                                break;
                            case INT:
                                IntNum result = (IntNum) m.evaluate(ctx.mkIntConst(var.getIdentifierString()), false);
                                variable_values.put(var, result.getInt());
                                break;
                            case REAL:
                                // TODO
                                break;
                            case STRING:
                                // TODO
                                break;
                            case VOID:
                                // TODO
                                break;
                        }
                    }
                    return variable_values;
                } else {
                    new_target.setBranchingNodeAttribute(BranchingNodeAttribute.UNREACHABLE);
                }
            }
        }
        throw new FuzzingException.NoMorePaths();
    }

    private BranchingNode find_unexplored(BranchingNode current_node, Integer current_depth) {

        //Max depth functionality
        if (current_depth > max_depth) {
            return null;
        }

        // Max loop unrolling functionality
        if (current_node.getBranchingNodeAttribute() == BranchingNodeAttribute.LOOP) {
            increment_loop(current_node.getNodeHash());

            if (over_loop_limit(current_node.getNodeHash())) {
                decrement_loop(current_node.getNodeHash());
                return null;
            }
        }

        BranchingNodeAttribute node_type = current_node.getBranchingNodeAttribute();

        if (node_type == BranchingNodeAttribute.BRANCH || node_type == BranchingNodeAttribute.LOOP) {
            for (Boolean taken_flag : new boolean[]{true, false}) {
                BranchingNode child_node = current_node.getChildBranch(taken_flag);
                BranchingNode child_target_result = find_unexplored(child_node, current_depth + 1);
                if (child_target_result != null) {
                    return child_target_result;
                }
            }
            if (node_type == BranchingNodeAttribute.LOOP) {
                decrement_loop(current_node.getNodeHash());
            }
            return null;
        } else if (node_type == BranchingNodeAttribute.UNKNOWN) {
            return current_node;
        } else {
            return null;
        }
    }

    private void increment_loop(Integer node_hash) {
        if (loop_unrolls.containsKey(node_hash)) {
            loop_unrolls.put(node_hash, loop_unrolls.get(node_hash) + 1);
        } else {
            loop_unrolls.put(node_hash, 1);
        }
    }

    private void decrement_loop(Integer node_hash) {
        loop_unrolls.put(node_hash, loop_unrolls.get(node_hash) - 1);
    }

    private boolean over_loop_limit(Integer node_hash) {
        return loop_unrolls.get(node_hash) > max_loop_unrolling;
    }
}
