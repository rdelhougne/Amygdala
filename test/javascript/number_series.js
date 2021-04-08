var n = 20;
var k = 6;

function printNumber(n, original, k, flag) {
	print(n);
	if (n <= 0) {
		if (flag == false) {
			flag = true;
		} else {
			flag = false;
		}
	}
	
	if (n == original && !flag) {
		return;
	}
	
	if (flag) {
		printNumber(n - k, original, k, flag);
	} else {
		printNumber(n + k, original, k, flag);
	}
}

printNumber(n, n, k, true);
