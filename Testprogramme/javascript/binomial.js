function factorial(n) {
	if (n < 0) {
		return null;
	}
	if (n == 0) {
		return 1;
	}
	if (n == 1) {
		return 1;
	}
	var v = 1;
	while (n != 0) {
		v = v * n;
		n = n - 1;
	}
	return v;
}

function permutation(n, k) {
	return factorial(n) / factorial(n - k);
}

function combination(n, k) {
	return permutation(n, k) / factorial(k);
}

function binomial(n, k) {
	if (n < 0 || k < 0 || n < k) {
		throw 'Invalid values';
	}
	return combination(n, k);
}

var input_n = 2;
var input_k = 3;

print(binomial(input_n, input_k));
