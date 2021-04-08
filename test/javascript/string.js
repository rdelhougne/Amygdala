/*
 * String operations test.
 * All operations from https://zyh1121.github.io/z3str3Docs/inputLanguage.html are supported.
 */

fruit = "apple";
product = "juice";
quantity = 3;

// concatenation
if (fruit + " " + product == "lime juice") {
	print("String concatenation 1 solved.");
}
if (fruit.concat(" ", product) == "peach ice-tea") {
	print("String concatenation 2 solved.");
}

// length attribute
if (fruit.length == 6) {
	print("String length solved.");
}

// char at index
if (product.charAt(12) == "S") {
	print("String charAt() solved.");
}
if (product[13] == "J") {
	print("String attribute operator solved.");
}

// Substring
/*if (fruit.slice(5, 9) == "kiwi") {
	print("Substring 1 solved.");
}
if (fruit.slice(-12, -6) == "Banana") {
	print("Substring 2 solved.");
}
if (fruit.slice(3) == "Apple") {
	print("Substring 3 solved.");
}
if (fruit.slice(-4) == "Melon") {
	print("Substring 4 solved.");
}

if (fruit.substring(1, 5) == "nana") {
	print("Substring 5 solved.");
}
if (fruit.substring(3) == "nanas") {
	print("Substring 6 solved.");
}*/

if (fruit.substr(6, 4) == "pple") {
	print("Substring 7 solved.");
}
/*if (fruit.substr(6) == "pples") {
	print("Substring 8 solved.");
}
if (fruit.substr(-3) == "lon") {
	print("Substring 9 solved.");
}*/

// Includes
/*if (product.includes("old")) {
	print("String includes() solved.");
}
if (product.includes("refurbished", 42)) { // Sometimes triggers segfault in z3lib.so
	print("String includes() solved.");
}*/

// Search
if (product.indexOf("new") == 6) {
	print("String indexOf() 1 solved.");
}
if (product.indexOf("new", 8) == 12) {
	print("String indexOf() 2 solved.");
}

// Integer String conversion
if ("Gerald buys " + quantity + " " + fruit + " " + product == "Gerald buys 8 Melon Smoothies") {
	print("Integer to String solved.");
}

print("The current product is " + fruit + " " + product + ", with quantity of " + quantity + ".");
