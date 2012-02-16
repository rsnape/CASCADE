/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.*;

import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.context.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * This class implements an abstracted version of the "Wattbox"
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

	static final double[] INITIALIZATION_TEMPS = {7,7,7,7,7,7,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16,17,17,18,18,17,17,16,16,15,15,14,14,12,12,10,10,8,8,7,7};
	HouseholdProsumer owner;
	WattboxUserProfile userProfile;
	WattboxLifestyle userLifestyle;
	int ticksPerDay;

	double[] dayPredictedCostSignal;
	// Should always have b > A, otherwise there can be zero or negative cost to consumption
	// which often makes the optimisation algorithms "spike" the consumption at that point.
	double predictedCostToRealCostA = 9;
	double realCostOffsetb = 10;
	double[] setPointProfile;
	double[] optimisedSetPointProfile;
	double[] currentTempProfile;

	//Prior day's temperature profile.  Works on the principle that
	// in terms of temperature, today is likely to be similar to yesterday
	double[] priorDayExternalTempProfile;
	double[] heatPumpDemandProfile;
	double[] hotWaterVolumeDemandProfile;

	double[] EVChargingProfile;

	double[] heatPumpOnOffProfile;

	protected CascadeContext mainContext;

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
	private double maxImmersionHeatPerTick;
	private double[] noElecHeatingDemand = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

	//Uniform coldAndWetApplTimeslotDelayRandDist;  //temp


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
		checkForNewAppliancesAndUpdateConstants();

		double[] ownersCostSignal = owner.getPredictedCostSignal();
		this.dayPredictedCostSignal = Arrays.copyOfRange(ownersCostSignal, timeStep % ownersCostSignal.length, timeStep % ownersCostSignal.length + ticksPerDay);

		//System.out.println("update");

		//System.out.println("dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));

		this.dayPredictedCostSignal = ArrayUtils.offset(ArrayUtils.multiply(this.dayPredictedCostSignal, predictedCostToRealCostA),realCostOffsetb);

		//System.out.println("afterOffset dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));

		if (owner.getHasElectricalSpaceHeat())
		{
			this.setPointProfile = Arrays.copyOf(owner.getSetPointProfile(), owner.getSetPointProfile().length);
			this.optimisedSetPointProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
			this.currentTempProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
			this.heatPumpDemandProfile = calculateEstimatedSpaceHeatPumpDemand(this.setPointProfile);
			// (20/01/12) Check if sum of <heatPumpDemandProfile> is consistent at end day
			if (Consts.DEBUG)
				System.out.println("Sum(Wattbox estimated heatPumpDemandProfile): "+ ArrayUtils.sum(this.heatPumpDemandProfile));
		}
		else
		{
			this.heatPumpDemandProfile = Arrays.copyOf(noElecHeatingDemand, noElecHeatingDemand.length);
		}

		if (owner.getHasElectricalWaterHeat())
		{
			this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.getBaselineHotWaterVolumeProfile(),(timeStep % owner.getBaselineHotWaterVolumeProfile().length), (timeStep % owner.getBaselineHotWaterVolumeProfile().length) + ticksPerDay);
			this.waterHeatDemandProfile = ArrayUtils.multiply(hotWaterVolumeDemandProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (owner.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP) );
		}
		else
		{
			this.waterHeatDemandProfile = Arrays.copyOf(noElecHeatingDemand, noElecHeatingDemand.length);
		}

		if (coldAppliancesControlled && owner.getHasColdAppliances())
		{
			optimiseColdProfile(timeStep);
		}

		if (wetAppliancesControlled && owner.getHasWetAppliances())
		{
			optimiseWetProfile(timeStep);
		}


		// Note - optimise space heating first.  This is so that we can look for absolute
		// heat pump limit and add the cost of using immersion heater (COP 0.9) to top
		// up water heating if the heat pump is too great
		if (spaceHeatingControlled && owner.getHasElectricalSpaceHeat())
		{
			optimiseSetPointProfile();
			if (Consts.DEBUG) System.out.println("Optimised set point profile = " + Arrays.toString(this.heatPumpOnOffProfile));
		}	

		if (waterHeatingControlled && owner.getHasElectricalWaterHeat())
		{
			optimiseWaterHeatProfileWithSpreading();
		}

		if (eVehicleControlled && owner.hasElectricVehicle())
		{
			optimiseEVProfile();
		}

		//At the end of the step, set the temperature profile for today's 
		//(which will be yesterday's when it is used)
		this.priorDayExternalTempProfile = owner.getContext().getAirTemperature(timeStep, ticksPerDay);
	}

	/**
	 * A method to allow Wattbox to be dynamic in the sense of monitoring its owner
	 * for any new appliances and updating its state to accommodate.
	 */
	private void checkForNewAppliancesAndUpdateConstants() {
		// This could be more sophisticated, but for now all this needs to do is make sure that
		// the wattbox assigns load only according to the rated heat pump and immersion capacity
		// of its owner
		this.maxHeatPumpElecDemandPerTick = (owner.ratedPowerHeatPump * (double) 24 / ticksPerDay);
		this.maxImmersionHeatPerTick = Consts.MAX_DOMESTIC_IMMERSION_POWER * (double) 24 / ticksPerDay;
	}

	/**
	 * method to return all the optimised profiles currently held by this Wattbox
	 */
	public WeakHashMap<String,double[]> getCurrentProfiles()
	{
		WeakHashMap<String,double[]> returnMap = new WeakHashMap<String,double[]>();
		returnMap.put("HeatPump", optimisedSetPointProfile);
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
	private void optimiseWaterHeatProfileWithSpreading() 
	{		
		if (Consts.DEBUG) System.out.println("== OptimiseWaterHeatProfile for a  "+ owner.getAgentID() + " ==");

		double[] baseArray = ArrayUtils.multiply(this.hotWaterVolumeDemandProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (owner.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP)) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP);
		if (Consts.DEBUG) System.out.println("hotWaterVolumeDemandProfile: "+ Arrays.toString(hotWaterVolumeDemandProfile));

		this.waterHeatDemandProfile = Arrays.copyOf(baseArray, baseArray.length);
		if (Consts.DEBUG) System.out.println("waterHeatDemandProfile: "+ Arrays.toString(waterHeatDemandProfile));

		if (Consts.DEBUG) System.out.println("spreadWaterDemand(baseArray) : "+ Arrays.toString(spreadWaterDemand(baseArray)));

		double[] totalHeatDemand = ArrayUtils.add(this.heatPumpDemandProfile, spreadWaterDemand(baseArray));

		if (Consts.DEBUG) System.out.println("totalHeatDemand: "+ Arrays.toString(totalHeatDemand));

		double currCost = evaluateCost(totalHeatDemand);
		double[] tempArray = Arrays.copyOf(baseArray, baseArray.length);

		for (int i = 0; i < baseArray.length; i++)
		{
			if (baseArray[i] > 0)
			{
				double extraHeatRequired = 0;
				for (int j = i-1; j >= 0; j--)
				{
					extraHeatRequired += (Consts.WATER_TEMP_LOSS_PER_SECOND * ((double)Consts.SECONDS_PER_DAY / ticksPerDay)) * this.hotWaterVolumeDemandProfile[i] * (Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP;
					tempArray[j] += baseArray[i] + extraHeatRequired;
					tempArray[j+1] = 0;
					totalHeatDemand = ArrayUtils.add(this.heatPumpDemandProfile, spreadWaterDemand(tempArray));
					double newCost = evaluateCost(totalHeatDemand);
					if (newCost < currCost)
					{
						this.waterHeatDemandProfile = ArrayUtils.add(totalHeatDemand, ArrayUtils.negate(this.heatPumpDemandProfile));
						currCost = newCost;
					}
				}
			}
		}
	}

	/**
	 * Imposes the constraints of the household appliances (heat pump and immersion capacities)
	 * on the water heating profile proposed by the Wattbox algorithm
	 * 
	 * @param basicDemandArray
	 * @return
	 */
	private double[] spreadWaterDemand(double[] basicDemandArray)
	{
		double[] totalHeatDemand = ArrayUtils.add(basicDemandArray, this.heatPumpDemandProfile);
		for (int i = 0; i < totalHeatDemand.length; i++)
		{
			if (totalHeatDemand[i] > this.maxHeatPumpElecDemandPerTick )
			{
				//If heat pump can't cope, use the immersion to top up.
				totalHeatDemand[i]  = totalHeatDemand[i]  + ((totalHeatDemand[i]  - this.maxHeatPumpElecDemandPerTick ) * Consts.DOMESTIC_HEAT_PUMP_WATER_COP / Consts.IMMERSION_HEATER_COP);
				if (totalHeatDemand[i] > this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick)
				{
					//Even the immersion can't cope - spread the load into the ticks beforehand
					double heatToMove = (totalHeatDemand[i] - ( this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick)) * Consts.IMMERSION_HEATER_COP;
					totalHeatDemand[i] = this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick;
					for (int j = i-1; j >= 0 && heatToMove > 0; j--)
					{
						totalHeatDemand[j] += heatToMove / Consts.DOMESTIC_HEAT_PUMP_WATER_COP;
						if (totalHeatDemand[j] > this.maxHeatPumpElecDemandPerTick)
						{
							totalHeatDemand[j]  = totalHeatDemand[j]  + ((totalHeatDemand[j]  - this.maxHeatPumpElecDemandPerTick ) * Consts.DOMESTIC_HEAT_PUMP_WATER_COP / Consts.IMMERSION_HEATER_COP);
							if (totalHeatDemand[j] > this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick)
							{		
								//Even the immersion can't cope - spread the load into the ticks beforehand

								heatToMove = (totalHeatDemand[j] - ( this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick)) * Consts.IMMERSION_HEATER_COP;
								totalHeatDemand[j] = this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick;
							}
							else
							{
								heatToMove = 0;
							}
						}
						else
						{
							heatToMove = 0;
						}	
					}

					if (heatToMove > 0)
					{
						//haven't been able to heat the water by the required time.  Move extra load to the end of the day... ?
						//System.out.println("Water heating fail");
						//totalHeatDemand[0] += heatToMove;
						for (int j = totalHeatDemand.length - 1; j >= i && heatToMove > 0; j--)
						{
							totalHeatDemand[j] += heatToMove / Consts.DOMESTIC_HEAT_PUMP_WATER_COP;
							if (totalHeatDemand[j] > this.maxHeatPumpElecDemandPerTick)
							{
								totalHeatDemand[j]  = totalHeatDemand[j]  + ((totalHeatDemand[j]  - this.maxHeatPumpElecDemandPerTick ) * Consts.DOMESTIC_HEAT_PUMP_WATER_COP / Consts.IMMERSION_HEATER_COP);
								if (totalHeatDemand[j] > this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick)
								{		
									//Even the immersion can't cope - spread the load into the ticks beforehand

									heatToMove = (totalHeatDemand[j] - ( this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick)) * Consts.IMMERSION_HEATER_COP;
									totalHeatDemand[j] = this.maxHeatPumpElecDemandPerTick + this.maxImmersionHeatPerTick;
								}
								else
								{
									heatToMove = 0;
								}
							}
							else
							{
								heatToMove = 0;
							}	
						}
					}
				}
			}
		}

		return ArrayUtils.add(totalHeatDemand, ArrayUtils.negate(this.heatPumpDemandProfile));
	}

	
	private double[] optimiseWaterHeatVolumeBased()
	{
		double[] optimisedWaterVolumeProfile = Arrays.copyOf(this.hotWaterVolumeDemandProfile, this.hotWaterVolumeDemandProfile.length);
		
		return EVChargingProfile;
	
	}

	/**
	 * Optimise the Wet appliance usage profile for the household
	 * which owns this Wattbox.
	 * 
	 */
	private void optimiseWetProfile(int timeStep) 
	{
		//System.out.println("==OptimiseWetProfil for a  "+ owner.getAgentID()+"; timeStep: "+ timeStep);
		//System.out.println("dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));

		WeakHashMap<String,double[]> wetApplianceProfiles = owner.getWetAppliancesProfiles();
		double [] washer_loads = wetApplianceProfiles.get(Consts.WET_APP_WASHER_ORIGINAL);
		double [] dryer_loads = wetApplianceProfiles.get(Consts.WET_APP_DRYER_ORIGINAL);
		double [] dishwasher_loads = wetApplianceProfiles.get(Consts.WET_APP_DISHWASHER_ORIGINAL);

		double [] washer_loads_day = Arrays.copyOfRange(washer_loads,(timeStep % washer_loads.length) , (timeStep % washer_loads.length) + ticksPerDay);
		double [] dryer_loads_day = Arrays.copyOfRange(dryer_loads,(timeStep % dryer_loads.length) , (timeStep % dryer_loads.length) + ticksPerDay);
		double [] dishwasher_loads_day = Arrays.copyOfRange(dishwasher_loads,(timeStep % dishwasher_loads.length) , (timeStep % dishwasher_loads.length) + ticksPerDay);

		//System.out.println("BEFORE washer_loads_day: "+ Arrays.toString(washer_loads_day));
		//System.out.println("BEFORE dryer_loads_day: "+ Arrays.toString(dryer_loads_day));
		//System.out.println("BEFORE dishwasher_loads_day: "+ Arrays.toString(dishwasher_loads_day));

		double[] currentWasherCostArr = ArrayUtils.mtimes(washer_loads_day, dayPredictedCostSignal);

		//System.out.println("currentWasherCost: "+ Arrays.toString(currentWasherCostArr));

		int maxIndexForWasher = ArrayUtils.indexOfMax(currentWasherCostArr);
		double maxValForWasher = washer_loads_day[maxIndexForWasher];
		double currentCostForWasher = currentWasherCostArr[maxIndexForWasher];

		if ((maxValForWasher > 0) && (maxIndexForWasher < washer_loads_day.length-1)) {

			//System.out.println("max index for Wahser: "+ maxIndexForWasher + " val: "+maxValForWasher + " current cost: "+ currentCostForWasher);
			int newIndexForWasher = mainContext.coldAndWetApplTimeslotDelayRandDist.nextIntFromTo(maxIndexForWasher+1, washer_loads_day.length-1);

			double newCostForWasher = maxValForWasher* dayPredictedCostSignal[newIndexForWasher];

			//System.out.println("newIndexForWasher (1st): "+ newIndexForWasher +" new cost: "+ newCostForWahser);
			if (dayPredictedCostSignal[maxIndexForWasher] != dayPredictedCostSignal[newIndexForWasher])
				//System.out.println("Signal: "+dayPredictedCostSignal[maxIndexForWasher] + " != (new) "  +dayPredictedCostSignal[newIndexForWasher]);

				if (newCostForWasher < currentCostForWasher) {
					//System.out.println(" newCostForWahser < currentCostForWasher ");

					washer_loads_day[maxIndexForWasher] = 0;
					washer_loads_day[newIndexForWasher] = washer_loads_day[newIndexForWasher] + maxValForWasher;

					ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_WASHER), washer_loads_day,timeStep % washer_loads.length);
				}
				else  {
					//System.out.println(" newCostForWahser !< currentCostForWasher ");

					int iWasher=newIndexForWasher+1;
					boolean lowerCostFound = false;
					while ((iWasher < washer_loads_day.length) && (!lowerCostFound)) {
						newCostForWasher = maxValForWasher* dayPredictedCostSignal[iWasher];
						//System.out.println(ind+" newCostForWahser to considered: "+newCostForWahser + "  "+ (newCostForWahser < currentCostForWasher));
						if (dayPredictedCostSignal[maxIndexForWasher] != dayPredictedCostSignal[iWasher])
						{
							//System.out.println("Signal: "+dayPredictedCostSignal[maxIndexForWasher] + " != (new) "  +dayPredictedCostSignal[ind]);
						}
							
							if (newCostForWasher < currentCostForWasher) {
								lowerCostFound=true;
							}
							else {
								iWasher++;
							}	
					}

					if (lowerCostFound) {
						washer_loads_day[maxIndexForWasher] = 0;
						washer_loads_day[iWasher] = washer_loads_day[iWasher] + maxValForWasher;

						ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_WASHER), washer_loads_day,timeStep % washer_loads.length);
						//System.out.println("lowerCostFound: newIndexForWasher (2nd found): "+ ind +" new cost (2): "+ newCostForWahser);		
					}	
				}
		}

		//=============================================================

		double[] currentDryerCostArr = ArrayUtils.mtimes(dryer_loads_day, dayPredictedCostSignal);
		int maxIndexForDryer = ArrayUtils.indexOfMax(currentDryerCostArr);
		double maxValForDryer = dryer_loads_day[maxIndexForDryer];        
		double currentCostForDryer = currentDryerCostArr[maxIndexForDryer];

		if ((maxValForDryer > 0) && (maxIndexForDryer < dryer_loads_day.length-1)) {

			//System.out.println("max index for Dryer: "+ maxIndexForDryer + " val: "+maxValForDryer);
			int newIndexForDryer = mainContext.coldAndWetApplTimeslotDelayRandDist.nextIntFromTo(maxIndexForDryer+1, dryer_loads_day.length-1); 
			//System.out.println("newIndexForDryer: "+ newIndexForDryer);	
			double newCostForDryer = maxValForDryer* dayPredictedCostSignal[newIndexForDryer];

			if (newCostForDryer < currentCostForDryer) {
				//System.out.println(" newCostForDryer < currentCostForDryer ");

				dryer_loads_day[maxIndexForDryer] = 0;
				dryer_loads_day[maxIndexForDryer] = dryer_loads_day[maxIndexForDryer] + maxValForDryer;

				ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_DRYER), dryer_loads_day,timeStep % dryer_loads.length);
			}
			else  {
				//System.out.println(" newCostForDryer !< currentCostForDryer ");
				int iDryer=newIndexForDryer+1;
				boolean lowerCostFoundForDryer = false;
				while ((iDryer < dryer_loads_day.length) && (!lowerCostFoundForDryer)) {
					newCostForDryer = maxValForDryer* dayPredictedCostSignal[iDryer];
					//System.out.println(iDryer+" newCostForDryer to considered: "+newCostForDryer + "  "+ (newCostForDryer < currentCostForDryer));
					if (dayPredictedCostSignal[maxIndexForDryer] != dayPredictedCostSignal[iDryer])
					{
						//System.out.println("Signal: "+dayPredictedCostSignal[maxIndexForDryer] + " != (new) "  +dayPredictedCostSignal[iDryer]);
					}

					if (newCostForDryer < currentCostForDryer) {
						lowerCostFoundForDryer=true;
					}
					else {
						iDryer++;
					}	
				}

				if (lowerCostFoundForDryer) {
					dryer_loads_day[maxIndexForDryer] = 0;
					dryer_loads_day[iDryer] = dryer_loads_day[iDryer] + maxValForDryer;

					ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_DRYER), dryer_loads_day,timeStep % dryer_loads.length);
					//System.out.println("lowerCostFoundForDryer: newIndexForDryer (2nd found): "+ iDryer +" new cost (2): "+ newCostForDryer);		
				}	
			}
		}

		double[] currentDishwasherCostArr = ArrayUtils.mtimes(dishwasher_loads_day, dayPredictedCostSignal);
		int maxIndexForDishwasher = ArrayUtils.indexOfMax(currentDishwasherCostArr);
		double maxValForDishwasher = dishwasher_loads_day[maxIndexForDishwasher];        
		double currentCostForDishwasher = currentDishwasherCostArr[maxIndexForDishwasher];

		if ((maxValForDishwasher > 0) && (maxIndexForDishwasher < dishwasher_loads_day.length-1)) {

			//dishwasher_loads_day[maxIndexForDishwasher] = 0;
			//System.out.println("max index for Dishwasher: "+ maxIndexForDishwasher + " val: "+maxValForDishwasher);
			int newIndexForDishwasher = mainContext.coldAndWetApplTimeslotDelayRandDist.nextIntFromTo(maxIndexForDishwasher+1, dishwasher_loads_day.length-1); 
			//System.out.println("newIndexForDishwasher: "+ newIndexForDishwasher);
			double newCostForDishwasher = maxValForDishwasher* dayPredictedCostSignal[newIndexForDishwasher];

			if (newCostForDishwasher < currentCostForDishwasher) {
				//System.out.println(" newCostForDishwasher < currentCostForDishwasher ");

				dishwasher_loads_day[maxIndexForDishwasher] = 0;
				dishwasher_loads_day[newIndexForDishwasher] = dishwasher_loads_day[newIndexForDishwasher] + maxValForDishwasher;

				ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_DISHWASHER), dishwasher_loads_day,timeStep % dishwasher_loads.length);
			}
			else  {
				//System.out.println(" newCostForDishWahser !< currentCostForDishwasher ");
				int iDish=newIndexForDishwasher+1;
				boolean lowerCostFoundForDishwasher = false;
				while ((iDish < dishwasher_loads_day.length) && (!lowerCostFoundForDishwasher)) {
					newCostForDishwasher = maxValForDishwasher* dayPredictedCostSignal[iDish];
					//System.out.println(iDish+" newCostForDishwahser to considered: "+newCostForDishwasher + "  "+ (newCostForDishwasher < currentCostForDishwasher));
					if (dayPredictedCostSignal[maxIndexForWasher] != dayPredictedCostSignal[iDish])
					{
						//System.out.println("Signal: "+dayPredictedCostSignal[maxIndexForDishwasher] + " != (new) "  +dayPredictedCostSignal[iDish]);
					}
					if (newCostForDishwasher < currentCostForDishwasher) {
						lowerCostFoundForDishwasher=true;
					}
					else {
						iDish++;
					}	
				}

				if (lowerCostFoundForDishwasher) {
					dishwasher_loads_day[maxIndexForDishwasher] = 0;
					dishwasher_loads_day[iDish] = washer_loads_day[iDish] + maxValForDishwasher;

					ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_DISHWASHER), dishwasher_loads_day,timeStep % dishwasher_loads.length);
					//System.out.println("lowerCostFound: newIndexForDishwasher (2nd found): "+ iDish +" new cost (2): "+ newCostForDishwasher);		
				}	
			}
		}

		owner.setWetAppliancesProfiles(wetApplianceProfiles);

		//System.out.println("AFTER washer_loads_day: "+ Arrays.toString(washer_loads_day));
		//System.out.println("AFTER dryer_loads_day: "+ Arrays.toString(dryer_loads_day));
		//System.out.println("AFTER dishwasher_loads_day: "+ Arrays.toString(dishwasher_loads_day));
		//System.out.println("==END of OptimiseWetProfile === ");	
	}

	/**
	 * Optimise the Cold appliance usage profile for the household
	 * which owns this Wattbox.
	 * 
	 */
	private void optimiseColdProfile(int timeStep) 
	{	
		//if (Consts.DEBUG) System.out.println("==OptimiseColdProfil for "+ owner.getAgentName()+"; timeStep: "+ timeStep);

		WeakHashMap<String,double[]> coldApplianceProfiles = owner.getColdAppliancesProfiles();
		double [] fridge_loads = coldApplianceProfiles.get(Consts.COLD_APP_FRIDGE_ORIGINAL);
		double [] freezer_loads = coldApplianceProfiles.get(Consts.COLD_APP_FREEZER_ORIGINAL);
		double [] fridge_freezer_loads = coldApplianceProfiles.get(Consts.COLD_APP_FRIDGEFREEZER_ORIGINAL);
		
		double [] fridge_loads_day = Arrays.copyOfRange(fridge_loads,(timeStep % fridge_loads.length) , (timeStep % fridge_loads.length) + ticksPerDay);
		double [] freezer_loads_day = Arrays.copyOfRange(freezer_loads,(timeStep % freezer_loads.length) , (timeStep % freezer_loads.length) + ticksPerDay);
		double [] fridge_freezer_loads_day = Arrays.copyOfRange(fridge_freezer_loads,(timeStep % fridge_freezer_loads.length) , (timeStep % fridge_freezer_loads.length) + ticksPerDay);

		//System.out.println("BEFORE fridge_loads: "+ Arrays.toString(fridge_loads_day));
		//System.out.println("BEFORE freezer_loads: "+ Arrays.toString(freezer_loads_day));
		//System.out.println("BEFORE fridge_freezer_loads: "+ Arrays.toString(fridge_freezer_loads_day));

		double[] currentFridgeCost = ArrayUtils.mtimes(fridge_loads_day, dayPredictedCostSignal);
		int maxIndexForFridge = ArrayUtils.indexOfMax(currentFridgeCost);
		int minIndexForFridge = ArrayUtils.indexOfMin(currentFridgeCost);

		//System.out.println("minIndexForFridge: "+ minIndexForFridge + " minVal= "+ fridge_loads_day[minIndexForFridge]);
		//System.out.println("maxIndexForFridge: "+ maxIndexForFridge + " maxVal= "+ fridge_loads_day[maxIndexForFridge]);

		if (maxIndexForFridge < (fridge_loads_day.length -1 ))
		{
			double temp = fridge_loads_day[minIndexForFridge];
			fridge_loads_day[minIndexForFridge] = fridge_loads_day[maxIndexForFridge];
			fridge_loads_day[maxIndexForFridge] = temp;
		}

		ArrayUtils.replaceRange(coldApplianceProfiles.get(Consts.COLD_APP_FRIDGE), fridge_loads_day,timeStep % fridge_loads.length);

		int timeShift = mainContext.coldAndWetApplTimeslotDelayRandDist.nextIntFromTo(1, 2); //Shift load by 1 or 2 timeslot

		double[] currentFreezerCost = ArrayUtils.mtimes(freezer_loads_day, dayPredictedCostSignal);
		int maxIndexForFreezer = ArrayUtils.indexOfMax(currentFreezerCost);

		double maxValForFeezer = freezer_loads_day[maxIndexForFreezer];

		if ((maxValForFeezer >0)  && (maxIndexForFreezer < freezer_loads_day.length-1)) {
			freezer_loads_day[maxIndexForFreezer] = 0;

			//System.out.println("max index for freezer: "+ maxIndexForFreezer + " val: "+maxValForFeezer);

			int newIndexForFreezer = maxIndexForFreezer + timeShift;
			//System.out.println("newIndex for Freezer before: "+ newIndexForFreezer);
			if (newIndexForFreezer >= freezer_loads_day.length)
				newIndexForFreezer = freezer_loads_day.length -1;

			//System.out.println("newIndexForFeezer after: "+ newIndexForFreezer);

			freezer_loads_day[newIndexForFreezer] = freezer_loads_day[newIndexForFreezer] + maxValForFeezer;

			ArrayUtils.replaceRange(coldApplianceProfiles.get(Consts.COLD_APP_FREEZER), freezer_loads_day,timeStep % freezer_loads.length);
		}

		//-------------

		double[] currentFridgeFreezerCost = ArrayUtils.mtimes(fridge_freezer_loads_day, dayPredictedCostSignal);
		int maxIndexForFridgeFreezer = ArrayUtils.indexOfMax(currentFridgeFreezerCost);

		double maxValForFridgeFeezer = fridge_freezer_loads_day[maxIndexForFridgeFreezer];

		if ((maxValForFridgeFeezer >0)  && (maxIndexForFridgeFreezer < fridge_freezer_loads_day.length -1) ) {

			fridge_freezer_loads_day[maxIndexForFridgeFreezer] = 0;

			//System.out.println("max index for FridgeFreezer: "+ maxIndexForFridgeFreezer + " val: "+maxValForFridgeFeezer);

			int newIndexForFridgeFreezer = maxIndexForFridgeFreezer + timeShift;
			//System.out.println("newIndexForFridgeFreezer before: "+ newIndexForFridgeFreezer);
			if (newIndexForFridgeFreezer >= fridge_freezer_loads_day.length)
				newIndexForFridgeFreezer = fridge_freezer_loads_day.length -1;

			//System.out.println("newIndexForFridgeFreezer after: "+ newIndexForFridgeFreezer);

			fridge_freezer_loads_day[newIndexForFridgeFreezer] = fridge_freezer_loads_day[newIndexForFridgeFreezer] + maxValForFridgeFeezer;

			ArrayUtils.replaceRange(coldApplianceProfiles.get(Consts.COLD_APP_FRIDGEFREEZER), fridge_freezer_loads_day,timeStep % fridge_freezer_loads.length);
		}

		owner.setColdAppliancesProfiles(coldApplianceProfiles);

		//System.out.println("AFTER fridge_loads: "+ Arrays.toString(fridge_loads_day));
		//System.out.println("AFTER freezer_loads: "+ Arrays.toString(freezer_loads_day));
		//System.out.println("AFTER fridge_freezer_loads: "+ Arrays.toString(fridge_freezer_loads_day));
		//System.out.println("==END of OptimiseColdProfile === ");
	}

	/**
	 * @param dayPredictedCostSignal the dayPredictedCostSignal to set
	 */
	public void setDayPredictedCostSignal(double[] dayPredictedCostSignal) 
	{
		this.dayPredictedCostSignal = dayPredictedCostSignal;
	}

	/**
	 * Finds an optimised set point profile for the day ahead 
	 * based on the projected temperature
	 * and therefore predicted heat pump demand.  This is combined with the 
	 * signal (sent by the aggregator) as a proxy for predicted cost of 
	 * electricity for the day ahead to produce the estimated cost of the 
	 * switching profile.
	 * 
	 * Subject to constraints that 
	 * <ul><li> temperature drop in switch off periods must not exceed a certain amount.
	 * <li> heat pump may be switched off for only one contiguous period per day
	 * <li> the "switch off period" may be between half an hour and four hours</ul>
	 *  
	 * Currently uses a "brute force" algorithm to evaluate all profiles
	 * It uses the profile which minimises the predicted cost of operation.
	 * 
	 */
	void optimiseSetPointProfile()
	{ 		
		if (Consts.DEBUG)
		{
			System.out.println("WattboxController:: optimise set point called for agent " + owner.getAgentName());
		}
		//Initialise optimisation
		double[] localSetPointArray = Arrays.copyOf(setPointProfile, setPointProfile.length);
		this.optimisedSetPointProfile = Arrays.copyOf(setPointProfile, setPointProfile.length);
		double[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));
		double[] localDemandProfile = calculateEstimatedSpaceHeatPumpDemand(setPointProfile);
		double leastCost = evaluateCost(localDemandProfile);
		double newCost = leastCost;
		double maxRecoveryPerTick = 0.5d * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) * (Consts.SECONDS_PER_DAY / ticksPerDay) * ArrayUtils.max(deltaT)) ; // i.e. can't recover more than 50% of heat loss at 90% COP.  TODO: Need to code this better later

		for (int i = 0; i < localSetPointArray.length; i++)
		{
			//Start each evaluation from the basepoint of the original (user specified) set point profile
			localSetPointArray = Arrays.copyOf(setPointProfile, setPointProfile.length);
			double totalTempLoss = 0;
			double[] otherPrices = Arrays.copyOf(this.dayPredictedCostSignal, this.dayPredictedCostSignal.length);

			for ( int j = 0; (j < Consts.HEAT_PUMP_MAX_SWITCHOFF && (i+j < ticksPerDay)); j++)
			{
				double tempLoss = (((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) * (Consts.SECONDS_PER_DAY / ticksPerDay) * Math.max(0,(localSetPointArray[i+j] - priorDayExternalTempProfile[i+j]))) / owner.buildingThermalMass);
				//System.out.println("Temp loss in tick " + (i + j) + " = " + tempLoss);
				totalTempLoss += tempLoss;						

				for (int k = i+j; k < localSetPointArray.length; k++)
				{
					localSetPointArray[k] = this.setPointProfile[k] - totalTempLoss;
					//availableHeatRecoveryTicks++;
				}
				double availableHeatRecoveryTicks = localSetPointArray.length - j;

				//Sort out where to regain the temperature (if possible)
				int n = (int) Math.ceil((totalTempLoss * owner.buildingThermalMass) / maxRecoveryPerTick);
				// Take this slot out of the potential cheap slots to recover temp in.
				otherPrices[i+j] = Double.POSITIVE_INFINITY;

				if (n < availableHeatRecoveryTicks && n > 0)
				{
					//We know it's possible to recover the temperature lost
					//in switch off period under the constraints set.
					double tempToRecover = (totalTempLoss / (double) n);

					//Find the cheapest timeslots in which to recover the temperature
					//If this selection results in a tie, the slot is chosen
					//randomly
					int[] recoveryIndices = ArrayUtils.findNSmallestIndices(otherPrices,n);

					//Add on temperature in each temperature recovery slot and
					//all subsequent slots - thus building an optimised profile.
					for (int l : recoveryIndices)
					{
						for (int m = l; m < ticksPerDay; m++)
						{
							localSetPointArray[m] += tempToRecover;
							//System.out.println("Adding " + tempToRecover + " at index " + m + " to counter temp loss at tick " + (i+j+1));
						}
						if ((i+j) == 0 && Consts.DEBUG)
						{
							System.out.println("Evaluating switchoff for tick zero, set point array = " + Arrays.toString(localSetPointArray));
						}
						//System.out.println("In here, adding temp " + tempToRecover + " from index " + l);
						//System.out.println("With result " + Arrays.toString(localSetPointArray));
					}

					double[] tempDifference = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(localSetPointArray));


					if (ArrayUtils.max(ArrayUtils.add(ArrayUtils.absoluteValues(tempDifference), ArrayUtils.negate(Consts.MAX_PERMITTED_TEMP_DROPS))) > Consts.FLOATING_POINT_TOLERANCE)
					{
						//if the temperature drop, or rise, is too great, this profile is unfeasible and we return null
					}
					else
					{
						//calculate energy implications and cost for this candidate setPointProfile
						localDemandProfile = calculateEstimatedSpaceHeatPumpDemand(localSetPointArray);
						if (owner.getAgentID() == 0)//TESTTTESTTEST((i+j) == 0 && Consts.DEBUG)
						{
							System.out.println("Calculated demand for set point array turning off at tick " + i + " for " + (j+1) + " ticks " + Arrays.toString(localSetPointArray));
							System.out.println("Demand = " + Arrays.toString(localDemandProfile));
						}
						if (localDemandProfile != null)
						{
							//in here if the set point profile is achievable
							newCost = evaluateCost(localDemandProfile);

							if (owner.getAgentID() == 0)//TESTTTESTTEST
							{
								System.out.println(newCost + " vs. " + leastCost);

							}

							//Decide whether to swap the new profile with the current best one
							//based on cost.
							//Many strategies are available - here we give two options
							//Either the new cost must be better by an amount greater than some decision threshold
							//held in Consts.COST_DECISION_THRESHOLD
							// OR (currently used) the cost must simply be better, with a tie in cost
							// being decided by a "coin toss".

							//if((newCost - leastCost) < (0 - Consts.COST_DECISION_THRESHOLD))
							if (newCost < leastCost || (newCost == leastCost && RandomHelper.nextIntFromTo(0,1) == 1))
							{
								leastCost = newCost;
								this.optimisedSetPointProfile = Arrays.copyOf(localSetPointArray, localSetPointArray.length);
								this.heatPumpDemandProfile = ArrayUtils.multiply(localDemandProfile, (1/Consts.DOMESTIC_HEAT_PUMP_SPACE_COP));
							}
						}
						else
						{
							//Impossible to recover heat within heat pump limits - discard this attempt.
							if (owner.getAgentID() == 1)
							{System.err.println("WattboxController: Can't recover heat with " + availableHeatRecoveryTicks + " ticks, need " + n);}
						}
					}
				}
			}
		}

		this.expectedNextDaySpaceHeatCost = leastCost;
	}

	/**
	 * Calculates the estimated demand caused by a given set point array
	 * 
	 * @param localSetPointArray - an array containing the set point profile for which the estimated demand should be calculated
	 * @return the estimated demand profile or null if the set point profile is not physically achievable
	 */
	private double[] calculateEstimatedSpaceHeatPumpDemand(double[] localSetPointArray) {
		double[] energyProfile = new double[ticksPerDay];
		double[] deltaT = ArrayUtils.add(localSetPointArray, ArrayUtils.negate(priorDayExternalTempProfile));
		//int availableHeatRecoveryTicks = ticksPerDay;
		double maxRecoveryPerTick = 0.5d * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE; // i.e. can't recover more than 50% of heat loss at 90% COP.  TODO: Need to code this better later
		//double internalTemp = this.setPointProfile[0];

		for (int i = 0; i < ticksPerDay; i++)
		{
			//--availableHeatRecoveryTicks;
			//currentTempProfile[i] = internalTemp;
			double tempChange;

			if (i > 0)
			{
				tempChange = localSetPointArray[i] - localSetPointArray[i - 1];
			}
			else
			{
				tempChange = localSetPointArray[i] - this.setPointProfile[0] ;
			}

			//double setPointMaintenanceEnergy = deltaT[i] * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR)) * (Consts.SECONDS_PER_DAY / ticksPerDay);
			double setPointMaintenanceEnergy = (this.setPointProfile[i] - priorDayExternalTempProfile[i]) * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR)) * (Consts.SECONDS_PER_DAY / ticksPerDay);

			//tempChangePower can be -ve if the temperature is falling.  If tempChangePower magnitude
			//is greater than or equal to setPointMaintenance, the heat pump is off.
			double tempChangeEnergy = tempChange * owner.buildingThermalMass;

			// Although the temperature profiles supplied should be such that the heat
			// can always be recovered within a reasonable cap - we could put a double
			// check in here.
			/*				if (tempChangeEnergy > maxRecoveryPerTick * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR) * (Consts.SECONDS_PER_DAY / ticksPerDay) * ArrayUtils.max(deltaT)))
				{
					//System.err.println("WattboxController: Should never get here - asked to get demand for a profile that can't recover temp");
					return null;
				}*/

			//Add in the energy to maintain the new temperature, otherwise it can be cheaper
			//To let the temperature fall and re-heat under flat price and external temperature
			//conditions.  TODO: Is this a good physical analogue?
			//setPointMaintenanceEnergy += tempChange * ((owner.buildingHeatLossRate / Consts.KWH_TO_JOULE_CONVERSION_FACTOR)) * (Consts.SECONDS_PER_DAY / ticksPerDay);

			double heatPumpEnergyNeeded = Math.max(0, (setPointMaintenanceEnergy + tempChangeEnergy) / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP);
			//double heatPumpEnergyNeeded = (setPointMaintenanceEnergy / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) + (tempChangeEnergy * (Consts.DOMESTIC_HEAT_PUMP_SPACE_COP * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE));


			//zero the energy if heat pump would be off
			if(deltaT[i] < Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF)
			{
				//heat pump control algorithm would switch off pump
				heatPumpEnergyNeeded = 0;
			}

			if (heatPumpEnergyNeeded > (owner.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP * 24 / ticksPerDay))
			{
				//This profiel produces a value that exceeds the total capacity of the
				//heat pump and is therefore unachievable.
				//System.out.println("Nulling the demand profile for energy needed " + heatPumpEnergyNeeded );
				//Can't satisfy this demand for this set point profile, return null
				return null;
			}

			energyProfile[i] = heatPumpEnergyNeeded;
		}

		return energyProfile;

	}

	/**
	 * Estimates the cost of a given demand profile fiven the current predicted 
	 * costs for the following day.
	 * 
	 * @param localDemandProfile the demand profile array to be evaluated
	 * @return the cost of the demand profile at the current predicted costs per tick
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
	public WattboxController(HouseholdProsumer owner) 
	{
		super();

		this.owner = owner;
		ticksPerDay = owner.getContext().getNbOfTickPerDay();
		//this.priorDayExternalTempProfile = Arrays.copyOf(INITIALIZATION_TEMPS, INITIALIZATION_TEMPS.length);
		//Initialise with a flat external temperature - thus no incentive to move demand on first day of use.
		this.priorDayExternalTempProfile = new double[ticksPerDay];
		Arrays.fill(priorDayExternalTempProfile, Consts.INITIALISATION_EXTERNAL_TEMP);

		if (owner.getHasElectricalSpaceHeat())
			this.heatPumpOnOffProfile = Arrays.copyOf(owner.spaceHeatPumpOn,owner.spaceHeatPumpOn.length);

		if (owner.getHasElectricalWaterHeat())
			this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.getBaselineHotWaterVolumeProfile(),((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.getBaselineHotWaterVolumeProfile().length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.getBaselineHotWaterVolumeProfile().length) + ticksPerDay);

		this.maxHeatPumpElecDemandPerTick = (owner.ratedPowerHeatPump * (double) 24 / ticksPerDay);
		this.maxImmersionHeatPerTick = Consts.MAX_DOMESTIC_IMMERSION_POWER * (double) 24 / ticksPerDay;

	}

	public WattboxController(HouseholdProsumer owner, CascadeContext context) {
		this(owner);
		this.mainContext = context;

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
		this(owner);
		this.userProfile = userProfile;
		this.userLifestyle = userLifestyle;

	}
}
