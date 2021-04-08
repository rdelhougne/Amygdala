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

package org.fuzzingtool.core.symbolic.basic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.core.components.VariableIdentifier;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class SymbolicVariable extends SymbolicNode {
	private final VariableIdentifier identifier;

	public SymbolicVariable(LanguageSemantic s, VariableIdentifier var_id) {
		this.language_semantic = s;
		this.identifier = var_id;
	}

	@Override
	public String toHRStringJS() {
		return this.identifier.getIdentifierString();
	}

	@Override
	public String toSMTExprJS() {
		return this.identifier.getIdentifierString();
	}

	@Override
	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.UndecidableExpression {
		switch (this.identifier.getVariableType()) {
			case BOOLEAN:
				return Pair.create(ctx.mkBoolConst(identifier.getIdentifierString()), ExpressionType.BOOLEAN);
			case BIGINT:
				return Pair.create(ctx.mkIntConst(identifier.getIdentifierString()), ExpressionType.BIGINT);
			case NUMBER_INTEGER:
				return Pair.create(ctx.mkIntConst(identifier.getIdentifierString()), ExpressionType.NUMBER_INTEGER);
			case NUMBER_REAL:
				return Pair.create(ctx.mkRealConst(identifier.getIdentifierString()), ExpressionType.NUMBER_REAL);
			case STRING:
				return Pair.create(ctx.mkConst(identifier.getIdentifierString(), ctx.getStringSort()),
								   ExpressionType.STRING);
			default:
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot solve for type " +
						this.identifier.getVariableType().toString());
		}
	}
}
