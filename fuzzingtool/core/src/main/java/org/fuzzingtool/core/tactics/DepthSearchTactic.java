package org.fuzzingtool.core.tactics;

import com.microsoft.z3.*;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.BranchingNode;
import org.fuzzingtool.core.components.BranchingNodeAttribute;
import org.fuzzingtool.core.components.VariableIdentifier;
import org.fuzzingtool.core.symbolic.SymbolicException;

import java.util.HashMap;
import java.util.Map;

public class DepthSearchTactic extends FuzzingTactic {
	private final BranchingNode root_node;
	private final Context ctx;
	private final Logger logger;

	//Options
	private Integer max_loop_unrolling = 16;
	private Integer max_depth = 32;

	private final HashMap<Integer, Integer> loop_unrolls;

	public DepthSearchTactic(BranchingNode n, Context c, Logger l) {
		this.root_node = n;
		this.ctx = c;
		this.logger = l;
		loop_unrolls = new HashMap<>();
	}

	@Override
	public boolean calculate() {
		this.next_values = new HashMap<>();
		Map<VariableIdentifier, Object> new_values = new HashMap<>();
		this.has_next_path = false;
		this.next_path.clear();
		this.loop_unrolls.clear();

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
				}
				/*try {
					for(String s: new_target.getSymbolicPathHRExpression()) {
						logger.log(s);
					}
				} catch (SymbolicException.NotImplemented nie) {
					nie.printStackTrace();
					logger.log(nie.getMessage());
				}*/
				logger.hypnotize(expr.getSExpr());
				Solver s = ctx.mkSolver();
				s.add(expr);
				Status status = s.check();
				logger.shock("Checked.");
				if (status == Status.SATISFIABLE) {
					Model model = s.getModel();
					FuncDecl[] declarations = model.getConstDecls();
					for (FuncDecl d: declarations) {
						String declaration_name = d.getName().toString();
						VariableIdentifier identifier = VariableIdentifier.fromString(declaration_name);
						Expr result = model.getConstInterp(d);

						switch (identifier.getVariableType()) {
							case BOOLEAN:
								if (result.isBool()) {
									new_values.put(identifier, result.isTrue());
								} else {
									logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Bool.");
								}
								break;
							case STRING:
								if (result.isString()) {
									new_values.put(identifier, result.getString());
								} else {
									logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to String.");
								}
								break;
							case BIGINT:
							case NUMBER_INTEGER:
								if (result.isIntNum()) {
									try {
										IntNum cast_result = (IntNum) result;
										new_values.put(identifier, cast_result.getInt());
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
										new_values.put(identifier, Double.parseDouble(cast_result.toDecimalString(128)));
									} catch (ClassCastException cce) {
										logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Double.");
									}
								} else {
									logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Double.");
								}
								break;
							default:
								logger.critical("Variable " + identifier.getIdentifierString() + " has not allowed type '" + identifier.getVariableType().toString() + "'.");
								break;
						}
					}

					try {
						this.next_path = new_target.getProgramPath();
					} catch (SymbolicException.NotImplemented nie) {
						logger.critical("Cannot get program path from new target node:" + nie.getMessage());
						new_target.setBranchingNodeAttribute(BranchingNodeAttribute.UNREACHABLE);
						continue;
					}

					this.next_values = new_values;
					this.has_next_path = true;
					return true;
				} else if (status == Status.UNSATISFIABLE){
					new_target.setBranchingNodeAttribute(BranchingNodeAttribute.UNREACHABLE);
				} else {
					new_target.setBranchingNodeAttribute(BranchingNodeAttribute.UNREACHABLE);
					logger.info("Satisfiability of expression unknown, reason: " + s.getReasonUnknown());
					logger.alert(s.getStatistics().toString());
				}
				logger.shock("Before.");
				s.reset();
				logger.shock("End.");
			}
		}
		logger.shock("Super-End.");
		return false;
	}

	private BranchingNode find_unexplored(BranchingNode current_node, Integer current_depth) {

		//Max depth functionality
		if (current_depth > max_depth) {
			return null;
		}

		// Max loop unrolling functionality
		if (current_node.getBranchingNodeAttribute() == BranchingNodeAttribute.LOOP) {
			increment_loop(current_node.getBranchIdentifier());

			if (over_loop_limit(current_node.getBranchIdentifier())) {
				decrement_loop(current_node.getBranchIdentifier());
				return null;
			}
		}

		if (current_node.isUndecidable()) {
			return null;
		}

		if (current_node.isDiverging()) {
			return null;
		}

		if (current_node.isExplored()) {
			return null;
		}

		BranchingNodeAttribute node_type = current_node.getBranchingNodeAttribute();

		if (node_type == BranchingNodeAttribute.BRANCH || node_type == BranchingNodeAttribute.LOOP) {
			for (Boolean taken_flag: new boolean[]{true, false}) {
				BranchingNode child_node = current_node.getChildBranch(taken_flag);
				BranchingNode child_target_result = find_unexplored(child_node, current_depth + 1);
				if (child_target_result != null) {
					return child_target_result;
				}
			}
			if (node_type == BranchingNodeAttribute.LOOP) {
				decrement_loop(current_node.getBranchIdentifier());
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
