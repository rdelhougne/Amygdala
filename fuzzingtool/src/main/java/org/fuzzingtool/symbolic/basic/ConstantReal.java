package org.fuzzingtool.symbolic.basic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class ConstantReal extends SymbolicNode {
    Double value;

    public ConstantReal(Double value) {
        this.value = value;
        this.type = Type.REAL;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }

    @Override
    public String toSMTExpr() {
        return String.valueOf(this.value);
    }

    @Override
    public Expr toZ3Expr(Context ctx) {
        return ctx.mkReal(this.value.intValue()); // TODO
    }
}
