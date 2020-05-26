package org.fuzzingtool.symbolic.basic;

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
}
