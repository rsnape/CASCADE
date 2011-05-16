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
import uk.ac.dmu.iesd.cascade.Consts.*;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;


/**
 * @author J. Richard Snape
 * @version $Revision: 1.2 $ $Date: 2011/05/11 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial basic functionality including pure elastic reaction to price signal
 * 1.01 - Introduction of smart adaptation in addition to elastic behavioural adaptation
 * 1.1 - refactor to an abstract class holding only generic prosumer functions
 * 1.2. - implements ICognitiveAgent (Babak)
 */
public abstract class ProsumerAgent implements ICognitiveAgent {
	/*
	 * Agent properties
	 */
	String contextName;
	public int ticksPerDay;

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
	

	 /*
	 * Weather and temperature variables
	 */
	float insolation; //current insolation at the given half hour tick
	float windSpeed; //current wind speed at the given half hour tick
	float airTemperature; // outside air temperature


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
	int predictionValidTime;

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
	float availableGeneration; // Generation Capability at this instant (note in kW)

	/*
	 * Economic variables which all prosumers will wish to calculate.
	 */
	float actualCost; // The demand multiplied by the cost signal.  Note that this may be in "real" currency, or not
	float inelasticTotalDayDemand;
	protected float[] smartOptimisedProfile;

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


	/*
	 * This method receives the centralised value signal and stores it to the
	 * Prosumer's memory.
	 * 
	 * @param signal - the array containing the cost signal - one member per time tick
	 * @param length - the length of the signal
	 * @param validTime - the time (in ticks) from which the signal is valid
	 */
	public boolean receiveValueSignal(float[] signal, int length, int validTime) {
		boolean success = true;
		// Can only receive if we have a smart meter to receive data

		if (hasSmartMeter)
		{
			// Note the time from which the signal is valid.
			// Note - Repast can cope with fractions of a tick (a double is returned)
			// but I am assuming here we will deal in whole ticks and alter the resolution should we need
			int time = (int) RepastEssentials.GetTickCount();
			int newSignalLength = length;
			setPredictionValidTime(validTime);
			float[] tempArray;

			int signalOffset = time - validTime;

			if (signalOffset != 0)
			{
				if (Consts.DEBUG)
				{
					System.out.println("Signal valid from time other than current time");
				}
				newSignalLength = newSignalLength - signalOffset;
			}

			if ((predictedCostSignal == null) || (newSignalLength != getPredictedCostSignalLength()))
			{
				if (Consts.DEBUG)
				{
					System.out.println("Re-defining length of signal in agent" + agentID);
				}
				setPredictedCostSignalLength(newSignalLength);
				predictedCostSignal = new float[newSignalLength];
			}

			if (signalOffset < 0)
			{
				// This is a signal projected into the future.
				// pad the signal with copies of what was in it before and then add the new signal on
				System.arraycopy(signal, 0, predictedCostSignal, 0 - signalOffset, length);
			}
			else
			{
				// This was valid from now or some point in the past.  Copy in the portion that is still valid and 
				// then "wrap" the front bits round to the end of the array.
				System.arraycopy(signal, signalOffset, predictedCostSignal, 0, length);
			}

			if (Consts.DEBUG)
			{
				System.out.println(this.agentID + " received value signal " + Arrays.toString(signal));
			}
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

		/*
		 * At this very basic stage, the abstract class has no step behaviour
		 * this should all be taken care of by the inheriting classes
		 */
		return returnValue;

	}

	protected void checkWeather(int time)
	{
		// Note at the moment, no geographical info is needed to read the weather
		// this is because weather is a flat file and not spatially differentiated
		CascadeContext myContext = (CascadeContext) FindContext(contextName);
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
	}

	/*
	 * No argument constructor - basic prosumer created
	 */
	public ProsumerAgent() {
		super();
	}
}
