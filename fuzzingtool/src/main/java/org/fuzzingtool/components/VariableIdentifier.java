package org.fuzzingtool.components;

import org.fuzzingtool.symbolic.Type;

/**
 * This class is for identifying Variables in the program context.
 * For now it is basically "String", but should be replaced with information like function scope, stack frame, parent object etc.
 * This class is immutable.
 */
public final class VariableIdentifier {
    private String identifier; // TODO replace
    private Type type;

    public VariableIdentifier(String id, Type t) {
        this.identifier = id;
        this.type = t;
    }

    /**
     * This method provides a String that can be used by the SMT-Solvers.
     *
     * @return A String based upon all context information
     */
    public String getIdentifierString() {
        return identifier;
    }

    /**
     * This method provides the type of the Variable.
     *
     * @return The variable type
     */
    public Type getVariableType() {
        return type;
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode(); // TODO
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof VariableIdentifier) {
            return this.identifier.equals(((VariableIdentifier) obj).identifier) && this.type.equals(((VariableIdentifier) obj).type);
        }
        return false;
    }
}
