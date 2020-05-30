package org.fuzzingtool.tactics;

public class FuzzingException {
    public static class NoMorePaths extends Exception {
        public NoMorePaths() {
            super("Can't get any more paths.");
        }
    }
}
