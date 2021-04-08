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

import com.microsoft.z3.*;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

/**
 * Class for abstract '%' operation.
 */
public class Modulo extends SymbolicNode {
	public Modulo(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.language_semantic = s;
		addChildren(a, b);
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
		//TODO check statement 3...
		if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, b_numeric) || checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, b_numeric)) {
			return a_numeric;
		}
		//TODO check statement 5...
		//TODO check statement 6...
		if (checkTypeAll(ExpressionType.NUMBER_INTEGER, a_numeric, b_numeric)) {
			return Pair.create(ctx.mkMod((IntExpr) a_numeric.getLeft(), (IntExpr) b_numeric.getLeft()), ExpressionType.NUMBER_INTEGER);
		} else if (checkTypeAll(ExpressionType.BIGINT, a_numeric, b_numeric)) {
			return Pair.create(ctx.mkMod((IntExpr) a_numeric.getLeft(), (IntExpr) b_numeric.getLeft()), ExpressionType.BIGINT);
		} else {
			throw new SymbolicException.UndecidableExpression("Z3", "Cannot solve modulo operation with types other than NUMBER_INTEGER and BIGINT");
		}
	}
}
