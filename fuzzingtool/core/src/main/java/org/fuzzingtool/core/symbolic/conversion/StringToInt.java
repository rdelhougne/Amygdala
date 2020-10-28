package org.fuzzingtool.core.symbolic.conversion;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class StringToInt extends SymbolicNode {
	public StringToInt(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.language_semantic = s;
		addChildren(a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses("parseInt(" + this.children[0].toHRString() + ", " + this.children[1].toHRString() + ")");
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("str.to.int " + this.children[0].toSMTExpr());
	}

	/**
	 * JavaScript parseInt() function.
	 *
	 * @param ctx Z3-Context
	 * @return Result of the 'parseInt' operation
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		// TODO Radix
		Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
		Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);
		Pair<Expr, ExpressionType> a_to_string = toStringZ3JS(ctx, a);
		// TODO
		return toNumericZ3JS(ctx, a_to_string);
	}
}
