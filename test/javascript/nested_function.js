var u = 45;

function add() {
	var counter = 0;
	print(this);
	print(this.u);
	function plus() {
		var sinc = 42;
		function extra_plus() {
			counter += sinc;
		}
		extra_plus();
	}
	plus();
	//abc = counter; // hier wird ebenfalls das Attribut des globalen Objekts mit WritePropertyNode gesetzt
	return counter;
}

var o = add();

print(o);
