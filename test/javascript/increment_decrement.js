var wave = 5;

wave = wave++;
wave++;
wave = --wave;
wave = ++wave + wave--;
--wave;

print(wave);

// wave=7
if (wave == 15) {
	print("Found me!");
}

