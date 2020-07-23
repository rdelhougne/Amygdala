package org.fuzzingtool.symbolic.arithmetic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.ExpressionType;
import org.graalvm.collections.Pair;

public class Subtraction extends SymbolicNode {
	public Subtraction(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
		this.languageSemantic = s;
		addChildren(2, a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses(this.children[0].toHRString() + " - " + this.children[1].toHRString());
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("- " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
	}

	/**
	 * Subtraction Operator as in
	 * <a href="https://tc39.es/ecma262/2020/#sec-subtraction-operator-minus">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @return Result of the Subtraction
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> a_numeric = toNumericZ3JS(ctx, this.children[0].toZ3Expr(ctx));
		Pair<Expr, ExpressionType> b_numeric = toNumericZ3JS(ctx, this.children[1].toZ3Expr(ctx));

		// https://tc39.es/ecma262/2020/#sec-numeric-types-number-subtract
		if (checkTypeContains(ExpressionType.NUMBER_NAN, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric) ||
				checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric) &&
				checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
			return Pair.create(null, a_numeric.getRight());
		}
		if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, a_numeric) ||
				checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, a_numeric)) {
			return Pair.create(null, a_numeric.getRight());
		}
		if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NEG_INFINITY);
		}
		if (checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_POS_INFINITY);
		}
		if (checkTypeAll(ExpressionType.BIGINT, a_numeric, b_numeric)) {
			return Pair.create(ctx.mkSub((ArithExpr) a_numeric.getLeft(), (ArithExpr) b_numeric.getLeft()),
							   ExpressionType.BIGINT);
		}
		if (checkTypeAll(ExpressionType.NUMBER_INTEGER, a_numeric, b_numeric)) {
			return Pair.create(ctx.mkSub((ArithExpr) a_numeric.getLeft(), (ArithExpr) b_numeric.getLeft()),
							   ExpressionType.NUMBER_INTEGER);
		}
		return Pair.create(ctx.mkSub((ArithExpr) a_numeric.getLeft(), (ArithExpr) b_numeric.getLeft()),
						   ExpressionType.NUMBER_REAL);
	}
}
