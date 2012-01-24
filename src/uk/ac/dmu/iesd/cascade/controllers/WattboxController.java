/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.*;

import cern.jet.random.Uniform;

import repast.simphony.context.Context;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;

import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
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
	//double[] coldApplianceProfile;
	//double[] wetApplianceProfile;
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
		double[] ownersCostSignal = owner.getPredictedCostSignal();
		this.dayPredictedCostSignal = Arrays.copyOfRange(ownersCostSignal, timeStep % ownersCostSignal.length, timeStep % ownersCostSignal.length + ticksPerDay);
		
		//System.out.println("update");

		//System.out.println("dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));

		this.dayPredictedCostSignal = ArrayUtils.offset(ArrayUtils.multiply(this.dayPredictedCostSignal, predictedCostToRealCostA),realCostOffsetb);
		
		//System.out.println("afterOffset dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));

		if (owner.getHasElectricalSpaceHeat()) {
			this.setPointProfile = Arrays.copyOf(owner.getSetPointProfile(), owner.getSetPointProfile().length);
			this.optimisedSetPointProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
			this.currentTempProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
		}
		
		//if (owner.getHasColdAppliances())
			//this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,(timeStep % owner.coldApplianceProfile.length) , (timeStep % owner.coldApplianceProfile.length) + ticksPerDay);
		
		//if (owner.getHasWetAppliances())
			//this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,(timeStep % owner.wetApplianceProfile.length), (timeStep % owner.wetApplianceProfile.length) + ticksPerDay);

		if (owner.getHasElectricalWaterHeat())
			this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.getBaselineHotWaterVolumeProfile(),(timeStep % owner.getBaselineHotWaterVolumeProfile().length), (timeStep % owner.getBaselineHotWaterVolumeProfile().length) + ticksPerDay);
		
		if (owner.getHasElectricalSpaceHeat())
		{
			this.heatPumpDemandProfile = calculateSpaceHeatPumpDemand(this.setPointProfile);
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
			//optimiseSpaceHeatProfile();
			optimiseSetPointProfile();
			//System.out.println(Arrays.toString(this.heatPumpOnOffProfile));
		}	

		if (waterHeatingControlled && owner.getHasElectricalWaterHeat())
		{
			//optimiseWaterHeatProfile();
			optimiseWaterHeatProfileWithSpreading();
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
		if(Consts.DEBUG)
		{
			
			/*System.out.println("&&&&&& WattboxController:: putting arrays into the return map and returning to caller");
			if (this.coldAppliancesControlled && owner.getHasColdAppliances()) {
			  System.out.println("ColdProfile.length: "+ coldApplianceProfile.length);
			  System.out.println("ColdProfile: "+ Arrays.toString(coldApplianceProfile));
			}
			if (this.waterHeatingControlled && owner.getHasWetAppliances()) {
			    System.out.println("WetProfile.length: "+ wetApplianceProfile.length);
			   System.out.println("WetProfile: "+ Arrays.toString(wetApplianceProfile));
		    } */
			//System.out.println("WaterHeatProfile.length: "+ waterHeatDemandProfile.length);
			//System.out.println("WaterHeatProfile: "+ Arrays.toString(waterHeatDemandProfile));
			//System.out.println("HeatPumpProfile.length: "+ optimisedSetPointProfile.length);
			//System.out.println("HeatPupProfile: "+ Arrays.toString(optimisedSetPointProfile));

		}
		returnMap.put("HeatPump", optimisedSetPointProfile);
		
		//if (this.coldAppliancesControlled && owner.getHasColdAppliances())
			//returnMap.put("ColdApps", coldApplianceProfile);
		
		//if (this.waterHeatingControlled && owner.getHasWetAppliances())
			//returnMap.put("WetApps", wetApplianceProfile);
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
		
		System.out.println("==OptimiseWaterHeatProfil for a  "+ owner.getAgentID());

		double[] baseArray = ArrayUtils.multiply(this.hotWaterVolumeDemandProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (owner.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP) );
		System.out.println("hotWaterVolumeDemandProfile: "+ Arrays.toString(hotWaterVolumeDemandProfile));
		System.out.println("baseArray:              "+ Arrays.toString(baseArray)); //=waterHeatProfile

		this.waterHeatDemandProfile = Arrays.copyOf(baseArray, baseArray.length);
		System.out.println("waterHeatDemandProfile: "+ Arrays.toString(waterHeatDemandProfile));
		
		System.out.println("spreadWaterDemand(baseArray) : "+ Arrays.toString(spreadWaterDemand(baseArray)));

		System.out.println("spreadWaterDemand(baseArray)2: "+ Arrays.toString(spreadWaterDemand(baseArray)));

		double[] totalHeatDemand = ArrayUtils.add(this.heatPumpDemandProfile, spreadWaterDemand(baseArray));
		
		System.out.println("totalHeatDemand: "+ Arrays.toString(totalHeatDemand));

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


	/**
	 * Optimise the Wet appliance usage profile for the household
	 * which owns this Wattbox.
	 * 
	 */
	/*private void optimiseWetProfile_old() 
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

	} */
	
	/*
	private void optimiseWetProfile() 
	{
		
		//System.out.println("WetProf length: "+ wetApplianceProfile.length);
		//System.out.println("WetProf Before Optim: "+ Arrays.toString(wetApplianceProfile));
		//System.out.println("dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));
    
		double[] currentCost = ArrayUtils.mtimes(wetApplianceProfile, dayPredictedCostSignal);
		int maxIndex = ArrayUtils.indexOfMax(currentCost);
		
		double maxVal = wetApplianceProfile[maxIndex];
		
		wetApplianceProfile[maxIndex] = 0;
		
		int timeShift = 8; //temporary test: delay 4 hours 
		
		int newIndex = maxIndex + timeShift;
		//System.out.println("newIndex before: "+ newIndex);
		if (newIndex >= wetApplianceProfile.length)
			newIndex = wetApplianceProfile.length -1;
		
		//System.out.println("newIndex after: "+ newIndex);

		wetApplianceProfile[newIndex] = wetApplianceProfile[newIndex] + maxVal;
		
		//System.out.println("Wet Prof AFTER Optim: "+ Arrays.toString(wetApplianceProfile));

	} */
	
	
	private void optimiseWetProfile(int timeStep) 
	{
		System.out.println("==OptimiseWetProfil for a  "+ owner.getAgentID()+"; timeStep: "+ timeStep);
		System.out.println("dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));

		
		HashMap wetApplianceProfiles = owner.getWetAppliancesProfiles();
		double [] washer_loads = (double []) wetApplianceProfiles.get(Consts.WET_APP_WASHER);
		double [] dryer_loads = (double []) wetApplianceProfiles.get(Consts.WET_APP_DRYER);
		double [] dishwasher_loads = (double []) wetApplianceProfiles.get(Consts.WET_APP_DISHWASHER);
	
		double [] washer_loads_day = Arrays.copyOfRange(washer_loads,(timeStep % washer_loads.length) , (timeStep % washer_loads.length) + ticksPerDay);
		double [] dryer_loads_day = Arrays.copyOfRange(dryer_loads,(timeStep % dryer_loads.length) , (timeStep % dryer_loads.length) + ticksPerDay);
		double [] dishwasher_loads_day = Arrays.copyOfRange(dishwasher_loads,(timeStep % dishwasher_loads.length) , (timeStep % dishwasher_loads.length) + ticksPerDay);
		
		System.out.println("BEFORE washer_loads_day: "+ Arrays.toString(washer_loads_day));
		System.out.println("BEFORE dryer_loads_day: "+ Arrays.toString(dryer_loads_day));
		System.out.println("BEFORE dishwasher_loads_day: "+ Arrays.toString(dishwasher_loads_day));
		
		double[] currentWasherCost = ArrayUtils.mtimes(washer_loads_day, dayPredictedCostSignal);
		
		System.out.println("currentWasherCost: "+ Arrays.toString(currentWasherCost));

		int maxIndexForWasher = ArrayUtils.indexOfMax(currentWasherCost);
        double maxValForWasher = washer_loads_day[maxIndexForWasher];
        
        if ((maxValForWasher > 0) && (maxIndexForWasher < washer_loads_day.length-1)) {
        	
           	System.out.println("max index for Wahser: "+ maxIndexForWasher + " val: "+maxValForWasher);
    		int newIndexForWasher = mainContext.coldAndWetApplTimeslotDelayRandDist.nextIntFromTo(maxIndexForWasher+1, washer_loads_day.length-1);
        	System.out.println("newIndexForWasher: "+ newIndexForWasher);
        	
        	double newValueForWahser = maxValForWasher* dayPredictedCostSignal[newIndexForWasher];
        	
        	if (newValueForWahser < maxValForWasher) {
        		washer_loads_day[maxIndexForWasher] = 0;
            	washer_loads_day[newIndexForWasher] = washer_loads_day[newIndexForWasher] + maxValForWasher;
                  	
            	ArrayUtils.replaceRange(washer_loads, washer_loads_day,timeStep % washer_loads.length);
            	wetApplianceProfiles.put(Consts.WET_APP_WASHER, washer_loads);
        		
        	}
        	else  {
        		int ind=newIndexForWasher+1;
        		boolean lowerCostFound = false;
        		while ((ind < washer_loads_day.length-1) && (!lowerCostFound)) {
        			newValueForWahser = maxValForWasher* dayPredictedCostSignal[ind];
        			if (newValueForWahser < maxValForWasher) {
        				lowerCostFound=true;
        			}
        			else {
        				ind++;
        			}	
        		}
        		
        		if (lowerCostFound) {
        			washer_loads_day[maxIndexForWasher] = 0;
                	washer_loads_day[newIndexForWasher] = washer_loads_day[newIndexForWasher] + maxValForWasher;
                      	
                	ArrayUtils.replaceRange(washer_loads, washer_loads_day,timeStep % washer_loads.length);
                	wetApplianceProfiles.put(Consts.WET_APP_WASHER, washer_loads);
            		
        		}
        		
        	}
        
        }
   
        /*
		double[] currentDryerCost = ArrayUtils.mtimes(dryer_loads_day, dayPredictedCostSignal);
		int maxIndexForDryer = ArrayUtils.indexOfMax(currentDryerCost);
        double maxValForDryer = dryer_loads_day[maxIndexForDryer];
        
        if ((maxValForDryer > 0) && (maxIndexForDryer < dryer_loads_day.length-1)) {
        	
        	dryer_loads_day[maxIndexForDryer] = 0;

        	System.out.println("max index for Dryer: "+ maxIndexForDryer + " val: "+maxValForDryer);
    		int newIndexForDryer = mainContext.coldAndWetApplTimeslotDelayRandDist.nextIntFromTo(maxIndexForDryer+1, dryer_loads_day.length-1); 
        	System.out.println("newIndexForDryer: "+ newIndexForDryer);
        	dryer_loads_day[newIndexForDryer] = dryer_loads_day[newIndexForDryer] + maxValForDryer;
        	
        	ArrayUtils.replaceRange(dryer_loads, dryer_loads_day,timeStep % dryer_loads.length);
        	wetApplianceProfiles.put(Consts.WET_APP_DRYER, dryer_loads);
        }
        
		double[] currentDishwasherCost = ArrayUtils.mtimes(dishwasher_loads_day, dayPredictedCostSignal);
		int maxIndexForDishwasher = ArrayUtils.indexOfMax(currentDishwasherCost);
        double maxValForDishwasher = dishwasher_loads_day[maxIndexForDishwasher];
        
        if ((maxValForDishwasher > 0) && (maxIndexForDishwasher < dishwasher_loads_day.length-1)) {
        	
        	dishwasher_loads_day[maxIndexForDishwasher] = 0;

        	System.out.println("max index for Dishwasher: "+ maxIndexForDishwasher + " val: "+maxValForDishwasher);
    		int newIndexForDishwasher = mainContext.coldAndWetApplTimeslotDelayRandDist.nextIntFromTo(maxIndexForDishwasher+1, dishwasher_loads_day.length-1); 

        	System.out.println("newIndexForDishwasher: "+ newIndexForDishwasher);
        	dishwasher_loads_day[newIndexForDishwasher] = dishwasher_loads_day[newIndexForDishwasher] + maxValForDishwasher;
        	
        	ArrayUtils.replaceRange(dishwasher_loads, dishwasher_loads_day,timeStep % dishwasher_loads.length);
        	wetApplianceProfiles.put(Consts.WET_APP_DISHWASHER, dishwasher_loads);
        }
        
        */
        
		owner.setWetAppliancesProfiles(wetApplianceProfiles);
		
		System.out.println("AFTER washer_loads_day: "+ Arrays.toString(washer_loads_day));
		System.out.println("AFTER dryer_loads_day: "+ Arrays.toString(dryer_loads_day));
		System.out.println("AFTER dishwasher_loads_day: "+ Arrays.toString(dishwasher_loads_day));
		System.out.println("==END of OptimiseWetProfile === ");
		
	}

	/**
	 * Optimise the Cold appliance usage profile for the household
	 * which owns this Wattbox.
	 * 
	 */
	
	/*
	private void optimiseColdProfile_Old() 
	{
		
		System.out.println("ColdProf.length: "+ coldApplianceProfile.length);
		System.out.println("ColdProf Before Optim: "+ Arrays.toString(coldApplianceProfile));
		System.out.println("dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));

		double[] currentCost = ArrayUtils.mtimes(coldApplianceProfile, dayPredictedCostSignal);
		int maxIndex = ArrayUtils.indexOfMax(currentCost);
		int minIndex = ArrayUtils.indexOfMin(currentCost);
		
        System.out.println("minIndex: "+ minIndex + " minVal= "+ coldApplianceProfile[minIndex]);
		System.out.println("maxIndex: "+ maxIndex + " maxVal= "+ coldApplianceProfile[maxIndex]);


		// First pass - simply swap the load in the max cost slot with that in the min cost slot
		//TODO: very crude and will give nasty positive feedback in all likelihood
		if (maxIndex < (coldApplianceProfile.length -1 ))
		{
			double temp = coldApplianceProfile[minIndex];
			coldApplianceProfile[minIndex] = coldApplianceProfile[maxIndex];
			coldApplianceProfile[maxIndex] = temp;
		}
	}  */
	
	/**
	 * Optimise the Cold appliance usage profile for the household
	 * which owns this Wattbox.
	 * 
	 */
	private void optimiseColdProfile(int timeStep) 
	{	
		//System.out.println("==OptimiseColdProfil for a  "+ owner.getAgentID()+"; timeStep: "+ timeStep);
		
		HashMap coldApplianceProfiles = owner.getColdAppliancesProfiles();
		double [] fridge_loads = (double []) coldApplianceProfiles.get(Consts.COLD_APP_FRIDGE);
		double [] freezer_loads = (double []) coldApplianceProfiles.get(Consts.COLD_APP_FREEZER);
		double [] fridge_freezer_loads = (double []) coldApplianceProfiles.get(Consts.COLD_APP_FRIDGEFREEZER);
	
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
		
		ArrayUtils.replaceRange(fridge_loads, fridge_loads_day,timeStep % fridge_loads.length);
		coldApplianceProfiles.put(Consts.COLD_APP_FRIDGE, fridge_loads);
		
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
        	
    		ArrayUtils.replaceRange(freezer_loads, freezer_loads_day,timeStep % freezer_loads.length);
    		
    		coldApplianceProfiles.put(Consts.COLD_APP_FREEZER, freezer_loads);

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
        	
    		ArrayUtils.replaceRange(fridge_freezer_loads, fridge_freezer_loads_day,timeStep % fridge_freezer_loads.length);
    		coldApplianceProfiles.put(Consts.COLD_APP_FRIDGEFREEZER, fridge_freezer_loads);
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
		if (Consts.DEBUG)
		{
			System.out.println("WattboxController:: optimise set point called for agent " + owner.getAgentName());
		}
		//Initialise optimisation
		double[] localSetPointArray = Arrays.copyOf(setPointProfile, setPointProfile.length);
		this.optimisedSetPointProfile = Arrays.copyOf(setPointProfile, setPointProfile.length);
		double[] deltaT = ArrayUtils.add(this.setPointProfile, ArrayUtils.negate(priorDayExternalTempProfile));
		double[] localDemandProfile = calculateSpaceHeatPumpDemand(setPointProfile);
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
					double tempToRecover = (totalTempLoss / (double) n);
					//It's possible to recover the temperature
					
					int[] recoveryIndices = ArrayUtils.findNSmallestIndices(otherPrices,n);

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
						localDemandProfile = calculateSpaceHeatPumpDemand(localSetPointArray);
						if ((i+j) == 0 && Consts.DEBUG)
						{
							System.out.println("Calculated demand for set point array = " + Arrays.toString(localSetPointArray));
							System.out.println("Demand = " + Arrays.toString(localDemandProfile));
						}
						if (localDemandProfile != null)
						{
							//in here if the set point profile is achievable
							newCost = evaluateCost(localDemandProfile);
							
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
	 * @param localSetPointArray
	 * @return
	 */
	private double[] calculateSpaceHeatPumpDemand(double[] localSetPointArray) {
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
	public WattboxController(HouseholdProsumer owner) 
	{
		this.owner = owner;
		ticksPerDay = owner.getContext().getNbOfTickPerDay();
		//this.priorDayExternalTempProfile = Arrays.copyOf(INITIALIZATION_TEMPS, INITIALIZATION_TEMPS.length);
		//Initialise with a flat external temperature - thus no incentive to move demand on first day of use.
		this.priorDayExternalTempProfile = new double[ticksPerDay];
		Arrays.fill(priorDayExternalTempProfile, Double.parseDouble("7"));  //TODO: Richard will declare this number as Const.
		
		if (owner.getHasElectricalSpaceHeat())
			this.heatPumpOnOffProfile = Arrays.copyOf(owner.spaceHeatPumpOn,owner.spaceHeatPumpOn.length);
		//this.heatPumpDemandProfile = new double[ticksPerDay];
		//double [] prosumerBaselineHotwaterVolProfile = owner.getBaselineHotWaterVolumeProfile();
		
		if (owner.getHasElectricalWaterHeat())
			this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.getBaselineHotWaterVolumeProfile(),((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.getBaselineHotWaterVolumeProfile().length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.getBaselineHotWaterVolumeProfile().length) + ticksPerDay);
		//this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(prosumerBaselineHotwaterVolProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % prosumerBaselineHotwaterVolProfile.length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % prosumerBaselineHotwaterVolProfile.length) + ticksPerDay);


		//if(owner.coldApplianceProfile != null)
		//{
			//this.coldApplianceProfile = Arrays.copyOfRange(owner.coldApplianceProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.coldApplianceProfile.length) , ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.coldApplianceProfile.length) + ticksPerDay);
		//}
		
		//if (owner.wetApplianceProfile != null)
		//{
			//this.wetApplianceProfile = Arrays.copyOfRange(owner.wetApplianceProfile,((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.wetApplianceProfile.length), ((Math.max(0, (int)RepastEssentials.GetTickCount())) % owner.wetApplianceProfile.length) + ticksPerDay);
		//}

		this.maxHeatPumpElecDemandPerTick = (owner.ratedPowerHeatPump * (double) 24 / ticksPerDay);
		this.maxImmersionHeatPerTick = Consts.MAX_DOMESTIC_IMMERSION_POWER * (double) 24 / ticksPerDay;
		
        //coldAndWetApplTimeslotDelayRandDist = RandomHelper.createUniform();  //temp
		
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
		super();
		this.owner = owner;
		this.userProfile = userProfile;
		this.userLifestyle = userLifestyle;
		
	}
}
