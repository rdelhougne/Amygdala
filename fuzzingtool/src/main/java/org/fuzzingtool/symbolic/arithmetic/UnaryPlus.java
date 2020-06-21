package org.fuzzingtool.symbolic.arithmetic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

/**
 * Class for abstract unary '+' operation.
 */
public class UnaryPlus extends SymbolicNode {
	public UnaryPlus(LanguageSemantic s, SymbolicNode a) throws SymbolicException.WrongParameterSize {
		this.languageSemantic = s;
		addChildren(1, a);
	}

	@Override
	public String toHRStringJS() {
		return "+" + this.children[0].toString();
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("+ " + this.children[0].toSMTExpr());
	}

	/**
	 * Unary Plus operator as in
     * <a href="https://tc39.es/ecma262/2020/#sec-unary-plus-operator">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @return Result of the unary '+' operation
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		return toNumericZ3JS(ctx, this.children[0].toZ3Expr(ctx));
	}
}
