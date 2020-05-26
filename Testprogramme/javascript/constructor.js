function Car(make, model, year) {
	this.make = make;
	this.model = model;
	this.year = year;
	this.owner = function(name) {
		return name + " owns a beautiful " + model + " from " + year + ".";
	}
}

var k = 1;

car1 = new Car("Honda", "Civic", 2005);
car2 = new Car("Landrover", "Defender", 1995);

ownerstr1 = car1.owner("George");
ownerstr2 = car2.owner("Herbert");
ownerstr3 = car1.owner("Frank");

print(ownerstr2);

