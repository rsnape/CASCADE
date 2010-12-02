package prosumermodel;

import java.io.*;
import java.math.*;
import java.util.*;
import javax.measure.unit.*;
import org.jscience.mathematics.number.*;
import org.jscience.mathematics.vector.*;
import org.jscience.physics.amount.*;
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
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;

/**
 * @author jsnape
 * @version $Revision: 1.0 $ $Date: 2010/11/17 17:00:00 $
 * 
 */
public class ProsumerAgent {
	/*
	 * Agent properties
	 */
	String contextName;
	int ticksPerDay;

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */
	
	// the agent can see "smart" information
	boolean hasSmartMeter = false; 
	// the agent acts on "smart" information but not via automatic control
	// action on information is mediated by human input
	boolean exercisesBehaviourChange = false; 
	boolean hasSmartControl = false; //i.e. the agent allows automatic "smart" control of its demand / generation based on "smart" information
	boolean receivesCostSignal = false; //we may choose to model some who remain outside the smart signal system
	boolean hasCHP = false;
	boolean hasAirSourceHeatPump = false;
	boolean hasGroundSourceHeatPump = false;
	boolean hasWind = false;
	boolean hasHydro = false;
	// Thermal Generation included so that Prosumer can represent
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
	float ratedPowerSolarWaterHeat;
	float ratedPowerElectricalWaterHeat;
	float ratedPowerElectricalSpaceHeat;
	float ratedCapacityElectricVehicle; // Note kWh rather than kW
	float ratedCapacityElectricalStorage;   // Note kWh rather than kW
	float ratedCapacityHotWaterStorage;
	float ratedCapacitySpaceHeatStorage; // Note - related to thermal mass
	
	/*
	 * Weather and temperature variables
	 */
	float insolation; //current insolation at the given half hour tick
	float windSpeed; //current wind speed at the given half hour tick
	float airTemperature; // outside air temperature
	
	
	// For prosumers in buildings - set to zero if irrelevant
	float buildingHeatCapacity;
	float buildingHeatLossRate;
	float buildingTemperatureSetPoint;
	
	/*
	 * Electrical properties
	 */
	int nodeID;
	int connectionNominalVoltage;
	int[] connectedNodes;
	// distance to source is in metres, can be distance from nearest transformer
	// Can be specified in first instance, or calculated from geographical info below
	// if we go GIS heavy
	int distanceFromSource;

	/*
	 * Geographical properties
	 * 
	 * Could go for this if we wish to go GIS heavy
	 */
	float latitude;
	float longitude;
	float altitude = 0;

	/*
	 * Imported signals and profiles.
	 */
	float[] baseDemandProfile;
	float[] predictedCostSignal;
	int predictedCostSignalLength;
	
	/*
	 * Exported signals and profiles.
	 */
	float[] currentDemandProfile;
	float[] predictedPriceSignal;
	int predictedPriceSignalLength;

	/*
	 * This is net demand, may be +ve (consumption), 0, or -ve (generation)
	 */
	float netDemand; // (note in kW)
	float percentageMoveableDemand;  // This can be set constant, or calculated from available appliances
	int maxTimeShift; // The "furthest" a demand may be moved in time.  This can be constant or calculated dynamically.
	float availableGeneration; // Generation Capability at this instant (note in kW)

	/*
	 * Behavioural properties
	 */
	// Learner adoptSmartMeterLearner;
	// Learner adoptSmartControlLearner;
	// Learner consumptionPatternLearner;
	
	float minSetPoint;  // The minimum temperature for this prosumer's building in centigrade (where relevant)
	float maxSetPoint;  // The maximum temperature for this prosumer's building in centigrade (where relevant)
	float currentInternalTemp;
	float costThreshold;
	float actualCost; // The demand multiplied by the cost signal.  Note that this may be in "real" currency, or not
	float inelasticTotalDayDemand;
	
	/*
	 * Accessor functions (NetBeans style)
	 * TODO: May make some of these private to respect agent conventions of autonomy / realistic simulation of humans
	 */
	
	public float getNetDemand() {
		return netDemand;
	}

	public void setNetDemand(float newDemand) {
		netDemand = newDemand;
	}
	
	public int getPredictedCostSignalLength() {
		return predictedCostSignalLength;
	}

	public void setPredictedCostSignalLength(int length) {
		predictedCostSignalLength = length;
	}
	
	public float getUnadaptedDemand(){
		// Cope with tick count being null between project initialisation and start.
		int index = Math.max(((int) RepastEssentials.GetTickCount() % baseDemandProfile.length), 0);
		return (baseDemandProfile[index]);
	}
	
	public float getInsolation() {
		return insolation;
	}
	
	public float getWindSpeed() {
		return windSpeed;
	}
	
	public float getAirTemperature() {
		return airTemperature;
	}

	/*
	 * Communication functions
	 */

	/*
	 * This method receives the centralised value signal and stores it to the
	 * Prosumer's memory.
	 */
	public boolean receiveValueSignal(float[] signal, int length) {
		boolean success = true;
		// Can only receive if we have a smart meter to receive data
		if (hasSmartMeter)
		{
			setPredictedCostSignalLength(length);
			predictedCostSignal = new float[length];
			System.arraycopy(signal, 0, predictedCostSignal, 0, length);
		}

		return success;
	}

	public boolean receiveInfluence() {
		boolean success = true;

		return success;
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
	public boolean step() {

		// Define the return value variable.  Set this false if errors encountered.
		boolean returnValue = true;

		// Note the simulation time if needed.
		// Note - Repast can cope with fractions of a tick (a double is returned)
		// but I am assuming here we will deal in whole ticks and alter the resolution should we need
		int time = (int) RepastEssentials.GetTickCount();
		int timeOfDay = (time % ticksPerDay);
		SmartGridContext myContext = (SmartGridContext) FindContext(contextName);
		
		checkWeather(time);
	
		//Do all the "once-per-day" things here
		if (timeOfDay == 0)
		{
			inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);

			//Receive the prediction signal every midnight if you have a smart meter
			if (hasSmartMeter){
				//receiveValueSignal(myContext.systemPriceSignalData, myContext.systemPriceSignalDataLength);
			}
		}
		
		if (hasSmartControl){
			smartControlLearn();
			setNetDemand(smartDemand(time));
		}
		else if (hasSmartMeter && exercisesBehaviourChange) {
			learnBehaviourChange();
			setNetDemand(evaluateBehaviour(time));
		}
		else
		{
			//No adaptation case
			learnSmartAdaptationDecision();
			setNetDemand(baseDemandProfile[time % baseDemandProfile.length]);
		}
		
		
		
		// Return (this will be false if problems encountered).
		return returnValue;

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
	
	/*
	 * Evaluates the net demand mediated by the prosumers behaviour in a given half hour.
	 * 
	 * NOTE: 	As implemented - this method does not enforce total demand parity over a given day (i.e.
	 * 			integral of netDemand over a day is not necessarily constant.
	 * 
	 * @param 	int time	- the simulation time in ticks at which to evaluate this prosumer's behaviour
	 * @return 	float myDemand		- the demand for this half hour, mediated via behaviour change
	 */
	private float evaluateBehaviour(int time)
	{
		float myDemand;
		
		//As a basic strategy ("pass-through"), we set the demand now to
		//basic demand as of now.
		myDemand = baseDemandProfile[time % baseDemandProfile.length];
		
		// Adapt behaviour somewhat.  Note that this does not enforce total demand the same over a day.
		// Note, we can only moderate based on cost signal
		// if we receive it (i.e. if we have smart meter)
		// TODO: may have to refine this - do we differentiate smart meter and smart display - i.e. whether receive only or Tx/Rx
		if(hasSmartMeter && predictedCostSignalLength > 0)
		{
			float predictedCostNow = predictedCostSignal[time % predictedCostSignalLength];
			if ( predictedCostNow > costThreshold){
				//Infinitely elastic version (i.e. takes no account of percenteageMoveableDemand
				myDemand = myDemand * (float) Math.exp( - ((predictedCostNow - costThreshold) / costThreshold));
				
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
	 * @return 	float myDemand		- the demand for this half hour, mediated via behaviour change
	 */
	private float smartDemand(int time)
	{
		float myDemand;
		myDemand = baseDemandProfile[time % baseDemandProfile.length];
		return myDemand;
	}
	
	private void learnBehaviourChange()
	{
		// TODO: Implement the behavioural (social?) learning in here
	}
	
	private void smartControlLearn()
	{
		// TODO: Implement the smart device (optimisation) learning in here
	}
	
	private void learnSmartAdaptationDecision()
	{
		// TODO: implement learning whether to adopt smart control in here
		// Could be a TpB based model.
	}
	
	private void checkWeather(int time)
	{
		// Note at the moment, no geographical info is needed to read the weather
		// this is because weather is a flat file and not spatially differentiated
		SmartGridContext myContext = (SmartGridContext) FindContext(contextName);
		insolation = myContext.getInsolation(time);
		windSpeed = myContext.getWindSpeed(time);
		airTemperature = myContext.getAirTemperature(time);		
	}
	
    /**
    *
    * This value is used to automatically generate agent identifiers.
    * @field serialVersionUID
    *
    */
   private static final long serialVersionUID = 1L;

   /**
    *
    * This value is used to automatically generate agent identifiers.
    * @field agentIDCounter
    *
    */
   protected static long agentIDCounter = 1;

   /**
    *
    * This value is the agent's identifier.
    * @field agentID
    *
    */
   protected String agentID = "Prosumer " + (agentIDCounter++);
	
    /**
    *
    * This method provides a human-readable name for the agent.
    * @method toString
    *
    */
   @ProbeID()
   public String toString() {
       // Set the default agent identifier.
       String returnValue = this.agentID;
       // Return the results.
       return returnValue;

   }
   
   public String getAgentID()
   {
	   return this.agentID;
   }
	
	/*
	 * Constructor function(s)
	 */
	public ProsumerAgent(String myContext, float[] baseDemand, Parameters parm) {
		super();
		this.contextName = myContext;
		this.percentageMoveableDemand = (float) RandomHelper.nextDoubleFromTo(0, 0.15);
		this.ticksPerDay = (Integer) parm.getValue("ticksPerDay");
		if (baseDemand.length % ticksPerDay != 0)
		{
			System.err.println("baseDemand array not a whole number of days");
			System.err.println("Will be truncated and may cause unexpected behaviour");
		}
		this.baseDemandProfile = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.baseDemandProfile, 0, baseDemand.length);
	}

	/*
	 * No argument constructor - basic prosumer created
	 */
	public ProsumerAgent() {
		super();
	}
}
