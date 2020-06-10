package org.fuzzingtool.tactics;

public abstract class FuzzingTactic {
    public abstract void setOption(String option_name, Object value);
    public abstract Object getOption(String option_name);
}
