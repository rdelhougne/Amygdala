package org.fuzzingtool.core.symbolic.logical;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class And extends SymbolicNode {
	public And(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.languageSemantic = s;
		addChildren(a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses(this.children[0].toHRString() + " && " + this.children[1].toHRString());
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("and " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
	}

	/**
	 * Logic and operator as in
     * <a href="https://tc39.es/ecma262/2020/#sec-binary-logical-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @return Result of the '&&' operation
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		// TODO solve expr like this: (('in' && 8) + '_hello') === 'in_hello'
		Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
		Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);
		if (checkTypeAll(ExpressionType.BOOLEAN, a, b)) {
			return Pair.create(ctx.mkAnd((BoolExpr) a.getLeft(), (BoolExpr) b.getLeft()), ExpressionType.BOOLEAN);
		} else {
			throw new SymbolicException.NotImplemented("Expression 'AND' can only handle boolean children for now.");
		}
	}
}
