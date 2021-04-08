/* Copyright, TU Dortmund 2020 Malte Mues
 * contributed-by: Malte Mues (mail.mues@gmail.com)
 *
 * This benchmark task is a modificaiton of the following original Benchmark:
 * Origin of the benchmark:
 *     license: MIT (see /java/jayhorn-recursive/LICENSE)
 *     repo: https://github.com/jayhorn/cav_experiments.git
 *     branch: master
 *     root directory: benchmarks/recursive
 * The benchmark was taken from the repo: 24 January 2018
 *
 * Following the original license model, modifications are as well licensed  under the
 * MIT license.
 */


function addition(m, n, c) {
	if (n == 0) {
		return m;
	}

	if (c >= 150) {
		throw "ERROR: addition()";
	}

	c = c + 1;
	if (n > 0) {
		return addition(m + 1, n - 1, c);
	} else {
		return addition(m - 1, n + 1, c);
	}
}

function main() {
	var m = 42; // input
	var n = 42; // input

	if (m < 0 || m >= 10000) {
		return;
	}
	if (n < 0 || n >= 10000) {
		return;
	}

	var c = 0;
	var result = addition(m, n, c);
	if (result != m + n) {
		throw "ERROR: main()";
	}
}

main();
