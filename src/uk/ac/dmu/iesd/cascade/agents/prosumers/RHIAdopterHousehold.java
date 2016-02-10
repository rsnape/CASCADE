package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.gis.GeographyWithin;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.HEATING_TYPE;
import uk.ac.dmu.iesd.cascade.behaviour.psychological.cognitive.SCTModel;
import uk.ac.dmu.iesd.cascade.context.AdoptionContext;
import uk.ac.dmu.iesd.cascade.context.RHIContext;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

public class RHIAdopterHousehold extends HouseholdProsumer
{
	private int RHICapital = 5000000; // Budget in tenths of pence
	private int smartContCapital = 750000;

	private HEATING_TYPE RHIEligibleHeatingTechnology;

	/**
	 * @param rHIEligibleHeatingTechnology
	 *            the rHIEligibleHeatingTechnology to set
	 */
	public void setRHIEligibleHeatingTechnology(HEATING_TYPE rHIEligibleHeatingTechnology)
	{
		this.RHIEligibleHeatingTechnology = rHIEligibleHeatingTechnology;
	}

	private boolean renewableHeatAdopted;

	public HEATING_TYPE RHIEligibleHeatingOwned;

	/**
	 * @return the rHIEligibleHeatingOwned
	 */
	public HEATING_TYPE getRHIEligibleHeatingOwned()
	{
		return this.RHIEligibleHeatingOwned;
	}

	/**
	 * @param rHIEligibleHeatingOwned
	 *            the rHIEligibleHeatingOwned to set
	 */
	public void setRHIEligibleHeatingOwned(HEATING_TYPE rHIEligibleHeatingOwned)
	{
		this.RHIEligibleHeatingOwned = rHIEligibleHeatingOwned;
	}

	private Geography<RHIAdopterHousehold> myGeography;
	private ArrayList<RHIAdopterHousehold> myNeighboursCache;
	private int numCachedNeighbours;

	private SCTModel RHIDecisionModel = new SCTModel();

	private RHIContext mainContext = (RHIContext) super.mainContext;

	private Date nextCogniscentDate;

	private int numThoughts;
	// public boolean hasSmartControl;

	// public boolean hasElectricVehicle;
	private double potentialRHICapacity = RandomHelper.nextDoubleFromTo(2, 4);
	public double observedRadius;

	private double perceivedSmartControlBenefit;

	/**
	 * A prosumer agent's base name it can be reassigned (renamed) properly by
	 * descendants of this class
	 **/
	protected static String agentBaseName = "Household";

	// private CascadeContext mainContext;
	// public int defraCategory;
	public double microgenPropensity;
	public double economicAbility;
	public double RHIlikelihood;
	public double insulationPropensity;
	public double HEMSPropensity;
	// public double EVPropensity;
	// public double habit;
	private double adoptionThreshold;

	private double economicSensitivity;
	private double decisionUrgency = 0.001;
	private double perceivedRHIBenefit = 0;
	private double ratedPowerRHI;

	public boolean getHasRHI()
	{
		return this.renewableHeatAdopted;
	}

	public int getHasRHINum()
	{
		return this.getHasRHI() ? 1 : 0;
	}

	public int getHasSmartControlNum()
	{
		return this.hasSmartControl ? 1 : 0;
	}

	@ScheduledMethod(start = 3, interval = 48, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void checkTime()
	{
		if (this.mainContext == null)
		{
			Context testContext = ContextUtils.getContext(this);
			if (testContext instanceof AdoptionContext)
			{
				this.mainContext = (RHIContext) testContext;
			}
		}

		if (this.mainContext.getDateTime().getTime() > this.nextCogniscentDate.getTime()
				&& this.mainContext.getDateTime().getTime() <= (this.nextCogniscentDate.getTime() + Consts.MSECS_PER_DAY))
		{
if (			this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug(this.getAgentName() + " Thinking with RHI ownership = " + this.getHasRHI() + "..."
					+ this.RHIlikelihood);
}
			this.considerOptions(); // Could make the consideration further
									// probabilistic...
if (			this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug("Resulting in RHI ownership = " + this.getHasRHI() + ", likelihood:" + this.RHIlikelihood
					+ ", neightbours:" + this.numCachedNeighbours);
}

			// this.myGeography.move(this, this.myGeography.getGeometry(this));
			this.numThoughts++;
			this.decisionUrgency = Math.exp(-((this.mainContext.dateToTick(this.mainContext.getRHIAvailableUntil()) - this.mainContext
					.getTickCount()) / (this.mainContext.ticksPerDay * 28)));
if (			this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug("New decision urgency = " + this.decisionUrgency + " from tariff available until tick: "
					+ this.mainContext.dateToTick(this.mainContext.getRHIAvailableUntil()));
}
			this.nextCogniscentDate.setTime(this.mainContext.getDateTime().getTime()
					+ ((long) (this.mainContext.nextThoughtGenerator.nextDouble() * Consts.MSECS_PER_DAY)));
		}
	}

	private void considerOptions()
	{
		this.RHIlikelihood = this.microgenPropensity;
		if (this.mainContext.logger.isTraceEnabled())
		{
if (			this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger.trace(this.agentName + " gathering information from baseline likelihood of " + this.RHIlikelihood
					+ "...");
}
		}
		this.checkTariffs();
		this.calculateSCT();

		if (RandomHelper.nextDouble() > this.habit)
		{
			// habit change - introducing a longer term feedback,
			// should be equivalent to norm shifting over the population.
			this.microgenPropensity = this.RHIlikelihood;
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Updating propensity, i.e. changing habit, based on likelihood");
			}
		}

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(this.agentName + " making decision based on likelihood of " + this.RHIlikelihood + "...");
		}
		this.makeDecision();
	}

	/**
	 * 
	 */
	private void calculateSCT()
	{

		/*
		 * Set perception of others' behaviour based on observed percentage
		 */
		this.RHIDecisionModel.setPerceptionOfOthers(this.observeNeighbours());
		this.RHIDecisionModel.setSelfEfficacy(this.economicAbility); // Could do
																		// this
																		// one
																		// off
																		// at
																		// initialisation
		this.RHIDecisionModel.setOutcomeExpectation(this.decisionUrgency * this.economicSensitivity * this.perceivedRHIBenefit);

		/*
		 * Set economic factors in socio structural construct. Note that a value
		 * of 0 is an absolute veto
		 */
		double quote = this.mainContext.getRHISystemPrice(this.potentialRHICapacity, this.RHIEligibleHeatingTechnology);
		if (this.RHICapital < quote || this.potentialRHICapacity == 0)
		{
			this.RHIDecisionModel.setSocioStructural(0);
		}
		else
		{
			double socioStructuralFactors = 0; // 0 = absolutely not, 1 =
												// maximum likelihood
			socioStructuralFactors = (this.RHICapital - quote) / this.RHICapital; // Cheaper
																					// it
																					// is,
																					// the
																					// more
																					// attractive
																					// -
																					// free
																					// =
																					// socio
																					// structural
																					// of
																					// 1
			this.RHIDecisionModel.setSocioStructural(socioStructuralFactors);
		}

		this.RHIlikelihood = this.RHIDecisionModel.getGoal();
	}

	private void makeDecision()
	{
if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(this.getAgentName() + " has microgen propensity " + this.microgenPropensity
				+ " and RHI adoption likelihood " + this.RHIlikelihood);
}

		this.RHIDecisionModel.setAbsoluteBehaviourThreshold(this.getAdoptionThreshold());
		this.RHIDecisionModel.calculateBehaviour();

		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Calculated behaviour from SCT" + this.RHIDecisionModel.getBehaviour());
		}

		// if (RHIlikelihood > getAdoptionThreshold())
		if (this.RHIDecisionModel.getBinaryBehaviourDecisionHardThreshold())
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug(this.agentName + " Adopted RHI");
			}
			this.setRHI();

		}

		if (false)// (ArrayUtils.sum(this.baseProfile)*RandomHelper.nextDouble()*365*100
					// > smartContCapital)
		{
			this.setWattboxController();
		}

	}

	/**
	 * 
	 */
	public void setRHI()
	{
		this.renewableHeatAdopted = true;
		this.ratedPowerRHI = this.potentialRHICapacity;
		this.RHIEligibleHeatingOwned = this.RHIEligibleHeatingTechnology;
	}

	private void checkTariffs()
	{
		double RHITariffPence = this.currentTariff() / 10; // Note that tariffs
															// are stored as
															// integer tenths of
															// pence to aid
															// precision

		this.RHIlikelihood += this.economicSensitivity * RHITariffPence * this.decisionUrgency;
	}

	private double currentTariff()
	{
		return (this.mainContext.getRHITariff(this.potentialRHICapacity, this.RHIEligibleHeatingTechnology));
	}

	private double observeNeighbours()
	{
		// TODO: Need to allow for positive and negative influence here,
		// Not purely positive.

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("Observing neighbours");
		}
		ArrayList<RHIAdopterHousehold> neighbours = this.getNeighbours();
		int observedAdoption = 0;
		int observed = 0;
		for (RHIAdopterHousehold h : neighbours)
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Into observation loop");
			}
			boolean observe = (RandomHelper.nextDouble() > 0.5);
			observe = true; // for testing

			if (observe)
			{
				if (h.getHasRHI())
				{

					boolean negativeImpression = (RandomHelper.nextDouble() > 0.5);
					if (negativeImpression)
					{

					}
					else
					{
						observedAdoption++;
					}
				}
				observed++;
			}
		}

if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace("Returninglikelihood to agent " + this.getAgentName() + " based on " + observedAdoption + " of "
				+ this.numCachedNeighbours + " neighbours observed to have RHI (" + observed + " observed this round)");
}

		// Likelihood of adopting now - based on observation alone
		// Note that the 0.5 is an arbitrary and tunable parameter.
		return (observed == 0) ? 0 : ((double) observedAdoption) / observed;

	}

	/*
	 * Note - OK to cache neighbours as static in this simulation. If households
	 * may move to different physical houses, this would have to change.
	 */
	private ArrayList<RHIAdopterHousehold> getNeighbours()
	{

		if (this.myNeighboursCache == null)
		{
			GeographyWithin<RHIAdopterHousehold> neighbourhood = new GeographyWithin<RHIAdopterHousehold>(this.myGeography,
					this.observedRadius, this);
			this.myNeighboursCache = IterableUtils.Iterable2ArrayList(neighbourhood.query());
			this.numCachedNeighbours = this.myNeighboursCache.size();
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(this.getAgentName() + " found neighbours : " + this.myNeighboursCache.toString());
			}
		}

if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(this.getAgentName() + " has " + this.numCachedNeighbours + " neighbours : "
				+ this.myNeighboursCache.toString());
}

		return this.myNeighboursCache;
	}

	@Override
	public String getAgentName()
	{
		return this.agentName;
	}

	/**
	 * Constructs a prosumer agent with the context in which is created
	 * 
	 * @param context
	 *            the context in which this agent is situated
	 */
	public RHIAdopterHousehold(RHIContext context)
	{
		this(context, new double[48]);
		// Date startTime = (Date) context.simStartDate.clone();
		// startTime.setTime(startTime.getTime() + ((long)
		// (context.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
		// this.nextCogniscentDate = startTime;

	}

	/**
	 * Test for Richard need no arg constructor for shapefile load
	 */
	public RHIAdopterHousehold()
	{
		super();
	}

	/**
	 * Constructs a prosumer agent with the context in which is created.
	 * 
	 * Needs some work - note the hard coded time on which to base all next
	 * decision times.
	 * 
	 */
	public RHIAdopterHousehold(RHIContext context, double[] otherDemandProfile)
	{
		super(context, otherDemandProfile);
		this.agentName = "Household_" + this.agentID;
		this.mainContext = context;
		this.setStartDateAndFirstThought();
		this.initialiseSCT();
		if (context.logger.isDebugEnabled())
		{
			context.logger.debug(this.agentName + " initialised, first thought at " + this.nextCogniscentDate.toGMTString());
		}
	}

	public void setStartDateAndFirstThought()
	{
		Date startTime = null;
		try
		{
			startTime = (new SimpleDateFormat("dd/MM/yyyy")).parse("01/08/2014"); // TODO:
																					// Nasty
																					// -
																					// nasty
																					// -
																					// hard
																					// coded
																					// start
																					// date
																					// :(
		}
		catch (ParseException e)
		{
			System.err.println("Weird - I can't parse this fixed string that I've always parsed before");
		}

		// Date startTime = (Date) this.mainContext.simStartDate.clone();
		// startTime.setTime(startTime.getTime() + ((long)
		// (this.mainContext.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 *
		// 1000)));
		// this.nextCogniscentDate = startTime;

		startTime.setTime(startTime.getTime() + ((long) (this.mainContext.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
		this.nextCogniscentDate = startTime;
		this.economicSensitivity = RandomHelper.nextDouble();
		this.perceivedSmartControlBenefit = RandomHelper.nextDouble(); // comment
																		// this
																		// out
																		// for
																		// non-smart
																		// adoption
																		// scenarios

	}

	public int getNumThoughts()
	{
		return this.numThoughts;
	}

	@Override
	public int getDefraCategory()
	{
		return this.defraCategory;
	}

	public void setContext(RHIContext c)
	{
		this.mainContext = c;
		super.mainContext = c;
	}

	public void setGeography(Geography g)
	{
		this.myGeography = g;
	}

	/**
	 * @param adoptionThreshold
	 *            the adoptionThreshold to set
	 */
	public void setAdoptionThreshold(double adoptionThreshold)
	{
		this.adoptionThreshold = adoptionThreshold;
	}

	/**
	 * @return the adoptionThreshold
	 */
	public double getAdoptionThreshold()
	{
		return this.adoptionThreshold;
	}

	double[] baseProfile = new double[48];
	double dailySaving = 0;
	ArrayList<Double> dailySavings = new ArrayList<Double>();
	private double dailyBaseTotal;

	/*
	 * Note - scheduled methods must be public in order that the scheduler can
	 * call them!!
	 */
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void determineBaseline()
	{
		int now = this.mainContext.getTickcount();
		if (now < 48)
		{
			this.baseProfile[now] = this.getNetDemand();
		}

	}

	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void calculateSaving()
	{
		double d = this.getNetDemand();
		int hh = this.mainContext.getTimeslotOfDay();

		if (hh == 0)
		{
			this.dailySavings.add(this.dailySaving);
			this.dailySaving = 0;
		}

		// dailySaving += currentTariff() * this.getRHIGen();
		// all avoided consumption presently valued at 10p per unit. Future
		// implementations
		// should translate the current price prediction to a cost
		double avoidedkWhprice = 100;
		avoidedkWhprice = 100 + 50 * this.getPredictedCostSignal()[hh];
		this.dailySaving += avoidedkWhprice * (this.baseProfile[hh] - Math.max(0, d));
		if (d < 0)
		{
			// Exporting power, add an extra benefit at 3.1p per unit
			this.dailySaving += AdoptionContext.FIT_EXPORT_TARIFF * Math.abs(d);
		}

	}

	public double getDailySaving()
	{
		if (this.dailySavings.size() > 0)
		{
			return this.dailySavings.get(this.dailySavings.size() - 1);
		}
		return 0;
	}

	@ScheduledMethod(start = 49, interval = 48, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateDailyRHIBenefit()
	{

		double estimatedRHIGen = this.potentialRHICapacity;
		this.perceivedRHIBenefit = estimatedRHIGen * this.currentTariff();
	}

	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateSmartControlBenefit()
	{

	}

	private void initialiseSCT()
	{
		this.RHIDecisionModel.setWeightGoalToBehaviour(5);
		this.RHIDecisionModel.setWeightSocioStructuralToOutcomeExp(4);
		this.RHIDecisionModel.setWeightSocioStructuralToPerceptionOfOthers(2);
		this.RHIDecisionModel.setWeightSelfEfficacyToOutcomeExp(5);
		this.RHIDecisionModel.setWeightOutcomeExpToGoal(5);
		this.RHIDecisionModel.setWeightPerceptionOfOthersToGoal(3);
		this.RHIDecisionModel.setWeightSelfEfficacyToGoal(4);
		this.RHIDecisionModel.setWeightSocioStructuralToGoal(5);
		this.RHIDecisionModel.setWeightOutcomeExpToBehaviour(5);
		this.RHIDecisionModel.setWeightPerceptionOfOthersToBehaviour(1);
		this.RHIDecisionModel.setWeightSelfEfficacyToBehaviour(5);
		this.RHIDecisionModel.setWeightSocioStructuralToBehaviour(5);
		this.RHIDecisionModel.setWeightOutcomeToSelfEfficacy(4);
		this.RHIDecisionModel.setWeightOutcomeToSocioStructural(1);
		this.RHIDecisionModel.setWeightOutcomeToOutcomeExp(5);
	}
}
