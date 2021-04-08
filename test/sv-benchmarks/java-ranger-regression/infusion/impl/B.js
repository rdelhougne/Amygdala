// https://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.12.5
class B {
	constructor() {
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
}
