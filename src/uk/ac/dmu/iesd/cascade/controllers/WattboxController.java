/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.Arrays;



import uk.ac.dmu.iesd.cascade.context.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.context.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * @author jsnape
 *
 * This class implements an abstracted version of the "Wattbox"
 * Smart controller developed by Dr. Peter Boait
 * 
 * The Wattbox learnt variable is occupancy, with the controlled
 * variable being temperature set point.  Set point is determined
 * by an optimisation algorithm based upon an input signal and 
 * projected load for the following day
 * 
 */
public class WattboxController{
	
	HouseholdProsumer owner;
	WattboxUserProfile userProfile;
	WattboxLifestyle userLifestyle;
	
	float[] dayPredictedCostSignal;
	// The output temperature profile, optimised to minimise 
	// consumption "cost"
	float[] setPointProfile;
	
	//Prior day's temperature profile.  Works on the principle that
	// in terms of temperature, today is likely to be similar to yesterday
	float[] priorDayExternalTempProfile;
	
	// We will have two sets of probability vectors
	// one for 
	
	
	
	/*
	 * 
	 */
	
	public void evaluateProbabilities()
	{
		
	}

	/**
	 * @param dayPredictedCostSignal the dayPredictedCostSignal to set
	 */
	public void setDayPredictedCostSignal(float[] dayPredictedCostSignal) {
		this.dayPredictedCostSignal = dayPredictedCostSignal;
	}
	
	
	/**
	 * @param owner
	 */
	WattboxController(HouseholdProsumer owner) {
		this.owner = owner;
		// TODO Auto-generated constructor stub
	}


	
	void optimiseSetPointProfile(float[] predictedCostSignal)
	{ 		
		float[] localSetPointArray;
		localSetPointArray = new float [owner.ticksPerDay];
		Arrays.fill(localSetPointArray, 0, localSetPointArray.length - 1, owner.getSetPoint());
		float profileCost = 0;
		float oldProfileCost = Float.POSITIVE_INFINITY;
		
		while (profileCost < oldProfileCost)
		{
			oldProfileCost = profileCost;
			profileCost = ArrayUtils.sum(ArrayUtils.mtimes(localSetPointArray, predictedCostSignal));
		}
		
		setPointProfile = localSetPointArray;
	}

	/**
	 * @param owner
	 * @param userProfile
	 * @param userLifestyle
	 * @param dayPredictedCostSignal
	 * @param setPointProfile
	 * @param priorDayExternalTempProfile
	 * @param probElecLoadGivenOccAndActive
	 * @param probOccAndActive
	 * @param probElecLoadGivenNotOccAndActive
	 */
	public WattboxController(HouseholdProsumer owner,
			WattboxUserProfile userProfile, WattboxLifestyle userLifestyle) {
		super();
		this.owner = owner;
		this.userProfile = userProfile;
		this.userLifestyle = userLifestyle;
		this.dayPredictedCostSignal = dayPredictedCostSignal;
		this.setPointProfile = setPointProfile;
		this.priorDayExternalTempProfile = priorDayExternalTempProfile;
		}
	

	

}
