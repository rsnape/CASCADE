package prosumermodel;

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
import smartgrid.helperfunctions.ArrayUtils;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;
import prosumermodel.SmartGridConstants.*;


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
public class WindGeneratorProsumer extends GeneratorProsumer {

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */
	int numTurbines;
/*
 * TODO - need some operating characteristic parameters here - e.g. time to start
 * ramp up generation etc. etc.
 */
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

	public int getSmartControlForCount()
	{
		if (hasSmartControl){
			return 1;
		}
		else
		{
			return 0;
		}
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
		return (baseDemandProfile[index]) - currentGeneration();
	}

	public float getCurrentPrediction() {
		int timeSinceSigValid = (int) RepastEssentials.GetTickCount() - getPredictionValidTime();
		if (predictedCostSignalLength > 0) {
			return predictedCostSignal[timeSinceSigValid % predictedCostSignalLength];
		}
		else
		{
			return 0;
		}
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
	/**
	 * @return the predictionValidTime
	 */
	public int getPredictionValidTime() {
		return predictionValidTime;
	}

	/**
	 * @param predictionValidTime the predictionValidTime to set
	 */
	public void setPredictionValidTime(int predictionValidTime) {
		this.predictionValidTime = predictionValidTime;
	}

	/*
	 * Communication functions
	 */

	/*
	 * Helper method for the common case where the signal transmitted is valid from the 
	 * current time
	 * 
	 * @param signal - the array containing the cost signal - one member per time tick
	 * @param length - the length of the signal
	 * 
	 */
	public boolean receiveValueSignal(float[] signal, int length) {
		boolean success = true;

		receiveValueSignal(signal, length, (int) RepastEssentials.GetTickCount());

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
	 * This method defines the step behaviour of a wind generator agent
	 * This represents a farm of 1 or more turbines i.e. anything that is
	 * purely wind - can be a single turbine or many
	 * 
	 * Input variables: none
	 * 
	 * Return variables: boolean returnValue - returns true if the method
	 * executes successfully
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
		}

		/* 
		 * As wind is non-dispatchable, simply return the current generation
		 * each time.  The only adaptation possible would be shutting down
		 * some / all turbines
		 * 
		 * TODO - implement turbine shutdown
		 */
		setNetDemand( 0 - currentGeneration());
		
		// Return (this will be false if problems encountered).
		return returnValue;

	}

	/**
	 * @return
	 */
	private float currentGeneration() {
		float returnAmount = 0;

		returnAmount = returnAmount + windGeneration();
		if (SmartGridConstants.debug)
		{
			if (returnAmount != 0)
			{
				System.out.println("Generating " + returnAmount);
			}
		}
		return returnAmount;
	}

	/**
	 * @return - float containing the current generation for this wind generator
	 */
	private float windGeneration() {
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
	 * @param time
	 * @return float giving sum of baseDemand for the day.
	 */
	private float calculateFixedDayTotalDemand(int time) {
		int baseProfileIndex = time % baseDemandProfile.length;
		return ArrayUtils.sum(Arrays.copyOfRange(baseDemandProfile,baseProfileIndex,baseProfileIndex+ticksPerDay - 1));
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
	public WindGeneratorProsumer(String myContext, float[] baseDemand, float capacity, Parameters parm) {
		/*
		 * If number of wind turbines not specified, assume 1
		 */
		this(myContext, baseDemand, capacity, parm, 1);
	}
	
	public WindGeneratorProsumer(String myContext, float[] baseDemand, float capacity, Parameters parm, int turbines) {
		super();
		this.contextName = myContext;
		this.hasWind = true;
		this.ratedPowerWind = capacity;
		this.numTurbines = turbines;
		this.percentageMoveableDemand = 0;
		this.maxTimeShift = 0;
		this.ticksPerDay = (Integer) parm.getValue("ticksPerDay");
		if (baseDemand.length % ticksPerDay != 0)
		{
			System.err.println("baseDemand array not a whole number of days");
			System.err.println("Will be truncated and may cause unexpected behaviour");
		}
		this.baseDemandProfile = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.baseDemandProfile, 0, baseDemand.length);
		//Initialise the smart optimised profile to be the same as base demand
		//smart controller will alter this
		this.smartOptimisedProfile = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0, smartOptimisedProfile.length);
	}

	/*
	 * No argument constructor - basic prosumer created
	 */
	public WindGeneratorProsumer() {
		super();
	}
}
