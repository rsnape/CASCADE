/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.*;

import repast.simphony.essentials.RepastEssentials;

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
	 * 
	 * TODO: currently unimplemented
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
		float[] ownersCostSignal = owner.getPredictedCostSignal();
		this.dayPredictedCostSignal = Arrays.copyOfRange(ownersCostSignal, timeStep % ownersCostSignal.length, timeStep % ownersCostSignal.length + ticksPerDay);
		this.setPointProfile = owner.getSetPointProfile();
		System.out.println("array length , from , to");
		System.out.println(owner.coldApplianceProfile.length +","+ (timeStep % owner.coldApplianceProfile.length) +","+ ((timeStep % owner.coldApplianceProfile.length) + ticksPerDay));
		System.out.println(owner.wetApplianceProfile.length +","+ (timeStep % owner.wetApplianceProfile.length) +","+ ((timeStep % owner.wetApplianceProfile.length) + ticksPerDay));
		this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,(timeStep % owner.coldApplianceProfile.length) , (timeStep % owner.coldApplianceProfile.length) + ticksPerDay);
		this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,(timeStep % owner.wetApplianceProfile.length), (timeStep % owner.wetApplianceProfile.length) + ticksPerDay);
		this.heatPumpDemandProfile = calculatePredictedHeatPumpDemand(heatPumpOnOffProfile);


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
	 * method to return all the optimised profiles currently held by this Wattbox
	 */
	public WeakHashMap getCurrentProfiles()
	{
		WeakHashMap returnMap = new WeakHashMap();
		returnMap.put("HeatPump", heatPumpOnOffProfile);
		returnMap.put("ColdApps", coldApplianceProfile);
		returnMap.put("WetApps", wetApplianceProfile);
		return returnMap;
	}

	/**
	 * turns a heat pump switching profile, in combination with the outside temperature
	 * profile and building physics parameters into a predicted demand curve
	 * for the heat pump.
	 * 
	 * Returns null if the pump switching profile passed in violates constraints on 
	 * temperature deviation from the set point.
	 * 
	 * @return the predicted heat pump demand for a certain heat pump switching profile
	 * given an estimate for the temperature profile for the day when it is used
	 */
	private float[] calculatePredictedHeatPumpDemand(boolean[] heatPumpProfile) 
	{
		float[] power = new float[ticksPerDay];
		float[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));
		float tempLoss = 0;
		// t is time since last heating in seconds
		int t = 0;

		for (int i = 0; i < ticksPerDay; i++)
		{
			float internalTemp = setPointProfile[i] - tempLoss;
			float heatLossRecoveryPower = 0;

			if (deltaT[i] > Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF && heatPumpProfile[i])
			{
				float setPointMaintenancePower = deltaT[i] * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) / Consts.DOMESTIC_HEAT_PUMP_COP);
				//TODO: This assumes that all lost temp is recovered in one time period.  This surely can't be the case
				t = 0;
				power[i] = setPointMaintenancePower + heatLossRecoveryPower;
			}
			else
			{	
				power[i] = 0;
				float tau = (owner.buildingThermalMass * Consts.KWH_TO_JOULE_CONVERSION_FACTOR) / owner.buildingHeatLossRate;
				t = t + Consts.SECONDS_PER_HALF_HOUR;
				float newTemp = priorDayExternalTempProfile[i] + (deltaT[i] * (float) Math.exp(-(t / tau)));
				tempLoss = setPointProfile[i] - newTemp;
				heatLossRecoveryPower = tempLoss * (owner.buildingThermalMass / Consts.DOMESTIC_HEAT_PUMP_COP_HEAT_RECOVERY);
				//System.out.print("Temp loss is " + tempLoss);
				if(tempLoss > Consts.DAYTIME_TEMP_LOSS_THRESHOLD)
				{
					//if the temperature drop is too great, this profile is unfeasible and we return null
					return null;
				}
			}
		}

		return power;
	}

	/**
	 * Optimise the Electric Vehicle charging profile for the household
	 * which owns this Wattbox.
	 * 
	 * TODO: Not yet implemented.
	 */
	private void optimiseEVProfile() {
	}

	/**
	 * finds an optimised heat pump switching profile for the day ahead 
	 * based on the projected temperature
	 * and therefore predicted heat pump demand.  This is combined with the 
	 * signal (sent by the aggregator) as a proxy for predicted cost of 
	 * electricity for the day ahead to produce the estimated cost of the 
	 * switching profile.
	 * 
	 * Subject to constraints that temperature drop in switch off periods must
	 * not exceed a certain amount.
	 * 
	 * Currently uses a "brute force" algorithm to evaluate all profiles with between
	 * 1 and 7 periods of continuous "switch off".  It uses the profile which minimises
	 * the predicted cost of operation.
	 */
	private void optimiseSpaceHeatProfile() 
	{
		float bestCost;

		if(this.heatPumpDemandProfile == null)
		{
			// If the demand profile is null, this means the switiching profile
			// is invalid or undefined, so set a very high cost to this
			// so that all alternatives will be better.
			bestCost = Float.MAX_VALUE;
		}
		else
		{
			float[] initialPredictedCosts = ArrayUtils.mtimes(this.dayPredictedCostSignal, this.heatPumpDemandProfile);
			bestCost = ArrayUtils.sum(initialPredictedCosts);
		}

		//Go through all timeslots switching the heat pump off for certain periods
		//subject to constraints and find the least cost option.
		//TODO: This is "brute force" optimisation - there will be a better way...

		boolean[] tempPumpOnOffProfile = new boolean[ticksPerDay];
		Arrays.fill(tempPumpOnOffProfile,true);
		float[] tempHeatPumpProfile = new float[ticksPerDay];
		float[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));

		for (int i = 0; i < ticksPerDay; i++)
		{
			for ( int j = Consts.HEAT_PUMP_MIN_SWITCHOFF - 1; j < Consts.HEAT_PUMP_MAX_SWITCHOFF; j++)
			{
				//Don't try impossible combinations - can't recover heat beyond the day boundary
				if (i+j < ticksPerDay - 1)
				{
					float thisCost = 0;

					tempPumpOnOffProfile[i+j] = false;


					tempHeatPumpProfile = calculatePredictedHeatPumpDemand(tempPumpOnOffProfile);
					if (Consts.DEBUG && owner.getAgentID() == 1)
					{
						//Some debugging output for one agent only
						System.out.println("On off profile for calculation " + Arrays.toString(tempPumpOnOffProfile));
						System.out.println("HP demand profile " + Arrays.toString(tempHeatPumpProfile));
					}

					if (tempHeatPumpProfile != null)
					{
						thisCost = ArrayUtils.sum(ArrayUtils.mtimes(this.dayPredictedCostSignal, tempHeatPumpProfile));
						if (thisCost < bestCost)
						{

							bestCost = thisCost;
							this.heatPumpOnOffProfile = Arrays.copyOf(tempPumpOnOffProfile, tempPumpOnOffProfile.length);
							this.heatPumpDemandProfile = Arrays.copyOf(tempHeatPumpProfile, tempHeatPumpProfile.length);
							if (Consts.DEBUG && owner.getAgentID() == 1)
							{
								//Some debugging output for one agent only
								System.out.println("Replacing the current profile with a better one");
								System.out.println("On off profile to use " + Arrays.toString(heatPumpOnOffProfile));
								System.out.println("HP demand profile " + Arrays.toString(heatPumpDemandProfile));
								System.out.println("This cost is " + thisCost + " against best " + bestCost);

							}
						}
					}
				}
			}

			//reset to always on and try the next timeslot;
			Arrays.fill(tempPumpOnOffProfile,true);
		}
	}

	/**
	 * Optimise the Water Heating profile for the household
	 * which owns this Wattbox.
	 * 
	 * TODO: Not yet implemented.
	 */
	private void optimiseWaterHeatProfile() 
	{
	}

	/**
	 * Optimise the Wet appliance usage profile for the household
	 * which owns this Wattbox.
	 * 
	 * TODO: Not yet implemented.
	 */
	private void optimiseWetProfile() 
	{
	}

	/**
	 * 
	 */
	private void optimiseColdProfile() 
	{
		float[] currentCost = ArrayUtils.mtimes(coldApplianceProfile, dayPredictedCostSignal);
		int maxIndex = ArrayUtils.indexOfMax(currentCost);
		int minIndex = ArrayUtils.indexOfMin(currentCost);
		// First pass - simply swap the load in the max cost slot with that in the min cost slot
		//TODO: very crude and will give nasty positive feedback in all likelihood
		if (maxIndex < (coldApplianceProfile.length -1 ))
		{
			float temp = coldApplianceProfile[minIndex];
			coldApplianceProfile[minIndex] = coldApplianceProfile[maxIndex];
			coldApplianceProfile[maxIndex] = temp;
		}
	}


	/**
	 * @param dayPredictedCostSignal the dayPredictedCostSignal to set
	 */
	public void setDayPredictedCostSignal(float[] dayPredictedCostSignal) 
	{
		this.dayPredictedCostSignal = dayPredictedCostSignal;
	}

	/**
	 * Alter set point profile to use heat pump optimally
	 * 
	 * NOTE: currently unused - preferring a simpler model instead c.f. optimiseHeatPumpDemand
	 */
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
	public WattboxController(HouseholdProsumer owner) 
	{
		this.owner = owner;
		this.priorDayExternalTempProfile = Arrays.copyOf(INITIALIZATION_TEMPS, INITIALIZATION_TEMPS.length);
		this.heatPumpOnOffProfile = Arrays.copyOf(owner.spaceHeatPumpOn,owner.spaceHeatPumpOn.length);
		ticksPerDay = owner.getContext().getTickPerDay();
		if(owner.coldApplianceProfile != null)
		{
			this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.coldApplianceProfile.length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.coldApplianceProfile.length) + ticksPerDay);
		}
		if (owner.wetApplianceProfile != null)
		{
			this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.wetApplianceProfile.length), ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.wetApplianceProfile.length) + ticksPerDay);
		}
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
			WattboxUserProfile userProfile, WattboxLifestyle userLifestyle) 
	{
		super();
		this.owner = owner;
		this.userProfile = userProfile;
		this.userLifestyle = userLifestyle;
	}
}
