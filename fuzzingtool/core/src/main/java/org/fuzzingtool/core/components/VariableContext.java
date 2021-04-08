/*
 * Copyright 2021 Robert Delhougne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fuzzingtool.core.components;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSRuntime;
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
		if (key instanceof DynamicObject) {
			return JSRuntime.toString(key);
		}
		throw new IllegalArgumentException("VariableContext.convertProperty(): Cannot use property key with type '" + key.getClass().getSimpleName() + "'");
	}
}
