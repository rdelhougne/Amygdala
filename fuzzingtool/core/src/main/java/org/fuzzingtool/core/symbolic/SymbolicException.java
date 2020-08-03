package org.fuzzingtool.core.symbolic;

public class SymbolicException {
	public static class IncompatibleType extends Exception {
		public IncompatibleType(ExpressionType a, ExpressionType b, String op_name) {
			super("Types " + a.toString() + " and " + b.toString() +
                          " are not compatible in Operation " + op_name + ".");
		}

		public IncompatibleType(ExpressionType a, String op_name) {
			super("ExpressionType " + a.toString() + " cannot be used with Operation " + op_name + ".");
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
