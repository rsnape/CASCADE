package uk.ac.dmu.iesd.cascade.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.WeakHashMap;

import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * This class implements an abstracted version of the "Wattbox" Smart controller
 * developed by Dr. Peter Boait
 * 
 * The Wattbox learned variable is occupancy, with the controlled variable being
 * temperature set point. Set point is determined by an optimisation algorithm
 * based upon an input signal and projected load for the following day
 * 
 * @author jrsnape
 * 
 *         Version history (for intermediate steps see Git repository history) -
 *         Used Modified to receive and treate separate related loads for
 *         composite wet and cold (by Babak Mahdavi)
 * 
 */
public class OriginalProportionalWattboxController implements ISmartController
{

	static final double[] INITIALIZATION_TEMPS = { 7, 7, 7, 7, 7, 7, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 17, 17, 16, 16, 15, 15, 14, 14, 12, 12, 10, 10, 8, 8, 7, 7 };
	HouseholdProsumer owner;
	WattboxUserProfile userProfile;
	WattboxLifestyle userLifestyle;
	int ticksPerDay;

	double[] dayPredictedCostSignal;
	// Should always have b > A, otherwise there can be zero or negative cost to
	// consumption
	// which often makes the optimisation algorithms "spike" the consumption at
	// that point.
	double predictedCostToRealCostA = 9;
	double realCostOffsetb = 10;
	double[] setPointProfile;
	double[] optimisedSetPointProfile;
	double[] currentTempProfile;

	// Prior day's temperature profile. Works on the principle that
	// in terms of temperature, today is likely to be similar to yesterday
	double[] priorDayExternalTempProfile;
	double[] heatPumpDemandProfile;
	double[] hotWaterVolumeDemandProfile;

	double[] EVChargingProfile;

	double[] heatPumpOnOffProfile;

	protected CascadeContext mainContext;

	/**
	 * Boolean configuration options for what elements of the owner's electrical
	 * demand are controlled by the Wattbox.
	 * 
	 * Options are cold appliances, wet appliances, water heating, space heating
	 * and electric vehicles. All default to true and should be explicitly set
	 * false if required by the Wattbox owner.
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
	private double[] noElecHeatingDemand = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	// Uniform coldAndWetApplTimeslotDelayRandDist; //temp

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
		/**** TODO: Be aware of below - means signal goes neg and pos ****/
		// this.dayPredictedCostSignal =
		// ArrayUtils.offset(ArrayUtils.multiply(this.dayPredictedCostSignal,
		// predictedCostToRealCostA),realCostOffsetb);

		if (owner.isHasElectricalSpaceHeat())
		{
			this.setPointProfile = Arrays.copyOf(owner.getSetPointProfile(), owner.getSetPointProfile().length);
			this.optimisedSetPointProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
			this.currentTempProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
			this.heatPumpDemandProfile = calculateEstimatedSpaceHeatPumpDemand(this.setPointProfile);
		} else
		{
			this.heatPumpDemandProfile = Arrays.copyOf(noElecHeatingDemand, noElecHeatingDemand.length);
		}

		if (owner.isHasElectricalWaterHeat())
		{
			this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.getBaselineHotWaterVolumeProfile(), (timeStep % owner.getBaselineHotWaterVolumeProfile().length), (timeStep % owner.getBaselineHotWaterVolumeProfile().length) + ticksPerDay);
			this.waterHeatDemandProfile = ArrayUtils.multiply(hotWaterVolumeDemandProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (owner.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP));
		} else
		{
			this.waterHeatDemandProfile = Arrays.copyOf(noElecHeatingDemand, noElecHeatingDemand.length);
		}

		if (coldAppliancesControlled && owner.isHasColdAppliances())
		{
			gapColdLoadProbabilistic(timeStep);
		}

		if (wetAppliancesControlled && owner.isHasWetAppliances())
		{
			optimiseWetProfileProbabilistic(timeStep);
		}

		// Note - optimise space heating first. This is so that we can look for
		// absolute
		// heat pump limit and add the cost of using immersion heater (COP 0.9)
		// to top
		// up water heating if the heat pump is too great
		if (spaceHeatingControlled && owner.isHasElectricalSpaceHeat())
		{
			// optimiseSetPointProfile();
			assignSetPointsProbabilistic();
			/*if (Consts.DEBUG)
			{
				this.owner.mainContext.logger.debug("Optimised set point profile = " + Arrays.toString(this.setPointProfile));
			}*/
		}

		if (waterHeatingControlled && owner.isHasElectricalWaterHeat())
		{
			placeWaterHeatingProportional();
		}

		if (eVehicleControlled && owner.isHasElectricVehicle())
		{
			optimiseEVProfile();
		}

		// At the end of the step, set the temperature profile for today's
		// (which will be yesterday's when it is used)
		this.priorDayExternalTempProfile = owner.getContext().getAirTemperature(timeStep, ticksPerDay);
	}

	/**
	 * @param timeStep
	 */
	private void gapColdLoadProbabilistic(int timeStep)
	{
		if (ArrayUtils.max(this.dayPredictedCostSignal) == 0 && ArrayUtils.min(this.dayPredictedCostSignal) == 0)
		{
			// No point doing anything if the cost signal is null (flat == all
			// zeros)
			return;
		}

		int Nt = 1;// The maximum shutoff time for cold load

		WeakHashMap<String, double[]> coldApplianceProfiles = owner.getColdAppliancesProfiles();
		double[] fridge_loads = coldApplianceProfiles.get(Consts.COLD_APP_FRIDGE_ORIGINAL);
		double[] freezer_loads = coldApplianceProfiles.get(Consts.COLD_APP_FREEZER_ORIGINAL);
		double[] fridge_freezer_loads = coldApplianceProfiles.get(Consts.COLD_APP_FRIDGEFREEZER_ORIGINAL);

		double[] fridge_loads_day = Arrays.copyOfRange(fridge_loads, (timeStep % fridge_loads.length), (timeStep % fridge_loads.length) + ticksPerDay);
		double[] freezer_loads_day = Arrays.copyOfRange(freezer_loads, (timeStep % freezer_loads.length), (timeStep % freezer_loads.length) + ticksPerDay);
		double[] fridge_freezer_loads_day = Arrays.copyOfRange(fridge_freezer_loads, (timeStep % fridge_freezer_loads.length), (timeStep % fridge_freezer_loads.length) + ticksPerDay);

		// %Begin by eliminating the negative signal slots as we won't want to
		// gap them
		// %but only if S is not null.
		// if max(S)>0.01
		double[] S = this.dayPredictedCostSignal.clone();
		for (int i = 0; i < 48; i++)
		{
			if (S[i] < 0)
				S[i] = 0;
		}

		for (int i = 1; i < 48; i++)
		{
			S[i] += S[i - 1];// %create intervals proportionate to original S
		}
		S = ArrayUtils.normalizeValues(S, 1, false); // Normalise values

		double n = RandomHelper.nextDouble();
		int k = 0; // %throw dice and initialise index
		while (S[k] < n)
			k++;

		double fridgeLoss = 0;
		double freezerLoss = 0;
		double fridgeFreezerLoss = 0;
		for (int j = k; j < k + Nt && j < 48; j++)
		{
			fridgeLoss += fridge_loads_day[j];
			freezerLoss += freezer_loads_day[j];
			fridgeFreezerLoss += fridge_freezer_loads_day[j];
			fridge_loads_day[j] = 0;
			freezer_loads_day[j] = 0;
			fridge_freezer_loads_day[j] = 0;
		}

		double fridgeAdd = fridgeLoss / (48 - Nt);
		double freezerAdd = freezerLoss / (48 - Nt);
		double fridgeFreezerAdd = fridgeFreezerLoss / (48 - Nt);

		for (int j = 0; j < 48; j++)
		{
			if (j < k && j > k + Nt)
			{
				fridge_loads_day[j] += fridgeAdd;
				freezer_loads_day[j] += freezerAdd;
				fridge_freezer_loads_day[j] += fridgeFreezerAdd;

			}
		}

		ArrayUtils.replaceRange(coldApplianceProfiles.get(Consts.COLD_APP_FREEZER), freezer_loads_day, timeStep % freezer_loads.length);
		ArrayUtils.replaceRange(coldApplianceProfiles.get(Consts.COLD_APP_FRIDGE), fridge_loads_day, timeStep % fridge_loads.length);
		ArrayUtils.replaceRange(coldApplianceProfiles.get(Consts.COLD_APP_FRIDGEFREEZER), fridge_freezer_loads_day, timeStep % fridge_freezer_loads.length);
		owner.setColdAppliancesProfiles(coldApplianceProfiles); // actually
																// unecessary as
																// done in place
	}

	/**
	 * Probabilistic assignment of desired heat pump "switch offs" and therefore
	 * set point to enact the next day
	 */
	private void assignSetPointsProbabilistic()
	{
		if (ArrayUtils.max(this.dayPredictedCostSignal) == 0 && ArrayUtils.min(this.dayPredictedCostSignal) == 0)
		{
			// signal is null - do nothing
			this.optimisedSetPointProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
			return;
		}

		double[] ExtTemp = this.priorDayExternalTempProfile.clone();
		double Tex = ArrayUtils.max(ExtTemp);//ArrayUtils.sum(ExtTemp) / ExtTemp.length;
		double Trm = ArrayUtils.max(owner.getSetPointProfile());//20; // %Fixed room temperature at this stage
		int Nt = 2;// Consts.HEAT_PUMP_MIN_SWITCHOFF; // %Fixed no of timeslots
					// to
					// 'gap' at this stage
		double CoP = Consts.DOMESTIC_HEAT_PUMP_SPACE_COP;// 2.4; %Baseline heat
															// pump performance
		// %Begin by eliminating the negative signal slots as we won't want to
		// gap them
		double[] S = this.dayPredictedCostSignal.clone();
		for (int i = 0; i < 48; i++)
		{
			if (S[i] < 0)
				S[i] = 0;
		}
		
/*		// Start of test.  this looks at signal over whole switch off period, not just start
		double[] Stemp = new double[S.length];
		for (int j = 0; j < S.length - Nt+1; j++)
		{
			double p = 0;
			for (int k = 0; k < Nt; k++)
			{
				p+=S[j+k];
			}
			p /= Nt;
			Stemp[j] = p;
		}
		S=Stemp;
		// end of test
*/
		S = ArrayUtils.normalizeValues(S, 1, false); // Normalise values
		for (int i = 1; i < 48; i++)
		{
			S[i] += S[i - 1];// %create intervals proportionate to original S
								// (effectively a CDF)
		}
		S = ArrayUtils.normalizeValues(S, 1, false); // Normalise values for CDF

		double[] AggL = new double[48];// zeros(1,48); %Initialise total
										// responsive load
		double[] BaseL = new double[48];// %Initialise total baseline load
		double[] L = new double[48];
		// Obtain thermal mass (kWh/degC), loss rate(W/degC) and time constant
		// (tau = mass / loss rate)
		// from owner building

		// %Now distribute gapping
		// double Tdrop = Trm -
		// ((Trm-Tex)*Math.exp(-1.0*Nt/owner.tau)+Tex);//%Work out temp drop
		// from each gapped timeslot
		double Tdrop = (Trm - Tex) * owner.freeRunningTemperatureLossPerTickMultiplier;
		if (Tdrop < 0.5)// && max(S)>0.01 %If S not null do response
		{
			// L=ArrayUtils.offset(L,(Nt*(Trm-Tex)*(owner.buildingHeatLossRate/1000)/((48-Nt)*0.9*CoP)));//%Add
			// gap recovery load spread over all timeslots
			double n = RandomHelper.nextDouble();
			int k = 0;// %throw dice and initialise index
			while (S[k] < n)
			{// %go through 48 steps to find (probabilistic) place for gap to
				// start
				k++;
			}

			double Tadd = (Tdrop * Nt) / (48 - Nt); // Amount to add to other
													// timeslots to
													// compensate for drop
			double totDrop = 0;
			double addedBefore = 0;
			// Shape the set point profile to reflect the gap timing

			for (int i = 0; i < k; i++)
			{
				addedBefore += Tadd;
				this.setPointProfile[i] += addedBefore;
			}

			for (int i = k; i < k + Nt && i < 48; i++)
			{
				this.setPointProfile[i] += addedBefore;
				totDrop += Tdrop;
				this.setPointProfile[i] -=totDrop; // set to =0 For test

			}
			for (int i = k + Nt; i < 48; i++)
			{
				this.setPointProfile[i] += addedBefore;
				this.setPointProfile[i] -= totDrop;
				this.setPointProfile[i] += (i + 1 - k - Nt) * Tadd;
			}

			this.optimisedSetPointProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
		}
	}

	/**
	 * Probabilistic re-assignment of water heating demand where probability of
	 * assigning to a slot is in inverse proportion to the price signal in all
	 * preceding slots in the day
	 */
	private void placeWaterHeatingProportional()
	{
		
		if (ArrayUtils.max(this.dayPredictedCostSignal) == 0 && ArrayUtils.min(this.dayPredictedCostSignal) == 0)
		{
			// If null signal, do nothing
			return;
		}

		// Note! This should be constant as yet, because
		// hotWaterVolumeDemandProfile =owner.baseHotWaterVolumeDemProfile
		// which is changed only on initialisation
		double[] baseArray = ArrayUtils.multiply(this.hotWaterVolumeDemandProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (owner.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP)) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP);
	
		this.owner.mainContext.logger.trace("hotWaterVolumeDemandProfile: "+ Arrays.toString(hotWaterVolumeDemandProfile));

		this.waterHeatDemandProfile = new double[baseArray.length];// Arrays.copyOf(baseArray,
																	// baseArray.length);
		this.owner.mainContext.logger.trace("waterHeatDemandProfile: "+ Arrays.toString(waterHeatDemandProfile));

		//this.owner.mainContext.logger.trace("spreadWaterDemand(baseArray) : "+ Arrays.toString(spreadWaterDemand(baseArray)));

		// double[] totalHeatDemand = ArrayUtils.add(this.heatPumpDemandProfile,
		// spreadWaterDemand(baseArray));

		for (int i = 0; i < this.waterHeatDemandProfile.length; i++)
		{
			double Sk = baseArray[i];

			if (Sk > 0)
			{

				double[] Wd = createAttractivityCDFFromSignal(i);

				double n = RandomHelper.nextDouble();// %Throw a dice again and
														// reset k
				int k = 0;
				while (k < Wd.length && Wd[k] < n)
					k++;// %find k value in which to do this water heating

				this.waterHeatDemandProfile[k] += baseArray[i];
				for (int m = k; m < i; m++)
				{
					this.waterHeatDemandProfile[m] += (Sk / (owner.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP)) * 0.5);// %Top
					// up
					// for
					// losses - estimated at 0.5 deg C per half hour
				}
			}
		}

	}

	/*
	 * Returns a CDF based on attractivity (inverse of price) of timeslots prior
	 * to slot i passed in.
	 */
	private double[] createAttractivityCDFFromSignal(int i)
	{
		double[] Wd = new double[dayPredictedCostSignal.length];
		double[] pre_prices = Arrays.copyOfRange(dayPredictedCostSignal, 0, i + 1);

		double Smax = ArrayUtils.max(pre_prices); // %find highest S value
		pre_prices = ArrayUtils.multiply(pre_prices, -1/*
														 * d /
														 * ArrayUtils.sum(pre_prices
														 * )
														 */);

		if (Smax < 0)
		{
			Smax = -Smax;
		}
		pre_prices = ArrayUtils.offset(pre_prices, Smax);
		// if (ArrayUtils.sum(pre_prices)!=0)
		{
			// in window before i
			// in window before i

			System.arraycopy(pre_prices, 0, Wd, 0, pre_prices.length); // generate
																		// "window function"
																		// based
																		// on
																		// inverse
																		// of
																		// price
																		// before
																		// slot
																		// i
			for (int k = 1; k <= i; k++)
			{
				Wd[k] = Wd[k] + Wd[k - 1];// %create cumulative distribution
											// of Wd
			}
			Wd = ArrayUtils.normalizeValues(Wd, 1, false); // Normalise CDF
			Arrays.fill(Wd, i + 1, Wd.length, 1);
		}
		return Wd;
	}

	/**
	 * A method to allow Wattbox to be dynamic in the sense of monitoring its
	 * owner for any new appliances and updating its state to accommodate.
	 */
	private void checkForNewAppliancesAndUpdateConstants()
	{
		// This could be more sophisticated, but for now all this needs to do is
		// make sure that
		// the wattbox assigns load only according to the rated heat pump and
		// immersion capacity
		// of its owner
		this.maxHeatPumpElecDemandPerTick = (owner.ratedPowerHeatPump * (double) 24 / ticksPerDay);
		this.maxImmersionHeatPerTick = Consts.MAX_DOMESTIC_IMMERSION_POWER * (double) 24 / ticksPerDay;
	}

	/**
	 * method to return all the optimised profiles currently held by this
	 * Wattbox
	 */
	public WeakHashMap<String, double[]> getCurrentProfiles()
	{
		WeakHashMap<String, double[]> returnMap = new WeakHashMap<String, double[]>();
		returnMap.put("HeatPump", optimisedSetPointProfile);
		returnMap.put("WaterHeat", waterHeatDemandProfile);
		return returnMap;
	}

	/**
	 * Optimise the Electric Vehicle charging profile for the household which
	 * owns this Wattbox.
	 * 
	 * TODO: Not yet implemented.
	 */
	private void optimiseEVProfile()
	{
	}

	/**
	 * Optimise the Wet appliance usage profile for the household which owns
	 * this Wattbox. It expects wet profiles to be 'discrete' (i.e. many/most
	 * timeslots contain zero values)
	 * 
	 * Allows wet appliance loads to be shifted by up to @see
	 * Consts.MAX_ALLOWED_WET_APP_MOVE timeslots after they were scheduled
	 */
	private void optimiseWetProfileProbabilistic(int timeStep)
	{
		this.owner.mainContext.logger.trace("==OptimiseWetProfil for a  "+ owner.getAgentID()+"; timeStep: "+ timeStep);
		this.owner.mainContext.logger.trace("dayPredictedCostSignal: "+ Arrays.toString(dayPredictedCostSignal));

		if (ArrayUtils.max(this.dayPredictedCostSignal) == 0 && ArrayUtils.min(this.dayPredictedCostSignal) == 0)
		{
			// No point doing anything if passed a null signal
			return;
		}
		
		WeakHashMap<String, double[]> wetApplianceProfiles = owner.getWetAppliancesProfiles();
		double[] washer_loads = wetApplianceProfiles.get(Consts.WET_APP_WASHER_ORIGINAL);
		double[] dryer_loads = wetApplianceProfiles.get(Consts.WET_APP_DRYER_ORIGINAL);
		double[] dishwasher_loads = wetApplianceProfiles.get(Consts.WET_APP_DISHWASHER_ORIGINAL);

		double[] washer_loads_day = Arrays.copyOfRange(washer_loads, (timeStep % washer_loads.length), (timeStep % washer_loads.length) + ticksPerDay);
		double[] dryer_loads_day = Arrays.copyOfRange(dryer_loads, (timeStep % dryer_loads.length), (timeStep % dryer_loads.length) + ticksPerDay);
		double[] dishwasher_loads_day = Arrays.copyOfRange(dishwasher_loads, (timeStep % dishwasher_loads.length), (timeStep % dishwasher_loads.length) + ticksPerDay);

		int Tw = Consts.MAX_ALLOWED_WET_APP_MOVE;
		this.owner.mainContext.logger.trace("BEFORE washer_loads_day: "+ Arrays.toString(washer_loads_day));
		this.owner.mainContext.logger.trace("BEFORE dryer_loads_day: "+ Arrays.toString(dryer_loads_day));
		this.owner.mainContext.logger.trace("BEFORE dishwasher_loads_day: "+ Arrays.toString(dishwasher_loads_day));

		// Extract cycles
		ArrayList<double[]> washCycles = new ArrayList<double[]>();
		ArrayList<double[]> dryCycles = new ArrayList<double[]>();
		ArrayList<double[]> dishwashCycles = new ArrayList<double[]>();

		int i = 0;

		while (i < washer_loads_day.length)
		{
			if (washer_loads_day[i] > 0)
			{
				double[] thisCycle = new double[washer_loads_day.length];
				while (i < washer_loads_day.length && washer_loads_day[i] > 0)
				{
					thisCycle[i] = washer_loads_day[i];
					i++;
				}
				washCycles.add(thisCycle);
			}
			i++;
		}
		i = 0;
		while (i < dryer_loads_day.length)
		{
			if (dryer_loads_day[i] > 0)
			{
				double[] thisCycle = new double[dryer_loads_day.length];
				while (i < dryer_loads_day.length && dryer_loads_day[i] > 0)
				{
					thisCycle[i] = dryer_loads_day[i];
					i++;
				}
				dryCycles.add(thisCycle);
			}
			i++;
		}
		i = 0;
		while (i < dishwasher_loads_day.length)
		{
			if (dishwasher_loads_day[i] > 0)
			{
				double[] thisCycle = new double[dishwasher_loads_day.length];
				while (i < dishwasher_loads_day.length && dishwasher_loads_day[i] > 0)
				{
					thisCycle[i] = dishwasher_loads_day[i];
					i++;
				}
				dishwashCycles.add(thisCycle);
			}
			i++;
		}

		for (double[] c : washCycles)
		{
			i = 0;
			while (c[i] == 0)
			{
				i++;
			}
			moveWetLoad(c, dayPredictedCostSignal, Tw, i);
		}

		for (double[] c : dryCycles)
		{
			i = 0;
			while (c[i] == 0)
			{
				i++;
			}
			moveWetLoad(c, dayPredictedCostSignal, Tw, i);
		}

		for (double[] c : dishwashCycles)
		{
			i = 0;
			while (c[i] == 0)
			{
				i++;
			}
			moveWetLoad(c, dayPredictedCostSignal, Tw, i);
		}

		double[] newWash = new double[washer_loads_day.length];
		double[] newDry = new double[washer_loads_day.length];
		double[] newDish = new double[washer_loads_day.length];

		for (double[] n : washCycles)
		{
			newWash = ArrayUtils.add(newWash, n);
		}
		for (double[] n : dryCycles)
		{
			newDry = ArrayUtils.add(newDry, n);
		}
		for (double[] n : dishwashCycles)
		{
			newDish = ArrayUtils.add(newDish, n);
		}

		ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_WASHER), newWash, timeStep % washer_loads.length);

		ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_DRYER), newDry, timeStep % dryer_loads.length);

		ArrayUtils.replaceRange(wetApplianceProfiles.get(Consts.WET_APP_DISHWASHER), newDish, timeStep % dishwasher_loads.length);
		
		owner.setWetAppliancesProfiles(wetApplianceProfiles);

	}

	/**
	 * @param washer_loads_day
	 * @param dayPredictedCostSignal2
	 * @param tw
	 */
	private void moveWetLoad(double[] w, double[] s, int tw, int i)
	{
		double[] Wd = new double[s.length];
		double[] sW = ArrayUtils.multiply(s, -1);
		sW = ArrayUtils.offset(sW, -ArrayUtils.min(sW));

		int j = i;
		while (j < w.length && w[j] > 0)
		{
			j++;
		}
		// i is start of cycle, j is end

		double[] thisCycle = Arrays.copyOfRange(w, i, j);

		Arrays.fill(w, i, j, 0);

		for (int k = i; k < s.length && k < i + tw; k++)
		{
			Wd[k] = (k == 0 ? 0 : Wd[k - 1]) + sW[k];
		}
		for (int k = i + tw; k < Wd.length; k++)
		{
			Wd[k] = Wd[k - 1];
		}
		Wd = ArrayUtils.normalizeValues(Wd, 1, false);

		double tVar = RandomHelper.nextDouble();
		int k = i;
		while (k + (j - i) < w.length && Wd[k] < tVar) // cycle must fit in day
														// firstly - then be put
														// in the best slot
		{
			k++;
		}

		System.arraycopy(thisCycle, 0, w, k, thisCycle.length);

	}

	/**
	 * @param dayPredictedCostSignal
	 *            the dayPredictedCostSignal to set
	 */
	public void setDayPredictedCostSignal(double[] dayPredictedCostSignal)
	{
		this.dayPredictedCostSignal = dayPredictedCostSignal;
	}

	/**
	 * Calculates the estimated demand caused by a given set point array
	 * 
	 * @param localSetPointArray
	 *            - an array containing the set point profile for which the
	 *            estimated demand should be calculated
	 * @return the estimated demand profile or null if the set point profile is
	 *         not physically achievable
	 */
	private double[] calculateEstimatedSpaceHeatPumpDemand(
			double[] localSetPointArray)
	{
		double[] energyProfile = new double[ticksPerDay];
		double[] deltaT = ArrayUtils.add(localSetPointArray, ArrayUtils.negate(priorDayExternalTempProfile));
		// int availableHeatRecoveryTicks = ticksPerDay;
		double maxRecoveryPerTick = 0.5d * Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE; // i.e.
																								// later
		// double internalTemp = this.setPointProfile[0];

		for (int i = 0; i < ticksPerDay; i++)
		{
			// --availableHeatRecoveryTicks;
			// currentTempProfile[i] = internalTemp;
			double tempChange;

			if (i > 0)
			{
				tempChange = localSetPointArray[i] - localSetPointArray[i - 1];
			} else
			{
				tempChange = localSetPointArray[i] - this.setPointProfile[0];
			}

			// double setPointMaintenanceEnergy = deltaT[i] *
			// ((owner.buildingHeatLossRate /
			// Consts.KWH_TO_JOULE_CONVERSION_FACTOR)) * (Consts.SECONDS_PER_DAY
			// / ticksPerDay);
			double setPointMaintenanceEnergy = (this.setPointProfile[i] - priorDayExternalTempProfile[i]) * ((owner.getBuildingHeatLossRate() / Consts.KWH_TO_JOULE_CONVERSION_FACTOR)) * (Consts.SECONDS_PER_DAY / ticksPerDay);

			// tempChangePower can be -ve if the temperature is falling. If
			// tempChangePower magnitude
			// is greater than or equal to setPointMaintenance, the heat pump is
			// off.
			double tempChangeEnergy = tempChange * owner.getBuildingThermalMass();

			// Although the temperature profiles supplied should be such that
			// the heat
			// can always be recovered within a reasonable cap - we could put a
			// double
			// check in here.
			/*
			 * if (tempChangeEnergy > maxRecoveryPerTick *
			 * ((owner.buildingHeatLossRate /
			 * Consts.KWH_TO_JOULE_CONVERSION_FACTOR) * (Consts.SECONDS_PER_DAY
			 * / ticksPerDay) * ArrayUtils.max(deltaT))) { //System.err.println(
			 * "WattboxController: Should never get here - asked to get demand for a profile that can't recover temp"
			 * ); return null; }
			 */

			// Add in the energy to maintain the new temperature, otherwise it
			// can be cheaper
			// To let the temperature fall and re-heat under flat price and
			// external temperature
			// conditions. TODO: Is this a good physical analogue?
			// setPointMaintenanceEnergy += tempChange *
			// ((owner.buildingHeatLossRate /
			// Consts.KWH_TO_JOULE_CONVERSION_FACTOR)) * (Consts.SECONDS_PER_DAY
			// / ticksPerDay);

			double heatPumpEnergyNeeded = Math.max(0, (setPointMaintenanceEnergy + tempChangeEnergy) / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP);
			// double heatPumpEnergyNeeded = (setPointMaintenanceEnergy /
			// Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) + (tempChangeEnergy *
			// (Consts.DOMESTIC_HEAT_PUMP_SPACE_COP *
			// Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE));

			// zero the energy if heat pump would be off
			if (deltaT[i] < Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF)
			{
				// heat pump control algorithm would switch off pump
				heatPumpEnergyNeeded = 0;
			}

			if (heatPumpEnergyNeeded > (owner.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP * 24 / ticksPerDay))
			{
				// This profiel produces a value that exceeds the total capacity
				// of the
				// heat pump and is therefore unachievable.
				this.owner.mainContext.logger.trace("Nulling the demand profile for energy needed " + heatPumpEnergyNeeded );
				// Can't satisfy this demand for this set point profile, return
				// null
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
	 * @param localDemandProfile
	 *            the demand profile array to be evaluated
	 * @return the cost of the demand profile at the current predicted costs per
	 *         tick
	 */
	private double evaluateCost(double[] localDemandProfile)
	{
		return ArrayUtils.sum(ArrayUtils.mtimes(localDemandProfile, this.dayPredictedCostSignal));
	}

	/**
	 * Constructor - creates an uninitialized Wattbox controller assigned to the
	 * prosumer passed in as an argument.
	 * 
	 * @param owner
	 *            - the prosumer agent that owns this wattbox
	 */
	public OriginalProportionalWattboxController(HouseholdProsumer owner)
	{
		super();

		this.owner = owner;
		ticksPerDay = owner.getContext().getNbOfTickPerDay();
		// this.priorDayExternalTempProfile =
		// Arrays.copyOf(INITIALIZATION_TEMPS, INITIALIZATION_TEMPS.length);
		// Initialise with a flat external temperature - thus no incentive to
		// move demand on first day of use.
		this.priorDayExternalTempProfile = new double[ticksPerDay];
		Arrays.fill(priorDayExternalTempProfile, Consts.INITIALISATION_EXTERNAL_TEMP);

		if (owner.isHasElectricalSpaceHeat())
			this.heatPumpOnOffProfile = Arrays.copyOf(owner.spaceHeatPumpOn, owner.spaceHeatPumpOn.length);

		if (owner.isHasElectricalWaterHeat())
			this.hotWaterVolumeDemandProfile = Arrays.copyOfRange(owner.getBaselineHotWaterVolumeProfile(), ((Math.max(0, (int) RepastEssentials.GetTickCount())) % owner.getBaselineHotWaterVolumeProfile().length), ((Math.max(0, (int) RepastEssentials.GetTickCount())) % owner.getBaselineHotWaterVolumeProfile().length) + ticksPerDay);

		this.maxHeatPumpElecDemandPerTick = (owner.ratedPowerHeatPump * (double) 24 / ticksPerDay);
		this.maxImmersionHeatPerTick = Consts.MAX_DOMESTIC_IMMERSION_POWER * (double) 24 / ticksPerDay;

	}

	public OriginalProportionalWattboxController(HouseholdProsumer owner, CascadeContext context)
	{
		this(owner);
		owner.mainContext = context;

	}

	/**
	 * Constructor - creates a Wattbox controller assigned to the prosumer
	 * passed in as an argument and initialized with a user profile and
	 * lifestyle also passed in.
	 * 
	 * @param owner
	 *            - the prosumer agent that owns this wattbox
	 * @param userProfile
	 * @param userLifestyle
	 */
	public OriginalProportionalWattboxController(HouseholdProsumer owner, WattboxUserProfile userProfile, WattboxLifestyle userLifestyle)
	{
		this(owner);
		this.userProfile = userProfile;
		this.userLifestyle = userLifestyle;

	}
}
