const MAX_INT = 2147483647;
const MIN_INT = -2147483648;

class Divs32 {
	divS32(numerator, denominator) {
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
				quotient = -tempAbsQuotient
			} else {
				quotient = tempAbsQuotient
			}
		}
		return quotient;
	}
}
