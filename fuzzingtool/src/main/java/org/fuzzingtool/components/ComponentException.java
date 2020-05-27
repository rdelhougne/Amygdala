package org.fuzzingtool.components;

public class ComponentException {
    public static class InconsistentBranchingTreeException extends Exception {
        public InconsistentBranchingTreeException() {
            super("Node Connection Error"); // TODO
        }
    }
}
