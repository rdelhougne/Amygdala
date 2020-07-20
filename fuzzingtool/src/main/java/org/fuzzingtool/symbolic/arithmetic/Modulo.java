package org.fuzzingtool.symbolic.arithmetic;

import com.microsoft.z3.*;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

/**
 * Class for abstract '%' operation.
 */
public class Modulo extends SymbolicNode {
	public Modulo(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
		this.languageSemantic = s;
		addChildren(2, a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses(this.children[0].toHRString() + " % " + this.children[1].toHRString());
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("% " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
	}

	/**
	 * Remainder operator as in
	 * <a href="https://tc39.es/ecma262/2020/#sec-multiplicative-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @return Result of the '%' operation
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> a_numeric = toNumericZ3JS(ctx, this.children[0].toZ3Expr(ctx));
		Pair<Expr, ExpressionType> b_numeric = toNumericZ3JS(ctx, this.children[1].toZ3Expr(ctx));

		// https://tc39.es/ecma262/2020/#sec-numeric-types-number-remainder
		if (checkTypeContains(ExpressionType.NUMBER_NAN, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, a_numeric) || checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, a_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		//TODO punkt 3 prüfen
		if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, b_numeric) || checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, b_numeric)) {
			return a_numeric;
		}
		//TODO punkt 5 prüfen
		//TODO punkt 6...
		if (checkTypeAll(ExpressionType.NUMBER_INTEGER, a_numeric, b_numeric)) {
			return Pair.create(ctx.mkMod((IntExpr) a_numeric.getLeft(), (IntExpr) b_numeric.getLeft()), ExpressionType.NUMBER_INTEGER);
		} else if (checkTypeAll(ExpressionType.BIGINT, a_numeric, b_numeric)) {
			return Pair.create(ctx.mkMod((IntExpr) a_numeric.getLeft(), (IntExpr) b_numeric.getLeft()), ExpressionType.BIGINT);
		} else {
			throw new SymbolicException.UndecidableExpression("Z3", "Cannot solve modulo operation with types other than NUMBER_INTEGER and BIGINT");
		}
	}
}
