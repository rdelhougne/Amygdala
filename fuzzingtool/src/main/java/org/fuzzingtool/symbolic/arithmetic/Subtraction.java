package org.fuzzingtool.symbolic.arithmetic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class Subtraction extends SymbolicNode {
    public Subtraction(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (a.type == b.type && (a.type == Type.INT || a.type == Type.REAL)) {
            this.type = a.type;
            addChildren(2, a, b);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, b.type, "SUB");
        }
    }

    @Override
    public String toString() {
        return parentheses(this.children[0].toString() + " - " + this.children[1].toString());
    }

    @Override
    public String toSMTExpr() {
        return parentheses("- " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    @Override
    public Expr toZ3Expr(Context ctx) {
        if (allChildrenTypeOr(Type.INT, Type.REAL)) {
            ArithExpr a = (ArithExpr) this.children[0].toZ3Expr(ctx);
            ArithExpr b = (ArithExpr) this.children[1].toZ3Expr(ctx);
            return ctx.mkSub(a, b);
        } // TODO
        return null;
    }

    public static Subtraction sub(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        return new Subtraction(a, b);
    }
}
