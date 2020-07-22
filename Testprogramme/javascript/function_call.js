var a = 2;
var b = 2;
var c = 3;

function hurricane(m, n, o=9, p="rock") {
	return m + n + o + p;
}

r = hurricane(a - 1, b, c);
print(r); // expected: '6rock'

