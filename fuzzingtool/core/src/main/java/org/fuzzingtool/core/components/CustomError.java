package org.fuzzingtool.core.components;

import org.fuzzingtool.core.Logger;

/**
 * Class for managing user-defined errors.
 */
public class CustomError {
	private final Logger logger;

	// Options
	private boolean escalate_exceptions = false;

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
					if (escalate_exceptions) {
						logger.info("Custom error class '" + name + "' enabled.");
					} else {
						logger.info("Custom error class '" + name + "' disabled.");
					}
					break;
				default:
					logger.warning("CustomError: Unknown option '" + name + "'.");
			}
		} catch (ClassCastException cce) {
			logger.warning("CustomError: Cannot cast value for option '" + name + "'.");
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
			default:
				logger.warning("CustomError: Unknown option '" + name + "'.");
				return null;
		}
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
	 * @return Value of option "escalate_exceptions"
	 */
	public boolean escalateExceptions() {
		return escalate_exceptions;
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
