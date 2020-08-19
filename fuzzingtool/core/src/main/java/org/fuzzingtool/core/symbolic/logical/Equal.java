package org.fuzzingtool.core.symbolic.logical;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class Equal extends SymbolicNode {
	public Equal(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.languageSemantic = s;
		addChildren(a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses(this.children[0].toHRString() + " == " + this.children[1].toHRString());
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("= " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
	}

	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
		Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);

		return abstractEqualityComparisonZ3JS(ctx, a, b);
	}

	/**
	 * Performs an abstract equality comparison as in
	 * <a href="https://tc39.es/ecma262/2020/#sec-abstract-equality-comparison">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @param x   left-side Parameter
	 * @param y   right-side Parameter
	 * @return Result of x == y, always ExpressionType.BOOLEAN
	 */
	public static Pair<Expr, ExpressionType> abstractEqualityComparisonZ3JS(Context ctx, Pair<Expr, ExpressionType> x,
                                                                            Pair<Expr, ExpressionType> y) throws
			SymbolicException.UndecidableExpression, SymbolicException.NotImplemented {
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
		// BigInt is handled above
		if (checkTypeAll(ExpressionType.BOOLEAN, x)) {
			return abstractEqualityComparisonZ3JS(ctx, toNumericZ3JS(ctx, x), y);
		}
		if (checkTypeAll(ExpressionType.BOOLEAN, y)) {
			return abstractEqualityComparisonZ3JS(ctx, x, toNumericZ3JS(ctx, y));
		}
		if (checkTypeAll(ExpressionType.OBJECT, x) || checkTypeAll(ExpressionType.OBJECT, y)) {
			throw new SymbolicException.UndecidableExpression("Z3", "Cannot perform equality with OBJECT type.");
		}
		if ((checkTypeAll(ExpressionType.BIGINT, x) && checkTypeIsNumeric(y)) ||
				checkTypeAll(ExpressionType.BIGINT, y) && checkTypeIsNumeric(x)) {
			if (checkTypeContains(ExpressionType.NUMBER_NAN, x, y) ||
					checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, x, y) ||
					checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, x, y)) {
				return Pair.create(ctx.mkFalse(), ExpressionType.BOOLEAN);
			}
			return Pair.create(ctx.mkEq(x.getLeft(), y.getLeft()), ExpressionType.BOOLEAN);
		}
		throw new SymbolicException.NotImplemented("Equality not implemented for types " + x.getRight().name() + " and " + y.getRight().name() + ".");
	}
}
