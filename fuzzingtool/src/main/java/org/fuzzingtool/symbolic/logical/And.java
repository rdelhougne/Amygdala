package org.fuzzingtool.symbolic.logical;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class And extends SymbolicNode {
    public And(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (a.type == Type.BOOLEAN && b.type == Type.BOOLEAN) {
            this.type = Type.BOOLEAN;
            addChildren(2, a, b);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, "AND");
        }
    }

    @Override
    public String toString() {
        return parentheses(this.children[0].toString() + " && " + this.children[1].toString());
    }

    @Override
    public String toSMTExpr() {
        return parentheses("and " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    @Override
    public Expr toZ3Expr(Context ctx) {
        if (allChildrenType(Type.BOOLEAN)) {
            BoolExpr a = (BoolExpr) this.children[0].toZ3Expr(ctx);
            BoolExpr b = (BoolExpr) this.children[1].toZ3Expr(ctx);
            return ctx.mkAnd(a, b);
        } // TODO
        return null;
    }

    public static And and(SymbolicNode a, SymbolicNode b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        return new And(a, b);
    }
}
