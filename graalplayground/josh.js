var person_josh = {
	age: 43,
	height: "150m"
};

person_josh.age = 23;

function setter(person_object) {
	person_object.age = 5000;
}

// Triggers caching
setter(person_josh);

if (person_josh.age == 160000) {
	throw TypeError;
}

print(person_josh.age);

/*function Car(make) {
	this.make = make;
}

car1 = new Car(2005);
car2 = new Car(1995);

print(car1.make);
print(car2.make);*/

