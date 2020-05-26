package org.fuzzingtool.symbolic;

public abstract class SymbolicNode {
    protected SymbolicNode[] children;
    public Type type;

    public abstract String toString();
    public abstract String toSMTExpr();

    public void addChildren(int expected, SymbolicNode... nodes) throws SymbolicException.WrongParameterSize {
        if (nodes.length != expected) {
            throw new SymbolicException.WrongParameterSize(nodes.length, 2, this.getClass().getSimpleName());
        } else {
            this.children = new SymbolicNode[expected];
            System.arraycopy(nodes, 0, this.children, 0, nodes.length);
        }
    }

    protected String encapsulate(String inner) {
        return "(" + inner + ")";
    }
}
