package org.fuzzingtool.components;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.logical.Not;
import org.graalvm.collections.Pair;

import java.util.ArrayList;

public class BranchingNode {
    /**
     * State of the branch, BRANCH and LOOP indicate a taken branch, all others indicate not yet taken or error states
     */
    private BranchingNodeAttribute branchingNodeAttribute;

    /**
     * The symbolic expression of this node (and therefore all child-nodes) is undecidable by the SMT-Solver, e.g. if the expression contains strings but the solver can't handle string types
     */
    private boolean isUndecidable = false;

    /**
     * All possible subsequent paths were already explored
     */
    private boolean isExplored = false;

    /**
     * hash-code of the corresponding TruffleNode
     */
    private Integer nodeHash;

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
        this.nodeHash = 0;
        this.symbolic_expression = null;
        this.branchingNodeAttribute = BranchingNodeAttribute.UNKNOWN;
        this.parentNode = null;
        this.parentNodeTakenFlag = false;
    }

    private BranchingNode(BranchingNode parent, Boolean taken_flag) {
        this.nodeHash = 0;
        this.symbolic_expression = null;
        this.branchingNodeAttribute = BranchingNodeAttribute.UNKNOWN;
        this.parentNode = parent;
        this.parentNodeTakenFlag = taken_flag;
    }

    public void initializeChildren() {
        this.childNodeTaken = new BranchingNode(this, true);
        this.childNodeNotTaken = new BranchingNode(this, false);
    }

    public void setProperties(SymbolicNode exp, Integer node_hash, BranchingNodeAttribute bt) {
        assert exp != null && node_hash != null && bt != null;
        this.symbolic_expression = exp;
        this.branchingNodeAttribute = bt;
        this.nodeHash = node_hash;
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

    public Integer getNodeHash() {
        return nodeHash;
    }

    /*public HashSet<VariableIdentifier> getSymbolicPathIdentifiers() {
        if (parentNode != null) {
            HashSet<VariableIdentifier> all_from_parents = this.parentNode.getSymbolicPathIdentifiers();
            if (this.branchingNodeAttribute == BranchingNodeAttribute.BRANCH || this.branchingNodeAttribute == BranchingNodeAttribute.LOOP) {
                all_from_parents.addAll(this.symbolic_expression.getSymbolicVars());
            }
            return all_from_parents;
        } else {
            return this.symbolic_expression.getSymbolicVars();
        }
    }*/

    public ArrayList<String> getSymbolicPathSMTExpression() {
        if (parentNode != null) {
            return this.parentNode.getSymbolicPathSMTExpression(this.parentNodeTakenFlag);
        } else {
            return new ArrayList<>();
        }
    }

    public ArrayList<String> getSymbolicPathSMTExpression(Boolean taken_flag) {
        try {
            if (parentNode != null) {
                ArrayList<String> all_from_parents = this.parentNode.getSymbolicPathSMTExpression(this.parentNodeTakenFlag);
                all_from_parents.add(getLocalSMTExpression(taken_flag));
                return all_from_parents;
            } else {
                ArrayList<String> my_expr = new ArrayList<>();
                my_expr.add(getLocalSMTExpression(taken_flag));
                return my_expr;
            }
        } catch (SymbolicException.WrongParameterSize | SymbolicException.NotImplemented ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public ArrayList<String> getSymbolicPathHRExpression() throws SymbolicException.WrongParameterSize {
        if (parentNode != null) {
            return this.parentNode.getSymbolicPathHRExpression(this.parentNodeTakenFlag);
        } else {
            return new ArrayList<>();
        }
    }

    public ArrayList<String> getSymbolicPathHRExpression(Boolean taken_flag) throws SymbolicException.WrongParameterSize {
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

    public BoolExpr getSymbolicPathZ3Expression(Context ctx) throws SymbolicException.WrongParameterSize, SymbolicException.NotImplemented, SymbolicException.UndecidableExpression {
        if (parentNode != null) {
            return this.parentNode.getSymbolicPathZ3Expression(this.parentNodeTakenFlag, ctx);
        } else {
            throw new SymbolicException.NotImplemented("Cannot get expression of root node without hint");
        }
    }

    public BoolExpr getSymbolicPathZ3Expression(Boolean taken_flag, Context ctx) throws SymbolicException.WrongParameterSize, SymbolicException.UndecidableExpression, SymbolicException.NotImplemented {
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

    public String getLocalSMTExpression(Boolean taken) throws SymbolicException.WrongParameterSize, SymbolicException.NotImplemented {
        if (taken) {
            return this.symbolic_expression.toSMTExpr();
        } else {
            SymbolicNode not = new Not(nodeLanguageSemantic, this.symbolic_expression);
            return not.toSMTExpr();
        }
    }

    public String getLocalHRExpression(Boolean taken) throws SymbolicException.WrongParameterSize {
        if (taken) {
            return this.symbolic_expression.toString();
        } else {
            SymbolicNode not = new Not(nodeLanguageSemantic, this.symbolic_expression);
            return not.toString();
        }
    }

    public BoolExpr getLocalZ3Expression(Boolean taken, Context ctx) throws SymbolicException.WrongParameterSize, SymbolicException.NotImplemented, SymbolicException.UndecidableExpression {
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
}
