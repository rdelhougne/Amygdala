function AlarmOutputs() {

	this.isAudioDisabled = 42; // input
	this.notificationMessage = 42; // input
	this.audioNotificationCommand = 42; // input
	this.highestLevelAlarm = 42; // input
	this.logMessageId = 42; // input

}

/* Block signals for model 'ALARM_Functional' */
function B() {

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

function ConfigOutputs() {

	this.patientId = 42; // input
	this.drugName = 42; // input
	this.drugConcentration = 42; // input
	this.infusionTotalDuration = 42; // input
	this.vtbiTotal = 42; // input
	this.flowRateBasal = 42; // input
	this.flowRateIntermittentBolus = 42; // input
	this.durationIntermittentBolus = 42; // input
	this.intervalIntermittentBolus = 42; // input
	this.flowRatePatientBolus = 42; // input
	this.durationPatientBolus = 42; // input
	this.lockoutPeriodPatientBolus = 42; // input
	this.maxNumberOfPatientBolus = 42; // input
	this.flowRateKVO = 42; // input
	this.enteredReservoirVolume = 42; // input
	this.reservoirVolume = 42; // input
	this.configured = 42; // input
	this.errorMessageID = 42; // input
	this.requestConfigType = true; // input
	this.requestConfirmInfusionInitiate = true; // input
	this.requestPatientDrugInfo = true; // input
	this.requestInfusionInfo = true; // input
	this.logMessageID = 42; // input
	this.configTimer = 42; // input
	this.configMode = 42; // input

}

function DeviceConfigurationInputs() {

	this.audioEnableDuration = 42; // input
	this.audioLevel = 42; // input
	this.configWarningDuration = 42; // input
	this.emptyReservoir = 42; // input
	this.lowReservoir = 42; // input
	this.maxConfigDuration = 42; // input
	this.maxDurationOverInfusion = 42; // input
	this.maxDurationUnderInfusion = 42; // input
	this.maxPausedDuration = 42; // input
	this.maxIdleDuration = 42; // input
	this.toleranceMax = 42; // input
	this.toleranceMin = 42; // input
	this.logInterval = 42; // input
	this.systemTestInterval = 42; // input
	this.maxDisplayDuration = 42; // input
	this.maxConfirmStopDuration = 42; // input

}

function DeviceSensorInputs() {

	this.flowRate = 42; // input
	this.flowRateNotStable = true; // input
	this.airInLine = true; // input
	this.occlusion = true; // input
	this.doorOpen = true; // input
	this.temp = true; // input
	this.airPressure = true; // input
	this.humidity = true; // input
	this.batteryDepleted = true; // input
	this.batteryLow = true; // input
	this.batteryUnableToCharge = true; // input
	this.supplyVoltage = true; // input
	this.cpuInError = true; // input
	this.rtcInError = true; // input
	this.watchdogInterrupted = true; // input
	this.memoryCorrupted = true; // input
	this.pumpTooHot = true; // input
	this.pumpOverheated = true; // input
	this.pumpPrimed = true; // input
	this.postSuccessful = true; // input

}

var MAX_INT = 2147483647;
var MIN_INT = -2147483648;

function divS32(numerator, denominator) {
	var quotient; // int
	var tempAbsQuotient; // int
	var quotientNeedsNegation = false; // boolean
	if (denominator == 0) {
		if (numerator >= 0) {
			quotient = MAX_INT;
		} else {
			quotient = MIN_INT;
		}
		/* Divide by zero handler */
	} else {
		// quotientNeedsNegation = ((numerator < 0) != (denominator < 0));

		if ((numerator < 0) && (denominator > 0)) quotientNeedsNegation = true;
		else if ((numerator > 0) && (denominator < 0)) quotientNeedsNegation = true;
		else quotientNeedsNegation = false;

		var calcDenominator; // int

		/* replacing this computation
		tempAbsQuotient = (int) (numerator >= 0 ? numerator : -numerator) /
				(denominator >= 0 ? denominator : -denominator);*/

		if (denominator >= 0) calcDenominator = denominator;
		else calcDenominator = -denominator;

		if (numerator >= 0) {
			tempAbsQuotient = numerator / calcDenominator;
		} else {
			tempAbsQuotient = -numerator / calcDenominator;
		}

		if (quotientNeedsNegation) {
			quotient = -tempAbsQuotient;
		} else {
			quotient = tempAbsQuotient;
		}
	}
	return quotient;
}

function DrugDatabaseInputs() {

	this.knownPrescription = true; // input
	this.drugName = 42; // input
	this.drugConcentrationHigh = 42; // input
	this.drugConcentrationLow = 42; // input
	this.VTBIHigh = 42; // input
	this.VTBILow = 42; // input
	this.intervalPatientBolus = 42; // input
	this.numberMaxPatientBolus = 42; // input
	this.flowRateKVO = 42; // input
	this.flowRateHigh = 42; // input
	this.flowRateLow = 42; // input

}

/* Block states (auto storage) for model 'ALARM_Functional' */
function DW() {

	this.isActiveC2AlarmFunctional = 0; /* '<Root>/Alarm  Sub-System' */
	this.isC2AlarmFunctional = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveNotification = 0; /* '<Root>/Alarm  Sub-System' */
	this.isVisual = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveVisual = 0; /* '<Root>/Alarm  Sub-System' */
	this.isAudio = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveAudio = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveCheckAlarm = 0; /* '<Root>/Alarm  Sub-System' */
	this.isCancelAlarm = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveCancelAlarm = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveSetAlarmStatus = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveLevel4 = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsEmptyReservoir = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsEmptyReservoir = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsSystemMonitorFailed = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsSystemMonitorFailed = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsEnviromentalError = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsEnviromentalError = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveLevel3 = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsOverInfusionFlowRate = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsOverInfusionFlowRate = 0; /* '<Root>/Alarm  Sub-System' */
	this.isInfusionNotStartedWarning = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveInfusionNotStartedWarning = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsOverInfusionVTBI = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsOverInfusionVTBI = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsAirInLine = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsAirInLine = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsOcclusion = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsOcclusion = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsDoorOpen = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsDoorOpen = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveLevel2 = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsLowReservoir = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsLowReservoir = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveLevel1 = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsUnderInfusion = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsUnderInfusion = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsFlowRateNotStable = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsFlowRateNotStable = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsIdleTimeExceeded = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsIdleTimeExceeded = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsPausedTimeExceeded = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsPausedTimeExceeded = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsConfigTimeWarning = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsConfigTimeWarning = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsBatteryError = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsBatteryError = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsPumpHot = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsPumpHot = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsLoggingFailed = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsLoggingFailed = 0; /* '<Root>/Alarm  Sub-System' */
	this.isIsHardwareError = 0; /* '<Root>/Alarm  Sub-System' */
	this.isActiveIsHardwareError = 0; /* '<Root>/Alarm  Sub-System' */
	this.overInfusionTimer = 0; /* '<Root>/Alarm  Sub-System' */
	this.underInfusionTimer = 0; /* '<Root>/Alarm  Sub-System' */
	this.currentAlarm = 0; /* '<Root>/Alarm  Sub-System' */
	this.audioTimer = 0; /* '<Root>/Alarm  Sub-System' */
	this.cancelAlarm = 0; /* '<Root>/Alarm  Sub-System' */
	this.maxAlarmLevel = 0; /* '<Root>/Alarm  Sub-System' */
	this.idletimer = 0; /* '<Root>/Alarm  Sub-System' */
	this.pausedtimer = 0; /* '<Root>/Alarm  Sub-System' */

}

function InfusionManagerOutputs() {

	this.commandedFlowRate = 42; // input
	this.currentSystemMode = 42; // input
	this.newInfusion = true; // input
	this.logMessageId = 42; // input
	this.actualInfusionDuration = 42; // input

}

function LogOutput() {

	this.log = 42; // input
	this.loggingFailed = true; // input

}

function OperatorCommands() {

	this.systemStart = true; // input
	this.systemStop = true; // input
	this.Infusion_Initiate = true; // input
	this.infusionInhibit = true; // input
	this.infusionCancel = true; // input
	this.dataConfig = true; // input
	this.next = true; // input
	this.back = true; // input
	this.cancel = true; // input
	this.keyboard = true; // input
	this.disableAudio = 42; // input
	this.notificationCancel = true; // input
	this.configurationType = 42; // input
	this.confirmStop = true; // input

}

function SystemMonitorOutput() {

	this.systemMonitorFailed = true; // input

}

function SystemStatusOutputs() {

	this.Reservoir_Empty = true; // input
	this.reservoirVolume = 42; // input
	this.volumeInfused = 42; // input
	this.logMessageID = 42; // input
	this.inTherapy = true; // input

}

function TopLevelModeOutputs() {

	this.systemOn = true; // input
	this.requestConfirmStop = true; // input
	this.logMessageId = 42; // input

}





























var IN_ALARM_DISPLAY = 1;
var IN_ALARMS = 1;
var IN_CHECK = 1;
var IN_DISABLED = 1;
var IN_MONITOR = 2;
var IN_NOT_ON = 2;
var IN_NO_ACTIVE_CHILD = 0;
var IN_NO = 1;
var IN_OFF = 2;
var IN_OFF_I = 1;
var IN_ON = 3;
var IN_ON_A = 2;
var IN_SILENCED = 4;
var IN_YES = 2;
var IN_YES_O = 3;
var IN_COUNTING = 3;


function writeLog(logEvent, localB) {
	/* Graphical Function 'writeLog': '<S1>:1478' */
	/* Transition: '<S1>:1480' */
	localB.alarmOutLogMessageID = logEvent;
}

function checkOverInfusionFlowRate(localB) {
	var ov;

	/* Graphical Function 'checkOverInfusionFlowRate': '<S1>:4055' */
	/* Transition: '<S1>:4061' */
	ov = 0;
	if (localB.inTherapy) {
		/* Transition: '<S1>:4062' */
		var div1 = divS32(localB.toleranceMax, 100);
		var div2 = divS32(localB.toleranceMin, 100);

		var commandedFlowRate = localB.commandedFlowRate;
		var flowRate = localB.flowRate;

		if (localB.flowRate > localB.flowRateHigh) {
			/* Transition: '<S1>:4063' */
			ov = 1;
		} else if (flowRate > commandedFlowRate * div1 + commandedFlowRate) {
			/* Transition: '<S1>:4064' */
			ov = 1;
		} else {
			if (flowRate > commandedFlowRate * div2 + commandedFlowRate) {
				/* Transition: '<S1>:4065' */
				ov = 2;
			}
		}
	}

	// ov stands for OverInfusion
	return ov;
}

function stepScalingFactor(inputVal) {
	/* Graphical Function 'Step_Scaling_Factor': '<S1>:4730' */
	/* Transition: '<S1>:4732' */
	return inputVal;
}

/* Function for Chart: '<Root>/Alarm  Sub-System' */
function checkUnderInfusion(localB) {
	var c;

	/* Graphical Function 'checkUnderInfusion': '<S1>:4130' */
	/* Transition: '<S1>:4137' */
	c = 0;
	if (localB.inTherapy) {
		/* Transition: '<S1>:4139' */
		var div1 = divS32(localB.toleranceMax, 100);
		var div2 = divS32(localB.toleranceMin, 100);
		var flowRate = localB.flowRate;
		var commandedFlowRate = localB.commandedFlowRate;

		if (localB.flowRate < localB.flowRateLow) {
			/* Transition: '<S1>:4138' */
			c = 1;
		} else if (flowRate < commandedFlowRate - commandedFlowRate * div1) {
			/* Transition: '<S1>:4140' */
			c = 1;
		} else {
			if (flowRate < commandedFlowRate - commandedFlowRate * div2) {
				/* Transition: '<S1>:4142' */
				c = 2;
			}
		}
	}

	return c;
}

function level1(localB, localDW) {
	var underInfusion;

	/* During 'Level1': '<S1>:4113' */
	/* During 'InfusionNotStartedWarning': '<S1>:4577' */
	if (localDW.isInfusionNotStartedWarning == IN_NO) {
		/* During 'No': '<S1>:4583' */
		if (localB.infusionInitiate && (!localB.reservoirEmpty)) {
			/* Transition: '<S1>:4580' */
			localDW.isInfusionNotStartedWarning = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4582' */
		if (localB.infusionInitiate && (!localB.reservoirEmpty)) {
			/* Transition: '<S1>:4707' */
			localDW.isInfusionNotStartedWarning = IN_YES;
		} else {
			/* Transition: '<S1>:4581' */
			localDW.isInfusionNotStartedWarning = IN_NO;
		}
	}

	/* During 'IsUnderInfusion': '<S1>:4114' */
	underInfusion = checkUnderInfusion(localB);

	if (localDW.isIsUnderInfusion == IN_CHECK) {
		/* During 'Check': '<S1>:4127' */
		if (underInfusion == 1) {
			/* Transition: '<S1>:4119' */
			localDW.isIsUnderInfusion = IN_YES_O;
			/* Entry 'Yes': '<S1>:4126' */
		} else {
			if (underInfusion == 2) {
				/* Transition: '<S1>:4121' */
				localDW.underInfusionTimer = 0;
				localDW.isIsUnderInfusion = IN_MONITOR;
				/* Entry 'Monitor': '<S1>:4128' */
			}
		}
	} else if (localDW.isIsUnderInfusion == IN_MONITOR) {
		/* During 'Monitor': '<S1>:4128' */
		var scalingFactor1 = stepScalingFactor(localB.maxdurationunderinfusion);
		if ((underInfusion == 1) || (localDW.underInfusionTimer > scalingFactor1)) {
			/* Transition: '<S1>:4122' */
			localDW.underInfusionTimer = 0;
			localDW.isIsUnderInfusion = IN_YES_O;

			/* Entry 'Yes': '<S1>:4126' */
		} else if (underInfusion == 2) {
			/* Transition: '<S1>:4124' */
			localDW.underInfusionTimer++;
			localDW.isIsUnderInfusion = IN_MONITOR;

			/* Entry 'Monitor': '<S1>:4128' */
		} else {
			if (underInfusion == 0) {
				/* Transition: '<S1>:4118' */
				localDW.underInfusionTimer = 0;
				localDW.isIsUnderInfusion = IN_CHECK;

				/* Entry 'Check': '<S1>:4127' */
			}
		}
	} else {
		/* During 'Yes': '<S1>:4126' */
		if (localDW.cancelAlarm == 10) {
			/* Transition: '<S1>:4502' */
			localDW.isIsUnderInfusion = IN_CHECK;

			/* Entry 'Check': '<S1>:4127' */
		}
	}

	/* During 'IsFlowRateNotStable': '<S1>:4143' */
	if (localDW.isIsFlowRateNotStable == IN_NO) {
		/* During 'No': '<S1>:4148' */
		if (localB.inTherapy && localB.flowRateNotStable) {
			/* Transition: '<S1>:4145' */
			localDW.isIsFlowRateNotStable = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4147' */
		if ((localDW.cancelAlarm == 11) && (!localB.flowRateNotStable)) {
			/* Transition: '<S1>:4146' */
			localDW.isIsFlowRateNotStable = IN_NO;
		}
	}

	/* During 'IsIdleTimeExceeded': '<S1>:4149' */

	if (localDW.isIsIdleTimeExceeded == IN_NO) {
		/* During 'No': '<S1>:4153' */
		var scalingFactor = stepScalingFactor(localB.maxIdleDuration);
		if ((localB.currentSystemMode == 1) && (scalingFactor == 1)) {
			/* Transition: '<S1>:4750' */
			/* Exit 'No': '<S1>:4153' */
			localDW.idletimer = 0;
			localDW.isIsIdleTimeExceeded = IN_YES;
		} else if (localB.currentSystemMode == 1) {
			/* Transition: '<S1>:4746' */
			/* Exit 'No': '<S1>:4153' */
			localDW.idletimer = 0;
			localDW.isIsIdleTimeExceeded = IN_COUNTING;

			/* Entry 'counting': '<S1>:4745' */
			localDW.idletimer++;
		} else {
			localDW.idletimer = 0;
		}
	} else if (localDW.isIsIdleTimeExceeded == IN_YES) {
		/* During 'Yes': '<S1>:4154' */
		if (localDW.cancelAlarm == 12) {
			/* Transition: '<S1>:4152' */
			localDW.isIsIdleTimeExceeded = IN_NO;

			/* Entry 'No': '<S1>:4153' */
			localDW.idletimer = 0;
		}
	} else {
		/* During 'counting': '<S1>:4745' */
		var scalingFactor = stepScalingFactor(localB.maxIdleDuration);
		if (localDW.idletimer >= scalingFactor) {
			/* Transition: '<S1>:4747' */
			/* Exit 'counting': '<S1>:4745' */
			localDW.idletimer++;
			localDW.isIsIdleTimeExceeded = IN_YES;
		} else {
			localDW.idletimer++;
		}
	}

	/* During 'IsPausedTimeExceeded': '<S1>:4155' */

	if (localDW.isIsPausedTimeExceeded == IN_NO) {
		/* During 'No': '<S1>:4756' */

		var currentSystemModel = localB.currentSystemMode;
		var scalingFactor = stepScalingFactor(localB.maxPausedDuration);

		if (((currentSystemModel == 6) || (currentSystemModel == 7) || (currentSystemModel == 8))
			&& (scalingFactor == 1)) {
			/* Transition: '<S1>:4761' */
			/* Exit 'No': '<S1>:4756' */
			localDW.pausedtimer = 0;
			localDW.isIsPausedTimeExceeded = IN_YES;
		} else if ((currentSystemModel == 6)
			|| (currentSystemModel == 7)
			|| (currentSystemModel == 8)) {
			/* Transition: '<S1>:4757' */
			/* Exit 'No': '<S1>:4756' */
			localDW.pausedtimer = 0;
			localDW.isIsPausedTimeExceeded = IN_COUNTING;

			/* Entry 'counting': '<S1>:4752' */
			localDW.pausedtimer++;
		} else {
			localDW.pausedtimer = 0;
		}
	} else if (localDW.isIsPausedTimeExceeded == IN_YES) {
		/* During 'Yes': '<S1>:4755' */
		if (localDW.cancelAlarm == 13) {
			/* Transition: '<S1>:4754' */
			localDW.isIsPausedTimeExceeded = IN_NO;

			/* Entry 'No': '<S1>:4756' */
			localDW.pausedtimer = 0;
		}
	} else {
		/* During 'counting': '<S1>:4752' */
		var scalingFactor2 = stepScalingFactor(localB.maxPausedDuration);

		if (localDW.pausedtimer >= scalingFactor2) {
			/* Transition: '<S1>:4758' */
			/* Exit 'counting': '<S1>:4752' */
			localDW.pausedtimer++;
			localDW.isIsPausedTimeExceeded = IN_YES;
		} else {
			localDW.pausedtimer++;
		}
	}

	var scalingFactor = stepScalingFactor(localB.scalingFactor2);
	/* During 'IsConfigTimeWarning': '<S1>:4161' */
	if (localDW.isIsConfigTimeWarning == IN_NO) {
		/* During 'No': '<S1>:4166' */
		if (localB.configTimer > scalingFactor) {
			/* Transition: '<S1>:4163' */
			localDW.isIsConfigTimeWarning = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4165' */
		var scalingFactor2 = stepScalingFactor(localB.scalingFactor2);
		var cancelAlarml = localDW.cancelAlarm;
		var Config_Timerl = localB.configTimer;
		if ((cancelAlarml == 14) && (!(Config_Timerl > scalingFactor2))) {
			/* Transition: '<S1>:4164' */
			localDW.isIsConfigTimeWarning = IN_NO;
		}
	}

	/* During 'IsBatteryError': '<S1>:4167' */
	if (localDW.isIsBatteryError == IN_NO) {
		/* During 'No': '<S1>:4172' */
		var batteryLow = localB.batteryLow;
		var batteryUnableToCharge = localB.batteryUnableToCharge;
		var supplyVoltage = localB.supplyVoltage;

		if (batteryLow || batteryUnableToCharge || supplyVoltage) {
			/* Transition: '<S1>:4169' */
			localDW.isIsBatteryError = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4171' */
		var cancelAlarml = localDW.cancelAlarm;
		var batteryLow = localB.batteryLow;
		var batteryUnableToCharge = localB.batteryUnableToCharge;
		var supplyVoltage = localB.supplyVoltage;

		if ((cancelAlarml == 15) && (!(batteryLow || batteryUnableToCharge || supplyVoltage))) {
			/* Transition: '<S1>:4170' */
			localDW.isIsBatteryError = IN_NO;
		}
	}

	/* During 'IsPumpHot': '<S1>:4173' */
	if (localDW.isIsPumpHot == IN_NO) {
		/* During 'No': '<S1>:4178' */
		if (localB.pumpOverheated) {
			/* Transition: '<S1>:4175' */
			localDW.isIsPumpHot = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4177' */
		if ((localDW.cancelAlarm == 16) && (!localB.pumpOverheated)) {
			/* Transition: '<S1>:4176' */
			localDW.isIsPumpHot = IN_NO;
		}
	}

	/* During 'IsLoggingFailed': '<S1>:4179' */
	if (localDW.isIsLoggingFailed == IN_NO) {
		/* During 'No': '<S1>:4184' */
		if (localB.loggingFailed) {
			/* Transition: '<S1>:4181' */
			localDW.isIsLoggingFailed = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4183' */
		if ((localDW.cancelAlarm == 17) && (!localB.loggingFailed)) {
			/* Transition: '<S1>:4182' */
			localDW.isIsLoggingFailed = IN_NO;
		}
	}

	/* During 'IsSystemMonitorFailed': '<S1>:4185' */
	if (localDW.isIsSystemMonitorFailed == IN_NO) {
		/* During 'No': '<S1>:4190' */
		if (localB.systemMonitorFailed) {
			/* Transition: '<S1>:4187' */
			localDW.isIsSystemMonitorFailed = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4189' */
		if ((localDW.cancelAlarm == 18) && (!localB.systemMonitorFailed)) {
			/* Transition: '<S1>:4188' */
			localDW.isIsSystemMonitorFailed = IN_NO;
		}
	}
}

/* Function for Chart: '<Root>/Alarm  Sub-System' */
function setCurrentAlarm(localDW) {
	var s;

	/* Graphical Function 'setCurrentAlarm': '<S1>:3955' */
	/* Transition: '<S1>:3975' */
	s = 0;
	localDW.maxAlarmLevel = 1;
	if (localDW.isInfusionNotStartedWarning == IN_YES) {
		/* Transition: '<S1>:4593' */
		s = 19;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:4591' */
	}

	if (localDW.isIsSystemMonitorFailed == IN_YES) {
		/* Transition: '<S1>:3976' */
		s = 18;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3977' */
	}

	if (localDW.isIsLoggingFailed == IN_YES) {
		/* Transition: '<S1>:3979' */
		s = 17;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3978' */
	}

	if (localDW.isIsPumpHot == IN_YES) {
		/* Transition: '<S1>:3981' */
		s = 16;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3980' */
	}

	if (localDW.isIsBatteryError == IN_YES) {
		/* Transition: '<S1>:3982' */
		s = 15;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3983' */
	}

	if (localDW.isIsConfigTimeWarning == IN_YES) {
		/* Transition: '<S1>:3985' */
		s = 14;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3984' */
	}

	if (localDW.isIsPausedTimeExceeded == IN_YES) {
		/* Transition: '<S1>:3986' */
		s = 13;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3987' */
	}

	if (localDW.isIsIdleTimeExceeded == IN_YES) {
		/* Transition: '<S1>:3989' */
		s = 12;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3988' */
	}

	if (localDW.isIsFlowRateNotStable == IN_YES) {
		/* Transition: '<S1>:3991' */
		s = 11;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3990' */
	}

	if (localDW.isIsUnderInfusion == IN_YES_O) {
		/* Transition: '<S1>:3992' */
		s = 10;
		localDW.maxAlarmLevel = 1;
	} else {
		/* Transition: '<S1>:3993' */
	}

	if (localDW.isIsLowReservoir == IN_YES) {
		/* Transition: '<S1>:3994' */
		s = 9;
		localDW.maxAlarmLevel = 2;
	} else {
		/* Transition: '<S1>:3995' */
	}

	if (localDW.isIsDoorOpen == IN_YES) {
		/* Transition: '<S1>:3997' */
		s = 8;
		localDW.maxAlarmLevel =
			3; // TODO: Soha Mutated that for the sake of the expirement of the repair motivational
		// example
	} else {
		/* Transition: '<S1>:3996' */
	}

	if (localDW.isIsOcclusion == IN_YES) {
		/* Transition: '<S1>:3999' */
		s = 7;
		localDW.maxAlarmLevel = 3;
	} else {
		/* Transition: '<S1>:3998' */
	}

	if (localDW.isIsAirInLine == IN_YES) {
		/* Transition: '<S1>:4000' */
		s = 6;
		localDW.maxAlarmLevel = 3;
	} else {
		/* Transition: '<S1>:4001' */
	}

	if (localDW.isIsOverInfusionVTBI == IN_YES) {
		/* Transition: '<S1>:4003' */
		s = 5;
		localDW.maxAlarmLevel = 3;
	} else {
		/* Transition: '<S1>:4002' */
	}

	if (localDW.isIsOverInfusionFlowRate == IN_YES_O) {
		/* Transition: '<S1>:4005' */
		s = 4;
		localDW.maxAlarmLevel = 3;
	} else {
		/* Transition: '<S1>:4004' */
	}

	if (localDW.isIsHardwareError == IN_YES) {
		/* Transition: '<S1>:4007' */
		s = 3;
		localDW.maxAlarmLevel = 4;
	} else {
		/* Transition: '<S1>:4006' */
	}

	if (localDW.isIsEnviromentalError == IN_YES) {
		/* Transition: '<S1>:4009' */
		s = 2;
		localDW.maxAlarmLevel = 4;
	} else {
		/* Transition: '<S1>:4008' */
	}

	if (localDW.isIsEmptyReservoir == IN_YES) {
		/* Transition: '<S1>:4011' */
		s = 1;
		localDW.maxAlarmLevel = 4;
	} else {
		/* Transition: '<S1>:4010' */
	}

	return s;
}

/* Function for Chart: '<Root>/Alarm  Sub-System' */
function setHighestAlarm(localDW) {
	/* Graphical Function 'setHighestAlarm': '<S1>:4098' */
	/* Transition: '<S1>:4104' */
	return localDW.maxAlarmLevel;
}

/* Funcztion for Chart: '<Root>/Alarm  Sub-System' */
function checkAlarm(localB, localDW) {
	var overInfusion;

	/* During 'CheckAlarm': '<S1>:3953' */
	/* During 'CancelAlarm': '<S1>:4012' */
	if (localDW.isCancelAlarm == IN_OFF_I) {
		/* During 'OFF': '<S1>:4017' */
		if ((localDW.currentAlarm > 0) && localB.notificationCancel) {
			/* Transition: '<S1>:4014' */
			localDW.isCancelAlarm = IN_ON_A;

			/* Entry 'ON': '<S1>:4016' */
			localDW.cancelAlarm = localDW.currentAlarm;
			writeLog(3, localB);
		}
	} else {
		/* During 'ON': '<S1>:4016' */
		/* Transition: '<S1>:4015' */
		localDW.isCancelAlarm = IN_OFF_I;
	}

	/* During 'Level4': '<S1>:4019' */
	/* During 'IsEmptyReservoir': '<S1>:4020' */
	if (localDW.isIsEmptyReservoir == IN_NO) {
		/* During 'No': '<S1>:4024' */
		if (localB.inTherapy && localB.reservoirEmpty) {
			/* Transition: '<S1>:4022' */
			localDW.isIsEmptyReservoir = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4025' */
		if (localB.inTherapy && localB.reservoirEmpty) {
			/* Transition: '<S1>:4615' */
			localDW.isIsEmptyReservoir = IN_YES;
		} else {
			if ((localDW.cancelAlarm == 1) && (!localB.reservoirEmpty)) {
				/* Transition: '<S1>:4023' */
				localDW.isIsEmptyReservoir = IN_NO;
			}
		}
	}

	var batteryDepleted = localB.batteryDepleted;
	var rtcInError = localB.rtcInError;
	var cpuInError = localB.cpuInError;
	var memoryCorruptedl = localB.memoryCorrupted;
	var pumpTooHotl = localB.pumpTooHot;
	var watchdogInterruptedl = localB.watchdogInterrupted;
	var cancelAlarml = localDW.cancelAlarm;
	/* During 'IsHardwareError': '<S1>:4217' */
	if (localDW.isIsHardwareError == IN_NO) {
		/* During 'No': '<S1>:4222' */

		if (batteryDepleted
			|| rtcInError
			|| cpuInError
			|| memoryCorruptedl
			|| pumpTooHotl
			|| watchdogInterruptedl) {
			/* Transition: '<S1>:4223' */
			localDW.isIsHardwareError = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4220' */
		if ((cancelAlarml == 2)
			&& (!(batteryDepleted
				|| rtcInError
				|| cpuInError
				|| memoryCorruptedl
				|| pumpTooHotl
				|| watchdogInterruptedl))) {
			/* Transition: '<S1>:4221' */
			localDW.isIsHardwareError = IN_NO;
		}
	}

	/* During 'IsEnviromentalError': '<S1>:4032' */
	if (localDW.isIsEnviromentalError == IN_NO) {
		/* During 'No': '<S1>:4037' */
		var temp = localB.temp;
		var humidity = localB.humidity;
		var airPressure = localB.airPressure;

		if (temp || humidity || airPressure) {
			/* Transition: '<S1>:4034' */
			localDW.isIsEnviromentalError = IN_YES;
		}
	} else {
		cancelAlarml = localDW.cancelAlarm;
		var templ = localB.temp;
		var humidityl = localB.humidity;
		var airPressurel = localB.airPressure;

		/* During 'Yes': '<S1>:4036' */
		if ((cancelAlarml == 3) && (!(templ || humidityl || airPressurel))) {
			/* Transition: '<S1>:4035' */
			localDW.isIsEnviromentalError = IN_NO;
		}
	}

	/* During 'Level3': '<S1>:4038' */
	/* During 'IsOverInfusionFlowRate': '<S1>:4039' */
	overInfusion = checkOverInfusionFlowRate(localB);
	if (localDW.isIsOverInfusionFlowRate == IN_CHECK) {
		/* During 'Check': '<S1>:4052' */
		if (overInfusion == 1) {
			/* Transition: '<S1>:4044' */
			/* Exit 'Check': '<S1>:4052' */
			localDW.isIsOverInfusionFlowRate = IN_YES_O;

			/* Entry 'Yes': '<S1>:4051' */
		} else {
			if (overInfusion == 2) {
				/* Transition: '<S1>:4046' */
				localDW.overInfusionTimer = 0;

				/* Exit 'Check': '<S1>:4052' */
				localDW.isIsOverInfusionFlowRate = IN_MONITOR;

				/* Entry 'Monitor': '<S1>:4053' */
			}
		}
	} else if (localDW.isIsOverInfusionFlowRate == IN_MONITOR) {
		/* During 'Monitor': '<S1>:4053' */
		var scalingFactor2 = stepScalingFactor(localB.maxDurationOverInfusion);
		if ((overInfusion == 1) || (localDW.overInfusionTimer > scalingFactor2)) {
			/* Transition: '<S1>:4047' */
			localDW.overInfusionTimer = 0;

			/* Exit 'Monitor': '<S1>:4053' */
			localDW.isIsOverInfusionFlowRate = IN_YES_O;

			/* Entry 'Yes': '<S1>:4051' */
		} else if (overInfusion == 0) {
			/* Transition: '<S1>:4042' */
			localDW.overInfusionTimer = 0;

			/* Exit 'Monitor': '<S1>:4053' */
			localDW.isIsOverInfusionFlowRate = IN_CHECK;

			/* Entry 'Check': '<S1>:4052' */
		} else {
			if (overInfusion == 2) {
				/* Transition: '<S1>:4049' */
				localDW.overInfusionTimer++;

				/* Exit 'Monitor': '<S1>:4053' */
				localDW.isIsOverInfusionFlowRate = IN_MONITOR;

				/* Entry 'Monitor': '<S1>:4053' */
			}
		}
	} else {
		/* During 'Yes': '<S1>:4051' */
		if (overInfusion == 1) {
			/* Transition: '<S1>:4701' */
			localDW.isIsOverInfusionFlowRate = IN_YES_O;

			/* Entry 'Yes': '<S1>:4051' */
		} else {
			if (localDW.cancelAlarm == 4) {
				/* Transition: '<S1>:4501' */
				localDW.isIsOverInfusionFlowRate = IN_CHECK;

				/* Entry 'Check': '<S1>:4052' */
			}
		}
	}

	/* During 'IsOverInfusionVTBI': '<S1>:4066' */
	if (localDW.isIsOverInfusionVTBI == IN_NO) {
		/* During 'No': '<S1>:4071' */
		if (localB.inTherapy && (localB.volumeInfused > localB.VTBIHigh)) {
			/* Transition: '<S1>:4068' */
			localDW.isIsOverInfusionVTBI = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4070' */
		if (localB.inTherapy && (localB.volumeInfused > localB.VTBIHigh)) {
			/* Transition: '<S1>:4702' */
			localDW.isIsOverInfusionVTBI = IN_YES;
		} else {
			if (localDW.cancelAlarm == 5) {
				/* Transition: '<S1>:4069' */
				localDW.isIsOverInfusionVTBI = IN_NO;
			}
		}
	}

	/* During 'IsAirInLine': '<S1>:4072' */
	if (localDW.isIsAirInLine == IN_NO) {
		/* During 'No': '<S1>:4077' */
		if (localB.airInLine) {
			/* Transition: '<S1>:4074' */
			localDW.isIsAirInLine = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4076' */
		if ((localDW.cancelAlarm == 6) && (!localB.airInLine)) {
			/* Transition: '<S1>:4075' */
			localDW.isIsAirInLine = IN_NO;
		}
	}

	/* During 'IsOcclusion': '<S1>:4078' */
	if (localDW.isIsOcclusion == IN_NO) {
		/* During 'No': '<S1>:4083' */
		if (localB.occlusion) {
			/* Transition: '<S1>:4080' */
			localDW.isIsOcclusion = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4082' */
		if ((localDW.cancelAlarm == 7) && (!localB.occlusion)) {
			/* Transition: '<S1>:4081' */
			localDW.isIsOcclusion = IN_NO;
		}
	}

	/* During 'IsDoorOpen': '<S1>:4084' */
	if (localDW.isIsDoorOpen == IN_NO) {
		/* During 'No': '<S1>:4088' */
		if (localB.doorOpen) {
			/* Transition: '<S1>:4086' */
			localDW.isIsDoorOpen = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4089' */
		if ((localDW.cancelAlarm == 8) && (!localB.doorOpen)) {
			/* Transition: '<S1>:4087' */
			localDW.isIsDoorOpen = IN_NO;
		}
	}

	/* During 'Level2': '<S1>:4090' */
	/* During 'IsLowReservoir': '<S1>:4091' */
	if (localDW.isIsLowReservoir == IN_NO) {
		/* During 'No': '<S1>:4095' */
		if (localB.inTherapy && (localB.reservoirVolume < localB.lowReservoir)) {
			/* Transition: '<S1>:4093' */
			localDW.isIsLowReservoir = IN_YES;
		}
	} else {
		/* During 'Yes': '<S1>:4096' */
		if ((localDW.cancelAlarm == 9) && (!(localB.reservoirVolume < localB.lowReservoir))) {
			/* Transition: '<S1>:4094' */
			localDW.isIsLowReservoir = IN_NO;
		}
	}

	level1(localB, localDW);

	/* During 'SetAlarmStatus': '<S1>:4018' */
	localDW.currentAlarm = setCurrentAlarm(localDW);
	localB.alarmOutHighestLevelAlarm = setHighestAlarm(localDW);
}

/* Function for Chart: '<Root>/Alarm  Sub-System' */
function alarms(localB, localDW) {
	/* During 'Alarms': '<S1>:3907' */
	if (!localB.systemOn) {
		/* Transition: '<S1>:3901' */
		/* Exit Internal 'Alarms': '<S1>:3907' */
		/* Exit Internal 'Notification': '<S1>:3908' */
		/* Exit Internal 'Audio': '<S1>:3919' */
		if (localDW.isAudio == IN_DISABLED) {
			/* Exit 'Disabled': '<S1>:3939' */
			localB.alarmOutAudioNotificationCommand = 0;
			localDW.isAudio = IN_NO_ACTIVE_CHILD;
		} else if (localDW.isAudio == IN_OFF) {
			/* Exit 'OFF': '<S1>:3937' */
			localB.alarmOutAudioNotificationCommand = 0;
			localDW.isAudio = IN_NO_ACTIVE_CHILD;
		} else if (localDW.isAudio == IN_ON) {
			/* Exit 'ON': '<S1>:3938' */
			localB.alarmOutAudioNotificationCommand = localB.audioLevel;
			localDW.isAudio = IN_NO_ACTIVE_CHILD;
		} else if (localDW.isAudio == IN_SILENCED) {
			/* Exit 'Silenced': '<S1>:3952' */
			localB.alarmOutAudioNotificationCommand = 0;
			localDW.isAudio = IN_NO_ACTIVE_CHILD;
		} else {
			localDW.isAudio = IN_NO_ACTIVE_CHILD;
		}

		/* Exit 'Audio': '<S1>:3919' */
		localB.alarmOutDisplayAudioDisabledIndicator = localB.disableAudio;
		localDW.isActiveAudio = 0;

		/* Exit Internal 'Visual': '<S1>:3909' */
		if (localDW.isVisual == IN_ALARM_DISPLAY) {
			/* Exit 'AlarmDisplay': '<S1>:3913' */
			localB.alarmOutDisplayNotificationCommand = localDW.currentAlarm;
			localDW.isVisual = IN_NO_ACTIVE_CHILD;
		} else {
			localDW.isVisual = IN_NO_ACTIVE_CHILD;
		}

		localDW.isActiveVisual = 0;
		localDW.isActiveNotification = 0;

		/* Exit Internal 'CheckAlarm': '<S1>:3953' */
		/* Exit 'SetAlarmStatus': '<S1>:4018' */
		localDW.currentAlarm = setCurrentAlarm(localDW);
		localB.alarmOutHighestLevelAlarm = setHighestAlarm(localDW);
		localDW.cancelAlarm = 0;
		localDW.isActiveSetAlarmStatus = 0;

		/* Exit Internal 'Level1': '<S1>:4113' */
		/* Exit Internal 'IsSystemMonitorFailed': '<S1>:4185' */
		localDW.isIsSystemMonitorFailed = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsSystemMonitorFailed = 0;

		/* Exit Internal 'IsLoggingFailed': '<S1>:4179' */
		localDW.isIsLoggingFailed = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsLoggingFailed = 0;

		/* Exit Internal 'IsPumpHot': '<S1>:4173' */
		localDW.isIsPumpHot = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsPumpHot = 0;

		/* Exit Internal 'IsBatteryError': '<S1>:4167' */
		localDW.isIsBatteryError = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsBatteryError = 0;

		/* Exit Internal 'IsConfigTimeWarning': '<S1>:4161' */
		localDW.isIsConfigTimeWarning = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsConfigTimeWarning = 0;

		/* Exit Internal 'IsPausedTimeExceeded': '<S1>:4155' */

		if (localDW.isIsPausedTimeExceeded == IN_NO) {
			/* Exit 'No': '<S1>:4756' */
			localDW.pausedtimer = 0;
			localDW.isIsPausedTimeExceeded = IN_NO_ACTIVE_CHILD;
		} else if (localDW.isIsPausedTimeExceeded == IN_COUNTING) {
			/* Exit 'counting': '<S1>:4752' */
			localDW.pausedtimer++;
			localDW.isIsPausedTimeExceeded = IN_NO_ACTIVE_CHILD;
		} else {
			localDW.isIsPausedTimeExceeded = IN_NO_ACTIVE_CHILD;
		}

		localDW.isActiveIsPausedTimeExceeded = 0;

		/* Exit Internal 'IsIdleTimeExceeded': '<S1>:4149' */

		if (localDW.isIsIdleTimeExceeded == IN_NO) {
			/* Exit 'No': '<S1>:4153' */
			localDW.idletimer = 0;
			localDW.isIsIdleTimeExceeded = IN_NO_ACTIVE_CHILD;
		} else if (localDW.isIsIdleTimeExceeded == IN_COUNTING) {
			/* Exit 'counting': '<S1>:4745' */
			localDW.idletimer++;
			localDW.isIsIdleTimeExceeded = IN_NO_ACTIVE_CHILD;
		}
		localDW.isIsIdleTimeExceeded = IN_NO_ACTIVE_CHILD;

		localDW.isActiveIsIdleTimeExceeded = 0;

		/* Exit Internal 'IsFlowRateNotStable': '<S1>:4143' */
		localDW.isIsFlowRateNotStable = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsFlowRateNotStable = 0;

		/* Exit Internal 'IsUnderInfusion': '<S1>:4114' */
		localDW.isIsUnderInfusion = IN_NO_ACTIVE_CHILD;

		/* Exit 'IsUnderInfusion': '<S1>:4114' */
		localDW.isActiveIsUnderInfusion = 0;

		/* Exit Internal 'InfusionNotStartedWarning': '<S1>:4577' */
		localDW.isInfusionNotStartedWarning = IN_NO_ACTIVE_CHILD;
		localDW.isActiveInfusionNotStartedWarning = 0;
		localDW.isActiveLevel1 = 0;

		/* Exit Internal 'Level2': '<S1>:4090' */
		/* Exit Internal 'IsLowReservoir': '<S1>:4091' */
		localDW.isIsLowReservoir = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsLowReservoir = 0;
		localDW.isActiveLevel2 = 0;

		/* Exit Internal 'Level3': '<S1>:4038' */
		/* Exit Internal 'IsDoorOpen': '<S1>:4084' */
		localDW.isIsDoorOpen = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsDoorOpen = 0;

		/* Exit Internal 'IsOcclusion': '<S1>:4078' */
		localDW.isIsOcclusion = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsOcclusion = 0;

		/* Exit Internal 'IsAirInLine': '<S1>:4072' */
		localDW.isIsAirInLine = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsAirInLine = 0;

		/* Exit Internal 'IsOverInfusionVTBI': '<S1>:4066' */
		localDW.isIsOverInfusionVTBI = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsOverInfusionVTBI = 0;

		/* Exit Internal 'IsOverInfusionFlowRate': '<S1>:4039' */
		if (localDW.isIsOverInfusionFlowRate == IN_CHECK) {
			/* Exit 'Check': '<S1>:4052' */
			localDW.isIsOverInfusionFlowRate = IN_NO_ACTIVE_CHILD;
		} else if (localDW.isIsOverInfusionFlowRate == IN_MONITOR) {
			/* Exit 'Monitor': '<S1>:4053' */
			localDW.isIsOverInfusionFlowRate = IN_NO_ACTIVE_CHILD;
		} else {
			localDW.isIsOverInfusionFlowRate = IN_NO_ACTIVE_CHILD;
		}

		/* Exit 'IsOverInfusionFlowRate': '<S1>:4039' */
		localDW.isActiveIsOverInfusionFlowRate = 0;
		localDW.isActiveLevel3 = 0;

		/* Exit Internal 'Level4': '<S1>:4019' */
		/* Exit Internal 'IsEnviromentalError': '<S1>:4032' */
		localDW.isIsEnviromentalError = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsEnviromentalError = 0;

		/* Exit Internal 'IsHardwareError': '<S1>:4217' */
		localDW.isIsHardwareError = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsHardwareError = 0;

		/* Exit Internal 'IsEmptyReservoir': '<S1>:4020' */
		localDW.isIsEmptyReservoir = IN_NO_ACTIVE_CHILD;
		localDW.isActiveIsEmptyReservoir = 0;
		localDW.isActiveLevel4 = 0;

		/* Exit Internal 'CancelAlarm': '<S1>:4012' */
		localDW.isCancelAlarm = IN_NO_ACTIVE_CHILD;
		localDW.isActiveCancelAlarm = 0;
		localDW.isActiveCheckAlarm = 0;
		localDW.isC2AlarmFunctional = IN_NOT_ON;
	} else {
		checkAlarm(localB, localDW);

		/* During 'Notification': '<S1>:3908' */
		/* During 'Visual': '<S1>:3909' */
		if (localDW.isVisual == IN_ALARM_DISPLAY) {
			/* During 'AlarmDisplay': '<S1>:3913' */
			if (localDW.currentAlarm == 0) {
				/* Transition: '<S1>:3912' */
				/* Exit 'AlarmDisplay': '<S1>:3913' */
				localDW.isVisual = IN_OFF;

				/* Entry 'OFF': '<S1>:3914' */
				localB.alarmOutDisplayNotificationCommand = 0;
			} else if (localDW.currentAlarm > 0) {
				/* Transition: '<S1>:4614' */
				/* Exit 'AlarmDisplay': '<S1>:3913' */
				localDW.isVisual = IN_ALARM_DISPLAY;

				/* Entry 'AlarmDisplay': '<S1>:3913' */
				localB.alarmOutDisplayNotificationCommand = localDW.currentAlarm;
			} else {
				localB.alarmOutDisplayNotificationCommand = localDW.currentAlarm;
			}
		} else {
			/* During 'OFF': '<S1>:3914' */
			if (localDW.currentAlarm > 0) {
				/* Transition: '<S1>:3911' */
				localDW.isVisual = IN_ALARM_DISPLAY;

				/* Entry 'AlarmDisplay': '<S1>:3913' */
				localB.alarmOutDisplayNotificationCommand = localDW.currentAlarm;
			}
		}

		/* During 'Audio': '<S1>:3919' */
		localB.alarmOutDisplayAudioDisabledIndicator = localB.disableAudio;

		if (localDW.isAudio == IN_DISABLED) {
			/* During 'Disabled': '<S1>:3939' */
			if (localB.disableAudio == 2) {
				/* Transition: '<S1>:4610' */
				/* Exit 'Disabled': '<S1>:3939' */
				localDW.isAudio = IN_SILENCED;

				/* Entry 'Silenced': '<S1>:3952' */
				localDW.audioTimer = 0;
				localB.alarmOutAudioNotificationCommand = 0;
				localDW.audioTimer++;
			} else {
				/* Transition: '<S1>:4604' */
				if ((localB.alarmOutHighestLevelAlarm > 2) && (localB.disableAudio == 0)) {
					/* Transition: '<S1>:4736' */
					/* Exit 'Disabled': '<S1>:3939' */
					localDW.isAudio = IN_ON;

					/* Entry 'ON': '<S1>:3938' */
					localB.alarmOutAudioNotificationCommand = localB.audioLevel;
				} else if (localB.disableAudio == 0) {
					/* Transition: '<S1>:3934' */
					/* Transition: '<S1>:3928' */
					/* Exit 'Disabled': '<S1>:3939' */
					localDW.isAudio = IN_OFF;

					/* Entry 'OFF': '<S1>:3937' */
					localB.alarmOutAudioNotificationCommand = 0;
				} else {
					localB.alarmOutAudioNotificationCommand = 0;
				}
			}
		} else if (localDW.isAudio == IN_OFF) {
			/* During 'OFF': '<S1>:3937' */
			if (localB.disableAudio == 1) {
				/* Transition: '<S1>:4743' */
				/* Exit 'OFF': '<S1>:3937' */
				localDW.isAudio = IN_DISABLED;

				/* Entry 'Disabled': '<S1>:3939' */
				localB.alarmOutAudioNotificationCommand = 0;
			} else {
				/* Transition: '<S1>:4738' */
				if (localB.disableAudio == 2) {
					/* Transition: '<S1>:4739' */
					/* Exit 'OFF': '<S1>:3937' */
					localDW.isAudio = IN_SILENCED;

					/* Entry 'Silenced': '<S1>:3952' */
					localDW.audioTimer = 0;
					localB.alarmOutAudioNotificationCommand = 0;
					localDW.audioTimer++;
				} else if ((localB.alarmOutHighestLevelAlarm > 2) && (localB.disableAudio == 0)) {
					/* Transition: '<S1>:3925' */
					/* Exit 'OFF': '<S1>:3937' */
					localDW.isAudio = IN_ON;

					/* Entry 'ON': '<S1>:3938' */
					localB.alarmOutAudioNotificationCommand = localB.audioLevel;
				} else {
					localB.alarmOutAudioNotificationCommand = 0;
				}
			}
		} else if (localDW.isAudio == IN_ON) {
			/* During 'ON': '<S1>:3938' */
			if (localB.disableAudio == 1) {
				/* Transition: '<S1>:3931' */
				/* Exit 'ON': '<S1>:3938' */
				localDW.isAudio = IN_DISABLED;

				/* Entry 'Disabled': '<S1>:3939' */
				localB.alarmOutAudioNotificationCommand = 0;
			} else {
				/* Transition: '<S1>:3927' */
				if (localB.disableAudio == 2) {
					/* Transition: '<S1>:4739' */
					/* Exit 'ON': '<S1>:3938' */
					localDW.isAudio = IN_SILENCED;

					/* Entry 'Silenced': '<S1>:3952' */
					localDW.audioTimer = 0;
					localB.alarmOutAudioNotificationCommand = 0;
					localDW.audioTimer++;
				} else if ((localB.alarmOutHighestLevelAlarm > 2) && (localB.disableAudio == 0)) {
					/* Transition: '<S1>:4609' */
					/* Exit 'ON': '<S1>:3938' */
					localDW.isAudio = IN_ON;

					/* Entry 'ON': '<S1>:3938' */
					localB.alarmOutAudioNotificationCommand = localB.audioLevel;
				} else {
					/* Transition: '<S1>:3926' */
					/* Exit 'ON': '<S1>:3938' */
					localDW.isAudio = IN_OFF;

					/* Entry 'OFF': '<S1>:3937' */
					localB.alarmOutAudioNotificationCommand = 0;
				}
			}
		} else {
			/* During 'Silenced': '<S1>:3952' */
			if (localB.disableAudio == 1) {
				/* Transition: '<S1>:4611' */
				/* Exit 'Silenced': '<S1>:3952' */
				localDW.isAudio = IN_DISABLED;

				/* Entry 'Disabled': '<S1>:3939' */
				localB.alarmOutAudioNotificationCommand = 0;
			} else {
				/* Transition: '<S1>:4605' */
				if ((localB.alarmOutHighestLevelAlarm > 2) && (localB.disableAudio == 0)) {
					/* Transition: '<S1>:4736' */
					/* Exit 'Silenced': '<S1>:3952' */
					localDW.isAudio = IN_ON;

					/* Entry 'ON': '<S1>:3938' */
					localB.alarmOutAudioNotificationCommand = localB.audioLevel;
				} else {
					var audioTimerl = localDW.audioTimer;
					var Disable_Audiol = localB.disableAudio;
					var scalingFactor = stepScalingFactor(localB.audioEnableDuration);
					if ((audioTimerl > scalingFactor) || (Disable_Audiol == 0)) {
						/* Transition: '<S1>:3936' */
						/* Transition: '<S1>:3928' */
						/* Exit 'Silenced': '<S1>:3952' */
						localDW.isAudio = IN_OFF;

						/* Entry 'OFF': '<S1>:3937' */
						localB.alarmOutAudioNotificationCommand = 0;
					} else {
						localB.alarmOutAudioNotificationCommand = 0;
						localDW.audioTimer++;
					}
				}
			}
		}
	}
}

/* Function for Chart: '<Root>/Alarm  Sub-System' */
function enterInternalCheckAlarm(localB, localDW) {
	var overInfusion;

	/* Entry Internal 'CheckAlarm': '<S1>:3953' */
	localDW.isActiveCancelAlarm = 1;

	/* Entry Internal 'CancelAlarm': '<S1>:4012' */
	/* Transition: '<S1>:4013' */
	localDW.isCancelAlarm = IN_OFF_I;
	localDW.isActiveLevel4 = 1;

	/* Entry Internal 'Level4': '<S1>:4019' */
	localDW.isActiveIsEmptyReservoir = 1;

	/* Entry Internal 'IsEmptyReservoir': '<S1>:4020' */
	if (localB.inTherapy && localB.reservoirEmpty) {
		/* Transition: '<S1>:4193' */
		localDW.isIsEmptyReservoir = IN_YES;
	} else {
		/* Transition: '<S1>:4021' */
		localDW.isIsEmptyReservoir = IN_NO;
	}

	localDW.isActiveIsHardwareError = 1;

	var Battery_Depletedl = localB.batteryDepleted;
	var RTC_In_Errorl = localB.rtcInError;
	var CPU_In_Errorl = localB.cpuInError;
	var Memory_Corruptedl = localB.memoryCorrupted;
	var Pump_Too_Hotl = localB.pumpTooHot;
	var Watchdog_Interruptedl = localB.watchdogInterrupted;
	var Templ = localB.temp;
	var Humidityl = localB.humidity;
	var Air_Pressurel = localB.airPressure;

	/* Entry Internal 'IsHardwareError': '<S1>:4217' */
	if (Battery_Depletedl
		|| RTC_In_Errorl
		|| CPU_In_Errorl
		|| Memory_Corruptedl
		|| Pump_Too_Hotl
		|| Watchdog_Interruptedl) {
		/* Transition: '<S1>:4224' */
		localDW.isIsHardwareError = IN_YES;
	} else {
		/* Transition: '<S1>:4219' */
		localDW.isIsHardwareError = IN_NO;
	}

	localDW.isActiveIsEnviromentalError = 1;

	/* Entry Internal 'IsEnviromentalError': '<S1>:4032' */
	if (Templ || Humidityl || Air_Pressurel) {
		/* Transition: '<S1>:4198' */
		localDW.isIsEnviromentalError = IN_YES;
	} else {
		/* Transition: '<S1>:4033' */
		localDW.isIsEnviromentalError = IN_NO;
	}

	localDW.isActiveLevel3 = 1;

	/* Entry Internal 'Level3': '<S1>:4038' */
	localDW.isActiveIsOverInfusionFlowRate = 1;

	/* Entry 'IsOverInfusionFlowRate': '<S1>:4039' */
	overInfusion = checkOverInfusionFlowRate(localB);

	/* Entry Internal 'IsOverInfusionFlowRate': '<S1>:4039' */
	if (overInfusion == 1) {
		/* Transition: '<S1>:4697' */
		localDW.isIsOverInfusionFlowRate = IN_YES_O;

		/* Entry 'Yes': '<S1>:4051' */
	} else if (overInfusion == 2) {
		/* Transition: '<S1>:4699' */
		localDW.isIsOverInfusionFlowRate = IN_MONITOR;

		/* Entry 'Monitor': '<S1>:4053' */
	} else {
		/* Transition: '<S1>:4041' */
		localDW.isIsOverInfusionFlowRate = IN_CHECK;

		/* Entry 'Check': '<S1>:4052' */
	}

	localDW.isActiveIsOverInfusionVTBI = 1;

	var In_Therapyl = localB.inTherapy;
	var Volume_Infusedl = localB.volumeInfused;
	var VTBI_Highl = localB.VTBIHigh;

	/* Entry Internal 'IsOverInfusionVTBI': '<S1>:4066' */
	if (In_Therapyl && (Volume_Infusedl > VTBI_Highl)) {
		/* Transition: '<S1>:4201' */
		localDW.isIsOverInfusionVTBI = IN_YES;
	} else {
		/* Transition: '<S1>:4067' */
		localDW.isIsOverInfusionVTBI = IN_NO;
	}

	localDW.isActiveIsAirInLine = 1;

	/* Entry Internal 'IsAirInLine': '<S1>:4072' */
	if (localB.airInLine) {
		/* Transition: '<S1>:4202' */
		localDW.isIsAirInLine = IN_YES;
	} else {
		/* Transition: '<S1>:4073' */
		localDW.isIsAirInLine = IN_NO;
	}

	localDW.isActiveIsOcclusion = 1;

	/* Entry Internal 'IsOcclusion': '<S1>:4078' */
	if (localB.occlusion) {
		/* Transition: '<S1>:4203' */
		localDW.isIsOcclusion = IN_YES;
	} else {
		/* Transition: '<S1>:4079' */
		localDW.isIsOcclusion = IN_NO;
	}

	localDW.isActiveIsDoorOpen = 1;

	/* Entry Internal 'IsDoorOpen': '<S1>:4084' */
	if (localB.doorOpen) {
		/* Transition: '<S1>:4204' */
		localDW.isIsDoorOpen = IN_YES;
	} else {
		/* Transition: '<S1>:4085' */
		localDW.isIsDoorOpen = IN_NO;
	}

	localDW.isActiveLevel2 = 1;

	/* Entry Internal 'Level2': '<S1>:4090' */
	localDW.isActiveIsLowReservoir = 1;

	/* Entry Internal 'IsLowReservoir': '<S1>:4091' */
	if (localB.inTherapy && (localB.reservoirVolume < localB.lowReservoir)) {
		/* Transition: '<S1>:4205' */
		localDW.isIsLowReservoir = IN_YES;
	} else {
		/* Transition: '<S1>:4092' */
		localDW.isIsLowReservoir = IN_NO;
	}

	localDW.isActiveLevel1 = 1;

	/* Entry Internal 'Level1': '<S1>:4113' */
	localDW.isActiveInfusionNotStartedWarning = 1;

	/* Entry Internal 'InfusionNotStartedWarning': '<S1>:4577' */
	if (localB.infusionInitiate && (!localB.reservoirEmpty)) {
		/* Transition: '<S1>:4578' */
		localDW.isInfusionNotStartedWarning = IN_YES;
	} else {
		/* Transition: '<S1>:4579' */
		localDW.isInfusionNotStartedWarning = IN_NO;
	}

	localDW.isActiveIsUnderInfusion = 1;

	/* Entry 'IsUnderInfusion': '<S1>:4114' */
	overInfusion = checkUnderInfusion(localB);

	/* Entry Internal 'IsUnderInfusion': '<S1>:4114' */
	if (overInfusion == 1) {
		/* Transition: '<S1>:4709' */
		localDW.isIsUnderInfusion = IN_YES_O;

		/* Entry 'Yes': '<S1>:4126' */
	} else if (overInfusion == 2) {
		/* Transition: '<S1>:4710' */
		localDW.isIsUnderInfusion = IN_MONITOR;

		/* Entry 'Monitor': '<S1>:4128' */
	} else {
		/* Transition: '<S1>:4116' */
		localDW.isIsUnderInfusion = IN_CHECK;

		/* Entry 'Check': '<S1>:4127' */
	}

	localDW.isActiveIsFlowRateNotStable = 1;

	/* Entry Internal 'IsFlowRateNotStable': '<S1>:4143' */
	if (localB.inTherapy && localB.flowRateNotStable) {
		/* Transition: '<S1>:4210' */
		localDW.isIsFlowRateNotStable = IN_YES;
	} else {
		/* Transition: '<S1>:4144' */
		localDW.isIsFlowRateNotStable = IN_NO;
	}

	localDW.isActiveIsIdleTimeExceeded = 1;

	/* Entry Internal 'IsIdleTimeExceeded': '<S1>:4149' */
	var scalingFactor1 = stepScalingFactor(localB.maxIdleDuration);

	if ((localB.currentSystemMode == 1) && (scalingFactor1 == 1)) {
		/* Transition: '<S1>:4749' */
		localDW.isIsIdleTimeExceeded = IN_YES;
	} else if (localB.currentSystemMode == 1) {
		/* Transition: '<S1>:4748' */
		localDW.idletimer = 0;
		localDW.isIsIdleTimeExceeded = IN_COUNTING;

		/* Entry 'counting': '<S1>:4745' */
		localDW.idletimer++;
	} else {
		/* Transition: '<S1>:4150' */
		localDW.isIsIdleTimeExceeded = IN_NO;

		/* Entry 'No': '<S1>:4153' */
		localDW.idletimer = 0;
	}

	localDW.isActiveIsPausedTimeExceeded = 1;

	var Current_System_Model = localB.currentSystemMode;

	/* Entry Internal 'IsPausedTimeExceeded': '<S1>:4155' */
	var scalingFactor = stepScalingFactor(localB.maxPausedDuration);
	if (((Current_System_Model == 6) || (Current_System_Model == 7) || (Current_System_Model == 8))
		&& (scalingFactor == 1)) {
		/* Transition: '<S1>:4760' */
		localDW.isIsPausedTimeExceeded = IN_YES;
	} else if ((Current_System_Model == 6)
		|| (Current_System_Model == 7)
		|| (Current_System_Model == 8)) {
		/* Transition: '<S1>:4759' */
		localDW.pausedtimer = 0;
		localDW.isIsPausedTimeExceeded = IN_COUNTING;

		/* Entry 'counting': '<S1>:4752' */
		localDW.pausedtimer++;
	} else {
		/* Transition: '<S1>:4753' */
		localDW.isIsPausedTimeExceeded = IN_NO;

		/* Entry 'No': '<S1>:4756' */
		localDW.pausedtimer = 0;
	}

	localDW.isActiveIsConfigTimeWarning = 1;

	/* Entry Internal 'IsConfigTimeWarning': '<S1>:4161' */
	var scalingFactor2 = stepScalingFactor(localB.scalingFactor2);

	if (localB.configTimer > scalingFactor2) {
		/* Transition: '<S1>:4207' */
		localDW.isIsConfigTimeWarning = IN_YES;
	} else {
		/* Transition: '<S1>:4162' */
		localDW.isIsConfigTimeWarning = IN_NO;
	}

	localDW.isActiveIsBatteryError = 1;

	/* Entry Internal 'IsBatteryError': '<S1>:4167' */
	var Battery_Lowl = localB.batteryLow;
	var Battery_Unable_To_Chargel = localB.batteryUnableToCharge;
	var Supply_Voltagel = localB.supplyVoltage;

	if (Battery_Lowl || Battery_Unable_To_Chargel || Supply_Voltagel) {
		/* Transition: '<S1>:4212' */
		localDW.isIsBatteryError = IN_YES;
	} else {
		/* Transition: '<S1>:4168' */
		localDW.isIsBatteryError = IN_NO;
	}

	localDW.isActiveIsPumpHot = 1;

	/* Entry Internal 'IsPumpHot': '<S1>:4173' */
	if (localB.pumpOverheated) {
		/* Transition: '<S1>:4208' */
		localDW.isIsPumpHot = IN_YES;
	} else {
		/* Transition: '<S1>:4174' */
		localDW.isIsPumpHot = IN_NO;
	}

	localDW.isActiveIsLoggingFailed = 1;

	/* Entry Internal 'IsLoggingFailed': '<S1>:4179' */
	if (localB.loggingFailed) {
		/* Transition: '<S1>:4213' */
		localDW.isIsLoggingFailed = IN_YES;
	} else {
		/* Transition: '<S1>:4180' */
		localDW.isIsLoggingFailed = IN_NO;
	}

	localDW.isActiveIsSystemMonitorFailed = 1;

	/* Entry Internal 'IsSystemMonitorFailed': '<S1>:4185' */
	if (localB.systemMonitorFailed) {
		/* Transition: '<S1>:4209' */
		localDW.isIsSystemMonitorFailed = IN_YES;
	} else {
		/* Transition: '<S1>:4186' */
		localDW.isIsSystemMonitorFailed = IN_NO;
	}

	localDW.isActiveSetAlarmStatus = 1;

	/* Entry 'SetAlarmStatus': '<S1>:4018' */
	localDW.currentAlarm = setCurrentAlarm(localDW);
	localB.alarmOutHighestLevelAlarm = setHighestAlarm(localDW);
}

/* Function for Chart: '<Root>/Alarm  Sub-System' */
function enterInternalAlarms(localB, localDW) {
	/* Entry Internal 'Alarms': '<S1>:3907' */
	localDW.isActiveCheckAlarm = 1;
	enterInternalCheckAlarm(localB, localDW);
	localDW.isActiveNotification = 1;

	/* Entry Internal 'Notification': '<S1>:3908' */
	localDW.isActiveVisual = 1;

	/* Entry Internal 'Visual': '<S1>:3909' */
	if (localDW.currentAlarm > 0) {
		/* Transition: '<S1>:4608' */
		localDW.isVisual = IN_ALARM_DISPLAY;

		/* Entry 'AlarmDisplay': '<S1>:3913' */
		localB.alarmOutDisplayNotificationCommand = localDW.currentAlarm;
	} else {
		/* Transition: '<S1>:3910' */
		localDW.isVisual = IN_OFF;

		/* Entry 'OFF': '<S1>:3914' */
		localB.alarmOutDisplayNotificationCommand = 0;
	}

	localDW.isActiveAudio = 1;

	/* Entry 'Audio': '<S1>:3919' */
	localB.alarmOutDisplayAudioDisabledIndicator = localB.disableAudio;

	/* Entry Internal 'Audio': '<S1>:3919' */
	if (localB.disableAudio == 1) {
		/* Transition: '<S1>:4599' */
		localDW.isAudio = IN_DISABLED;

		/* Entry 'Disabled': '<S1>:3939' */
		localB.alarmOutAudioNotificationCommand = 0;
	} else if (localB.disableAudio == 2) {
		/* Transition: '<S1>:4600' */
		localDW.isAudio = IN_SILENCED;

		/* Entry 'Silenced': '<S1>:3952' */
		localDW.audioTimer = 0;
		localB.alarmOutAudioNotificationCommand = 0;
		localDW.audioTimer++;
	} else if ((localB.alarmOutHighestLevelAlarm > 2) && (localB.disableAudio == 0)) {
		/* Transition: '<S1>:4601' */
		localDW.isAudio = IN_ON;

		/* Entry 'ON': '<S1>:3938' */
		localB.alarmOutAudioNotificationCommand = localB.audioLevel;
	} else {
		/* Transition: '<S1>:3924' */
		localDW.isAudio = IN_OFF;

		/* Entry 'OFF': '<S1>:3937' */
		localB.alarmOutAudioNotificationCommand = 0;
	}
}

/* Initial conditions for referenced model: 'alarmFunctional' */
function alarmFunctionalInit(localB, localDW) {
	/* InitializeConditions for Chart: '<Root>/Alarm  Sub-System' */
	localDW.isActiveCheckAlarm = 0;
	localDW.isActiveCancelAlarm = 0;
	localDW.isCancelAlarm = IN_NO_ACTIVE_CHILD;
	localDW.isActiveLevel1 = 0;
	localDW.isActiveInfusionNotStartedWarning = 0;
	localDW.isInfusionNotStartedWarning = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsBatteryError = 0;
	localDW.isIsBatteryError = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsConfigTimeWarning = 0;
	localDW.isIsConfigTimeWarning = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsFlowRateNotStable = 0;
	localDW.isIsFlowRateNotStable = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsIdleTimeExceeded = 0;
	localDW.isIsIdleTimeExceeded = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsLoggingFailed = 0;
	localDW.isIsLoggingFailed = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsPausedTimeExceeded = 0;
	localDW.isIsPausedTimeExceeded = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsPumpHot = 0;
	localDW.isIsPumpHot = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsSystemMonitorFailed = 0;
	localDW.isIsSystemMonitorFailed = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsUnderInfusion = 0;
	localDW.isIsUnderInfusion = IN_NO_ACTIVE_CHILD;
	localDW.isActiveLevel2 = 0;
	localDW.isActiveIsLowReservoir = 0;
	localDW.isIsLowReservoir = IN_NO_ACTIVE_CHILD;
	localDW.isActiveLevel3 = 0;
	localDW.isActiveIsAirInLine = 0;
	localDW.isIsAirInLine = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsDoorOpen = 0;
	localDW.isIsDoorOpen = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsOcclusion = 0;
	localDW.isIsOcclusion = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsOverInfusionFlowRate = 0;
	localDW.isIsOverInfusionFlowRate = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsOverInfusionVTBI = 0;
	localDW.isIsOverInfusionVTBI = IN_NO_ACTIVE_CHILD;
	localDW.isActiveLevel4 = 0;
	localDW.isActiveIsEmptyReservoir = 0;
	localDW.isIsEmptyReservoir = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsEnviromentalError = 0;
	localDW.isIsEnviromentalError = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIsHardwareError = 0;
	localDW.isIsHardwareError = IN_NO_ACTIVE_CHILD;
	localDW.isActiveSetAlarmStatus = 0;
	localDW.isActiveNotification = 0;
	localDW.isActiveAudio = 0;
	localDW.isAudio = IN_NO_ACTIVE_CHILD;
	localDW.isActiveVisual = 0;
	localDW.isVisual = IN_NO_ACTIVE_CHILD;
	localDW.isActiveC2AlarmFunctional = 0;
	localDW.isC2AlarmFunctional = IN_NO_ACTIVE_CHILD;
	localDW.overInfusionTimer = 0;
	localDW.underInfusionTimer = 0;
	localDW.currentAlarm = 0;
	localDW.audioTimer = 0;
	localDW.cancelAlarm = 0;
	localDW.maxAlarmLevel = 0;
	localDW.idletimer = 0;
	localDW.pausedtimer = 0;
	localB.alarmOutDisplayAudioDisabledIndicator = 0;
	localB.alarmOutDisplayNotificationCommand = 0;
	localB.alarmOutAudioNotificationCommand = 0;
	localB.alarmOutHighestLevelAlarm = 0;
	localB.alarmOutLogMessageID = 0;
}

/* Output and update for referenced model: 'alarmFunctional' */
function alarmFunctional(rtuImIn, rtuTlmModeIn, rtuSysMonIn, rtuLoggingIn, rtuOpCmdIn, rtuDbIn, rtuSensorIn, rtuConstIn, rtuSysStatIn, rtuConfigIn, rtyAlarmOut, localB, localDW) {
	/* BusSelector: '<Root>/BusConversion_InsertedFor_IM_IN_at_outport_0' */
	localB.commandedFlowRate = rtuImIn.commandedFlowRate;
	localB.currentSystemMode = rtuImIn.currentSystemMode;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_TLM_MODE_IN_at_outport_0' */
	localB.systemOn = rtuTlmModeIn.systemOn;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_SYS_MON_IN_at_outport_0' */
	localB.systemMonitorFailed = rtuSysMonIn.systemMonitorFailed;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_LOGGING_IN_at_outport_0' */
	localB.loggingFailed = rtuLoggingIn.loggingFailed;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_OP_CMD_IN_at_outport_0' */
	localB.infusionInitiate = rtuOpCmdIn.Infusion_Initiate;
	localB.disableAudio = rtuOpCmdIn.disableAudio;
	localB.notificationCancel = rtuOpCmdIn.notificationCancel;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_DB_IN_at_outport_0' */
	localB.VTBIHigh = rtuDbIn.VTBIHigh;
	localB.flowRateHigh = rtuDbIn.flowRateHigh;
	localB.flowRateLow = rtuDbIn.flowRateLow;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_SENSOR_IN_at_outport_0' */
	localB.flowRate = rtuSensorIn.flowRate;
	localB.flowRateNotStable = rtuSensorIn.flowRateNotStable;
	localB.airInLine = rtuSensorIn.airInLine;
	localB.occlusion = rtuSensorIn.occlusion;
	localB.doorOpen = rtuSensorIn.doorOpen;
	localB.temp = rtuSensorIn.temp;
	localB.airPressure = rtuSensorIn.airPressure;
	localB.humidity = rtuSensorIn.humidity;
	localB.batteryDepleted = rtuSensorIn.batteryDepleted;
	localB.batteryLow = rtuSensorIn.batteryLow;
	localB.batteryUnableToCharge = rtuSensorIn.batteryUnableToCharge;
	localB.supplyVoltage = rtuSensorIn.supplyVoltage;
	localB.cpuInError = rtuSensorIn.cpuInError;
	localB.rtcInError = rtuSensorIn.rtcInError;
	localB.watchdogInterrupted = rtuSensorIn.watchdogInterrupted;
	localB.memoryCorrupted = rtuSensorIn.memoryCorrupted;
	localB.pumpTooHot = rtuSensorIn.pumpTooHot;
	localB.pumpOverheated = rtuSensorIn.pumpOverheated;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_CONST_IN_at_outport_0' */
	localB.audioEnableDuration = rtuConstIn.audioEnableDuration;
	localB.audioLevel = rtuConstIn.audioLevel;
	localB.scalingFactor2 = rtuConstIn.configWarningDuration;
	localB.lowReservoir = rtuConstIn.lowReservoir;
	localB.maxDurationOverInfusion = rtuConstIn.maxDurationOverInfusion;
	localB.maxdurationunderinfusion = rtuConstIn.maxDurationUnderInfusion;
	localB.maxPausedDuration = rtuConstIn.maxPausedDuration;
	localB.maxIdleDuration = rtuConstIn.maxIdleDuration;
	localB.toleranceMax = rtuConstIn.toleranceMax;
	localB.toleranceMin = rtuConstIn.toleranceMin;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_SYS_STAT_IN_at_outport_0' */
	localB.reservoirEmpty = rtuSysStatIn.Reservoir_Empty;
	localB.reservoirVolume = rtuSysStatIn.reservoirVolume;
	localB.volumeInfused = rtuSysStatIn.volumeInfused;
	localB.inTherapy = rtuSysStatIn.inTherapy;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_CONFIG_IN_at_outport_0' */
	localB.configTimer = rtuConfigIn.configTimer;

	/* Chart: '<Root>/Alarm  Sub-System' */
	/* Gateway: Alarm  Sub-System */
	/* During: Alarm  Sub-System */
	if (localDW.isActiveC2AlarmFunctional == 0) {
		/* Entry: Alarm  Sub-System */
		localDW.isActiveC2AlarmFunctional = 1;

		/* Entry Internal: Alarm  Sub-System */
		if (localB.systemOn) {
			/* Transition: '<S1>:4696' */
			localDW.isC2AlarmFunctional = IN_ALARMS;
			enterInternalAlarms(localB, localDW);
		} else {
			/* Transition: '<S1>:3904' */
			localDW.isC2AlarmFunctional = IN_NOT_ON;
		}
	} else if (localDW.isC2AlarmFunctional == IN_ALARMS) {
		alarms(localB, localDW);
	} else {
		/* During 'NOT_ON': '<S1>:3899' */
		if (localB.systemOn) {
			/* Transition: '<S1>:3900' */
			localDW.isC2AlarmFunctional = IN_ALARMS;
			enterInternalAlarms(localB, localDW);
		}
	}

	/* End of Chart: '<Root>/Alarm  Sub-System' */

	/* BusCreator: '<Root>/BusConversion_InsertedFor_ALARM_OUT_at_inport_0' */
	rtyAlarmOut.isAudioDisabled = localB.alarmOutDisplayAudioDisabledIndicator;
	rtyAlarmOut.notificationMessage = localB.alarmOutDisplayNotificationCommand;
	rtyAlarmOut.audioNotificationCommand = localB.alarmOutAudioNotificationCommand;
	rtyAlarmOut.highestLevelAlarm = localB.alarmOutHighestLevelAlarm;
	rtyAlarmOut.logMessageId = localB.alarmOutLogMessageID;
}


function prop1() {
	//alarm = new AlarmFunctional();
	var rtuImIn = new InfusionManagerOutputs();
	var rtuTlmModeIn = new TopLevelModeOutputs();
	var rtuSysMonIn = new SystemMonitorOutput();
	var rtuLoggingIn = new LogOutput();
	var rtuOpCmdIn = new OperatorCommands();
	var rtuDbIn = new DrugDatabaseInputs();
	var rtuSensorIn = new DeviceSensorInputs();
	var rtuConstIn = new DeviceConfigurationInputs();
	var rtuSysStatIn = new SystemStatusOutputs();
	var rtuConfigIn = new ConfigOutputs();
	var rtyAlarmOut = new AlarmOutputs();
	var localB = new B();
	var localDW = new DW();

	alarmFunctionalInit(localB, localDW);

	if ((0 <= rtuImIn.commandedFlowRate)
		&& (0 <= rtuImIn.currentSystemMode)
		&& (0 <= rtuOpCmdIn.disableAudio)
		&& (0 <= rtuDbIn.VTBIHigh)
		&& (0 <= rtuDbIn.flowRateHigh)
		&& (0 <= rtuDbIn.flowRateLow)
		&& (0 <= rtuSensorIn.flowRate)
		&& (0 <= rtuConstIn.audioEnableDuration)
		&& (0 <= rtuConstIn.audioLevel)
		&& (0 <= rtuConstIn.configWarningDuration)
		&& (0 <= rtuConstIn.lowReservoir)
		&& (0 <= rtuConstIn.maxConfigDuration)
		&& (0 <= rtuConstIn.maxDurationOverInfusion)
		&& (0 <= rtuConstIn.maxDurationUnderInfusion)
		&& (0 <= rtuConstIn.maxPausedDuration)
		&& (0 <= rtuConstIn.maxIdleDuration)
		&& (0 <= rtuConstIn.toleranceMax)
		&& (0 <= rtuConstIn.toleranceMin)
		&& (0 <= rtuSysStatIn.volumeInfused)
		&& (0 <= rtuSysStatIn.reservoirVolume)
		&& (0 <= rtuConfigIn.configMode)
		&& (0 <= rtyAlarmOut.isAudioDisabled)
		&& (0 <= rtyAlarmOut.notificationMessage)
		&& (0 <= rtyAlarmOut.audioNotificationCommand)
		&& (0 <= rtyAlarmOut.highestLevelAlarm)
		&& (0 <= rtyAlarmOut.logMessageId)
		&& (0 <= rtuImIn.commandedFlowRate)
		&& (rtuImIn.currentSystemMode <= 255)
		&& (rtuOpCmdIn.disableAudio <= 255)
		&& (rtuDbIn.VTBIHigh <= 255)
		&& (rtuDbIn.flowRateHigh <= 255)
		&& (rtuDbIn.flowRateLow <= 255)
		&& (rtuSensorIn.flowRate <= 255)
		&& (rtuConstIn.audioEnableDuration <= 255)
		&& (rtuConstIn.audioLevel <= 255)
		&& (rtuConstIn.configWarningDuration <= 255)
		&& (rtuConstIn.lowReservoir <= 255)
		&& (rtuConstIn.maxConfigDuration <= 255)
		&& (rtuConstIn.maxDurationOverInfusion <= 255)
		&& (rtuConstIn.maxDurationUnderInfusion <= 255)
		&& (rtuConstIn.maxPausedDuration <= 255)
		&& (rtuConstIn.maxIdleDuration <= 255)
		&& (rtuConstIn.toleranceMax <= 255)
		&& (rtuConstIn.toleranceMin <= 255)
		&& (rtuSysStatIn.volumeInfused <= 255)
		&& (rtuSysStatIn.reservoirVolume <= 255)
		&& (rtuConfigIn.configMode <= 255)
		&& (rtyAlarmOut.isAudioDisabled <= 255)
		&& (rtyAlarmOut.notificationMessage <= 255)
		&& (rtyAlarmOut.audioNotificationCommand <= 255)
		&& (rtyAlarmOut.highestLevelAlarm <= 255)
		&& (rtyAlarmOut.logMessageId <= 255)
		&& (rtuImIn.commandedFlowRate <= 255)) {

		var run = 5; // input
		while (run > 0) {
			alarmFunctional(rtuImIn, rtuTlmModeIn, rtuSysMonIn, rtuLoggingIn, rtuOpCmdIn, rtuDbIn, rtuSensorIn, rtuConstIn, rtuSysStatIn, rtuConfigIn, rtyAlarmOut, localB, localDW);
			run = run - 1;
		}

		// Prop1: empty_reservoir_implies_alarm_L4 -- this assertion should pass
		var checkCondition = rtuTlmModeIn.systemOn && rtuSysStatIn.inTherapy && rtuSysStatIn.Reservoir_Empty;
		var checkOutput = rtyAlarmOut.highestLevelAlarm == 4;
		if (!checkCondition || checkOutput) {
			print("success");
		} else {
			throw "error!";
		}
	}
}

prop1();
