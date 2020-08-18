var const_bool = true;
var const_string_num = "34";
var const_string_num_infinity = "Infinity";
var const_string_random = "funnyfrisch";

var var_integer = 42;
var var_string = "house";

/*if (const_bool && true == var_integer) {
	print("Found me 1!");
}*/

if ("32" + const_string_num == var_integer) {
	print("Found me 2!");
}

if (const_bool + ", indeed" == var_string) {
	print("Found me 3!");
}
