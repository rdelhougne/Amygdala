i = 5;

function Car(make, model, year) {
	this.make = make;
	this.model = model;
	this.year = year;
	this.owner = function(name) {
		return name + " owns a beautiful " + model + " from " + year + ".";
	}
}

car1 = new Car("Honda", "Civic", 2005);
car2 = new Car("Landrover", "Defender", 1995);

Car.prototype.country = i;

/*ownerstr1 = car1.owner("George");
ownerstr2 = car2.owner("Herbert");
ownerstr3 = car1.owner("Frank");

print(ownerstr2);
print(ownerstr3);*/

print(car1.country);
print(car2.country);

if (car2.country == 4) {
	print("Solved!");
}
