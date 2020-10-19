package org.fuzzingtool.core.symbolic;

/**
 * This enum denotes the type of a symbolic expression. Every symbolic type that is supported should be listed here.
 */
public enum ExpressionType {
	// These types are fully supported (constants + variables + expressions)
	BOOLEAN, // JavaScript boolean type https://tc39.es/ecma262/2020/#sec-ecmascript-language-types-boolean-type
	STRING, // https://tc39.es/ecma262/2020/#sec-ecmascript-language-types-string-type
	BIGINT, // JavaScript BigInt type https://tc39.es/ecma262/2020/#sec-ecmascript-language-types-bigint-type, represents BigInt values
	NUMBER_INTEGER, // JavaScript Number type https://tc39.es/ecma262/2020/#sec-ecmascript-language-types-number-type, represents 'Number' values that are handled as integers internally
	NUMBER_REAL, // JavaScript Number type https://tc39.es/ecma262/2020/#sec-ecmascript-language-types-number-type, represents Doubles

	// These types are only supported in constants and some expressions (may throw Undecidable Exception!)
	UNDEFINED, // https://tc39.es/ecma262/2020/#sec-ecmascript-language-types-undefined-type
	NULL, // https://tc39.es/ecma262/2020/#sec-ecmascript-language-types-null-type
	NUMBER_NAN, // Double NaN value, Real cannot represent this
	NUMBER_POS_INFINITY, // Double positive Infinity value, Real cannot represent this
	NUMBER_NEG_INFINITY, // Double negative Infinity value, Real cannot represent this
	OBJECT, // https://tc39.es/ecma262/2020/#sec-object-type
	SYMBOL, // https://tc39.es/ecma262/2020/#sec-ecmascript-language-types-symbol-type

	INTERNAL_ERROR // used to flag errors in the fuzzing program
}
