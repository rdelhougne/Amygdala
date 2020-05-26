package org.fuzzingtool.symbolic.basic;

import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

public class SymbolicName extends SymbolicNode {
    private String name;

    public SymbolicName(String id, Type type) {
        this.name = id;
        this.type = type;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String toSMTExpr() {
        return name;
    }

    public static SymbolicName symbolic_boolean(String id) {
        return new SymbolicName(id, Type.BOOLEAN);
    }

    public static SymbolicName symbolic_int(String id) {
        return new SymbolicName(id, Type.INT);
    }

    public static SymbolicName symbolic_real(String id) {
        return new SymbolicName(id, Type.REAL);
    }

    public static SymbolicName symbolic_string(String id) {
        return new SymbolicName(id, Type.STRING);
    }
}
