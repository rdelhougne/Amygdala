package org.fuzzingtool.symbolic.logical;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
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
        return parentheses("¬" + this.children[0].toString());
    }

    @Override
    public String toSMTExpr() {
        return parentheses("not " + this.children[0].toSMTExpr());
    }

    @Override
    public Expr toZ3Expr(Context ctx) {
        if (allChildrenType(Type.BOOLEAN)) {
            BoolExpr a = (BoolExpr) this.children[0].toZ3Expr(ctx);
            return ctx.mkNot(a);
        } // TODO
        return null;
    }

    public static Not not(SymbolicNode a) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        return new Not(a);
    }
}
