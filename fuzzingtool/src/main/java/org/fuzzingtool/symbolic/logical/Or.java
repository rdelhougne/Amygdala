package org.fuzzingtool.symbolic.logical;

import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class Or extends SymbolicNode {
    public Or(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (a.type == Type.BOOLEAN && b.type == Type.BOOLEAN) {
            this.type = Type.BOOLEAN;
            addChildren(2, a, b);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, "OR");
        }
    }

    @Override
    public String toString() {
        return encapsulate(this.children[0].toString() + " || " + this.children[1].toString());
    }

    @Override
    public String toSMTExpr() {
        return encapsulate("or " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    public static Or or(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        return new Or(a, b);
    }
}
