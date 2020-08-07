nenner = 2;

function DivisionException(message) {
	this.message = message;
	this.name = "DivisionException";
}

function divide(n, m) {
	var l = 5;
	if (m == 0) {
		throw new DivisionException("Teilen durch Null nicht möglich");
	}

	return n / m;
}

try {
	var ausgabe = divide(5, nenner);
	print(ausgabe);
} catch (e) {
	print(e.name + ": " + e.message);
}
