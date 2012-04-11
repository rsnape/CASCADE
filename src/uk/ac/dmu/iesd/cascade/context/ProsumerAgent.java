package uk.ac.dmu.iesd.cascade.context;

import java.util.Arrays;

import repast.simphony.engine.schedule.*;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.*;
import repast.simphony.ui.probe.*;
import uk.ac.dmu.iesd.cascade.Consts;

/**
 *  A <em>ProsumerAgent</em> is an object which can both consume and generate 
 *  electricity at the same time. 
 *  The <code>ProsumerAgent</code> class is the abstract superclass of prosumer agents.
 *  Examples of prosumer agents are the household and wind generator agents.
 *  <p>
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.3 $ $Date: 2011/05/17 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial basic functionality including pure elastic reaction to price signal
 * 1.1 - refactor to an abstract class holding only generic prosumer functions
 * 1.2. - implements ICognitiveAgent (Babak)
 * 1.3. - changed constructor, modified/added/removed methods, made the class properly abstract,
 *        sub-classes will/should not override the methodes defined here, except those made abstract (Babak) 
 *    
 *        ----Extra comment added to test git ignore----
 */
public abstract class ProsumerAgent implements ICognitiveAgent {

	/**
	 * a prosumer agent's ID
	 * This field is automatically assigned by constructor 
	 * when the agent is created for the first time
	 * */
	protected long agentID = -1; 

	/**
	 * This field is used for counting number of agents 
	 * instantiated by descendants of this class  
	 **/	
	private static long agentIDCounter = 0; 

	/**
	 * A prosumer agent's name
	 * This field can be <code>null</code>.
	 * */
	protected String agentName;

	/**
	 * A prosumer agent's base name  
	 * it can be reassigned (renamed) properly by descendants of this class  
	 **/	
	protected static String agentBaseName = "prosumer";

	/**
	 * A boolean to determine whether the name has
	 * been set explicitly. <code>nameExplicitlySet</code> will
	 * be false if the name has not been set and true if it has.
	 * @see #getName
	 * @see #setName(String)
	 */
	protected boolean nameExplicitlySet = false;

	protected CascadeContext mainContext;
	
	/**
	 * a prosumer agent's (price) elasticity factor 
	 * e in Peter Boait formula
	 * */
	protected double e_factor; 

	protected int ticksPerDay; //TODO: This needs to be removed 

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */

	// the agent can see "smart" information
	protected boolean hasSmartMeter = false; 
	// the agent acts on "smart" information but not via automatic control
	// action on information is mediated by human input
	protected boolean exercisesBehaviourChange = false; 
	protected boolean hasSmartControl = false; //i.e. the agent allows automatic "smart" control of its demand / generation based on "smart" information
	protected boolean receivesCostSignal = false; //we may choose to model some who remain outside the smart signal system


	/*
	 * Weather and temperature variables
	 * 
	 * TODO: (from RS) These can (and should?) be simply read from the context whenever needed
	 * 
	 */
	protected double insolation; //current insolation at the given half hour tick
	protected double windSpeed; //current wind speed at the given half hour tick
	protected double airTemperature; // outside air temperature


	/*
	 * Electrical properties
	 * 
	 * TODO: (from RS) I think the Electrical properties should be here as every Prosumer
	 * must have them.  This is what distinguishes prosumer from other agents - it
	 * has a physical grid connection.
	 */
	//protected int nodeID;
	//protected int connectionNominalVoltage;
	//protected int[] connectedNodes;
	// distance to source is in metres, can be distance from nearest transformer
	// Can be specified in first instance, or calculated from geographical info below
	// if we go GIS heavy
	//protected int distanceFromSource;


	/*
	 * Imported signals and profiles.
	 */
	protected double[] arr_otherDemandProfile;
	protected double[] predictedCostSignal;
	protected int predictionValidTime;

	/*
	 * Exported signals and profiles.
	 */
	//protected double[] currentDemandProfile;
	//protected double[] predictedPriceSignal;
	//protected int predictedPriceSignalLength;

	/*
	 * This is net demand, may be +ve (consumption), 0, or -ve (generation)
	 */
	protected double netDemand; // (note in kW)
	//protected double availableGeneration; // Generation Capability at this instant (note in kW)

	/*
	 * Economic variables which all prosumers will wish to calculate.
	 */
	//protected double actualCost; // The demand multiplied by the cost signal.  Note that this may be in "real" currency, or not
	protected double inelasticTotalDayDemand;
	protected double[] smartOptimisedProfile;


	/**
	 * serialVersionUID
	 */
	protected static final long serialVersionUID = 1L;  // TODO: to make sure this class will be serialized, if not remove this.



	/**
	 * Returns a string representation of this agent and its key values 
	 * Currently is used by Repast as the method which produces and returns the probe ID.  
	 * @return    a string representation of this agent
	 **/
	@ProbeID()
	public String toString() {	
		return getAgentName()+" "+getAgentID();
	}

	/**
	 * Returns a string representing the state of this agent. This 
	 * method is intended to be used for debugging purposes, and the 
	 * content and format of the returned string are left to the implementing 
	 * concrete subclasses. The returned string may be empty but may not be 
	 * <code>null</code>.
	 * 
	 * @return  a string representation of this agent's state parameters
	 */
	protected abstract String paramStringReport();


	/**
	 * Returns the agent's ID. 
	 * @return  unique ID number of this agent
	 */
	public long getAgentID(){
		return this.agentID;
	}

	/**
	 * Returns the agent's name. If the name has not been explicitly set, 
	 * the default base name will be used.  
	 * @return  agent's name as string
	 */
	public String getAgentName() {
		if (this.agentName == null && !nameExplicitlySet) {
			this.agentName = this.agentBaseName;
		}
		return this.agentName;
	}

	/**
	 * Sets name of this agent  
	 * @param name the string that is to be this agent's name
	 * @see #getName
	 */
	public void setAgentName(String name) {
		this.agentName = name;
		nameExplicitlySet = true;
	}

	/**
	 * Returns the context in which this agent is a part 
	 * @return  the context where the agent is a part
	 **/
	public CascadeContext getContext(){
		return this.mainContext;
	}

	/**
	 * Determines whether this agent has a smart meter
	 * @return <code>true</code> if the agent has smart meter, <code>false</code> otherwise
	 */
	public boolean hasSmartMeter() {
		return this.hasSmartMeter;
	}

	/**
	 * Returns the net demand <code>netDemand</code> for this agent 
	 * @return  the <code>netDemand</code> 
	 **/
	public double getNetDemand() {
		return this.netDemand;
	}
	/**
	 * Sets the <code>netDemand</code> of this agent  
	 * @param nd the new net demand for the agent
	 * @see #getNetDemand
	 */
	public void setNetDemand(double nd) {
		//if (Consts.DEBUG) System.out.println("HHP: setND: "+nd);
		this.netDemand = nd;
	}
	
	/**
	 * this method should define the step for Prosumer agents.
	 * The schedule should start at right timeslot among all concrete subclasses.
	 * Currently it starts at 0, 
	 * however it does not have any priority restriction (such as being the last)
	 * so, it will be executed first (before the Aggreagator which has the LAST priority)  
	 */
	abstract protected void step();


	/**
	 * Sets the <code>price elasticity factor</code> of this agent  
	 * @param e (price) elasticity factor
	 * @see #getElasticityFactor
	 */
	public void setElasticityFactor(double e) {
		this.e_factor = e;
	}
	
	/**
	 * Get the <code>price elasticity factor</code> of this agent  
	 * @return e (price) elasticity factor
	 * @see #getElasticityFactor
	 */
	public double getElasticityFactor() {
		return this.e_factor;
	}
	
	
	//---------------------------------------------------------------
	//TODO: methodes declcared from here until the end of this class 
	//may be changed/removed
	//---------------------------------------------------------------
	
	/*
	 * Method used by display visualisation.  Shouldn't normally be read by others outside
	 * the scope...
	 * 
	 * TODO: Is this implementation the best?
	 * 
	 * @return boolean indicating whether this prosumer has a smart control device
	 */
	public boolean getHasSmartControl()
	{
		return hasSmartControl;
	}
	
	/*
	 * Hacky mcHack to get the Repast Simphony visualisation working with this agent quickly
	 * 
	 * TODO:  Something better than this needed...
	 */
	public double getHasSmartControlAsDouble()
	{
		if (hasSmartControl)
		{
			return 1d;
		}
		else
		{
			return 0d;
		}
	}

	public int getPredictedCostSignalLength() {
		return predictedCostSignal.length;
	}


	public double getCurrentPrediction() {
		int timeSinceSigValid = (int) RepastEssentials.GetTickCount() - getPredictionValidTime();
		if (predictedCostSignal.length > 0 && timeSinceSigValid >= 0) {
			return getPredictedCostSignal()[timeSinceSigValid % predictedCostSignal.length];
		}
		else  {
			return 0;
		}
	}


	/**
	 * TODO: check if it is needed (may changed/removed) 
	 * @return the predictionValidTime
	 */
	public int getPredictionValidTime() {
		return predictionValidTime;
	}

	/**
	 * TODO: check if it is needed (may changed/removed) 
	 * @param predictionValidTime the predictionValidTime to set
	 */
	public void setPredictionValidTime(int predictionValidTime) {
		this.predictionValidTime = predictionValidTime;
	}

	/*
	 * TODO: this methods needs to be changed/removed; this will done by the SmartDevice class
	 * mechanism which will be an Observer and get updated from aggregator (observable) 
	 * 
	 * This method receives the centralised value signal and stores it to the
	 * Prosumer's memory.
	 * 
	 * @param signal - the array containing the cost signal - one member per time tick
	 * @param length - the length of the signal
	 * @param validTime - the time (in ticks) from which the signal is valid
	 */
	public boolean receiveValueSignal(double[] signal, int length) {
		//if (Consts.DEBUG) System.out.println("ProsumerAgent:: receiveValueSignal()");
		boolean success = true;
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

				System.out.println("ProsumerAgent: Signal valid from time other than current time");

				newSignalLength = newSignalLength - signalOffset;
			}

			if ((getPredictedCostSignal() == null) || (newSignalLength != predictedCostSignal.length))
			{
			
				 System.out.println("ProsumerAgent: Re-defining length of signal in agent" + agentID);
			
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

		return success;
	}
	
	private void setSignalType_S(double[] signal){
		
	}
	
	/**
	 * This method is used by other agents (e.g. Aggregator) to send their signal
	 * to prosumer's agent. 
	 * It receives different types of signals containing Real values.
	 * 
	 * TODO: in reality, these signals are sent to customer's device, so if in the 
	 * future, there will be a separate smart device object, this method should be implemented 
	 * there and not in the prosumer's agent directly. 
	 *  
	 * @param signal the array containing the signal
	 * @param length the length of the signal
	 * @param signalType the type of the signal 
	 * @return true if signal received successfully, false otherwise 
	 */
	public boolean receiveSignal(double[] signal, int length, Consts.SIGNAL_TYPE signalType) {
		boolean signalRecievedSuccessfully = false;

		switch (signalType) { 
		case S: 
			setSignalType_S(signal);
			break;
		default:  //
			break;
		}
		return signalRecievedSuccessfully;
	}


	public double getInsolation() {
		return insolation;
	}

	public double getWindSpeed() {
		return windSpeed;
	}

	public double getAirTemperature() {
		return airTemperature;
	}
	
	/*
	 * TODO: is this how the prosumer should get updated about the whether 
	 * This methods needs to be changed/removed; this will done by the SmartDevice class
	 */
	protected void checkWeather(int time)
	{
		// Note at the moment, no geographical info is needed to read the weather
		// this is because weather is a flat file and not spatially differentiated
		//CascadeContext myContext = (CascadeContext) FindContext(contextName);
		insolation = mainContext.getInsolation(time);
		windSpeed = mainContext.getWindSpeed(time);
		airTemperature = mainContext.getAirTemperature(time);		
	}

	/**
	 * @param predictedCostSignal the predictedCostSignal to set
	 */
	public void setPredictedCostSignal(double[] predictedCostSignal) {
		this.predictedCostSignal = Arrays.copyOf(predictedCostSignal, predictedCostSignal.length);
	}

	/**
	 * @return the predictedCostSignal
	 */
	public double[] getPredictedCostSignal() {
		return predictedCostSignal;
	}



	/**
	 * Constructs a prosumer agent with the context in which is created
	 * @param context the context in which this agent is situated 
	 */
	public ProsumerAgent(CascadeContext context) {
		
		this.agentID = agentIDCounter++;
		this.mainContext = context;
	}


}
