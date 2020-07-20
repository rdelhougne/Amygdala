var k = 5;

function Artificial(start) {
    this.start = start;
    this.next = function() {
		start = start + 17;
		return start * 37;
	};
}

var generator = new Artificial(k);

generator.next();
generator.next();
generator.next();
generator.next();

// k = 6
if (generator.next() == 3367) {
	print("Found me!");
}

