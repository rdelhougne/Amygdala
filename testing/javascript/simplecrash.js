'use strict';

function simplecrash(str_param) {
	if (str_param.length > 0 && str_param[0] == "b") {
		if (str_param.length > 1 && str_param[1] == "a") {
			if (str_param.length > 2 && str_param[2] == "d") {
				if (str_param.length > 3 && str_param[3] == "!") {
					throw new Error('Not good...')
				}
			}
		}
	}
}

simplecrash(process.argv[2]);
