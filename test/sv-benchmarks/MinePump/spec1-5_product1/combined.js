var WATER_LEVEL_ENUM_LOW = 0;
var WATER_LEVEL_ENUM_NORMAL = 1;
var WATER_LEVEL_ENUM_HIGH = 2;

function Environment() {
	this.waterLevel = WATER_LEVEL_ENUM_LOW;	// Enum
	this.methaneLevelCritical = false;		// Boolean

	this.lowerWaterLevel = function () {
		if (this.waterLevel == WATER_LEVEL_ENUM_NORMAL) {
			this.waterLevel = WATER_LEVEL_ENUM_LOW;
		}
		if (this.waterLevel == WATER_LEVEL_ENUM_HIGH) {
			this.waterLevel = WATER_LEVEL_ENUM_NORMAL;
		}
	};

	this.waterRise = function () {
		if (this.waterLevel == WATER_LEVEL_ENUM_NORMAL) {
			this.waterLevel = WATER_LEVEL_ENUM_HIGH;
		}
		if (this.waterLevel == WATER_LEVEL_ENUM_LOW) {
			this.waterLevel = WATER_LEVEL_ENUM_NORMAL;
		}
	};

	this.changeMethaneLevel = function () {
		this.methaneLevelCritical = !this.methaneLevelCritical;
	};

	this.isMethaneLevelCritical = function () {
		return this.methaneLevelCritical;
	};

	this.toString = function () {
		return "Env(Water:" + this.waterLevel + ",Meth:" + (this.methaneLevelCritical ? "CRIT" : "OK") + ")";
	};

	this.isHighWaterSensorDry = function () {
		return this.waterLevel != WATER_LEVEL_ENUM_HIGH;
	  };

	this.getWaterLevel = function () {
		return this.waterLevel;
  };
}

function MinePump(env) {
	this.pumpRunning = false;	// Boolean
	this.systemActive = true;	// Boolean
	this.env = env;				// Environment

	this.timeShift = function () {
		if (this.pumpRunning) this.env.lowerWaterLevel();
		if (this.systemActive) this.processEnvironment();
	};

	this.processEnvironment__wrappee__base = function () {};

	this.processEnvironment__wrappee__highWaterSensor = function () {
    if (!this.pumpRunning && this.isHighWaterLevel()) {
      this.activatePump();
      this.processEnvironment__wrappee__base();
    } else {
      this.processEnvironment__wrappee__base();
    }
  };

  this.processEnvironment = function () {
    if (this.pumpRunning && this.isMethaneAlarm()) {
      this.deactivatePump();
    } else {
      this.processEnvironment__wrappee__highWaterSensor();
    }
  };

  this.isHighWaterLevel = function () {
    return !this.env.isHighWaterSensorDry();
  };

	this.activatePump = function () {
		this.pumpRunning = true;
	};

	this.isPumpRunning = function () {
		return this.pumpRunning;
	};

	this.deactivatePump = function () {
		this.pumpRunning = false;
	};

	this.isMethaneAlarm = function () {
		return this.env.isMethaneLevelCritical();
	};

	this.toString = function () {
		return "Pump(System:"
			+ (this.systemActive ? "On" : "Off")
			+ ",Pump:"
			+ (this.pumpRunning ? "On" : "Off")
			+ ") "
			+ this.env.toString();
	};

	this.getEnv = function () {
		return this.env;
	};

	this.stopSystem = function () {
		if (this.pumpRunning) {
			this.deactivatePump();
		  }
		  if (this.pumpRunning) {
			  throw "ERROR: stopSystem";
		  }
		  this.systemActive = false;
	};

	this.startSystem = function () {
		if (this.pumpRunning) {
			throw "ERROR: startSystem";
		}
		this.systemActive = true;
	};

	this.isSystemActive = function () {
		return this.systemActive;
	};
}

function Actions() {
	this.env = new Environment();
	this.p = new MinePump(this.env);
	this.methAndRunningLastTime = false;	// Boolean
	this.switchedOnBeforeTS = false;		// Boolean


	this.waterRise = function () {
		this.env.waterRise();
	};

	this.methaneChange = function () {
		this.env.changeMethaneLevel();
	};

	this.stopSystem = function () {
		if (this.p.isSystemActive()) this.p.stopSystem();
	};

	this.startSystem = function () {
		if (!this.p.isSystemActive()) this.p.startSystem();
	};

	this.timeShift = function () {
		if (this.p.isSystemActive()) this.Specification5_1();
		this.p.timeShift();
		if (this.p.isSystemActive()) {
			this.Specification1();
			this.Specification2();
			this.Specification3();
			this.Specification4();
			this.Specification5_2();
		}
	};

	this.getSystemState = function () {
		return this.p.toString();
	};

	// Specification 1 methan is Critical and pumping leads to Error
	this.Specification1 = function () {
		var e = this.p.getEnv();
		var b1 = e.isMethaneLevelCritical();
		var b2 = this.p.isPumpRunning();

		if (b1 && b2) {
			throw "ERROR: Specification 1";
		}
	};

	// Specification 2: When the pump is running, and there is methane, then it is
	// in switched off at most 1 timesteps.
	this.Specification2 = function () {
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
	};

	// Specification 3: When the water is high and there is no methane, then the
	// pump is on.
	this.Specification3 = function () {
		var e = this.p.getEnv();
		var b1 = e.isMethaneLevelCritical();
		var b2 = this.p.isPumpRunning();
		var b3 = e.getWaterLevel() == WATER_LEVEL_ENUM_HIGH;

		if (!b1 && b3 && !b2) {
			throw "ERROR: Specification 3";
		}
	};

	// Specification 4: the pump is never on when the water level is low
	this.Specification4 = function () {
		var e = this.p.getEnv();
		var b2 = this.p.isPumpRunning();
		var b3 = e.getWaterLevel() == WATER_LEVEL_ENUM_LOW;

		if (b3 && b2) {
			throw "ERROR: Specification 4";
		}
	};

	// Specification 5: The Pump is never switched on when the water is below the
	// highWater sensor.
	this.Specification5_1 = function () {
		this.switchedOnBeforeTS = this.p.isPumpRunning();
	};

	// Specification 5: The Pump is never switched on when the water is below the
	// highWater sensor.
	this.Specification5_2 = function () {
		var e = this.p.getEnv();
		var b1 = this.p.isPumpRunning();
		var b2 = e.getWaterLevel() != WATER_LEVEL_ENUM_HIGH;

		if ((b2) && (b1 && !this.switchedOnBeforeTS)) {
			throw "ERROR: Specification 5.2";
		}
	};
}








// spec1-5_product1

var cleanupTimeShifts = 42; // input
var num_steps = 42; // input

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
var action4_1 = false; // input
var action4_2 = false; // input
var action4_3 = false; // input
var action4_4 = false; // input

function randomSequenceOfActions() {
	var a = new Actions();

	simulate(a, action1_1, action1_2, action1_3, action1_4);
	simulate(a, action2_1, action2_2, action2_3, action2_4);
	simulate(a, action3_1, action3_2, action3_3, action3_4);
	simulate(a, action4_1, action4_2, action4_3, action4_4);

	while (num_steps > 0) {
		// "randomize"
		action1_1 = !action2_2 || action3_3;
		action1_2 = action2_1 && !action3_1;
		action1_3 = !action2_1 || action4_4;
		action1_4 = action1_1 && !action1_3;
		action2_1 = !action3_1 && action3_2;
		action2_2 = action1_1 || !action3_3;
		action2_3 = !action3_2 || action1_3;
		action2_4 = action2_1 && !action4_3;
		action3_1 = !action1_3 && action2_4;
		action3_2 = action2_3 || !action4_1;
		action3_3 = !action1_1 || action2_2;
		action3_4 = action3_1 && !action3_3;
		action4_1 = !action1_4 || action3_3;
		action4_2 = action2_4 && !action2_3;
		action4_3 = !action3_4 && action1_1;
		action4_4 = action3_3 || !action2_2;
		simulate(a, action1_1, action2_2, action3_3, action4_4);
		num_steps = num_steps - 1;
	}

	while (cleanupTimeShifts > 0) {
		a.timeShift();
		cleanupTimeShifts = cleanupTimeShifts - 1;
	}
}

function simulate(a, action1, action2, action3, action4) {
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

randomSequenceOfActions();
