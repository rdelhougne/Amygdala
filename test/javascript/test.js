var glob = 8;

function adder(ta, n1, n2, n3, n4) {
	return ta + n1 + n2 + n3 + n4 + glob;
}

function test(n) {
	n = n + 1;
	n = adder(n, 1, 2, 3, 4);
	return n;
}

var res1 = test(3);
var res2 = test(5);
print("Test: " + res1);
