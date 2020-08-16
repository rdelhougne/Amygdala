package org.fuzzingtool.core.components;

import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import org.fuzzingtool.core.Logger;

/**
 * Class for managing user-defined errors.
 */
public class CustomError {
	private final Logger logger;
	private boolean some_enabled = false;

	// Options
	private boolean escalate_exceptions = false;
	private boolean equal_is_strict_equal = false;
	private boolean boolean_op_only_boolean_operands = false;
	private boolean arith_op_no_undefined = false;
	private boolean arith_op_no_null = false;
	private boolean arith_op_no_nan = false;
	private boolean arith_op_no_infinity = false;
	private boolean division_op_no_zero = false;

	public CustomError(Logger lgr) {
		this.logger = lgr;
	}

	/**
	 * Set an option for this instance.
	 *
	 * @param name Name of the option
	 * @param value Option value
	 */
	public void setOption(String name, Object value) {
		try {
			switch (name) {
				case "escalate_exceptions":
					escalate_exceptions = (boolean) value;
					logSetOption(name, escalate_exceptions);
					break;
				case "equal_is_strict_equal":
					equal_is_strict_equal = (boolean) value;
					logSetOption(name, equal_is_strict_equal);
					break;
				case "boolean_op_only_boolean_operands":
					boolean_op_only_boolean_operands = (boolean) value;
					logSetOption(name, boolean_op_only_boolean_operands);
					break;
				case "arith_op_no_undefined":
					arith_op_no_undefined = (boolean) value;
					logSetOption(name, arith_op_no_undefined);
					break;
				case "arith_op_no_null":
					arith_op_no_null = (boolean) value;
					logSetOption(name, arith_op_no_null);
					break;
				case "arith_op_no_nan":
					arith_op_no_nan = (boolean) value;
					logSetOption(name, arith_op_no_nan);
					break;
				case "arith_op_no_infinity":
					arith_op_no_infinity = (boolean) value;
					logSetOption(name, arith_op_no_infinity);
					break;
				case "division_op_no_zero":
					division_op_no_zero = (boolean) value;
					logSetOption(name, division_op_no_zero);
					break;
				default:
					logger.warning("CustomError: Unknown option '" + name + "'.");
			}
		} catch (ClassCastException cce) {
			logger.warning("CustomError: Cannot cast value for option '" + name + "'.");
		}
		recalculateSomeEnabled();
	}

	private void logSetOption(String name, boolean enabled) {
		if (enabled) {
			logger.info("Custom error class '" + name + "' enabled.");
		} else {
			logger.info("Custom error class '" + name + "' disabled.");
		}
	}

	/**
	 * Get an option value from the instance.
	 *
	 * @param name Name of the option
	 * @return Option value
	 */
	public Object getOption(String name) {
		switch (name) {
			case "escalate_exceptions":
				return this.escalate_exceptions;
			case "equal_is_strict_equal":
				return this.equal_is_strict_equal;
			case "boolean_op_only_boolean_operands":
				return this.boolean_op_only_boolean_operands;
			case "arith_op_no_undefined":
				return this.arith_op_no_undefined;
			case "arith_op_no_null":
				return this.arith_op_no_null;
			case "arith_op_no_nan":
				return this.arith_op_no_nan;
			case "arith_op_no_infinity":
				return this.arith_op_no_infinity;
			case "division_op_no_zero":
				return this.division_op_no_zero;
			default:
				logger.warning("CustomError: Unknown option '" + name + "'.");
				return null;
		}
	}

	/**
	 * Check if at least one option is enabled.
	 *
	 * @return true, if at least one custom error class is enabled
	 */
	public boolean someEnabled() {
		return this.some_enabled;
	}

	/**
	 * Recalculate if at least one option is enabled.
	 */
	private void recalculateSomeEnabled() {
		this.some_enabled = this.escalate_exceptions ||
							this.equal_is_strict_equal ||
							this.boolean_op_only_boolean_operands ||
							this.arith_op_no_undefined ||
							this.arith_op_no_null ||
							this.arith_op_no_nan ||
							this.arith_op_no_infinity ||
							this.division_op_no_zero;
	}

	/**
	 * Enable an error class, shorthand for {@link #setOption(String, Object)}.
	 *
	 * @param type Name of the error class
	 */
	public void enable(String type) {
		setOption(type, true);
	}

	/**
	 * Disable an error class, shorthand for {@link #setOption(String, Object)}.
	 *
	 * @param type Name of the error class
	 */
	public void disable(String type) {
		setOption(type, false);
	}

	// exposed functions for use in wrapper nodes

	/**
	 * Central method for inspecting values and firing an {@link EscalatedException}
	 * depending on the return value.
	 *
	 * @param operation_name Operation name
	 * @param value Value of the value...
	 * @param line_num Line number for logging
	 */
	public void inspectInputValue(String operation_name, Object value, int input_index, int line_num) {
		switch (operation_name) {
			case "JSDivideNodeGen":
				inspectDivisionZero(operation_name, value, input_index, line_num);
				// no break, division is also arithmetic operation
			case "JSAddNodeGen":
			case "JSSubtractNodeGen":
			case "JSMultiplyNodeGen":
			case "JSUnaryMinusNodeGen":
			case "JSUnaryPlusNodeGen":
			case "JSModuloNodeGen":
			case "JSAddSubNumericUnitNodeGen":
			case "SqrtNodeGen":
			case "JSExponentiateNodeGen":
				inspectArithmeticValues(operation_name, value, line_num);
				break;
			case "JSAndNode":
			case "JSOrNode":
			case "JSNotNodeGen":
				if (boolean_op_only_boolean_operands) {
					if (!JSGuards.isBoolean(value)) {
						logger.info("Object of type '" + value.toString() + "' for boolean operation detected (boolean_op_only_boolean_operands).");
						throw new EscalatedException("Object of type '" + value.toString() + "' for boolean operation detected (boolean_op_only_boolean_operands). [" + operation_name + ", line " + line_num + "]");
					}
				}
				break;
		}
	}

	private void inspectDivisionZero(String operation_name, Object value, int input_index, int line_num) {
		if (division_op_no_zero && input_index == 1) {
			Object num = JSRuntime.toNumeric(value);
			if (num instanceof BigInt) {
				BigInt bigint_num = (BigInt) num;
				if (bigint_num.equals(BigInt.ZERO)) {
					logger.info("Detected zero value for division operation (division_op_no_zero).");
					throw new EscalatedException(
							"Detected zero value for division operation (division_op_no_zero). [" + operation_name + ", line " +
									line_num + "]");
				}
			} else if (num instanceof Integer) {
				Integer int_num = (Integer) num;
				if (int_num.equals(0)) {
					logger.info("Detected zero value for division operation (division_op_no_zero).");
					throw new EscalatedException(
							"Detected zero value for division operation (division_op_no_zero). [" + operation_name +
									", line " +
									line_num + "]");
				}
			} else if (num instanceof SafeInteger) {
				SafeInteger safeint_num = (SafeInteger) num;
				if (safeint_num.intValue() == 0) {
					logger.info("Detected zero value for division operation (division_op_no_zero).");
					throw new EscalatedException(
							"Detected zero value for division operation (division_op_no_zero). [" + operation_name + ", line " +
									line_num + "]");
				}
			} else if (num instanceof Double) {
				Double double_num = (Double) num;
				if (double_num.equals(0.0)) {
					logger.info("Detected zero value for division operation (division_op_no_zero).");
					throw new EscalatedException(
							"Detected zero value for division operation (division_op_no_zero). [" + operation_name + ", line " +
									line_num + "]");
				}
			} else {
				logger.warning("CustomError::inspect(): Unexpected value " + num.toString());
			}
		}
	}

	/**
	 * Central method for inspecting values and firing an {@link EscalatedException}
	 * depending on the return value.
	 *
	 * @param operation_name Operation name
	 * @param value Value of the value...
	 * @param line_num Line number for logging
	 */
	public void inspectReturnValue(String operation_name, Object value, int line_num) {
		switch (operation_name) {
			case "JSDivideNodeGen":
			case "JSAddNodeGen":
			case "JSSubtractNodeGen":
			case "JSMultiplyNodeGen":
			case "JSUnaryMinusNodeGen":
			case "JSUnaryPlusNodeGen":
			case "JSModuloNodeGen":
			case "JSAddSubNumericUnitNodeGen":
			case "SqrtNodeGen":
			case "JSExponentiateNodeGen":
				inspectArithmeticValues(operation_name, value, line_num);
				break;
		}
	}

	private void inspectArithmeticValues(String operation_name, Object value, int line_num) {
		if (arith_op_no_undefined) {
			if (JSGuards.isUndefined(value)) {
				logger.info("Detected undefined value for arithmetic operation (arith_op_no_undefined).");
				throw new EscalatedException(
						"Detected undefined value for arithmetic operation (arith_op_no_undefined). [" + operation_name + ", line " + line_num + "]");
			}
		}
		if (arith_op_no_null) {
			//if (JSGuards.isJSNull(value)) { //TODO does not work
			if (value.toString().startsWith("DynamicObject<null>")) {
				logger.info("Detected null value for arithmetic operation (arith_op_no_null).");
				throw new EscalatedException(
						"Detected null value for arithmetic operation (arith_op_no_null). [" + operation_name + ", line " + line_num + "]");
			}
		}
		if (arith_op_no_nan) {
			if (JSGuards.isNumberDouble(value) && JSRuntime.isNaN(value)) {
				logger.info("Detected NaN value for arithmetic operation (arith_op_no_nan).");
				throw new EscalatedException(
						"Detected NaN value for arithmetic operation (arith_op_no_nan). [" + operation_name + ", line " + line_num + "]");
			}
		}
		if (arith_op_no_infinity) {
			if (JSGuards.isNumberDouble(value) && (JSRuntime.isPositiveInfinity((double) value) || JSRuntime.isPositiveInfinity(-(double) value))) {
				logger.info("Detected Infinity value for arithmetic operation (arith_op_no_infinity).");
				throw new EscalatedException(
						"Detected Infinity value for arithmetic operation (arith_op_no_infinity). [" + operation_name + ", line " + line_num + "]");
			}
		}
	}

	/**
	 * @return Value of option "escalate_exceptions"
	 */
	public boolean escalateExceptionsEnabled() {
		return escalate_exceptions;
	}

	/**
	 * @return Value of option "equal_is_strict_equal"
	 */
	public boolean equalIsStrictEqualEnabled() {
		return equal_is_strict_equal;
	}

	/**
	 * Create a new Exception that cannot be caught.
	 *
	 * @param message Message of the exception
	 * @return Exception of type "EscalatedException"
	 */
	public static EscalatedException createException(String message) {
		return new EscalatedException(message);
	}

	/**
	 * This class should be used whenever an error occurs in the programs execution.
	 * All JavaScript exceptions (e.g. com.oracle.truffle.js.runtime.UserScriptException)
	 * will be caught by a try-catch-statement in the guest language source code.
	 * This exception extends RuntimeException directly and therefore will not be caught
	 * by any safety measures in the guest language. It can only be caught by the Fuzzer
	 * itself.
	 */
	public static class EscalatedException extends RuntimeException {
		public EscalatedException() {
			super();
		}

		public EscalatedException(String message) {
			super(message);
		}

		public EscalatedException(String message, Throwable cause) {
			super(message, cause);
		}

		public EscalatedException(Throwable cause) {
			super(cause);
		}

		protected EscalatedException(String message, Throwable cause,
								   boolean enableSuppression,
								   boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}
	}
}
