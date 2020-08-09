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
	private final ExpressionType constantType;
	private final Object value;

	public SymbolicConstant(LanguageSemantic s, ExpressionType t, Object v) throws SymbolicException.IncompatibleType {
		this.languageSemantic = s;
		this.constantType = t;

		switch (t) {
			case BOOLEAN:
				if (!JSGuards.isBoolean(v)) {
					throw new SymbolicException.IncompatibleType(t);
				}
				this.value = v;
				break;
			case STRING:
				if (!JSGuards.isString(v)) {
					throw new SymbolicException.IncompatibleType(t);
				}
				this.value = v;
				break;
			case BIGINT:
			case NUMBER_INTEGER:
				if (!JSGuards.isNumberInteger(v)) {
					throw new SymbolicException.IncompatibleType(t);
				}
				this.value = v;
				break;
			case NUMBER_REAL:
				if (!JSGuards.isNumberDouble(v)) {
					throw new SymbolicException.IncompatibleType(t);
				}
				this.value = v;
				break;
			default:
				this.value = null;
				break;
		}
	}

	@Override
	public String toHRStringJS() {
		switch (this.constantType) {
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
		switch (this.constantType) {
			case BOOLEAN:
				return Pair.create(ctx.mkBool((Boolean) this.value), ExpressionType.BOOLEAN);
			case STRING:
				return Pair.create(ctx.mkString((String) value), ExpressionType.STRING);
			case BIGINT:
				return Pair.create(ctx.mkInt((Integer) this.value), ExpressionType.BIGINT);
			case NUMBER_INTEGER:
				return Pair.create(ctx.mkInt((Integer) this.value), ExpressionType.NUMBER_INTEGER);
			case NUMBER_REAL:
				// https://stackoverflow.com/questions/11249894/precision-for-double-parsedouble-and-string-valueof
				return Pair.create(ctx.mkReal(String.valueOf((Double) this.value)), ExpressionType.NUMBER_REAL);
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
				throw new SymbolicException.UndecidableExpression("Z3", "Internal error detected.");
			default:
				throw new SymbolicException.NotImplemented("Unknown constant expression type.");
		}
	}
}
