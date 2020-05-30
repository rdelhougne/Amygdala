package org.fuzzingtool.symbolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.components.VariableIdentifier;

import java.util.HashSet;

public abstract class SymbolicNode {
    protected SymbolicNode[] children;
    public Type type;

    public abstract String toString();
    public abstract String toSMTExpr();

    /**
     * This function returns the Z3-Solver datastructure for the given node
     * and the given Z3-Context
     *
     * @param ctx The context to create the sybolic datatypes
     * @return A Pair: first slot is the complete symbolic datastructure, second slot is a HashMap which contains the Identification of all symbolic variables in the datastructure and their corresponding z3-object.
     */
    public abstract Expr toZ3Expr(Context ctx);

    public HashSet<VariableIdentifier> getSymbolicVars() {
        HashSet<VariableIdentifier> symset = new HashSet<>();
        if (this.children != null) {
            for (SymbolicNode cn : this.children) {
                symset.addAll(cn.getSymbolicVars());
            }
        }
        return symset;
    }

    public void addChildren(int expected, SymbolicNode... nodes) throws SymbolicException.WrongParameterSize {
        if (nodes.length != expected) {
            throw new SymbolicException.WrongParameterSize(nodes.length, 2, this.getClass().getSimpleName());
        } else {
            this.children = new SymbolicNode[expected];
            System.arraycopy(nodes, 0, this.children, 0, nodes.length);
        }
    }

    /**
     * Checks if all the children have the same type check_type.
     *
     * @param check_type The type to check
     * @return True, if all the children have type check_type, false otherwise
     */
    public boolean allChildrenType(Type check_type) {
        for (SymbolicNode cn: this.children) {
            if (cn.type != check_type) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all the children have one of the types provided.
     * The children can have different types.
     *
     * @param check_type List of types to check
     * @return True if all children have one of the provided types
     */
    public boolean allChildrenTypeOr(Type... check_type) {
        for (SymbolicNode cn: this.children) {
            boolean found = false;
            for (Type t: check_type) {
                if (cn.type == t) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encloses a string in parentheses.
     *
     * @param inner String to be enclosed
     * @return "(" + inner + ")"
     */
    protected String parentheses(String inner) {
        return "(" + inner + ")";
    }
}
