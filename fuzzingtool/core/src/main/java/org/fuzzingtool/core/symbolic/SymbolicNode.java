package org.fuzzingtool.core.symbolic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.SeqExpr;
import com.microsoft.z3.enumerations.Z3_decl_kind;
import org.graalvm.collections.Pair;

import java.util.HashSet;
import java.util.Set;

public abstract class SymbolicNode {
	protected SymbolicNode[] children;
	protected LanguageSemantic language_semantic;
	private Pair<Expr, ExpressionType> cached_z3_expression = null;
	private String cached_hr_string = null;
	private String cached_smt_expression = null;

	public static boolean partial_evaluation_on_cast = false;

	public final String toHRString() throws SymbolicException.NotImplemented {
		if (this.language_semantic == LanguageSemantic.JAVASCRIPT) {
			if (this.cached_hr_string == null) {
				this.cached_hr_string = toHRStringJS();
			}
			return this.cached_hr_string;
		} else {
			throw new SymbolicException.NotImplemented("Only JavaScript semantic supported for now");
		}
	}

	public final String toSMTExpr() throws SymbolicException.NotImplemented {
		if (this.language_semantic == LanguageSemantic.JAVASCRIPT) {
			if (this.cached_smt_expression == null) {
				this.cached_smt_expression = toSMTExprJS();
			}
			return this.cached_smt_expression;
		} else {
			throw new SymbolicException.NotImplemented("Only JavaScript semantic supported for now");
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
		if (this.language_semantic == LanguageSemantic.JAVASCRIPT) {
			if (this.cached_z3_expression == null) {
				this.cached_z3_expression = toZ3ExprJS(ctx);
			}
			return this.cached_z3_expression;
		} else {
			throw new SymbolicException.NotImplemented("Only JavaScript semantic supported in Z3-expressions");
		}
	}

	public abstract Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.UndecidableExpression,
			SymbolicException.NotImplemented;

	public void addChildren(SymbolicNode... nodes) {
		this.children = new SymbolicNode[nodes.length];
		System.arraycopy(nodes, 0, this.children, 0, nodes.length);
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
			SymbolicException.UndecidableExpression {
		switch (expression.getRight()) {
			case BOOLEAN:
				if (partial_evaluation_on_cast) {
					return tryPartialEvaluationCastNumeric(ctx, expression);
				} else {
					throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
							expression.getRight().toString() + "' to a Number type");
				}
			case STRING:
				if (!containsVars(expression.getLeft())) {
					if (partial_evaluation_on_cast) {
						return tryPartialEvaluationCastNumeric(ctx, expression);
					} else {
						throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
							expression.getRight().toString() + "' to a Number type");
					}
				} else {
					return Pair.create(ctx.stringToInt(expression.getLeft()), ExpressionType.NUMBER_INTEGER);
				}
			case BIGINT:
			case NUMBER_INTEGER:
			case NUMBER_REAL:
			case NUMBER_NAN:
			case NUMBER_POS_INFINITY:
			case NUMBER_NEG_INFINITY:
				return expression;
			case UNDEFINED:
				return Pair.create(null, ExpressionType.NUMBER_NAN);
			case NULL:
				return Pair.create(ctx.mkInt(0), ExpressionType.NUMBER_INTEGER);
			default:
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
						expression.getRight().toString() + "' to a Number type");
		}
	}

	/**
	 * This method tries to convert an expression to a string as in https://tc39.es/ecma262/2020/#sec-tostring
	 *
	 * @param ctx        The Z3-Context
	 * @param expression The expression to be casted.
	 * @return The casted expression, or the same expression if the ExpressionTypes is already a string type
	 * @throws SymbolicException.UndecidableExpression Is thrown whenever the symbolic casting fails.
	 */
	public static Pair<Expr, ExpressionType> toStringZ3JS(Context ctx, Pair<Expr, ExpressionType> expression) throws
			SymbolicException.UndecidableExpression { //TODO

		switch (expression.getRight()) {
			case BOOLEAN:
				if (partial_evaluation_on_cast) {
					return tryPartialEvaluationCast(ctx, expression, ExpressionType.STRING);
				} else {
					throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
							expression.getRight().toString() + "' to a String type");
				}
			case STRING:
				return expression;
			case BIGINT:
			case NUMBER_INTEGER:
				return Pair.create(ctx.intToString(expression.getLeft()), ExpressionType.STRING);
			case UNDEFINED:
				return Pair.create(ctx.mkString("undefined"), ExpressionType.STRING);
			case NULL:
				return Pair.create(ctx.mkString("null"), ExpressionType.STRING);
			case NUMBER_NAN:
				return Pair.create(ctx.mkString("NaN"), ExpressionType.STRING);
			case NUMBER_POS_INFINITY:
				return Pair.create(ctx.mkString("Infinity"), ExpressionType.STRING);
			case NUMBER_NEG_INFINITY:
				return Pair.create(ctx.mkString("-Infinity"), ExpressionType.STRING);
			default:
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
						expression.getRight().toString() + "' to a String type");
		}
	}

	/**
	 * This method tries to convert an expression to an integer as in https://tc39.es/ecma262/2020/#sec-tointeger
	 *
	 * @param ctx        The Z3-Context
	 * @param expression The expression to be casted.
	 * @return The casted expression, or the same expression if the ExpressionTypes is already a integer type
	 * @throws SymbolicException.UndecidableExpression Is thrown whenever the symbolic casting fails.
	 */
	public static Pair<Expr, ExpressionType> toIntegerZ3JS(Context ctx, Pair<Expr, ExpressionType> expression) throws
			SymbolicException.UndecidableExpression {

		Pair<Expr, ExpressionType> exp_number = toNumericZ3JS(ctx, expression);
		switch (exp_number.getRight()) {
			case BIGINT:
			case NUMBER_INTEGER:
			case NUMBER_POS_INFINITY:
			case NUMBER_NEG_INFINITY:
				return exp_number;
			case NUMBER_REAL:
				// TODO
				return Pair.create(ctx.mkReal2Int((RealExpr) exp_number.getLeft()), ExpressionType.NUMBER_INTEGER);
			case NUMBER_NAN:
				return Pair.create(ctx.mkInt(0), ExpressionType.NUMBER_INTEGER);
			default:
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression of type '" +
						expression.getRight().toString() + "' to Integer. This is a possible modeling inconsistency!");
		}
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
						"Method toBoolean not implemented for type '" + expression.getRight().toString() + "'");
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
						expression.getRight().toString() + "'");
		}
	}

	/**
	 * This method tries to cast an expression to another type by evaluating
	 * the expression and converting the result. This only works if the expression
	 * is a constant expression. If the expression cannot be casted, an exception is thrown.
	 *
	 * @param ctx The Z3-Context
	 * @param expression The expression to be casted
	 * @param goal Type goal
	 * @return Casted expression
	 * @throws SymbolicException.UndecidableExpression Is thrown whenever the symbolic casting fails.
	 */
	public static Pair<Expr, ExpressionType> tryPartialEvaluationCast(Context ctx, Pair<Expr, ExpressionType> expression, ExpressionType goal) throws
			SymbolicException.UndecidableExpression {
		Expr old_expr = expression.getLeft();
		ExpressionType old_expr_type = expression.getRight();
		if (!containsVars(old_expr)) {
			Expr simplified_expr = old_expr.simplify();
			if (simplified_expr.isConst()) {
				switch (goal) {
					case STRING:
						if (simplified_expr.isBool()) {
							if (simplified_expr.isTrue()) {
								return Pair.create(ctx.mkString("true"), ExpressionType.STRING);
							}
							if (simplified_expr.isFalse()) {
								return Pair.create(ctx.mkString("false"), ExpressionType.STRING);
							}
						}
						break;
				}
				throw new SymbolicException.UndecidableExpression("Z3", "Casting from expression '" +
						simplified_expr.toString() + "' to '" + goal.name() + "' with partial evaluation not implemented");
			} else {
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression with type '" +
						old_expr_type.name() + "' to '" + goal.name() + "' with partial evaluation because it can not be simplified to a constant");
			}
		} else {
			throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression with type '" +
					old_expr_type.name() + "' to '" + goal.name() + "' with partial evaluation because it contains variables");
		}
	}

	/**
	 * This method tries to cast an expression to numeric type by evaluating
	 * the expression and converting the result. This only works if the expression
	 * is a constant expression. If the expression cannot be casted, an exception is thrown.
	 * https://tc39.es/ecma262/2020/#sec-tonumber
	 *
	 * @param ctx The Z3-Context
	 * @param expression The expression to be casted
	 * @return Casted expression
	 * @throws SymbolicException.UndecidableExpression Is thrown whenever the symbolic casting fails.
	 */
	public static Pair<Expr, ExpressionType> tryPartialEvaluationCastNumeric(Context ctx, Pair<Expr, ExpressionType> expression) throws
			SymbolicException.UndecidableExpression {
		Expr old_expr = expression.getLeft();
		ExpressionType old_expr_type = expression.getRight();
		if (!containsVars(old_expr)) {
			Expr simplified_expr = old_expr.simplify();
			if (simplified_expr.isConst()) {
				if (simplified_expr.isBool()) {
					if (simplified_expr.isTrue()) {
						return Pair.create(ctx.mkInt(1), ExpressionType.NUMBER_INTEGER);
					}
					if (simplified_expr.isFalse()) {
						return Pair.create(ctx.mkInt(0), ExpressionType.NUMBER_INTEGER);
					}
				} else if (simplified_expr.isString()) {
					// kind of https://tc39.es/ecma262/2020/#sec-tonumber-applied-to-the-string-type...
					String result = simplified_expr.getString().trim();
					if (result.equals("Infinity") || result.equals("+Infinity")) {
						return Pair.create(null, ExpressionType.NUMBER_POS_INFINITY);
					} else if (result.equals("-Infinity")) {
						return Pair.create(null, ExpressionType.NUMBER_NEG_INFINITY);
					} else {
						try {
							int int_rep = Integer.parseInt(result);
							return Pair.create(ctx.mkInt(int_rep), ExpressionType.NUMBER_INTEGER);
						} catch (NumberFormatException nfe) {
							// do nothing
						}
						try {
							Double double_rep = Double.valueOf(result);
							return Pair.create(ctx.mkReal(String.valueOf(double_rep)), ExpressionType.NUMBER_REAL);
						} catch (NumberFormatException nfe) {
							// do nothing
						}
					}
					return Pair.create(null, ExpressionType.NUMBER_NAN);
				} else {
					throw new SymbolicException.UndecidableExpression("Z3", "Casting from expression '" +
							simplified_expr.toString() + "' to a numeric value with partial evaluation not implemented");
				}
			} else {
				throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression with type '" +
						old_expr_type.name() + "' to a numeric value with partial evaluation because it can not be simplified to a constant");
			}
		} else {
			throw new SymbolicException.UndecidableExpression("Z3", "Cannot cast expression with type '" +
					old_expr_type.name() + "' to a numeric value with partial evaluation because it contains variables");
		}
		return Pair.create(null, ExpressionType.INTERNAL_ERROR);
	}

	/**
	 * Get a set of the variables in the expression.
	 * https://stackoverflow.com/questions/14080398/z3py-how-to-get-the-list-of-variables-from-a-formula
	 *
	 * @param expr Z3 expression
	 * @return A set of the variables
	 */
	public static Set<String> getVars(Expr expr) {
		Set<String> var_set = new HashSet<>();
		if (expr.isConst()) {
			if (expr.getFuncDecl().getDeclKind() == Z3_decl_kind.Z3_OP_UNINTERPRETED) {
				var_set.add(expr.toString());
			}
		} else {
			for (Expr arg_expr: expr.getArgs()) {
				var_set.addAll(getVars(arg_expr));
			}
		}
		return var_set;
	}

	/**
	 * Check if the provided expression contains any variables
	 *
	 * @param expr Z3 expression
	 * @return true if the expression contains any variables, false otherwise
	 */
	public static boolean containsVars(Expr expr) {
		if (expr.isConst()) {
			return expr.getFuncDecl().getDeclKind() == Z3_decl_kind.Z3_OP_UNINTERPRETED;
		} else {
			for (Expr arg_expr: expr.getArgs()) {
				if (containsVars(arg_expr)) {
					return true;
				}
			}
		}
		return false;
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
