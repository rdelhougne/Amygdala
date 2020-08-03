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
import java.util.Queue;

public class BranchingNode {
	/**
	 * State of the branch, BRANCH and LOOP indicate a taken branch,
	 * all others indicate not yet taken or error states
	 */
	private BranchingNodeAttribute branchingNodeAttribute;

	/**
	 * The symbolic expression of this node (and therefore all child-nodes) is
	 * undecidable by the SMT-Solver, e.g. if the expression contains strings
	 * but the solver can't handle string types
	 */
	private boolean isUndecidable = false;

	/**
	 * This node contains an expression that cannot be solved reliably, and thus
	 * the path is diverging at this node. This happens for example if the
	 * expression depends on a random number.
	 */
	private boolean isDiverging = false;

	/**
	 * All possible subsequent paths were already explored
	 */
	private boolean isExplored = false;

	/**
	 * String.hashCode() of an identifier string consisting of <source file uri>:<character start position>:<character end position>
	 */
	private Integer branch_identifier;

	/**
	 * Line number of the expression in the source code (for visualization)
	 */
	private Integer lineNumber;

	/**
	 * String of the expression as in the source-code (for visualization)
	 */
	private String source_code_expression;

	/**
	 * Connection to another BranchingNode, if the expression evaluates to true
	 */
	private BranchingNode childNodeTaken;

	/**
	 * Connection to another BranchingNode, if the expression evaluates to false
	 */
	private BranchingNode childNodeNotTaken;

	/**
	 * Symbolic expression for this node
	 */
	private SymbolicNode symbolic_expression;

	/**
	 * Connection to parent node
	 */
	private BranchingNode parentNode;

	/**
	 * A flag that indicates if the current node is the "childNodeTaken" or
	 * the "childNodeNotTaken" variable of the parent node.
	 */
	private Boolean parentNodeTakenFlag;

	/**
	 * Language semantic of the node
	 */
	private static final LanguageSemantic nodeLanguageSemantic = LanguageSemantic.JAVASCRIPT; //TODO

	public BranchingNode(SymbolicNode exp, Integer node_hash, BranchingNodeAttribute bt) {
		setProperties(exp, node_hash, bt);
		initializeChildren();
	}

	public BranchingNode() {
		this.branch_identifier = 0;
		this.symbolic_expression = null;
		this.branchingNodeAttribute = BranchingNodeAttribute.UNKNOWN;
		this.parentNode = null;
		this.parentNodeTakenFlag = false;
	}

	private BranchingNode(BranchingNode parent, Boolean taken_flag) {
		this.branch_identifier = 0;
		this.symbolic_expression = null;
		this.branchingNodeAttribute = BranchingNodeAttribute.UNKNOWN;
		this.parentNode = parent;
		this.parentNodeTakenFlag = taken_flag;
	}

	public void initializeChildren() {
		this.childNodeTaken = new BranchingNode(this, true);
		this.childNodeNotTaken = new BranchingNode(this, false);
	}

	public void setProperties(SymbolicNode exp, Integer identifier, BranchingNodeAttribute bt) {
		assert exp != null && identifier != null && bt != null;
		this.symbolic_expression = exp;
		this.branchingNodeAttribute = bt;
		this.branch_identifier = identifier;
	}

	public void setBranchingNodeAttribute(BranchingNodeAttribute branchingNodeAttribute) {
		this.branchingNodeAttribute = branchingNodeAttribute;
	}

	public BranchingNodeAttribute getBranchingNodeAttribute() {
		return this.branchingNodeAttribute;
	}

	public void setParent(BranchingNode parent_node, Boolean flag) {
		this.parentNode = parent_node;
		this.parentNodeTakenFlag = flag;
	}

	public BranchingNode getParent() {
		return this.parentNode;
	}

	public void setSourceCodeExpression(String source_code_expression) {
		this.source_code_expression = source_code_expression;
	}

	public boolean isUndecidable() {
		return isUndecidable;
	}

	public void setUndecidable() {
		this.isUndecidable = true;

		if (childNodeTaken != null) {
			childNodeTaken.setUndecidable();
		}
		if (childNodeNotTaken != null) {
			childNodeNotTaken.setUndecidable();
		}
	}

	public boolean isDiverging() {
		return isDiverging;
	}

	public void setDiverging() {
		this.isDiverging = true;
		// do not traverse
	}

	public boolean isExplored() {
		return isExplored;
	}

	public void setExplored() {
		this.isExplored = true;

		if (childNodeTaken != null) {
			childNodeTaken.setExplored();
		}
		if (childNodeNotTaken != null) {
			childNodeNotTaken.setExplored();
		}
	}

	public String getSourceCodeExpression() {
		return this.source_code_expression;
	}

	public void setChildBranch(BranchingNode child_node, Boolean taken) {
		if (taken) {
			this.childNodeTaken = child_node;
		} else {
			this.childNodeNotTaken = child_node;
		}
	}

	public BranchingNode getChildBranch(Boolean taken) {
		if (taken) {
			return this.childNodeTaken;
		} else {
			return this.childNodeNotTaken;
		}
	}

	public Integer getBranchIdentifier() {
		return branch_identifier;
	}

	public ArrayList<String> getSymbolicPathSMTExpression() throws SymbolicException.NotImplemented {
		if (parentNode != null) {
			return this.parentNode.getSymbolicPathSMTExpression(this.parentNodeTakenFlag);
		} else {
			return new ArrayList<>();
		}
	}

	public ArrayList<String> getSymbolicPathSMTExpression(Boolean taken_flag) throws SymbolicException.NotImplemented {
		if (parentNode != null) {
			ArrayList<String> all_from_parents =
					this.parentNode.getSymbolicPathSMTExpression(this.parentNodeTakenFlag);
			all_from_parents.add(getLocalSMTExpression(taken_flag));
			return all_from_parents;
		} else {
			ArrayList<String> my_expr = new ArrayList<>();
			my_expr.add(getLocalSMTExpression(taken_flag));
			return my_expr;
		}
	}

	public ArrayList<String> getSymbolicPathHRExpression() throws SymbolicException.NotImplemented {
		if (parentNode != null) {
			return this.parentNode.getSymbolicPathHRExpression(this.parentNodeTakenFlag);
		} else {
			return new ArrayList<>();
		}
	}

	public ArrayList<String> getSymbolicPathHRExpression(Boolean taken_flag) throws SymbolicException.NotImplemented {
		if (parentNode != null) {
			ArrayList<String> all_from_parents = this.parentNode.getSymbolicPathHRExpression(this.parentNodeTakenFlag);
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
		if (parentNode != null) {
			return this.parentNode.getSymbolicPathZ3Expression(this.parentNodeTakenFlag, ctx);
		} else {
			throw new SymbolicException.NotImplemented("Cannot get expression of root node without hint");
		}
	}

	public BoolExpr getSymbolicPathZ3Expression(Boolean taken_flag, Context ctx) throws
			SymbolicException.UndecidableExpression,
			SymbolicException.NotImplemented {
		if (parentNode != null) {
			BoolExpr all_from_parents = this.parentNode.getSymbolicPathZ3Expression(this.parentNodeTakenFlag, ctx);
			try {
				return ctx.mkAnd(all_from_parents, getLocalZ3Expression(taken_flag, ctx));
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
			SymbolicNode not = new Not(nodeLanguageSemantic, this.symbolic_expression);
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
			SymbolicNode not = new Not(nodeLanguageSemantic, this.symbolic_expression);
			return not.toHRString();
		}
	}

	public BoolExpr getLocalZ3Expression(Boolean taken, Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		if (this.symbolic_expression == null) {
			throw new SymbolicException.UndecidableExpression("Z3", "Local symbolic expression is null.");
		}
		if (taken) {
			Pair<Expr, ExpressionType> expr = this.symbolic_expression.toZ3Expr(ctx);
			assert expr.getRight() == ExpressionType.BOOLEAN;
			return (BoolExpr) expr.getLeft();
		} else {
			SymbolicNode not = new Not(nodeLanguageSemantic, this.symbolic_expression);
			Pair<Expr, ExpressionType> expr = not.toZ3Expr(ctx);
			assert expr.getRight() == ExpressionType.BOOLEAN;
			return (BoolExpr) expr.getLeft();
		}
	}

	public Queue<Pair<Integer, Boolean>> getProgramPath() throws SymbolicException.NotImplemented {
		if (parentNode != null) {
			return this.parentNode.getProgramPath(this.parentNodeTakenFlag);
		} else {
			throw new SymbolicException.NotImplemented("Cannot get path of root node without hint");
		}
	}

	public Queue<Pair<Integer, Boolean>> getProgramPath(Boolean taken_flag) {
		if (parentNode != null) {
			Queue<Pair<Integer, Boolean>> all_from_parents = this.parentNode.getProgramPath(this.parentNodeTakenFlag);
			all_from_parents.offer(Pair.create(this.branch_identifier, taken_flag));
			return all_from_parents;
		} else {
			Queue<Pair<Integer, Boolean>> q = new LinkedList<>();
			q.offer(Pair.create(this.branch_identifier, taken_flag));
			return q;
		}
	}
}
