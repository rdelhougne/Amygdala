package org.fuzzingtool.symbolic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.SeqExpr;
import org.graalvm.collections.Pair;

public abstract class SymbolicNode {
	protected SymbolicNode[] children;
	protected LanguageSemantic languageSemantic;

	public static final boolean ENHANCED_CASTING = false;

	public final String toHRString() throws SymbolicException.NotImplemented {
		if (this.languageSemantic == LanguageSemantic.JAVASCRIPT) {
			return toHRStringJS();
		} else {
			throw new SymbolicException.NotImplemented("Only JavaScript semantic supported for now.");
		}
	}

	public final String toSMTExpr() throws SymbolicException.NotImplemented {
		if (this.languageSemantic == LanguageSemantic.JAVASCRIPT) {
			return toSMTExprJS();
		} else {
			throw new SymbolicException.NotImplemented("Only JavaScript semantic supported for now.");
		}
	}

	public abstract String toHRStringJS() throws SymbolicException.NotImplemented;

	public abstract String toSMTExprJS() throws SymbolicException.NotImplemented;

	/**
	 * This function returns the Z3-Solver data-structure for the given node
	 * and the given Z3-Context
	 *
	 * @param ctx The context to create the symbolic data-types
	 * @return A Pair: first slot is the complete symbolic data-structure, second slot is the type of the expression,
     * dependant on the language semantic of the node.
	 */
	public final Pair<Expr, ExpressionType> toZ3Expr(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		if (this.languageSemantic == LanguageSemantic.JAVASCRIPT) {
			return toZ3ExprJS(ctx);
		} else {
			throw new SymbolicException.NotImplemented("Only JavaScript semantic supported in Z3-expressions.");
		}
	}

	public abstract Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.UndecidableExpression,
			SymbolicException.NotImplemented;

	public void addChildren(int expected, SymbolicNode... nodes) throws SymbolicException.WrongParameterSize {
		if (nodes.length != expected) {
			throw new SymbolicException.WrongParameterSize(nodes.length, 2, this.getClass().getSimpleName());
		} else {
			this.children = new SymbolicNode[expected];
			System.arraycopy(nodes, 0, this.children, 0, nodes.length);
		}
	}

	/**
	 * This method performs a typecast with JavaScript semantic and the abilities of the Z3-Solver.
	 * The condition return.getRight() == goal is always satisfied, or an UndecidableExpression exception is thrown.
	 * This is likely to happen A LOT at this point.
	 * To further reduce the likelihood of UndecidableExpression to be thrown, the casting function
	 * tries to solve constant expressions and then manually convert them.
	 *
	 * @param ctx        The Z3-Context
	 * @param expression The expression to be casted.
	 * @param goal       The type of expression 'expression' is casted into
	 * @return The casted expression, or the same expression if the ExpressionTypes are already equal.
	 * @throws SymbolicException.UndecidableExpression Is thrown whenever the symbolic casting fails.
	 */
	public static Pair<Expr, ExpressionType> tryCastZ3JS(Context ctx, Pair<Expr, ExpressionType> expression,
                                                         ExpressionType goal) throws
			SymbolicException.UndecidableExpression { //TODO
		if (expression.getRight() == goal) {
			return expression;
		}

		if (expression.getRight() == ExpressionType.NUMBER_INTEGER) {
			if (goal == ExpressionType.STRING) {
				return Pair.create(ctx.intToString(expression.getLeft()), ExpressionType.STRING);
			}
		}

		if (expression.getRight() == ExpressionType.STRING) {
			//TODO evtl const überprüfen und NaN zurückgeben
			if (goal == ExpressionType.NUMBER_INTEGER) {
				return Pair.create(ctx.stringToInt(expression.getLeft()), ExpressionType.NUMBER_INTEGER);
			}
			if (goal == ExpressionType.BIGINT) {
				return Pair.create(ctx.stringToInt(expression.getLeft()), ExpressionType.BIGINT);
			}
		}

		if (expression.getRight() == ExpressionType.UNDEFINED) {
			if (goal == ExpressionType.STRING) {
				return Pair.create(ctx.mkString("undefined"), ExpressionType.STRING);
			}
		}

		if (expression.getRight() == ExpressionType.NULL) {
			if (goal == ExpressionType.STRING) {
				return Pair.create(ctx.mkString("null"), ExpressionType.STRING);
			}
		}

		if (expression.getRight() == ExpressionType.NUMBER_NAN) {
			if (goal == ExpressionType.STRING) {
				return Pair.create(ctx.mkString("NaN"), ExpressionType.STRING);
			}
		}

		if (expression.getRight() == ExpressionType.NUMBER_POS_INFINITY) {
			if (goal == ExpressionType.STRING) {
				return Pair.create(ctx.mkString("Infinity"), ExpressionType.STRING);
			}
		}

		if (expression.getRight() == ExpressionType.NUMBER_NEG_INFINITY) {
			if (goal == ExpressionType.STRING) {
				return Pair.create(ctx.mkString("-Infinity"), ExpressionType.STRING);
			}
		}

		throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
				expression.getRight().toString() + "' to '" + goal.toString() + "'.");
	}

	/**
	 * This method tries to convert an expression to a number format as in https://tc39.es/ecma262/2020/#sec-tonumber
	 *
	 * @param ctx        The Z3-Context
	 * @param expression The expression to be casted.
	 * @return The casted expression, or the same expression if the ExpressionTypes is already a number type
	 * @throws SymbolicException.UndecidableExpression Is thrown whenever the symbolic casting fails.
	 */
	public static Pair<Expr, ExpressionType> toNumericZ3JS(Context ctx, Pair<Expr, ExpressionType> expression) throws
			SymbolicException.UndecidableExpression { //TODO
		if (expression.getRight() == ExpressionType.UNDEFINED) {
			return Pair.create(null, ExpressionType.NUMBER_NAN);
		}

		if (expression.getRight() == ExpressionType.NULL) {
			return Pair.create(ctx.mkInt(0), ExpressionType.NUMBER_INTEGER);
		}

		if (expression.getRight() == ExpressionType.BOOLEAN) {
			if (ENHANCED_CASTING) {
				//TODO wenn möglich einen konstanten Ausdruck auflösen
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
						expression.getRight().toString() + "' to a Number type.");
			} else {
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
						expression.getRight().toString() + "' to a Number type.");
			}
		}

		if (expression.getRight() == ExpressionType.STRING) {
			if (ENHANCED_CASTING) {
				//TODO wenn möglich einen konstanten Ausdruck auflösen
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
						expression.getRight().toString() + "' to a Number type.");
			} else {
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
						expression.getRight().toString() + "' to a Number type.");
			}
		}

		if (expression.getRight() == ExpressionType.SYMBOL || expression.getRight() == ExpressionType.OBJECT) {
			throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
					expression.getRight().toString() + "' to a Number type.");
		}

		return expression;
	}

	/**
	 * This method tries to convert an expression to a Boolean as in https://tc39.es/ecma262/2020/#sec-toboolean
	 *
	 * @param ctx        The Z3-Context
	 * @param expression The expression to be casted.
	 * @return The casted expression, or the same expression if the ExpressionTypes is already a boolean
	 */
	public static Pair<Expr, ExpressionType> toBooleanZ3JS(Context ctx, Pair<Expr, ExpressionType> expression) throws
			SymbolicException.NotImplemented {
		switch (expression.getRight()) {
			case BOOLEAN:
				return expression;
			case STRING:
				return Pair.create(ctx.mkNot(ctx.mkEq(ctx.mkLength((SeqExpr) expression.getLeft()), ctx.mkInt(0))),
								   ExpressionType.BOOLEAN);
			case BIGINT:
			case NUMBER_INTEGER:
			case NUMBER_REAL:
				return Pair.create(ctx.mkNot(ctx.mkEq(expression.getLeft(), ctx.mkInt(0))), ExpressionType.BOOLEAN);
			case UNDEFINED:
			case NUMBER_NAN:
			case NULL:
				return Pair.create(ctx.mkFalse(), ExpressionType.BOOLEAN);
			case NUMBER_POS_INFINITY:
			case SYMBOL:
			case OBJECT:
			case NUMBER_NEG_INFINITY:
				return Pair.create(ctx.mkTrue(), ExpressionType.BOOLEAN);
			default:
				throw new SymbolicException.NotImplemented(
						"Method toBoolean not implemented for type '" + expression.getRight().toString() + "'.");
		}
	}

	/**
	 * This method tries to negate a numeric value.
	 *
	 * @param ctx        The Z3-Context
	 * @param expression The expression to be casted.
	 * @return The casted expression, or the same expression if the ExpressionTypes is already a number type
	 * @throws SymbolicException.UndecidableExpression Is thrown whenever the symbolic casting fails.
	 */
	public static Pair<Expr, ExpressionType> negateNumericZ3JS(Context ctx, Pair<Expr, ExpressionType> expression) throws
			SymbolicException.UndecidableExpression { //TODO
		switch (expression.getRight()) {
			case BIGINT:
				return Pair.create(ctx.mkUnaryMinus((ArithExpr) expression.getLeft()), ExpressionType.BIGINT);
			case NUMBER_INTEGER:
				return Pair.create(ctx.mkUnaryMinus((ArithExpr) expression.getLeft()), ExpressionType.NUMBER_INTEGER);
			case NUMBER_REAL:
				return Pair.create(ctx.mkUnaryMinus((ArithExpr) expression.getLeft()), ExpressionType.NUMBER_REAL);
			case NUMBER_NAN:
				return Pair.create(null, ExpressionType.NUMBER_NAN);
			case NUMBER_POS_INFINITY:
				return Pair.create(null, ExpressionType.NUMBER_NEG_INFINITY);
			case NUMBER_NEG_INFINITY:
				return Pair.create(null, ExpressionType.NUMBER_POS_INFINITY);
			default:
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot negate expression with type '" +
						expression.getRight().toString() + "'.");
		}
	}

	@SafeVarargs
	public static boolean checkTypeContains(ExpressionType expected, Pair<Expr, ExpressionType>... parameters) {
		for (Pair<Expr, ExpressionType> param: parameters) {
			if (param.getRight() == expected) {
				return true;
			}
		}
		return false;
	}

	@SafeVarargs
	public static boolean checkTypeAll(ExpressionType expected, Pair<Expr, ExpressionType>... parameters) {
		for (Pair<Expr, ExpressionType> param: parameters) {
			if (param.getRight() != expected) {
				return false;
			}
		}
		return true;
	}

	@SafeVarargs
	public static boolean checkTypeIsNumeric(Pair<Expr, ExpressionType>... parameters) {
		for (Pair<Expr, ExpressionType> param: parameters) {
			if (param.getRight() == ExpressionType.BOOLEAN || param.getRight() == ExpressionType.STRING ||
					param.getRight() == ExpressionType.UNDEFINED || param.getRight() == ExpressionType.NULL ||
					param.getRight() == ExpressionType.OBJECT || param.getRight() == ExpressionType.SYMBOL) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Encloses a string in parentheses.
	 *
	 * @param inner String to be enclosed
	 * @return "(" + inner + ")"
	 */
	protected String parentheses(String inner) {
		return "(" + inner + ")";
	}
}
