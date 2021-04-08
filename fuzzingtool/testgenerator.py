#!/usr/bin/env python3

import random

ARITHMETIC_OPS = ["+", "-", "*", "/"]
BOOLEAN_OPS = ["==", "===", "<", ">", "<=", ">="]
INPUT_VARIABLE_NAMES = [
	"input_number_integer",
	"input_number_real",
	"input_boolean",
	"input_string"
]
CONSTANT_VARIABLE_NAMES = [
	"const_null",
	"const_undefined",
	"const_nan",
	"const_pos_infinity",
	"const_neg_infinity",
	"const_number_integer",
	"const_number_real",
	"const_boolean",
	"const_string"
]
CONSTANT_NAMES = [
	"null",
	"undefined",
	"NaN",
	"Infinity",
	"15",
	"7004",
	"-45",
	"0",
	"-1",
	"5.8",
	"132.3984",
	"-3434.239829382",
	"'abra'",
	"'kadabra'",
	"'18'",
	"'-60'",
	"'56.7'",
	"'-34.2'",
	"'2.0'",
	"'-7.0'",
	"'NaN'",
	"'Infinity'"
]

PROGRAM_DECL_PREFIX = """//--------------------------------Inputs----------------------------------------

var input_number_integer = 5; // Input
var input_number_real = 6.7; // Input
//var input_bigint = BigInt(9001);
var input_boolean = true; // Input
var input_string = 'abc'; // Input

//-------------------------------Constants--------------------------------------

var const_null = null;
var const_undefined = undefined;
var const_nan = NaN;
var const_pos_infinity = Infinity;
var const_neg_infinity = -Infinity;
var const_number_integer = 7;
var const_number_real = 9.877;
//var const_bigint = BigInt(9001);
var const_boolean = false;
var const_string = 'halelulja';

//------------------------------Conditions--------------------------------------

"""


def generate_simple_expression(num_const):
	return "print(" + generate_expression((0, 0), (0, 0), num_const) + ");"


def generate_program(num_expressions, num_input_vars, num_const_vars, num_const):
	program_str = PROGRAM_DECL_PREFIX
	concrete_num_expressions = random.randint(num_expressions[0], num_expressions[1])
	for exp_indx in range(concrete_num_expressions):
		program_str += generate_if(num_input_vars, num_const_vars, num_const, f"Got me, {exp_indx}!")
		program_str += "\n\n"
	return program_str


def generate_if(num_input_vars, num_const_vars, num_const, print_str):
	if_exp = f"if ({generate_expression(num_input_vars, num_const_vars, num_const)}) {{\n"
	if_exp += f"\tprint('{print_str}');\n}}"
	return if_exp


def generate_expression(num_input_vars, num_const_vars, num_const):
	concrete_num_input_vars = random.randint(num_input_vars[0], num_input_vars[1])
	concrete_num_const_vars = random.randint(num_const_vars[0], num_const_vars[1])
	concrete_num_const = random.randint(num_const[0], num_const[1])
	parts = []
	for _ in range(concrete_num_input_vars):
		parts.append(random.choice(INPUT_VARIABLE_NAMES))
	for _ in range(concrete_num_const_vars):
		parts.append(random.choice(CONSTANT_VARIABLE_NAMES))
	for _ in range(concrete_num_const):
		parts.append(random.choice(CONSTANT_NAMES))

	random.shuffle(parts)

	expression = ""
	for part_index in range(len(parts)):
		if part_index == len(parts) - 1:
			expression += parts[part_index]
		elif part_index == len(parts) - 2:
			expression += parts[part_index] + " " + random.choice(BOOLEAN_OPS) + " "
		else:
			expression += parts[part_index] + " " + random.choice(ARITHMETIC_OPS) + " "

	return expression
