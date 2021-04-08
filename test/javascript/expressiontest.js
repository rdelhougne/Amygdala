var a = 5;
var b = 6;
var c = "78";
var d = "lalala";
var e = null;
var f = NaN;
var ff = -Infinity;
var g = 3.0 //JSConstantIntegerNode
var h = 8.2 //JSConstantDoubleNode
var fruits = ['Apple', 'Banana', 'Kiwi', 'Melon'];
var i = fruits[2.0000000000000000000000000000000000000000000000000000000001]; //ReadElementNode: 2.0, 2 & 2.00...001 geht, 2.3 führt zu 'undefined'
print(i);
print(ff);

print(5 + {}); // Ergibt 5[object Object]
print(undefined + 4); // Ergibt NaN

if (a <= c) { //JSLessOrEqualNodeGen
	print("true 1");
}

if (a + g > h) {
	print("true 2");
}

if (a === c) { // JSIdenticalNodeGen
	print("true 3");
}
