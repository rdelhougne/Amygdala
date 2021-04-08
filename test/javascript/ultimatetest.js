/*
 *  If I can trace these variables, I can trace everything. (Probably)
 */

k = 1;
u = 34;

var complex_age = {
	age_john: u,
	age_joe: 43,
	age_marylin: 78
};

var person_john = {
	firstName: "John",
	lastName: "Doe",
	age: 42,
	eyeColor: "blue",
	age_offset: 18,
	set_age: function(age_local) {
		this.age = this.age_offset * age_local;
		//age = this.age_offset * age_local; //hier wird ein globales attribut gesetzt!! wtf!! -> stellt aber bisher kein problem dar!
	}
};

var ages = [12, 78, 66, complex_age, 43];

function age_adder(object, age_inc) {
	object.set_age(age_inc);
}

function person_management(index) {
	person_attribute_setter(person_john);
	c_a = ages[index];
	h = c_a.age_john;
	age_adder(person_john, h - k);
}

function person_attribute_setter(person_object) {
	person_object.age_offset = 5000;
}

person_management(3);

// e.g. if k = 2
if (person_john.age == 160000) {
	throw TypeError;
}

print("John is " + person_john.age + " years old, holy sh*t!");
//print(age); //wtfff
