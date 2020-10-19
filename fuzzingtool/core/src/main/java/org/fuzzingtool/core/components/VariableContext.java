package org.fuzzingtool.core.components;

import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.fuzzingtool.core.symbolic.basic.SymbolicConstant;

import java.util.HashMap;
import java.util.Map;

public class VariableContext {
	private final Map<String, SymbolicNode> properties;

	public VariableContext() {
		this.properties = new HashMap<>();
	}

	public VariableContext(Map<String, SymbolicNode> map) {
		this.properties = map;
	}

	public SymbolicNode get(Object key) {
		return properties.getOrDefault(convertProperty(key), new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null));
	}

	public void set(Object key, SymbolicNode value) {
		properties.put(convertProperty(key), value);
	}

	public boolean hasProperty(Object key) {
		return properties.containsKey(convertProperty(key));
	}

	public void clear() {
		properties.clear();
	}

	public VariableContext copy() {
		return new VariableContext(new HashMap<>(this.properties));
	}

	private String convertProperty(Object key) {
		if (key instanceof Integer || key instanceof Long) {
			return String.valueOf(key);
		}
		if (key instanceof String) {
			return (String) key;
		}
		throw new IllegalArgumentException("VariableContext.convertProperty(): Cannot use property key with type '" + key.getClass().getSimpleName() + "'");
	}
}
