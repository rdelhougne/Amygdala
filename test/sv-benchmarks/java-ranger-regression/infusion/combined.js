function AlarmOutputs() {
	this.isAudioDisabled = 42;			// input
	this.notificationMessage = 42;		// input
	this.audioNotificationCommand = 42;	// input
	this.highestLevelAlarm = 42;		// input
	this.logMessageId = 42;				// input
}

function B() {
	// Integer
	this.highestLevelAlarm = 0;
	this.infusionTotalDuration = 0;
	this.vtbiTotal = 0;
	this.flowRateBasal = 0;
	this.flowRateIntermittentBolus = 0;
	this.durationIntermittentBolus = 0;
	this.intervalIntermittentBolus = 0;
	this.flowRatePatientBolus = 0;
	this.durationPatientBolus = 0;
	this.lockoutPeriodPatientBolus = 0;
	this.maxNumberOfPatientBolus = 0;
	this.flowRateKvo = 0;
	this.configured = 0;
	this.volumeInfused = 0;
	this.imOutFlowRateCommanded = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.imOutCurrentSystemMode = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.imOutLogMessageID = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.imOutActualInfusionDuration = 0; /* '<Root>/Infusion Manager Sub-System' */
	// Boolean
	this.infusionInitiate = false;
	this.infusionInhibit = false;
	this.infusionCancel = false;
	this.patientBolusRequest = false;
	this.reservoirEmpty = false;
	this.imOutNewInfusion = false; /* '<Root>/Infusion Manager Sub-System' */
}

function ConfigOutputs() {
	// Integer
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
	// Boolean
	this.requestConfigType = true; // input
	this.requestConfirmInfusionInitiate = true; // input
	this.requestPatientDrugInfo = true; // input
	this.requestInfusionInfo = true; // input
	// Integer
	this.logMessageID = 42; // input
	this.configTimer = 42; // input
	this.configMode = 42; // input
}

function DW() {
	// Integer
	this.isActiveC2InfusionMgrFunctional = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isC2InfusionMgrFunctional = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isInfusionManager = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isTherapy = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isArbiter = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isActiveArbiter = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isAlarmPaused = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isActiveAlarmPaused = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isManualPaused = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isActiveManualPaused = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isBasal = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isActiveBasal = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isArbiterD = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isActiveArbiterC = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isPatient = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isActivePatient = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isIntermittent = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.isActiveIntermittent = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.sbolusTimer = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.pbolusTimer = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.lockTimer = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.numberPbolus = 0; /* '<Root>/Infusion Manager Sub-System' */
	this.sbolusInterTimer = 0; /* '<Root>/Infusion Manager Sub-System' */
	// Boolean
	this.sbolusReq = false; /* '<Root>/Infusion Manager Sub-System' */
	this.inPatientBolus = false; /* '<Root>/Infusion Manager Sub-System' */
}

function InfusionManagerOutputs() {
	// Integer
	this.commandedFlowRate = 42; // input
	this.currentSystemMode = 42; // input
	this.logMessageId = 42; // input
	this.actualInfusionDuration = 42; // input
	// Boolean
	this.newInfusion = true; // input
}

function OperatorCommands() {
	// Boolean
	this.systemStart = true; // input
	this.systemStop = true; // input
	this.infusionInitiate = true; // input
	this.infusionInhibit = true; // input
	this.infusionCancel = true; // input
	this.dataConfig = true; // input
	this.next = true; // input
	this.back = true; // input
	this.cancel = true; // input
	this.keyboard = true; // input
	this.notificationCancel = true; // input
	this.confirmStop = true; // input
	// Integer
	this.disableAudio = 42; // input
	this.configurationType = 42; // input
}

function PatientInputs() {
	this.patientBolusRequest = true; // input
}

function SystemStatusOutputs() {
	// Boolean
	this.reservoirEmpty = true; // input
	this.inTherapy = true; // input
	// Integer
	this.reservoirVolume = 42; // input
	this.volumeInfused = 42; // input
	this.logMessageId = 42; // input
}

function TopLevelModeOutputs() {
	// Boolean
	this.systemOn = true; // input
	this.requestConfirmStop = true; // input
	// Integer
	this.logMessageID = 42; // input
}

var IN_ACTIVE = 1;
var IN_Basal = 1;
var IN_IDLE = 1;
var IN_INFUSION_MANAGER = 1;
var IN_INTERMITTENT_BOLUS = 2;
var IN_LOCKOUT = 1;
var IN_MANUAL_PAUSED_KVO = 1;
var IN_NOT_ON = 2;
var IN_NO_ACTIVE_CHILD = 0;
var IN_OFF = 1;
var IN_OFF_B = 2;
var IN_ON = 2;
var IN_ON_B = 3;
var IN_PAUSED = 2;
var IN_Patient_Bolus = 3;
var IN_PAUSED_KVO = 2;
var IN_PAUSED_NoKVO = 3;
var IN_THERAPY = 2;


function stepScalingFactor(inputVal) {
	/* Graphical Function 'Step_Scaling_Factor': '<S1>:4016' */
	/* Transition: '<S1>:4013' */
	return inputVal;
}

/* Function for Chart: '<Root>/Infusion Manager Sub-System' */
function writeLog(logEvent, localB) {
	/* Graphical Function 'writeLog': '<S1>:3724' */
	/* Transition: '<S1>:3726' */
	localB.imOutLogMessageID = logEvent;
}

/* Function for Chart: '<Root>/Infusion Manager Sub-System' */
function resetForNewInfusion(localB, localDW) {
	/* Graphical Function 'resetForNewInfusion': '<S1>:3956' */
	/* Transition: '<S1>:3958' */
	localDW.sbolusTimer = 0;
	localDW.pbolusTimer = 0;
	localDW.numberPbolus = 0;
	localDW.sbolusInterTimer = 0;
	localB.imOutFlowRateCommanded = 0;
	localB.imOutActualInfusionDuration = 0;
	writeLog(1, localB);
}

/* Function for Chart: '<Root>/Infusion Manager Sub-System' */
function exitInternalActive(localDW) {
	/* Exit Internal 'ACTIVE': '<S1>:3905' */
	/* Exit Internal 'Arbiter': '<S1>:3913' */
	localDW.isArbiterD = IN_NO_ACTIVE_CHILD;
	localDW.isActiveArbiterC = 0;

	/* Exit Internal 'INTERMITTENT': '<S1>:3936' */
	if (localDW.isIntermittent == IN_ON) {
		/* Exit 'ON': '<S1>:3941' */
		localDW.sbolusTimer++;
		localDW.sbolusReq = false;
		localDW.isIntermittent = IN_NO_ACTIVE_CHILD;
	} else {
		localDW.isIntermittent = IN_NO_ACTIVE_CHILD;
	}

	localDW.isActiveIntermittent = 0;

	/* Exit Internal 'PATIENT': '<S1>:3927' */
	if (localDW.isPatient == IN_ON_B) {
		/* Exit 'ON': '<S1>:3934' */
		localDW.pbolusTimer++;
		localDW.isPatient = IN_NO_ACTIVE_CHILD;
	} else {
		localDW.isPatient = IN_NO_ACTIVE_CHILD;
	}

	localDW.isActivePatient = 0;

	/* Exit Internal 'BASAL': '<S1>:3907' */
	localDW.isBasal = IN_NO_ACTIVE_CHILD;
	localDW.isActiveBasal = 0;
}

/* Function for Chart: '<Root>/Infusion Manager Sub-System' */
function exit_internal_PAUSED(localB, localDW) {
	/* Exit Internal 'PAUSED': '<S1>:3876' */
	/* Exit Internal 'Arbiter': '<S1>:3877' */

	if (localDW.isArbiter == IN_MANUAL_PAUSED_KVO) {
		/* Exit 'Manual_Paused_KVO': '<S1>:3892' */
		localB.imOutFlowRateCommanded = localB.flowRateKvo;
		localB.imOutCurrentSystemMode = 8;
		localDW.isArbiter = IN_NO_ACTIVE_CHILD;
	} else if (localDW.isArbiter == IN_PAUSED_KVO) {
		/* Exit 'Paused_KVO': '<S1>:3891' */
		localB.imOutFlowRateCommanded = localB.flowRateKvo;
		localB.imOutCurrentSystemMode = 7;
		localDW.isArbiter = IN_NO_ACTIVE_CHILD;
	} else if (localDW.isArbiter == IN_PAUSED_NoKVO) {
		/* Exit 'Paused_NoKVO': '<S1>:3890' */
		localB.imOutFlowRateCommanded = 0;
		localB.imOutCurrentSystemMode = 6;
		localDW.isArbiter = IN_NO_ACTIVE_CHILD;
	} else localDW.isArbiter = IN_NO_ACTIVE_CHILD;

	localDW.isActiveArbiter = 0;

	/* Exit Internal 'Manual_Paused': '<S1>:3899' */
	localDW.isManualPaused = IN_NO_ACTIVE_CHILD;
	localDW.isActiveManualPaused = 0;

	/* Exit Internal 'Alarm_Paused': '<S1>:3893' */
	localDW.isAlarmPaused = IN_NO_ACTIVE_CHILD;
	localDW.isActiveAlarmPaused = 0;
}

function therapyExitOperations(localB) {
	/* Graphical Function 'TherapyExitOperations': '<S1>:3953' */
	/* Transition: '<S1>:3955' */
	localB.imOutFlowRateCommanded = 0;
	localB.imOutNewInfusion = false;
}

function sbolus_trigger(localB, localDW) {
	var sb;

	/* Graphical Function 'sbolus_trigger': '<S1>:3943' */
	/* Transition: '<S1>:3947' */
	sb = 0;

	var scalingFactor = stepScalingFactor(localB.intervalIntermittentBolus);
	var sbolusInterTimer_l = localDW.sbolusInterTimer;

	if (!((sbolusInterTimer_l > scalingFactor || (sbolusInterTimer_l < scalingFactor)) || (!(sbolusInterTimer_l == scalingFactor)))) {
		/* Transition: '<S1>:3949' */
		sb = 1;
		localDW.sbolusInterTimer = 0;
	} else {
		/* Transition: '<S1>:3948' */
	}

	return sb;
}

/* Function for Chart: '<Root>/Infusion Manager Sub-System' */
function enterInternalActive(localB, localDW) {
	/* Entry Internal 'ACTIVE': '<S1>:3905' */
	localDW.isActiveBasal = 1;

	/* Entry Internal 'BASAL': '<S1>:3907' */
	/* Transition: '<S1>:3908' */
	localDW.isBasal = IN_ON;
	localDW.isActivePatient = 1;

	/* Entry Internal 'PATIENT': '<S1>:3927' */
	if (localDW.inPatientBolus) {
		/* Transition: '<S1>:3930' */
		localDW.isPatient = IN_LOCKOUT;

		/* Entry 'LOCKOUT': '<S1>:3935' */
		localDW.lockTimer++;
	} else {
		/* Transition: '<S1>:3928' */
		localDW.isPatient = IN_OFF_B;

		/* Entry 'OFF': '<S1>:3933' */
		localDW.pbolusTimer = 0;
		localDW.inPatientBolus = false;
	}

	localDW.isActiveIntermittent = 1;

	/* Entry 'INTERMITTENT': '<S1>:3936' */
	localDW.sbolusInterTimer++;

	/* Entry Internal 'INTERMITTENT': '<S1>:3936' */
	/* Transition: '<S1>:3937' */
	localDW.isIntermittent = IN_OFF;

	/* Entry 'OFF': '<S1>:3940' */
	localDW.sbolusTimer = 0;
	localDW.sbolusReq = false;
	localDW.sbolusReq = (sbolus_trigger(localB, localDW) != 0);
	localDW.isActiveArbiterC = 1;

	/* Entry Internal 'Arbiter': '<S1>:3913' */
	/* Transition: '<S1>:3916' */
	if (localDW.isPatient == IN_ON_B) {
		/* Transition: '<S1>:3917' */
		localDW.isArbiterD = IN_Patient_Bolus;

		/* Entry 'Patient_Bolus': '<S1>:3924' */
		localB.imOutFlowRateCommanded = localB.flowRatePatientBolus;
		localB.imOutCurrentSystemMode = 4;
	} else if (localDW.isIntermittent == IN_ON) {
		/* Transition: '<S1>:3918' */
		localDW.isArbiterD = IN_INTERMITTENT_BOLUS;

		/* Entry 'Intermittent_Bolus': '<S1>:3925' */
		localB.imOutFlowRateCommanded = localB.flowRateIntermittentBolus;
		localB.imOutCurrentSystemMode = 3;
	} else {
		/* Transition: '<S1>:3919' */
		localDW.isArbiterD = IN_Basal;

		/* Entry 'Basal': '<S1>:3926' */
		localB.imOutFlowRateCommanded = localB.flowRateBasal;
		localB.imOutCurrentSystemMode = 2;
	}
}

/* Function for Chart: '<Root>/Infusion Manager Sub-System' */
function enterInternalPaused(localB, localDW) {
	/* Entry Internal 'PAUSED': '<S1>:3876' */
	localDW.isActiveAlarmPaused = 1;

	/* Entry Internal 'Alarm_Paused': '<S1>:3893' */
	if (localB.highestLevelAlarm >= 3) {
		/* Transition: '<S1>:3964' */
		localDW.isAlarmPaused = IN_ON;
	} else {
		/* Transition: '<S1>:3894' */
		localDW.isAlarmPaused = IN_OFF;
	}

	localDW.isActiveManualPaused = 1;

	/* Entry Internal 'Manual_Paused': '<S1>:3899' */
	if (localB.infusionInhibit) {
		/* Transition: '<S1>:3965' */
		localDW.isManualPaused = IN_ON;
	} else {
		/* Transition: '<S1>:3900' */
		localDW.isManualPaused = IN_OFF;
	}

	localDW.isActiveArbiter = 1;

	/* Entry Internal 'Arbiter': '<S1>:3877' */
	/* Transition: '<S1>:3881' */
	var isAlarmPaused_l = localDW.isAlarmPaused;
	var highestLevelAlarm_l = localB.highestLevelAlarm;
	if ((isAlarmPaused_l == IN_ON) && (highestLevelAlarm_l == 4)) {
		/* Transition: '<S1>:3882' */
		localDW.isArbiter = IN_PAUSED_NoKVO;

		/* Entry 'Paused_NoKVO': '<S1>:3890' */
		localB.imOutFlowRateCommanded = 0;
		localB.imOutCurrentSystemMode = 6;
	} else if ((isAlarmPaused_l == IN_ON) && (highestLevelAlarm_l == 3)) {
		/* Transition: '<S1>:3884' */
		localDW.isArbiter = IN_PAUSED_KVO;

		/* Entry 'Paused_KVO': '<S1>:3891' */
		localB.imOutFlowRateCommanded = localB.flowRateKvo;
		localB.imOutCurrentSystemMode = 7;
	} else {
		/* Transition: '<S1>:3883' */
		localDW.isArbiter = IN_MANUAL_PAUSED_KVO;

		/* Entry 'Manual_Paused_KVO': '<S1>:3892' */
		localB.imOutFlowRateCommanded = localB.flowRateKvo;
		localB.imOutCurrentSystemMode = 8;
	}
}

/* Function for Chart: '<Root>/Infusion Manager Sub-System' */
function resetAllInfusionDetails(localB, localDW) {
	/* Graphical Function 'resetAllInfusionDetails': '<S1>:3959' */
	/* Transition: '<S1>:3961' */
	resetForNewInfusion(localB, localDW);
	localDW.lockTimer = 0;
	localDW.inPatientBolus = false;
	localDW.numberPbolus = 0;
}

function therapy(localB, localDW) {
	// DB_prinTF("1: %2x %2x ",localB.infusionInitiate,localB.reservoirEmpty);

	/* During 'THERAPY': '<S1>:3867' */
	var infusioninitiateL = localB.infusionInitiate;
	var reservoiremptyL = localB.reservoirEmpty;
	var configuredL = localB.configured;
	var infusionCancelL = localB.infusionCancel;

	if ((infusioninitiateL && reservoiremptyL) || (configuredL < 1) || infusionCancelL) {
		/* Transition: '<S1>:3987' */
		/* Exit Internal 'THERAPY': '<S1>:3867' */
		if (localDW.isTherapy == IN_ACTIVE) {
			exitInternalActive(localDW);
			localDW.isTherapy = IN_NO_ACTIVE_CHILD;
		} else if (localDW.isTherapy == IN_PAUSED) {
			exit_internal_PAUSED(localB, localDW);
			localDW.isTherapy = IN_NO_ACTIVE_CHILD;
		} else localDW.isTherapy = IN_NO_ACTIVE_CHILD;

		/* Exit 'THERAPY': '<S1>:3867' */
		therapyExitOperations(localB);
		localDW.isInfusionManager = IN_IDLE;

		/* Entry 'IDLE': '<S1>:3866' */
		localB.imOutCurrentSystemMode = 1;
		localB.imOutFlowRateCommanded = 0;
		resetAllInfusionDetails(localB, localDW);
	} else if (infusioninitiateL && (configuredL == 1) && reservoiremptyL) {
		/* Transition: '<S1>:3861' */
		resetForNewInfusion(localB, localDW);

		/* Transition: '<S1>:3863' */
		localB.imOutNewInfusion = true;

		/* Exit Internal 'THERAPY': '<S1>:3867' */
		if (localDW.isTherapy == IN_ACTIVE) {
			exitInternalActive(localDW);
			localDW.isTherapy = IN_NO_ACTIVE_CHILD;
		} else if (localDW.isTherapy == IN_PAUSED) {
			exit_internal_PAUSED(localB, localDW);
			localDW.isTherapy = IN_NO_ACTIVE_CHILD;
		} else localDW.isTherapy = IN_NO_ACTIVE_CHILD;

		/* Exit 'THERAPY': '<S1>:3867' */
		therapyExitOperations(localB);
		localDW.isInfusionManager = IN_THERAPY;

		/* Entry Internal 'THERAPY': '<S1>:3867' */
		var infusionInhibitL = localB.infusionInhibit;
		var highestLevelAlarm_l = localB.highestLevelAlarm;

		if (infusionInhibitL || (highestLevelAlarm_l >= 3)) {
			/* Transition: '<S1>:3994' */
			localDW.isTherapy = IN_PAUSED;
			enterInternalPaused(localB, localDW);
		} else {
			/* Transition: '<S1>:3875' */
			localDW.isTherapy = IN_ACTIVE;
			enterInternalActive(localB, localDW);
		}
	} else {
		var imOutActualInfusionDurationL = localB.imOutActualInfusionDuration;
		var volumeInfusedL = localB.volumeInfused;
		var vtbiTotalL = localB.vtbiTotal;
		var scalingFactor = stepScalingFactor(localB.infusionTotalDuration - 1);
		if ((imOutActualInfusionDurationL >= scalingFactor) || (volumeInfusedL >= vtbiTotalL)) {
			/* Transition: '<S1>:3865' */
			/* Exit Internal 'THERAPY': '<S1>:3867' */

			if (localDW.isTherapy == IN_ACTIVE) {
				exitInternalActive(localDW);
				localDW.isTherapy = IN_NO_ACTIVE_CHILD;
			} else if (localDW.isTherapy == IN_PAUSED) {
				exit_internal_PAUSED(localB, localDW);
				localDW.isTherapy = IN_NO_ACTIVE_CHILD;
			} else localDW.isTherapy = IN_NO_ACTIVE_CHILD;

			/* Exit 'THERAPY': '<S1>:3867' */
			therapyExitOperations(localB);
			localDW.isInfusionManager = IN_IDLE;

			/* Entry 'IDLE': '<S1>:3866' */
			localB.imOutCurrentSystemMode = 1;
			localB.imOutFlowRateCommanded = 0;
			resetAllInfusionDetails(localB, localDW);
		} else {
			localB.imOutNewInfusion = false;
			if (localDW.isTherapy == IN_ACTIVE) {
				/* During 'ACTIVE': '<S1>:3905' */
				var infusionInhibitL = localB.infusionInhibit;
				var highestLevelAlarmL = localB.highestLevelAlarm;
				if (infusionInhibitL || (highestLevelAlarmL >= 3)) {
					/* Transition: '<S1>:3871' */
					exitInternalActive(localDW);
					localDW.isTherapy = IN_PAUSED;
					enterInternalPaused(localB, localDW);
				} else {
					/* During 'BASAL': '<S1>:3907' */
					if (localDW.isBasal == IN_OFF) {
						/* During 'OFF': '<S1>:3912' */
						if (localB.infusionInitiate) {
							/* Transition: '<S1>:3909' */
							localDW.isBasal = IN_ON;
						}
					} else {
						/* During 'ON': '<S1>:3911' */
						scalingFactor = stepScalingFactor(localB.infusionTotalDuration - 1);
						if (localB.imOutActualInfusionDuration >= scalingFactor) {
							/* Transition: '<S1>:3910' */
							localDW.isBasal = IN_OFF;
						}
					}

					/* During 'PATIENT': '<S1>:3927' */

					if (localDW.isPatient == IN_LOCKOUT) {
						/* During 'LOCKOUT': '<S1>:3935' */
						scalingFactor = stepScalingFactor(localB.lockoutPeriodPatientBolus - 1);
						if (localDW.lockTimer >= scalingFactor) {
							/* Transition: '<S1>:3931' */
							localDW.isPatient = IN_OFF_B;
							/* Entry 'OFF': '<S1>:3933' */
							localDW.pbolusTimer = 0;
							localDW.inPatientBolus = false;
						} else {
							localDW.lockTimer++;
						}
					} else if (localDW.isPatient == IN_OFF_B) {
						/* During 'OFF': '<S1>:3933' */
						var patientBolusRequestL = localB.patientBolusRequest;
						highestLevelAlarmL = localB.highestLevelAlarm;
						var numberPbolusL = localDW.numberPbolus;
						var maxNumberOfPatientBolusL = localB.maxNumberOfPatientBolus;

						if (patientBolusRequestL
							&& (highestLevelAlarmL < 2)
							&& (numberPbolusL < maxNumberOfPatientBolusL)) {
							/* Transition: '<S1>:3929' */
							localDW.isPatient = IN_ON_B;

							/* Entry 'ON': '<S1>:3934' */
							localDW.numberPbolus++;
							localDW.inPatientBolus = true;
							localDW.pbolusTimer++;
						}
					} else {
						/* During 'ON': '<S1>:3934' */
						scalingFactor = stepScalingFactor(localB.durationPatientBolus - 1);
						var pbolusTimerL = localDW.pbolusTimer;
						highestLevelAlarmL = localB.highestLevelAlarm;
						if ((pbolusTimerL >= scalingFactor) || (highestLevelAlarmL == 2)) {
							/* Transition: '<S1>:3932' */
							localDW.lockTimer = 0;

							/* Exit 'ON': '<S1>:3934' */
							localDW.pbolusTimer++;
							localDW.isPatient = IN_LOCKOUT;

							/* Entry 'LOCKOUT': '<S1>:3935' */
							localDW.lockTimer++;
						} else {
							localDW.pbolusTimer++;
						}
					}

					/* During 'INTERMITTENT': '<S1>:3936' */
					localDW.sbolusInterTimer++;
					if (localDW.isIntermittent == IN_OFF) {
						/* During 'OFF': '<S1>:3940' */
						var sbolusReq_l = localDW.sbolusReq;
						highestLevelAlarmL = localB.highestLevelAlarm;
						highestLevelAlarmL = localB.highestLevelAlarm;

						if (sbolusReq_l && (highestLevelAlarmL < 2)) {
							/* Transition: '<S1>:3938' */
							localDW.isIntermittent = IN_ON;
						} else {
							localDW.sbolusReq = (sbolus_trigger(localB, localDW) != 0);
						}
					} else {
						/* During 'ON': '<S1>:3941' */
						scalingFactor = stepScalingFactor(localB.durationIntermittentBolus - 1);
						var sbolusTimerL = localDW.sbolusTimer;
						highestLevelAlarmL = localB.highestLevelAlarm;
						if ((sbolusTimerL >= scalingFactor) || (highestLevelAlarmL == 2)) {
							/* Transition: '<S1>:3939' */
							/* Exit 'ON': '<S1>:3941' */
							localDW.isIntermittent = IN_OFF;

							/* Entry 'OFF': '<S1>:3940' */
							localDW.sbolusTimer = 0;
							localDW.sbolusReq = false;
							localDW.sbolusReq = (sbolus_trigger(localB, localDW) != 0);
						} else {
							localDW.sbolusTimer++;
							localDW.sbolusReq = false;
						}
					}

					/* During 'Arbiter': '<S1>:3913' */
					if (localDW.isArbiterD == IN_Basal) {
						/* During 'Basal': '<S1>:3926' */
						/* Transition: '<S1>:3923' */
						/* Transition: '<S1>:3920' */
						localB.imOutActualInfusionDuration++;

						/* Transition: '<S1>:4002' */
						if (localDW.isPatient == IN_ON_B) {
							/* Transition: '<S1>:3917' */
							localDW.isArbiterD = IN_Patient_Bolus;

							/* Entry 'Patient_Bolus': '<S1>:3924' */
							localB.imOutFlowRateCommanded = localB.flowRatePatientBolus;
							localB.imOutCurrentSystemMode = 4;
						} else if (localDW.isIntermittent == IN_ON) {
							/* Transition: '<S1>:3918' */
							localDW.isArbiterD = IN_INTERMITTENT_BOLUS;

							/* Entry 'Intermittent_Bolus': '<S1>:3925' */
							localB.imOutFlowRateCommanded = localB.flowRateIntermittentBolus;
							localB.imOutCurrentSystemMode = 3;
						} else {
							/* Transition: '<S1>:3919' */
							localDW.isArbiterD = IN_Basal;

							/* Entry 'Basal': '<S1>:3926' */
							localB.imOutFlowRateCommanded = localB.flowRateBasal;
							localB.imOutCurrentSystemMode = 2;
						}
					} else if (localDW.isArbiterD == IN_INTERMITTENT_BOLUS) {
						/* During 'Intermittent_Bolus': '<S1>:3925' */
						/* Transition: '<S1>:3922' */
						/* Transition: '<S1>:3920' */
						localB.imOutActualInfusionDuration++;

						/* Transition: '<S1>:4002' */
						if (localDW.isPatient == IN_ON_B) {
							/* Transition: '<S1>:3917' */
							localDW.isArbiterD = IN_Patient_Bolus;

							/* Entry 'Patient_Bolus': '<S1>:3924' */
							localB.imOutFlowRateCommanded = localB.flowRatePatientBolus;
							localB.imOutCurrentSystemMode = 4;
						} else if (localDW.isIntermittent == IN_ON) {
							/* Transition: '<S1>:3918' */
							localDW.isArbiterD = IN_INTERMITTENT_BOLUS;

							/* Entry 'Intermittent_Bolus': '<S1>:3925' */
							localB.imOutFlowRateCommanded = localB.flowRateIntermittentBolus;
							localB.imOutCurrentSystemMode = 3;
						} else {
							/* Transition: '<S1>:3919' */
							localDW.isArbiterD = IN_Basal;

							/* Entry 'Basal': '<S1>:3926' */
							localB.imOutFlowRateCommanded = localB.flowRateBasal;
							localB.imOutCurrentSystemMode = 2;
						}
					} else {
						/* During 'Patient_Bolus': '<S1>:3924' */
						/* Transition: '<S1>:3921' */
						/* Transition: '<S1>:3920' */
						localB.imOutActualInfusionDuration++;

						/* Transition: '<S1>:4002' */
						if (localDW.isPatient == IN_ON_B) {
							/* Transition: '<S1>:3917' */
							localDW.isArbiterD = IN_Patient_Bolus;

							/* Entry 'Patient_Bolus': '<S1>:3924' */
							localB.imOutFlowRateCommanded = localB.flowRatePatientBolus;
							localB.imOutCurrentSystemMode = 4;
						} else if (localDW.isIntermittent == IN_ON) {
							/* Transition: '<S1>:3918' */
							localDW.isArbiterD = IN_INTERMITTENT_BOLUS;

							/* Entry 'Intermittent_Bolus': '<S1>:3925' */
							localB.imOutFlowRateCommanded = localB.flowRateIntermittentBolus;
							localB.imOutCurrentSystemMode = 3;
						} else {
							/* Transition: '<S1>:3919' */
							localDW.isArbiterD = IN_Basal;

							/* Entry 'Basal': '<S1>:3926' */
							localB.imOutFlowRateCommanded = localB.flowRateBasal;
							localB.imOutCurrentSystemMode = 2;
						}
					}
				}
			} else {
				/* During 'PAUSED': '<S1>:3876' */
				infusioninitiateL = localB.infusionInitiate;
				var highestLevelAlarmL = localB.highestLevelAlarm;
				var infusionInhibitL = localB.infusionInhibit;
				if (infusioninitiateL && (highestLevelAlarmL < 3) && (!infusionInhibitL)) {
					/* Transition: '<S1>:3870' */
					exit_internal_PAUSED(localB, localDW);
					localDW.isTherapy = IN_ACTIVE;
					enterInternalActive(localB, localDW);
				} else {
					/* During 'Alarm_Paused': '<S1>:3893' */
					if (localDW.isAlarmPaused == IN_OFF) {
						/* During 'OFF': '<S1>:3897' */
						if (localB.highestLevelAlarm >= 3) {
							/* Transition: '<S1>:3895' */
							localDW.isAlarmPaused = IN_ON;
						}
					} else {
						/* During 'ON': '<S1>:3898' */
						infusioninitiateL = localB.infusionInitiate;
						highestLevelAlarmL = localB.highestLevelAlarm;
						if (infusioninitiateL && (highestLevelAlarmL < 3)) {
							/* Transition: '<S1>:3896' */
							localDW.isAlarmPaused = IN_OFF;
						}
					}

					/* During 'Manual_Paused': '<S1>:3899' */
					if (localDW.isManualPaused == IN_OFF) {
						/* During 'OFF': '<S1>:3903' */
						if (localB.infusionInhibit) {
							/* Transition: '<S1>:3901' */
							localDW.isManualPaused = IN_ON;
						}
					} else {
						/* During 'ON': '<S1>:3904' */
						infusioninitiateL = localB.infusionInitiate;
						infusionInhibitL = localB.infusionInhibit;
						if (infusioninitiateL && (!infusionInhibitL)) {
							/* Transition: '<S1>:3902' */
							localDW.isManualPaused = IN_OFF;
						}
					}

					/* During 'Arbiter': '<S1>:3877' */
					if (localDW.isArbiter == IN_MANUAL_PAUSED_KVO) {
						/* During 'Manual_Paused_KVO': '<S1>:3892' */
						/* Transition: '<S1>:3888' */
						/* Transition: '<S1>:3889' */
						/* Transition: '<S1>:3885' */
						var isAlarmPausedL = localDW.isAlarmPaused;
						highestLevelAlarmL = localB.highestLevelAlarm;
						if ((isAlarmPausedL == IN_ON) && (highestLevelAlarmL == 4)) {
							/* Transition: '<S1>:3882' */
							/* Exit 'Manual_Paused_KVO': '<S1>:3892' */
							localDW.isArbiter = IN_PAUSED_NoKVO;

							/* Entry 'Paused_NoKVO': '<S1>:3890' */
							localB.imOutFlowRateCommanded = 0;
							localB.imOutCurrentSystemMode = 6;
						} else if ((isAlarmPausedL == IN_ON) && (highestLevelAlarmL == 3)) {
							/* Transition: '<S1>:3884' */
							/* Exit 'Manual_Paused_KVO': '<S1>:3892' */
							localDW.isArbiter = IN_PAUSED_KVO;

							/* Entry 'Paused_KVO': '<S1>:3891' */
							localB.imOutFlowRateCommanded = localB.flowRateKvo;
							localB.imOutCurrentSystemMode = 7;
						} else {
							/* Transition: '<S1>:3883' */
							/* Exit 'Manual_Paused_KVO': '<S1>:3892' */
							localDW.isArbiter = IN_MANUAL_PAUSED_KVO;

							/* Entry 'Manual_Paused_KVO': '<S1>:3892' */
							localB.imOutFlowRateCommanded = localB.flowRateKvo;
							localB.imOutCurrentSystemMode = 8;
						}
					} else if (localDW.isArbiter == IN_PAUSED_KVO) {
						var isAlarmPausedL = localDW.isAlarmPaused;
						highestLevelAlarmL = localB.highestLevelAlarm;

						/* During 'Paused_KVO': '<S1>:3891' */
						/* Transition: '<S1>:3887' */
						/* Transition: '<S1>:3889' */
						/* Transition: '<S1>:3885' */
						if ((isAlarmPausedL == IN_ON) && (highestLevelAlarmL == 4)) {
							/* Transition: '<S1>:3882' */
							/* Exit 'Paused_KVO': '<S1>:3891' */
							localDW.isArbiter = IN_PAUSED_NoKVO;

							/* Entry 'Paused_NoKVO': '<S1>:3890' */
							localB.imOutFlowRateCommanded = 0;
							localB.imOutCurrentSystemMode = 6;
						} else if ((isAlarmPausedL == IN_ON) && (highestLevelAlarmL == 3)) {
							/* Transition: '<S1>:3884' */
							/* Exit 'Paused_KVO': '<S1>:3891' */
							localDW.isArbiter = IN_PAUSED_KVO;

							/* Entry 'Paused_KVO': '<S1>:3891' */
							localB.imOutFlowRateCommanded = localB.flowRateKvo;
							localB.imOutCurrentSystemMode = 7;
						} else {
							/* Transition: '<S1>:3883' */
							/* Exit 'Paused_KVO': '<S1>:3891' */
							localDW.isArbiter = IN_MANUAL_PAUSED_KVO;

							/* Entry 'Manual_Paused_KVO': '<S1>:3892' */
							localB.imOutFlowRateCommanded = localB.flowRateKvo;
							localB.imOutCurrentSystemMode = 8;
						}
					} else {
						/* During 'Paused_NoKVO': '<S1>:3890' */
						/* Transition: '<S1>:3886' */
						/* Transition: '<S1>:3889' */
						/* Transition: '<S1>:3885' */
						var isAlarmPausedL = localDW.isAlarmPaused;
						highestLevelAlarmL = localB.highestLevelAlarm;

						if ((isAlarmPausedL == IN_ON) && (highestLevelAlarmL == 4)) {
							/* Transition: '<S1>:3882' */
							/* Exit 'Paused_NoKVO': '<S1>:3890' */
							localDW.isArbiter = IN_PAUSED_NoKVO;

							/* Entry 'Paused_NoKVO': '<S1>:3890' */
							localB.imOutFlowRateCommanded = 0;
							localB.imOutCurrentSystemMode = 6;
						} else if ((isAlarmPausedL == IN_ON) && (highestLevelAlarmL == 3)) {
							/* Transition: '<S1>:3884' */
							/* Exit 'Paused_NoKVO': '<S1>:3890' */
							localDW.isArbiter = IN_PAUSED_KVO;

							/* Entry 'Paused_KVO': '<S1>:3891' */
							localB.imOutFlowRateCommanded = localB.flowRateKvo;
							localB.imOutCurrentSystemMode = 7;
						} else {
							/* Transition: '<S1>:3883' */
							/* Exit 'Paused_NoKVO': '<S1>:3890' */
							localDW.isArbiter = IN_MANUAL_PAUSED_KVO;

							/* Entry 'Manual_Paused_KVO': '<S1>:3892' */
							localB.imOutFlowRateCommanded = localB.flowRateKvo;
							localB.imOutCurrentSystemMode = 8;
						}
					}
				}
			}
		}
	}
}

/* Initial conditions for referenced model: 'infusionMgrFunctional' */
function init(localB, localDW) {
	/* InitializeConditions for Chart: '<Root>/Infusion Manager Sub-System' */
	/*localDW.isInfusionManager = IN_NO_ACTIVE_CHILD;
	localDW.isTherapy = IN_NO_ACTIVE_CHILD;
	localDW.isActiveArbiterC = 0;
	localDW.isArbiterD = IN_NO_ACTIVE_CHILD;
	localDW.isActiveBasal = 0;
	localDW.isBasal = IN_NO_ACTIVE_CHILD;
	localDW.isActiveIntermittent = 0;
	localDW.isIntermittent = IN_NO_ACTIVE_CHILD;
	localDW.isActivePatient = 0;
	localDW.isPatient = IN_NO_ACTIVE_CHILD;
	localDW.isActiveAlarmPaused = 0;
	localDW.isAlarmPaused = IN_NO_ACTIVE_CHILD;
	localDW.isActiveArbiter = 0;
	localDW.isArbiter = IN_NO_ACTIVE_CHILD;
	localDW.isActiveManualPaused = 0;
	localDW.isManualPaused = IN_NO_ACTIVE_CHILD;
	localDW.isActiveC2InfusionMgrFunctional = 0;
	localDW.isC2InfusionMgrFunctional =
			IN_NO_ACTIVE_CHILD;
	localDW.sbolusReq = false;
	localDW.sbolusTimer = 0;
	localDW.pbolusTimer = 0;
	localDW.lockTimer = 0;
	localDW.numberPbolus = 0;
	localDW.inPatientBolus = false;
	localDW.sbolusInterTimer = 0;
	localB.imOutFlowRateCommanded = 0;
	localB.imOutCurrentSystemMode = 0;
	localB.imOutNewInfusion = false;
	localB.IM_OUT_Log_Message_ID = 0;
	localB.imOutActualInfusionDuration = 0;*/
}

/* Output and update for referenced model: 'infusionMgrFunctional' */
function infusionMgrFunctional(rtuTlmModeIn, rtuOpCmdIn, rtuPatientIn, rtuConfigIn, rtuAlarmIn, rtuSysStatIn, rtyImOut, localB, localDW) {
	/* BusSelector: '<Root>/BusConversion_InsertedFor_ALARM_IN_at_outport_0' */
	localB.highestLevelAlarm = rtuAlarmIn.highestLevelAlarm;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_CONFIG_IN_at_outport_0' */
	localB.infusionTotalDuration = rtuConfigIn.infusionTotalDuration;
	localB.vtbiTotal = rtuConfigIn.vtbiTotal;
	localB.flowRateBasal = rtuConfigIn.flowRateBasal;
	localB.flowRateIntermittentBolus = rtuConfigIn.flowRateIntermittentBolus;
	localB.durationIntermittentBolus = rtuConfigIn.durationIntermittentBolus;
	localB.intervalIntermittentBolus = rtuConfigIn.intervalIntermittentBolus;
	localB.flowRatePatientBolus = rtuConfigIn.flowRatePatientBolus;
	localB.durationPatientBolus = rtuConfigIn.durationPatientBolus;
	localB.lockoutPeriodPatientBolus = rtuConfigIn.lockoutPeriodPatientBolus;
	localB.maxNumberOfPatientBolus = rtuConfigIn.maxNumberOfPatientBolus;
	localB.flowRateKvo = rtuConfigIn.flowRateKVO;
	localB.configured = rtuConfigIn.configured;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_OP_CMD_IN_at_outport_0' */
	localB.infusionInitiate = rtuOpCmdIn.infusionInitiate;
	localB.infusionInhibit = rtuOpCmdIn.infusionInhibit;
	localB.infusionCancel = rtuOpCmdIn.infusionCancel;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_PATIENT_IN_at_outport_0' */
	localB.patientBolusRequest = rtuPatientIn.patientBolusRequest;

	/* BusSelector: '<Root>/BusConversion_InsertedFor_SYS_STAT_IN_at_outport_0' */
	localB.reservoirEmpty = rtuSysStatIn.reservoirEmpty;
	localB.volumeInfused = rtuSysStatIn.volumeInfused;

	/* Chart: '<Root>/Infusion Manager Sub-System' incorporates:
		*  BusSelector: '<Root>/BusConversion_InsertedFor_TLM_MODE_IN_at_outport_0'
		*/
	/* Gateway: Infusion Manager Sub-System */
	/* During: Infusion Manager Sub-System */
	// DB_prinTF("10: ");
	if (localDW.isActiveC2InfusionMgrFunctional == 0) {
		/* Entry: Infusion Manager Sub-System */
		// DB_prinTF("11: ");
		localDW.isActiveC2InfusionMgrFunctional = 1;

		/* Entry Internal: Infusion Manager Sub-System */
		if (rtuTlmModeIn.systemOn) {
			/* Transition: '<S1>:3986' */
			localDW.isC2InfusionMgrFunctional = IN_INFUSION_MANAGER;

			/* Entry Internal 'Infusion_Manager': '<S1>:3858' */
			/* Transition: '<S1>:3860' */
			localDW.isInfusionManager = IN_IDLE;

			/* Entry 'IDLE': '<S1>:3866' */
			localB.imOutCurrentSystemMode = 1;
			localB.imOutFlowRateCommanded = 0;
			resetAllInfusionDetails(localB, localDW);
		} else {
			/* Transition: '<S1>:3744' */
			localDW.isC2InfusionMgrFunctional = IN_NOT_ON;

			/* Entry 'NOT_ON': '<S1>:3740' */
			localB.imOutCurrentSystemMode = 0;
			localB.imOutFlowRateCommanded = 0;
		}
	} else if (localDW.isC2InfusionMgrFunctional == IN_INFUSION_MANAGER) {
		/* During 'Infusion_Manager': '<S1>:3858' */
		// DB_prinTF("12: ");
		if (!rtuTlmModeIn.systemOn) {
			/* Transition: '<S1>:3732' */
			/* Exit Internal 'Infusion_Manager': '<S1>:3858' */

			if (localDW.isInfusionManager == IN_IDLE) {
				/* Exit 'IDLE': '<S1>:3866' */
				localB.imOutCurrentSystemMode = 1;
				localB.imOutFlowRateCommanded = 0;
				resetAllInfusionDetails(localB, localDW);
				localDW.isInfusionManager = IN_NO_ACTIVE_CHILD;
			} else if (localDW.isInfusionManager == IN_THERAPY) {

				/* Exit Internal 'THERAPY': '<S1>:3867' */
				if (localDW.isTherapy == IN_ACTIVE) {
					exitInternalActive(localDW);
					localDW.isTherapy = IN_NO_ACTIVE_CHILD;
				} else if (localDW.isTherapy == IN_PAUSED) {
					exit_internal_PAUSED(localB, localDW);
					localDW.isTherapy = IN_NO_ACTIVE_CHILD;

				} else localDW.isTherapy = IN_NO_ACTIVE_CHILD;

				/* Exit 'THERAPY': '<S1>:3867' */
				therapyExitOperations(localB);
				localDW.isInfusionManager = IN_NO_ACTIVE_CHILD;
			} else localDW.isInfusionManager = IN_NO_ACTIVE_CHILD;

			localDW.isC2InfusionMgrFunctional = IN_NOT_ON;

			/* Entry 'NOT_ON': '<S1>:3740' */
			localB.imOutCurrentSystemMode = 0;
			localB.imOutFlowRateCommanded = 0;
		} else if (localDW.isInfusionManager == IN_IDLE) {
			// DB_prinTF("13: ");
			// DB_prinTF("2: %d %d %d ",localB.infusionInitiate, localB.configured
			// ,localB.reservoirEmpty);
			/* During 'IDLE': '<S1>:3866' */
			var infusionCancelL = localB.infusionCancel;
			var infusionInhibitL = localB.infusionInhibit;
			var infusionInitiateL = localB.infusionInitiate;
			var configuredL = localB.configured;
			var reservoirEmptyL = localB.reservoirEmpty;
			if (infusionCancelL || infusionInhibitL) {
				// DB_prinTF("30: ");
				/* Transition: '<S1>:3993' */
				/* Exit 'IDLE': '<S1>:3866' */
				localB.imOutCurrentSystemMode = 1;
				localB.imOutFlowRateCommanded = 0;
				resetAllInfusionDetails(localB, localDW);
				localDW.isInfusionManager = IN_IDLE;

				/* Entry 'IDLE': '<S1>:3866' */
				localB.imOutCurrentSystemMode = 1;
				localB.imOutFlowRateCommanded = 0;
				resetAllInfusionDetails(localB, localDW);
			} else if (infusionInitiateL && (configuredL > 0) && (!reservoirEmptyL)) {
				// DB_prinTF("31: ");
				/* Transition: '<S1>:3864' */
				resetAllInfusionDetails(localB, localDW);

				/* Transition: '<S1>:3863' */
				localB.imOutNewInfusion = true;

				/* Exit 'IDLE': '<S1>:3866' */
				localB.imOutCurrentSystemMode = 1;
				localB.imOutFlowRateCommanded = 0;
				resetAllInfusionDetails(localB, localDW);
				localDW.isInfusionManager = IN_THERAPY;

				/* Entry Internal 'THERAPY': '<S1>:3867' */
				infusionInhibitL = localB.infusionInhibit;
				var highestLevelAlarm_l = localB.highestLevelAlarm;

				if (infusionInhibitL || (highestLevelAlarm_l >= 3)) {
					/* Transition: '<S1>:3994' */
					localDW.isTherapy = IN_PAUSED;
					enterInternalPaused(localB, localDW);
				} else {
					/* Transition: '<S1>:3875' */
					localDW.isTherapy = IN_ACTIVE;
					enterInternalActive(localB, localDW);
				}
			} else {
				// DB_prinTF("32: ");
				localB.imOutCurrentSystemMode = 1;
				localB.imOutFlowRateCommanded = 0;
			}
		} else {
			// DB_prinTF("14: ");
			therapy(localB, localDW);
		}
	} else {
		// DB_prinTF("20: ");
		/* During 'NOT_ON': '<S1>:3740' */
		if (rtuTlmModeIn.systemOn) {
			/* Transition: '<S1>:3741' */
			/* Exit 'NOT_ON': '<S1>:3740' */
			localDW.isC2InfusionMgrFunctional = IN_INFUSION_MANAGER;

			/* Entry Internal 'Infusion_Manager': '<S1>:3858' */
			/* Transition: '<S1>:3860' */
			localDW.isInfusionManager = IN_IDLE;

			/* Entry 'IDLE': '<S1>:3866' */
			localB.imOutCurrentSystemMode = 1;
			localB.imOutFlowRateCommanded = 0;
			resetAllInfusionDetails(localB, localDW);
		} else {
			localB.imOutCurrentSystemMode = 0;
			localB.imOutFlowRateCommanded = 0;
		}
	}

	/* End of Chart: '<Root>/Infusion Manager Sub-System' */

	/* BusCreator: '<Root>/BusConversion_InsertedFor_IM_OUT_at_inport_0' */
	rtyImOut.commandedFlowRate = localB.imOutFlowRateCommanded;
	rtyImOut.currentSystemMode = localB.imOutCurrentSystemMode;
	rtyImOut.newInfusion = localB.imOutNewInfusion;
	rtyImOut.logMessageId = localB.imOutLogMessageID;
	rtyImOut.actualInfusionDuration = localB.imOutActualInfusionDuration;
}

function prop1() {
	//var infusionMgr = new InfusionMgrFunctional();
	var localB = new B();
	var localDW = new DW();

	init(localB, localDW);

	var rtuTlmModeIn = new TopLevelModeOutputs();
	var rtuOpCmdIn = new OperatorCommands();
	var rtuPatientIn = new PatientInputs();
	var rtuConfigIn = new ConfigOutputs();
	var rtuAlarmIn = new AlarmOutputs();
	var rtuSysStatIn = new SystemStatusOutputs();
	var rtyImOut = new InfusionManagerOutputs();

	if ((0 <= rtuConfigIn.infusionTotalDuration)
		&& (0 <= rtuConfigIn.vtbiTotal)
		&& (0 <= rtuConfigIn.flowRateBasal)
		&& (0 <= rtuConfigIn.flowRateIntermittentBolus)
		&& (0 <= rtuConfigIn.durationIntermittentBolus)
		&& (0 <= rtuConfigIn.intervalIntermittentBolus)
		&& (0 <= rtuConfigIn.flowRatePatientBolus)
		&& (0 <= rtuConfigIn.durationPatientBolus)
		&& (0 <= rtuConfigIn.lockoutPeriodPatientBolus)
		&& (0 <= rtuConfigIn.maxNumberOfPatientBolus)
		&& (0 <= rtuConfigIn.flowRateKVO)
		&& (0 <= rtuConfigIn.enteredReservoirVolume)
		&& (0 <= rtuConfigIn.configured)
		&& (0 <= rtuAlarmIn.highestLevelAlarm)
		&& (0 <= rtuSysStatIn.volumeInfused)
		&& (rtuConfigIn.infusionTotalDuration <= 255)
		&& (rtuConfigIn.vtbiTotal <= 255)
		&& (rtuConfigIn.flowRateBasal <= 255)
		&& (rtuConfigIn.flowRateIntermittentBolus <= 255)
		&& (rtuConfigIn.durationIntermittentBolus <= 255)
		&& (rtuConfigIn.intervalIntermittentBolus <= 255)
		&& (rtuConfigIn.flowRatePatientBolus <= 255)
		&& (rtuConfigIn.durationPatientBolus <= 255)
		&& (rtuConfigIn.lockoutPeriodPatientBolus <= 255)
		&& (rtuConfigIn.maxNumberOfPatientBolus <= 255)
		&& (rtuConfigIn.flowRateKVO <= 255)
		&& (rtuConfigIn.enteredReservoirVolume <= 255)
		&& (rtuConfigIn.configured <= 255)
		&& (rtuAlarmIn.highestLevelAlarm <= 255)
		&& (rtuSysStatIn.volumeInfused <= 255)) {

		var run = 5; // input
		while (run > 0) {
			infusionMgrFunctional(rtuTlmModeIn, rtuOpCmdIn, rtuPatientIn, rtuConfigIn, rtuAlarmIn, rtuSysStatIn, rtyImOut, localB, localDW);
			run = run - 1;
		}

		var checkCondition;
		var checkOutput;

		// Prop1: mode_range
		checkCondition =
			((rtyImOut.currentSystemMode == 0)
				|| (rtyImOut.currentSystemMode == 1)
				|| (rtyImOut.currentSystemMode == 2)
				|| (rtyImOut.currentSystemMode == 3)
				|| (rtyImOut.currentSystemMode == 4)
				|| (rtyImOut.currentSystemMode == 6)
				|| (rtyImOut.currentSystemMode == 7)
				|| (rtyImOut.currentSystemMode == 8));

		if (checkCondition) {
			print("successful");
		} else {
			throw "error";
		}
	}
}

prop1();
