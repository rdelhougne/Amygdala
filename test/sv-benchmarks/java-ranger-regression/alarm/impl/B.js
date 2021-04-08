/* Block signals for model 'ALARM_Functional' */
class B {
	constructor() {
		this.commandedFlowRate = 0;
		this.currentSystemMode = 0;
		this.disableAudio = 0;
		this.VTBIHigh = 0;
		this.flowRateHigh = 0;
		this.flowRateLow = 0;
		this.flowRate = 0;
		this.audioEnableDuration = 0;
		this.audioLevel = 0;
		this.scalingFactor2 = 0;
		this.lowReservoir = 0;
		this.maxDurationOverInfusion = 0;
		this.maxdurationunderinfusion = 0;
		this.maxPausedDuration = 0;
		this.maxIdleDuration = 0;
		this.toleranceMax = 0;
		this.toleranceMin = 0;
		this.reservoirVolume = 0;
		this.volumeInfused = 0;
		this.configTimer = 0;
		this.alarmOutDisplayAudioDisabledIndicator = 0; /* '<Root>/Alarm  Sub-System' */
		this.alarmOutDisplayNotificationCommand = 0; /* '<Root>/Alarm  Sub-System' */
		this.alarmOutAudioNotificationCommand = 0; /* '<Root>/Alarm  Sub-System' */
		this.alarmOutHighestLevelAlarm = 0; /* '<Root>/Alarm  Sub-System' */
		this.alarmOutLogMessageID = 0; /* '<Root>/Alarm  Sub-System' */
		this.systemOn = false;
		this.systemMonitorFailed = false;
		this.loggingFailed = false;
		this.infusionInitiate = false;
		this.notificationCancel = false;
		this.flowRateNotStable = false;
		this.airInLine = false;
		this.occlusion = false;
		this.doorOpen = false;
		this.temp = false;
		this.airPressure = false;
		this.humidity = false;
		this.batteryDepleted = false;
		this.batteryLow = false;
		this.batteryUnableToCharge = false;
		this.supplyVoltage = false;
		this.cpuInError = false;
		this.rtcInError = false;
		this.watchdogInterrupted = false;
		this.memoryCorrupted = false;
		this.pumpTooHot = false;
		this.pumpOverheated = false;
		this.reservoirEmpty = false;
		this.inTherapy = false;
	}
}
