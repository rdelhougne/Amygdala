package org.fuzzingtool.components;

import org.fuzzingtool.symbolic.ExpressionType;

import java.util.Objects;

/**
 * This class is for identifying Variables in the program context.
 * For now it is basically "String", but should be replaced with information
 * like function scope, stack frame, parent object etc.
 * This class is immutable.
 */
public final class VariableIdentifier {
	private final ExpressionType var_type;
	private final String gid;

	public VariableIdentifier(ExpressionType variable_type, String unique_id) {
		Objects.requireNonNull(variable_type);
		this.var_type = variable_type;
		Objects.requireNonNull(unique_id);
		this.gid = unique_id;
	}

	/**
	 * This method provides a String that can be used by the SMT-Solvers.
	 *
	 * @return A String based upon all context information
	 */
	public String getIdentifierString() {
		StringBuilder builder = new StringBuilder();
		switch (var_type) {
			case BOOLEAN:
				builder.append("boolean_");
				break;
			case STRING:
				builder.append("string_");
				break;
			case BIGINT:
				builder.append("bigint_");
				break;
			case NUMBER_INTEGER:
				builder.append("integer_");
				break;
			case NUMBER_REAL:
				builder.append("real_");
				break;
			default:
				return "ERROR";
		}
		builder.append(this.gid);
		return builder.toString();
	}

	public String getGid() {
		return this.gid;
	}

	public ExpressionType getVariableType() {
		return this.var_type;
	}

	@Override
	public int hashCode() {
		return this.gid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof VariableIdentifier) {
			return this.gid.equals(((VariableIdentifier) obj).gid);
		}
		return false;
	}

	public static VariableIdentifier fromString(String representation) {
		String[] splitted = representation.split("_");
		if (splitted.length != 2) {
			throw new IllegalArgumentException("String representation '" + representation + "' of VariableIdentifier is invalid.");
		}
		ExpressionType id_type = null;
		switch (splitted[0]) {
			case "boolean":
				id_type = ExpressionType.BOOLEAN;
				break;
			case "string":
				id_type = ExpressionType.STRING;
				break;
			case "bigint":
				id_type = ExpressionType.BIGINT;
				break;
			case "integer":
				id_type = ExpressionType.NUMBER_INTEGER;
				break;
			case "real":
				id_type = ExpressionType.NUMBER_REAL;
				break;
			default:
				throw new IllegalArgumentException("String representation '" + representation + "' of VariableIdentifier is invalid.");
		}
		return new VariableIdentifier(id_type, splitted[1]);
	}
}
