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

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.fuzzingtool.core.symbolic.logical.Not;
import org.graalvm.collections.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class BranchingNode {
	/**
	 * State of the branch, BRANCH and LOOP indicate a taken branch,
	 * all others indicate not yet taken or error states
	 */
	private BranchingNodeAttribute branching_node_attribute;

	/**
	 * The symbolic expression of this node (and therefore all child-nodes) is
	 * undecidable by the SMT-Solver, e.g. if the expression contains strings
	 * but the solver can't handle string types
	 */
	private boolean is_undecidable = false;

	/**
	 * This node contains an expression that cannot be solved reliably, and thus
	 * the path is diverging at this node. This happens for example if the
	 * expression depends on a random number.
	 */
	private boolean is_diverging = false;

	/**
	 * All possible subsequent paths were already explored
	 */
	private boolean is_explored = false;

	/**
	 * String.hashCode() of an identifier string consisting of <source file uri>:<character start position>:<character end position>
	 */
	private Integer branch_identifier;

	/**
	 * Line number of the expression in the source code (for visualization)
	 */
	private Integer line_number;

	/**
	 * String of the expression as in the source-code (for visualization)
	 */
	private String source_code_expression;

	/**
	 * Connection to another BranchingNode, if the expression evaluates to true
	 */
	private BranchingNode child_node_taken;

	/**
	 * Connection to another BranchingNode, if the expression evaluates to false
	 */
	private BranchingNode child_node_not_taken;

	/**
	 * Symbolic expression for this node
	 */
	private SymbolicNode symbolic_expression;

	/**
	 * Connection to parent node
	 */
	private BranchingNode parent_node;

	/**
	 * A flag that indicates if the current node is the "childNodeTaken" or
	 * the "childNodeNotTaken" variable of the parent node.
	 */
	private Boolean parent_node_taken_flag;

	/**
	 * An Integer, representing the depth of the node in the tree.
	 */
	private final Integer depth;

	/**
	 * Language semantic of the node
	 */
	private static final LanguageSemantic node_language_semantic = LanguageSemantic.JAVASCRIPT; //TODO

	/**
	 * Caching mechanisms
	 */
	private BoolExpr cached_z3_expression = null;
	private Queue<Pair<Integer, Boolean>> cached_program_path = null;
	private ArrayList<String> cached_hr_string = null;
	private ArrayList<String> cached_smt_expression = null;

	public BranchingNode() {
		this.branch_identifier = 0;
		this.symbolic_expression = null;
		this.branching_node_attribute = BranchingNodeAttribute.UNKNOWN;
		this.parent_node = null;
		this.parent_node_taken_flag = false;
		this.depth = 0;
	}

	private BranchingNode(BranchingNode parent, Boolean taken_flag) {
		this.branch_identifier = 0;
		this.symbolic_expression = null;
		this.branching_node_attribute = BranchingNodeAttribute.UNKNOWN;
		this.parent_node = parent;
		this.parent_node_taken_flag = taken_flag;
		this.depth = parent.getDepth() + 1;
	}

	public void initializeChildren() {
		this.child_node_taken = new BranchingNode(this, true);
		this.child_node_not_taken = new BranchingNode(this, false);
	}

	public void setProperties(SymbolicNode exp, Integer identifier, BranchingNodeAttribute bt) {
		assert exp != null && identifier != null && bt != null;
		this.symbolic_expression = exp;
		this.branching_node_attribute = bt;
		this.branch_identifier = identifier;
	}

	public void setBranchingNodeAttribute(BranchingNodeAttribute branchingNodeAttribute) {
		this.branching_node_attribute = branchingNodeAttribute;
	}

	public BranchingNodeAttribute getBranchingNodeAttribute() {
		return this.branching_node_attribute;
	}

	public void setParent(BranchingNode parent_node, Boolean flag) {
		this.parent_node = parent_node;
		this.parent_node_taken_flag = flag;
	}

	public BranchingNode getParent() {
		return this.parent_node;
	}

	public Integer getDepth() {
		return this.depth;
	}

	public void setSourceCodeExpression(String source_code_expression) {
		this.source_code_expression = source_code_expression;
	}

	public boolean isUndecidable() {
		return is_undecidable;
	}

	public void setUndecidable() {
		this.is_undecidable = true;

		if (child_node_taken != null) {
			child_node_taken.setUndecidable();
		}
		if (child_node_not_taken != null) {
			child_node_not_taken.setUndecidable();
		}
	}

	public boolean isDiverging() {
		return is_diverging;
	}

	public void setDiverging() {
		this.is_diverging = true;
		// do not traverse
	}

	public boolean isExplored() {
		return is_explored;
	}

	public void setExplored() {
		this.is_explored = true;

		if (child_node_taken != null) {
			child_node_taken.setExplored();
		}
		if (child_node_not_taken != null) {
			child_node_not_taken.setExplored();
		}
	}

	public String getSourceCodeExpression() {
		return this.source_code_expression;
	}

	public void setChildBranch(BranchingNode child_node, Boolean taken) {
		if (taken) {
			this.child_node_taken = child_node;
		} else {
			this.child_node_not_taken = child_node;
		}
	}

	public BranchingNode getChildBranch(Boolean taken) {
		if (taken) {
			return this.child_node_taken;
		} else {
			return this.child_node_not_taken;
		}
	}

	public Integer getBranchIdentifier() {
		return branch_identifier;
	}

	public ArrayList<String> getSymbolicPathSMTExpression() throws SymbolicException.NotImplemented {
		if (parent_node != null) {
			if (this.cached_smt_expression == null) {
				this.cached_smt_expression = this.parent_node.getSymbolicPathSMTExpression(this.parent_node_taken_flag);
			}
			return new ArrayList<>(this.cached_smt_expression);
		} else {
			return new ArrayList<>();
		}
	}

	public ArrayList<String> getSymbolicPathSMTExpression(Boolean taken_flag) throws SymbolicException.NotImplemented {
		if (parent_node != null) {
			if (this.cached_smt_expression == null) {
				this.cached_smt_expression = this.parent_node.getSymbolicPathSMTExpression(this.parent_node_taken_flag);
			}
			ArrayList<String> all_from_parents = new ArrayList<>(this.cached_smt_expression);
			all_from_parents.add(getLocalSMTExpression(taken_flag));
			return all_from_parents;
		} else {
			ArrayList<String> my_expr = new ArrayList<>();
			my_expr.add(getLocalSMTExpression(taken_flag));
			return my_expr;
		}
	}

	public ArrayList<String> getSymbolicPathHRExpression() throws SymbolicException.NotImplemented {
		if (parent_node != null) {
			if (this.cached_hr_string == null) {
				this.cached_hr_string = this.parent_node.getSymbolicPathHRExpression(this.parent_node_taken_flag);
			}
			return new ArrayList<>(this.cached_hr_string);
		} else {
			return new ArrayList<>();
		}
	}

	public ArrayList<String> getSymbolicPathHRExpression(Boolean taken_flag) throws SymbolicException.NotImplemented {
		if (parent_node != null) {
			if (this.cached_hr_string == null) {
				this.cached_hr_string = this.parent_node.getSymbolicPathHRExpression(this.parent_node_taken_flag);
			}
			ArrayList<String> all_from_parents = new ArrayList<>(this.cached_hr_string);
			all_from_parents.add(getLocalHRExpression(taken_flag));
			return all_from_parents;
		} else {
			ArrayList<String> my_expr = new ArrayList<>();
			my_expr.add(getLocalHRExpression(taken_flag));
			return my_expr;
		}
	}

	public BoolExpr getSymbolicPathZ3Expression(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		if (parent_node != null) {
			if (this.cached_z3_expression == null) {
				this.cached_z3_expression = this.parent_node.getSymbolicPathZ3Expression(this.parent_node_taken_flag, ctx);
			}
			return this.cached_z3_expression;
		} else {
			throw new SymbolicException.NotImplemented("Cannot get expression of root node without hint");
		}
	}

	public BoolExpr getSymbolicPathZ3Expression(Boolean taken_flag, Context ctx) throws
			SymbolicException.UndecidableExpression,
			SymbolicException.NotImplemented {
		if (parent_node != null) {
			if (this.cached_z3_expression == null) {
				this.cached_z3_expression = this.parent_node.getSymbolicPathZ3Expression(this.parent_node_taken_flag, ctx);
			}
			try {
				return ctx.mkAnd(this.cached_z3_expression, getLocalZ3Expression(taken_flag, ctx));
			} catch (SymbolicException.NotImplemented | SymbolicException.UndecidableExpression ex) {
				this.setUndecidable();
				throw ex;
			}
		} else {
			try {
				return getLocalZ3Expression(taken_flag, ctx);
			} catch (SymbolicException.NotImplemented | SymbolicException.UndecidableExpression ex) {
				this.setUndecidable();
				throw ex;
			}
		}
	}

	public String getLocalSMTExpression(Boolean taken) throws SymbolicException.NotImplemented {
		if (this.symbolic_expression == null) {
			return "!ERROR!";
		}
		if (taken) {
			return this.symbolic_expression.toSMTExpr();
		} else {
			SymbolicNode not = new Not(node_language_semantic, this.symbolic_expression);
			return not.toSMTExpr();
		}
	}

	public String getLocalHRExpression(Boolean taken) throws SymbolicException.NotImplemented {
		if (this.symbolic_expression == null) {
			return "!ERROR!";
		}
		if (taken) {
			return this.symbolic_expression.toHRString();
		} else {
			SymbolicNode not = new Not(node_language_semantic, this.symbolic_expression);
			return not.toHRString();
		}
	}

	public BoolExpr getLocalZ3Expression(Boolean taken, Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		if (this.symbolic_expression == null) {
			throw new SymbolicException.UndecidableExpression("Z3", "Local symbolic expression is null");
		}
		if (taken) {
			Pair<Expr, ExpressionType> expr = SymbolicNode.toBooleanZ3JS(ctx, this.symbolic_expression.toZ3Expr(ctx));
			return (BoolExpr) expr.getLeft();
		} else {
			SymbolicNode not = new Not(node_language_semantic, this.symbolic_expression);
			Pair<Expr, ExpressionType> expr = not.toZ3Expr(ctx);
			return (BoolExpr) expr.getLeft();
		}
	}

	public Queue<Pair<Integer, Boolean>> getProgramPath() throws SymbolicException.NotImplemented {
		if (parent_node != null) {
			if (this.cached_program_path == null) {
				this.cached_program_path = this.parent_node.getProgramPath(this.parent_node_taken_flag);
			}
			return new LinkedList<>(this.cached_program_path);
		} else {
			throw new SymbolicException.NotImplemented("Cannot get path of root node without hint");
		}
	}

	public Queue<Pair<Integer, Boolean>> getProgramPath(Boolean taken_flag) {
		if (parent_node != null) {
			if (this.cached_program_path == null) {
				this.cached_program_path = this.parent_node.getProgramPath(this.parent_node_taken_flag);
			}
			Queue<Pair<Integer, Boolean>> q = new LinkedList<>(this.cached_program_path);
			q.offer(Pair.create(this.branch_identifier, taken_flag));
			return q;
		} else {
			Queue<Pair<Integer, Boolean>> q = new LinkedList<>();
			q.offer(Pair.create(this.branch_identifier, taken_flag));
			return q;
		}
	}

	/**
	 * Returns the height of the tree (the depth of it's deepest node).
	 *
	 * @return The height of the tree
	 */
	public int getTreeHeight() {
		if (child_node_taken == null || child_node_not_taken == null) {
			return depth;
		} else {
			int depth_taken = child_node_taken.getTreeHeight();
			int depth_not_taken = child_node_not_taken.getTreeHeight();
			return Math.max(depth_taken, depth_not_taken);
		}
	}

	/**
	 * Returns number of nodes in the tree.
	 *
	 * @return Number of nodes
	 */
	public int getTreeSize() {
		if (child_node_taken == null || child_node_not_taken == null) {
			return 1;
		} else {
			int size_taken = child_node_taken.getTreeSize();
			int size_not_taken = child_node_not_taken.getTreeSize();
			return size_taken + size_not_taken + 1;
		}
	}

	/**
	 * Returns the number of nodes of a specific type (BranchingNodeAttribute)
	 *
	 * @param out The Map the numbers are written to
	 */
	public void getComponents(Map<BranchingNodeAttribute, Integer> out) {
		out.put(branching_node_attribute, out.get(branching_node_attribute) + 1);
		if (branching_node_attribute == BranchingNodeAttribute.BRANCH || branching_node_attribute == BranchingNodeAttribute.LOOP) {
			child_node_taken.getComponents(out);
			child_node_not_taken.getComponents(out);
		}
	}
}
