package org.fuzzingtool.symbolic.basic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.components.VariableIdentifier;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.Type;

import java.util.HashSet;

public class SymbolicName extends SymbolicNode {
    private VariableIdentifier identifier;

    public SymbolicName(VariableIdentifier var_id) {
        this.identifier = var_id;
        this.type = var_id.getVariableType();
    }

    @Override
    public String toString() {
        return identifier.getIdentifierString();
    }

    @Override
    public String toSMTExpr() {
        return identifier.getIdentifierString();
    }

    @Override
    public Expr toZ3Expr(Context ctx) {
        switch (this.type) {
            case BOOLEAN:
                return ctx.mkBoolConst(identifier.getIdentifierString());
            case INT:
                return ctx.mkIntConst(identifier.getIdentifierString());
            case REAL:
                return ctx.mkRealConst(identifier.getIdentifierString());
            case STRING:
                return null; // TODO
            case VOID:
                return null; // TODO
        }
        return null;
    }

    @Override
    public HashSet<VariableIdentifier> getSymbolicVars() {
        HashSet<VariableIdentifier> sym_set = new HashSet<>();
        sym_set.add(this.identifier);
        return sym_set;
    }

    public static SymbolicName symbolic_boolean(String id) {
        return new SymbolicName(new VariableIdentifier(id, Type.BOOLEAN));
    }

    public static SymbolicName symbolic_int(String id) {
        return new SymbolicName(new VariableIdentifier(id, Type.INT));
    }

    public static SymbolicName symbolic_real(String id) {
        return new SymbolicName(new VariableIdentifier(id, Type.REAL));
    }

    public static SymbolicName symbolic_string(String id) {
        return new SymbolicName(new VariableIdentifier(id, Type.STRING));
    }
}
