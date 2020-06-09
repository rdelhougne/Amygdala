package org.fuzzingtool.components;

import org.fuzzingtool.symbolic.ExpressionType;

/**
 * This class is for identifying Variables in the program context.
 * For now it is basically "String", but should be replaced with information like function scope, stack frame, parent object etc.
 * This class is immutable.
 */
public final class VariableIdentifier {
    private String identifier; // TODO replace

    public VariableIdentifier(String id) {
        this.identifier = id;
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
    /*public ExpressionType getVariableType() {
        return type;
    }*/

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
            return this.identifier.equals(((VariableIdentifier) obj).identifier);
        }
        return false;
    }

    public static VariableIdentifier fromString(String representation) {
        // TODO Parse String
        return new VariableIdentifier(representation);
    }
}
