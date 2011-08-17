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
 * * This class implements an abstracted version of the "Wattbox"
 * Smart controller developed by Dr. Peter Boait
 * 
 * The Wattbox learned variable is occupancy, with the controlled
 * variable being temperature set point.  Set point is determined
 * by an optimisation algorithm based upon an input signal and 
 * projected load for the following day
 * 
 * @author jrsnape
 */
public class WattboxController implements ISmartController{

	static final float[] INITIALIZATION_TEMPS = {7,7,7,7,7,7,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16,17,17,18,18,17,17,16,16,15,15,14,14,12,12,10,10,8,8,7,7};
	HouseholdProsumer owner;
	WattboxUserProfile userProfile;
	WattboxLifestyle userLifestyle;
	int ticksPerDay;

	float[] dayPredictedCostSignal;
	float predictedCostToRealCostA = 1;
	float realCostOffsetb = 10;
	float[] setPointProfile;

	//Prior day's temperature profile.  Works on the principle that
	// in terms of temperature, today is likely to be similar to yesterday
	float[] priorDayExternalTempProfile;
	float[] coldApplianceProfile;
	float[] wetApplianceProfile;
	float[] heatPumpDemandProfile;
	float[] waterHeatProfile;

	float[] EVChargingProfile;

	float[] heatPumpOnOffProfile;

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
		this.dayPredictedCostSignal = ArrayUtils.offset(ArrayUtils.multiply(this.dayPredictedCostSignal, predictedCostToRealCostA),realCostOffsetb);
		this.setPointProfile = owner.getSetPointProfile();
		this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,(timeStep % owner.coldApplianceProfile.length) , (timeStep % owner.coldApplianceProfile.length) + ticksPerDay);
		this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,(timeStep % owner.wetApplianceProfile.length), (timeStep % owner.wetApplianceProfile.length) + ticksPerDay);
		this.waterHeatProfile = Arrays.copyOfRange(owner.baselineWaterHeatProfile,(timeStep % owner.baselineWaterHeatProfile.length), (timeStep % owner.baselineWaterHeatProfile.length) + ticksPerDay);
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
			//System.out.println(Arrays.toString(this.heatPumpOnOffProfile));
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
		returnMap.put("WaterHeat", waterHeatProfile);
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
	private float[] calculatePredictedHeatPumpDemand(float[] heatPumpProfile) 
	{
		float[] energyProfile = new float[ticksPerDay];
		float[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));
		float tempLoss = 0;
		// t is time since last heating in seconds
		float t = 0;
		int availableHeatRecoveryTicks = ticksPerDay;
		float maxRecoveryPerTick = 0.5f * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE; // i.e. can't recover more than 50% of heat loss at 90% COP.  TODO: Need to code this better later
		float heatLoss = 0;

		for (int i = 0; i < ticksPerDay; i++)
		{
			--availableHeatRecoveryTicks;
			float internalTemp = setPointProfile[i] - tempLoss;


			if (deltaT[i] > Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF && (heatPumpProfile[i] > 0))
			{
				float setPointMaintenancePower = deltaT[i] * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) * (Consts.SECONDS_PER_DAY / ticksPerDay);
				//TODO: This assumes that all lost temp is recovered in one time period.  This surely can't be the case
				if (heatLoss > 0)
				{
					//need to place recovery power somewhere
					//calculate required recovery timeTicks
					int n = (int) Math.ceil(heatLoss / (maxRecoveryPerTick * setPointMaintenancePower));

					if (availableHeatRecoveryTicks < n)
					{
						//Recovery impossible - terminate processing
						return null;
					}
					else
					{
						availableHeatRecoveryTicks = availableHeatRecoveryTicks - n;
						int[] recoveryIndices = ArrayUtils.findNSmallestIndices(Arrays.copyOfRange(this.dayPredictedCostSignal,i,ticksPerDay),n);

						for (int k : recoveryIndices)
						{
							//System.out.println("in here for index " + k);
							heatPumpProfile[k + i] = 1 + maxRecoveryPerTick;
						}

						//We have now allocated the recovery power required, so zero it.
						heatLoss = 0;
						tempLoss = 0;
					}
				}
				t = 0;
				energyProfile[i] = setPointMaintenancePower * heatPumpProfile[i];
			}
			else
			{	
				energyProfile[i] = 0;

				float newTemp = priorDayExternalTempProfile[i] + ((deltaT[i] - tempLoss) * (float) Math.exp(-((float)((float)Consts.SECONDS_PER_DAY / ticksPerDay) / owner.tau)));
				tempLoss = setPointProfile[i] - newTemp;
				heatLoss = tempLoss * (owner.buildingThermalMass / Consts.DOMESTIC_HEAT_PUMP_COP_HEAT_RECOVERY);
				//System.out.print("Temp loss is " + tempLoss);
				if(tempLoss > Consts.DAYTIME_TEMP_LOSS_THRESHOLD)
				{
					//if the temperature drop is too great, this profile is unfeasible and we return null
					return null;
				}
			}
		}

		return energyProfile;
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
	 * Subject to constraints that 
	 * <li> temperature drop in switch off periods must not exceed a certain amount.
	 * <li> heat pump may be switched off for only one period per day
	 * <li> the "switch off period" may be between half an hour and four hours
	 * 
	 * Currently uses a "brute force" algorithm to evaluate all profiles
	 * It uses the profile which minimises the predicted cost of operation.
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

		float[] tempPumpOnOffProfile = new float[ticksPerDay];
		Arrays.fill(tempPumpOnOffProfile,1);
		float[] tempHeatPumpProfile = new float[ticksPerDay];
		float[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));

		for (int i = 0; i < ticksPerDay; i++)
		{
			for ( int j = Consts.HEAT_PUMP_MIN_SWITCHOFF - 1; j < Consts.HEAT_PUMP_MAX_SWITCHOFF; j++)
			{
				//Don't try impossible combinations - can't recover heat beyond the day boundary
				if (i+j < ticksPerDay - 3)
				{
					float thisCost = 0;

					tempPumpOnOffProfile[i+j] = 0;
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
			Arrays.fill(tempPumpOnOffProfile,1);
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
		float[] returnArray = Arrays.copyOf(this.waterHeatProfile, this.waterHeatProfile.length);
		for (int i = 0; i < returnArray.length; i++)
		{
			float currDemand = returnArray[i];
			float currCost = currDemand * this.dayPredictedCostSignal[i];
			int moveTo = -1;
			float newHeatDemand = 0;
			
			if (currDemand > 0)
			{
				float extraHeatRequired = 0;
				float adaptedDemand = 0;
				for (int j = i-1; j >= 0; j--)
				{
					extraHeatRequired += (Consts.WATER_TEMP_LOSS_PER_SECOND * ((float)Consts.SECONDS_PER_DAY / ticksPerDay)) * owner.dailyHotWaterUsage * (Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR);
					adaptedDemand = currDemand + (extraHeatRequired / Consts.DOMESTIC_HEAT_PUMP_WATER_COP);
					if (adaptedDemand * this.dayPredictedCostSignal[j] < currCost)
					{
						moveTo = j;
						newHeatDemand = adaptedDemand;
						currCost = adaptedDemand * this.dayPredictedCostSignal[j];
					}
				}
				
				if (moveTo > -1)
				{
					returnArray[i] = 0;
					returnArray[moveTo] = newHeatDemand;
				}
			}
		}
		
		waterHeatProfile = returnArray;
	}

	/**
	 * Optimise the Wet appliance usage profile for the household
	 * which owns this Wattbox.
	 * 
	 */
	private void optimiseWetProfile() 
	{
		float[] currentCost = ArrayUtils.mtimes(wetApplianceProfile, dayPredictedCostSignal);
		int maxIndex = ArrayUtils.indexOfMax(currentCost);
		int minIndex = ArrayUtils.indexOfMin(currentCost);
		// First pass - simply swap the load in the max cost slot with that in the min cost slot
		//TODO: very crude and will give nasty positive feedback in all likelihood
		if (maxIndex < (wetApplianceProfile.length -1 ))
		{
			float temp = wetApplianceProfile[minIndex];
			wetApplianceProfile[minIndex] = wetApplianceProfile[maxIndex];
			wetApplianceProfile[maxIndex] = temp;
		}
	}

	/**
	 * Optimise the Cold appliance usage profile for the household
	 * which owns this Wattbox.
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
		this.waterHeatProfile = Arrays.copyOfRange(owner.baselineWaterHeatProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.baselineWaterHeatProfile.length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.baselineWaterHeatProfile.length) + ticksPerDay);

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
