var no_use = 13

var HomoSap = {
	a: 0.3029549426680,
	c: 0.1979883004921,
	g: 0.1975473066391,
	t: 0.3015094502008
}

function makeCumulative(table) {
	var last = null;
	for (var c in table) {
		if (last) table[c] += table[last];
		last = c;
	}
}

makeCumulative(HomoSap);
