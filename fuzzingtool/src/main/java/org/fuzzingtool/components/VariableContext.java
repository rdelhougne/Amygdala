package org.fuzzingtool.components;

import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.basic.SymbolicConstant;

import java.util.HashMap;

public class VariableContext {
	private final HashMap<String, SymbolicNode> attribute_contents = new HashMap<>();
	private final HashMap<Integer, SymbolicNode> array_contents = new HashMap<>();
	private int array_length = 0;
	private final ContextType context_type;

	public VariableContext(ContextType context_type) {
		this.context_type = context_type;
	}

	public SymbolicNode getValue(String key) {
		if (context_type == ContextType.OBJECT || context_type == ContextType.FUNCTION_SCOPE) {
			if (attribute_contents.containsKey(key)) {
				return attribute_contents.get(key);
			} else {
				throw new IllegalArgumentException("Context does not contain attribute " + key);
			}
		}
		return null;
	}

	public boolean hasValue(String key) {
		if (context_type == ContextType.OBJECT || context_type == ContextType.FUNCTION_SCOPE) {
			return attribute_contents.containsKey(key);
		}
		return false;
	}

	public void setValue(String key, SymbolicNode value) {
		if (context_type == ContextType.OBJECT || context_type == ContextType.FUNCTION_SCOPE) {
			attribute_contents.put(key, value);
		}
	}

	public SymbolicNode getIndex(int array_index) {
		if (context_type == ContextType.ARRAY) {
			try {
				return array_contents.getOrDefault(array_index, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null));
			} catch (SymbolicException.IncompatibleType incompatibleType) {
				incompatibleType.printStackTrace();
			}
		}
		return null;
	}

	public void setIndex(int array_index, SymbolicNode value) {
		if (context_type == ContextType.ARRAY) {
			array_contents.put(array_index, value);
			if (array_index >= array_length) {
				array_length = array_index + 1;
			}
		}
	}

	public void appendValue(SymbolicNode value) {
		if (context_type == ContextType.ARRAY) {
			array_contents.put(array_length, value);
			array_length++;
		}
	}

	public int getLength() {
		if (context_type == ContextType.ARRAY) {
			return array_length;
		}
		return 0;
	}

	public ContextType getContextType() {
		return this.context_type;
	}

	public void clear() {
		attribute_contents.clear();
		array_contents.clear();
		array_length = 0;
	}

	public enum ContextType {
		OBJECT, FUNCTION_SCOPE, ARRAY
	}
}
