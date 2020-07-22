/*
 * Playground for object modifications.
 */

var u = 15;

var person_john = {
	firstName: "John",
	lastName: "Doe",
	age: u,
	inc_age: function(inc) {
		this.age = this.age + inc;
	}
};

//person_john.age = 43;

person_john.inc_age(1);

if (person_john.age > 50) {
	print("John maybe is old, maybe.")
}

