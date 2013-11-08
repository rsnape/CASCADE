package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.ui.probe.ProbeID;
import repast.simphony.util.ContextUtils;
import uk.ac.dmu.iesd.cascade.agents.ICognitiveAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.IObservable;
import uk.ac.dmu.iesd.cascade.util.IObserver;
import uk.ac.dmu.iesd.cascade.util.ObservableComponent;

/**
 * An <em>AggregatorAgent</em> is an object that represents a
 * commercial/business entity providing energy services to prosumer agents (
 * <code>ProsumerAgent</code>) such as household prosumers (
 * <code>HouseholdProsumer</code>) [i.e. it is involved in retail trade], while
 * at the same time is securing its gross energy need by trading in the
 * wholesale market. The <code>AggregatorAgent</code> class is the abstract
 * superclass of aggregator agents. Examples of aggregator agents include ESCO,
 * RECO and GENCO.
 * <p>
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.3 $ $Date: 2012/05/16 13:00:00 $
 * 
 *          Version history (for intermediate steps see Git repository history)
 * 
 * 			1.0 - initial version including boiler plate stuff and netDemand etc.(JRS)
 *          1.1 - Implements ICognitiveAgent (Babak) 1.2 - Made the class
 *          abstract; modified the constructor, added/modified/removed
 *          fields/methods made some methods abstract (Babak) 1.3 - Modified the
 *          base class for ASTEM market integration, notably by adding category
 *          and type (Babak)
 */
public abstract class AggregatorAgent implements ICognitiveAgent, IObservable
{

	/*
	 * Agent properties
	 */
	/**
	 * This field is used for counting number of agents instantiated by concrete
	 * descendants of this class
	 **/
	private static int agentIDCounter = 0;

	/**
	 * An aggregator agent's base name it can be reassigned (renamed) properly
	 * by descendants of this class
	 **/
	protected static String agentBaseName = "Aggregator";

	/**
	 * an aggregator agent's ID This field is automatically assigned by
	 * constructor when the agent is created for the first time
	 * */
	protected int id = -1;

	/**
	 * An aggregator agent's name This field can be <code>null</code>.
	 * */
	protected String agentName;

	/**
	 * This field (C) is the "marginal cost" per KWh in the ith timeslot (which
	 * aggregator can predict 24 hours ahead). This is the cost for the part
	 * that cosumer's demand that cannot be predicted further ahead and supplied
	 * via long term contracts because it is variable due to weather, TV
	 * schedules, or owns generators not being able to meet part of the
	 * predicated demand. For the inital experiment, C would be proportional to
	 * national demand Ni with the option to make it Ni^2 or fractional power
	 **/
	double[] arr_i_C;

	double[] arr_i_norm_C; // normalized costs

	double[] arr_i_B; // (B) baseline at timeslot i

	/**
	 * This field (e) is "price elasticity factor" at timeslot i It is extend to
	 * which total demand in the day is reduced or increased by the value of S
	 * (given S*e) there are 48 of them (day divided into 48 timeslots) When a
	 * single no zero value of S is broadcast by the aggregator in the ith
	 * timeslot, the total aggregated response from the prosumers will involve
	 * changes to demand form baseline in some or all timeslots. If those
	 * changes are added up for the day, the net value which may be + or - tells
	 * us the value of S*e. Since we know S, we can get the e for the ith
	 * timeslot in which the S was broadcast.
	 **/
	double[] arr_i_e;

	/**
	 * This field (k) is "displacement factor" at timeslot ij There are 48^2 of
	 * them (48 values at each timeslot; a day divided into 48 timeslots) It is
	 * calculated in the training process.
	 **/
	double[][] arr_ij_k;

	/**
	 * A boolen to determine whether the name has been set explicitly.
	 * <code>nameExplicitlySet</code> will be false if the name has not been set
	 * and true if it has.
	 * 
	 * @see #getName
	 * @see #setName(String)
	 */
	protected boolean nameExplicitlySet = false;

	protected CascadeContext mainContext;

	protected ObservableComponent observableProxy;

	boolean autoControl;
	// String contextName;
	/*
	 * This is net demand, may be +ve (consumption), 0, or -ve (generation)
	 */
	protected double netDemand;
	double[] predictedCustomerDemand;
	double[] overallSystemDemand;
	// priceSignal units are £/MWh which translates to p/kWh if divided by 10
	double[] priceSignal;
	boolean priceSignalChanged = true; // set true when we wish to send a new
										// and different price signal.
	// True by default as it will always be new until the first broadcast
	protected int ticksPerDay;

	protected ArrayList<Double> dailyPredictedCost;
	// private double[] dailyPredictedCostArr;

	protected ArrayList<Double> dailyActualCost;

	protected double cumulativeCostSaving = 0;

	protected double[] arr_day_D;

	protected double[] arr_PN; // Physical Notifications
	protected double[] arr_oldPN; // (old/previous day) Physical Notifications

	protected BMU_CATEGORY category;
	protected BMU_TYPE type;

	protected double maxGen = 0; // maximum generation
	private double minGen = 0;
	protected double maxDem = 0; // maximum demand
	protected double minDem = 0;

	public double getMaxGenCap()
	{
		return maxGen;
	}

	public double getMinDemCap()
	{
		return minDem;
	}

	public BMU_CATEGORY getCategory()
	{
		return category;
	}

	public BMU_TYPE getType()
	{
		return type;
	}

	public String getCategoryAsString()
	{
		String categoryInString;
		switch (this.category)
		{
		case GEN_T:
			categoryInString = "T";
			break;
		case GENDEM_E:
			categoryInString = "E";
			break;
		case GENDEM_I:
			categoryInString = "I";
			break;
		case DEM_S:
			categoryInString = "S";
			break;
		default:
			categoryInString = "Invalid day";
			break;
		}
		return categoryInString;
	}

	public double[] getPN()
	{
		return this.arr_PN;
	}

	public double[] getPreviousDayPN()
	{
		return this.arr_oldPN;
	}

	/**
	 * Returns a string representation of this agent and its key values
	 * Currently is used by Repast as the method which produces and returns the
	 * probe ID.
	 * 
	 * @return a string representation of this agent
	 **/
	@ProbeID()
	public String toString()
	{
		return getClass().getName() + " " + getID();
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
	public int getID()
	{
		return this.id;
	}

	/**
	 * Returns the agent's name. If the name has not been explicitly set, the
	 * default base name will be used.
	 * 
	 * @return agent's name as string
	 */
	public String getAgentName()
	{
		if (this.agentName == null && !nameExplicitlySet)
		{
			this.agentName = this.agentBaseName + " (" + this.getClass().getSimpleName() + " " + this.id + ")";
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
		nameExplicitlySet = true;
	}

	/**
	 * Returns the net demand <code>netDemand</code> for this agent (D)
	 * 
	 * @return the <code>netDemand (D)</code>
	 **/
	public double getNetDemand()
	{
		return this.netDemand;
	}

	public double[] getDayNetDemands()
	{
		return this.arr_day_D;
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
		this.netDemand = nd;
		if (this.arr_day_D != null)
		{
			this.arr_day_D[(int) RepastEssentials.GetTickCount() % ticksPerDay] = nd;
		}
	}

	/**
	 * Returns the price signal <code>priceSignal</code> for this agent (S)
	 * 
	 * @return the <code>priceSignal (S)</code>
	 **/
	public double getCurrentPriceSignal()
	{
		double time = RepastEssentials.GetTickCount();
		// if (Consts.DEBUG) System.out.println(
		// time+" getCurrentPriceSignal: "+priceSignal[(int) time %
		// priceSignal.length]);
		return priceSignal[(int) time % priceSignal.length];
	}

	/*
	 * public double getCurrentCostOfDemand() { return getCurrentCost_C() *
	 * this.getNetDemand(); }
	 */

	/**
	 * This sets the predicted market sell price calculated by the market
	 * aggregator for the current tick + 48 (i.e. 24 hours ahead of now).
	 * Currently called at each tick within step_pre function of RECO aggregator
	 * class.
	 */
	/*
	 * public void setCostsForTick48_C(int i, double costPrediction) {
	 * arr_i_C[i] = costPrediction; }
	 */

	/**
	 * This is added to get the cost of buying wholesale electricty for this
	 * aggregator at a given tick location for the day. It therefore expects i
	 * to be from 0 up to 48 STEFAN THOR SMITH
	 */

	public double getCostAtTick_C(int i)
	{
		return arr_i_C[i];
	}

	/**
	 * This method returns the cost of buying wholesale electricity for this
	 * aggregator at the current tick
	 */

	public double getCurrentCost_C()
	{
		return arr_i_C[(int) RepastEssentials.GetTickCount() % ticksPerDay];
	}

	public double getCurrentNormalizedCost_C()
	{
		if (arr_i_norm_C.length > 0)
			return arr_i_norm_C[(int) RepastEssentials.GetTickCount() % ticksPerDay];
		else
			return -1;
	}

	public double getCurrentBaseline_B()
	{
		return arr_i_B[(int) RepastEssentials.GetTickCount() % ticksPerDay];
	}

	public double getDayPredictedCost()
	{
		if (dailyPredictedCost.size() > 1)
			return dailyPredictedCost.get(mainContext.getDayCount() - 1);
		// return dailyPredictedCost.get(mainContext.getDayCount());

		else
			return 0;
	}

	public double getDayActualCost()
	{
		if (dailyActualCost.size() > 1)
			return dailyActualCost.get(mainContext.getDayCount() - 1);
		else
			return 0;
	}

	public double getCostDifference()
	{
		if (mainContext.getDayCount() <= Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
			return 0;
		else
			return getDayPredictedCost() - getDayActualCost();
	}

	public double getCumulativeCostSaving()
	{
		return this.cumulativeCostSaving;
	}

	public double getCurrentPriceElasticityFactor_e()
	{
		// if (Consts.DEBUG) System.out.println(
		// RepastEssentials.GetTickCount()+" getCurrentPriceElasticityFactor_e: "+arr_i_e[(int)
		// RepastEssentials.GetTickCount() % ticksPerDay]);

		return arr_i_e[(int) RepastEssentials.GetTickCount() % ticksPerDay];
	}

	/**
	 * Return the standard deviation of baseline load <code>arr_i_B</code> of
	 * this agent at the end of day
	 */
	public double getBaselineStdDev()
	{
		double val = new StandardDeviation().evaluate(arr_i_B, ArrayUtils.avg(arr_i_B));
		return val;
	}

	/**
	 * Return the standard deviation of load <code>hist_day_arr_D</code> of this
	 * agent at the end of day
	 */
	public double getNetDemandStdDev()
	{
		double val = new StandardDeviation().evaluate(arr_day_D, ArrayUtils.avg(arr_day_D));
		return val;
	}

	/**
	 * (30/01/12) DF Write out <code>day_arr_D</code> for demand flattening test
	 * This is only use for calculating standard deviation for the demand
	 * flattening condition
	 */
	/*
	 * public void printOutNetDemand4DemandFlatteningTest() {
	 * 
	 * CSVWriter res; String Filename = "NetDemand_FlatteningTest.csv";
	 * 
	 * if(RepastEssentials.GetTickCount() == ticksPerDay *
	 * (Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE +
	 * Consts.AGGREGATOR_TRAINING_PERIODE)) { res = new CSVWriter(Filename,
	 * false); res.appendText("NetDemand:"); res.appendRow(day_arr_D); } else {
	 * res = new CSVWriter(Filename, true); res.appendRow(day_arr_D); }
	 * 
	 * return; }
	 */

	/**
	 * Add an observer to the list of observer objects
	 * 
	 * @param anIObserver
	 *            the observer (IObsever) who wants to be added and updated when
	 *            a specific state changes or event occurs
	 */
	public void addObserver(IObserver anIObserver)
	{
		observableProxy.addObserver(anIObserver);
	}

	/**
	 * Deletes an observer from the list of observer objects
	 * 
	 * @param anIObserver
	 *            the observer (IObsever) who wants to be removed from the
	 *            observers' list
	 */
	public void deleteObserver(IObserver anIObserver)
	{
		observableProxy.deleteObserver(anIObserver);
	}

	/**
	 * Clears the list of observers
	 */
	public void deleteObservers()
	{
		observableProxy.deleteObservers();
	}

	/**
	 * Returns the number of observers in the list
	 * 
	 * @return number (count) of observers (in the list)
	 */
	public int countObservers()
	{
		return observableProxy.countObservers();
	}

	/**
	 * This method can be called when a specific state in an aggregator object
	 * (in which an observer object is interested) has changed. It notifies all
	 * of its observers by calling their <code>update</code> method. Here this
	 * task is delegated to an instance of <code>ObservableComponent</code>
	 * (observableProxy) which implements the necessary code for doing
	 * performing the task.
	 * 
	 * @param obs
	 *            the observed object.
	 * @param changeCodeArg
	 *            the changed code argument.
	 * @see ObservableComponent#notifyObservers(Object,Object)
	 * @see IObserver#update(Object,Object)
	 */
	protected void notifyObservers(Object obs, Object changeCodeArg)
	{
		observableProxy.notifyObservers(obs, changeCodeArg);
	}

	// ------------------------------------------------------------------------------
	/*
	 * TODO: the methods defined below will be checked later. In part, it must
	 * be determined which are the common behavior of all aggregator agents and
	 * whether different specific aggregator agents behave differently or the
	 * same when it comes to an specific method. These will determine which
	 * method signatures should stay here (and the implementation will be the
	 * concrete agrregator responsibility) and which one stay here as common
	 * behavior for all aggregator agent (like the ones defined above). / Babak
	 */

	void setPriceSignalFlatRate(double price)
	{
		double[] oldPrice = priceSignal;
		Arrays.fill(priceSignal, price);
		priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
	}

	void setPriceSignalEconomySeven(double highprice, double lowprice)
	{
		// Hack to change E7 tariff at 07:30
		int morningChangeTimeIndex = (int) (ticksPerDay / (24 / 7.5));
		// Hack to change E7 tariff at 23:30
		int eveningChangeTimeIndex = (int) (ticksPerDay / (24 / 23.5));
		double[] oldPrice = Arrays.copyOf(priceSignal, priceSignal.length);

		for (int offset = 0; offset < priceSignal.length; offset = offset + ticksPerDay)
		{
			Arrays.fill(priceSignal, offset, offset + morningChangeTimeIndex, lowprice);
			Arrays.fill(priceSignal, offset + morningChangeTimeIndex, offset + eveningChangeTimeIndex, highprice);
			Arrays.fill(priceSignal, offset + eveningChangeTimeIndex, offset + priceSignal.length, lowprice);
			priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
		}
	}

	void setPriceSignalRoscoeAndAult(double A, double B, double C)
	{
		double price;
		double x;
		if (Consts.DEBUG)
			System.out.println(" setPriceSignalRoscoeAndAult ");
		for (int i = 0; i < priceSignal.length; i++)
		{
			// Note that the division by 10 is to convert the units of predicted
			// customer demand
			// to those compatible with capacities expressed in GW.
			// TODO: unify units throughout the model
			x = (predictedCustomerDemand[i % ticksPerDay] / 10) / (Consts.MAX_SUPPLY_CAPACITY_GWATTS - Consts.MAX_GENERATOR_CAPACITY_GWATTS);
			price = (double) (A * Math.exp(B * x) + C);
			if ((Boolean) RepastEssentials.GetParameter("verboseOutput"))
			{
				if (Consts.DEBUG)
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
		// This is where we may alter the signal based on the demand
		// In this simple implementation, we simply scale the signal based on
		// deviation of
		// actual demand from projected demand for use next time round.

		// Define a variable to hold the aggregator's predicted demand at this
		// instant.
		double predictedInstantaneousDemand;
		// There are various things we may want the aggregator to do - e.g.
		// learn predicted instantaneous
		// demand, have a less dynamic but still non-zero predicted demand
		// or predict zero net demand (i.e. aggregators customer base is
		// predicted self-sufficient

		// predictedInstantaneousDemand = predictedCustomerDemand[(int) time %
		// predictedCustomerDemand.length];
		predictedInstantaneousDemand = 0;

		if (netDemand > predictedInstantaneousDemand)
		{
			priceSignal[(int) time % priceSignal.length] = (double) (priceSignal[(int) time % priceSignal.length] * (1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			// Now introduce some prediction - it was high today, so moderate
			// tomorrow...
			if (priceSignal.length > ((int) time % priceSignal.length + ticksPerDay))
			{
				priceSignal[(int) time % priceSignal.length + ticksPerDay] = (double) (priceSignal[(int) time % priceSignal.length + ticksPerDay] * (1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			}
			priceSignalChanged = true;
		}
	}

	void setPriceSignalScratchTest()
	{
		// This is where we may alter the signal based on the demand
		// In this simple implementation, we simply scale the signal based on
		// deviation of
		// actual demand from projected demand for use next time round.

		// Define a variable to hold the aggregator's predicted demand at this
		// instant.

		// There are various things we may want the aggregator to do - e.g.
		// learn predicted instantaneous
		// demand, have a less dynamic but still non-zero predicted demand
		// or predict zero net demand (i.e. aggregators customer base is
		// predicted self-sufficient

		// predictedInstantaneousDemand = predictedCustomerDemand[(int) time %
		// predictedCustomerDemand.length];

		priceSignal = ArrayUtils.multiply(overallSystemDemand, 1 / ArrayUtils.max(overallSystemDemand));
	}

	void setPriceSignalZero()
	{
		Arrays.fill(priceSignal, 0f);
		priceSignalChanged = true;
	}

	/*
	 * helper methods
	 */
	private void broadcastDemandSignal(List<ProsumerAgent> broadcastCusts,
			double time, int broadcastLength)
	{

		// To avoid computational load (and realistically model a reasonable
		// broadcast strategy)
		// only prepare and transmit the price signal if it has changed.
		if (priceSignalChanged)
		{
			// populate the broadcast signal with the price signal starting from
			// now and continuing for
			// broadcastLength samples - repeating copies of the price signal if
			// necessary to pad the
			// broadcast signal out.
			double[] broadcastSignal = new double[broadcastLength];
			int numCopies = (int) Math.floor((broadcastLength - 1) / priceSignal.length);
			int startIndex = (int) time % priceSignal.length;
			System.arraycopy(priceSignal, startIndex, broadcastSignal, 0, priceSignal.length - startIndex);
			for (int i = 1; i <= numCopies; i++)
			{
				int addIndex = (priceSignal.length - startIndex) * i;
				System.arraycopy(priceSignal, 0, broadcastSignal, addIndex, priceSignal.length);
			}

			if (broadcastLength > (((numCopies + 1) * priceSignal.length) - startIndex))
			{
				System.arraycopy(priceSignal, 0, broadcastSignal, ((numCopies + 1) * priceSignal.length) - startIndex, broadcastLength - (((numCopies + 1) * priceSignal.length) - startIndex));
			}

			for (ProsumerAgent a : broadcastCusts)
			{
				// Broadcast signal to all customers - note we simply say that
				// the signal is valid
				// from now currently, in future implementations we may want to
				// be able to insert
				// signals valid at an offset from now.

				a.receiveValueSignal(broadcastSignal, broadcastLength);
			}
		}

		priceSignalChanged = false;
	}

	/**
	 * This method should define the pre_step for the agents. It is assumed that
	 * aggregator takes the first step, using this method, usually to send a
	 * signal It then processes the behaviour of the prosumers on the sent
	 * signal for the same timeslot using this step method This has priority 1
	 * (first), followed by prosumers' step (priority second), and aggregators'
	 * step (priority thrid)
	 */
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = Consts.AGGREGATOR_PRE_STEP_PRIORITY_FIRST)
	abstract public void bizPreStep();

	/**
	 * This method should define the step for the agents. It is assumed that
	 * aggregator takes the first step, using step_pre() method, usually to send
	 * a signal It then processes the behaviour of the prosumers on the sent
	 * signal for the same timeslot using this step method This has priority 3,
	 * after step_pre and the prosumers' step (priority second)
	 */
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = Consts.AGGREGATOR_STEP_PRIORITY_SIXTH)
	abstract public void bizStep();

	@ScheduledMethod(start = 0, interval = 0, priority = Consts.PRE_INITIALISE_FIRST_TICK)
	private void initContext()
	{
		if (this.mainContext == null)
		{
			this.mainContext = (CascadeContext) ContextUtils.getContext(this);
		}
	}

	/**
	 * Constructs an aggregator agent with default category and type
	 */
	public AggregatorAgent(CascadeContext context)
	{
		this.id = agentIDCounter++;

		this.mainContext = context;
		observableProxy = new ObservableComponent();

		this.ticksPerDay = context.getNbOfTickPerDay();

		/* Should the category and type be defaulted here? */
		/*
		 * I think not - it's possible not all aggregators should have these
		 * (all traders yes, but all aggregators?)
		 */
	}

	public AggregatorAgent(CascadeContext context, BMU_CATEGORY cat, BMU_TYPE t, double maxDem, double minDem)
	{
		this(context, cat, t);
		this.maxDem = maxDem;
		this.minDem = minDem;
	}

	public AggregatorAgent(CascadeContext context, BMU_CATEGORY cat, BMU_TYPE t, double mGen)
	{
		this(context, cat, t);
		this.maxGen = mGen;
	}

	/**
	 * Constructs an aggregator agent
	 * 
	 * @param context
	 * @param cat
	 * @param t
	 */
	public AggregatorAgent(CascadeContext context, BMU_CATEGORY cat, BMU_TYPE t)
	{
		this(context);

		category = cat;
		type = t;

		arr_PN = new double[ticksPerDay];
		arr_oldPN = new double[ticksPerDay];

	}

	public AggregatorAgent()
	{
		this.id = agentIDCounter++;
	}
}
