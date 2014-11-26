package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.Arrays;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.ui.probe.ProbeID;
import repast.simphony.util.ContextUtils;
import uk.ac.dmu.iesd.cascade.agents.ICognitiveAgent;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * A <em>ProsumerAgent</em> is an object which can both consume and generate
 * electricity at the same time. The <code>ProsumerAgent</code> class is the
 * abstract superclass of prosumer agents. Examples of prosumer agents are the
 * household and wind generator agents.
 * <p>
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.4 $ $Date: 2013/08/17 12:00:00 $
 * 
 *          Version history (for intermediate steps see Git repository history
 * 
 *          1.0 - Initial basic functionality including pure elastic reaction to
 *          price signal 1.1 - refactor to an abstract class holding only
 *          generic prosumer functions (17 March 2011) 1.2. - implements
 *          ICognitiveAgent (Babak) 1.3. - changed constructor,
 *          modified/added/removed methods, made the class properly abstract,
 *          sub-classes will/should not override the methodes defined here,
 *          except those made abstract (Babak)
 * 
 *          1.4 - cleanup (JRS)
 * 
 *          ----Extra comment added to test git ignore----
 */
public abstract class ProsumerAgent implements ICognitiveAgent
{

	/**
	 * serialVersionUID - a unique identifier if this class is serialized
	 */
	protected static final long serialVersionUID = 1L; // TODO: to make sure
														// this class will be
														// serialized, if not
														// remove this.

	/**
	 * a prosumer agent's ID This field is automatically assigned by constructor
	 * when the agent is created for the first time
	 * */
	protected long agentID = -1;

	/**
	 * This field is used for counting number of agents instantiated by
	 * descendants of this class
	 **/
	private static long agentIDCounter = 0;

	/**
	 * A prosumer agent's name This field can be <code>null</code>.
	 * */
	protected String agentName;

	/**
	 * A prosumer agent's base name it can be reassigned (renamed) properly by
	 * descendants of this class
	 **/
	protected static String agentBaseName = "prosumer";

	/**
	 * A boolean to determine whether the name has been set explicitly.
	 * <code>nameExplicitlySet</code> will be false if the name has not been set
	 * and true if it has.
	 * 
	 * @see #getName
	 * @see #setName(String)
	 */
	protected boolean nameExplicitlySet = false;

	/**
	 * Reference to the @link CascadeContext within which this ProsumerAgent
	 * exists.
	 * 
	 * @see CascadeContext
	 */
	public CascadeContext mainContext;

	/**
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */

	/**
	 * Does the agent have the ability to send and receive "smart" information
	 */
	public boolean hasSmartMeter = false;

	/**
	 * does this agent change its behaviour in response to information it
	 * gathers?
	 */
	public boolean exercisesBehaviourChange = false;

	/**
	 * Does this agent have a form of smart control - i.e. automated control in
	 * response to information gathered from its smart meter (either information
	 * about itself or received information about the system
	 */
	public boolean hasSmartControl = false; // i.e. the agent allows automatic
											// "smart" control of its demand /
											// generation based on "smart"
											// information

	/**
	 * Does this agent receive a smart cost signal?
	 */
	protected boolean receivesCostSignal = false; // we may choose to model some
													// who remain outside the
													// smart signal system

	/*
	 * Weather and temperature variables
	 * 
	 * TODO: (from RS) These can (and should?) be simply read from the context
	 * whenever needed
	 */
	protected double insolation; // current insolation at the given half hour
									// tick
	protected double windSpeed; // current wind speed at the given half hour
								// tick
	protected double airTemperature; // outside air temperature
	protected double airDensity; // air density

	// protected int ticksPerDay; //TODO: JRS This needs to be removed - get it
	// from the context if needed

	/*
	 * Electrical properties
	 * 
	 * TODO: (from RS) I think the Electrical properties should be here as every
	 * Prosumer must have them. This is what distinguishes prosumer from other
	 * agents - it has a physical grid connection.
	 */
	// protected int nodeID;
	// protected int connectionNominalVoltage;
	// protected int[] connectedNodes;
	// distance to source is in metres, can be distance from nearest transformer
	// Can be specified in first instance, or calculated from geographical info
	// below
	// if we go GIS heavy
	// protected int distanceFromSource;

	/*
	 * Imported signals and profiles.
	 */
	protected double[] arr_otherDemandProfile;
	protected double[] predictedCostSignal;
	protected int predictionValidTime;

	/**
	 * This is net demand, may be +ve (consumption), 0, or -ve (generation)
	 * 
	 * TODO: JRS MUST reach agreement of sign for Demand vs. Generation and
	 * units - even if we introduce a factor such that we can translate between
	 * average kW in a timestep to kWh per timestep (kWaverage = kWh * 24 /
	 * ticks per day)
	 */
	protected double netDemand; // (note in kW)

	/*
	 * Economic variables which all prosumers will wish to calculate.
	 */
	protected double inelasticTotalDayDemand;

	// TODO : JRS - should this be here? Used only in HouseholdProsumer and
	// NonDomesticProsumer - set to zero otherwise
	protected double[] smartOptimisedProfile;

	/**
	 * Returns a string representation of this agent and its key values
	 * Currently is used by Repast as the method which produces and returns the
	 * probe ID.
	 * 
	 * @return a string representation of this agent
	 **/
	@Override
	@ProbeID()
	public String toString()
	{
		return this.getAgentName() + " " + this.getAgentID();
	}

	/**
	 * Returns a string representing the state of this agent. This method is
	 * intended to be used for debugging purposes, and the content and format of
	 * the returned string are left to the implementing concrete subclasses. The
	 * returned string may be empty but may not be <code>null</code>.
	 * 
	 * @return a string representation of this agent's state parameters
	 */
	protected abstract String paramStringReport();

	/**
	 * Returns the agent's ID.
	 * 
	 * @return unique ID number of this agent
	 */
	public long getAgentID()
	{
		return this.agentID;
	}

	/**
	 * Returns the agent's name. If the name has not been explicitly set, the
	 * default base name will be used.
	 * 
	 * @return agent's name as string
	 */
	public String getAgentName()
	{
		if (this.agentName == null && !this.nameExplicitlySet)
		{
			this.agentName = ProsumerAgent.agentBaseName;
		}
		return this.agentName;
	}

	/**
	 * Sets name of this agent
	 * 
	 * @param name
	 *            the string that is to be this agent's name
	 * @see #getName
	 */
	public void setAgentName(String name)
	{
		this.agentName = name;
		this.nameExplicitlySet = true;
	}

	/**
	 * Returns the context in which this agent is a part
	 * 
	 * @return the context where the agent is a part
	 **/
	public CascadeContext getContext()
	{
		return this.mainContext;
	}

	/**
	 * Determines whether this agent has a smart meter
	 * 
	 * @return <code>true</code> if the agent has smart meter,
	 *         <code>false</code> otherwise
	 */
	public boolean hasSmartMeter()
	{
		return this.hasSmartMeter;
	}

	/**
	 * Returns the net demand <code>netDemand</code> for this agent
	 * 
	 * @return the <code>netDemand</code>
	 **/
	public double getNetDemand()
	{
		return this.netDemand;
	}

	/**
	 * Sets the <code>netDemand</code> of this agent
	 * 
	 * @param nd
	 *            the new net demand for the agent
	 * @see #getNetDemand
	 */
	public void setNetDemand(double nd)
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("HHP: setND: " + nd);
		}
		this.netDemand = nd;
	}

	/**
	 * this method should define the step for Prosumer agents. The schedule
	 * should start at right timeslot among all concrete subclasses. Currently
	 * it starts at 0, however it does not have any priority restriction (such
	 * as being the last) so, it will be executed first (before the Aggregator
	 * which has the LAST priority)
	 */
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = Consts.PROSUMER_PRIORITY_FIFTH)
	abstract public void step();

	// ---------------------------------------------------------------
	// TODO: methodes declcared from here until the end of this class
	// may be changed/removed
	// ---------------------------------------------------------------

	/*
	 * Method used by display visualisation. Shouldn't normally be read by
	 * others outside the scope...
	 * 
	 * TODO: Is this implementation the best?
	 * 
	 * @return boolean indicating whether this prosumer has a smart control
	 * device
	 */
	public boolean getHasSmartControl()
	{
		return this.hasSmartControl;
	}

	/*
	 * Hacky mcHack to get the Repast Simphony visualisation working with this
	 * agent quickly
	 * 
	 * TODO: Something better than this needed...
	 */
	public double getHasSmartControlAsDouble()
	{
		if (this.hasSmartControl)
		{
			return 1d;
		}
		else
		{
			return 0d;
		}
	}

	public int getPredictedCostSignalLength()
	{
		return this.predictedCostSignal.length;
	}

	public double getCurrentPrediction()
	{
		int timeSinceSigValid = (int) RepastEssentials.GetTickCount() - this.getPredictionValidTime();
		if (this.predictedCostSignal.length > 0 && timeSinceSigValid >= 0)
		{
			return this.getPredictedCostSignal()[timeSinceSigValid % this.predictedCostSignal.length];
		}
		else
		{
			return 0;
		}
	}

	/**
	 * TODO: check if it is needed (may changed/removed)
	 * 
	 * @return the predictionValidTime
	 */
	public int getPredictionValidTime()
	{
		return this.predictionValidTime;
	}

	/**
	 * TODO: check if it is needed (may changed/removed)
	 * 
	 * @param predictionValidTime
	 *            the predictionValidTime to set
	 */
	public void setPredictionValidTime(int predictionValidTime)
	{
		this.predictionValidTime = predictionValidTime;
	}

	/*
	 * TODO: this methods needs to be changed/removed; this will done by the
	 * SmartDevice class mechanism which will be an Observer and get updated
	 * from aggregator (observable)
	 * 
	 * This method receives the centralised value signal and stores it to the
	 * Prosumer's memory.
	 * 
	 * @param signal - the array containing the cost signal - one member per
	 * time tick
	 * 
	 * @param length - the length of the signal
	 * 
	 * @param validTime - the time (in ticks) from which the signal is valid
	 */
	public boolean receiveValueSignal(double[] signal, int length)
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("ProsumerAgent:: receiveValueSignal()");
		}
		boolean success = true;
		// Can only receive if we have a smart meter to receive data
		int validTime = (int) RepastEssentials.GetTickCount();

		if (this.hasSmartMeter)
		{
			// Note the time from which the signal is valid.
			// Note - Repast can cope with fractions of a tick (a double is
			// returned)
			// but I am assuming here we will deal in whole ticks and alter the
			// resolution should we need
			int time = (int) RepastEssentials.GetTickCount();
			int newSignalLength = length;
			this.setPredictionValidTime(validTime);
			double[] tempArray;

			int signalOffset = time - validTime;
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("time: " + time + " validTime" + validTime);
			}
			if (signalOffset != 0)
			{

				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("ProsumerAgent: Signal valid from time other than current time");
				}

				newSignalLength = newSignalLength - signalOffset;
			}

			if ((this.getPredictedCostSignal() == null) || (newSignalLength != this.predictedCostSignal.length))
			{

				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("ProsumerAgent: Re-defining length of signal in agent" + this.agentID);
				}

				this.setPredictedCostSignal(new double[newSignalLength]);
			}

			if (signalOffset < 0)
			{
				// This is a signal projected into the future.
				// pad the signal with copies of what was in it before and then
				// add the new signal on
				System.arraycopy(signal, 0, this.getPredictedCostSignal(), 0 - signalOffset, length);
			}
			else
			{
				// This was valid from now or some point in the past. Copy in
				// the portion that is still valid and
				// then "wrap" the front bits round to the end of the array.
				System.arraycopy(signal, signalOffset, this.predictedCostSignal, 0, length);
			}
		}

		return success;
	}

	private void setSignalType_S(double[] signal)
	{

	}

	/**
	 * This method is used by other agents (e.g. Aggregator) to send their
	 * signal to prosumer's agent. It receives different types of signals
	 * containing Real values.
	 * 
	 * TODO: in reality, these signals are sent to customer's device, so if in
	 * the future, there will be a separate smart device object, this method
	 * should be implemented there and not in the prosumer's agent directly.
	 * 
	 * @param signal
	 *            the array containing the signal
	 * @param length
	 *            the length of the signal
	 * @param signalType
	 *            the type of the signal
	 * @return true if signal received successfully, false otherwise
	 */
	public boolean receiveSignal(double[] signal, int length, Consts.SIGNAL_TYPE signalType)
	{
		boolean signalRecievedSuccessfully = false;

		switch (signalType)
		{
		case S:
			this.setSignalType_S(signal);
			break;
		default: //
			break;
		}
		return signalRecievedSuccessfully;
	}

	public double getInsolation()
	{
		return this.insolation;
	}

	public double getWindSpeed()
	{
		return this.windSpeed;
	}

	public double getAirTemperature()
	{
		return this.airTemperature;
	}

	public double getAirDensity()
	{
		return this.airDensity;
	}

	/*
	 * TODO: is this how the prosumer should get updated about the weather This
	 * methods needs to be changed/removed; this will done by the SmartDevice
	 * class
	 */
	protected void checkWeather(int time)
	{
		// Note at the moment, no geographical info is needed to read the
		// weather
		// this is because weather is a flat file and not spatially
		// differentiated
		// CascadeContext myContext = (CascadeContext) FindContext(contextName);
		this.insolation = this.mainContext.getInsolation(time);
		this.windSpeed = this.mainContext.getWindSpeed(time);
		this.airTemperature = this.mainContext.getAirTemperature(time);
		this.airDensity = this.mainContext.getAirDensity(time);
	}

	/**
	 * @param predictedCostSignal
	 *            the predictedCostSignal to set
	 */
	public void setPredictedCostSignal(double[] predictedCostSignal)
	{
		this.predictedCostSignal = Arrays.copyOf(predictedCostSignal, predictedCostSignal.length);
	}

	/**
	 * @return the predictedCostSignal
	 */
	public double[] getPredictedCostSignal()
	{
		return this.predictedCostSignal;
	}

	/**
	 * Constructs a prosumer agent with the context in which is created
	 * 
	 * @param context
	 *            the context in which this agent is situated
	 */
	public ProsumerAgent(CascadeContext context)
	{

		this.agentID = ProsumerAgent.agentIDCounter++;
		this.mainContext = context;
	}

	@ScheduledMethod(start = 0, interval = 0, priority = Consts.PRE_INITIALISE_FIRST_TICK)
	private void initContext()
	{
		if (this.mainContext == null)
		{
			this.mainContext = (CascadeContext) ContextUtils.getContext(this);
		}
	}

	public ProsumerAgent()
	{
		this.agentID = ProsumerAgent.agentIDCounter++;
	}

}
