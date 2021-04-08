var k = 6;
var l = 8;

function ackermann(n, m) {
	while (n != 0) {
		if (m == 0) {
			m = 1;
		} else {
			m = ackermann(n, m - 1);
		}
		n = n - 1;
	}
	return m + 1;
}

var res = ackermann(k, l);
print("Ackermannfunktion: " + res);
