class Actions {
	constructor() {
		this.env = new Environment();
		this.p = new MinePump();
		this.methAndRunningLastTime = false;	// Boolean
		this.switchedOnBeforeTS = false;		// Boolean
	}


	waterRise() {
		this.env.waterRise();
	}

	methaneChange() {
		this.env.changeMethaneLevel();
	}

	stopSystem() {
		if (this.p.isSystemActive()) this.p.stopSystem();
	}

	startSystem() {
		if (!this.p.isSystemActive()) this.p.startSystem();
	}

	timeShift() {
		if (this.p.isSystemActive()) this.Specification5_1();
		this.p.timeShift();
		if (this.p.isSystemActive()) {
			this.Specification1();
			this.Specification2();
			this.Specification3();
			this.Specification4();
			this.Specification5_2();
		}
	}

	getSystemState() {
		return this.p.toString();
	}

	// Specification 1 methan is Critical and pumping leads to Error
	Specification1() {
		var e = this.p.getEnv();
		var b1 = e.isMethaneLevelCritical();
		var b2 = this.p.isPumpRunning();

		if (b1 && b2) {
			throw "ERROR: Specification 1";
		}
	}

	// Specification 2: When the pump is running, and there is methane, then it is
	// in switched off at most 1 timesteps.
	Specification2() {
		var e = this.p.getEnv();
		var b1 = e.isMethaneLevelCritical();
		var b2 = this.p.isPumpRunning();

		if (b1 && b2) {
			if (this.methAndRunningLastTime) {
				throw "ERROR: Specification 2";
			} else {
				this.methAndRunningLastTime = true;
			}
		} else {
			this.methAndRunningLastTime = false;
		}
	}

	// Specification 3: When the water is high and there is no methane, then the
	// pump is on.
	Specification3() {
		var e = this.p.getEnv();
		var b1 = e.isMethaneLevelCritical();
		var b2 = this.p.isPumpRunning();
		var b3 = e.getWaterLevel() == WATER_LEVEL_ENUM_HIGH;

		if (!b1 && b3 && !b2) {
			throw "ERROR: Specification 3";
		}
	}

	// Specification 4: the pump is never on when the water level is low
	Specification4() {
		var e = this.p.getEnv();
		var b2 = this.p.isPumpRunning();
		var b3 = e.getWaterLevel() == WATER_LEVEL_ENUM_LOW;

		if (b3 && b2) {
			throw "ERROR: Specification 4";
		}
	}

	// Specification 5: The Pump is never switched on when the water is below the
	// highWater sensor.
	Specification5_1() {
		this.switchedOnBeforeTS = this.p.isPumpRunning();
	}

	// Specification 5: The Pump is never switched on when the water is below the
	// highWater sensor.
	Specification5_2() {
		var e = this.p.getEnv();
		var b1 = this.p.isPumpRunning();
		var b2 = e.getWaterLevel() != WATER_LEVEL_ENUM_HIGH;

		if ((b2) && (b1 && !switchedOnBeforeTS)) {
			throw "ERROR: Specification 5.2";
		}
	}
}
