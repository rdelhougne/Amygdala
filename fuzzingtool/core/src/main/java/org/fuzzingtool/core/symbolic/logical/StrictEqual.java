package org.fuzzingtool.core.symbolic.logical;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class StrictEqual extends SymbolicNode {
	public StrictEqual(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.language_semantic = s;
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

		return abstractStrictEqualityComparisonZ3JS(ctx, a, b);
	}

	/**
	 * Performs an abstract strict equality comparison as in
	 * <a href="https://tc39.es/ecma262/2020/#sec-abstract-equality-comparison">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @param x   left-side Parameter
	 * @param y   right-side Parameter
	 * @return Result of x === y, always ExpressionType.BOOLEAN
	 */
	public static Pair<Expr, ExpressionType> abstractStrictEqualityComparisonZ3JS(Context ctx, Pair<Expr,
            ExpressionType> x, Pair<Expr, ExpressionType> y) throws
			SymbolicException.UndecidableExpression, SymbolicException.NotImplemented {
		if (!typeEquality(x, y)) {
			return Pair.create(ctx.mkFalse(), ExpressionType.BOOLEAN);
		}
		if (checkTypeIsNumeric(x, y)) {
			return numericEquality(ctx, x, y);
		}
		return sameValueNonNumericZ3JS(ctx, x, y);
	}

	public static boolean typeEquality(Pair<Expr, ExpressionType> x, Pair<Expr, ExpressionType> y) {
		switch (x.getRight()) {
			case BOOLEAN:
				return y.getRight() == ExpressionType.BOOLEAN;
			case STRING:
				return y.getRight() == ExpressionType.STRING;
			case BIGINT:
				return y.getRight() == ExpressionType.BIGINT;
			case UNDEFINED:
				return y.getRight() == ExpressionType.UNDEFINED;
			case NULL:
				return y.getRight() == ExpressionType.NULL;
			case OBJECT:
				return y.getRight() == ExpressionType.OBJECT;
			case SYMBOL:
				return y.getRight() == ExpressionType.SYMBOL;
			default:
				return y.getRight() == ExpressionType.NUMBER_INTEGER || y.getRight() == ExpressionType.NUMBER_REAL ||
						y.getRight() == ExpressionType.NUMBER_NAN ||
						y.getRight() == ExpressionType.NUMBER_POS_INFINITY ||
						y.getRight() == ExpressionType.NUMBER_NEG_INFINITY;
		}
	}

	//https://tc39.es/ecma262/2020/#sec-numeric-types-number-equal
	public static Pair<Expr, ExpressionType> numericEquality(Context ctx, Pair<Expr, ExpressionType> x, Pair<Expr,
            ExpressionType> y) throws
			SymbolicException.UndecidableExpression {
		if (checkTypeContains(ExpressionType.NUMBER_NAN, x, y)) {
			return Pair.create(ctx.mkFalse(), ExpressionType.BOOLEAN);
		}
		if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, x, y) ||
				checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, x, y)) {
			return Pair.create(ctx.mkTrue(), ExpressionType.BOOLEAN);
		}
		if (checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, x, y) ||
				checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, x, y)) {
			throw new SymbolicException.UndecidableExpression("Z3", "Cannot solve equality with 'Infinity' type");
		}
		return Pair.create(ctx.mkEq(x.getLeft(), y.getLeft()), ExpressionType.BOOLEAN);
	}

	/**
	 * Performs a comparison between non-numeric literals
	 * <a href="https://tc39.es/ecma262/2020/#sec-samevaluenonnumeric">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @param x   left-side Parameter
	 * @param y   right-side Parameter
	 * @return Always ExpressionType.BOOLEAN
	 */
	public static Pair<Expr, ExpressionType> sameValueNonNumericZ3JS(Context ctx, Pair<Expr, ExpressionType> x,
                                                                     Pair<Expr, ExpressionType> y) throws
			SymbolicException.NotImplemented {
		assert x.getRight() != ExpressionType.BIGINT;
		assert x.getRight() != ExpressionType.NUMBER_INTEGER;
		assert x.getRight() != ExpressionType.NUMBER_REAL;
		assert x.getRight() != ExpressionType.NUMBER_NAN;
		assert x.getRight() != ExpressionType.NUMBER_POS_INFINITY;
		assert x.getRight() != ExpressionType.NUMBER_NEG_INFINITY;
		assert x.getRight() == y.getRight();

		switch (x.getRight()) {
			case BOOLEAN:
			case STRING:
				return Pair.create(ctx.mkEq(x.getLeft(), y.getLeft()), ExpressionType.BOOLEAN);
			case UNDEFINED:
			case NULL:
				return Pair.create(ctx.mkTrue(), ExpressionType.BOOLEAN);
			case OBJECT:
				throw new SymbolicException.NotImplemented("Comparison between Objects not implemented");
			case SYMBOL:
				throw new SymbolicException.NotImplemented("Comparison between Symbols not implemented");
		}

		throw new SymbolicException.NotImplemented("Cannot perform comparison");
	}
}
