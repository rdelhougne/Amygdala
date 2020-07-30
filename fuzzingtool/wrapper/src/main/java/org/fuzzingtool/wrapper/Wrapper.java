package org.fuzzingtool.wrapper;

public class Wrapper {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("ERROR: Wrong number of options.");
			return;
		}
		Fuzzer fuzzer = new Fuzzer(args[0]);
		if (fuzzer.usable()) {
			fuzzer.fuzz();
			fuzzer.print_results();
		}
	}
}
