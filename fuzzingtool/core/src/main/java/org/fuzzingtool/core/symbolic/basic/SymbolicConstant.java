package org.fuzzingtool.core.symbolic.basic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.oracle.truffle.js.nodes.JSGuards;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicException;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class SymbolicConstant extends SymbolicNode {
	private final ExpressionType constant_type;
	private final Object value;

	public SymbolicConstant(LanguageSemantic s, ExpressionType t, Object v) {
		Object new_value;
		ExpressionType new_type;
		this.language_semantic = s;

		try {
			switch (t) {
				case BOOLEAN:
					if (!JSGuards.isBoolean(v)) {
						throw new SymbolicException.IncompatibleType(t);
					}
					new_type = t;
					new_value = v;
					break;
				case STRING:
					if (!JSGuards.isString(v)) {
						throw new SymbolicException.IncompatibleType(t);
					}
					new_type = t;
					new_value = v;
					break;
				case BIGINT:
				case NUMBER_INTEGER:
					if (!JSGuards.isNumberInteger(v) && !JSGuards.isNumberLong(v)) {
						throw new SymbolicException.IncompatibleType(t);
					}
					new_type = t;
					new_value = v;
					break;
				case NUMBER_REAL:
					if (!JSGuards.isNumberDouble(v)) {
						throw new SymbolicException.IncompatibleType(t);
					}
					new_type = t;
					new_value = v;
					break;
				default:
					new_type = t;
					new_value = null;
					break;
			}
		} catch ( SymbolicException.IncompatibleType it) {
			new_type = ExpressionType.INTERNAL_ERROR;
			new_value = null;
			System.out.println("[CRITICAL] IncompatibleType exception occurred: " + it.getMessage());
		}
		this.constant_type = new_type;
		this.value = new_value;
	}

	@Override
	public String toHRStringJS() {
		switch (this.constant_type) {
			case BOOLEAN:
				return ((Boolean) this.value).toString();
			case STRING:
				return "\"" + this.value + "\"";
			case BIGINT:
			case NUMBER_INTEGER:
			case NUMBER_REAL:
				return String.valueOf(this.value);
			case UNDEFINED:
				return "undefined";
			case NULL:
				return "null";
			case NUMBER_NAN:
				return "NaN";
			case NUMBER_POS_INFINITY:
				return "Infinity";
			case NUMBER_NEG_INFINITY:
				return "-Infinity";
			case OBJECT:
				return "JS.Object";
			case SYMBOL:
				return "JS.Symbol";
			case INTERNAL_ERROR:
				return "(INTERNAL_ERROR)";
			default:
				return "(NOT SUPPORTED)";
		}
	}

	@Override
	public String toSMTExprJS() {
		return toHRStringJS(); //TODO
	}

	public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented,
			SymbolicException.UndecidableExpression {
		switch (this.constant_type) {
			case BOOLEAN:
				return Pair.create(ctx.mkBool((Boolean) this.value), ExpressionType.BOOLEAN);
			case STRING:
				return Pair.create(ctx.mkString((String) value), ExpressionType.STRING);
			case BIGINT:
				if (this.value instanceof Integer) {
					return Pair.create(ctx.mkInt((Integer) this.value), ExpressionType.BIGINT);
				} else {
					return Pair.create(ctx.mkInt(((Long) this.value).intValue()), ExpressionType.BIGINT);
				}
			case NUMBER_INTEGER:
				if (this.value instanceof Integer) {
					return Pair.create(ctx.mkInt((Integer) this.value), ExpressionType.NUMBER_INTEGER);
				} else {
					return Pair.create(ctx.mkInt(((Long) this.value).intValue()), ExpressionType.NUMBER_INTEGER);
				}
			case NUMBER_REAL:
				// https://stackoverflow.com/questions/11249894/precision-for-double-parsedouble-and-string-valueof
				return Pair.create(ctx.mkReal(String.valueOf(this.value)), ExpressionType.NUMBER_REAL);
			case UNDEFINED:
				return Pair.create(null, ExpressionType.UNDEFINED);
			case NULL:
				return Pair.create(null, ExpressionType.NULL);
			case NUMBER_NAN:
				return Pair.create(null, ExpressionType.NUMBER_NAN);
			case NUMBER_POS_INFINITY:
				return Pair.create(null, ExpressionType.NUMBER_POS_INFINITY);
			case NUMBER_NEG_INFINITY:
				return Pair.create(null, ExpressionType.NUMBER_NEG_INFINITY);
			case OBJECT:
				return Pair.create(null, ExpressionType.OBJECT);
			case SYMBOL:
				return Pair.create(null, ExpressionType.SYMBOL);
			case INTERNAL_ERROR:
				throw new SymbolicException.UndecidableExpression("Z3", "Internal error detected");
			default:
				throw new SymbolicException.NotImplemented("Unknown constant expression type");
		}
	}
}
