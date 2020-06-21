package org.fuzzingtool.tactics;

import com.microsoft.z3.*;
import org.fuzzingtool.Logger;
import org.fuzzingtool.components.BranchingNode;
import org.fuzzingtool.components.BranchingNodeAttribute;
import org.fuzzingtool.components.VariableIdentifier;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.SymbolicException;

import java.util.HashMap;

public class DepthSearchTactic extends FuzzingTactic {
    private final BranchingNode root_node;
    private final Context ctx;
    private final Logger logger;

    //Options
    private Integer max_loop_unrolling = 16;
    private Integer max_depth = 32;

    private HashMap<Integer, Integer> loop_unrolls;

    public DepthSearchTactic(BranchingNode n, Context c, Logger l) {
        this.root_node = n;
        this.ctx = c;
        this.logger = l;
        loop_unrolls = new HashMap<>();
    }

    @Override
    public HashMap<VariableIdentifier, Object> getNextValues(HashMap<VariableIdentifier, ExpressionType> variable_types) throws FuzzingException.NoMorePaths {
        boolean path_found = true;
        while (path_found) {
            BranchingNode new_target = find_unexplored(root_node, 1);
            if (new_target == null) {
                path_found = false;
            } else {
                BoolExpr expr;
                try {
                    expr = new_target.getSymbolicPathZ3Expression(ctx);
                } catch (SymbolicException.NotImplemented ni) {
                    logger.warning(ni.getMessage());
                    continue;
                } catch (SymbolicException.UndecidableExpression ue) {
                    logger.info(ue.getMessage());
                    continue;
                } catch (SymbolicException.WrongParameterSize wrongParameterSize) {
                    wrongParameterSize.printStackTrace();
                    continue;
                }
                Solver s = ctx.mkSolver();
                s.add(expr);
                if (s.check() == Status.SATISFIABLE) {
                    Model model = s.getModel();
                    FuncDecl[] declarations = model.getConstDecls();
                    HashMap<VariableIdentifier, Object> variable_values = new HashMap<>();
                    for (FuncDecl d: declarations) {
                        String declaration_name = d.getName().toString();
                        VariableIdentifier identifier = VariableIdentifier.fromString(declaration_name);
                        Expr result = model.getConstInterp(d);

                        if (variable_types.containsKey(identifier)) {
                            switch (variable_types.get(identifier)) {
                                case BOOLEAN:
                                    if (result.isBool()) {
                                        variable_values.put(identifier, result.isTrue());
                                    } else {
                                        logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Bool.");
                                    }
                                    break;
                                case STRING:
                                    if (result.isString()) {
                                        variable_values.put(identifier, result.getString());
                                    } else {
                                        logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to String.");
                                    }
                                    break;
                                case BIGINT:
                                case NUMBER_INTEGER:
                                    if (result.isIntNum()) {
                                        try {
                                            IntNum cast_result = (IntNum) result;
                                            variable_values.put(identifier, cast_result.getInt());
                                        } catch (ClassCastException cce) {
                                            logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Integer.");
                                        }
                                    } else {
                                        logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Integer.");
                                    }
                                    break;
                                case NUMBER_REAL: //TODO Z3 RatNum to Double conversion
                                    if (result.isRatNum()) {
                                        try {
                                            RatNum cast_result = (RatNum) result;
                                            variable_values.put(identifier, Double.parseDouble(cast_result.toDecimalString(128)));
                                        } catch (ClassCastException cce) {
                                            logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Double.");
                                        }
                                    } else {
                                        logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Double.");
                                    }
                                    break;
                                default:
                                    logger.critical("Variable " + identifier.getIdentifierString() + " has not allowed type '" + variable_types.get(identifier).toString() + "'.");
                                    break;
                            }
                        } else {
                            logger.critical("No type for variable: " + identifier.getIdentifierString());
                        }
                    }
                    this.loop_unrolls.clear();
                    return variable_values;
                } else {
                    new_target.setBranchingNodeAttribute(BranchingNodeAttribute.UNREACHABLE);
                }
            }
        }
        this.loop_unrolls.clear();
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

        if (current_node.isUndecidable()) {
            return null;
        }

        if (current_node.isExplored()) {
            return null;
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

    @Override
    public void setOption(String option_name, Object value) {
        switch (option_name) {
            case "max_loop_unrolling":
                try {
                    this.max_loop_unrolling = (Integer) value;
                    logger.info("DEPTH_SEARCH.max_loop_unrolling option set: " + value);
                } catch (ClassCastException cce) {
                    logger.warning("DepthSearchTactic: Wrong parameter type for option 'max_loop_unrolling' (Integer).");
                }
                break;
            case "max_depth":
                try {
                    this.max_depth = (Integer) value;
                    logger.info("DEPTH_SEARCH.max_depth option set: " + value);
                } catch (ClassCastException cce) {
                    logger.warning("DepthSearchTactic: Wrong parameter type for option 'max_depth' (Integer).");
                }
                break;
            default:
                logger.warning("DEPTH_SEARCH: Unknown option '" + option_name + "'.");
        }
    }

    @Override
    public Object getOption(String option_name) {
        switch (option_name) {
            case "max_loop_unrolling":
                return this.max_loop_unrolling;
            case "max_depth":
                return this.max_depth;
            default:
                logger.warning("DepthSearchTactic: Unknown option '" + option_name + "'.");
                return null;
        }
    }

    @Override
    public String getTactic() {
        return "DEPTH_SEARCH";
    }
}
