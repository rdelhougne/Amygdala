package org.fuzzingtool.symbolic.basic;

import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class ConstantBoolean extends SymbolicNode {
    Boolean value;

    public ConstantBoolean(Boolean value) {
        this.value = value;
        this.type = Type.BOOLEAN;
    }

    @Override
    public String toString() {
        if (value) {
            return "True";
        } else {
            return "False";
        }
    }

    @Override
    public String toSMTExpr() {
        return "ConstantBoolean: NOT IMPLEMENTED";
    }
}
