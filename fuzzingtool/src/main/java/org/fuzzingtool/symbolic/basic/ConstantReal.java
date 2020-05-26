package org.fuzzingtool.symbolic.basic;

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
}
