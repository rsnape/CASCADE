package uk.ac.dmu.iesd.cascade.context;

import java.io.*;
import java.math.*;
import java.util.*;

import javax.measure.unit.*;
import org.apache.tools.ant.taskdefs.Sync.MyCopy;
import org.hsqldb.lib.ArrayUtil;
import org.jfree.util.ArrayUtilities;
import org.jscience.mathematics.number.*;
import org.jscience.mathematics.vector.*;
import org.jscience.physics.amount.*;
//import cern.colt.Arrays;
import repast.simphony.adaptation.neural.*;
import repast.simphony.adaptation.regression.*;
import repast.simphony.context.*;
import repast.simphony.context.space.continuous.*;
import repast.simphony.context.space.gis.*;
import repast.simphony.context.space.graph.*;
import repast.simphony.context.space.grid.*;
import repast.simphony.engine.environment.*;
import repast.simphony.engine.schedule.*;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.*;
import repast.simphony.space.graph.*;
import repast.simphony.ui.RSApplication;
import repast.simphony.ui.RSGui;
import repast.simphony.visualization.ProbeEvent;
import repast.simphony.visualizationOGL2D.DisplayOGL2D;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.controllers.*;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.ui.ProsumerProbeListener;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.InitialProfileGenUtils;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.3 $ $Date: 2011/08/15 15:29:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial split of categories of prosumer from the abstract class representing all prosumers
 * 1.1 - 
 * 1.2 - add in more sophisticated model incorporating displaceable and non-displaceable demand, appliance
 * 		 ownership etc.  Richard
 * 1.3 - added heat pump model and temperature calculation model (Richard)
 */
public class HouseholdProsumer extends ProsumerAgent{

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
	float ratedPowerCHP;
	float ratedPowerAirSourceHeatPump;
	float ratedPowerGroundSourceHeatPump;
	float ratedPowerWind;
	float ratedPowerHydro;
	float ratedPowerThermalGeneration;
	float ratedPowerPV;
	float ratedPowerSolarWaterHeat;
	public float ratedPowerHeatPump;
	float ratedCapacityElectricVehicle; // Note kWh rather than kW
	float ratedCapacityElectricalStorage;   // Note kWh rather than kW
	float ratedCapacityHotWaterStorage;   // Note kWh rather than kW
	public float dailyHotWaterUsage;
	float ratedCapacitySpaceHeatStorage; // Note - related to thermal mass

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
	public float buildingThermalMass;
	/**
	 * Heat loss rate for the building (expressed in Watts per degree C, or Joules per second per deg C)
	 */
	public float buildingHeatLossRate;

	/*
	 * temperature control parameters
	 */
	float[] setPointProfile;
	float[] optimisedSetPointProfile;
	float setPoint;
	float minSetPoint;  // The minimum temperature for this Household's building in centigrade (where relevant)
	float maxSetPoint;  // The maximum temperature for this Household's building in centigrade (where relevant)
	public float waterSetPoint;
	private float currentInternalTemp;
	float currentWaterTemp;

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
	float percentageMoveableDemand;  // This can be set constant, or calculated from available appliances
	int maxTimeShift; // The "furthest" a demand may be moved in time.  This can be constant or calculated dynamically.
	/*
	 * Behavioural properties
	 */
	// Learner adoptSmartMeterLearner;
	// Learner adoptSmartControlLearner;
	// Learner consumptionPatternLearner;
	float transmitPropensitySmartControl;
	float transmitPropensityProEnvironmental;
	float visibilityMicrogen;

	/*
	 * This may or may not be used, but is a threshold cost above which actions
	 * take place for the household
	 */
	float costThreshold;

	/**
	 * Richard hack to get DEFRA profiles going
	 */
	float microgenPropensity;
	float insulationPropensity;
	float HEMSPropensity;
	float EVPropensity;
	float habit;
	int defraCategory;
	public float[] spaceHeatPumpOn; 
	private boolean waterHeaterOn;

	/**
	 * Simulation variables needed throughout
	 */
	int time;
	int timeOfDay;
	private float[] dailyElasticity;

	/**
	 * Available smart devices
	 */
	ISmartController mySmartController;
	WeakHashMap currentSmartProfiles;
	public float[] coldApplianceProfile;
	public float[] wetApplianceProfile;
	public float[] baselineHotWaterVolumeProfile;
	private float[] waterHeatProfile;

	//For ease of access to a debug type outputter
	CSVWriter sampleOutput;
	private float[] recordedHeatPumpDemand;

	//Arrays for the day's history (mainly for GUI - may use, write to file or something else in the future)
	private float[] historicalBaseDemand;
	private float[] historicalColdDemand;
	private float[] historicalWetDemand;
	private float[] historicalSpaceHeatDemand;

	/**
	 * Building heat flow time constant (thermal mass or specific heat capacity / heat loss rate)
	 */
	public float tau;
	private float timeSinceHeating = 0	;
	private float[] historicalIntTemp;
	private float[] historicalExtTemp;
	private float[] historicalWaterHeatDemand;
	public float freeRunningTemperatureLossPerTickMultiplier;



	/**
	 * Accessor functions (NetBeans style)
	 * TODO: May make some of these private to respect agent conventions of autonomy / realistic simulation of humans
	 */

	public float getCurrentInternalTemp() {
		return currentInternalTemp;
	}

	public float getSetPoint() {
		return setPoint;
	}

	public float[] getSetPointProfile() {
		return Arrays.copyOf(setPointProfile, setPointProfile.length);
	}
	
	/**
	 * @return
	 */
	public float[] getOptimisedSetPointProfile() {
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
	public float[] getHistoricalBaseDemand() {
		// TODO Auto-generated method stub
		return this.historicalBaseDemand;
	}

	/**
	 * @return
	 */
	public float[] getHistoricalColdDemand() {
		// TODO Auto-generated method stub
		return this.historicalColdDemand;
	}

	/**
	 * @return
	 */
	public float[] getHistoricalWetDemand() {
		// TODO Auto-generated method stub
		return this.historicalWetDemand;
	}

	/**
	 * @return
	 */
	public float[] getHistoricalSpaceHeatDemand() {
		// TODO Auto-generated method stub
		return this.historicalSpaceHeatDemand;
	}

	/**
	 * @return
	 */
	public float[] getHistoricalWaterHeatDemand() {
		// TODO Auto-generated method stub
		return this.historicalWaterHeatDemand;
	}

	/**
	 * @return
	 */
	public float[] getHistoricalIntTemp() {
		return historicalIntTemp;
	}

	/**
	 * @return
	 */
	public float[] getHistoricalExtTemp() {
		return historicalExtTemp;
	}

	/**
	 * @param buildingThermalMass the buildingThermalMass to set
	 */
	public void setBuildingThermalMass(float buildingThermalMass) {
		this.buildingThermalMass = buildingThermalMass;
	}

	/**
	 * @param buildingHeatLossRate the buildingHeatLossRate to set
	 */
	public void setBuildingHeatLossRate(float buildingHeatLossRate) {
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

	/******************
	 * This method defines the step behaviour of a prosumer agent
	 * 
	 * Input variables: none
	 * 
	 ******************/
	@ScheduledMethod(start = 0, interval = 1, shuffle = true)
	public void step() {
		// Note the simulation time if needed.
		// Note - Repast can cope with fractions of a tick (a double is returned)
		// but I am assuming here we will deal in whole ticks and alter the resolution should we need
		time = (int) RepastEssentials.GetTickCount();
		timeOfDay = (time % ticksPerDay);

		checkWeather(time);
		this.setPoint = this.optimisedSetPointProfile[time % ticksPerDay];

		//Do all the "once-per-day" things here
		if (timeOfDay == 0)
		{
			//TODO: decide whether the inelastic day demand is something that needs
			// calculating here
			inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);
			if (hasSmartControl){
				mySmartController.update(time);
				currentSmartProfiles = mySmartController.getCurrentProfiles();
				ArrayUtils.replaceRange(this.coldApplianceProfile, (float[]) currentSmartProfiles.get("ColdApps"),time % this.coldApplianceProfile.length);
				//this.wetApplianceProfile = (float[]) currentSmartProfiles.get("WetApps");
				this.optimisedSetPointProfile = (float[]) currentSmartProfiles.get("HeatPump");
				this.setWaterHeatProfile((float[]) currentSmartProfiles.get("WaterHeat"));
			}

			//***Richard output test for prosumer behaviour***

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
			setNetDemand(baseDemandProfile[time % baseDemandProfile.length] - currentGeneration());

			learnSmartAdoptionDecision(time);
		}

		//After the heat input has been calculated, re-calculate the internal temperature of the house
		recordInternalAndExternalTemp(time);
	}

	public float getUnadaptedDemand(){
		// Cope with tick count being null between project initialisation and start.
		int index = Math.max(((int) RepastEssentials.GetTickCount() % baseDemandProfile.length), 0);
		return (baseDemandProfile[index]) - currentGeneration();
	}

	/**
	 * Method to return the realtime generation of this Household Prosumer (as distinct
	 * from the net demand for the prosumer).
	 * 
	 * @return a float giving the realtime generation of this Household Prosumer
	 *
	 */
	private float currentGeneration() {
		float returnAmount = 0;

		returnAmount = returnAmount + CHPGeneration() + windGeneration() + hydroGeneration() + thermalGeneration() + PVGeneration();
		if (Consts.DEBUG)
		{
			if (returnAmount != 0)
			{
				System.out.println("Generating " + returnAmount);
			}
		}
		return returnAmount;
	}

	/**
	 * @return
	 */
	private float PVGeneration() {
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
	private float thermalGeneration() {
		// Assume no thermal gen in domestic for now
		return 0;
	}

	/**
	 * @return
	 */
	private float hydroGeneration() {
		// Assumer no domestic hydro for now
		return 0;
	}

	/**
	 * @return
	 */
	private float windGeneration() {
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
	private float CHPGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private float nonDispaceableDemand() {
		//TODO: For expediency just used melody's model for this, however could use
		// finer grained model of lighting, brown and cooking

		//return lightingDemand() + miscBrownDemand() + cookingDemand();
		return baseDemandProfile[time % baseDemandProfile.length];
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
	private float heatingDemand() {
		recordedHeatPumpDemand[timeOfDay] = 0;

		if (hasElectricalSpaceHeat)
		{
			// TODO: this assumes only space heat and always uses heat pump - expand for other forms of electrical heating
			recordedHeatPumpDemand[timeOfDay] += calculateHeatPumpDemandAndInternalTemp(time);

		}

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
	private float wetApplianceDemand() {
		return this.wetApplianceProfile[time % wetApplianceProfile.length];
	}
	
	/**
	 * @return
	 */
	private float coldApplianceDemand() 
	{
		return this.coldApplianceProfile[time % wetApplianceProfile.length];
	}
	
	/**
	 * @return
	 */
	private float cookingDemand() {
		// Currently incorporated into the base demand
		return 0;
	}



	/**
	 * @return
	 */
	private float miscBrownDemand() {
		// Currently incorporated into the base demand
		return 0;
	}

	/**
	 * @return
	 */
	private float lightingDemand() {
		// Currently incorporated into the base demand
		return 0;
	}

	/**
	 * Calculates the heat pump demand at a given timestep (in kWh)
	 * 
	 * @param timeStep
	 * @return
	 */
	private float calculateHeatPumpDemandAndInternalTemp(int timeStep) {
		//demand is the local variable holding the energy demand
		float demand = 0;
		float deltaT = this.currentInternalTemp - this.getContext().getAirTemperature(timeStep);

		float requiredTempChange = this.setPoint - currentInternalTemp;
		float maintenanceEnergy =  ((deltaT * (this.buildingHeatLossRate)) * ((float)(Consts.SECONDS_PER_DAY / ticksPerDay))) / Consts.KWH_TO_JOULE_CONVERSION_FACTOR;
		float heatingEnergy = requiredTempChange * this.buildingThermalMass;
		
		if ((requiredTempChange < (0 - Consts.FLOATING_POINT_TOLERANCE)) || (deltaT < Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF))
		{
			//heat pump off, leave demand at zero and decrement internal temperature
			this.currentInternalTemp -= maintenanceEnergy / this.buildingThermalMass;
		}
		else
		{
			demand = maintenanceEnergy + heatingEnergy;
			if (demand > ((this.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) * (float) 24 / ticksPerDay))
			{
				demand = (this.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP) * ((float) 24 / ticksPerDay);
				this.currentInternalTemp = this.currentInternalTemp + ((demand - maintenanceEnergy) / this.buildingThermalMass);
			}
			else
			{
				this.currentInternalTemp = this.setPoint;
			}
		}
		
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
		float extTemp = this.getContext().getAirTemperature(timeStep);

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
	 * @return float giving sum of baseDemand for the day.
	 */
	private float calculateFixedDayTotalDemand(int time) {
		int baseProfileIndex = time % baseDemandProfile.length;
		return ArrayUtils.sum(Arrays.copyOfRange(baseDemandProfile,baseProfileIndex,baseProfileIndex+ticksPerDay - 1));
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
	 * @return 	float myDemand		- the demand for this half hour, mediated via behaviour change
	 */
	private float evaluateElasticBehaviour(int time)
	{
		float myDemand;
		int timeSinceSigValid = time - predictionValidTime;

		//As a basic strategy only the base (non-displaceable) demand is
		//elastic
		myDemand = baseDemandProfile[time % baseDemandProfile.length];

		// Adapt behaviour somewhat.  Note that this does not enforce total demand the same over a day.
		// Note, we can only moderate based on cost signal
		// if we receive it (i.e. if we have smart meter)
		// TODO: may have to refine this - do we differentiate smart meter and smart display - i.e. whether receive only or Tx/Rx
		if(hasSmartMeter && getPredictedCostSignalLength() > 0)
		{
			float predictedCostNow = getPredictedCostSignal()[timeSinceSigValid % getPredictedCostSignalLength()];
			myDemand = myDemand * (1 - predictedCostNow * dailyElasticity[time % ticksPerDay]);
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
	 * @return 	float myDemand		- the demand for this half hour, mediated via behaviour change
	 */
	private float smartDemand(int time)
	{

		// Evaluate behaviour applies elasticity behaviour to the base
		// (non-displaceable) load.
		float currentBase = evaluateElasticBehaviour(time);
		float currentCold = coldApplianceDemand();
		float currentWet = wetApplianceDemand();
		float currentHeat = heatingDemand();
		historicalBaseDemand[time % ticksPerDay] = currentBase;
		historicalWetDemand[time % ticksPerDay] = currentWet;
		historicalColdDemand[time % ticksPerDay] = currentCold;
		historicalSpaceHeatDemand[time % ticksPerDay] = currentHeat - getWaterHeatProfile()[time % ticksPerDay];
		historicalWaterHeatDemand[time % ticksPerDay] = getWaterHeatProfile()[time % ticksPerDay];

		float returnAmount = currentBase + currentCold + currentWet + currentHeat;
		if (Consts.DEBUG)
		{
			if (returnAmount != 0)
			{
				System.out.println("Total demand (not net against generation) " + returnAmount);
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
		float moveableLoad = inelasticTotalDayDemand * percentageMoveableDemand;
		float [] daysCostSignal = new float [ticksPerDay];
		float [] daysOptimisedDemand = new float [ticksPerDay];
		if ((Boolean) RepastEssentials.GetParameter("verboseOutput"))
		{
			System.out.println("predictedCostSignal "+getPredictedCostSignal()+" time "+time+ " predictionValidTime "+predictionValidTime+" daysCostSignal "+ daysCostSignal +" ticksPerDay "+ticksPerDay);
		}
		System.arraycopy(getPredictedCostSignal(), time - this.predictionValidTime, daysCostSignal, 0, ticksPerDay);

		System.arraycopy(smartOptimisedProfile, time % smartOptimisedProfile.length, daysOptimisedDemand, 0, ticksPerDay);

		float [] tempArray = ArrayUtils.mtimes(daysCostSignal, daysOptimisedDemand);

		float currentCost = ArrayUtils.sum(tempArray);
		// Algorithm to minimise this whilst satisfying constraints of
		// maximum movable demand and total demand being inelastic.

		float movedLoad = 0;
		float movedThisTime = -1;
		float swapAmount = -1;
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
				System.out.println(agentID + " moving " + movedLoad + "MaxIndex = " + maxIndex + " minIndex = " + minIndex + Arrays.toString(tempArray));
			}
			tempArray = ArrayUtils.mtimes(daysOptimisedDemand, daysCostSignal);			                   	                                             
		}
		System.arraycopy(daysOptimisedDemand, 0, smartOptimisedProfile, time % smartOptimisedProfile.length, ticksPerDay);
		if (Consts.DEBUG)
		{
			if (ArrayUtils.sum(daysOptimisedDemand) != inelasticTotalDayDemand)
			{
				//TODO: This always gets triggerd - I wonder if the "day" i'm taking
				//here and in the inelasticdemand method are "off-by-one"
				System.out.println("optimised signal has varied the demand !!! In error !" + (ArrayUtils.sum(daysOptimisedDemand) - inelasticTotalDayDemand));
			}

			System.out.println("Saved " + (currentCost - ArrayUtils.sum(tempArray)) + " cost");
		}
	}

	private void learnSmartAdoptionDecision(int time)
	{

		// TODO: implement learning whether to adopt smart control in here
		// Could be a TpB based model.
		float inwardInfluence = 0;
		float internalInfluence = this.HEMSPropensity;
		Iterable socialConnections = FindNetwork("socialNetwork").getInEdges(this);
		// Get social influence - note communication is not every tick
		// hence the if clause
		if ((time % (21 * ticksPerDay)) == 0)
		{

			for (Object thisConn: socialConnections)
			{
				RepastEdge myConn = ((RepastEdge) thisConn);
				if (((HouseholdProsumer) myConn.getSource()).hasSmartControl)
				{

					inwardInfluence = inwardInfluence + (float) myConn.getWeight() * ((HouseholdProsumer) myConn.getSource()).transmitPropensitySmartControl;
				}
			}
		}

		float decisionCriterion = inwardInfluence + internalInfluence;
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
	 * Constructor
	 * 
	 * Creates a prosumer agent representing a household within the given context and with
	 * a basic demand profile as passed into the constructor.
	 * 
	 * @param context - the context within which this agent exists
	 * @param baseDemand - a floating point array containing the base demand for this prosumer.  Can be arbitrary length.
	 */
	public HouseholdProsumer(CascadeContext context, float[] baseDemand) {
		super(context);
		this.setAgentName("Household_" + agentID);
		this.percentageMoveableDemand = (float) RandomHelper.nextDoubleFromTo(0, Consts.MAX_DOMESTIC_MOVEABLE_LOAD_FRACTION);
		this.ticksPerDay = context.getTickPerDay();
		this.setNumOccupants(context.occupancyGenerator.nextInt() + 1);
		//Assign hot water storage capacity - note based on EST report, page 9
		this.dailyHotWaterUsage = (float) context.waterUsageGenerator.nextDouble(Consts.EST_INTERCEPT + (this.numOccupants * Consts.EST_SLOPE), Consts.EST_STD_DEV);
				
		this.waterSetPoint = Consts.DOMESTIC_SAFE_WATER_TEMP;
		//TODO: something more sophisticated to give the baseline water heat requirement
		float[] hotWaterNeededProfile = new float[this.mainContext.ticksPerDay];
		float drawOffPerOccupant = this.dailyHotWaterUsage / this.numOccupants;
		
		for (int i = 0; i < this.numOccupants; i++)
		{
			hotWaterNeededProfile[this.mainContext.drawOffGenerator.nextInt()] = drawOffPerOccupant;	
		}
		
		this.baselineHotWaterVolumeProfile = Arrays.copyOf(hotWaterNeededProfile, hotWaterNeededProfile.length);
		this.setWaterHeatProfile(ArrayUtils.multiply(hotWaterNeededProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY / Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (this.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP) / Consts.DOMESTIC_HEAT_PUMP_WATER_COP) ));
		this.ratedPowerHeatPump = Consts.TYPICAL_HEAT_PUMP_ELEC_RATING;
		
		this.setPointProfile = new float[ticksPerDay];
		Arrays.fill(setPointProfile, 20); // This puts in a flat set point through the day set by the consumer
		//this.setPointProfile = Arrays.copyOf(Consts.BASIC_AVERAGE_SET_POINT_PROFILE, Consts.BASIC_AVERAGE_SET_POINT_PROFILE.length);
//		this.setPointProfile = ArrayUtils.offset(this.setPointProfile, (float) RandomHelper.nextDoubleFromTo(-2, 2));
		this.optimisedSetPointProfile = Arrays.copyOf(setPointProfile, setPointProfile.length);
		this.setPoint = optimisedSetPointProfile[0];
		this.currentInternalTemp = this.setPoint;
		this.setPredictedCostSignal(Consts.ZERO_COST_SIGNAL);
		setUpColdApplianceOwnership();
		this.dailyElasticity = new float[ticksPerDay];

		//Arrays to hold some history - mainly for gui purposes
		this.historicalBaseDemand = new float[ticksPerDay];
		this.historicalWetDemand = new float[ticksPerDay];
		this.historicalColdDemand = new float[ticksPerDay];
		this.historicalSpaceHeatDemand = new float[ticksPerDay];
		this.historicalWaterHeatDemand = new float[ticksPerDay];
		this.historicalIntTemp = new float[ticksPerDay];
		this.historicalExtTemp = new float[ticksPerDay];
		//TODO - get more thoughtful elasticity model than this random formulation
		for (int i = 0; i < dailyElasticity.length; i++)
		{
			dailyElasticity[i] = (float) RandomHelper.nextDoubleFromTo(0, 0.1);
		}

		if (baseDemand.length % ticksPerDay != 0)
		{
			System.err.println("baseDemand array not a whole number of days");
			System.err.println("Will be truncated and may cause unexpected behaviour");
		}
		this.baseDemandProfile = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.baseDemandProfile, 0, baseDemand.length);
		this.coldApplianceProfile = InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.DAYS_PER_YEAR, this.hasRefrigerator, this.hasFridgeFreezer, (this.hasUprightFreezer && this.hasChestFreezer));
		// Initialise the base case where heat pump is on all day
		spaceHeatPumpOn = new float[ticksPerDay];
		Arrays.fill(spaceHeatPumpOn, 1);
		recordedHeatPumpDemand = new float[ticksPerDay];

		/*
		 *Set up "smart" stuff here
		 */
		this.mySmartController = new WattboxController(this);
		this.hasSmartControl = true;

		//Initialise the smart optimised profile to be the same as base demand
		//smart controller will alter this
		this.smartOptimisedProfile = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0, smartOptimisedProfile.length);

		//Richard test - just to monitor evolution of one agent
		if (this.agentID == 1)
		{
			sampleOutput = new CSVWriter("richardTestOutput.csv", false);
		}
	}

	/**
	 * @param waterHeatProfile the waterHeatProfile to set
	 */
	public void setWaterHeatProfile(float[] waterHeatProfile) {
		this.waterHeatProfile = waterHeatProfile;
	}

	/**
	 * @return the waterHeatProfile
	 */
	public float[] getWaterHeatProfile() {
		return waterHeatProfile;
	}


}
