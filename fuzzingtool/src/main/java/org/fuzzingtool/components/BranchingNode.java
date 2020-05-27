package org.fuzzingtool.components;

import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.logical.Not;

import java.util.ArrayList;

public class BranchingNode {
    /**
     * State of the branch, BRANCH and LOOP indicate a taken branch, all others indicate not yet taken or error states
     */
    private BranchingNodeAttribute branchingNodeAttribute;

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

    public BranchingNode(SymbolicNode exp, Integer node_hash, BranchingNodeAttribute bt) {
        setProperties(exp, node_hash, bt);
        initializeChildren();
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

    public void setParent(BranchingNode pnode, Boolean flag) {
        this.parentNode = pnode;
        this.parentNodeTakenFlag = flag;
    }

    public BranchingNode getParent() {
        return this.parentNode;
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
        } catch (SymbolicException.IncompatibleType | SymbolicException.WrongParameterSize incompatibleType) {
            incompatibleType.printStackTrace();
        }
        return null;
    }

    public ArrayList<String> getSymbolicPathHRExpression() {
        if (parentNode != null) {
            return this.parentNode.getSymbolicPathHRExpression(this.parentNodeTakenFlag);
        } else {
            return new ArrayList<>();
        }
    }

    public ArrayList<String> getSymbolicPathHRExpression(Boolean taken_flag) {
        try {
            if (parentNode != null) {
                ArrayList<String> all_from_parents = this.parentNode.getSymbolicPathHRExpression(this.parentNodeTakenFlag);
                all_from_parents.add(getLocalHRExpression(taken_flag));
                return all_from_parents;
            } else {
                ArrayList<String> my_expr = new ArrayList<>();
                my_expr.add(getLocalHRExpression(taken_flag));
                return my_expr;
            }
        } catch (SymbolicException.IncompatibleType | SymbolicException.WrongParameterSize incompatibleType) {
            incompatibleType.printStackTrace();
        }
        return null;
    }

    public String getLocalSMTExpression(Boolean taken) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (taken) {
            return this.symbolic_expression.toSMTExpr();
        } else {
            SymbolicNode not = new Not(this.symbolic_expression);
            return not.toSMTExpr();
        }
    }

    public String getLocalHRExpression(Boolean taken) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (taken) {
            return this.symbolic_expression.toString();
        } else {
            SymbolicNode not = new Not(this.symbolic_expression);
            return not.toString();
        }
    }
}
