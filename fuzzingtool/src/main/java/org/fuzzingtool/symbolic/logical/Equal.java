package org.fuzzingtool.symbolic.logical;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class Equal extends SymbolicNode {
    public Equal(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
        this.languageSemantic = s;
        addChildren(2, a, b);
    }

    @Override
    public String toHRStringJS() {
        return parentheses(this.children[0].toString() + " == " + this.children[1].toString());
    }

    @Override
    public String toSMTExprJS() throws SymbolicException.NotImplemented {
        return parentheses("= " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    @Override
    public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented, SymbolicException.UndecidableExpression {
        Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
        Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);

        return abstractEqualityComparisonZ3JS(ctx, a, b);
    }

    /**
     * Performs an abstract equality comparison as in <a href="https://tc39.es/ecma262/2020/#sec-abstract-equality-comparison">ECMAScript® 2020 Language Specification</a>
     *
     * @param ctx Z3-Context
     * @param x left-side Parameter
     * @param y right-side Parameter
     * @return Result of x == y, always ExpressionType.BOOLEAN
     */
    public static Pair<Expr, ExpressionType> abstractEqualityComparisonZ3JS(Context ctx, Pair<Expr, ExpressionType> x, Pair<Expr, ExpressionType> y) throws SymbolicException.UndecidableExpression, SymbolicException.NotImplemented {
        if (StrictEqual.typeEquality(x, y)) {
            return StrictEqual.abstractStrictEqualityComparisonZ3JS(ctx, x, y);
        }
        if (x.getRight() == ExpressionType.NULL && y.getRight() == ExpressionType.UNDEFINED) {
            return Pair.create(ctx.mkTrue(), ExpressionType.BOOLEAN);
        }
        if (x.getRight() == ExpressionType.UNDEFINED && y.getRight() == ExpressionType.NULL) {
            return Pair.create(ctx.mkTrue(), ExpressionType.BOOLEAN);
        }
        if (checkTypeIsNumeric(x) && checkTypeAll(ExpressionType.STRING, y)) {
            Pair<Expr, ExpressionType> y_numeric = toNumericZ3JS(ctx, y);
            return Pair.create(ctx.mkEq(x.getLeft(), y_numeric.getLeft()), ExpressionType.BOOLEAN);
        }
        if (checkTypeAll(ExpressionType.STRING, x) && checkTypeIsNumeric(y)) {
            Pair<Expr, ExpressionType> x_numeric = toNumericZ3JS(ctx, x);
            return Pair.create(ctx.mkEq(x_numeric.getLeft(), y.getLeft()), ExpressionType.BOOLEAN);
        }
        //TODO rest of types
        throw new SymbolicException.NotImplemented("Advanced equality not implemented.");
    }
}
