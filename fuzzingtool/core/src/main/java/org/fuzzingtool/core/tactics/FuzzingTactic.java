package org.fuzzingtool.core.tactics;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.RatNum;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.BranchingNode;
import org.fuzzingtool.core.components.BranchingNodeAttribute;
import org.fuzzingtool.core.components.TimeProbe;
import org.fuzzingtool.core.components.VariableIdentifier;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.graalvm.collections.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public abstract class FuzzingTactic {
	protected Context ctx = null;
	protected Logger logger = null;
	protected TimeProbe probe = null;

	// Results
	protected boolean has_next_path = false;
	protected Map<VariableIdentifier, Object> next_values = new HashMap<>();
	protected Queue<Pair<Integer, Boolean>> next_path = new LinkedList<>();

	//Options
	protected Integer max_loop_unrolling = 16;
	protected Integer max_depth = 32;

	protected HashMap<Integer, Integer> loop_unrolls = new HashMap<>();

	/**
	 * Set an option value.
	 *
	 * @param option_name name of the option
	 * @param value new option value
	 */
	public abstract void setOption(String option_name, Object value);

	/**
	 * Returns tactic options.
	 *
	 * @param option_name name of the option
	 * @return value for given option
	 */
	public abstract Object getOption(String option_name);

	public abstract String getTactic();

	protected abstract BranchingNode findUnexplored();

	/**
	 * Calculates the next program path.
	 *
	 * @return true, if a path is found, false otherwise
	 */
	public final boolean calculate() {
		this.next_values = new HashMap<>();
		Map<VariableIdentifier, Object> new_values = new HashMap<>();
		this.has_next_path = false;
		this.next_path.clear();

		boolean path_found = true;
		while (path_found) {
			BranchingNode new_target = findUnexplored();
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
				Solver s = ctx.mkSolver();
				s.add(expr);
				probe.switchState(TimeProbe.ProgramState.SOLVE);
				Status status = s.check();
				probe.switchState(TimeProbe.ProgramState.TACTIC);
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
									logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Bool");
								}
								break;
							case STRING:
								if (result.isString()) {
									new_values.put(identifier, result.getString());
								} else {
									logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to String");
								}
								break;
							case BIGINT:
							case NUMBER_INTEGER:
								if (result.isIntNum()) {
									try {
										IntNum cast_result = (IntNum) result;
										new_values.put(identifier, cast_result.getInt());
									} catch (ClassCastException cce) {
										logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Integer");
									}
								} else {
									logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Integer");
								}
								break;
							case NUMBER_REAL: //TODO Z3 RatNum to Double conversion
								if (result.isRatNum()) {
									try {
										RatNum cast_result = (RatNum) result;
										new_values.put(identifier, Double.parseDouble(cast_result.toDecimalString(128)));
									} catch (ClassCastException cce) {
										logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Double");
									}
								} else {
									logger.critical("Cannot cast Z3 Expression '" + result.toString() + "' to Double");
								}
								break;
							default:
								logger.critical("Variable " + identifier.getIdentifierString() + " has not allowed type '" + identifier.getVariableType().toString() + "'");
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
				}
				s.reset();
			}
		}
		return false;
	}

	protected boolean nodeIsInvalid(BranchingNode node) {
		// Max depth functionality
		if (node.getDepth() > this.max_depth) {
			return true;
		}

		// Max loop unrolling functionality
		if (overLoopLimit(node)) {
			return true;
		}

		// Other characteristics
		return node.isUndecidable() || node.isDiverging() || node.isExplored();
	}

	protected void incrementLoop(BranchingNode node) {
		if (node.getBranchingNodeAttribute() == BranchingNodeAttribute.LOOP) {
			Integer node_hash = node.getBranchIdentifier();
			if (loop_unrolls.containsKey(node_hash)) {
				loop_unrolls.put(node_hash, loop_unrolls.get(node_hash) + 1);
			} else {
				loop_unrolls.put(node_hash, 1);
			}
		}
	}

	protected void decrementLoop(BranchingNode node) {
		if (node.getBranchingNodeAttribute() == BranchingNodeAttribute.LOOP) {
			Integer node_hash = node.getBranchIdentifier();
			loop_unrolls.put(node_hash, loop_unrolls.get(node_hash) - 1);
		}
	}

	protected boolean overLoopLimit(BranchingNode node) {
		if (node.getBranchingNodeAttribute() == BranchingNodeAttribute.LOOP) {
			Integer node_hash = node.getBranchIdentifier();
			return this.loop_unrolls.get(node_hash) > this.max_loop_unrolling;
		} else {
			return false;
		}
	}

	/**
	 * Check if a path is found and the parameters exist.
	 *
	 * @return true, if a unexplored path is found, false otherwise
	 */
	public boolean hasNextPath() {
		return has_next_path;
	}

	/**
	 * Returns the new variable-value-map.
	 *
	 * @return values for the variables, empty if {@link #hasNextPath()} returns false
	 */
	public Map<VariableIdentifier, Object> getNextValues() {
		return next_values;
	}

	/**
	 * Returns the new program path as a queue.
	 *
	 * @return new program path, empty if {@link #hasNextPath()} returns false
	 */
	public Queue<Pair<Integer, Boolean>> getNextPath() {
		return next_path;
	}

	/**
	 * Set the time probe
	 *
	 * @param tp The time probe object
	 */
	public void setTimeProbe(TimeProbe tp) {
		this.probe = tp;
	}
}
