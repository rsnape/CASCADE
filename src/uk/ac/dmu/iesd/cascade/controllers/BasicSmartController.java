/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.WeakHashMap;

import uk.ac.dmu.iesd.cascade.context.ProsumerAgent;

/**
 * @author jsnape
 *
 * Minimal implementation of the ISmartContoller interface
 */
public class BasicSmartController implements ISmartController {

	ProsumerAgent owner;

	float[] dayPredictedCostSignal;

	/**
	 * Method to tell the smart controller to gather the data that it needs and
	 * update its internal state.
	 * 
	 * Note - this may include learning algorithms if appropriate
	 */
	public void update(int timeStep)
	{

	}

	/**
	 * Method to extract the current profiles for controlled appliance / device usage
	 * 
	 * @return a WeakHashMap containing whose keys describe the type of load profile and
	 * 			whose values are floating point arrays of the current profile for the day
	 */
	public WeakHashMap getCurrentProfiles()
	{
		//TODO: This does nothing !!!
		return new WeakHashMap();
	}

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
