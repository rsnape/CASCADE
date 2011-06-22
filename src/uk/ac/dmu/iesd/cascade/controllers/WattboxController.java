/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.*;

import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.context.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.context.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * @author jrsnape
 *
 * This class implements an abstracted version of the "Wattbox"
 * Smart controller developed by Dr. Peter Boait
 * 
 * The Wattbox learned variable is occupancy, with the controlled
 * variable being temperature set point.  Set point is determined
 * by an optimisation algorithm based upon an input signal and 
 * projected load for the following day
 * 
 */
public class WattboxController implements ISmartController{

	static final float[] INITIALIZATION_TEMPS = {7,7,7,7,7,7,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16,17,17,18,18,17,17,16,16,15,15,14,14,12,12,10,10,8,8,7,7};
	HouseholdProsumer owner;
	WattboxUserProfile userProfile;
	WattboxLifestyle userLifestyle;
	int ticksPerDay;

	float[] dayPredictedCostSignal;
	// The  temperature profile, optimised to minimise consumption "cost"
	float[] setPointProfile;

	//Prior day's temperature profile.  Works on the principle that
	// in terms of temperature, today is likely to be similar to yesterday
	float[] priorDayExternalTempProfile;

	float[] coldApplianceProfile;
	float[] wetApplianceProfile;
	float[] heatPumpDemandProfile;
	
	float[] EVChargingProfile;

	boolean[] heatPumpOnOffProfile;
	
	/**
	 * Boolean configuration options for what elements of the owner's
	 * electrical demand are controlled by the Wattbox.
	 * 
	 * Options are cold appliances, wet appliances, water heating, space heating
	 * and electric vehicles.  All default to true and should be explicitly
	 * set false if required by the Wattbox owner.
	 */
	private boolean coldAppliancesControlled = true;
	private boolean wetAppliancesControlled = true;
	private boolean waterHeatingControlled = true;
	private boolean spaceHeatingControlled = true;
	private boolean eVehicleControlled = true;


	/**
	 * method to re-evaluate the internal probabilities held by this Wattbox
	 */
	private void evaluateProbabilities()
	{

	}

	/**
	 * updates the occupancy profiles, probabilities of occupancy and lifestyle
	 * and optimises profiles based on this
	 */
	public void update(int timeStep)
	{
		this.dayPredictedCostSignal = owner.getPredictedCostSignal();
		this.coldApplianceProfile = owner.coldApplianceBaseProfile;
		this.heatPumpDemandProfile = calculatePredictedHeatPumpDemand(timeStep);
		this.setPointProfile = owner.getSetPointProfile();

		//TODO: Think about whether / how water and space heating are coupled.

		if (coldAppliancesControlled)
		{
			optimiseColdProfile();
		}
		if (wetAppliancesControlled)
		{
			optimiseWetProfile();
		}
		if (waterHeatingControlled && owner.isHasElectricalWaterHeat())
		{
			optimiseWaterHeatProfile();
		}
		if (spaceHeatingControlled && owner.isHasElectricalSpaceHeat())
		{
			optimiseSpaceHeatProfile();
		}
		if (eVehicleControlled && owner.isHasElectricVehicle())
		{
			optimiseEVProfile();
		}

		//TODO: I don't like this - getting the predicted temperatures.  Can we do something better - use yesterday's?
		this.priorDayExternalTempProfile = owner.getContext().getAirTemperature(timeStep, ticksPerDay);
	}

	/**
	 * @return
	 */
	private float[] calculatePredictedHeatPumpDemand(int timeStep) {

		float[] power = new float[ticksPerDay];
		float[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));

		for (int i = 0; i < ticksPerDay; i++)
		{
			if (deltaT[i] > Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF)
			{
				power[i] = deltaT[i] * (owner.buildingHeatLossRate / Consts.DOMESTIC_HEAT_PUMP_COP);
			}
		}

		return power;

	}

	/**
	 * 
	 */
	private void optimiseEVProfile() {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 */
	private void optimiseSpaceHeatProfile() {
		
		float[] initialPredictedCosts = ArrayUtils.mtimes(this.dayPredictedCostSignal, this.heatPumpDemandProfile);
		float bestCost = ArrayUtils.sum(initialPredictedCosts);
		//Go through all timeslots switching the heat pump off for certain periods
		//subject to constraints and find the least cost option.

		boolean[] tempPumpOnOffProfile = new boolean[ticksPerDay];
		Arrays.fill(tempPumpOnOffProfile,true);
		float[] tempHeatPumpProfile = new float[ticksPerDay];
		float[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));
		
		for (int i = 0; i < ticksPerDay; i++)
		{
			for ( int j = Consts.HEAT_PUMP_MIN_SWITCHOFF - 1; j <= Consts.HEAT_PUMP_MAX_SWITCHOFF; j++)
			{
				//Don't try impossible combinations - can't recover heat beyond the day boundary
				if (i+j < ticksPerDay - 1)
				{
					float thisCost = 0;
					tempPumpOnOffProfile[i + j] = false;
					
					if (thisCost < bestCost)
					{
						
						bestCost = thisCost;
						this.heatPumpOnOffProfile = tempPumpOnOffProfile;
						this.heatPumpDemandProfile = tempHeatPumpProfile;
					}
				}
			}
			
			//reset to always on and try the next timeslot;
			Arrays.fill(tempPumpOnOffProfile,true);
		}
		
		/*
		owner.setPoint = owner.getSetPointProfile()[timeStep % ticksPerDay];
		float deltaT =  owner.setPoint - extTemp;
		float tau = owner.buildingThermalMass / owner.buildingHeatLossRate;
		if (owner.spaceHeatPumpOn[timeStep])
		{
			owner.currentInternalTemp = owner.setPoint;
		}
		else
		{
			owner.currentInternalTemp = extTemp + deltaT * (float) Math.exp(-(timeStep / tau));
		}*/
	}

	/**
	 * 
	 */
	private void optimiseWaterHeatProfile() {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 */
	private void optimiseWetProfile() {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 */
	private void optimiseColdProfile() {
		float[] currentCost = ArrayUtils.mtimes(coldApplianceProfile, dayPredictedCostSignal);
		int maxIndex = ArrayUtils.indexOfMax(currentCost);
		// First pass - simply move the cold load in this period to the next timeslot
		//TODO: very crude and will give nasty positive feedback in all likelihood
		coldApplianceProfile[maxIndex + 1] = coldApplianceProfile[maxIndex + 1] + coldApplianceProfile[maxIndex];
		coldApplianceProfile[maxIndex] = 0;
	}

	public WeakHashMap getCurrentProfiles()
	{
		WeakHashMap returnMap = new WeakHashMap();
		returnMap.put("HeatPump", heatPumpOnOffProfile);
		returnMap.put("ColdApps", coldApplianceProfile);
		return returnMap;
	}


	/**
	 * @param dayPredictedCostSignal the dayPredictedCostSignal to set
	 */
	public void setDayPredictedCostSignal(float[] dayPredictedCostSignal) {
		this.dayPredictedCostSignal = dayPredictedCostSignal;
	}


	void optimiseSetPointProfile()
	{ 		
		float[] localSetPointArray;
		localSetPointArray = new float [owner.getContext().getTickPerDay()]; //TODO: tickPerDay should be resolved
		Arrays.fill(localSetPointArray, 0, localSetPointArray.length - 1, owner.getSetPoint());
		float profileCost = 0;
		float oldProfileCost = Float.POSITIVE_INFINITY;

		while (profileCost < oldProfileCost)
		{
			oldProfileCost = profileCost;
			profileCost = ArrayUtils.sum(ArrayUtils.mtimes(localSetPointArray, owner.getPredictedCostSignal()));
		}

		setPointProfile = localSetPointArray;
	}

	/**
	 * Constructor - creates an uninitialized Wattbox controller assigned to 
	 * the prosumer passed in as an argument.
	 * 
	 * @param owner - the prosumer agent that owns this wattbox
	 */
	public WattboxController(HouseholdProsumer owner) {
		this.owner = owner;
		this.priorDayExternalTempProfile = INITIALIZATION_TEMPS;
		ticksPerDay = owner.getContext().getTickPerDay();
	}

	/**
	 * Constructor - creates a Wattbox controller assigned to 
	 * the prosumer passed in as an argument and initialized with
	 * a user profile and lifestyle also passed in.
	 * 
	 * @param owner - the prosumer agent that owns this wattbox
	 * @param userProfile
	 * @param userLifestyle
	 */
	public WattboxController(HouseholdProsumer owner,
			WattboxUserProfile userProfile, WattboxLifestyle userLifestyle) {
		super();
		this.owner = owner;
		this.userProfile = userProfile;
		this.userLifestyle = userLifestyle;
	}
}
