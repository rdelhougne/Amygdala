package org.fuzzingtool.symbolic.basic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.components.VariableIdentifier;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class SymbolicVariable extends SymbolicNode {
    private VariableIdentifier identifier;
    private ExpressionType variableType;

    public SymbolicVariable(LanguageSemantic s, VariableIdentifier var_id, ExpressionType t) {
        this.languageSemantic = s;
        this.identifier = var_id;
        this.variableType = t;
    }

    @Override
    public String toHRStringJS() {
        return this.identifier.getIdentifierString();
    }

    @Override
    public String toSMTExprJS() {
        return this.identifier.getIdentifierString();
    }

    @Override
    public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.UndecidableExpression {
        switch (this.variableType) {
            case BOOLEAN:
                return Pair.create(ctx.mkBoolConst(identifier.getIdentifierString()), ExpressionType.BOOLEAN);
            case BIGINT:
                return Pair.create(ctx.mkIntConst(identifier.getIdentifierString()), ExpressionType.BIGINT);
            case NUMBER_INTEGER:
                return Pair.create(ctx.mkIntConst(identifier.getIdentifierString()), ExpressionType.NUMBER_INTEGER);
            case NUMBER_REAL:
                return Pair.create(ctx.mkRealConst(identifier.getIdentifierString()), ExpressionType.NUMBER_REAL);
            case STRING:
                return Pair.create(ctx.mkConst(identifier.getIdentifierString(), ctx.getStringSort()), ExpressionType.STRING);
            default:
                throw new SymbolicException.UndecidableExpression("Z3", "Cannot solve for type " + this.variableType.toString());
        }
    }

    /*@Override
    public HashSet<VariableIdentifier> getSymbolicVars() {
        HashSet<VariableIdentifier> sym_set = new HashSet<>();
        sym_set.add(this.identifier);
        return sym_set;
    }*/
}
