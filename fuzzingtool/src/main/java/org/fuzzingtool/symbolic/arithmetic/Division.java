package org.fuzzingtool.symbolic.arithmetic;

import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class Division extends SymbolicNode {
    public Division(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (a.type == b.type && (a.type == Type.INT || a.type == Type.REAL)) { //TODO
            this.type = a.type;
            addChildren(2, a, b);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, b.type, "DIV");
        }
    }

    @Override
    public String toString() {
        return encapsulate(this.children[0].toString() + " / " + this.children[1].toString());
    }

    @Override
    public String toSMTExpr() {
        return encapsulate("/ " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    public static Division div(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        return new Division(a, b);
    }
}
