package org.fuzzingtool.symbolic.logical;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class LessEqual extends SymbolicNode {
	public LessEqual(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
		this.languageSemantic = s;
		addChildren(2, a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses(this.children[0].toHRString() + " ≤ " + this.children[1].toHRString());
	}

	@Override
	public String toSMTExprJS() {
		return parentheses("LessEqual: NOT IMPLEMENTED");
	}

	/**
	 * Greater-than operator as in
     * <a href="https://tc39.es/ecma262/2020/#sec-relational-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @return Result of the '<=' operation, always BOOLEAN
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
		Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);

		Pair<Expr, ExpressionType> result = LessThan.abstractRelationalComparisonZ3JS(ctx, b, a);
		if (result.getRight() == ExpressionType.UNDEFINED) {
			return Pair.create(ctx.mkFalse(), ExpressionType.BOOLEAN);
		} else {
			return Pair.create(ctx.mkNot((BoolExpr) result.getLeft()), ExpressionType.BOOLEAN);
		}
	}
}
