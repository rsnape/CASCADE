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
	float[] optimisedSetPointProfile;
	float[] currentTempProfile;

	//Prior day's temperature profile.  Works on the principle that
	// in terms of temperature, today is likely to be similar to yesterday
	float[] priorDayExternalTempProfile;
	float[] coldApplianceProfile;
	float[] wetApplianceProfile;
	float[] heatPumpDemandProfile;
	float[] hotWaterVolumeDemandProfile;

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
	private float[] waterHeatDemandProfile;
	private float maxHeatPumpElecDemandPerTick;
	private float expectedNextDaySpaceHeatCost;


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
		this.optimisedSetPointProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
		this.currentTempProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
		this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,(timeStep % owner.coldApplianceProfile.length) , (timeStep % owner.coldApplianceProfile.length) + ticksPerDay);
		this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,(timeStep % owner.wetApplianceProfile.length), (timeStep % owner.wetApplianceProfile.length) + ticksPerDay);
		this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.baselineHotWaterVolumeProfile,(timeStep % owner.baselineHotWaterVolumeProfile.length), (timeStep % owner.baselineHotWaterVolumeProfile.length) + ticksPerDay);
		this.heatPumpDemandProfile = ArrayUtils.multiply(calculateSpaceHeatPumpDemand(this.setPointProfile), (1/Consts.DOMESTIC_HEAT_PUMP_SPACE_COP));


		//TODO: Think about whether / how water and space heating are coupled.

		if (coldAppliancesControlled)
		{
			optimiseColdProfile();
		}

		if (wetAppliancesControlled)
		{
			optimiseWetProfile();
		}


		// Note - optimise space heating first.  This is so that we can look for absolute
		// heat pump limit and add the cost of using immersion heater (COP 0.9) to top
		// up water heating if the heat pump is too great
		if (spaceHeatingControlled && owner.isHasElectricalSpaceHeat())
		{
			//optimiseSpaceHeatProfile();
			optimiseSetPointProfile();
			//System.out.println(Arrays.toString(this.heatPumpOnOffProfile));
		}	


		if (waterHeatingControlled && owner.isHasElectricalWaterHeat())
		{
			optimiseWaterHeatProfile();
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
		returnMap.put("HeatPump", optimisedSetPointProfile);
		returnMap.put("ColdApps", coldApplianceProfile);
		returnMap.put("WetApps", wetApplianceProfile);
		returnMap.put("WaterHeat", waterHeatDemandProfile);
		return returnMap;
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
	 * Optimise the Water Heating profile for the household
	 * which owns this Wattbox.
	 */
	private void optimiseWaterHeatProfile() 
	{		
		float[] returnArray = ArrayUtils.multiply(this.hotWaterVolumeDemandProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (owner.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP) );

		for (int i = 0; i < returnArray.length; i++)
		{
			float currDemand = returnArray[i];

			//If demand exceeds Heat Pump capacity, top up with immersion (i.e. reduce COP)
			if ((currDemand + this.heatPumpDemandProfile[i]) > this.maxHeatPumpElecDemandPerTick )
			{
				currDemand = currDemand + (((currDemand + this.heatPumpDemandProfile[i]) - this.maxHeatPumpElecDemandPerTick ) * Consts.DOMESTIC_HEAT_PUMP_WATER_COP / Consts.IMMERSION_HEATER_COP);
			}

			float currCost = currDemand * this.dayPredictedCostSignal[i];
			int moveTo = -1;
			float newHeatDemand = 0;

			if (currDemand > 0)
			{
				float extraHeatRequired = 0;
				float adaptedDemand = 0;
				for (int j = i-1; j >= 0; j--)
				{
					extraHeatRequired += (Consts.WATER_TEMP_LOSS_PER_SECOND * ((float)Consts.SECONDS_PER_DAY / ticksPerDay)) * this.hotWaterVolumeDemandProfile[i] * (Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR);
					adaptedDemand = currDemand + (extraHeatRequired / Consts.DOMESTIC_HEAT_PUMP_WATER_COP);

					//If demand exceeds Heat Pump capacity, top up with immersion (i.e. reduce COP)
					float totalHeatDemand = adaptedDemand + this.heatPumpDemandProfile[i];
					if (totalHeatDemand > this.maxHeatPumpElecDemandPerTick )
					{
						adaptedDemand = adaptedDemand + ((totalHeatDemand - this.maxHeatPumpElecDemandPerTick ) * Consts.DOMESTIC_HEAT_PUMP_WATER_COP / Consts.IMMERSION_HEATER_COP);
					}
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

		this.waterHeatDemandProfile = returnArray;
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
	 * finds an optimised set point profile for the day ahead 
	 * based on the projected temperature
	 * and therefore predicted heat pump demand.  This is combined with the 
	 * signal (sent by the aggregator) as a proxy for predicted cost of 
	 * electricity for the day ahead to produce the estimated cost of the 
	 * switching profile.
	 * 
	 * Subject to constraints that 
	 * <li> temperature drop in switch off periods must not exceed a certain amount.
	 * <li> heat pump may be switched off for only one contiguous period per day
	 * <li> the "switch off period" may be between half an hour and four hours
	 * 
	 * Currently uses a "brute force" algorithm to evaluate all profiles
	 * It uses the profile which minimises the predicted cost of operation.
	 * Alter set point profile to use heat pump optimally
	 * 
	 */
	void optimiseSetPointProfile()
	{ 		
		//Initialise optimisation
		float[] localSetPointArray = Arrays.copyOf(setPointProfile, setPointProfile.length);
		this.optimisedSetPointProfile = Arrays.copyOf(setPointProfile, setPointProfile.length);
		float[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));
		float[] localDemandProfile = calculateSpaceHeatPumpDemand(setPointProfile);
		float leastCost = evaluateCost(localDemandProfile);
		float newCost = leastCost;
		float maxRecoveryPerTick = 0.5f * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) * (Consts.SECONDS_PER_DAY / ticksPerDay) * ArrayUtils.max(deltaT)) ; // i.e. can't recover more than 50% of heat loss at 90% COP.  TODO: Need to code this better later

		for (int i = 0; i < localSetPointArray.length; i++)
		{
			//Start each evaluation from the basepoint of the original (user specified) set point profile
			localSetPointArray = Arrays.copyOf(setPointProfile, setPointProfile.length);
			float totalTempLoss = 0;
			
			for ( int j = Consts.HEAT_PUMP_MIN_SWITCHOFF - 1; (j < Consts.HEAT_PUMP_MAX_SWITCHOFF && (i+j < ticksPerDay)); j++)
			{

				float tempLoss = (((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) * (Consts.SECONDS_PER_DAY / ticksPerDay) * Math.max(0,(localSetPointArray[i+j] - priorDayExternalTempProfile[i+j]))) / owner.buildingThermalMass);
				//System.out.println("Temp loss in tick " + (i + j) + " = " + tempLoss);
				float availableHeatRecoveryTicks = 0;
				for (int k = i+j+1; k < localSetPointArray.length; k++)
				{
					localSetPointArray[k] -= tempLoss;
					availableHeatRecoveryTicks++;
				}

				//Sort out where to regain the temperature (if possible)
				totalTempLoss += tempLoss;
				
				int n = (int) Math.ceil((totalTempLoss * owner.buildingThermalMass) / maxRecoveryPerTick);

				if (n <= availableHeatRecoveryTicks)
				{
					float tempToRecover = (totalTempLoss / (float) n);
					//It's possible to recover the temperature
					int[] recoveryIndices = ArrayUtils.findNSmallestIndices(Arrays.copyOfRange(this.dayPredictedCostSignal,i+j+1,ticksPerDay),n);

					for (int l : recoveryIndices)
					{
						for (int m = i+j+l+2; m < ticksPerDay; m++)
						{
							localSetPointArray[m] += tempToRecover;
						}
						//System.out.println("In here, adding temp " + tempToRecover + " from index " + l);
						//System.out.println("With result " + Arrays.toString(localSetPointArray));
					}

					if (ArrayUtils.max(ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(localSetPointArray), ArrayUtils.negate(Consts.MAX_PERMITTED_TEMP_DROPS))) > Consts.FLOATING_POINT_TOLERANCE)
					{
						//if the temperature drop is too great, this profile is unfeasible and we return null
						if (owner.getAgentID() == 1)
						{System.err.println("Temp drop too great with this set point profile, discard");}
					}
					else
					{
						//System.out.println("Calculate pump profile for this temp profile");
						//calculate energy implications and cost for this candidate setPointProfile
						localDemandProfile = calculateSpaceHeatPumpDemand(localSetPointArray);
						if (localDemandProfile != null)
						{
							//in here if the set point profile is achievable
							newCost = evaluateCost(localDemandProfile);
							if (newCost < leastCost)
							{
								if(ArrayUtils.max(localSetPointArray) - ArrayUtils.max(this.setPointProfile) > 0.005)
								{
									System.err.println("Somehow got profile with significantly higher temp than baseline" + Arrays.toString(localSetPointArray));
								}
								
								leastCost = newCost;
								this.optimisedSetPointProfile = Arrays.copyOf(localSetPointArray, localSetPointArray.length);
								this.heatPumpDemandProfile = ArrayUtils.multiply(localDemandProfile, (1/Consts.DOMESTIC_HEAT_PUMP_SPACE_COP));
							}
						}
						else
						{
							//Impossible to recover heat within heat pump limits - discard this attempt.
							if (owner.getAgentID() == 1)
							{System.err.println("Can't recover heat with " + availableHeatRecoveryTicks + " ticks, need " + n);}
						}
					}
				}
			}
		}

		this.expectedNextDaySpaceHeatCost = leastCost;
	}

	/**
	 * @param localSetPointArray
	 * @return
	 */
	private float[] calculateSpaceHeatPumpDemand(float[] localSetPointArray) {
		float[] energyProfile = new float[ticksPerDay];
		float[] deltaT = ArrayUtils.add(localSetPointArray, ArrayUtils.negate(priorDayExternalTempProfile));
		//int availableHeatRecoveryTicks = ticksPerDay;
		float maxRecoveryPerTick = 0.5f * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE; // i.e. can't recover more than 50% of heat loss at 90% COP.  TODO: Need to code this better later
		float heatLoss = 0;
		float internalTemp = this.setPointProfile[0];

		for (int i = 0; i < ticksPerDay; i++)
		{
			//--availableHeatRecoveryTicks;
			currentTempProfile[i] = internalTemp;
			float nextSlotTemp;

			if (i < ticksPerDay - 1)
			{
				nextSlotTemp = localSetPointArray[i+1];
			}
			else
			{
				nextSlotTemp = localSetPointArray[0];
			}

			float setPointMaintenanceEnergy = deltaT[i] * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR)) * (Consts.SECONDS_PER_DAY / ticksPerDay);
			//tempChangePower can be -ve i the temperature is falling.  If tempChangePower magnitude
			//is greater than or equal to setPointMaintenance, the heat pump is off.
			float tempChangeEnergy = (nextSlotTemp - localSetPointArray[i]) * owner.buildingThermalMass;

			float heatPumpEnergyNeeded = Math.max(0, setPointMaintenanceEnergy + tempChangeEnergy);

			//zero the energy if heat pump would be off
			if(deltaT[i] < Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF)
			{
				//heat pump control algorithm would switch off pump
				heatPumpEnergyNeeded = 0;
			}

			if (heatPumpEnergyNeeded > (owner.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP))
			{
				//System.out.println("Nulling the demand profile for energy needed " + heatPumpEnergyNeeded );
				//Can't satisfy this demand for this set point profile, return null
				return null;
			}

			energyProfile[i] = heatPumpEnergyNeeded;
		}

		return energyProfile;

	}

	/**
	 * @param localDemandProfile
	 * @return
	 */
	private float evaluateCost(float[] localDemandProfile) {
		return ArrayUtils.sum(ArrayUtils.mtimes(localDemandProfile, this.dayPredictedCostSignal));
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
		//this.heatPumpDemandProfile = new float[ticksPerDay];
		this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.baselineHotWaterVolumeProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.baselineHotWaterVolumeProfile.length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.baselineHotWaterVolumeProfile.length) + ticksPerDay);

		ticksPerDay = owner.getContext().getTickPerDay();
		if(owner.coldApplianceProfile != null)
		{
			this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.coldApplianceProfile.length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.coldApplianceProfile.length) + ticksPerDay);
		}
		if (owner.wetApplianceProfile != null)
		{
			this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.wetApplianceProfile.length), ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.wetApplianceProfile.length) + ticksPerDay);
		}

		this.maxHeatPumpElecDemandPerTick = (owner.ratedPowerHeatPump * (float) 24 / ticksPerDay);
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
