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

public class StringSubstr extends SymbolicNode {
	public StringSubstr(LanguageSemantic s, SymbolicNode a, SymbolicNode b, SymbolicNode c) throws SymbolicException.WrongParameterSize {
		this.languageSemantic = s;
		addChildren(3, a, b, c);
	}

	public StringSubstr(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
		this.languageSemantic = s;
		addChildren(2, a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		if (children.length == 3) {
			return this.children[0].toHRString() + ".substr(" + this.children[1].toHRString() + ", " + this.children[2].toHRString() + ")";
		} else {
			return this.children[0].toHRString() + ".substr(" + this.children[1].toHRString() + ")";
		}
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		if (children.length == 3) {
			return parentheses("str.substr " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr() + " " +
									   this.children[2].toSMTExpr());
		} else {
			throw new SymbolicException.NotImplemented("SMT expression of short parameter substring");
		}
	}

	/**
	 * String substr method as in https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/substr
	 * Should be expanded to https://tc39.es/ecma262/2020/#sec-string.prototype.substring
	 *
	 * @param ctx Z3-Context
	 * @return Result of the method
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		// TODO not complete (negative indexes)
		if (children.length == 3) {
			Pair<Expr, ExpressionType> string_expr = SymbolicNode.toStringZ3JS(ctx, this.children[0].toZ3Expr(ctx));
			Pair<Expr, ExpressionType> index_expr = SymbolicNode.toIntegerZ3JS(ctx, this.children[1].toZ3Expr(ctx));
			Pair<Expr, ExpressionType> length_expr = SymbolicNode.toIntegerZ3JS(ctx, this.children[2].toZ3Expr(ctx));
			return Pair.create(ctx.mkExtract((SeqExpr) string_expr.getLeft(), (IntExpr) index_expr.getLeft(), (IntExpr) length_expr.getLeft()), ExpressionType.STRING);
		} else {
			throw new SymbolicException.NotImplemented("Expression of short parameter substring");
		}
	}
}
