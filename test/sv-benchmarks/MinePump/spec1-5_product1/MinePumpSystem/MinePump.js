class MinePump {
	constructor(env) {
		this.pumpRunning = false;	// Boolean
		this.systemActive = true;	// Boolean
		this.env = env;				// Environment
	}

	timeShift() {
		if (this.pumpRunning) this.env.lowerWaterLevel();
		if (this.systemActive) processEnvironment();
	}

	processEnvironment() { }

	activatePump() {
		this.pumpRunning = true;
	}

	isPumpRunning() {
		return this.pumpRunning;
	}

	deactivatePump() {
		this.pumpRunning = false;
	}

	isMethaneAlarm() {
		return this.env.isMethaneLevelCritical();
	}

	toString() {
		return "Pump(System:"
			+ (this.systemActive ? "On" : "Off")
			+ ",Pump:"
			+ (this.pumpRunning ? "On" : "Off")
			+ ") "
			+ this.env.toString();
	}

	getEnv() {
		return this.env;
	}

	stopSystem() {
		// feature not present
	}

	startSystem() {
		// feature not present
	}

	isSystemActive() {
		return this.systemActive;
	}
}
