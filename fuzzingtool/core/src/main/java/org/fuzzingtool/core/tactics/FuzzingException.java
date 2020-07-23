package org.fuzzingtool.core.tactics;

public class FuzzingException {
	public static class NoMorePaths extends Exception {
		public NoMorePaths() {
			super("Can't get any more paths.");
		}
	}
}
