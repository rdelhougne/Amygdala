package org.fuzzingtool.core.symbolic.string;

import com.microsoft.z3.*;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class StringIndexOf extends SymbolicNode {
	public StringIndexOf(LanguageSemantic s, SymbolicNode a, SymbolicNode b, SymbolicNode c) {
		this.languageSemantic = s;
		addChildren(a, b, c);
	}

	public StringIndexOf(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.languageSemantic = s;
		addChildren(a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		if (children.length == 3) {
			return this.children[0].toHRString() + ".indexOf(" + this.children[1].toHRString() + ", " + this.children[2].toHRString() + ")";
		} else {
			return this.children[0].toHRString() + ".indexOf(" + this.children[1].toHRString() + ")";
		}
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		if (children.length == 3) {
			return parentheses("str.indexof " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr() + " " +
									   this.children[2].toSMTExpr());
		} else {
			return parentheses("str.indexof " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr() + " 0");
		}
	}

	/**
	 * String includes method as in https://tc39.es/ecma262/2020/#sec-string.prototype.includes
	 *
	 * @param ctx Z3-Context
	 * @return Result of the method
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		// TODO NOT complete (regex)
		if (children.length == 2) {
			Pair<Expr, ExpressionType> string_expr = SymbolicNode.toStringZ3JS(ctx, this.children[0].toZ3Expr(ctx));
			Pair<Expr, ExpressionType> search_expr = SymbolicNode.toStringZ3JS(ctx, this.children[1].toZ3Expr(ctx));
			return Pair.create(ctx.mkIndexOf((SeqExpr) string_expr.getLeft(), (SeqExpr) search_expr.getLeft(), ctx.mkInt(0)), ExpressionType.NUMBER_INTEGER);
		} else {
			Pair<Expr, ExpressionType> string_expr = SymbolicNode.toStringZ3JS(ctx, this.children[0].toZ3Expr(ctx));
			Pair<Expr, ExpressionType> search_expr = SymbolicNode.toStringZ3JS(ctx, this.children[1].toZ3Expr(ctx));
			Pair<Expr, ExpressionType> min_index_expr = SymbolicNode.toIntegerZ3JS(ctx, this.children[2].toZ3Expr(ctx));
			return Pair.create(ctx.mkIndexOf((SeqExpr) string_expr.getLeft(), (SeqExpr) search_expr.getLeft(), (IntExpr) min_index_expr.getLeft()), ExpressionType.NUMBER_INTEGER);
		}
	}
}
