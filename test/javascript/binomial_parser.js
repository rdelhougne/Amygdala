function factorial(n) {
	if (n < 0) {
		return null;
	}
	if (n == 0) {
		return 1;
	}
	if (n == 1) {
		return 1;
	}
	var v = 1;
	while (n != 0) {
		v = v * n;
		n = n - 1;
	}
	return v;
}

function permutation(n, k) {
	return factorial(n) / factorial(n - k);
}

function combination(n, k) {
	return permutation(n, k) / factorial(k);
}

function binomial(n, k) {
	if (n < 0 || k < 0 || n < k) {
		throw 'Invalid values!';
	}
	return combination(n, k);
}

// Z3 stack smashing
/*var input_number_prefix = 1;
var input_number_shift = 3;
var input_userstring = "top:  4, bottom:1";
var input_both_combinations = true;

var top_index = input_userstring.indexOf("top") + 4;
var bottom_index = input_userstring.indexOf("bottom") + 7;

while(input_userstring.charAt(top_index) == " ") {
	top_index = top_index + 1;
}
while(input_userstring.charAt(bottom_index) == " ") {
	bottom_index = bottom_index + 1;
}
var index_top_to = input_userstring.indexOf(",", top_index);
var substr_length_top = index_top_to - top_index;
var number_top_substr = input_userstring.substr(top_index, substr_length_top);

var substr_length_bottom = input_userstring.length - bottom_index;
var number_bottom_substr = input_userstring.substr(bottom_index, substr_length_bottom);

var number_top = (input_number_prefix + number_top_substr) - input_number_shift;
var number_bottom = (input_number_prefix + number_bottom_substr) - input_number_shift;

print(number_top);
print(number_bottom);*/

// Z3 läuft nicht
/*var input_number_prefix = 1;
var input_number_shift = 3;
var input_userstring_top = "4";
var input_userstring_bottom = "5";
var input_both_combinations = true;

var number_top = (input_number_prefix + input_userstring_top) - input_number_shift;
var number_bottom = (input_number_prefix + input_userstring_bottom) - input_number_shift;*/

var input_number_shift = 3;
var input_n = 3;
var input_k = 4;
var input_sequence = "nk";
var input_invert = true;

var input_pos_n = input_sequence.indexOf("n");
var input_pos_k = input_sequence.indexOf("k");
/*if(input_sequence.charAt(input_pos_n + 1) == "s") {
	input_n = input_n + input_number_shift;
}
if(input_sequence.charAt(input_pos_k + 1) == "s") {
	input_k = input_k + input_number_shift;
}*/

/*try {
	var coefficient = 0;
	if (input_pos_n < input_pos_k) {
		if (input_invert) {
			coefficient = binomial(input_k, input_n);
		} else {
			coefficient = binomial(input_n, input_k);
		}
	} else {
		if (input_invert) {
			coefficient = binomial(input_n, input_k);
		} else {
			coefficient = binomial(input_k, input_n);
		}
	}
	print(coefficient);
} catch(ex) {
	print("Calculation not successful :(");
}*/

if (input_invert) {
	input_n = input_n - input_number_shift;
	input_k = input_k + input_number_shift;
} else {
	input_n = input_n + input_number_shift;
	input_k = input_k - input_number_shift;
}



try {
	var coefficient = 0;
	if (input_pos_n < input_pos_k) {
		coefficient = binomial(input_n, input_k);
	} else {
		coefficient = binomial(input_k, input_n);
	}
	print(coefficient);
} catch(ex) {
	print("Calculation not successful :(");
}
