var int_input_a = 3;
var int_input_b = 4;
var operation_input = "+";

function factorial(n) {
	if (n < 0) {
		throw 'Invalid values for factorial(n)!';
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
		throw 'Invalid value for binomial(n, k)!';
	}
	return combination(n, k);
}

function max(n, k) {
	if (n === k) {
		throw 'Numbers are equal!';
	}
	if (n > k) {
		return n;
	} else {
		return k;
	}
}

function min(n, k) {
	if (n === k) {
		throw 'Numbers are equal!';
	}
	if (n < k) {
		return n
	} else {
		return k;
	}
}

function power(base, exp) {
	if (exp < 0) {
		throw 'Invalid value for power(base, exp)!';
	}
	if (exp != 0) {
		return (base * power(base, exp - 1));
	} else {
		return 1;
	}
}

function recursive_gcd(n, k) {
	if (n < 1 || k < 1) {
		throw 'Invalid value for recursive_gcd(n, k)!';
	}
	if (k != 0) {
		return recursive_gcd(k, n % k);
	} else {
		return n;
	}
}

/*function gcd(n, k) {
	if ((n == 0 && k != 0) || (k == 0 && n != 0)) {
		throw 'Invalid value for gcd(n, k)!';
	}
	if (n < 0) {
		n = -n;
	}
	if (k < 0) {
		k = -k;
	}
	while(n != k) {
		if(n > k) {
			n = n - k;
		} else {
			k = k - n;
		}
	}
	return n;
}*/

function lcm(n, k) {
	if (n < 1 || k < 1) {
		throw 'Invalid value for lcm(n, k)!';
	}
	var max = 0;
	if (n > k) {
		max = n;
	} else {
		max = k;
	}
	while (true) {
		if (max % n == 0 && max % k == 0) {
			return max;
		}
		max++;
	}
}

function leap_year(year) {
	if (year % 400 == 0) {
		return true;
	} else if (year % 100 == 0) {
		return false;
	} else if (year % 4 == 0) {
		return true;
	} else {
		return false;
	}
}

function factors(n) {
	if (n < 1) {
		throw 'Invalid value for factors(n)!';
	}
	var sequence = ""
	for (var i = 1; i <= n; i++) {
		if (n % i == 0) {
			sequence = sequence + i + ","
		}
	}
	return sequence;
}

function digits(n) {
	var count = 0;
	while (n != 0) {
		count++;
		var remainder = n % 10;
		n = (n - remainder) / 10;
	}
	return count;
}

function digitsum(n) {
	var sum = 0;
	while (n != 0) {
		var remainder = n % 10;
		sum = sum + remainder;
		n = (n - remainder) / 10;
	}
	return sum;
}

function fibonacci(n) {
	if (n < 1) {
		throw 'Invalid value for fibonacci(n)!';
	}
	var sequence = ""
	var t1 = 0;
	var t2 = 1;
	for (var i = 1; i <= n; i++) {
		sequence = sequence + t1 + ",";
		nextTerm = t1 + t2;
		t1 = t2;
		t2 = nextTerm;
	}
	return sequence;
}

function all_primes(n, k) {
	if (n < 0 || k < 00 || n > k) {
		throw 'Invalid value for all_primes(n, k)!';
	}
	var sequence = ""
	for (var i = n; i < k; i++) {
		if (is_prime(i)) {
			sequence = sequence + i + ",";
		}
	}
	return sequence;
}

function is_prime(n) {
	var flag = true;
	for (var j = 2; j <= n / 2; j++) {
		if (n % j == 0) {
			flag = false;
			break;
		}
	}
	return flag;
}

function calculate(op, a, b) {
	if (op === "+") {
		return a + b;
	}
	if (op === "-") {
		return a - b;
	}
	if (op === "*") {
		return a * b;
	}
	if (op === "/") {
		return a / b;
	}
	if (op === "%") {
		return a % b;
	}
	if (op === "!") {
		return factorial(a);
	}
	if (op === "|") {
		return binomial(a, b);
	}
	if (op === "max") {
		return max(a, b);
	}
	if (op === "min") {
		return min(a, b);
	}
	if (op === "pow") {
		return power(a, b);
	}
	/*if (op === "gcd") {
		return gcd(a, b);
	}*/
	if (op === "rec_gcd") {
		return recursive_gcd(a, b);
	}
	if (op === "lcm") {
		return lcm(a, b);
	}
	if (op === "factors") {
		return factors(a);
	}
	if (op === "digits") {
		return digits(a);
	}
	if (op === "digitsum") {
		return digitsum(a);
	}
	if (op === "is_leap_year") {
		return leap_year(a);
	}
	if (op === "fib") {
		return fibonacci(a);
	}
	if (op === "is_prime") {
		return is_prime(a);
	}
	if (op === "all_primes") {
		return all_primes(a, b);
	}
}

var result = calculate(operation_input, int_input_a, int_input_b);
print("The result of operation " + operation_input + " is " + result);
