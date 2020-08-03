package org.fuzzingtool.core.symbolic.string;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.SeqExpr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class StringCharAt extends SymbolicNode {
	public StringCharAt(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.languageSemantic = s;
		addChildren(a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return this.children[0].toHRString() + ".charAt(" + this.children[1].toHRString() + ")";
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("str.at " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
	}

	/**
	 * String charAt method as in https://tc39.es/ecma262/2020/#sec-string.prototype.charat
	 *
	 * @param ctx Z3-Context
	 * @return Result of the method
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> string_expr = SymbolicNode.toStringZ3JS(ctx, this.children[0].toZ3Expr(ctx));
		Pair<Expr, ExpressionType> index_expr = SymbolicNode.toIntegerZ3JS(ctx, this.children[1].toZ3Expr(ctx));
		return Pair.create(ctx.mkAt((SeqExpr) string_expr.getLeft(), (IntExpr) index_expr.getLeft()), ExpressionType.STRING);
	}
}
