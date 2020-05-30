package org.fuzzingtool.symbolic.basic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class ConstantVoid extends SymbolicNode {

    public ConstantVoid() {
        this.type = Type.VOID;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public String toSMTExpr() {
        return "ConstantVoid: NOT IMPLEMENTED";
    }

    @Override
    public Expr toZ3Expr(Context ctx) {
        return null; // TODO
    }
}
