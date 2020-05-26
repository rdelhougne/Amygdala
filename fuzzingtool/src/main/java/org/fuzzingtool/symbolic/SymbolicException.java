package org.fuzzingtool.symbolic;

public class SymbolicException {
    public static class IncompatibleType extends Exception {
        public IncompatibleType(Type a, Type b, String op_name) {
            super("Types " + a.toString() + " and " + b.toString() + " are not compatible in Operation " + op_name + ".");
        }
        public IncompatibleType(Type a, String op_name) {
            super("Type " + a.toString() + " cannot be used with Operation " + op_name + ".");
        }
    }
    public static class WrongParameterSize extends Exception {
        public WrongParameterSize(int got, int expected, String op_name) {
            super("Wrong parameter size in operation " + op_name + ": Expected " + String.valueOf(expected) + ", got " + String.valueOf(got) + " instead.");
        }
    }
}
