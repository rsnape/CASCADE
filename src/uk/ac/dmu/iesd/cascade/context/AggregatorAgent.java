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
	 * an aggregator agent's ID
	 * This field is automatically assigned by constructor 
	 * when the agent is created for the first time
	 * */
	protected long agentID = -1; 

	/**
	 * This field is used for counting number of agents 
	 * instantiated by concrete descendants of this class  
	 **/	
	private static long agentIDCounter = 0; 

	/**
	 * An aggregator agent's name
	 * This field can be <code>null</code>.
	 * */
	protected String agentName;

	/**
	 * An aggregator agent's base name  
	 * it can be reassigned (renamed) properly by descendants of this class  
	 **/	
	protected static String agentBaseName = "aggregator";

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
	int predictedCustomerDemandLength;
	float[] overallSystemDemand;
	int overallSystemDemandLength;
	// priceSignal units are £/MWh which translates to p/kWh if divided by 10
	float[] priceSignal;
	int priceSignalLength;
	boolean priceSignalChanged = true;  //set true when we wish to send a new and different price signal.  
	//True by default as it will always be new until the first broadcast
	protected int ticksPerDay;
	
	
	//-----------------------------
	
	/**
	 * This field (e) is "price elasticity factor" at timeslot i
	 * It is extend to which total demand in the day is reduced or increased by the value of S
	 * (given S*e) 
	 * there are 48 of them (day divided into 48 timeslots)
	 * When a single no zero value of S is broadcast by the aggregator in the ith timeslot, 
	 * the total aggregated response from the prosumers will involve changes to demand form 
	 * baseline in some or all timeslots. If those changes are added up for the day, the 
	 * net value which may be + or - tells us the value of S*e. 
	 * Since we know S, we can get the e for the ith timeslot in which the S was broadcast. 
	 **/
	float[] arr_i_e; 
	
	/**
	 * This field (k) is "displacement factor" at timeslot ij
	 * There are 48^2 of them (48 values at each timeslot; a day divided into 48 timeslots)
	 * It is calculated in the training process. 
	 **/
	float[][] arr_ij_k; 
	
	/**
	 * This field (S) is "signal" at timeslot i sent to customer's (prosumers)
	 * so that they response to it accordingly. For example, if S=0, the aggregator can 
	 * assume that the prosumers respond with their default behavior, so there is 
	 * no timeshifting or demand or elastic response to price and the result is a baseline
	 * demand aggregate B
	 * When Si is not zero, the aggregator can calculate 
	 * the resultant aggregate deviation (delta_Bi)
	 **/
	float[] arr_i_S;  // (S) signal at timeslot i
	
	float[] arr_i_B;  // (B) baseline at timeslot i
	
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
		
	/**
	 * This 2D-array is used to keep the usual aggregate demand of all prosumers at each timeslot 
	 * of the day during both profile building and training periods (usually about 7 + 48 days). 
	 * In other words, the length of the columns of this 2D history array is equal to number of timeslot 
	 * during a day and the length of its rows is equal to the number of days the profile building and training 
	 * periods last.
	 *  
	 * TODO: if all the aggregator have this default behavior (e.g. building profile in the same way)
	 * this field may stay here otherwise, it will need to move to the appropriate implementor (e.g. RECO) 
	 **/
	float[][] hist_arr_ij_D; 
	
	/**
	 * This array is used to keep the average (i.e. baseline) of aggregate demands (D)
	 * (usually kept in the 2D history D array (hist_D_ij_arr))  
	 * TODO: if all the aggregator have this default behavior (e.g. building profile in the same way)
	 * this field may stay here otherwise, it will need to move to the appropriate implementor (e.g. RECO) 
	 **/
	//float[] histAvg_B_i_arr; 


	
	/**
	 * Constructs a prosumer agent with the context in which is created
	 * @param context the context in which this agent is situated 
	 */
	public AggregatorAgent(CascadeContext context) {
		this.agentID = agentIDCounter++;
		this.mainContext = context;
		observableProxy = new ObservableComponent();
	}


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
	 * Returns the net demand <code>netDemand</code> for this agent 
	 * @return  the <code>netDemand</code> 
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
	
	
	/**
	 * This method calculates and returns the price (Pi) per kWh 
	 * at given time-slot.
	 * (It implements the formula proposed by P. Boait, Formula #2) 
	 * Parameters a and b are "fixed pricing" parameters  and set by the aggregator and 
	 * known to the prosumer's smart meter and other devices so they react to Pi.  
	 * The summation of all Si to zero simplifies their calculation!
	 * In practice, it would be helpful to use normalized values between 1 and -1 to make up S
	 * @param   timeslot time slot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return Price(Pi) per KWh at given timeslot (i) 
	 */
	protected float calculate_Price_P(int timeslot) {
		float a = 2f; // the value of a must be set to a fixed price, e.g. ~baseline price 
		float b = 0.2f;  //this value of b amplifies the S value signal, to reduce or increase the price
		
		float Si = this.arr_i_S[timeslot];
        float Pi = a+ (b*Si);
        
		return Pi;
	}
	

	/**
	 * This method calculates and returns the baseline aggregate deviation (DeltaBi) at 
	 * given time-slot.
	 * It is invoked by aggregator when the Si is not zero and the aggregator
	 * wants to form a response.
	 * (It implements the formula proposed by P. Boait, Formula #3) 
	 * @param   timeslot_i time slot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return Baseline aggregate deviation (DelatBi) at given timeslot (i) 
	 */

	private float calculate_deltaB(int timeslot_i) {	
		float sumOf_SjKijBi=0;
		for (int j = 0; j < ticksPerDay; j++) {
			if (j != timeslot_i) // i!=j
				sumOf_SjKijBi = this.arr_i_S[timeslot_i]*this.arr_ij_k[timeslot_i][j]*this.arr_i_B[timeslot_i];
		}
		float leftSideEq = this.arr_i_S[timeslot_i]*this.arr_ij_k[timeslot_i][timeslot_i]*this.arr_i_B[timeslot_i];
		float deltaBi = leftSideEq + sumOf_SjKijBi;
		return deltaBi;
	}

	/**
	 * This method calculates and returns the demand predicted by the aggregator in each
	 * time-slot, taking account of both elastic and displacement changes.
	 * (It implements the formula proposed by P. Boait, Formula #4)
	 * When e=0 (i.e. all ei=0), then the sum of all Di = sum of all Bi
	 * @param   timeslot time slot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return Demand (Di) predicted by the aggregator at given timeslot (i) 
	 */
	protected float calcualte_PredictedDemand_D(int timeslot) {
		float Bi = this.arr_i_B[timeslot];
		float Si = this.arr_i_S[timeslot];
		float ei = this.arr_i_e[timeslot];

		float delta_Bi = calculate_deltaB(timeslot);

		float Di= Bi + (Si*ei*Bi) + delta_Bi;
		return Di;
	}
	
	/**
	 * This method calculates and returns "price elasticity factor" (e) at a given time-slot.
	 * (It implements the formula proposed by P. Boait, Formula #6)
	 * @param arr_D a float array containing aggregate demand (D) values for a timeslot of a day (usually 48 timeslots)
	 * @param arr_B a float array containing average baseline aggregate demand (B) values for each timeslot of a day (usulaly 48 timeslots) 
	 * @param s signal value at timeslot i
	 * @param B average baseline aggregate demand (B) value at timeslot i	 	 
	 * @return elasticity price factor (e) [at timeslot i]
	 */
	protected float calculate_e(float[] arr_D, float[] arr_B, float s, float B) {
		
	    float e=0;
		float sum_D = ArrayUtils.sum(arr_D);
		float sum_B = ArrayUtils.sum(arr_B);
		if (( s!=0) && (B!=0))
			e = (sum_D - sum_B) / (s*B);
		return e;
	}
	
	/**
	 * This method calculates and returns "displacement factor" (k) at given time-slots.
	 * (It implements the formula proposed by P. Boait, Formula #7 and #8)
	 * By stepping Si=1 through all the timeslots over 48 days, the aggregator obtains
	 * complete set of estimates for e and k. By repeating this training (if necessary) more
	 * accurate estimates can be obtained. 
	 * @param   timeslot time slot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return displacement factor (Kij) at given timeslots (i and j) 
	 */
	protected float calculate_k(int t_i, int t_j) {

		float k_ij = 0;
		float divisor = 1;

		if (t_i == t_j) {  // calculate Kii
			float delta_Bi= this.calculate_deltaB(t_i);
			float divident = delta_Bi - (this.arr_i_S[t_i] * this.arr_i_e[t_i] * this.arr_i_B[t_i]);
			divisor= this.arr_i_S[t_i] * this.arr_i_B[t_i];
			k_ij = divident/divisor;
		}
		
		else {  // calculate Kij
			float delta_Bj= this.calculate_deltaB(t_j);
			divisor= this.arr_i_S[t_i] * this.arr_i_B[t_j];
			k_ij = delta_Bj /divisor;
		}

		return k_ij;
	}
	
	/**
	 * This method calculates the predict demand at each given time-slot.
	 * @param   t timeslot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return displacement factor (Kij) at given timeslots (i and j) 
	 */
	protected void predictDemand(List<ProsumerAgent> customersList, int t) {
		
		float sumDemand = 0;
		for (ProsumerAgent a : customersList)
		{
			sumDemand = sumDemand + a.getNetDemand();
		}
		this.arr_i_B[t] = sumDemand;
		
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
	
	public float getCurrentPriceSignal()
	{
		double time = RepastEssentials.GetTickCount();
		return priceSignal[(int) time % priceSignalLength];
	} 
	
	void setPriceSignalFlatRate(float price)
	{
		float[] oldPrice = priceSignal;
		Arrays.fill(priceSignal, price);
		priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
	}
	
	void setPriceSignalEconomySeven(float highprice, float lowprice)
	{
		int morningChangeTimeIndex = (int) (ticksPerDay / (24 / 7.5));
		int eveningChangeTimeIndex = (int) (ticksPerDay / (24 / 23.5));
		float[] oldPrice = priceSignal;
		Arrays.fill(priceSignal, 0, morningChangeTimeIndex, lowprice);
		Arrays.fill(priceSignal, morningChangeTimeIndex + 1, eveningChangeTimeIndex, highprice);
		Arrays.fill(priceSignal, eveningChangeTimeIndex + 1, priceSignal.length - 1, lowprice);
		priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
	}
	
	void setPriceSignalRoscoeAndAult(float A, float B, float C)
	{
		float price;
		float x;
		
		for (int i = 0; i < priceSignalLength; i++)
		{	
			//Note that the division by 10 is to convert the units of predicted customer demand
			//to those compatible with capacities expressed in GW.
			//TODO: unify units throughout the model
			x = (predictedCustomerDemand[i % ticksPerDay] / 10 ) / (Consts.MAX_SUPPLY_CAPACITY_GWATTS - Consts.MAX_GENERATOR_CAPACITY_GWATTS);
			price = (float) (A * Math.exp(B * x) + C);
			if ((Boolean) RepastEssentials.GetParameter("verboseOutput"))
			{
			System.out.println("Price at tick" + i + " is " + price);
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
		
		//predictedInstantaneousDemand = predictedCustomerDemand[(int) time % predictedCustomerDemandLength];
		predictedInstantaneousDemand = 0;
		
		if (netDemand > predictedInstantaneousDemand) {
			priceSignal[(int) time % priceSignalLength] = (float) (priceSignal[(int) time % priceSignalLength] * ( 1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			// Now introduce some prediction - it was high today, so moderate tomorrow...
			if (priceSignalLength > ((int) time % priceSignalLength + ticksPerDay))
			{
				priceSignal[(int) time % priceSignalLength + ticksPerDay] = (float) (priceSignal[(int) time % priceSignalLength + ticksPerDay] * ( 1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
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
		
		//predictedInstantaneousDemand = predictedCustomerDemand[(int) time % predictedCustomerDemandLength];
				
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
			int numCopies = (int) Math.floor((broadcastLength - 1) / priceSignalLength);
			int startIndex = (int) time % priceSignalLength;
			System.arraycopy(priceSignal,startIndex,broadcastSignal,0,priceSignalLength - startIndex);
			for (int i = 1; i <= numCopies; i++)
			{
				int addIndex = (priceSignalLength - startIndex) * i;
				System.arraycopy(priceSignal, 0, broadcastSignal, addIndex, priceSignalLength);
			}

			if (broadcastLength > (((numCopies + 1) * priceSignalLength) - startIndex))
			{
				System.arraycopy(priceSignal, 0, broadcastSignal, ((numCopies + 1) * priceSignalLength) - startIndex, broadcastLength - (((numCopies + 1) * priceSignalLength) - startIndex));
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
	
	

}
