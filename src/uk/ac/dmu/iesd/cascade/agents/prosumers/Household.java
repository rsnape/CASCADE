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
import uk.ac.dmu.iesd.cascade.behaviour.psychological.cognitive.SCTModel;
import uk.ac.dmu.iesd.cascade.context.AdoptionContext;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

public class Household extends HouseholdProsumer
{
	private int PVCapital = 5000000; // Budget in tenths of pence
	private int smartContCapital = 750000;

	private Geography<Household> myGeography;
	private ArrayList<Household> myNeighboursCache;
	private int numCachedNeighbours;

	private SCTModel PVDecisionModel = new SCTModel();

	private AdoptionContext mainContext = (AdoptionContext) super.mainContext;

	private Date nextCogniscentDate;

	private int numThoughts;
	// public boolean hasSmartControl;

	// public boolean hasElectricVehicle;
	public double potentialPVCapacity;
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
	public double PVlikelihood;
	public double insulationPropensity;
	public double HEMSPropensity;
	// public double EVPropensity;
	// public double habit;
	private double adoptionThreshold;

	private double economicSensitivity;
	private double decisionUrgency = 0.001;
	private double perceivedPVBenefit = 0;

	public boolean getHasPV()
	{
		return this.hasPV;
	}

	public int getHasPVNum()
	{
		return this.hasPV ? 1 : 0;
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
				this.mainContext = (AdoptionContext) testContext;
			}
		}

		if (this.mainContext.getDateTime().getTime() > this.nextCogniscentDate.getTime()
				&& this.mainContext.getDateTime().getTime() <= (this.nextCogniscentDate.getTime() + Consts.MSECS_PER_DAY))
		{
if (			this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug(this.getAgentName() + " Thinking with PV ownership = " + this.getHasPV() + "..."
					+ this.PVlikelihood);
}
			this.considerOptions(); // Could make the consideration further
									// probabilistic...
if (			this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug("Resulting in PV ownership = " + this.getHasPV() + ", likelihood:" + this.PVlikelihood
					+ ", neightbours:" + this.numCachedNeighbours);
}

			// this.myGeography.move(this, this.myGeography.getGeometry(this));
			this.numThoughts++;
			this.decisionUrgency = Math.exp(-((this.mainContext.dateToTick(this.mainContext.getTarriffAvailableUntil()) - this.mainContext
					.getTickCount()) * 1.0 / (this.mainContext.ticksPerDay * 28)));
if (			this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug("New decision urgency = " + this.decisionUrgency + " from tariff available until tick: "
					+ this.mainContext.dateToTick(this.mainContext.getTarriffAvailableUntil()));
}
			this.nextCogniscentDate.setTime(this.mainContext.getDateTime().getTime()
					+ ((long) (this.mainContext.nextThoughtGenerator.nextDouble() * Consts.MSECS_PER_DAY)));
		}
	}

	private void considerOptions()
	{
		this.PVlikelihood = this.microgenPropensity;
		if (this.mainContext.logger.isTraceEnabled())
		{
if (			this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger.trace(this.agentName + " gathering information from baseline likelihood of " + this.PVlikelihood
					+ "...");
}
		}
		this.checkTariffs();
		this.calculateSCT();

		if (RandomHelper.nextDouble() > this.habit)
		{
			// habit change - introducing a longer term feedback,
			// should be equivalent to norm shifting over the population.
			this.microgenPropensity = this.PVlikelihood;
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Updating propensity, i.e. changing habit, based on likelihood");
			}
		}

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(this.agentName + " making decision based on likelihood of " + this.PVlikelihood + "...");
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
		this.PVDecisionModel.setPerceptionOfOthers(100*this.observeNeighbours());
		this.PVDecisionModel.setSelfEfficacy(this.economicAbility); // Could do
																	// this one
																	// off at
																	// initialisation
		this.PVDecisionModel.setOutcomeExpectation(this.decisionUrgency * this.economicSensitivity * this.perceivedPVBenefit);

		/*
		 * Set economic factors in socio structural construct. Note that a value
		 * of 0 is an absolute veto
		 */
		double quote = this.mainContext.getPVSystemPrice(this.potentialPVCapacity);
		if (this.PVCapital < quote || this.potentialPVCapacity == 0)
		{
			this.PVDecisionModel.setSocioStructural(0);
		}
		else
		{
			double socioStructuralFactors = 0; // 0 = absolutely not, 1 =
												// maximum likelihood
			socioStructuralFactors = (this.PVCapital - quote) / this.PVCapital; // Cheaper
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
			this.PVDecisionModel.setSocioStructural(socioStructuralFactors);
		}

		this.PVlikelihood = this.PVDecisionModel.getGoal();
	}

	private void makeDecision()
	{
if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(this.getAgentName() + " has microgen propensity " + this.microgenPropensity
				+ " and PV adoption likelihood " + this.PVlikelihood);
}

		this.PVDecisionModel.setAbsoluteBehaviourThreshold(this.getAdoptionThreshold());
		this.PVDecisionModel.calculateBehaviour();

		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Calculated behaviour from SCT" + this.PVDecisionModel.getBehaviour());
		}

		// if (PVlikelihood > getAdoptionThreshold())
		if (this.PVDecisionModel.getBinaryBehaviourDecisionHardThreshold())
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug(this.agentName + " Adopted PV");
			}
			this.setPV();

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
	public void setPV()
	{
		if (this.potentialPVCapacity > 0)
		{
			this.hasPV = true;
			this.ratedPowerPV = this.potentialPVCapacity;
		}

	}

	public double getPVGen()
	{
		return this.PVGeneration();
	}

	private void checkTariffs()
	{
		double PVTariffPence = this.currentTariff();

		this.PVlikelihood += this.economicSensitivity * PVTariffPence * (this.decisionUrgency * 1000);
	}

	private double currentTariff()
	{
		return ((double) this.mainContext.getPVTariff(this.potentialPVCapacity)) / 1000; // note
																							// tariffs
																							// stored
																							// as
																							// integer
																							// tenths
																							// of
																							// pence
		// Add the weight of the tariff influence to the adoption likelihood
	}

	private double observeNeighbours()
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("Observing neighbours");
		}
		ArrayList<Household> neighbours = this.getNeighbours();
		int observedAdoption = 0;
		int observed = 0;
		for (Household h : neighbours)
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Into observation loop");
			}
			boolean observe = (RandomHelper.nextDouble() > 0.5);
			observe = true; // for testing

			if (observe)
			{
				if (h.getHasPV())
				{
					observedAdoption++;
				}
				observed++;
			}
		}

if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace("Returninglikelihood to agent " + this.getAgentName() + " based on " + observedAdoption + " of "
				+ this.numCachedNeighbours + " neighbours observed to have PV (" + observed + " observed this round)");
}

		// Likelihood of adopting now - based on observation alone
		// Note that the 0.5 is an arbitrary and tunable parameter.
		return observed == 0 ? 0 : ((double) observedAdoption) / observed;

	}

	/*
	 * Note - OK to cache neighbours as static in this simulation. If households
	 * may move to different physical houses, this would have to change.
	 */
	private ArrayList<Household> getNeighbours()
	{

		if (this.myNeighboursCache == null)
		{
			GeographyWithin<Household> neighbourhood = new GeographyWithin<Household>(this.myGeography, this.observedRadius, this);
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
	public Household(AdoptionContext context)
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
	public Household()
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
	public Household(AdoptionContext context, double[] otherDemandProfile)
	{
		super(context, otherDemandProfile);
		this.agentName = "Household_" + this.agentID;
		this.mainContext = context;
		this.setStartDateAndFirstThought();
		this.initialiseSCT();
		if (RandomHelper.nextDouble() < 0.6) //60% of houses can have some form of PV (see EST references)
		{
			potentialPVCapacity = RandomHelper.nextDoubleFromTo(2, 4);
		}
		else
		{
			potentialPVCapacity = 0;
		}
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
			startTime = (new SimpleDateFormat("dd/MM/yyyy")).parse("01/04/2010");
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

	public void setContext(AdoptionContext c)
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

		this.dailySaving += this.currentTariff() * this.getPVGen();
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
	public void estimateDailyPVBenefit()
	{
		// EST generation factor = 833 kWh per kWp per year for South facing -
		// 640 for East facing, 661 West facing
		// Heavy shading attenuates by half. For now - use 750 as reasonable
		// estimate
		double estimatedPVGen = this.potentialPVCapacity * (Consts.PV_KWH_PER_KWP / 365);
		this.perceivedPVBenefit = estimatedPVGen * this.currentTariff();
	}

	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateSmartControlBenefit()
	{

	}

	public void initialiseSCT()
	{
		this.PVDecisionModel.setWeightGoalToBehaviour(3);
		this.PVDecisionModel.setWeightSocioStructuralToOutcomeExp(0);
		this.PVDecisionModel.setWeightSocioStructuralToPerceptionOfOthers(0);
		this.PVDecisionModel.setWeightSelfEfficacyToOutcomeExp(0);
		this.PVDecisionModel.setWeightOutcomeExpToGoal(3);
		this.PVDecisionModel.setWeightPerceptionOfOthersToGoal(0.2);
		this.PVDecisionModel.setWeightSelfEfficacyToGoal(1);
		this.PVDecisionModel.setWeightSocioStructuralToGoal(0);
		this.PVDecisionModel.setWeightOutcomeExpToBehaviour(0);
		this.PVDecisionModel.setWeightPerceptionOfOthersToBehaviour(0);
		this.PVDecisionModel.setWeightSelfEfficacyToBehaviour(0);
		this.PVDecisionModel.setWeightSocioStructuralToBehaviour(0);
		this.PVDecisionModel.setWeightOutcomeToSelfEfficacy(0);
		this.PVDecisionModel.setWeightOutcomeToSocioStructural(0);
		this.PVDecisionModel.setWeightOutcomeToOutcomeExp(0);
	}
}
