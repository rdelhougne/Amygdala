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

public enum Operation {
	// Arithmetic
	ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, UNARY_MINUS, UNARY_PLUS, MODULO, SQRT,

	// Logical
	AND, OR, NOT, EQUAL, STRICT_EQUAL, GREATER_EQUAL, GREATER_THAN, LESS_EQUAL, LESS_THAN,

	// String
	STR_LENGTH, STR_CONCAT, STR_CHAR_AT, STR_SUBSTR, STR_INCLUDES, STR_INDEXOF,

	// Array
	ARR_LENGTH, ARR_PUSH, ARR_JOIN,

	// Explicit Conversion
	STR_TO_INT
}
