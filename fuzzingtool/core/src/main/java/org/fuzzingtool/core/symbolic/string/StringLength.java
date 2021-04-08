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

package org.fuzzingtool.core.symbolic.string;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.SeqExpr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class StringLength extends SymbolicNode {
	public StringLength(LanguageSemantic s, SymbolicNode a) {
		this.language_semantic = s;
		addChildren(a);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return this.children[0].toHRString() + ".length";
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("str.len " + this.children[0].toSMTExpr());
	}

	/**
	 * String length attribute
	 *
	 * @param ctx Z3-Context
	 * @return Result of the .length attribute
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> string_expr = this.children[0].toZ3Expr(ctx);
		if (string_expr.getRight() == ExpressionType.STRING) {
			return Pair.create(ctx.mkLength((SeqExpr) string_expr.getLeft()), ExpressionType.NUMBER_INTEGER);
		} else {
			throw new SymbolicException.UndecidableExpression("Z3", "Cannot determine sequence length of expression of type '" + string_expr.getRight().name() + "'");
		}
	}
}
