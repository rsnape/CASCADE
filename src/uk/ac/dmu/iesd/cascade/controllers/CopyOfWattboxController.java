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
public class CopyOfWattboxController implements ISmartController{

	static final double[] INITIALIZATION_TEMPS = {7,7,7,7,7,7,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16,17,17,18,18,17,17,16,16,15,15,14,14,12,12,10,10,8,8,7,7};
	HouseholdProsumer owner;
	WattboxUserProfile userProfile;
	WattboxLifestyle userLifestyle;
	int ticksPerDay;

	double[] dayPredictedCostSignal;
	double predictedCostToRealCostA = 1;
	double realCostOffsetb = 10;
	double[] setPointProfile;
	double[] optimisedSetPointProfile;
	double[] currentTempProfile;

	//Prior day's temperature profile.  Works on the principle that
	// in terms of temperature, today is likely to be similar to yesterday
	double[] priorDayExternalTempProfile;
	double[] coldApplianceProfile;
	double[] wetApplianceProfile;
	double[] heatPumpDemandProfile;
	double[] hotWaterVolumeDemandProfile;

	double[] EVChargingProfile;

	double[] heatPumpOnOffProfile;

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
	private double[] waterHeatDemandProfile;
	private double maxHeatPumpElecDemandPerTick;
	private double expectedNextDaySpaceHeatCost;


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
		double[] ownersCostSignal = owner.getPredictedCostSignal();
		this.dayPredictedCostSignal = Arrays.copyOfRange(ownersCostSignal, timeStep % ownersCostSignal.length, timeStep % ownersCostSignal.length + ticksPerDay);
		this.dayPredictedCostSignal = ArrayUtils.offset(ArrayUtils.multiply(this.dayPredictedCostSignal, predictedCostToRealCostA),realCostOffsetb);
		this.setPointProfile = owner.getSetPointProfile();
		this.optimisedSetPointProfile = this.setPointProfile;
		this.currentTempProfile = this.setPointProfile;
		this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,(timeStep % owner.coldApplianceProfile.length) , (timeStep % owner.coldApplianceProfile.length) + ticksPerDay);
		this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,(timeStep % owner.wetApplianceProfile.length), (timeStep % owner.wetApplianceProfile.length) + ticksPerDay);
		this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.getBaselineHotWaterVolumeProfile(),(timeStep % owner.getBaselineHotWaterVolumeProfile().length), (timeStep % owner.getBaselineHotWaterVolumeProfile().length) + ticksPerDay);
		this.heatPumpDemandProfile = calculatePredictedHeatPumpDemandAndTempProfile(this.heatPumpOnOffProfile);


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
		if (spaceHeatingControlled && owner.getHasElectricalSpaceHeat())
		{
			//optimiseSpaceHeatProfile();
			optimiseSetPointProfile();
			//System.out.println(Arrays.toString(this.heatPumpOnOffProfile));
		}	


		if (waterHeatingControlled && owner.getHasElectricalWaterHeat())
		{
			optimiseWaterHeatProfile();
		}


		if (eVehicleControlled && owner.hasElectricVehicle())
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
	private double[] calculatePredictedHeatPumpDemandAndTempProfile(double[] heatPumpOnOffProfileToTest) 
	{
		double[] energyProfile = new double[ticksPerDay];
		double[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));
		int availableHeatRecoveryTicks = ticksPerDay;
		double maxRecoveryPerTick = 0.5d * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE; // i.e. can't recover more than 50% of heat loss at 90% COP.  TODO: Need to code this better later
		double heatLoss = 0;
		double internalTemp = this.setPointProfile[0];

		for (int i = 0; i < ticksPerDay; i++)
		{
			--availableHeatRecoveryTicks;
			currentTempProfile[i] = internalTemp;
			double setPointMaintenancePower = deltaT[i] * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) * (Consts.SECONDS_PER_DAY / ticksPerDay);


			if (deltaT[i] > Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF && (heatPumpOnOffProfileToTest[i] > 0))
			{

				if (heatLoss > 0)
				{
					//need to place recovery power somewhere
					//estimate required recovery timeTicks
					int n = (int) Math.ceil(heatLoss / (maxRecoveryPerTick * setPointMaintenancePower));

					if (availableHeatRecoveryTicks < n)
					{
						//Recovery impossible - terminate processing
						System.err.println("CopyOfWattBoxController: No available recovery ticks, profile is " + Arrays.toString(heatPumpOnOffProfileToTest));
						return null;
					}
					else
					{
						availableHeatRecoveryTicks = availableHeatRecoveryTicks - n;
						int[] recoveryIndices = ArrayUtils.findNSmallestIndices(Arrays.copyOfRange(this.dayPredictedCostSignal,i,ticksPerDay),n);

						for (int k : recoveryIndices)
						{
							//System.out.println("in here for index " + k);
							heatPumpOnOffProfileToTest[k + i] = 1 + maxRecoveryPerTick;
						}

						//We have now allocated the recovery power required, so zero it.
						heatLoss = 0;
					}
				}
				energyProfile[i] = setPointMaintenancePower * heatPumpOnOffProfileToTest[i];
				if (heatPumpOnOffProfileToTest[i] > 1)
				{
					internalTemp += (setPointMaintenancePower * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP * (heatPumpOnOffProfileToTest[i] - 1)) / owner.buildingThermalMass;
				}

			}
			else
			{	
				energyProfile[i] = 0;

				internalTemp -= setPointMaintenancePower / owner.buildingThermalMass;
				double tempLoss = setPointProfile[i] - internalTemp;

				heatLoss += setPointMaintenancePower * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP;
				//System.out.print("Temp loss is " + tempLoss);
				if( (i < Consts.NIGHT_TO_DAY_TRANSITION_TICK && tempLoss > Consts.NIGHT_TEMP_LOSS_THRESHOLD) || (i >= Consts.NIGHT_TO_DAY_TRANSITION_TICK && tempLoss > Consts.DAYTIME_TEMP_LOSS_THRESHOLD))
				{
					//if the temperature drop is too great, this profile is unfeasible and we return null
					System.err.println("Temp drop too great with profile " + Arrays.toString(heatPumpOnOffProfileToTest));
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
		double bestCost;
		this.optimisedSetPointProfile = Arrays.copyOf(setPointProfile, setPointProfile.length);
		double[] tempPumpOnOffProfile = new double[ticksPerDay];
		Arrays.fill(tempPumpOnOffProfile,1);
		double[] tempHeatPumpProfile = new double[ticksPerDay];

		if(this.heatPumpDemandProfile == null)
		{
			// If the demand profile is null, this means the switiching profile
			// is invalid or undefined, so set a very high cost to this
			// so that all alternatives will be better.
			bestCost = Double.MAX_VALUE;
		}
		else
		{
			double[] initialPredictedCosts = ArrayUtils.mtimes(this.dayPredictedCostSignal, this.heatPumpDemandProfile);
			bestCost = ArrayUtils.sum(initialPredictedCosts);
		}

		tempHeatPumpProfile = calculatePredictedHeatPumpDemandAndTempProfile(tempPumpOnOffProfile);
		this.heatPumpOnOffProfile = Arrays.copyOf(tempPumpOnOffProfile, tempPumpOnOffProfile.length);
		this.heatPumpDemandProfile = Arrays.copyOf(tempHeatPumpProfile, tempHeatPumpProfile.length);

		//Go through all timeslots switching the heat pump off for certain periods
		//subject to constraints and find the least cost option.
		//TODO: This is "brute force" optimisation - there will be a better way...

		for (int i = 0; i < ticksPerDay; i++)
		{
			for ( int j = Consts.HEAT_PUMP_MIN_SWITCHOFF - 1; j < Consts.HEAT_PUMP_MAX_SWITCHOFF; j++)
			{
				//Don't try impossible combinations - can't recover heat beyond the day boundary
				if (i+j < ticksPerDay - 1)
				{
					double thisCost = 0;

					tempPumpOnOffProfile[i+j] = 0;
					tempHeatPumpProfile = calculatePredictedHeatPumpDemandAndTempProfile(tempPumpOnOffProfile);
					if (Consts.DEBUG && owner.getAgentID() == 1)
					{
						//Some debugging output for one agent only
						System.out.println("CopyOfWattBoxController: On off profile for calculation " + Arrays.toString(tempPumpOnOffProfile));
						System.out.println("CopyOfWattBoxController: HP demand profile " + Arrays.toString(tempHeatPumpProfile));
					}

					if (tempHeatPumpProfile != null)
					{
						thisCost = ArrayUtils.sum(ArrayUtils.mtimes(this.dayPredictedCostSignal, tempHeatPumpProfile));
						if (thisCost < bestCost)
						{

							bestCost = thisCost;
							this.heatPumpOnOffProfile = Arrays.copyOf(tempPumpOnOffProfile, tempPumpOnOffProfile.length);
							this.heatPumpDemandProfile = Arrays.copyOf(tempHeatPumpProfile, tempHeatPumpProfile.length);
							this.optimisedSetPointProfile = Arrays.copyOf(currentTempProfile, currentTempProfile.length);
							if (Consts.DEBUG && owner.getAgentID() == 1)
							{
								//Some debugging output for one agent only
								System.out.println("CopyOfWattBoxController: Replacing the current profile with a better one");
								System.out.println("On off profile to use " + Arrays.toString(heatPumpOnOffProfile));
								System.out.println("HP demand profile " + Arrays.toString(heatPumpDemandProfile));
								System.out.println("CopyOfWattboxController: This cost is " + thisCost + " against best " + bestCost);

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
	 */
	private void optimiseWaterHeatProfile() 
	{		
		double[] returnArray = ArrayUtils.multiply(this.hotWaterVolumeDemandProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (owner.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP) );

		for (int i = 0; i < returnArray.length; i++)
		{
			double currDemand = returnArray[i];

			//If demand exceeds Heat Pump capacity, top up with immersion (i.e. reduce COP)
			if ((currDemand + this.heatPumpDemandProfile[i]) > this.maxHeatPumpElecDemandPerTick )
			{
				currDemand = currDemand + (((currDemand + this.heatPumpDemandProfile[i]) - this.maxHeatPumpElecDemandPerTick ) * Consts.DOMESTIC_HEAT_PUMP_WATER_COP / Consts.IMMERSION_HEATER_COP);
			}

			double currCost = currDemand * this.dayPredictedCostSignal[i];
			int moveTo = -1;
			double newHeatDemand = 0;

			if (currDemand > 0)
			{
				double extraHeatRequired = 0;
				double adaptedDemand = 0;
				for (int j = i-1; j >= 0; j--)
				{
					extraHeatRequired += (Consts.WATER_TEMP_LOSS_PER_SECOND * ((double)Consts.SECONDS_PER_DAY / ticksPerDay)) * this.hotWaterVolumeDemandProfile[i] * (Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR);
					adaptedDemand = currDemand + (extraHeatRequired / Consts.DOMESTIC_HEAT_PUMP_WATER_COP);

					//If demand exceeds Heat Pump capacity, top up with immersion (i.e. reduce COP)
					if ((adaptedDemand + this.heatPumpDemandProfile[i]) > this.maxHeatPumpElecDemandPerTick )
					{
						System.out.println("CopyOfWattBoxController: Topping up with immersion due to over capacity - before: " + adaptedDemand);
						adaptedDemand = currDemand + (((currDemand + this.heatPumpDemandProfile[i]) - this.maxHeatPumpElecDemandPerTick ) * Consts.DOMESTIC_HEAT_PUMP_WATER_COP / Consts.IMMERSION_HEATER_COP);
						System.out.println("And after: " + adaptedDemand);
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
		double[] currentCost = ArrayUtils.mtimes(wetApplianceProfile, dayPredictedCostSignal);
		int maxIndex = ArrayUtils.indexOfMax(currentCost);
		int minIndex = ArrayUtils.indexOfMin(currentCost);
		// First pass - simply swap the load in the max cost slot with that in the min cost slot
		//TODO: very crude and will give nasty positive feedback in all likelihood
		if (maxIndex < (wetApplianceProfile.length -1 ))
		{
			double temp = wetApplianceProfile[minIndex];
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
		double[] currentCost = ArrayUtils.mtimes(coldApplianceProfile, dayPredictedCostSignal);
		int maxIndex = ArrayUtils.indexOfMax(currentCost);
		int minIndex = ArrayUtils.indexOfMin(currentCost);
		// First pass - simply swap the load in the max cost slot with that in the min cost slot
		//TODO: very crude and will give nasty positive feedback in all likelihood
		if (maxIndex < (coldApplianceProfile.length -1 ))
		{
			double temp = coldApplianceProfile[minIndex];
			coldApplianceProfile[minIndex] = coldApplianceProfile[maxIndex];
			coldApplianceProfile[maxIndex] = temp;
		}
	}


	/**
	 * @param dayPredictedCostSignal the dayPredictedCostSignal to set
	 */
	public void setDayPredictedCostSignal(double[] dayPredictedCostSignal) 
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
		//Initialise optimisation
		double[] localSetPointArray = Arrays.copyOf(setPointProfile, setPointProfile.length);
		this.optimisedSetPointProfile = Arrays.copyOf(setPointProfile, setPointProfile.length);
		double[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));
		double[] localDemandProfile = calculateSpaceHeatPumpDemand(setPointProfile);
		double[] returnProfile = Arrays.copyOf(localDemandProfile, localDemandProfile.length);
		double leastCost = evaluateCost(localDemandProfile);
		double newCost = leastCost;
		double maxRecoveryPerTick = 0.5d * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) * (Consts.SECONDS_PER_DAY / ticksPerDay) * ArrayUtils.max(deltaT)) ; // i.e. can't recover more than 50% of heat loss at 90% COP.  TODO: Need to code this better later

		for (int i = 0; i < localSetPointArray.length; i++)
		{
			//Start each evaluation from the basepoint of the original (user specified) set point profile
			localSetPointArray = Arrays.copyOf(setPointProfile, setPointProfile.length);
			for ( int j = Consts.HEAT_PUMP_MIN_SWITCHOFF - 1; (j < Consts.HEAT_PUMP_MAX_SWITCHOFF && (i+j < ticksPerDay)); j++)
			{

				double tempLoss = (((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) * (Consts.SECONDS_PER_DAY / ticksPerDay) * (localSetPointArray[i+j] - priorDayExternalTempProfile[i+j])) / owner.buildingThermalMass);


				if( (i < Consts.NIGHT_TO_DAY_TRANSITION_TICK && tempLoss > Consts.NIGHT_TEMP_LOSS_THRESHOLD) || (i >= Consts.NIGHT_TO_DAY_TRANSITION_TICK && tempLoss > Consts.DAYTIME_TEMP_LOSS_THRESHOLD))
				{
					//if the temperature drop is too great, this profile is unfeasible and we return null
					System.err.println("CopyOfWattBoxController: Temp drop too great with this set point profile, discard");
				}
				else
				{
					double availableHeatRecoveryTicks = 0;
					for (int k = i+j+1; k < localSetPointArray.length; k++)
					{
						localSetPointArray[k] -= tempLoss;
						availableHeatRecoveryTicks++;
					}

					//Sort out where to regain the temperature (if possible)

					int n = (int) Math.ceil((tempLoss * owner.buildingThermalMass) / maxRecoveryPerTick);

					if (n <= availableHeatRecoveryTicks)
					{
						//It's possible to recover the temperature
						int[] recoveryIndices = ArrayUtils.findNSmallestIndices(Arrays.copyOfRange(this.dayPredictedCostSignal,i+j+1,ticksPerDay),n);

						for (int l : recoveryIndices)
						{
							localSetPointArray[i+j+l] += (tempLoss / n);
						}

						//calculate energy implications and cost for this candidate setPointProfile
						localDemandProfile = calculateSpaceHeatPumpDemand(localSetPointArray);
						if (localDemandProfile != null)
						{
							//in here if the set point profile is achievable
							newCost = evaluateCost(localDemandProfile);
							if (newCost < leastCost)
							{
								leastCost = newCost;
								returnProfile = localDemandProfile;
								this.optimisedSetPointProfile = localSetPointArray;
							}
						}
					}
					else
					{
						//Impossible to recover heat - discard this attempt.
					}
				}
			}
		}

		this.heatPumpDemandProfile = ArrayUtils.multiply(localDemandProfile, (1/Consts.DOMESTIC_HEAT_PUMP_SPACE_COP));
		this.expectedNextDaySpaceHeatCost = leastCost;
	}

	/**
	 * @param localSetPointArray
	 * @return
	 */
	private double[] calculateSpaceHeatPumpDemand(double[] localSetPointArray) {
		double[] energyProfile = new double[ticksPerDay];
		double[] deltaT = ArrayUtils.add(localSetPointArray, ArrayUtils.negate(priorDayExternalTempProfile));
		//int availableHeatRecoveryTicks = ticksPerDay;
		double maxRecoveryPerTick = 0.5d * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE; // i.e. can't recover more than 50% of heat loss at 90% COP.  TODO: Need to code this better later
		double heatLoss = 0;
		double internalTemp = this.setPointProfile[0];

		for (int i = 0; i < ticksPerDay; i++)
		{
			//--availableHeatRecoveryTicks;
			currentTempProfile[i] = internalTemp;
			double nextSlotTemp;

			if (i < ticksPerDay - 1)
			{
				nextSlotTemp = localSetPointArray[i+1];
			}
			else
			{
				nextSlotTemp = localSetPointArray[0];
			}

			double setPointMaintenanceEnergy = deltaT[i] * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR)) * (Consts.SECONDS_PER_DAY / ticksPerDay);
			//tempChangePower can be -ve i the temperature is falling.  If tempChangePower magnitude
			//is greater than or equal to setPointMaintenance, the heat pump is off.
			double tempChangeEnergy = (nextSlotTemp - localSetPointArray[i]) * owner.buildingThermalMass;

			double heatPumpEnergyNeeded = Math.max(0, setPointMaintenanceEnergy - tempChangeEnergy);

			//zero the energy if heat pump would be off
			if(deltaT[i] > Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF)
			{
				//heat pump control algorithm would switch off pump
				heatPumpEnergyNeeded = 0;
			}

			if (heatPumpEnergyNeeded > (owner.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP))
			{
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
	private double evaluateCost(double[] localDemandProfile) {
		return ArrayUtils.sum(ArrayUtils.mtimes(localDemandProfile, this.dayPredictedCostSignal));
	}

	/**
	 * Constructor - creates an uninitialized Wattbox controller assigned to 
	 * the prosumer passed in as an argument.
	 * 
	 * @param owner - the prosumer agent that owns this wattbox
	 */
	public CopyOfWattboxController(HouseholdProsumer owner) 
	{
		this.owner = owner;
		this.priorDayExternalTempProfile = Arrays.copyOf(INITIALIZATION_TEMPS, INITIALIZATION_TEMPS.length);
		this.heatPumpOnOffProfile = Arrays.copyOf(owner.spaceHeatPumpOn,owner.spaceHeatPumpOn.length);
		//this.heatPumpDemandProfile = new double[ticksPerDay];
		this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.getBaselineHotWaterVolumeProfile(),((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.getBaselineHotWaterVolumeProfile().length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.getBaselineHotWaterVolumeProfile().length) + ticksPerDay);

		ticksPerDay = owner.getContext().getNbOfTickPerDay();
		if(owner.coldApplianceProfile != null)
		{
			this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.coldApplianceProfile.length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.coldApplianceProfile.length) + ticksPerDay);
		}
		if (owner.wetApplianceProfile != null)
		{
			this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.wetApplianceProfile.length), ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.wetApplianceProfile.length) + ticksPerDay);
		}

		this.maxHeatPumpElecDemandPerTick = (owner.ratedPowerHeatPump * (double) 24 / ticksPerDay);
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
	public CopyOfWattboxController(HouseholdProsumer owner,
			WattboxUserProfile userProfile, WattboxLifestyle userLifestyle) 
	{
		super();
		this.owner = owner;
		this.userProfile = userProfile;
		this.userLifestyle = userLifestyle;
	}
}
