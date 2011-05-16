/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import uk.ac.dmu.iesd.cascade.context.ProsumerAgent;

/**
 * @author jsnape
 *
 */
public class BasicSmartController {

	ProsumerAgent owner;
	
	float[] dayPredictedCostSignal;

	/**
	 * @param dayPredictedCostSignal the dayPredictedCostSignal to set
	 */
	public void setDayPredictedCostSignal(float[] dayPredictedCostSignal) {
		this.dayPredictedCostSignal = dayPredictedCostSignal;
	}
	
	public BasicSmartController(ProsumerAgent owner)
	{
		this.owner = owner;
	}
	
}
