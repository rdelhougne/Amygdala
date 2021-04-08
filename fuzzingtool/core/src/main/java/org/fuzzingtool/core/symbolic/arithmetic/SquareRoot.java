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
import com.microsoft.z3.RealExpr;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

/**
 * Class for square root operation.
 */
public class SquareRoot extends SymbolicNode {
	public SquareRoot(LanguageSemantic s, SymbolicNode a) {
		this.language_semantic = s;
		addChildren(a);
	}

	@Override
	public String toHRStringJS() throws SymbolicException.NotImplemented {
		return "sqrt(" + this.children[0].toHRString() + ")";
	}

	@Override
	public String toSMTExprJS() throws SymbolicException.NotImplemented {
		return parentheses("- " + this.children[0].toSMTExpr());
	}

	/**
	 * Square Root, will probably not work: https://stackoverflow.com/questions/62939593/python-z3-solver-not-correctly-reporting-satisfiability-for-exponent-constraints
	 *
	 * @param ctx Z3-Context
	 * @return Result of the square root operation
	 * @throws SymbolicException.NotImplemented
	 */
	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		Pair<Expr, ExpressionType> a_numeric = toNumericZ3JS(ctx, this.children[0].toZ3Expr(ctx));
		RealExpr inv_square = ctx.mkReal("0.5");
		ArithExpr solution = ctx.mkPower((ArithExpr) a_numeric.getLeft(), inv_square);
		return Pair.create(solution, ExpressionType.NUMBER_REAL);
	}
}
