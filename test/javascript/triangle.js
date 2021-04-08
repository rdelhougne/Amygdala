var a = 34;
var b = 42;
var c = 56;

/*function kkk() {
	print("abcde");
	a = 666;
	return false;
}*/

function triangle_exists(a, b, c) {
	if (a != 0 && b != 0 && c != 0 && (a + b + c) == 180) {
		if ((a + b) >= c || (b + c) >= a || (a + c) >= b) {
			return true;
		} else {
			return false;
		}
	} else {
		return false;
	}
}

function triangle_type(a, b, c) {
	if (a == b) {
		if (b == c) {
			return 'equilateral';
		} else {
			return 'isosceles';
		}
	} else {
		if (b == c) {
			return 'isosceles';
		} else {
			if (a == c) {
				return 'isosceles';
			} else {
				return 'scalene';
			}
		}
	}
}

if (triangle_exists(a, b, c)) {
	print(triangle_type(a, b, c));
}
