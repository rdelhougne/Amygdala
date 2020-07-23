package org.fuzzingtool.core.tactics;

import org.fuzzingtool.core.components.VariableIdentifier;

import java.util.HashMap;

public abstract class FuzzingTactic {
	public abstract void setOption(String option_name, Object value);

	public abstract Object getOption(String option_name);

	public abstract String getTactic();

	public abstract HashMap<VariableIdentifier, Object> getNextValues() throws
			FuzzingException.NoMorePaths;
}
