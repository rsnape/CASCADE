package uk.ac.dmu.iesd.cascade.agents.prosumers;

import static repast.simphony.essentials.RepastEssentials.FindNetwork;
import static repast.simphony.essentials.RepastEssentials.GetParameter;

import java.util.Arrays;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;


/**
 * @author J. Richard Snape
 * @version $Revision: 1.00 $ $Date: 2011/03/17 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial split of categories of prosumer from the abstract class representing all prosumers
 * 
 * 
 */
public class NonDomesticProsumer extends ProsumerAgent{

	/*
	 * NOTE 
	 * @TODO - It is possible that we should have some kind of hierarchical
	 * inheritance and this should also be abstract, sub-classed to specific types
	 * of non-dom consumer.  For now, it is a placeholder.
	 */
	
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
	// Thermal Generation included so that Household can represent
	// Biomass, nuclear or fossil fuel generation in the future
	// what do we think?
	boolean hasThermalGeneration = false;
	boolean hasPV = false;
	boolean hasSolarWaterHeat = false;
	boolean hasElectricalWaterHeat = false;
	boolean hasElectricalSpaceHeat = false;
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
	double ratedPowerThermalGeneration;
	double ratedPowerPV;
	double ratedPowerSolarWaterHeat;
	double ratedPowerElectricalWaterHeat;
	double ratedPowerElectricalSpaceHeat;
	double ratedCapacityElectricalStorage;   // Note kWh rather than kW
	double ratedCapacityHotWaterStorage;
	double ratedCapacitySpaceHeatStorage; // Note - related to thermal mass


	// For this prosumer's property - set to zero if irrelevant
	double buildingHeatCapacity;
	double buildingHeatLossRate;
	double buildingTemperatureSetPoint;


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
	 * temperature control parameters
	 */
	double minSetPoint;  // The minimum temperature for this Household's building in centigrade (where relevant)
	double maxSetPoint;  // The maximum temperature for this Household's building in centigrade (where relevant)
	double currentInternalTemp;
	
	/*
	 * This may or may not be used, but is a threshold cost above which actions
	 * take place for the household
	 */
	double costThreshold;

	/*
	 * Accessor functions (NetBeans style)
	 * TODO: May make some of these private to respect agent conventions of autonomy / realistic simulation of humans
	 */



	
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



	/*
	 * Step behaviour
	 */

	/******************
	 * This method defines the step behaviour of a prosumer agent
	 * 
	 * Input variables: none
	 * 
	 * Return variables: boolean returnValue - returns true if the method
	 * executes succesfully
	 ******************/
	@ScheduledMethod(start = 1, interval = 1, shuffle = true)
	public void step() {

		// Define the return value variable.  Set this false if errors encountered.
		boolean returnValue = true;

		// Note the simulation time if needed.
		// Note - Repast can cope with fractions of a tick (a double is returned)
		// but I am assuming here we will deal in whole ticks and alter the resolution should we need
		int time = (int) RepastEssentials.GetTickCount();
		int timeOfDay = (time % this.mainContext.ticksPerDay);
		CascadeContext myContext = this.getContext();

		checkWeather(time);

		//Do all the "once-per-day" things here
		if (timeOfDay == 0)
		{
			inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);
			if (hasSmartControl){
				smartControlLearn(time);
			}
		}

		if (hasSmartControl){
			setNetDemand(smartDemand(time));
		}
		else if (hasSmartMeter && exercisesBehaviourChange) {
			learnBehaviourChange();
			setNetDemand(evaluateBehaviour(time));
			learnSmartAdoptionDecision(time);
		}
		else
		{
			//No adaptation case
			setNetDemand(arr_otherDemandProfile[time % arr_otherDemandProfile.length] - currentGeneration());

			learnSmartAdoptionDecision(time);
		}



		// Return (this will be false if problems encountered).
		//return returnValue;

	}

	/**
	 * @return
	 */
	private double currentGeneration() {
		double returnAmount = 0;

		returnAmount = returnAmount + CHPGeneration() + windGeneration() + hydroGeneration() + thermalGeneration() + PVGeneration();
		if (Consts.DEBUG)
		{
			if (returnAmount != 0)
			{
				if (Consts.DEBUG) System.out.println("NonDomesticProsumer: Generating " + returnAmount);
			}
		}
		return returnAmount;
	}

	/**
	 * @return
	 */
	private double PVGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private double thermalGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private double hydroGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private double windGeneration() {
		if(hasWind){
			//TODO: get a realistic model of wind production - this just linear between 
			//5 and 25 metres per second, zero below, max power above
			return (Math.max((Math.min(getWindSpeed(),25) - 5),0))/20 * ratedPowerWind;
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

	/*
	 * Evaluates the net demand mediated by the prosumers behaviour in a given half hour.
	 * 
	 * NOTE: 	As implemented - this method does not enforce total demand parity over a given day (i.e.
	 * 			integral of netDemand over a day is not necessarily constant.
	 * 
	 * @param 	int time	- the simulation time in ticks at which to evaluate this prosumer's behaviour
	 * @return 	double myDemand		- the demand for this half hour, mediated via behaviour change
	 */
	private double evaluateBehaviour(int time)
	{
		double myDemand;
		int timeSinceSigValid = time - predictionValidTime;

		//As a basic strategy ("pass-through"), we set the demand now to
		//basic demand as of now.
		myDemand = arr_otherDemandProfile[time % arr_otherDemandProfile.length];

		// Adapt behaviour somewhat.  Note that this does not enforce total demand the same over a day.
		// Note, we can only moderate based on cost signal
		// if we receive it (i.e. if we have smart meter)
		// TODO: may have to refine this - do we differentiate smart meter and smart display - i.e. whether receive only or Tx/Rx
		if(hasSmartMeter && getPredictedCostSignalLength() > 0)
		{
			double predictedCostNow = getPredictedCostSignal()[timeSinceSigValid % getPredictedCostSignalLength()];
			if ( predictedCostNow > costThreshold){
				//Infinitely elastic version (i.e. takes no account of percenteageMoveableDemand
				// TODO: Need a better logging system than this - send logs with a level and output to
				// console or file.  Can we use log4j?
				if (Consts.DEBUG)
				{
					if (Consts.DEBUG) System.out.println("NonDomesticProsumer: " + this.agentID + "Changing demand at time " + time + " with price signal " + (predictedCostNow - costThreshold) + " above threshold");
				}
				myDemand = myDemand * (1 - percentageMoveableDemand * (1 - Math.exp( - ((predictedCostNow - costThreshold) / costThreshold))));

			}
		}


		return myDemand;
	}

	/*
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
		//Very simple function at the moment - just return the smart profile for this time
		//Previously defined by smartControlLearn()
		double myDemand;
		myDemand = smartOptimisedProfile[time % smartOptimisedProfile.length];
		return myDemand;
	}

	private void learnBehaviourChange()
	{
		// TODO: Implement the behavioural (social?) learning in here
	}

	private void smartControlLearnFlat(int time)
	{
		// simplest smart controller implementation - perfect division of load through the day
		double moveableLoad = inelasticTotalDayDemand * percentageMoveableDemand;
		double [] daysCostSignal = new double [this.mainContext.ticksPerDay];
		double [] daysOptimisedDemand = new double [this.mainContext.ticksPerDay];
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
			Arrays.fill(daysOptimisedDemand, inelasticTotalDayDemand / this.mainContext.ticksPerDay);
			movedThisTime = 0;
			tempArray = ArrayUtils.mtimes(daysOptimisedDemand, daysCostSignal);			                   	                                             
		}
		System.arraycopy(daysOptimisedDemand, 0, smartOptimisedProfile, time % smartOptimisedProfile.length, this.mainContext.ticksPerDay);
		if (Consts.DEBUG)
		{
			if (ArrayUtils.sum(daysOptimisedDemand) != inelasticTotalDayDemand)
			{
				//TODO: This always gets triggerd - I wonder if the "day" i'm taking
				//here and in the inelasticdemand method are "off-by-one"
				if (Consts.DEBUG) System.out.println("NonDomesticProsumer: optimised signal has varied the demand !!! In error !" + (ArrayUtils.sum(daysOptimisedDemand) - inelasticTotalDayDemand));
			}

			if (Consts.DEBUG) System.out.println("Saved " + (currentCost - ArrayUtils.sum(tempArray)) + " cost");
		}
	}
	
	private void smartControlLearn(int time)
	{
		// smart device (optimisation) learning in here
		// in lieu of knowledge of what can "switch off" and "switch on", we assume that
		// the percentage moveable of the day's consumption is what may be time shifted
		double moveableLoad = inelasticTotalDayDemand * percentageMoveableDemand;
		double [] daysCostSignal = new double [this.mainContext.ticksPerDay];
		double [] daysOptimisedDemand = new double [this.mainContext.ticksPerDay];
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
				if (Consts.DEBUG) System.out.println("NonDomesticProsumer: " +agentID + " moving " + movedLoad + "MaxIndex = " + maxIndex + " minIndex = " + minIndex + Arrays.toString(tempArray));
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
				if (Consts.DEBUG) System.out.println("NonDomesticProsumer: optimised signal has varied the demand !!! In error !" + (ArrayUtils.sum(daysOptimisedDemand) - inelasticTotalDayDemand));
			}

			if (Consts.DEBUG) System.out.println("NonDomesticProsumer: Saved " + (currentCost - ArrayUtils.sum(tempArray)) + " cost");
		}
	}

	private void learnSmartAdoptionDecisionRemoveAll(int time)
	{
		hasSmartControl = false;
		return;
	}
	
	private void learnSmartAdoptionDecision(int time)
	{
		
		// TODO: implement learning whether to adopt smart control in here
		// Could be a TpB based model.
		double inwardInfluence = 0;
		double internalInfluence = 0;
		Iterable socialConnections = FindNetwork("socialNetwork").getInEdges(this);
		// Get social influence - note communication is not every tick
		// hence the if clause
		if ((time % (21 * this.mainContext.ticksPerDay)) == 0)
		{

			for (Object thisConn: socialConnections)
			{
				RepastEdge myConn = ((RepastEdge) thisConn);
				if (((NonDomesticProsumer) myConn.getSource()).hasSmartControl)
				{

					inwardInfluence = inwardInfluence + myConn.getWeight() * ((NonDomesticProsumer) myConn.getSource()).transmitPropensitySmartControl;
				}
			}
		}

		double decisionCriterion = inwardInfluence + internalInfluence;
		if(decisionCriterion > (Double) GetParameter("smartControlDecisionThreshold")) 
		{
			hasSmartControl = true;
		}


	}


	/**
	 * Constructor 
	 */
	public NonDomesticProsumer(CascadeContext context, double[] baseDemand) {
		super(context);
	
		this.percentageMoveableDemand = RandomHelper.nextDoubleFromTo(0, 0.5);
		setElasticityFactor(percentageMoveableDemand);
		this.mainContext.ticksPerDay = context.getNbOfTickPerDay();
		if (baseDemand.length % this.mainContext.ticksPerDay != 0)
		{
			System.err.print("Error/Warning message from "+this.getClass()+": BaseDemand array not a whole number of days.");
			System.err.println("NonDomesticProsumer: Will be truncated and may cause unexpected behaviour");
		}
		this.arr_otherDemandProfile = new double [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.arr_otherDemandProfile, 0, baseDemand.length);
		//Initialise the smart optimised profile to be the same as base demand
		//smart controller will alter this
		this.smartOptimisedProfile = new double [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0, smartOptimisedProfile.length);
	}


	/**
	 * @param percentageMoveableDemand2
	 */
	private void setElasticityFactor(double percentageMoveableDemand2)
	{
		
	}



}
