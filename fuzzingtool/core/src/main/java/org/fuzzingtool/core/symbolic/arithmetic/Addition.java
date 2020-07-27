package org.fuzzingtool.core.symbolic.arithmetic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.SeqExpr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

/**
 * Class for abstract '+' operation.
 */
public class Addition extends SymbolicNode {
	public Addition(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
		this.languageSemantic = s;
		addChildren(2, a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses(this.children[0].toHRString() + " + " + this.children[1].toHRString());
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("+ " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
	}

	/**
	 * Addition operator as in
     * <a href="https://tc39.es/ecma262/2020/#sec-addition-operator-plus">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @return Result of the '+' operation
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
		Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);
		if (a.getRight() == ExpressionType.STRING || b.getRight() == ExpressionType.STRING) {
			Pair<Expr, ExpressionType> a_string = toStringZ3JS(ctx, a);
			Pair<Expr, ExpressionType> b_string = toStringZ3JS(ctx, b);
			return Pair.create(ctx.mkConcat((SeqExpr) a_string.getLeft(), (SeqExpr) b_string.getLeft()),
							   ExpressionType.STRING);
		}
		Pair<Expr, ExpressionType> a_numeric = toNumericZ3JS(ctx, a);
		Pair<Expr, ExpressionType> b_numeric = toNumericZ3JS(ctx, b);
		// https://tc39.es/ecma262/2020/#sec-numeric-types-number-add
		if (checkTypeContains(ExpressionType.NUMBER_NAN, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric) &&
				checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_POS_INFINITY);
		}
		if (checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NEG_INFINITY);
		}
		if (checkTypeAll(ExpressionType.BIGINT, a_numeric, b_numeric)) {
			return Pair.create(ctx.mkAdd((ArithExpr) a_numeric.getLeft(), (ArithExpr) b_numeric.getLeft()),
							   ExpressionType.BIGINT);
		}
		if (checkTypeAll(ExpressionType.NUMBER_INTEGER, a_numeric, b_numeric)) {
			return Pair.create(ctx.mkAdd((ArithExpr) a_numeric.getLeft(), (ArithExpr) b_numeric.getLeft()),
							   ExpressionType.NUMBER_INTEGER);
		}
		return Pair.create(ctx.mkAdd((ArithExpr) a_numeric.getLeft(), (ArithExpr) b_numeric.getLeft()),
						   ExpressionType.NUMBER_REAL);
	}
}
