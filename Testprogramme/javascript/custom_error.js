nenner = 2;

function DivisionException(message) {
	this.message = message;
	this.name = "DivisionException";
}

function divide(n, m) {
	var l = 5;
	try {
		if (m == 0) {
			throw new DivisionException("Teilen durch Null nicht möglich");
		}
	} catch (e) {
		print("Catched!");
	}

	return n / m;
}

print(divide(5, nenner)); // fails with escalate_exceptions
print(23 == "23"); // fails with equal_is_strict_equal
print(40 || true); // fails with boolean_op_only_boolean_operands
print(98 * undefined); // fails with arith_op_no_undefined
print(null + 93); // fails with arith_op_no_null
print(45.7 - NaN); // fails with arith_op_no_nan
print(546456456 / Infinity); // fails with arith_op_no_infinity
print(34 / 0); // fails with division_op_no_zero
