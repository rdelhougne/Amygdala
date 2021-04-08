const WATER_LEVEL_ENUM_LOW = 0;
const WATER_LEVEL_ENUM_NORMAL = 1;
const WATER_LEVEL_ENUM_HIGH = 2;

class Environment {
	constructor() {
		this.waterLevel = WATER_LEVEL_ENUM_LOW;	// Enum
		this.methaneLevelCritical = false;		// Boolean
	}

	lowerWaterLevel() {
		switch (this.waterLevel) {
			case WATER_LEVEL_ENUM_HIGH:
				this.waterLevel = WATER_LEVEL_ENUM_NORMAL;
				break;
			case WATER_LEVEL_ENUM_NORMAL:
				this.waterLevel = WATER_LEVEL_ENUM_LOW;
				break;
		}
	}

	waterRise() {
		switch (this.waterLevel) {
			case WATER_LEVEL_ENUM_LOW:
				this.waterLevel = WATER_LEVEL_ENUM_NORMAL;
				break;
			case WATER_LEVEL_ENUM_NORMAL:
				this.waterLevel = WATER_LEVEL_ENUM_HIGH;
				break;
		}
	}

	changeMethaneLevel() {
		this.methaneLevelCritical = !this.methaneLevelCritical;
	}

	isMethaneLevelCritical() {
		return this.methaneLevelCritical;
	}

	toString() {
		return "Env(Water:" + this.waterLevel + ",Meth:" + (this.methaneLevelCritical ? "CRIT" : "OK") + ")";
	}

	getWaterLevel() {
		return this.waterLevel;
	}
}
