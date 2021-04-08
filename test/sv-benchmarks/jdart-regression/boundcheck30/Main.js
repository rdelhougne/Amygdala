/*
 * Contributed to SV-COMP by Falk Howar
 * License: MIT (see /java/jdart-regression/LICENSE-MIT)
 *
 */


function recursion(i) {
	if (i == 0) {
		return;
	}
	if (i > 0) {
		recursion(i - 1);
	}
	if (i < 0) {
		throw "ERROR: recursion()";
	}
}

function main() {
	var x = 42; // input
	if (x < 30 || x > 30) {
		return;
	}

	recursion(x);

	throw "ERROR: main()";
}

main();
