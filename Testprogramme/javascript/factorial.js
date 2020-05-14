/*var k = 5;

var person = {
	firstName: "John",
	lastName : "Doe",
	id       : 5566,
	fullName : function() {
		return this.firstName + " " + this.lastName;
	}
};

var person2 = {
	firstName: "John2",
	lastName : "Doe2",
	id       : 5567,
	fullName : function() {
		return this.firstName + " " + this.lastName;
	}
};*/

function factorial(n) {
	if (n < 0)
		return null;
	if (n == 0)
		return 1;
	if (n == 1)
		return 1;
	var v = 1;
	while (0 != n) {
		v = v * n;
		n = n - 1;
	}
	return v;
}

/*var name = person.fullName();
var name2 = person2.fullName();*/
k = 5;
var res = factorial(k);
print("Fakultät: " + res);
/*print(inj);
inj = 84;
print(inj);
specint = 5;
print(specint);*/
