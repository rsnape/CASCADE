/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.WeakHashMap;

import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;

/**
 * @author jsnape
 *
 */
public class FlattenWithinDaySmartController implements ISmartController {

	ProsumerAgent owner;

	double[] dayPredictedCostSignal;

	/**
	 * Method to tell the smart controller to gather the data that it needs and
	 * update its internal state.
	 * 
	 * Note - this may include learning algorithms if appropriate
	 */
	public void update(int timeStep)
	{	
		/*// simplest smart controller implementation - perfect division of load through the day
		double moveableLoad = owner.inelasticTotalDayDemand * owner.percentageMoveableDemand;
		double [] daysCostSignal = new double [owner.getContext().getTickPerDay()];
		double [] daysOptimisedDemand = new double [owner.getContext().getTickPerDay()];
		System.arraycopy(owner.getPredictedCostSignal(), owner.time - owner.predictionValidTime, daysCostSignal, 0, owner.getContext().getTickPerDay());

		System.arraycopy(owner.smartOptimisedProfile, owner.time % owner.smartOptimisedProfile.length, daysOptimisedDemand, 0, owner.getContext().getTickPerDay());

		double [] tempArray = ArrayUtils.mtimes(daysCostSignal, daysOptimisedDemand);

		double currentCost = ArrayUtils.sum(tempArray);
		// Algorithm to minimise this whilst satisfying constraints of
		// maximum movable demand and total demand being inelastic.

		double movedLoad = 0;
		double movedThisTime = -1;
		double swapAmount = -1;
		while (movedLoad < moveableLoad && movedThisTime != 0)
		{
			Arrays.fill(daysOptimisedDemand, owner.inelasticTotalDayDemand / owner.getContext().getTickPerDay());
			movedThisTime = 0;
			tempArray = ArrayUtils.mtimes(daysOptimisedDemand, daysCostSignal);			                   	                                             
		}
		System.arraycopy(daysOptimisedDemand, 0, owner.smartOptimisedProfile, owner.time % owner.smartOptimisedProfile.length, owner.getContext().getTickPerDay());
		if (Consts.DEBUG)
		{
			if (ArrayUtils.sum(daysOptimisedDemand) != owner.inelasticTotalDayDemand)
			{
				//TODO: This always gets triggerd - I wonder if the "day" i'm taking
				//here and in the inelasticdemand method are "off-by-one"
				if (Consts.DEBUG) System.out.println("optimised signal has varied the demand !!! In error !" + (ArrayUtils.sum(daysOptimisedDemand) - owner.inelasticTotalDayDemand));
			}

			if (Consts.DEBUG) System.out.println("Saved " + (currentCost - ArrayUtils.sum(tempArray)) + " cost");
		}*/
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
	public void setDayPredictedCostSignal(double[] dayPredictedCostSignal) {
		this.dayPredictedCostSignal = dayPredictedCostSignal;
	}

	public FlattenWithinDaySmartController(HouseholdProsumer owner)
	{
		this.owner = owner;
	}

}
