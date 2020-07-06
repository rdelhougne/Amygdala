package org.fuzzingtool.components;

import org.fuzzingtool.symbolic.SymbolicNode;

import java.util.HashMap;

public class VariableContext {
	private final HashMap<String, SymbolicNode> contents = new HashMap<>();
	private final ContextType context_type;

	public VariableContext(ContextType context_type) {
		this.context_type = context_type;
	}

	public SymbolicNode getValue(String key) {
		if (contents.containsKey(key)) {
			return contents.get(key);
		} else {
			throw new IllegalArgumentException("Context does not contain attribute " + key);
		}
	}

	public boolean hasValue(String key) {
		return contents.containsKey(key);
	}

	public void setValue(String key, SymbolicNode value) {
		contents.put(key, value);
	}

	public ContextType getContextType() {
		return this.context_type;
	}

	public enum ContextType {
		OBJECT, FUNCTION_SCOPE
	}
}
