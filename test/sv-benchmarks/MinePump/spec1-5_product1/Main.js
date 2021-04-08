const cleanupTimeShifts = 2;

var action1_1 = false; // input
var action1_2 = false; // input
var action1_3 = false; // input
var action1_4 = false; // input
var action2_1 = false; // input
var action2_2 = false; // input
var action2_3 = false; // input
var action2_4 = false; // input
var action3_1 = false; // input
var action3_2 = false; // input
var action3_3 = false; // input
var action3_4 = false; // input

function randomSequenceOfActions() {
	var a = new Actions();

	simulate(action1_1, action1_2, action1_3, action1_4);
	simulate(action2_1, action2_2, action2_3, action2_4);
	simulate(action3_1, action3_2, action3_3, action3_4);

	while (cleanupTimeShifts > 0) {
		a.timeShift();
		cleanupTimeShifts = cleanupTimeShifts - 1;
	}
}

function simulate(action1, action2, action3, action4) {
	if (action3) {
		action4 = false;
	}

	if (action1) {
		a.waterRise();
	}

	if (action2) {
		a.methaneChange();
	}

	if (action3) {
		a.startSystem();
	} else if (action4) {
		a.stopSystem();
	}

	a.timeShift();
}