package org.fuzzingtool.symbolic.logical;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.ExpressionType;
import org.graalvm.collections.Pair;

public class LessThan extends SymbolicNode {
    public LessThan(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
        this.languageSemantic = s;
        addChildren(2, a, b);
    }

    @Override
    public String toHRStringJS() {
        return parentheses(this.children[0].toString() + " < " + this.children[1].toString());
    }

    @Override
    public String toSMTExprJS() throws SymbolicException.NotImplemented {
        return parentheses("< " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    /**
     * Less-than operator as in <a href="https://tc39.es/ecma262/2020/#sec-relational-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
     *
     * @param ctx Z3-Context
     * @return Result of the '<' operation, always BOOLEAN
     * @throws SymbolicException.NotImplemented
     */
    @Override
    public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented, SymbolicException.UndecidableExpression {
        Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
        Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);

        Pair<Expr, ExpressionType> result = abstractRelationalComparisonZ3JS(ctx, a, b);
        if (result.getRight() == ExpressionType.UNDEFINED) {
            return Pair.create(ctx.mkFalse(), ExpressionType.BOOLEAN);
        } else {
            return result;
        }
    }

    /**
     * Performs an abstract relational comparison as in <a href="https://tc39.es/ecma262/2020/#sec-abstract-relational-comparison">ECMAScript® 2020 Language Specification</a>
     * We don't need the leftFirst flag, because all parameters are already primitives and therefore a conversion with potential side-effects does not occur.
     *
     * @param ctx Z3-Context
     * @param x left-side Parameter
     * @param y right-side Parameter
     * @return Result of x < y, can be ExpressionType.BOOLEAN or UNDEFINED
     */
    public static Pair<Expr, ExpressionType> abstractRelationalComparisonZ3JS(Context ctx, Pair<Expr, ExpressionType> x, Pair<Expr, ExpressionType> y) throws SymbolicException.UndecidableExpression {
        if (x.getRight() == ExpressionType.STRING && y.getRight() == ExpressionType.STRING) {
            throw new SymbolicException.UndecidableExpression("Z3", "Cannot compare strings with <");
        } else {
            if (x.getRight() == ExpressionType.BIGINT && y.getRight() == ExpressionType.STRING) {
                Pair<Expr, ExpressionType> y_bigint = tryCastZ3JS(ctx, y, ExpressionType.BIGINT);
                if (y_bigint.getRight() == ExpressionType.NUMBER_NAN) {
                    return Pair.create(null, ExpressionType.UNDEFINED);
                }
                return Pair.create(ctx.mkLt((ArithExpr) x.getLeft(), (ArithExpr) y_bigint.getLeft()), ExpressionType.BOOLEAN);
            }
            if (x.getRight() == ExpressionType.STRING && y.getRight() == ExpressionType.BIGINT) {
                Pair<Expr, ExpressionType> x_bigint = tryCastZ3JS(ctx, x, ExpressionType.BIGINT);
                if (x_bigint.getRight() == ExpressionType.NUMBER_NAN) {
                    return Pair.create(null, ExpressionType.UNDEFINED);
                }
                return Pair.create(ctx.mkLt((ArithExpr) x_bigint.getLeft(), (ArithExpr) y.getLeft()), ExpressionType.BOOLEAN);
            }

            Pair<Expr, ExpressionType> x_numeric = toNumericZ3JS(ctx, x);
            Pair<Expr, ExpressionType> y_numeric = toNumericZ3JS(ctx, y);

            if (checkTypeContains(ExpressionType.NUMBER_NAN, x_numeric, y_numeric)) {
                return Pair.create(null, ExpressionType.UNDEFINED);
            }
            if (checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, x_numeric) || checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, y_numeric)) {
                return Pair.create(ctx.mkTrue(), ExpressionType.BOOLEAN);
            }
            if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, x_numeric) || checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, y_numeric)) {
                return Pair.create(ctx.mkFalse(), ExpressionType.BOOLEAN);
            }

            return Pair.create(ctx.mkLt((ArithExpr) x_numeric.getLeft(), (ArithExpr) y_numeric.getLeft()), ExpressionType.BOOLEAN);
        }
    }
}
