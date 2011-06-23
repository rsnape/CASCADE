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
import repast.simphony.engine.watcher.*;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.groovy.math.*;
import repast.simphony.integration.*;
import repast.simphony.matlab.link.*;
import repast.simphony.query.*;
import repast.simphony.query.space.continuous.*;
import repast.simphony.query.space.gis.*;
import repast.simphony.query.space.graph.*;
import repast.simphony.query.space.grid.*;
import repast.simphony.query.space.projection.*;
import repast.simphony.parameter.*;
import repast.simphony.random.*;
import repast.simphony.space.continuous.*;
import repast.simphony.space.gis.*;
import repast.simphony.space.graph.*;
import repast.simphony.space.grid.*;
import repast.simphony.space.projection.*;
import repast.simphony.ui.probe.*;
import repast.simphony.util.*;
import simphony.util.messages.*;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.controllers.*;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.InitialProfileGenUtils;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.2 $ $Date: 2011/06/21 15:29:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial split of categories of prosumer from the abstract class representing all prosumers
 * 1.1 - 
 * 1.2 - add in more sophisticated model incorporating dispaceable and non-displaceabe demand, appliance
 * 		 ownership etc.  Richard
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
	float ratedPowerElectricalWaterHeat;
	float ratedPowerElectricalSpaceHeat;
	float ratedCapacityElectricVehicle; // Note kWh rather than kW
	float ratedCapacityElectricalStorage;   // Note kWh rather than kW
	float ratedCapacityHotWaterStorage;   // Note kWh rather than kW
	float dailyHotWaterUsage;
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
	public float buildingThermalMass;
	public float buildingHeatLossRate;

	/*
	 * temperature control parameters
	 */
	float[] setPointProfile;
	float setPoint;
	float minSetPoint;  // The minimum temperature for this Household's building in centigrade (where relevant)
	float maxSetPoint;  // The maximum temperature for this Household's building in centigrade (where relevant)
	float waterSetPoint;
	float currentInternalTemp;
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
	public boolean[] spaceHeatPumpOn; 
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

	/**
	 * Accessor functions (NetBeans style)
	 * TODO: May make some of these private to respect agent conventions of autonomy / realistic simulation of humans
	 */

	public float getSetPoint() {
		return setPoint;
	}

	public float[] getSetPointProfile() {
		return setPointProfile;
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

	public float getUnadaptedDemand(){
		// Cope with tick count being null between project initialisation and start.
		int index = Math.max(((int) RepastEssentials.GetTickCount() % baseDemandProfile.length), 0);
		return (baseDemandProfile[index]) - currentGeneration();
	}

	public int getDefraCategory()
	{
		return defraCategory;
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

		//Do all the "once-per-day" things here
		if (timeOfDay == 0)
		{
			//TODO: decide whether the inelastic day demand is something that needs
			// calculating here
			inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);
			if (hasSmartControl){
				mySmartController.update(time);
				currentSmartProfiles = mySmartController.getCurrentProfiles();
				this.coldApplianceProfile = (float[]) currentSmartProfiles.get("ColdApps");
				this.wetApplianceProfile = (float[]) currentSmartProfiles.get("WetApps");
			}
		}

		calculateInternalTemp(time);

		//Every step we do these actions
		if (hasSmartControl){
			setNetDemand(evaluateElasticBehaviour(time) + smartDemand(time));
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

		// Return (this will be false if problems encountered).
		//return returnValue;

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
		if (hasElectricalSpaceHeat)
		{
			// TODO: this assumes only space heat and always uses heat pump - expand for other forms of electrical heating
			return heatPumpDemand(timeOfDay);
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @return
	 */
	private float wetApplianceDemand() {
		return this.wetApplianceProfile[timeOfDay];
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
	private float coldApplianceDemand() 
	{
		return this.coldApplianceProfile[timeOfDay];
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
	 * Calculates the heat pump demand at a given timestep using a very simple thresholded
	 * heat pump model
	 * 
	 * @param timeStep
	 * @return
	 */
	private float heatPumpDemand(int timeStep) {
		float power = 0;
		float deltaT = this.setPointProfile[timeStep % ticksPerDay] - this.getContext().getAirTemperature(timeStep);

		if (deltaT > Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF  && spaceHeatPumpOn[timeStep])
		{
			power = deltaT * (this.buildingHeatLossRate / Consts.DOMESTIC_HEAT_PUMP_COP);
		}
		return power;
	}

	/**
	 * 
	 * @param timeStep
	 * @return
	 */
	private void calculateInternalTemp(int timeStep)
	{
		float extTemp = this.getContext().getAirTemperature(timeStep);
		this.setPoint = this.setPointProfile[timeStep % ticksPerDay];
		float deltaT =  this.setPoint - extTemp;
		float tau = this.buildingThermalMass / this.buildingHeatLossRate;
		if (spaceHeatPumpOn[timeOfDay])
		{
			this.currentInternalTemp = this.setPoint;
		}
		else
		{
			this.currentInternalTemp = extTemp + deltaT * (float) Math.exp(-(timeStep / tau));
		}
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
		if(hasSmartMeter && predictedCostSignalLength > 0)
		{
			float predictedCostNow = getPredictedCostSignal()[timeSinceSigValid % predictedCostSignalLength];
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
		float returnAmount = evaluateElasticBehaviour(time) + coldApplianceDemand() + wetApplianceDemand() + heatingDemand();
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
		this.percentageMoveableDemand = (float) RandomHelper.nextDoubleFromTo(0, Consts.MAX_DOMESTIC_MOVEABLE_LOAD_FRACTION);
		this.ticksPerDay = context.getTickPerDay();
		//Assign hot water storage capacity - note from a uniform distribution - may not be realistic.  TODO: Add realistic pdf of this distribution
		this.dailyHotWaterUsage = RandomHelper.nextIntFromTo(Consts.MIN_HOUSHOLD_HOT_WATER_USE, Consts.MAX_HOUSHOLD_HOT_WATER_USE);
		this.waterSetPoint = Consts.DOMESTIC_SAFE_WATER_TEMP;
		//TODO: Something more sophisticated than this very basic set point profile assignment and then add offset
		this.setPointProfile = Consts.BASIC_AVERAGE_SET_POINT_PROFILE;
		this.setPointProfile = ArrayUtils.offset(this.setPointProfile, (float) RandomHelper.nextDoubleFromTo(-2, 2));
		this.setPredictedCostSignal(Consts.ZERO_COST_SIGNAL);
		setUpColdApplianceOwnership();
		this.dailyElasticity = new float[ticksPerDay];
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
		spaceHeatPumpOn = new boolean[ticksPerDay];
		Arrays.fill(spaceHeatPumpOn, true);
		
		/*
		 *Set up "smart" stuff here
		 */
		this.mySmartController = new WattboxController(this);
		this.hasSmartControl = true;


		//Initialise the smart optimised profile to be the same as base demand
		//smart controller will alter this
		this.smartOptimisedProfile = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0, smartOptimisedProfile.length);
	}
}
