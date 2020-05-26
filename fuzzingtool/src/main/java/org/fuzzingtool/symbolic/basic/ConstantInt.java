package org.fuzzingtool.symbolic.basic;

import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class ConstantInt extends SymbolicNode {
    Integer value;

    public ConstantInt(Integer value) {
        this.value = value;
        this.type = Type.INT;
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
