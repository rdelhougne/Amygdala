package org.fuzzingtool.symbolic.logical;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class GreaterEqual extends SymbolicNode {
    public GreaterEqual(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (a.type == b.type && (a.type == Type.INT || a.type == Type.REAL)) {
            this.type = Type.BOOLEAN;
            addChildren(2, a, b);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, "GE");
        }
    }

    @Override
    public String toString() {
        return parentheses(this.children[0].toString() + " ≥ " + this.children[1].toString());
    }

    @Override
    public String toSMTExpr() {
        return parentheses("GreaterEqual: NOT IMPLEMENTED");
    }

    @Override
    public Expr toZ3Expr(Context ctx) {
        if (this.children[0].type == this.children[1].type && (this.children[0].type == Type.INT || this.children[0].type == Type.REAL)) {
            ArithExpr a = (ArithExpr) this.children[0].toZ3Expr(ctx);
            ArithExpr b = (ArithExpr) this.children[1].toZ3Expr(ctx);
            return ctx.mkGe(a, b);
        } // TODO
        return null;
    }

    public static GreaterEqual ge(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        return new GreaterEqual(a, b);
    }
}
