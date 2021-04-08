var k = 3;

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
	if (n == 7) {
		throw TypeError;
	}
	var v = 1;
	var l = n + 1;
	while (0 != l) {
		v = v * l;
		l = l - 1;
	}
	return v;
}

var res = factorial(k);
print("Fakultät: " + res);
