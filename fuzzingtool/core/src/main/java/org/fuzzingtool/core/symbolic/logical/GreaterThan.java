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

package org.fuzzingtool.core.symbolic.logical;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class GreaterThan extends SymbolicNode {
	public GreaterThan(LanguageSemantic s, SymbolicNode a, SymbolicNode b) {
		this.language_semantic = s;
		addChildren(a, b);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return parentheses(this.children[0].toHRString() + " > " + this.children[1].toHRString());
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("> " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
	}

	/**
	 * Greater-than operator as in
     * <a href="https://tc39.es/ecma262/2020/#sec-relational-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
	 *
	 * @param ctx Z3-Context
	 * @return Result of the '>' operation, always BOOLEAN
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
			return result;
		}
	}
}
