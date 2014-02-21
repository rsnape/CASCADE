package uk.ac.dmu.iesd.cascade.test;

import static repast.simphony.essentials.RepastEssentials.FindNetwork;
import static repast.simphony.essentials.RepastEssentials.GetParameter;

import java.util.Arrays;
import java.util.WeakHashMap;

import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.controllers.ISmartController;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

public class HHProsumer extends ProsumerAgent{

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */

	boolean hasCHP = false;
	boolean hasAirSourceHeatPump = false;
	boolean hasGroundSourceHeatPump = false;
	boolean hasWind = false;
	boolean hasHydro = false;
	// Thermal Generation included so that Household can represent
	// Biomass, nuclear or fossil fuel generation in the future
	// what do we think?
	boolean hasThermalGeneration = false;
	boolean hasPV = false;
	boolean hasSolarWaterHeat = false;
	boolean hasElectricalWaterHeat = false;
	boolean hasElectricalSpaceHeat = false;
	boolean hasElectricVehicle = false;
	boolean hasElectricalStorage = false; // Do we need to break out various storage technologies?
	boolean hasHotWaterStorage = false;
	boolean hasSpaceHeatStorage = false;

	/*
	 * the rated power of the various technologies / appliances we are interested in
	 * 
	 * Do not initialise these initially.  They should be initialised when an
	 * instantiated agent is given the boolean attribute which means that they
	 * have access to one of these technologies.
	 */
	double ratedPowerCHP;
	double ratedPowerAirSourceHeatPump;
	double ratedPowerGroundSourceHeatPump;
	double ratedPowerWind;
	double ratedPowerHydro;
	double ratedPowerThermalGeneration;
	double ratedPowerPV;
	double ratedPowerSolarWaterHeat;
	public double ratedPowerHeatPump;
	double ratedCapacityElectricVehicle; // Note kWh rather than kW
	double ratedCapacityElectricalStorage;   // Note kWh rather than kW
	double ratedCapacityHotWaterStorage;   // Note kWh rather than kW
	public double dailyHotWaterUsage;
	public double getDailyHotWaterUsage() {
		return dailyHotWaterUsage;
	}

	public void setDailyHotWaterUsage(double dailyHotWaterUsage) {
		this.dailyHotWaterUsage = dailyHotWaterUsage;
	}

	double ratedCapacitySpaceHeatStorage; // Note - related to thermal mass

	/*
	 * Appliance ownership parameters.  All default to false - should
	 * be set whilst building the scenario
	 */
	boolean hasFridgeFreezer;	
	boolean hasRefrigerator;	
	boolean hasUprightFreezer;	
	boolean hasChestFreezer;		
	boolean hasWashingMachine;	
	boolean hasWasherDryer;	
	boolean hasTumbleDryer;	
	boolean hasDishWasher;

	// For Households' heating requirements
	/**
	 * Thermal mass of building (expressed in kWh per deg C)
	 */
	public double buildingThermalMass;
	/**
	 * Heat loss rate for the building (expressed in Watts per degree C, or Joules per second per deg C)
	 */
	public double buildingHeatLossRate;

	/*
	 * temperature control parameters
	 */
	double[] setPointProfile;
	double[] optimisedSetPointProfile;
	double setPoint;
	double minSetPoint;  // The minimum temperature for this Household's building in centigrade (where relevant)
	double maxSetPoint;  // The maximum temperature for this Household's building in centigrade (where relevant)
	public double waterSetPoint;
	private double currentInternalTemp;
	double currentWaterTemp;

	//Occupancy information
	int numOccupants;
	int numAdults;
	int numChildren;
	int numTeenagers;
	int[] occupancyProfile;

	/*
	 * Specifically, a household may have a certain percentage of demand
	 * that it believes is moveable and / or a certain maximum time shift of demand
	 */
	double percentageMoveableDemand;  // This can be set constant, or calculated from available appliances
	int maxTimeShift; // The "furthest" a demand may be moved in time.  This can be constant or calculated dynamically.
	/*
	 * Behavioural properties
	 */
	// Learner adoptSmartMeterLearner;
	// Learner adoptSmartControlLearner;
	// Learner consumptionPatternLearner;
	double transmitPropensitySmartControl;
	double transmitPropensityProEnvironmental;
	double visibilityMicrogen;

	/*
	 * This may or may not be used, but is a threshold cost above which actions
	 * take place for the household
	 */
	double costThreshold;

	/**
	 * Richard hack to get DEFRA profiles going
	 */
	double microgenPropensity;
	double insulationPropensity;
	double HEMSPropensity;
	double EVPropensity;
	double habit;
	int defraCategory;
	public double[] spaceHeatPumpOn; 
	private boolean waterHeaterOn;

	/**
	 * Simulation variables needed throughout
	 */
	int time;
	int timeOfDay;
	private double[] dailyElasticity;

	/**
	 * Available smart devices
	 */
	ISmartController mySmartController;
	WeakHashMap currentSmartProfiles; 
	public double[] coldApplianceProfile;
	public double[] wetApplianceProfile;
	public double[] baselineHotWaterVolumeProfile;
	private double[] waterHeatProfile;

	//For ease of access to a debug type outputter
	CSVWriter sampleOutput;
	private double[] recordedHeatPumpDemand;

	//Arrays for the day's history (mainly for GUI - may use, write to file or something else in the future)
	private double[] historicalBaseDemand;
	private double[] historicalColdDemand;
	private double[] historicalWetDemand;
	private double[] historicalSpaceHeatDemand;

	/**
	 * Building heat flow time constant (thermal mass or specific heat capacity / heat loss rate)
	 */
	public double tau;
	private double timeSinceHeating = 0;
	private double[] historicalIntTemp;
	private double[] historicalExtTemp;
	private double[] historicalWaterHeatDemand;
	public double freeRunningTemperatureLossPerTickMultiplier;
	
	
	double[] modifiedDemandProfile;



	/**
	 * Accessor functions (NetBeans style)
	 * TODO: May make some of these private to respect agent conventions of autonomy / realistic simulation of humans
	 */

	public double getCurrentInternalTemp() {
		return currentInternalTemp;
	}

	public double getSetPoint() {
		return setPoint;
	}

	public double[] getSetPointProfile() {
		return Arrays.copyOf(setPointProfile, setPointProfile.length);
	}

	/**
	 * @return
	 */
	public double[] getOptimisedSetPointProfile() {
		return Arrays.copyOf(optimisedSetPointProfile, optimisedSetPointProfile.length);
	}


	public boolean isHasElectricVehicle() {
		return hasElectricVehicle;
	}

	public boolean isHasElectricalWaterHeat() {
		return hasElectricalWaterHeat;
	}

	public void setHasElectricalWaterHeat(boolean hasElectricalWaterHeat) {
		this.hasElectricalWaterHeat = hasElectricalWaterHeat;
	}

	public boolean isHasElectricalSpaceHeat() {
		return hasElectricalSpaceHeat;
	}

	public void setHasElectricalSpaceHeat(boolean hasElectricalSpaceHeat) {
		this.hasElectricalSpaceHeat = hasElectricalSpaceHeat;
	}
	public boolean isHasWashingMachine() {
		return hasWashingMachine;
	}

	public void setHasWashingMachine(boolean hasWashingMachine) {
		this.hasWashingMachine = hasWashingMachine;
	}

	public boolean isHasWasherDryer() {
		return hasWasherDryer;
	}

	public void setHasWasherDryer(boolean hasWasherDryer) {
		this.hasWasherDryer = hasWasherDryer;
	}

	public boolean isHasTumbleDryer() {
		return hasTumbleDryer;
	}

	public void setHasTumbleDryer(boolean hasTumbleDryer) {
		this.hasTumbleDryer = hasTumbleDryer;
	}

	public boolean isHasDishWasher() {
		return hasDishWasher;
	}

	public void setHasDishWasher(boolean hasDishWasher) {
		this.hasDishWasher = hasDishWasher;
	}

	public int getNumOccupants() {
		return numOccupants;
	}

	public void setNumOccupants(int numOccupants) {
		this.numOccupants = numOccupants;
	}

	public int getDefraCategory()
	{
		return defraCategory;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalBaseDemand() {
		// TODO Auto-generated method stub
		return this.historicalBaseDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalColdDemand() {
		// TODO Auto-generated method stub
		return this.historicalColdDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalWetDemand() {
		// TODO Auto-generated method stub
		return this.historicalWetDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalSpaceHeatDemand() {
		// TODO Auto-generated method stub
		return this.historicalSpaceHeatDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalWaterHeatDemand() {
		// TODO Auto-generated method stub
		return this.historicalWaterHeatDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalIntTemp() {
		return historicalIntTemp;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalExtTemp() {
		return historicalExtTemp;
	}

	/**
	 * @param buildingThermalMass the buildingThermalMass to set
	 */
	public void setBuildingThermalMass(double buildingThermalMass) {
		this.buildingThermalMass = buildingThermalMass;
	}

	/**
	 * @param buildingHeatLossRate the buildingHeatLossRate to set
	 */
	public void setBuildingHeatLossRate(double buildingHeatLossRate) {
		this.buildingHeatLossRate = buildingHeatLossRate;
	}

	/**
	 * Returns a string representing the state of this agent. This 
	 * method is intended to be used for debugging purposes, and the 
	 * content and format of the returned string should include the states (variables/parameters)
	 * which are important for debugging purpose.
	 * The returned string may be empty but may not be <code>null</code>.
	 * 
	 * @return  a string representation of this agent's state parameters
	 */
	protected String paramStringReport(){
		String str="";
		return str;

	}
	
	private double calcualteElasticDemand(int time)
	{
		double myDemand;
		//int timeSinceSigValid = time - predictionValidTime;
		
		//As a basic strategy only the base (non-displaceable) demand is
		//elastic
		
		myDemand = arr_otherDemandProfile[time % arr_otherDemandProfile.length];
		myDemand = myDemand * (1 - dailyElasticity[time % this.mainContext.ticksPerDay]);

       /*
		if(hasSmartMeter && getPredictedCostSignalLength() > 0)
		{
			double predictedCostNow = getPredictedCostSignal()[timeSinceSigValid % getPredictedCostSignalLength()];
			myDemand = myDemand * (1 - ((predictedCostNow / Consts.NORMALIZING_MAX_COST) * dailyElasticity[time % this.mainContext.ticksPerDay]));
			if (Consts.DEBUG)
			{
				if (Consts.DEBUG) System.out.println("HHProsumer:: Based on predicted cost = " + predictedCostNow + " demand set to " + (1 - ((predictedCostNow / Consts.NORMALIZING_MAX_COST) * dailyElasticity[time % this.mainContext.ticksPerDay])) + " of initial " );
			}
		} */

		return myDemand;
	}


	

	public double getUnadaptedDemand(){
		// Cope with tick count being null between project initialisation and start.
		int index = Math.max(((int) RepastEssentials.GetTickCount() % arr_otherDemandProfile.length), 0);
		return (arr_otherDemandProfile[index]) - currentGeneration();
	}

	/**
	 * Method to return the realtime generation of this Household Prosumer (as distinct
	 * from the net demand for the prosumer).
	 * 
	 * @return a double giving the realtime generation of this Household Prosumer
	 *
	 */
	private double currentGeneration() {
		double returnAmount = 0;

		returnAmount = returnAmount + CHPGeneration() + windGeneration() + hydroGeneration() + thermalGeneration() + PVGeneration();
		if (Consts.DEBUG)
		{
			if (returnAmount != 0)
			{
				if (Consts.DEBUG) System.out.println("HHProsumer:: Generating " + returnAmount);
			}
		}
		return returnAmount;
	}

	/**
	 * @return
	 */
	private double PVGeneration() {
		if (hasPV) 
		{
			// TODO: get a realistic model of solar production - this just assumes
			// linear relation between insolation and some arbitrary maximum insolation
			// at which the PV cell produces its rated power
			return (getInsolation() / Consts.MAX_INSOLATION) * ratedPowerPV;
		} 
		else 
		{
			return 0;
		}
	}

	/**
	 * @return
	 */
	private double thermalGeneration() {
		// Assume no thermal gen in domestic for now
		return 0;
	}

	/**
	 * @return
	 */
	private double hydroGeneration() {
		// Assumer no domestic hydro for now
		return 0;
	}

	/**
	 * @return
	 */
	private double windGeneration() {
		if (hasWind) 
		{
			// TODO: get a realistic model of wind production - this just linear
			// between 2.5 and 12.5 metres per second (5 to 25 mph / knots roughly),
			// zero below, max power above
			return (Math.max((Math.min(getWindSpeed(), 12.5f) - 2.5f), 0)) / 20
			* ratedPowerWind;
		} 
		else 
		{
			return 0;
		}
	}

	/**
	 * @return
	 */
	private double CHPGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private double nonDispaceableDemand() {
		//TODO: For expediency just used melody's model for this, however could use
		// finer grained model of lighting, brown and cooking

		//return lightingDemand() + miscBrownDemand() + cookingDemand();
		return arr_otherDemandProfile[time % arr_otherDemandProfile.length];
	}

	/**
	 * Returns the *electrical* load of heating demand
	 * Note that if heating is provided by solar thermal, gas boiler or CHP
	 * this demand will be zero.  
	 * 
	 * Only demands from immersion heater, heat pump or electrical storage
	 * heating are considered here
	 * 
	 * @return
	 */
	private double heatingDemand() {
		recordedHeatPumpDemand[timeOfDay] = 0;

		if (hasElectricalSpaceHeat)
		{
			//if (Consts.DEBUG) System.out.println(" ^^^hasElecSpaceheat ");
			// TODO: this assumes only space heat and always uses heat pump - expand for other forms of electrical heating
			recordedHeatPumpDemand[timeOfDay] += calculateHeatPumpDemandAndInternalTemp(time);
			//if (Consts.DEBUG) System.out.println("calcualteHeatPumpDem&IntTemp: "+recordedHeatPumpDemand[timeOfDay]);

		}

		if (hasElectricalWaterHeat)
		{
			//if (Consts.DEBUG) System.out.println(" ^^^hasElecWaterHeat ");
			// TODO: this assumes only space heat and always uses heat pump - expand for other forms of electrical heating
			//if (Consts.DEBUG) System.out.println("getWaterHeatProf: "+Arrays.toString(getWaterHeatProfile()));
			//if (Consts.DEBUG) System.out.println("recordedHeatPumpDemand: "+Arrays.toString(recordedHeatPumpDemand));
			recordedHeatPumpDemand[timeOfDay] += getWaterHeatProfile()[timeOfDay];
			//if (Consts.DEBUG) System.out.println("getWaterHeatProf: "+recordedHeatPumpDemand[timeOfDay]);


		}
		//if (Consts.DEBUG) System.out.println("return: "+ recordedHeatPumpDemand[timeOfDay]);

		return recordedHeatPumpDemand[timeOfDay];
	}

	/**
	 * @return
	 */
	private double wetApplianceDemand() {
		return this.wetApplianceProfile[time % wetApplianceProfile.length];
	}

	/**
	 * @return
	 */
	private double coldApplianceDemand() 
	{
		return this.coldApplianceProfile[time % wetApplianceProfile.length];
	}

	/**
	 * @return
	 */
	private double cookingDemand() {
		// Currently incorporated into the base demand
		return 0;
	}



	/**
	 * @return
	 */
	private double miscBrownDemand() {
		// Currently incorporated into the base demand
		return 0;
	}

	/**
	 * @return
	 */
	private double lightingDemand() {
		// Currently incorporated into the base demand
		return 0;
	}

	/**
	 * Calculates the heat pump demand at a given timestep (in kWh)
	 * 
	 * @param timeStep
	 * @return
	 */
	private double calculateHeatPumpDemandAndInternalTemp(int timeStep) {
		//demand is the local variable holding the energy demand
		
		//if (Consts.DEBUG) System.out.println("Inside: Calcualted Heat pump Demand&IntTemp");
		double demand = 0;
		double deltaT = this.currentInternalTemp - this.getContext().getAirTemperature(timeStep);
		//if (Consts.DEBUG) System.out.println("deltaT: "+ deltaT);

		double requiredTempChange = this.setPoint - currentInternalTemp;
		//if (Consts.DEBUG) System.out.println("requiredTempChange: "+ requiredTempChange);
		//if (Consts.DEBUG) System.out.println("buildingHeatLossRate: "+ buildingHeatLossRate);
		//if (Consts.DEBUG) System.out.println("Consts.SECONDS_PER_DAY / this.mainContext.ticksPerDay: "+ (double)Consts.SECONDS_PER_DAY / this.mainContext.ticksPerDay);
		//if (Consts.DEBUG) System.out.println("(Consts.SECONDS_PER_DAY / this.mainContext.ticksPerDay) / Consts.KWH_TO_JOULE_CONVERSION_FACTOR: "+ ((double)Consts.SECONDS_PER_DAY / this.mainContext.ticksPerDay)/Consts.KWH_TO_JOULE_CONVERSION_FACTOR);


		double maintenanceEnergy =  ((deltaT * (this.buildingHeatLossRate)) * ((double)(Consts.SECONDS_PER_DAY / this.mainContext.ticksPerDay))) / Consts.KWH_TO_JOULE_CONVERSION_FACTOR;
		//if (Consts.DEBUG) System.out.println("maintenanceEnergy: "+ maintenanceEnergy);

		double heatingEnergy = requiredTempChange * this.buildingThermalMass;
		//if (Consts.DEBUG) System.out.println("heatingEnergy: "+ heatingEnergy);


		if(Consts.DEBUG)
		{
			if (Consts.DEBUG) System.out.println("HHProsumer:: For agent " + this.getAgentName() + "at tick " + timeStep + " requiredTempChange = " + requiredTempChange + ", energy for temp maintenance = " + maintenanceEnergy + ", temp change energy = " + heatingEnergy);
		}

		if ((requiredTempChange < (0 - Consts.TEMP_CHANGE_TOLERANCE)) || (deltaT < Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF))
		{
			//heat pump off, leave demand at zero and decrement internal temperature
			this.currentInternalTemp -= maintenanceEnergy / this.buildingThermalMass;
			//if (Consts.DEBUG) System.out.println("currentInternalTemp1: "+ currentInternalTemp);
		}
		else
		{
			demand = maintenanceEnergy + heatingEnergy;
			if (demand > ((this.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) * (double) 24 / this.mainContext.ticksPerDay))
			{
				demand = (this.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) * ((double) 24 / this.mainContext.ticksPerDay);
				this.currentInternalTemp = this.currentInternalTemp + ((demand - maintenanceEnergy) / this.buildingThermalMass);
				//if (Consts.DEBUG) System.out.println("currentInternalTemp2: "+ currentInternalTemp);

			}
			else
			{
				this.currentInternalTemp = this.setPoint;
				//if (Consts.DEBUG) System.out.println("currentInternalTemp3: "+ currentInternalTemp);

			}
		}
		//if (Consts.DEBUG) System.out.println("demand: "+ demand);


		return demand;
	}

	/**
	 * Calculates the current internal temperature at the end of the current timestep
	 *  
	 * @param timeStep
	 * @return
	 */
	private void recordInternalAndExternalTemp(int timeStep)
	{
		double extTemp = this.getContext().getAirTemperature(timeStep);

		historicalIntTemp[timeStep % historicalIntTemp.length] = this.currentInternalTemp;
		historicalExtTemp[timeStep % historicalIntTemp.length] = extTemp;
	}

	/**
	 * calculates the water temperature
	 * 
	 * Currently has a very simple linear model of water cooling
	 * TODO: replace linear model with a more sophisticated negative exponential model.
	 * 
	 * @param timeStep
	 * @return
	 */
	private void calculateWaterTemp(int timeStep)
	{
		if (!waterHeaterOn)
		{
			this.currentWaterTemp = this.currentWaterTemp - 1;
		}
	}

	/**
	 * @param time
	 * @return double giving sum of baseDemand for the day.
	 */
	private double calculateFixedDayTotalDemand(int time) {
		int baseProfileIndex = time % arr_otherDemandProfile.length;
		return ArrayUtils.sum(Arrays.copyOfRange(arr_otherDemandProfile,baseProfileIndex,baseProfileIndex+this.mainContext.ticksPerDay - 1));
	}

	/*
	 * Logic helper methods
	 */

	/**
	 * Evaluates the net demand mediated by the prosumers elastic behaviour in a given half hour.
	 * 
	 * NOTE: 	As implemented - this method does not enforce total demand parity over a given day (i.e.
	 * 			integral of netDemand over a day is not necessarily constant.
	 * 
	 * @param 	int time	- the simulation time in ticks at which to evaluate this prosumer's behaviour
	 * @return 	double myDemand		- the demand for this half hour, mediated via behaviour change
	 * 
	 * TODO: Babak 
	 * Name the method to calcualteElasticDemand() it is called for those agents who have elasticity behavior
	 * instead of time (tick time, as currently done), pass timeslot as argument and use it directly,
	 * so there will be no need to timeSinceSigValid stuff
	 *  
	 */
	private double evaluateElasticBehaviour(int time)
	{
		if (Consts.DEBUG) System.out.println("HHProsumer:: evaluateElasticBehaviour --- time: "+time);
		double myDemand;
		int timeSinceSigValid = time - predictionValidTime;
		if (Consts.DEBUG) System.out.println("     predictionValidTime: "+predictionValidTime);
		if (Consts.DEBUG) System.out.println("     predictionValidTime: "+predictionValidTime);
		if (Consts.DEBUG) System.out.println("     timeSinceSigValid: "+timeSinceSigValid);

		//As a basic strategy only the base (non-displaceable) demand is
		//elastic
		myDemand = arr_otherDemandProfile[time % arr_otherDemandProfile.length];
		double initialDemand = myDemand;
		
		//if (Consts.DEBUG) System.out.println("     hasSmartMeter: "+hasSmartMeter);
		//if (Consts.DEBUG) System.out.println("     getPredictedCostSignalLength: "+getPredictedCostSignalLength());
		if (Consts.DEBUG) System.out.println("     PredictedCostSignal (cost Sig): "+Arrays.toString(getPredictedCostSignal()));
		//if (Consts.DEBUG) System.out.println("     dailyEalsticity : "+Arrays.toString(dailyElasticity));


		// Adapt behaviour somewhat.  Note that this does not enforce total demand the same over a day.
		// Note, we can only moderate based on cost signal
		// if we receive it (i.e. if we have smart meter)
		// TODO: may have to refine this - do we differentiate smart meter and smart display - i.e. whether receive only or Tx/Rx
		
		//if(hasSmartMeter && getPredictedCostSignalLength() > 0)
		if(getPredictedCostSignalLength() > 0)
		{
			if (Consts.DEBUG) System.out.println("     timeSinceSigValid % getPredictedCostSignalLength()): "+timeSinceSigValid % getPredictedCostSignalLength());
			double predictedCostNow = getPredictedCostSignal()[timeSinceSigValid % getPredictedCostSignalLength()];
			if (predictedCostNow == 1)
				if (Consts.DEBUG) System.out.println("    ********  predictedCost is ONE ****** ");
			if (Consts.DEBUG) System.out.println("     predictedCost (cost Signal): "+predictedCostNow);
			if (Consts.DEBUG) System.out.println("     dailyElasticity : "+dailyElasticity[time % this.mainContext.ticksPerDay]);

			myDemand = myDemand * (1 - ((predictedCostNow / Consts.NORMALIZING_MAX_COST) * dailyElasticity[time % this.mainContext.ticksPerDay]));
			if (Consts.DEBUG)
			{
				if (Consts.DEBUG) System.out.println("    initialDemand: "+initialDemand);
				if (Consts.DEBUG) System.out.println("    demand multiplier " + (1 - ((predictedCostNow / Consts.NORMALIZING_MAX_COST) * dailyElasticity[time % this.mainContext.ticksPerDay])) );
				if (Consts.DEBUG) System.out.println("    set finalDemand: " + myDemand);
			}
		}

		return myDemand;
	}
	
	private double evaluateElasticBehaviour2(int time)
	{
		if (Consts.DEBUG) System.out.println("HHProsumer:: evaluateElasticBehaviour --- time: "+time);
		double myDemand;
		int timeSinceSigValid = time - predictionValidTime;
		if (Consts.DEBUG) System.out.println("     predictionValidTime: "+predictionValidTime);
		if (Consts.DEBUG) System.out.println("     predictionValidTime: "+predictionValidTime);
		if (Consts.DEBUG) System.out.println("     timeSinceSigValid: "+timeSinceSigValid);

		//As a basic strategy only the base (non-displaceable) demand is
		//elastic
		myDemand = arr_otherDemandProfile[time % arr_otherDemandProfile.length];
		double initialDemand = myDemand;
		
		//if (Consts.DEBUG) System.out.println("     hasSmartMeter: "+hasSmartMeter);
		//if (Consts.DEBUG) System.out.println("     getPredictedCostSignalLength: "+getPredictedCostSignalLength());
		if (Consts.DEBUG) System.out.println("     PredictedCostSignal (cost Sig): "+Arrays.toString(getPredictedCostSignal()));
		//if (Consts.DEBUG) System.out.println("     dailyEalsticity : "+Arrays.toString(dailyElasticity));


		// Adapt behaviour somewhat.  Note that this does not enforce total demand the same over a day.
		// Note, we can only moderate based on cost signal
		// if we receive it (i.e. if we have smart meter)
		// TODO: may have to refine this - do we differentiate smart meter and smart display - i.e. whether receive only or Tx/Rx
		
		//if(hasSmartMeter && getPredictedCostSignalLength() > 0)
		if(getPredictedCostSignalLength() > 0)
		{
			if (Consts.DEBUG) System.out.println("     timeSinceSigValid % getPredictedCostSignalLength()): "+timeSinceSigValid % getPredictedCostSignalLength());
			double predictedCostNow = getPredictedCostSignal()[timeSinceSigValid % getPredictedCostSignalLength()];
			if (predictedCostNow == 1)
				if (Consts.DEBUG) System.out.println("    ********  predictedCost is ONE ****** ");
			if (Consts.DEBUG) System.out.println("     predictedCost (cost Signal): "+predictedCostNow);
			if (Consts.DEBUG) System.out.println("     dailyElasticity : "+dailyElasticity[time % this.mainContext.ticksPerDay]);
			if (predictedCostNow == 1)
				myDemand = myDemand * (1 - ((predictedCostNow / Consts.NORMALIZING_MAX_COST) * dailyElasticity[time % this.mainContext.ticksPerDay]));
			if (Consts.DEBUG)
			{
				if (Consts.DEBUG) System.out.println("    initialDemand: "+initialDemand);
				if (Consts.DEBUG) System.out.println("    demand multiplier " + (1 - ((predictedCostNow / Consts.NORMALIZING_MAX_COST) * dailyElasticity[time % this.mainContext.ticksPerDay])) );
				if (Consts.DEBUG) System.out.println("    set finalDemand: " + myDemand);
			}
		}

		return myDemand;
	}
	
	private double evaluateElasticBehaviour3_Peter(int time)
	{
		if (Consts.DEBUG) System.out.println("HHProsumer:: evaluateElasticBehaviour3 --- time: "+time);
		
		double demand =0;
		if (modifiedDemandProfile != null) {
			if (Consts.DEBUG) System.out.println(" modifiedDemandProfile: "+ Arrays.toString(modifiedDemandProfile));

			demand = modifiedDemandProfile[time % modifiedDemandProfile.length];
			if (Consts.DEBUG) System.out.println("   modifiedDemandProfile vlaue @ index " + (time % modifiedDemandProfile.length));
		}
		else demand = arr_otherDemandProfile[time % arr_otherDemandProfile.length];
		
		if (Consts.DEBUG) System.out.println(" set finalDemand: " + demand);
		return demand;

	}

	/**
	 * Evaluates the net demand mediated by smart controller behaviour at a given tick.
	 * 
	 * NOTE: 	As implemented - this method enforces total demand parity over a given day (i.e.
	 * 			integral of netDemand over a day is not necessarily constant.
	 * 
	 * @param 	int time	- the simulation time in ticks at which to evaluate this prosumer's behaviour
	 * @return 	double myDemand		- the demand for this half hour, mediated via behaviour change
	 */
	private double smartDemand(int time)
	{

		// Evaluate behaviour applies elasticity behaviour to the base
		// (non-displaceable) load.
		double currentBase = evaluateElasticBehaviour(time);
		//if (Consts.DEBUG) System.out.println("smartDemand: currentBase: "+currentBase);
		
		double currentCold = coldApplianceDemand();
		//if (Consts.DEBUG) System.out.println("smartDemand: currentCold: "+currentCold);

		double currentWet = wetApplianceDemand();
		//if (Consts.DEBUG) System.out.println("smartDemand: currentWet: "+currentWet);

		double currentHeat = heatingDemand();
		//if (Consts.DEBUG) System.out.println("smartDemand: currentHeat: "+currentHeat);

		
		historicalBaseDemand[time % this.mainContext.ticksPerDay] = currentBase;
		historicalWetDemand[time % this.mainContext.ticksPerDay] = currentWet;
		historicalColdDemand[time % this.mainContext.ticksPerDay] = currentCold;
		historicalSpaceHeatDemand[time % this.mainContext.ticksPerDay] = currentHeat - getWaterHeatProfile()[time % this.mainContext.ticksPerDay];
		historicalWaterHeatDemand[time % this.mainContext.ticksPerDay] = getWaterHeatProfile()[time % this.mainContext.ticksPerDay];

		double returnAmount = currentBase + currentCold + currentWet + currentHeat;
		//if (Consts.DEBUG) System.out.println("smartDemand: returnAmount: "+returnAmount);

		
		if (Consts.DEBUG)
		{
			if (returnAmount != 0)
			{
				if (Consts.DEBUG) System.out.println("HHProsumer:: Total demand (not net against generation) " + returnAmount);
			}
		}
		return returnAmount;
	}

	private void learnBehaviourChange()
	{
		// TODO: Implement the behavioural (social?) learning in here
	}

	private void smartControlLearn(int time)
	{
		// smart device (optimisation) learning in here
		// in lieu of knowledge of what can "switch off" and "switch on", we assume that
		// the percentage moveable of the day's consumption is what may be time shifted
		double moveableLoad = inelasticTotalDayDemand * percentageMoveableDemand;
		double [] daysCostSignal = new double [this.mainContext.ticksPerDay];
		double [] daysOptimisedDemand = new double [this.mainContext.ticksPerDay];
		if ((Boolean) RepastEssentials.GetParameter("verboseOutput"))
		{
			if (Consts.DEBUG) System.out.println("HHProsumer: predictedCostSignal "+getPredictedCostSignal()+" time "+time+ " predictionValidTime "+predictionValidTime+" daysCostSignal "+ daysCostSignal +" this.mainContext.ticksPerDay "+this.mainContext.ticksPerDay);
		}
		System.arraycopy(getPredictedCostSignal(), time - this.predictionValidTime, daysCostSignal, 0, this.mainContext.ticksPerDay);

		System.arraycopy(smartOptimisedProfile, time % smartOptimisedProfile.length, daysOptimisedDemand, 0, this.mainContext.ticksPerDay);

		double [] tempArray = ArrayUtils.mtimes(daysCostSignal, daysOptimisedDemand);

		double currentCost = ArrayUtils.sum(tempArray);
		// Algorithm to minimise this whilst satisfying constraints of
		// maximum movable demand and total demand being inelastic.

		double movedLoad = 0;
		double movedThisTime = -1;
		double swapAmount = -1;
		while (movedLoad < moveableLoad && movedThisTime != 0)
		{
			int maxIndex = ArrayUtils.indexOfMax(tempArray);
			int minIndex = ArrayUtils.indexOfMin(tempArray);
			swapAmount = (daysOptimisedDemand[minIndex] + daysOptimisedDemand[maxIndex]) / 2;
			movedThisTime = ((daysOptimisedDemand[maxIndex] - daysOptimisedDemand[minIndex]) / 2);
			movedLoad = movedLoad + movedThisTime;
			daysOptimisedDemand[maxIndex] = swapAmount;
			daysOptimisedDemand[minIndex] = swapAmount;
			if (Consts.DEBUG)
			{
				if (Consts.DEBUG) System.out.println("HHProsumer:: "+ agentID + " moving " + movedLoad + "MaxIndex = " + maxIndex + " minIndex = " + minIndex + Arrays.toString(tempArray));
			}
			tempArray = ArrayUtils.mtimes(daysOptimisedDemand, daysCostSignal);			                   	                                             
		}
		System.arraycopy(daysOptimisedDemand, 0, smartOptimisedProfile, time % smartOptimisedProfile.length, this.mainContext.ticksPerDay);
		if (Consts.DEBUG)
		{
			if (ArrayUtils.sum(daysOptimisedDemand) != inelasticTotalDayDemand)
			{
				//TODO: This always gets triggerd - I wonder if the "day" i'm taking
				//here and in the inelasticdemand method are "off-by-one"
				if (Consts.DEBUG) System.out.println("HHProsumer:: optimised signal has varied the demand !!! In error !" + (ArrayUtils.sum(daysOptimisedDemand) - inelasticTotalDayDemand));
			}

			if (Consts.DEBUG) System.out.println("HHProsumer:: Saved " + (currentCost - ArrayUtils.sum(tempArray)) + " cost");
		}
	}

	private void learnSmartAdoptionDecision(int time)
	{

		// TODO: implement learning whether to adopt smart control in here
		// Could be a TpB based model.
		double inwardInfluence = 0;
		double internalInfluence = this.HEMSPropensity;
		Iterable socialConnections = FindNetwork("socialNetwork").getInEdges(this);
		// Get social influence - note communication is not every tick
		// hence the if clause
		if ((time % (21 * this.mainContext.ticksPerDay)) == 0)
		{

			for (Object thisConn: socialConnections)
			{
				RepastEdge myConn = ((RepastEdge) thisConn);
				if (((HHProsumer) myConn.getSource()).hasSmartControl)
				{

					inwardInfluence = inwardInfluence +  myConn.getWeight() * ((HHProsumer) myConn.getSource()).transmitPropensitySmartControl;
				}
			}
		}

		double decisionCriterion = inwardInfluence + internalInfluence;
		this.HEMSPropensity = decisionCriterion;
		if(decisionCriterion > (Double) GetParameter("smartControlDecisionThreshold")) 
		{
			hasSmartControl = true;
		}
	}

	/**
	 * This method uses rule set as described in Boait et al draft paper to assign
	 * cold appliance ownership on a stochastic, but statistically representative, basis.
	 * 
	 * TODO: consider where this code should be placed.
	 */
	private void setUpColdApplianceOwnership()
	{
		// Set up cold appliance ownership
		if(RandomHelper.nextDouble() < 0.651)
		{
			this.hasFridgeFreezer = true;
			if (RandomHelper.nextDouble() < 0.15)
			{
				this.hasRefrigerator = true;
			}
		}
		else
		{
			if (RandomHelper.nextDouble() < 0.95)
			{
				this.hasRefrigerator = true;
			}
			if (RandomHelper.nextDouble() < 0.835)
			{
				this.hasUprightFreezer = true;
			}
		}

		if (RandomHelper.nextDouble() < 0.163)
		{
			this.hasChestFreezer = true;
		}
	}
	

	/**
	 * @param waterHeatProfile the waterHeatProfile to set
	 */
	public void setWaterHeatProfile(double[] waterHeatProfile) {
		this.waterHeatProfile = waterHeatProfile;
	}

	/**
	 * @return the waterHeatProfile
	 */
	public double[] getWaterHeatProfile() {
		return waterHeatProfile;
	}
	
	
	/*
	 * Overrides parent receiveValueSignal
	 */
	
	/*
	public boolean receiveValueSignal(double[] signal, int length) {
		if (Consts.DEBUG) System.out.println("HHProsumer:: receiveValueSignal()");

		boolean recievedSuccessfuly = true;
		// Can only receive if we have a smart meter to receive data
		int validTime = (int) RepastEssentials.GetTickCount();
	
		if (hasSmartMeter)
		{
			// Note the time from which the signal is valid.
			// Note - Repast can cope with fractions of a tick (a double is returned)
			// but I am assuming here we will deal in whole ticks and alter the resolution should we need
			int time = (int) RepastEssentials.GetTickCount();
			int newSignalLength = length;
			setPredictionValidTime(validTime);
			double[] tempArray;

			int signalOffset = time - validTime;
			//if (Consts.DEBUG) System.out.println("time: "+time+ " validTime"+validTime);
			if (signalOffset != 0)
			{
				if (Consts.DEBUG)
				{
					if (Consts.DEBUG) System.out.println("ProsumerAgent: Signal valid from time other than current time");
				}
				newSignalLength = newSignalLength - signalOffset;
			}

			if ((getPredictedCostSignal() == null) || (newSignalLength != predictedCostSignal.length))
			{
				if (Consts.DEBUG)
				{
					if (Consts.DEBUG) System.out.println("ProsumerAgent: Re-defining length of signal in agent" + agentID);
				}
				setPredictedCostSignal(new double[newSignalLength]);
			}

			if (signalOffset < 0)
			{
				// This is a signal projected into the future.
				// pad the signal with copies of what was in it before and then add the new signal on
				System.arraycopy(signal, 0, getPredictedCostSignal(), 0 - signalOffset, length);
			}
			else
			{
				// This was valid from now or some point in the past.  Copy in the portion that is still valid and 
				// then "wrap" the front bits round to the end of the array.
				System.arraycopy(signal, signalOffset, predictedCostSignal, 0, length);
			}
		}

		return recievedSuccessfuly;
	}  */
	
	
	/*
	 * overrid 
	 */

	public boolean receiveValueSignal(double[] signal, int length) {
		if (Consts.DEBUG) System.out.println("HHProsumerAgent:: override receiveValueSignal()");
		boolean success = true;
		// Can only receive if we have a smart meter to receive data
		int validTime = (int) RepastEssentials.GetTickCount();
		//if (Consts.DEBUG) System.out.println(validTime+ " HHProsumer recive signal at ticktime "+ RepastEssentials.GetTickCount());
		//if (Consts.DEBUG) System.out.println("This prosumer hasSmartMeter = " + hasSmartMeter + " and receives signal " + Arrays.toString(signal));
		
		if (hasSmartMeter)
		{
			// Note the time from which the signal is valid.
			// Note - Repast can cope with fractions of a tick (a double is returned)
			// but I am assuming here we will deal in whole ticks and alter the resolution should we need
			int time = (int) RepastEssentials.GetTickCount();
			int newSignalLength = length;
			setPredictionValidTime(validTime);
			double[] tempArray;

			int signalOffset = time - validTime;
			//if (Consts.DEBUG) System.out.println("time: "+time+ " validTime"+validTime);
			if (signalOffset != 0)
			{
				if (Consts.DEBUG)
				{
					if (Consts.DEBUG) System.out.println("ProsumerAgent: Signal valid from time other than current time");
				}
				newSignalLength = newSignalLength - signalOffset;
			}

			if ((getPredictedCostSignal() == null) || (newSignalLength != predictedCostSignal.length))
			{
				if (Consts.DEBUG)
				{
					if (Consts.DEBUG) System.out.println("ProsumerAgent: Re-defining length of signal in agent" + agentID);
				}
				setPredictedCostSignal(new double[newSignalLength]);
			}

			if (signalOffset < 0)
			{
				// This is a signal projected into the future.
				// pad the signal with copies of what was in it before and then add the new signal on
				System.arraycopy(signal, 0, getPredictedCostSignal(), 0 - signalOffset, length);
			}
			else
			{
				// This was valid from now or some point in the past.  Copy in the portion that is still valid and 
				// then "wrap" the front bits round to the end of the array.
				System.arraycopy(signal, signalOffset, predictedCostSignal, 0, length);
			}
			
			if (Consts.DEBUG) System.out.println(" signal Recieved: "+ Arrays.toString(signal));
			if (Consts.DEBUG) System.out.println(" predictedCostSignal: "+ Arrays.toString(predictedCostSignal));

			if (Consts.DEBUG) System.out.println(" baseDemandProfile: "+ Arrays.toString(arr_otherDemandProfile));
			int maxIndex = ArrayUtils.indexOfMax(predictedCostSignal);
			//int indexOf1 = ArrayUtils.indexOf(baseDemandProfile, 1);
			if (Consts.DEBUG) System.out.println(" maxIndex: "+ maxIndex);
			double valueOfMaxIndex = arr_otherDemandProfile[maxIndex];
			if (Consts.DEBUG) System.out.println(" valueOfMaxIndex "+ valueOfMaxIndex);
			modifiedDemandProfile = new double[this.mainContext.ticksPerDay];
			modifiedDemandProfile[maxIndex] = 0; 
			double demandToShiftEqually = valueOfMaxIndex/47;
			
			if (Consts.DEBUG) System.out.println(" demandToShiftEqually "+ demandToShiftEqually);
			
			for (int j = maxIndex+1; j < this.mainContext.ticksPerDay; j++) {
				
				modifiedDemandProfile[j] = arr_otherDemandProfile[j]+ demandToShiftEqually;

			}
			
			for(int j = maxIndex-1; j >= 0; --j) {
				modifiedDemandProfile[j] = arr_otherDemandProfile[j]+ demandToShiftEqually;
			}
			
			if (Consts.DEBUG) System.out.println(" after: modifiedDemandProfile: "+ Arrays.toString(modifiedDemandProfile));
			
			//System.arraycopy(modifiedDemandProfile, 0, baseDemandProfile, 0, baseDemandProfile.length);
			
			//if (Consts.DEBUG) System.out.println(" baseDemandProfile: "+ Arrays.toString(baseDemandProfile));

			
		}

		return success;
	}
	
	//@ScheduledMethod(start = 0, interval = 0, shuffle = true, priority = Consts.PROSUMER_INIT_PRIORITY_FIRST)
	@ScheduledMethod(start = 0, interval = 0, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void init() {
		
		//if (Consts.DEBUG) System.out.println("pppppppppppppp HHProsumer::step() pppppppppppp");
		if (Consts.DEBUG) System.out.println("    ---iiii----- HHProsumer: init() ---iii------ DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );

		time = (int) RepastEssentials.GetTickCount();
		timeOfDay = (time % this.mainContext.ticksPerDay);
		//setNetDemand(baseDemandProfile[time % baseDemandProfile.length]);
		
		//evaluateElasticBehaviour(time);
		
		/* ------------------
		 * have price elasticity behavior 
		 */
		if (Consts.DEBUG) System.out.println(" dayCount: "+mainContext.getDayCount());
		if (Consts.DEBUG) System.out.println(" tickTime: "+mainContext.getTickCount());
		if (Consts.DEBUG) System.out.println(" timeslot: "+mainContext.getTimeslotOfDay());
		setNetDemand(evaluateElasticBehaviour(time));
		
		if (Consts.DEBUG) System.out.println("   ---iii----- HHProsumer: init() END ----iii------ DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );


	}
	
	/******************
	 * This method defines the step behaviour of a prosumer agent
	 * 
	 * Input variables: none
	 * 
	 ******************/
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = Consts.PROSUMER_PRIORITY_FIFTH)
	public void step() {
		
		//if (Consts.DEBUG) System.out.println("pppppppppppppp HHProsumer::step() pppppppppppp");
		if (Consts.DEBUG) System.out.println("    -------- HHProsumer: step() ---------- DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );

		time = (int) RepastEssentials.GetTickCount();
		timeOfDay = (time % this.mainContext.ticksPerDay);
		//setNetDemand(baseDemandProfile[time % baseDemandProfile.length]);
		
		//evaluateElasticBehaviour(time);
		
		/* ------------------
		 * have price elasticity behavior 
		 */
		if (Consts.DEBUG) System.out.println(" dayCount: "+mainContext.getDayCount());
		if (Consts.DEBUG) System.out.println(" tickTime: "+mainContext.getTickCount());
		if (Consts.DEBUG) System.out.println(" timeslot: "+mainContext.getTimeslotOfDay());
		//setNetDemand(evaluateElasticBehaviour2(time));
		
		setNetDemand(evaluateElasticBehaviour3_Peter(time));

		
		if (Consts.DEBUG) System.out.println("     -------- HHProsumer: END ---------- DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );


		
	/*
		checkWeather(time);
		this.setPoint = this.optimisedSetPointProfile[time % this.mainContext.ticksPerDay];

		//Do all the "once-per-day" things here
		if (timeOfDay == 0)
		{
			//TODO: decide whether the inelastic day demand is something that needs
			// calculating here
			inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);
			//if (Consts.DEBUG) System.out.println(" inelasticTotalDayDemand: "+inelasticTotalDayDemand);
			if (hasSmartControl){
				mySmartController.update(time);
				currentSmartProfiles = mySmartController.getCurrentProfiles();
				ArrayUtils.replaceRange(this.coldApplianceProfile, (double[]) currentSmartProfiles.get("ColdApps"),time % this.coldApplianceProfile.length);
				this.optimisedSetPointProfile = (double[]) currentSmartProfiles.get("HeatPump");
				//if (Consts.DEBUG) System.out.println("CSP (HeatPump)" + Arrays.toString(optimisedSetPointProfile));
				this.setWaterHeatProfile((double[]) currentSmartProfiles.get("WaterHeat"));
				//if (Consts.DEBUG) System.out.println("CSP (WaterHeat)" + Arrays.toString((double[])currentSmartProfiles.get("WaterHeat")));

			}


			if (sampleOutput != null)
			{
				sampleOutput.appendText("timeStep " + time);
				sampleOutput.appendText(Arrays.toString(baseDemandProfile));
				sampleOutput.writeColHeaders(new String[]{"pumpSwitching", "wetApp","coldApp"});
				String[][] outputBuilder = new String[4][spaceHeatPumpOn.length];
				outputBuilder[0] = ArrayUtils.convertFloatArrayToString(spaceHeatPumpOn);
				outputBuilder[1] = ArrayUtils.convertFloatArrayToString(recordedHeatPumpDemand);
				outputBuilder[2] = ArrayUtils.convertFloatArrayToString(wetApplianceProfile);
				outputBuilder[3] = ArrayUtils.convertFloatArrayToString(coldApplianceProfile);
				sampleOutput.appendCols(outputBuilder);
			}

		}

		//Every step we do these actions

		if (hasSmartControl){
			//if (Consts.DEBUG) System.out.println(" *has smartControl");
			setNetDemand(smartDemand(time));
			
		}
		else if (hasSmartMeter && exercisesBehaviourChange) {
			//if (Consts.DEBUG) System.out.println(" **has smartMeter & BehC");

			learnBehaviourChange();
			setNetDemand(evaluateElasticBehaviour(time));
			learnSmartAdoptionDecision(time);
		}
		else
		{
			//No adaptation case
			//if (Consts.DEBUG) System.out.println(" ***else adaption case");
			setNetDemand(baseDemandProfile[time % baseDemandProfile.length] - currentGeneration());

			learnSmartAdoptionDecision(time);
		}

		//After the heat input has been calculated, re-calculate the internal temperature of the house
		recordInternalAndExternalTemp(time);
		*/

	}


	/**
	 * Constructor
	 * 
	 * Creates a prosumer agent representing a household within the given context and with
	 * a basic demand profile as passed into the constructor.
	 * 
	 * @param context - the context within which this agent exists
	 * @param baseDemand - a floating point array containing the base demand for this prosumer.  Can be arbitrary length.
	 */
	public HHProsumer(CascadeContext context, double[] baseDemand) {
		super(context);
		this.setAgentName("Household_" + agentID);
		
		if (baseDemand.length % this.mainContext.ticksPerDay != 0)
		{
			System.err.println("HHProsumer: baseDemand array not a whole number of days");
			System.err.println("HHProsumer: Will be truncated and may cause unexpected behaviour");
		}
		
		this.arr_otherDemandProfile = new double [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.arr_otherDemandProfile, 0, baseDemand.length);
		
		//+++++++++++++++++++++
		this.dailyElasticity = new double[this.mainContext.ticksPerDay];
		
		for (int i = 0; i < dailyElasticity.length; i++)
		{
			//dailyElasticity[i] = (double) RandomHelper.nextDoubleFromTo(0, 0.1);
			dailyElasticity[i] =  0.1d;
		}
		
		this.setPredictedCostSignal(Consts.ZERO_COST_SIGNAL);
		
		hasSmartMeter = true;
		
		//time = (int) RepastEssentials.GetTickCount();
		//timeOfDay = (time % this.mainContext.ticksPerDay);
			
		//setNetDemand(baseDemandProfile[0]);
		
		
		//if (Consts.DEBUG) System.out.println("HHProsumer BD file:"+ Arrays.toString(baseDemandProfile));
		
		/*
		this.percentageMoveableDemand = (double) RandomHelper.nextDoubleFromTo(0, Consts.MAX_DOMESTIC_MOVEABLE_LOAD_FRACTION);
		this.this.mainContext.ticksPerDay = context.getNbOfTickPerDay();
		this.setNumOccupants(context.occupancyGenerator.nextInt() + 1);
		//Assign hot water storage capacity - note based on EST report, page 9
		this.dailyHotWaterUsage = (double) context.waterUsageGenerator.nextDouble(Consts.EST_INTERCEPT + (this.numOccupants * Consts.EST_SLOPE), Consts.EST_STD_DEV);

		this.waterSetPoint = Consts.DOMESTIC_SAFE_WATER_TEMP;
		//TODO: something more sophisticated to give the baseline water heat requirement
		double[] hotWaterNeededProfile = new double[this.mainContext.getNbOfTickPerDay()];
		double drawOffPerOccupant = this.dailyHotWaterUsage / this.numOccupants;

		for (int i = 0; i < this.numOccupants; i++)
		{
			hotWaterNeededProfile[this.mainContext.drawOffGenerator.nextInt()] = drawOffPerOccupant;	
		}

		this.baselineHotWaterVolumeProfile = Arrays.copyOf(hotWaterNeededProfile, hotWaterNeededProfile.length);
		
		if(hasElectricalWaterHeat)
		{
			this.setWaterHeatProfile(ArrayUtils.multiply(hotWaterNeededProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (this.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP) ));
		}
		else
		{
			double[] noWaterHeating = new double[this.mainContext.getNbOfTickPerDay()];
			Arrays.fill(noWaterHeating,0);
			this.setWaterHeatProfile(noWaterHeating);
		}


		this.ratedPowerHeatPump = Consts.TYPICAL_HEAT_PUMP_ELEC_RATING;


		this.setPointProfile = new double[this.mainContext.ticksPerDay];
		Arrays.fill(setPointProfile, 20); // This puts in a flat set point through the day set by the consumer
		//this.setPointProfile = Arrays.copyOf(Consts.BASIC_AVERAGE_SET_POINT_PROFILE, Consts.BASIC_AVERAGE_SET_POINT_PROFILE.length);
		//		this.setPointProfile = ArrayUtils.offset(this.setPointProfile, (double) RandomHelper.nextDoubleFromTo(-2, 2));
		this.optimisedSetPointProfile = Arrays.copyOf(setPointProfile, setPointProfile.length);
		this.setPoint = optimisedSetPointProfile[0];
		this.currentInternalTemp = this.setPoint;
		this.setPredictedCostSignal(Consts.ZERO_COST_SIGNAL);
		setUpColdApplianceOwnership();
		this.dailyElasticity = new double[this.mainContext.ticksPerDay];

		//Arrays to hold some history - mainly for gui purposes
		this.historicalBaseDemand = new double[this.mainContext.ticksPerDay];
		this.historicalWetDemand = new double[this.mainContext.ticksPerDay];
		this.historicalColdDemand = new double[this.mainContext.ticksPerDay];
		this.historicalSpaceHeatDemand = new double[this.mainContext.ticksPerDay];
		this.historicalWaterHeatDemand = new double[this.mainContext.ticksPerDay];
		this.historicalIntTemp = new double[this.mainContext.ticksPerDay];
		this.historicalExtTemp = new double[this.mainContext.ticksPerDay];
		//TODO - get more thoughtful elasticity model than this random formulation
		for (int i = 0; i < dailyElasticity.length; i++)
		{
			dailyElasticity[i] = (float) RandomHelper.nextDoubleFromTo(0, 0.1);
		}

		if (baseDemand.length % this.mainContext.ticksPerDay != 0)
		{
			System.err.println("HHProsumer: baseDemand array not a whole number of days");
			System.err.println("HHProsumer: Will be truncated and may cause unexpected behaviour");
		}
		this.baseDemandProfile = new double [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.baseDemandProfile, 0, baseDemand.length);
		this.coldApplianceProfile = InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.DAYS_PER_YEAR, this.hasRefrigerator, this.hasFridgeFreezer, (this.hasUprightFreezer && this.hasChestFreezer));
		// Initialise the base case where heat pump is on all day
		spaceHeatPumpOn = new double[this.mainContext.ticksPerDay];
		Arrays.fill(spaceHeatPumpOn, 1);
		recordedHeatPumpDemand = new double[this.mainContext.ticksPerDay];

		
		//this.mySmartController = new WattboxController(this); //accept only HousheoldProsumer and not HHProsumer
		this.hasSmartControl = true; // Babak: this is also done in the populateContext() method of CascadeContextBuilder!

		//Initialise the smart optimised profile to be the same as base demand
		//smart controller will alter this
		this.smartOptimisedProfile = new double [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0, smartOptimisedProfile.length);

		//Richard test - just to monitor evolution of one agent
		if (this.agentID == 1)
		{
			sampleOutput = new CSVWriter("richardTestOutput.csv", false);
		}  */
	}


}
