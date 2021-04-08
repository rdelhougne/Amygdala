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

package org.fuzzingtool.core.symbolic;

public class SymbolicException {
	public static class IncompatibleType extends Exception {
		public IncompatibleType(ExpressionType a, ExpressionType b, String op_name) {
			super("Types " + a.toString() + " and " + b.toString() +
                          " are not compatible in Operation " + op_name);
		}

		public IncompatibleType(ExpressionType a, String op_name) {
			super("ExpressionType " + a.toString() + " cannot be used with Operation " + op_name);
		}

		public IncompatibleType(ExpressionType a) {
			super("Wrong object type of value for symbolic constant " + a.toString());
		}
	}

	public static class NotImplemented extends Exception {
		public NotImplemented(String msg) {
			super("Not fully implemented feature used: " + msg);
		}
	}

	public static class UndecidableExpression extends Exception {
		public UndecidableExpression(String solver, String msg) {
			super("Unable to construct expression for solver " + solver + ": " + msg);
		}
	}
}
