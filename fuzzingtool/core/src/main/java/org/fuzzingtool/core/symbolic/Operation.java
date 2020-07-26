package org.fuzzingtool.core.symbolic;

public enum Operation {
	// Arithmetic
	ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, UNARY_MINUS, UNARY_PLUS, MODULO,

	// Logical
	AND, OR, NOT, EQUAL, STRICT_EQUAL, GREATER_EQUAL, GREATER_THAN, LESS_EQUAL, LESS_THAN,

	// String
	STR_LENGTH
}
