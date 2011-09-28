package uk.ac.dmu.iesd.cascade.context;


import java.util.*;

import org.hsqldb.lib.ArrayUtil;

import repast.simphony.engine.schedule.*;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.space.graph.*;
import repast.simphony.ui.probe.*;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.IObservable;
import uk.ac.dmu.iesd.cascade.util.IObserver;
import uk.ac.dmu.iesd.cascade.util.ObservableComponent;
import flanagan.math.Matrix;


/**
 *  An <em>AggregatorAgent</em> is an object that represents a commercial/business 
 *  entity providing energy services to prosumer agents (<code>ProsumerAgent</code>) such
 *  as household prosumers (<code>HouseholdProsumer</code>) [i.e. it is involved in retail trade], 
 *  while at the same time is securing its gross energy need by trading in the wholesale market.
 *  The <code>AggregatorAgent</code> class is the abstract superclass of aggregator agents.
 *  Examples of aggregator agents include ESCO, RECO and GENCO. <p>
 *  
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.2 $ $Date: 2011/05/19 13:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history)
 * 
 * 1.1 - Implements ICognitiveAgent (Babak)
 * 1.2 - Made the class abstract; modified the constructor, added/modified/removed fields/methods
 *       made some methods abstract (Babak)
 */
public abstract class AggregatorAgent implements ICognitiveAgent, IObservable {

	/*
	 * Agent properties
	 */
	/**
	 * This field is used for counting number of agents 
	 * instantiated by concrete descendants of this class  
	 **/	
	private static long agentIDCounter = 0; 

	/**
	 * An aggregator agent's base name  
	 * it can be reassigned (renamed) properly by descendants of this class  
	 **/	
	protected static String agentBaseName = "aggregator";


	/**
	 * an aggregator agent's ID
	 * This field is automatically assigned by constructor 
	 * when the agent is created for the first time
	 * */
	protected long agentID = -1; 


	/**
	 * An aggregator agent's name
	 * This field can be <code>null</code>.
	 * */
	protected String agentName;

	/**
	 * This field (C) is the "marginal cost" per KWh in the ith timeslot 
	 * (which aggregator can predict 24 hours ahead).
	 * This is the cost for the part that cosumer's demand that cannot be predicted further
	 * ahead and supplied via long term contracts because it is variable due to weather, TV schedules, or owns
	 * generators not being able to meet part of the predicated demand. 
	 * For the inital experiment, C would be proportional to national demand Ni with 
	 * the option to make it Ni^2 or fractional power
	 **/
	float[] arr_i_C; 
	
	float[] arr_i_B; // (B) baseline at timeslot i

	/**
	 * A boolen to determine whether the name has
	 * been set explicitly. <code>nameExplicitlySet</code> will
	 * be false if the name has not been set and true if it has.
	 * @see #getName
	 * @see #setName(String)
	 */
	protected boolean nameExplicitlySet = false;

	protected CascadeContext mainContext;

	protected ObservableComponent observableProxy;

	boolean autoControl;
	//String contextName;
	/*
	 * This is net demand, may be +ve (consumption), 0, or 
	 * -ve (generation)
	 */
	protected float netDemand;
	float[] predictedCustomerDemand;
	float[] overallSystemDemand;
	// priceSignal units are £/MWh which translates to p/kWh if divided by 10
	float[] priceSignal;
	boolean priceSignalChanged = true;  //set true when we wish to send a new and different price signal.  
	//True by default as it will always be new until the first broadcast
	protected int ticksPerDay;
	
	protected ArrayList<Float> dailyPredictedCost;
	//private float[] dailyPredictedCostArr;

	protected ArrayList<Float> dailyActualCost; 
	
	protected float cumulativeCostSaving =0;


	//-----------------------------


	/**
	 * Returns a string representation of this agent and its key values 
	 * Currently is used by Repast as the method which produces and returns the probe ID.  
	 * @return  a string representation of this agent
	 **/
	@ProbeID()
	public String toString() {	
		return getClass().getName()+" "+getAgentID();
	}

	/**
	 * Returns a string representing the state of this agent. This 
	 * method is intended to be used for debugging purposes, and the 
	 * content and format of the returned string are left to the implementing 
	 * concrete subclasses. The returned string may be empty but may not be 
	 * <code>null</code>.
	 * 
	 * @return a string representation of this agent's state parameters
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
	 * Returns the net demand <code>netDemand</code> for this agent (D)
	 * @return  the <code>netDemand (D)</code> 
	 **/
	public float getNetDemand() {
		return this.netDemand;
	}
	
	/**
	 * Sets the <code>netDemand</code> of this agent  
	 * @param nd the new net demand for the agent
	 * @see #getNetDemand
	 */
	public void setNetDemand(float nd) {
		this.netDemand = nd;
	}
	
	

	/**
	 * Returns the price signal <code>priceSignal</code> for this agent (S)
	 * @return  the <code>priceSignal (S)</code> 
	 **/
	public float getCurrentPriceSignal()
	{
		double time = RepastEssentials.GetTickCount();
		return priceSignal[(int) time % priceSignal.length];
	}


	
	/*public float getCurrentCostOfDemand() {
		return getCurrentCost_C() * this.getNetDemand();
	} */
	/**
	 * This method returns the cost of buying wholesale electricity for this aggregator
	 * at the current tick
	 */
	
	public float getCurrentCost_C()
	{
		return arr_i_C[(int) RepastEssentials.GetTickCount() % ticksPerDay];
	}
	
	public float getCurrentBaseline_B()
	{
		return arr_i_B[(int) RepastEssentials.GetTickCount() % ticksPerDay];
	}
	
	
	public float getDayPredictedCost()
	{
		if (dailyPredictedCost.size() > 0)
			return  dailyPredictedCost.get(mainContext.getDayCount()-1);
		else return 0;
	}
	
	public float getDayActualCost()
	{
		if (dailyActualCost.size() > 0)
			return  dailyActualCost.get(mainContext.getDayCount()-1);
		else return 0;
	}
	
	
	public float getCostDifference()
	{
		if (mainContext.getDayCount() <= Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) 
			return 0;
		else return getDayPredictedCost() - getDayActualCost();
	}
	
	public float getCumulativeCostSaving()
	{
		 return this.cumulativeCostSaving;
	}
	
	public int getDayCount() {
		return mainContext.getDayCount();
	}
	

	/**
	 * This method should define the step for the agents.
	 * They should be scheduled appropriately by 
	 * concrete implementing subclasses 
	 */
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	abstract public void step();

	/**
	 * Add an observer to the list of observer objects
	 * @param anIObserver the observer (IObsever) who wants to be added and updated
	 *                    when a specific state changes or event occurs 
	 */
	public void addObserver(IObserver anIObserver){
		observableProxy.addObserver(anIObserver);
	}


	/**
	 * Deletes an observer from the list of observer objects
	 * @param anIObserver the observer (IObsever) who wants to be removed from the observers' list
	 */
	public void deleteObserver(IObserver anIObserver) {
		observableProxy.deleteObserver(anIObserver);
	}

	/**
	 * Clears the list of observers
	 */
	public void deleteObservers(){
		observableProxy.deleteObservers();
	}

	/**
	 * Returns the number of observers in the list
	 * @return number (count) of observers (in the list) 
	 */
	public int countObservers(){
		return observableProxy.countObservers();
	}

	/**
	 * This method can be called when a specific state in an aggregator object 
	 * (in which an observer object is interested) has changed. 
	 * It notifies all of its observers by calling their  <code>update</code> method.
	 * Here this task is delegated to an instance of <code>ObservableComponent</code> (observableProxy)
	 * which implements the necessary code for doing performing the task.
	 * @param   obs the observed object.
	 * @param   changeCodeArg the changed code argument.
	 * @see   ObservableComponent#notifyObservers(Object,Object)
	 * @see   IObserver#update(Object,Object)
	 */
	protected void notifyObservers(Object obs, Object changeCodeArg) {
		observableProxy.notifyObservers(obs, changeCodeArg);
	}



	// ------------------------------------------------------------------------------
	/* TODO: 
	 * the methods defined below will be checked later. 
	 * In part, it must be determined which are the common behavior of  
	 * all aggregator agents and whether different specific aggregator agents
	 * behave differently or the same when it comes to an specific method.
	 * These will determine which method signatures should stay here (and the implementation will 
	 * be the concrete agrregator responsibility) and which one stay here as common behavior for
	 * all aggregator agent (like the ones defined above). / Babak  
	 */

	

	void setPriceSignalFlatRate(float price)
	{
		float[] oldPrice = priceSignal;
		Arrays.fill(priceSignal, price);
		priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
	}

	void setPriceSignalEconomySeven(float highprice, float lowprice)
	{
		//Hack to change E7 tariff at 07:30
		int morningChangeTimeIndex = (int) (ticksPerDay / (24 / 7.5));
		//Hack to change E7 tariff at 23:30
		int eveningChangeTimeIndex = (int) (ticksPerDay / (24 / 23.5));
		float[] oldPrice = Arrays.copyOf(priceSignal,priceSignal.length);

		for(int offset = 0; offset < priceSignal.length; offset = offset + ticksPerDay)
		{
			Arrays.fill(priceSignal, offset, offset + morningChangeTimeIndex, lowprice);
			Arrays.fill(priceSignal, offset + morningChangeTimeIndex, offset + eveningChangeTimeIndex, highprice);
			Arrays.fill(priceSignal, offset + eveningChangeTimeIndex, offset + priceSignal.length, lowprice);
			priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
		}
	}

	void setPriceSignalRoscoeAndAult(float A, float B, float C)
	{
		float price;
		float x;

		for (int i = 0; i < priceSignal.length; i++)
		{	
			//Note that the division by 10 is to convert the units of predicted customer demand
			//to those compatible with capacities expressed in GW.
			//TODO: unify units throughout the model
			x = (predictedCustomerDemand[i % ticksPerDay] / 10 ) / (Consts.MAX_SUPPLY_CAPACITY_GWATTS - Consts.MAX_GENERATOR_CAPACITY_GWATTS);
			price = (float) (A * Math.exp(B * x) + C);
			if ((Boolean) RepastEssentials.GetParameter("verboseOutput"))
			{
				System.out.println("AggregatorAgent: Price at tick" + i + " is " + price);
			}
			if (price > Consts.MAX_SYSTEM_BUY_PRICE_PNDSPERMWH) 
			{
				price = Consts.MAX_SYSTEM_BUY_PRICE_PNDSPERMWH;
			}
			priceSignal[i] = price;
		}
		priceSignalChanged = true;
	}

	void setPriceSignalExpIncreaseOnOverCapacity(int time)
	{
		//This is where we may alter the signal based on the demand
		// In this simple implementation, we simply scale the signal based on deviation of 
		// actual demand from projected demand for use next time round.

		//Define a variable to hold the aggregator's predicted demand at this instant.
		float predictedInstantaneousDemand;
		// There are various things we may want the aggregator to do - e.g. learn predicted instantaneous
		// demand, have a less dynamic but still non-zero predicted demand 
		// or predict zero net demand (i.e. aggregators customer base is predicted self-sufficient

		//predictedInstantaneousDemand = predictedCustomerDemand[(int) time % predictedCustomerDemand.length];
		predictedInstantaneousDemand = 0;

		if (netDemand > predictedInstantaneousDemand) {
			priceSignal[(int) time % priceSignal.length] = (float) (priceSignal[(int) time % priceSignal.length] * ( 1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			// Now introduce some prediction - it was high today, so moderate tomorrow...
			if (priceSignal.length > ((int) time % priceSignal.length + ticksPerDay))
			{
				priceSignal[(int) time % priceSignal.length + ticksPerDay] = (float) (priceSignal[(int) time % priceSignal.length + ticksPerDay] * ( 1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			}
			priceSignalChanged = true; }
	}

	void setPriceSignalScratchTest()
	{
		//This is where we may alter the signal based on the demand
		// In this simple implementation, we simply scale the signal based on deviation of 
		// actual demand from projected demand for use next time round.

		//Define a variable to hold the aggregator's predicted demand at this instant.

		// There are various things we may want the aggregator to do - e.g. learn predicted instantaneous
		// demand, have a less dynamic but still non-zero predicted demand 
		// or predict zero net demand (i.e. aggregators customer base is predicted self-sufficient

		//predictedInstantaneousDemand = predictedCustomerDemand[(int) time % predictedCustomerDemand.length];

		priceSignal = ArrayUtils.multiply(overallSystemDemand, 1 / ArrayUtils.max(overallSystemDemand));
	}

	void setPriceSignalZero()
	{
		Arrays.fill(priceSignal,0f);
		priceSignalChanged = true;
	}

	/*
	 * helper methods
	 */
	private void broadcastDemandSignal(List<ProsumerAgent> broadcastCusts, double time, int broadcastLength) {


		// To avoid computational load (and realistically model a reasonable broadcast strategy)
		// only prepare and transmit the price signal if it has changed.
		if(priceSignalChanged)
		{
			//populate the broadcast signal with the price signal starting from now and continuing for
			//broadcastLength samples - repeating copies of the price signal if necessary to pad the
			//broadcast signal out.
			float[] broadcastSignal= new float[broadcastLength];
			int numCopies = (int) Math.floor((broadcastLength - 1) / priceSignal.length);
			int startIndex = (int) time % priceSignal.length;
			System.arraycopy(priceSignal,startIndex,broadcastSignal,0,priceSignal.length - startIndex);
			for (int i = 1; i <= numCopies; i++)
			{
				int addIndex = (priceSignal.length - startIndex) * i;
				System.arraycopy(priceSignal, 0, broadcastSignal, addIndex, priceSignal.length);
			}

			if (broadcastLength > (((numCopies + 1) * priceSignal.length) - startIndex))
			{
				System.arraycopy(priceSignal, 0, broadcastSignal, ((numCopies + 1) * priceSignal.length) - startIndex, broadcastLength - (((numCopies + 1) * priceSignal.length) - startIndex));
			}

			for (ProsumerAgent a : broadcastCusts){
				// Broadcast signal to all customers - note we simply say that the signal is valid
				// from now currently, in future implementations we may want to be able to insert
				// signals valid at an offset from now.
				if (Consts.DEBUG)
				{
					//System.out.println("Broadcasting to " + a.sAgentID);
				}
				a.receiveValueSignal(broadcastSignal, broadcastLength);
			}
		}

		priceSignalChanged = false;
	}


	/**
	 * Constructs a prosumer agent with the context in which is created
	 * @param context the context in which this agent is situated 
	 */
	public AggregatorAgent(CascadeContext context) {
		this.agentID = agentIDCounter++;
		this.mainContext = context;
		observableProxy = new ObservableComponent();
	}


}
