package org.fuzzingtool.symbolic.basic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class ConstantString extends SymbolicNode {
    String value;

    public ConstantString(String value) {
        this.value = value;
        this.type = Type.STRING;
    }

    @Override
    public String toString() {
        return "\"" + this.value + "\"";
    }

    @Override
    public String toSMTExpr() {
        return "ConstantString: NOT IMPLEMENTED"; // TODO
    }

    @Override
    public Expr toZ3Expr(Context ctx) {
        return ctx.mkString(value); // TODO ist das richtig?
    }
}
