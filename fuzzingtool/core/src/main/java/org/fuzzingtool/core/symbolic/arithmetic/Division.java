/*
 * Copyright 2021 Robert Delhougne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fuzzingtool.core.symbolic.arithmetic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

/**
 * Class for abstract '/' operation
 */
public class Division extends SymbolicNode {
	public Division(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.language_semantic = s;
		addChildren(a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses(this.children[0].toHRString() + " / " + this.children[1].toHRString());
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("/ " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
	}

	/**
	 * Division operator as in
	 * <a href="https://tc39.es/ecma262/2020/#sec-multiplicative-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @return Result of the '/' operation
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> a_numeric = toNumericZ3JS(ctx, this.children[0].toZ3Expr(ctx));
		Pair<Expr, ExpressionType> b_numeric = toNumericZ3JS(ctx, this.children[1].toZ3Expr(ctx));

		// https://tc39.es/ecma262/2020/#sec-numeric-types-number-divide
		if (checkTypeContains(ExpressionType.NUMBER_NAN, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric) &&
				checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}
		if (checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric) ||
				checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
			//TODO perhaps solve other expression to constant
			throw new SymbolicException.UndecidableExpression("Z3",
															  "Cannot solve division with infinity and non-infinity parameters");
		}
		//TODO zero rules
		if (checkTypeAll(ExpressionType.BIGINT, a_numeric, b_numeric)) {
			//TODO
			throw new SymbolicException.NotImplemented("Division for type BigInt not implemented");
		}
		// Division for "Number" type is always Real
		return Pair.create(ctx.mkDiv((ArithExpr) a_numeric.getLeft(), (ArithExpr) b_numeric.getLeft()),
						   ExpressionType.NUMBER_REAL);
	}
}
