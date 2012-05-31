package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.*;

import repast.simphony.engine.schedule.*;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.*;
import repast.simphony.space.graph.*;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.controllers.*;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import static repast.simphony.essentials.RepastEssentials.*;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.4 $ $Date: 2012/01/23 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial split of categories of prosumer from the abstract class representing all prosumers
 * 1.1 - 
 * 1.2 - add in more sophisticated model incorporating displaceable and non-displaceable demand, appliance
 * 		 ownership etc.  Richard
 * 1.3 - added heat pump model and temperature calculation model (Richard)
 * 1.4 - modified and added the parts that calculate the combined wet and cold profiles demand 
 *       which was previously only based on single profile (Babak)
 */
public class HouseholdProsumer extends ProsumerAgent{

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */
	
	private int lengthOfDemandProfile;

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
	private boolean hasElectricalWaterHeat = false;
	boolean hasElectricalSpaceHeat = false;
	boolean hasElectricVehicle = false;
	boolean hasElectricalStorage = false; // Do we need to break out various storage technologies?
	boolean hasHotWaterStorage = false;
	boolean hasSpaceHeatStorage = false;
	
	private boolean hasGas = false;

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
	private double dailyHotWaterUsage;
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
	public boolean hasFridgeFreezer = false;	
	public boolean hasRefrigerator = false;	
	public boolean hasUprightFreezer = false;	
	public boolean hasChestFreezer = false;		
	public boolean hasWashingMachine = false;	
	public boolean hasWasherDryer = false;	
	public boolean hasTumbleDryer = false;	
	public boolean hasDishWasher = false;
	

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
	private int numOccupants;
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
	WeakHashMap<String,double[]> currentSmartProfiles; 
	
	private double[] coldApplianceProfile;
	protected WeakHashMap<String,double[]> coldApplianceProfiles;
	
	public double[] wetApplianceProfile;
	protected WeakHashMap<String,double[]> wetApplianceProfiles;
	
	private double[] baselineHotWaterVolumeProfile;
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
	private double[] historicalIntTemp;
	private double[] historicalExtTemp;
	private double[] historicalWaterHeatDemand;
	public double freeRunningTemperatureLossPerTickMultiplier;

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
	
	public double[] getOtherDemandProfile() {
		return Arrays.copyOf(arr_otherDemandProfile, arr_otherDemandProfile.length);
	}


	public boolean isHasElectricVehicle() {
		return hasElectricVehicle;
	}
	
	public boolean isHasGas() {
		return hasGas;
	}

	public void setHasGas(boolean gas) {
		this.hasGas = gas;
	} 

	public boolean isHasElectricalWaterHeat() {
		return hasElectricalWaterHeat;
	}

	public void setHasElectricalWaterHeat(boolean electricalWaterHeat) {
		this.hasElectricalWaterHeat = electricalWaterHeat;
	}

	public boolean isHasElectricalSpaceHeat() {
		return hasElectricalSpaceHeat;
	}

	public void setHasElectricalSpaceHeat(boolean electricalSpaceHeat) {
		this.hasElectricalSpaceHeat = electricalSpaceHeat;
	}
	
	public boolean isHasWashingMachine() {
		return hasWashingMachine;
	}

	public void setHasWashingMachine(boolean washingMachine) {
		this.hasWashingMachine = washingMachine;
	}

	public boolean isHasWasherDryer() {
		return hasWasherDryer;
	}

	public void setHasWasherDryer(boolean washerDryer) {
		this.hasWasherDryer = washerDryer;
	}

	public boolean isHasTumbleDryer() {
		return hasTumbleDryer;
	}

	public void setHasTumbleDryer(boolean tumbleDryer) {
		this.hasTumbleDryer = tumbleDryer;
	}

	public boolean isHasDishWasher() {
		return hasDishWasher;
	}

	public void setHasDishWasher(boolean dishWasher) {
		this.hasDishWasher = dishWasher;
	}
	
	public boolean isHasWetAppliances() {
		boolean hasWetAppliances = false;

		hasWetAppliances = (hasWashingMachine || hasWasherDryer ||
				            hasTumbleDryer || hasDishWasher );
		
		return hasWetAppliances;
	}

	
	public boolean isHasRefrigerator() {
		return this.hasRefrigerator;
	}
	
	public boolean isHasFridgeFreezer() {
		return this.hasFridgeFreezer;
	}
	
	public boolean isHasUprightFreezer() {
		return this.hasUprightFreezer;
	}
	
	public boolean isHasChestFreezer() {
		return this.hasChestFreezer;
	}
	
	public boolean isHasColdAppliances() {
		
		boolean hasColdAppliances = false;
		
		 hasColdAppliances = (hasFridgeFreezer || hasRefrigerator 
				|| hasUprightFreezer ||	hasChestFreezer);

		return hasColdAppliances;
	}
	
	public void setColdAppliancesProfiles(WeakHashMap<String,double[]> coldProfile) {
		this.coldApplianceProfiles = coldProfile;
	}
	
	public WeakHashMap<String,double[]> getColdAppliancesProfiles() {
		return this.coldApplianceProfiles;
	}
	
	public double [] getColdAppliancesProfile() {
		return  calculateCombinedColdAppliancesProfile(getColdAppliancesProfiles());
	}
	
	
	public void setWetAppliancesProfiles(WeakHashMap<String,double[]> wetProfile) {
		this.wetApplianceProfiles = wetProfile;
	}
	
	public WeakHashMap<String,double[]> getWetAppliancesProfiles() {
		return this.wetApplianceProfiles;
	}
	
	public double [] getWetAppliancesProfile() {
		return  calculateCombinedWetAppliancesProfile(getWetAppliancesProfiles());
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
	public double[] getHistoricalOtherDemand() {
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
			if (returnAmount != 0 && Consts.DEBUG)
			{
				//System.out.println("HouseholdProsumer:: Generating " + returnAmount);
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
			return (Math.max((Math.min(getWindSpeed(), 12.5d) - 2.5d), 0)) / 20
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
			recordedHeatPumpDemand[timeOfDay] += calculateHeatPumpDemandAndInternalTemp(time) / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP;

		}

		// (20/01/12) Check if sum of <recordedHeatPumpDemand> is consistent at end day 
		if(timeOfDay == 47 && Consts.DEBUG)
			//System.out.println("SUM(RecordedHeatPumpDemand: " + ArrayUtils.sum(recordedHeatPumpDemand));
		
		if (hasElectricalWaterHeat)
		{
			// TODO: this assumes only space heat and always uses heat pump - expand for other forms of electrical heating
			recordedHeatPumpDemand[timeOfDay] += getWaterHeatProfile()[timeOfDay];

		}

		return recordedHeatPumpDemand[timeOfDay];
	}

	/**
	 * @return
	 */
	private double wetApplianceDemand() {
		if (this.isHasWetAppliances())
			return this.wetApplianceProfile[time % wetApplianceProfile.length];
		else return 0d;
	}

	/**
	 * @return
	 */
	private double coldApplianceDemand() 
	{
		if (this.isHasColdAppliances())
			return this.coldApplianceProfile[time % coldApplianceProfile.length];
		else return 0d;
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

		double demand = 0;
		double deltaT = this.currentInternalTemp - this.getContext().getAirTemperature(timeStep);

		double requiredTempChange = this.setPoint - currentInternalTemp;
		
		double maintenanceEnergy =  ((deltaT * (this.buildingHeatLossRate)) * ((double)(Consts.SECONDS_PER_DAY / ticksPerDay))) / Consts.KWH_TO_JOULE_CONVERSION_FACTOR;

		double heatingEnergy = requiredTempChange * this.buildingThermalMass / Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE;	// refer to Peter's email (24/01/12)
		
		if ((requiredTempChange < (0 - Consts.TEMP_CHANGE_TOLERANCE)) || (deltaT < Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF))
		{
			//heat pump off, leave demand at zero and decrement internal temperature
			this.currentInternalTemp -= maintenanceEnergy / this.buildingThermalMass;
			//if(Consts.DEBUG)
				//System.out.println("HouseholdProsumer:: HeatPump off - currentInternalTemp1: "+ currentInternalTemp);

		}
		else
		{
			demand = maintenanceEnergy + heatingEnergy;
			if (demand > ((this.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) *(double) 24 / ticksPerDay))
			{
				demand = (this.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) * ((double) 24 / ticksPerDay);
				this.currentInternalTemp = this.currentInternalTemp + ((demand - maintenanceEnergy) / this.buildingThermalMass);
				if (Consts.DEBUG)
				{
				//System.out.println("HouseholdProsumer:: Heatpump on max, can't regain set point currentInternalTemp2: "+ currentInternalTemp);
				}

			}
			else
			{
				this.currentInternalTemp = this.setPoint;
				

			}
		}
		//System.out.println("demand: "+ demand);
		//System.out.println("buildingHeatLossRate: " + buildingHeatLossRate);
		//System.out.println("buildingThermalMass: " + this.buildingThermalMass);

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
		//if (Consts.DEBUG) System.out.println(" baseDemandProfile "+ Arrays.toString(baseDemandProfile));
		//if (Consts.DEBUG) System.out.println(" baseProfileIndex "+ baseProfileIndex);
		//if (Consts.DEBUG) System.out.println("  calculateFixedDayTotalDemand: array2Sum: "+ Arrays.toString(Arrays.copyOfRange(baseDemandProfile,baseProfileIndex,baseProfileIndex+ticksPerDay - 1)));
		return ArrayUtils.sum(Arrays.copyOfRange(arr_otherDemandProfile,baseProfileIndex,baseProfileIndex+ticksPerDay - 1));
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
	 */
	private double evaluateElasticBehaviour(int time)
	{
		double myDemand;
		int timeSinceSigValid = time - predictionValidTime;

		//As a basic strategy only the base (non-displaceable) demand is
		//elastic
		
		
	 	/*double [] avgDemands7Days = {0.500677033,0.407725724,0.370908322,0.35877468,0.347866658,0.324998379,0.343484827,0.308885358,0.298035432,0.27947423,0.280343265,0.289909821,0.303676612,0.333950661,0.426824179,0.616930198,0.630372238,0.589740306,0.53584991,0.589862047,0.621102996,0.621623774,0.727205077,0.845539884,0.959784962,0.931100067,0.864783952,0.773823124,0.835309411,0.635960097,0.663524804,0.781160132,0.8361668,1.010964822,1.306060966,1.31329955,1.245892295,0.996678877,0.990747587,0.946006202,0.944353378,0.954267895,0.909130777,0.792284367,0.751724127,0.660532121,0.637187946,0.557241803};
	 	
	 	double [] avgDemands; 
	 	
		
		if (mainContext.getDayCount() < Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
			myDemand = baseDemandProfile[time % baseDemandProfile.length];
		else if (mainContext.getDayCount() >  Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE && mainContext.getDayCount()< Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE+ Consts.AGGREGATOR_TRAINING_PERIODE)
			myDemand = avgDemands7Days[time % avgDemands7Days.length];
		else myDemand = baseDemandProfile[time % baseDemandProfile.length];  */
		
		myDemand = arr_otherDemandProfile[time % arr_otherDemandProfile.length];
		//if (Consts.DEBUG) System.out.println("myDemand: "+myDemand); 

		// Adapt behaviour somewhat.  Note that this does not enforce total demand the same over a day.
		// Note, we can only moderate based on cost signal
		// if we receive it (i.e. if we have smart meter)
		// TODO: may have to refine this - do we differentiate smart meter and smart display - i.e. whether receive only or Tx/Rx
		
		if(hasSmartMeter && getPredictedCostSignalLength() > 0)
		{
			double predictedCostSignal = getPredictedCostSignal()[timeSinceSigValid % getPredictedCostSignalLength()];
			
			//if (mainContext.getDayCount() >  Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE+ Consts.AGGREGATOR_TRAINING_PERIODE)
				//predictedCostNow = predictedCostNow *1000;
			
			double myE = dailyElasticity[time % ticksPerDay];
			//double eChange = mainContext.hhProsumerElasticityTest.nextDouble();
			//if (Consts.DEBUG) System.out.println("eChange: "+ eChange);
			//myE = myE + eChange;
			//myDemand = myDemand * (1 - ((predictedCostNow / Consts.NORMALIZING_MAX_COST) * dailyElasticity[time % ticksPerDay]));
			
			//System.out.println("myDemand (before): "+myDemand);
			myDemand = myDemand * (1 - ((predictedCostSignal / Consts.NORMALIZING_MAX_COST) * myE));

			//System.out.println("myDemand: "+myDemand +", costSignal:"+predictedCostSignal);
			if (Consts.DEBUG)
			{
				//System.out.println("dailyElasticity: "+ Arrays.toString(dailyElasticity));
				//System.out.println("predictedCostSignal (all): "+ Arrays.toString(getPredictedCostSignal()));
				//System.out.println("predictedCostSignal: "+predictedCostSignal);
				//System.out.println("predictedCostNow * myE: "+predictedCostSignal * myE);
				//System.out.println("dailyElasticity[time % ticksPerDay]: "+dailyElasticity[time % ticksPerDay]);
				//System.out.println("HouseholdProsumer:: Based on predicted cost = " + predictedCostSignal + " demand set to " + (1 - ((predictedCostSignal / Consts.NORMALIZING_MAX_COST) * dailyElasticity[time % ticksPerDay])) + " of initial " );
			}
		}

		return myDemand;
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

		//if (Consts.DEBUG) System.out.println("HHPro: smartDemand() time: "+time);
		// Evaluate behaviour applies elasticity behaviour to the base
		// (non-displaceable) load.
		double currentBase = evaluateElasticBehaviour(time);

		double currentCold = coldApplianceDemand();
		
		//if (Consts.DEBUG) System.out.println("currentColdDemand is: "+currentCold);

		double currentWet = wetApplianceDemand();

		double currentHeat= 0;
		currentHeat = heatingDemand();


		historicalBaseDemand[time % ticksPerDay] = currentBase;
		historicalWetDemand[time % ticksPerDay] = currentWet;
		historicalColdDemand[time % ticksPerDay] = currentCold;

		//if (this.getHasElectricalSpaceHeat())
		if (this.isHasElectricalSpaceHeat() && this.isHasElectricalWaterHeat())
			historicalSpaceHeatDemand[time % ticksPerDay] = currentHeat - getWaterHeatProfile()[time % ticksPerDay]; //Verify this!
		else if (this.isHasElectricalSpaceHeat())
			historicalSpaceHeatDemand[time % ticksPerDay] = currentHeat;

		if (this.isHasElectricalWaterHeat())
			historicalWaterHeatDemand[time % ticksPerDay] = getWaterHeatProfile()[time % ticksPerDay];

		double returnAmount = currentBase + currentCold + currentWet + currentHeat;

     
		if (Consts.DEBUG)
		{
			if (returnAmount != 0)
			{
				//System.out.println("HouseholdProsumer:: Total demand (not net against generation) " + returnAmount);
			}
		}
		 
		//double returnAmount = currentBase;
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
		double [] daysCostSignal = new double [ticksPerDay];
		double [] daysOptimisedDemand = new double [ticksPerDay];
		if ((Boolean) RepastEssentials.GetParameter("verboseOutput"))
		{
			System.out.println("HouseholdProsumer: predictedCostSignal "+getPredictedCostSignal()+" time "+time+ " predictionValidTime "+predictionValidTime+" daysCostSignal "+ daysCostSignal +" ticksPerDay "+ticksPerDay);
		}
		System.arraycopy(getPredictedCostSignal(), time - this.predictionValidTime, daysCostSignal, 0, ticksPerDay);

		System.arraycopy(smartOptimisedProfile, time % smartOptimisedProfile.length, daysOptimisedDemand, 0, ticksPerDay);

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
			
			tempArray = ArrayUtils.mtimes(daysOptimisedDemand, daysCostSignal);			                   	                                             
		}
		System.arraycopy(daysOptimisedDemand, 0, smartOptimisedProfile, time % smartOptimisedProfile.length, ticksPerDay);
		if (Consts.DEBUG)
		{
			if (ArrayUtils.sum(daysOptimisedDemand) != inelasticTotalDayDemand)
			{
				//TODO: This always gets triggerd - I wonder if the "day" i'm taking
				//here and in the inelasticdemand method are "off-by-one"
				//if (Consts.DEBUG) System.out.println("HouseholdProsumer:: optimised signal has varied the demand !!! In error !" + (ArrayUtils.sum(daysOptimisedDemand) - inelasticTotalDayDemand));
			}

			//if (Consts.DEBUG) System.out.println("HouseholdProsumer:: Saved " + (currentCost - ArrayUtils.sum(tempArray)) + " cost");
		}
	}

	private void learnSmartAdoptionDecision(int time)
	{

		// TODO: implement learning whether to adopt smart control in here
		// Could be a TpB based model.
		double inwardInfluence = 0;
		double internalInfluence = this.HEMSPropensity;
		Iterable<?> socialConnections = FindNetwork("socialNetwork").getInEdges(this);
		// Get social influence - note communication is not every tick
		// hence the if clause
		if ((time % (21 * ticksPerDay)) == 0)
		{

			for (Object thisConn: socialConnections)
			{
				RepastEdge<?> myConn = ((RepastEdge<?>) thisConn);
				if (((HouseholdProsumer) myConn.getSource()).hasSmartControl)
				{

					inwardInfluence = inwardInfluence + (double) myConn.getWeight() * ((HouseholdProsumer) myConn.getSource()).transmitPropensitySmartControl;
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


	public void setBaselineHotWaterVolumeProfile(double[] aBaselineHotWaterVolumeProfile) {
		this.baselineHotWaterVolumeProfile = aBaselineHotWaterVolumeProfile;
	}
	
	public double[] getBaselineHotWaterVolumeProfile() {
		return this.baselineHotWaterVolumeProfile;
	}


	/**
	 * @param waterHeatProfile the waterHeatProfile to set
	 */
	public void setWaterHeatProfile(double[] waterHeatProfile) {
		this.waterHeatProfile = waterHeatProfile;
		//if (Consts.DEBUG) System.out.println("setWaterHeatProfile:    "+ Arrays.toString(waterHeatProfile));
	}

	/**
	 * @return the waterHeatProfile
	 */
	public double[] getWaterHeatProfile() {
		return waterHeatProfile;
	}


	//@ScheduledMethod(start = 0, interval = 0, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	/*
	public void init() {
		// Note the simulation time if needed.
		// Note - Repast can cope with fractions of a tick (a double is returned)
		// but I am assuming here we will deal in whole ticks and alter the resolution should we need
		
		if (Consts.DEBUG) System.out.println("    ---iiii----- HouseholdProsumer: init() ---iii------ DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );

		time = (int) RepastEssentials.GetTickCount();
		timeOfDay = (time % ticksPerDay);

		checkWeather(time);


		//Do all the "once-per-day" things here
		if (timeOfDay == 0)
		{
			//TODO: decide whether the inelastic day demand is something that needs
			// calculating here
			inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);
			if (hasSmartControl){
				mySmartController.update(time);
				currentSmartProfiles = mySmartController.getCurrentProfiles();
				if (this.getHasColdAppliances())
					ArrayUtils.replaceRange(this.coldApplianceProfile, (double[]) currentSmartProfiles.get("ColdApps"),time % this.coldApplianceProfile.length);
				this.optimisedSetPointProfile = (double[]) currentSmartProfiles.get("HeatPump");
				this.setWaterHeatProfile((double[]) currentSmartProfiles.get("WaterHeat"));

			}


			//***Richard output test for prosumer behaviour***

			if (sampleOutput != null)
			{
				sampleOutput.appendText("timeStep " + time);
				sampleOutput.appendText(Arrays.toString(baseDemandProfile));
				sampleOutput.writeColHeaders(new String[]{"pumpSwitching", "wetApp","coldApp"});
				String[][] outputBuilder = new String[4][this.ticksPerDay];
				if (this.getHasElectricalSpaceHeat()) {
					outputBuilder[0] = ArrayUtils.convertDoubleArrayToString(spaceHeatPumpOn);
					outputBuilder[1] = ArrayUtils.convertDoubleArrayToString(recordedHeatPumpDemand);
				}
				if(this.getHasWetAppliances())
					outputBuilder[2] = ArrayUtils.convertDoubleArrayToString(wetApplianceProfile);
				if(this.getHasColdAppliances())
					outputBuilder[3] = ArrayUtils.convertDoubleArrayToString(coldApplianceProfile);
				sampleOutput.appendCols(outputBuilder);
			}

		}
		
		if (this.getHasElectricalSpaceHeat())
			this.setPoint = this.optimisedSetPointProfile[time % ticksPerDay];

		if (Consts.DEBUG)
		{
			System.out.println("HouseholdProsumer:: " + this.agentName + "Proceeding to everyday actions");
		}

		//Every step we do these actions

		if (hasSmartControl){
			setNetDemand(smartDemand(time));
			if (Consts.DEBUG) System.out.println("Agent " + this.agentName + "set netdemand at time " + time + " to " + this.getNetDemand());

		}
		else if (hasSmartMeter && exercisesBehaviourChange) {

			learnBehaviourChange();
			setNetDemand(evaluateElasticBehaviour(time));
			learnSmartAdoptionDecision(time);
		}
		else
		{
			//No adaptation case
			setNetDemand(baseDemandProfile[time % baseDemandProfile.length] - currentGeneration());

			learnSmartAdoptionDecision(time);
		}

		//After the heat input has been calculated, re-calculate the internal temperature of the house
		recordInternalAndExternalTemp(time);

		if (Consts.DEBUG) System.out.println("   ---iii----- HouseholdProsumer: init() END ----iii------ DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );


	} */


	public void initializeRandomlyDailyElasticityArray(double from, double to) {
		for (int i = 0; i < dailyElasticity.length; i++)  {
			dailyElasticity[i] = RandomHelper.nextDoubleFromTo(from, to);
		}
		/*
		// (13/02/12) DF
		// Print out elasticity factor for 1 prosumer for testing revising elasticity calculation
		System.out.print("Prosumer Elasticity: ");
		for(int i=0; i< dailyElasticity.length; i++) {
			System.out.print(dailyElasticity[i] + ",");
		}
		System.out.println("");
		*/
	}

	// this is temporary method for test (can be removed later)
	public void initializeSimilarlyDailyElasticityArray(double val) {
		for (int i = 0; i < dailyElasticity.length; i++)  {
			dailyElasticity[i] = val;
		}
	}
	
	
	public void setRandomlyPercentageMoveableDemand(double from, double to) {
		percentageMoveableDemand = RandomHelper.nextDoubleFromTo(from, to);
	}
	
	public void initializeElectWaterHeatPar() {
		
		// Estimate the daily hot water usage in accordance with the model found and described
		// in Energy Saving Trust Paper - Measurement of Domestic Hot Water
		// Consumption in Dwellings (2008) prepared for DEFRA.
		double dailyHotWaterUsage = mainContext.waterUsageGenerator.nextDouble(Consts.EST_INTERCEPT + (this.getNumOccupants() * Consts.EST_SLOPE), Consts.EST_STD_DEV);
		this.setDailyHotWaterUsage(dailyHotWaterUsage);
		
		this.ratedPowerHeatPump = Consts.TYPICAL_HEAT_PUMP_ELEC_RATING;

		this.waterSetPoint = Consts.DOMESTIC_SAFE_WATER_TEMP;
		
		//TODO: something more sophisticated to give the baseline water heat requirement
		
		/*
		 * TODO: the length of hot water profile (along with the heat space profile) are left
		 * unchanged at one single day (unlike others which are by default 1Y long). 
		 * This is because the initial design of heat space and water did not considered 
		 * variation of set points (etc) for more than a day. Increasing the size of array containing
		 * set points etc without adjustment in wattbox controller will cause problem. This needs to be
		 * addressed at later stage. 
		 * 
		 */
		
		//double[] hotWaterNeededProfile = new double[this.lengthOfDemandProfile];
		double[] hotWaterNeededProfile = new double[mainContext.ticksPerDay];
		
				
		//share the needed water equally between occupants
		double drawOffPerOccupant = dailyHotWaterUsage / this.getNumOccupants();

		//if (Consts.DEBUG) System.out.println("hotWaterNeededProfile: "+ Arrays.toString(hotWaterNeededProfile));

		// Assign one draw off per occupant, at a time generated from the statistics gathered
		// in the EST paper referenced above.
		for (int i = 0; i < this.getNumOccupants(); i++)  
		{
			hotWaterNeededProfile[mainContext.drawOffGenerator.nextInt()] = drawOffPerOccupant;	
		}
		
		//System.out.println("drawOffGenerator x 100times");
		//for (int i = 0; i < 100; i++)  {
		//	System.out.println(mainContext.drawOffGenerator.nextInt());
		//}
		
		//System.out.println("AFTER drawOffGen");
		//System.out.println("hotWaterNeededProfile: "+ Arrays.toString(hotWaterNeededProfile));

		double [] baselineHotWaterVolumeProfile = Arrays.copyOf(hotWaterNeededProfile, hotWaterNeededProfile.length);
		this.setBaselineHotWaterVolumeProfile(baselineHotWaterVolumeProfile);
		
		//if (Consts.DEBUG) System.out.println("setBaselineHotWaterVolumeProfile:    "+ Arrays.toString(baselineHotWaterVolumeProfile));


		if(this.isHasElectricalWaterHeat())	{
			this.setWaterHeatProfile(ArrayUtils.multiply(hotWaterNeededProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (this.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP)) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP) );
		}
		else {
			double[] noWaterHeating = new double[mainContext.ticksPerDay];
			//double[] noWaterHeating = new double[this.lengthOfDemandProfile];
			Arrays.fill(noWaterHeating,0);
			this.setWaterHeatProfile(noWaterHeating);
		}
	}
	
	public void initializeElecSpaceHeatPar() {
		
		this.minSetPoint = Consts.HOUSEHOLD_MIN_SETPOINT;
		this.maxSetPoint = Consts.HOUSEHOLD_MAX_SETPOINT;

		this.ratedPowerHeatPump = Consts.TYPICAL_HEAT_PUMP_ELEC_RATING;

		/*
		 * TODO: the length of space heat profile (along with the hot water profile) are left
		 * unchanged to one single day (unlike others which are by default 1Y long). 
		 * This is because the initial design of heat space (and hot water) did not considered 
		 * variation of set points (etc) for more than a day. Increasing the size of array containing
		 * set points without an adjustment to the wattbox controller will cause problem. 
		 * This needs to be addressed at later stage. 
		 * 
		 */
		this.setPointProfile = new double[ticksPerDay];
		//this.setPointProfile = new double[this.lengthOfDemandProfile];

		Arrays.fill(setPointProfile, 20); // This puts in a flat set point through the day set by the consumer
		//this.setPointProfile = Arrays.copyOf(Consts.BASIC_AVERAGE_SET_POINT_PROFILE, Consts.BASIC_AVERAGE_SET_POINT_PROFILE.length);
		//		this.setPointProfile = ArrayUtils.offset(this.setPointProfile, (double) RandomHelper.nextDoubleFromTo(-2, 2));
		this.optimisedSetPointProfile = Arrays.copyOf(setPointProfile, setPointProfile.length);
		this.setPoint = optimisedSetPointProfile[0];
		this.currentInternalTemp = this.setPoint;
		
		// Initialise the base case where heat pump is on all day
		spaceHeatPumpOn = new double[ticksPerDay];
		Arrays.fill(spaceHeatPumpOn, 1);
		
		
		setBuildingHeatLossRate/*(225f);//For test*/(mainContext.buildingLossRateGenerator.nextDouble());
		setBuildingThermalMass/*(10f);//For test*/(mainContext.thermalMassGenerator.nextDouble());
		
		tau = (buildingThermalMass  * Consts.KWH_TO_JOULE_CONVERSION_FACTOR) / buildingHeatLossRate;
		
		freeRunningTemperatureLossPerTickMultiplier = (buildingHeatLossRate / buildingThermalMass) / (Consts.KWH_TO_JOULE_CONVERSION_FACTOR / (Consts.SECONDS_PER_DAY / mainContext.ticksPerDay));
		
	}
	
	public void setWattboxController() {
	
		this.mySmartController = new WattboxController(this, this.mainContext);
	}
	
	private double[] calculateCombinedColdAppliancesProfile(WeakHashMap<String,double[]> coldProfiles) {
		double [] fridge_loads = (double []) coldProfiles.get(Consts.COLD_APP_FRIDGE);
		double [] freezer_loads = (double []) coldProfiles.get(Consts.COLD_APP_FREEZER);
		double [] fridge_freezer_loads = (double []) coldProfiles.get(Consts.COLD_APP_FRIDGEFREEZER);
		return ArrayUtils.add(fridge_loads, freezer_loads, fridge_freezer_loads);		
	}
	
	private double[] calculateCombinedWetAppliancesProfile(WeakHashMap<String,double[]> wetApplianceProfiles2) {
		double [] washer_loads = (double []) wetApplianceProfiles2.get(Consts.WET_APP_WASHER);
		double [] dryer_loads = (double []) wetApplianceProfiles2.get(Consts.WET_APP_DRYER);
		double [] dishwasher_loads = (double []) wetApplianceProfiles2.get(Consts.WET_APP_DISHWASHER);
		return ArrayUtils.add(washer_loads, dryer_loads, dishwasher_loads);		
	}
	
	/**
	 * This method is used to check whether the 'profile building' period has completed.
	 * the 'profile building' period (the initial part of the training period, usually 4-7 days) is 
	 * currently determined by a rarely changed variable in the Consts class
	 * @see Consts#AGGREGATOR_PROFILE_BUILDING_PERIODE
	 * @return true if the profile building period is completed, false otherwise 
	 */
	private boolean isAggregateDemandProfileBuildingPeriodCompleted() {
		boolean isEndOfProfilBuilding = true;
		int daysSoFar = mainContext.getDayCount();
		//if (Consts.DEBUG) System.out.println("ADPBPC: daySoFar: "+daysSoFar +" < 7?");

		if (daysSoFar < Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
			isEndOfProfilBuilding = false;
		return isEndOfProfilBuilding;	
	}
	
	/******************
	 * This method defines the step behaviour of a prosumer agent
	 * 
	 * Input variables: none
	 * 
	 ******************/
	@ScheduledMethod(start = 0, interval = 1, priority = Consts.PROSUMER_PRIORITY_FIFTH)
	//@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = Consts.PROSUMER_PRIORITY_FIFTH)

	public void step() {
		// Note the simulation time if needed.
		// Note - Repast can cope with fractions of a tick (a double is returned)
		// but I am assuming here we will deal in whole ticks and alter the resolution should we need

		//if (Consts.DEBUG) System.out.println("  -------- HouseholdProsumer(" +this.getAgentID()+") step() ---------- DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );
		
		time = (int) RepastEssentials.GetTickCount();
		timeOfDay = (time % ticksPerDay);

		//if (Consts.DEBUG) System.out.println("checkWeather");
		checkWeather(time);

		//Do all the "once-per-day" things here
		
		if ((timeOfDay == 0) && isHasColdAppliances()) {
			this.coldApplianceProfile = calculateCombinedColdAppliancesProfile(this.coldApplianceProfiles);
		}
		
		if ((timeOfDay == 0) && isHasWetAppliances()) {
			this.wetApplianceProfile = calculateCombinedWetAppliancesProfile(this.wetApplianceProfiles);
		}
		
		

		if (timeOfDay == 0 && isAggregateDemandProfileBuildingPeriodCompleted())
		//if (timeOfDay == 0)
		{
			//TODO: decide whether the inelastic day demand is something that needs
			// calculating here
			inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);
			//if (Consts.DEBUG) System.out.println("  HHpro: inelasticTotalDayDemand: "+ inelasticTotalDayDemand);
			
			if (hasSmartControl){
				
				//if (Consts.DEBUG) System.out.println("--beforCallToUpdate; time: "+time);
				//double [] cold_1day = Arrays.copyOfRange(coldApplianceProfile,(time % coldApplianceProfile.length) , (time % coldApplianceProfile.length) + ticksPerDay);
				//if (Consts.DEBUG) System.out.println("BEFORE cold_1day: "+ Arrays.toString(cold_1day));

				mySmartController.update(time);
				
				if (this.isHasColdAppliances())
					this.coldApplianceProfile = calculateCombinedColdAppliancesProfile(this.coldApplianceProfiles);
				
				if (this.isHasWetAppliances())
					this.wetApplianceProfile = calculateCombinedWetAppliancesProfile(this.wetApplianceProfiles);

				//cold_1day = Arrays.copyOfRange(coldApplianceProfile,(time % coldApplianceProfile.length) , (time % coldApplianceProfile.length) + ticksPerDay);
				//if (Consts.DEBUG) System.out.println("AFTER cold_1day: "+ Arrays.toString(cold_1day));

				currentSmartProfiles = mySmartController.getCurrentProfiles();
				
				//this.coldApplianceProfile = calculateCombinedColdAppliancesProfile(this.coldApplianceProfiles);

				this.optimisedSetPointProfile = (double[]) currentSmartProfiles.get("HeatPump");
				this.setWaterHeatProfile((double[]) currentSmartProfiles.get("WaterHeat"));

			}

			//***Richard output test for prosumer behaviour***

			if (sampleOutput != null && Consts.DEBUG)
			{
				sampleOutput.appendText("day: "+ mainContext.getDayCount() + ", timeStep " + time);
				sampleOutput.appendText("baseDemandProfile: ");
				sampleOutput.appendText(Arrays.toString(arr_otherDemandProfile));
				//sampleOutput.appendText("coldApplianceProfile: ");
				//sampleOutput.appendText(Arrays.toString(coldApplianceProfile));
				//sampleOutput.appendText("wetApplianceProfile: ");
				//sampleOutput.appendText(Arrays.toString(wetApplianceProfile));
				
				sampleOutput.writeColHeaders(new String[]{"spaceHeatPumpOn", "recordedHeatPumpDemand", "wetApp","coldApp"});
				String[][] outputBuilder = new String[4][ticksPerDay];
				if (this.isHasElectricalSpaceHeat()) {
					//String[][] outputBuilder = new String[4][spaceHeatPumpOn.length];
					outputBuilder[0] = ArrayUtils.convertDoubleArrayToString(spaceHeatPumpOn);
					outputBuilder[1] = ArrayUtils.convertDoubleArrayToString(recordedHeatPumpDemand);
				}
				if (this.isHasWetAppliances())
					outputBuilder[2] = ArrayUtils.convertDoubleArrayToString(wetApplianceProfile);
				if (this.isHasColdAppliances())
					outputBuilder[3] = ArrayUtils.convertDoubleArrayToString(calculateCombinedColdAppliancesProfile(this.coldApplianceProfiles));
				sampleOutput.appendCols(outputBuilder);
			}

		}
		
		//if (Consts.DEBUG) System.out.println("  HHpro: getHasElectricalSpaceHeat: "+ getHasElectricalSpaceHeat());

		if (this.isHasElectricalSpaceHeat())
			this.setPoint = this.optimisedSetPointProfile[timeOfDay];

		//Every step we do these actions
       
		if (hasSmartControl){
			setNetDemand(smartDemand(time));
		}
		
		else if (hasSmartMeter && exercisesBehaviourChange) {

			learnBehaviourChange();
			setNetDemand(evaluateElasticBehaviour(time));
			learnSmartAdoptionDecision(time);
		}
		else
		{
			//No adaptation case
			setNetDemand(arr_otherDemandProfile[time % arr_otherDemandProfile.length] - currentGeneration());

			learnSmartAdoptionDecision(time);
		}

		//After the heat input has been calculated, re-calculate the internal temperature of the house
		recordInternalAndExternalTemp(time);

		//if (Consts.DEBUG) System.out.println("     -------- HouseholdProsumer: END ---------- DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );
	
	}

	/**
	 * Constructor
	 * 
	 * Creates a prosumer agent representing a household within the given context and with
	 * a basic demand profile as passed into the constructor.
	 * 
	 * @param context - the context within which this agent exists
	 * @param otherDemandProfile - a floating point array containing the misc base demand (lightening, entertainment, computers, small appliances) for this prosumer.  Can be arbitrary length.
	 */
	public HouseholdProsumer(CascadeContext context, double[] otherDemandProfile) {
		super(context);
		
		//all the immutable must be initialized here
		//miscDemand includes aggregate of ligtening, entertainment, computers, & small appliances (Kettle, Toaster, Microwave)
		
		this.setAgentName("Household_" + agentID);
		this.ticksPerDay = context.getNbOfTickPerDay();
		
		 this.lengthOfDemandProfile = ticksPerDay*Consts.NB_OF_DAYS_LOADED_DEMAND;
        // this.lengthOfDemandProfile = context.getLengthOfDemandProfiles();
		
		
		if (otherDemandProfile.length % ticksPerDay != 0) {
			System.err.println("HouseholdProsumer: baseDemand array not a whole number of days");
			System.err.println("HouseholdProsumer: Will be truncated and may cause unexpected behaviour");
		}
		
		//this.arr_otherDemandProfile = new double [otherDemandProfile.length];
		
		this.arr_otherDemandProfile = new double [lengthOfDemandProfile];
		
		
		//System.arraycopy(otherDemandProfile, 0, this.arr_otherDemandProfile, 0, otherDemandProfile.length);
		
		System.arraycopy(otherDemandProfile, 0, this.arr_otherDemandProfile, 0, lengthOfDemandProfile);

		
		//Initialise the smart optimised profile to be the same as base demand
		//smart controller will alter this
		//this.smartOptimisedProfile = new double [otherDemandProfile.length];
		
		this.smartOptimisedProfile = new double [lengthOfDemandProfile];
		
		//System.arraycopy(otherDemandProfile, 0, this.smartOptimisedProfile, 0, smartOptimisedProfile.length);
		System.arraycopy(otherDemandProfile, 0, this.smartOptimisedProfile, 0, smartOptimisedProfile.length);
		
		//this.dailyElasticity = new double[ticksPerDay];
		this.dailyElasticity = new double[lengthOfDemandProfile];
		
		//Arrays to hold some history - mainly for gui purposes
		this.historicalBaseDemand = new double[ticksPerDay];
		this.historicalWetDemand = new double[ticksPerDay];
		this.historicalColdDemand = new double[ticksPerDay];
		this.historicalSpaceHeatDemand = new double[ticksPerDay];
		this.historicalWaterHeatDemand = new double[ticksPerDay];
		this.historicalIntTemp = new double[ticksPerDay];
		this.historicalExtTemp = new double[ticksPerDay];
		this.recordedHeatPumpDemand = new double[ticksPerDay];
		
		this.coldApplianceProfiles = new WeakHashMap<String, double[]>();
		
		
		//Richard test - just to monitor evolution of one agent
		/*if (this.agentID == 1)
		{
			sampleOutput = new CSVWriter("richardTestOutput.csv", false);
		} */
		
	}

}
