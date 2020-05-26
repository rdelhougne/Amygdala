package org.fuzzingtool.symbolic.logical;

import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class Not extends SymbolicNode {
    public Not(SymbolicNode a) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (a.type == Type.BOOLEAN) {
            this.type = Type.BOOLEAN;
            addChildren(1, a);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, "NOT");
        }
    }

    @Override
    public String toString() {
        return encapsulate("¬" + this.children[0].toString());
    }

    @Override
    public String toSMTExpr() {
        return encapsulate("not " + this.children[0].toSMTExpr());
    }

    public static Not not(SymbolicNode a) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        return new Not(a);
    }
}
