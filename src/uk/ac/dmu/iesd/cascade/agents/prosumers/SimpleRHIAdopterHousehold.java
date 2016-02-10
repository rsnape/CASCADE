package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.IllegalParameterException;
import repast.simphony.query.space.gis.GeographyWithin;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.HEATING_TYPE;
import uk.ac.dmu.iesd.cascade.context.AdoptionContext;
import uk.ac.dmu.iesd.cascade.context.RHIContext;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

public class SimpleRHIAdopterHousehold extends HouseholdProsumer
{
	private int RHICapital = 5000000; // Budget in tenths of pence
	private int smartContCapital = 750000;

	// Weightings of the various influences
	double wHassle;
	double wPay;
	double wSocial;
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

	private boolean heatingBroken = false;
	private int daysBroken = 0;
	private HEATING_TYPE currentHeating = HEATING_TYPE.GRID_GAS;
	public HEATING_TYPE RHIEligibleHeatingOwned;

	/**
	 * @return the rHIEligibleHeatingOwned
	 */
	public HEATING_TYPE getRHIEligibleHeatingOwned()
	{
		return this.RHIEligibleHeatingOwned;
	}

	public int getHasGSHP()
	{
		return (this.RHIEligibleHeatingOwned == HEATING_TYPE.GND_SOURCE_HP) ? 1 : 0;
	}

	public int getHasASHP()
	{
		return (this.RHIEligibleHeatingOwned == HEATING_TYPE.AIR_SOURCE_HP) ? 1 : 0;
	}

	public int getHasBiomass()
	{
		return (this.RHIEligibleHeatingOwned == HEATING_TYPE.BIOMASS) ? 1 : 0;
	}

	public int getHasSolarThermal()
	{
		return (this.RHIEligibleHeatingOwned == HEATING_TYPE.SOLAR_THERMAL) ? 1 : 0;
	}

	/**
	 * @param rHIEligibleHeatingOwned
	 *            the rHIEligibleHeatingOwned to set
	 */
	public void setRHIEligibleHeatingOwned(HEATING_TYPE rHIEligibleHeatingOwned)
	{
		this.RHIEligibleHeatingOwned = rHIEligibleHeatingOwned;
	}

	private Geography<SimpleRHIAdopterHousehold> myGeography;
	private ArrayList<SimpleRHIAdopterHousehold> myNeighboursCache;
	private int numCachedNeighbours;

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
	protected static String agentBaseName = "SimpleRHIAdopter";

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

	@ScheduledMethod(start = 49, interval = 48, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
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

		Date rightNow = this.mainContext.getDateTime();

		if (RandomHelper.nextDouble() < this.mainContext.dailyChanceOfFailure)
		{
			this.heatingBroken = true;
			this.nextCogniscentDate.setTime(this.mainContext.getDateTime().getTime() - 1);
		}

		if (rightNow.getTime() > this.nextCogniscentDate.getTime()
				&& rightNow.getTime() <= (this.nextCogniscentDate.getTime() + Consts.MSECS_PER_DAY))
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug(this.getAgentName() + " Thinking with RHI ownership = " + this.getHasRHI() + "..."
						+ this.RHIlikelihood);
			}
			this.considerOptions(); // Could make the consideration further
			// probabilistic...
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("Resulting in RHI ownership = " + this.getHasRHI() + ", likelihood:" + this.RHIlikelihood
						+ ", neightbours:" + this.numCachedNeighbours);
			}

			// this.myGeography.move(this, this.myGeography.getGeometry(this));
			this.numThoughts++;
			this.decisionUrgency = Math.exp(-((this.mainContext.dateToTick(this.mainContext.getRHIAvailableUntil()) - this.mainContext
					.getTickCount()) / (this.mainContext.ticksPerDay * 28)));
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("New decision urgency = " + this.decisionUrgency + " from tariff available until tick: "
						+ this.mainContext.dateToTick(this.mainContext.getRHIAvailableUntil()));
			}

			// If haven't adopted RHI set a new thinking date. Otherwise no
			// point thinking again for the purposes of this simulation.
			if (!this.getHasRHI())
			{
				if (this.heatingBroken)
				{
					this.nextCogniscentDate.setTime(rightNow.getTime() + Consts.MSECS_PER_DAY - 1);
					this.daysBroken++;
				}
				else
				{
					this.nextCogniscentDate.setTime(rightNow.getTime()
							+ ((long) (this.mainContext.nextThoughtGenerator.nextDouble() * Consts.MSECS_PER_DAY)));
				}
			}
		}

		// Only allow heating failure to last max 7 days between September and
		// April
		// This is equivalent to saying that they fix or re-install their
		// incumbent
		// technology
		if ((rightNow.getMonth() > 8 || rightNow.getMonth() < 5) && this.daysBroken > 7)
		{
			this.heatingBroken = false;
			this.daysBroken = 0;
		}
	}

	private void considerOptions()
	{
		this.RHIlikelihood = this.microgenPropensity;
		if (this.mainContext.logger.isTraceEnabled())
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(this.agentName + " gathering information from baseline likelihood of " + this.RHIlikelihood
						+ "...");
			}
		}

		double benefitInPence = this.checkEconomics();

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

		this.makeDecision(benefitInPence);
	}

	private void makeDecision(double benefit)
	{

		// It is assumed that net disbenefit (-ve benefit) will never be
		// considered. This
		// may in fact be too harsh - our benefit does not account for worries
		// about gas prices increasing etc. On the other hand - it is likely
		// that electricity prices will increase also.

		if (benefit > 0)
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(this.getAgentName() + " has microgen propensity " + this.microgenPropensity
						+ " and RHI adoption likelihood " + this.RHIlikelihood);
			}

			// Neighbour influence varies from +1 to -1
			double neighbourInfluence = this.observeNeighbours();
			double capitalCost = this.mainContext.getQuote(this.RHIEligibleHeatingTechnology);

			double paybackYears = capitalCost / (benefit / 100 * 365);

			this.mainContext.logger.info(this.agentName + " thinking with propensity " + this.microgenPropensity + ", neighbour inf "
					+ neighbourInfluence + ", benefit " + benefit + "p / day, install quote £" + capitalCost + " and payback "
					+ paybackYears + " years.");

			if (paybackYears < 7)
			{
				// will consider the option at all if payback < the 7 years of
				// the scheme -
				// without this, the economics will not be considered.
				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("possible install - considering neighbours or exceptionally short payback");
				}

				// PUt in all the influencing factors (+ve is good)

				double paybackInfluence = 1 - paybackYears / 7;
				double hassleInfluence = this.mainContext.getHassleFactor(this.RHIEligibleHeatingTechnology);

				double totalInfluence = wPay * paybackInfluence - wHassle * hassleInfluence + wSocial * neighbourInfluence;

				this.mainContext.logger.info("Testing " + totalInfluence + " against adoption threshold " + this.adoptionThreshold);
				if (totalInfluence > this.adoptionThreshold)
				{
					this.setRHI();
				}

				// Original simple condition produced realistic curves
				/*
				 * if (neighbourInfluence > 0 || paybackYears < 3) {
				 * this.setRHI(); }
				 */
			}
		}

	}

	/**
	 * 
	 */
	public void setRHI()
	{
		if (this.RHIEligibleHeatingTechnology != null)
		{
			this.renewableHeatAdopted = true;
			this.ratedPowerRHI = this.potentialRHICapacity;
			this.RHIEligibleHeatingOwned = this.RHIEligibleHeatingTechnology;
			if (this.RHIEligibleHeatingOwned == HEATING_TYPE.AIR_SOURCE_HP || this.RHIEligibleHeatingOwned == HEATING_TYPE.GND_SOURCE_HP
					|| this.RHIEligibleHeatingOwned == HEATING_TYPE.ELECTRIC_STORAGE)
			{
				// in theory you could have other forms of water heat, but seems
				// unlikely. With storage space heat, likely to have immersion
				// water
				// heat.
				this.initializeElectWaterHeatPar();
				this.initializeElecSpaceHeatPar();
			}
			this.mainContext.logger.info("Adopted RHI tech " + this.RHIEligibleHeatingOwned);
		}
	}

	private double checkEconomics()
	{
		this.perceivedRHIBenefit = this.calculateDailyRHIBenefit(this.currentHeating, this.RHIEligibleHeatingTechnology);
		// currently, this returns the rational hard benefit as calculated by
		// calculateDailyRHIBenefit
		// At some point, could add in a perception factor more akin to the full
		// blown psychological model.
		return this.perceivedRHIBenefit;
	}

	private double currentTariff()
	{
		return (this.mainContext.getRHITariff(this.potentialRHICapacity, this.RHIEligibleHeatingTechnology));
	}

	private double observeNeighbours()
	{
		// TODO: Need to refine positive and negative influence here,
		// Not purely positive. Currently pretty naive - 50:50 add or subract

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("Observing neighbours");
		}

		ArrayList<SimpleRHIAdopterHousehold> neighbours = this.getNeighbours();
		int observedAdoption = 0;
		int observed = 0;
		for (SimpleRHIAdopterHousehold h : neighbours)
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
						observedAdoption--;

					}
					else
					{
						observedAdoption++;
					}
				}
				observed++;
			}
		}

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("Returning likelihood to agent " + this.getAgentName() + " based on " + observedAdoption + " of "
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
	private ArrayList<SimpleRHIAdopterHousehold> getNeighbours()
	{

		if (this.myNeighboursCache == null)
		{
			GeographyWithin<SimpleRHIAdopterHousehold> neighbourhood = new GeographyWithin<SimpleRHIAdopterHousehold>(this.myGeography,
					this.observedRadius, this);
			this.myNeighboursCache = IterableUtils.Iterable2ArrayList(neighbourhood.query());
			this.numCachedNeighbours = this.myNeighboursCache.size();
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(this.getAgentName() + " found neighbours : " + this.myNeighboursCache.toString());
			}

		}

		if (this.mainContext.logger.isTraceEnabled())
		{
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
	public SimpleRHIAdopterHousehold(RHIContext context)
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
	public SimpleRHIAdopterHousehold()
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
	public SimpleRHIAdopterHousehold(RHIContext context, double[] otherDemandProfile)
	{
		super(context, otherDemandProfile);
		this.agentName = "Household_" + this.agentID;
		this.mainContext = context;
		this.setStartDateAndFirstThought();
		if (context.logger.isDebugEnabled())
		{
			context.logger.debug(this.agentName + " initialised, first thought at " + this.nextCogniscentDate.toGMTString());
		}

		if (RandomHelper.nextDouble() < 0.5)
		{
			this.currentHeating = HEATING_TYPE.OIL;
		}
		else
		{
			this.currentHeating = HEATING_TYPE.CALOR_GAS;
		}

		getWeightsFromParamsIfAvailable();

		this.yearHistoryHeatingEnergy = new double[Consts.DAYS_PER_YEAR];
		Arrays.fill(this.yearHistoryHeatingEnergy, 50);
	}

	private void getWeightsFromParamsIfAvailable()
	{
		try
		{
			this.wPay = (Double) RepastEssentials.GetParameter("wEconomic");
		}
		catch (IllegalParameterException e)
		{
			System.err.println(e.getMessage());
			// Print error, but ignore - wPay will retain its default value.
		}

		try
		{
			this.wHassle = (Double) RepastEssentials.GetParameter("wHassle");
		}
		catch (IllegalParameterException e)
		{
			System.err.println(e.getMessage());
			// Print error, but ignore - wHassle will retain its default value.
		}

		try
		{
			this.wSocial = (Double) RepastEssentials.GetParameter("wSocial");
		}
		catch (IllegalParameterException e)
		{
			System.err.println(e.getMessage());
			// Print error, but ignore - wSocial will retain its default value.
		}

	}

	public void setStartDateAndFirstThought()
	{
		Date startTime = null;
		try
		{
			startTime = (new SimpleDateFormat("dd/MM/yyyy")).parse("01/08/2014");
			// TODO:NastY -nasty -hard coded start date :(
		}
		catch (ParseException e)
		{

		}

		// Date startTime = (Date) this.mainContext.simStartDate.clone();
		// startTime.setTime(startTime.getTime() + ((long)
		// (this.mainContext.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 *
		// 1000)));
		// this.nextCogniscentDate = startTime;

		startTime.setTime(startTime.getTime() + ((long) (this.mainContext.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
		// this.nextCogniscentDate = startTime;
		try
		{
			this.nextCogniscentDate = (new SimpleDateFormat("dd/MM/yyyy")).parse("01/08/2035");
		}
		catch (ParseException e)
		{
			System.err.println("Weird - I can't parse this fixed string that I've always parsed before");
		}
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
	private double[] yearHistoryHeatingEnergy;

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
	public void addDaysHeatingEnergyToYearHistory()
	{
		this.yearHistoryHeatingEnergy = ArrayUtils.rotate(this.yearHistoryHeatingEnergy, ArrayUtils.sum(this.dayHistoryHeatingEnergy));
	}

	public double calculateDailyRHIBenefit(HEATING_TYPE oldType, HEATING_TYPE newType)
	{

		if (newType == null)
		{
			return 0;
		}
		else
		{

			// double estimatedRHIGen = this.potentialRHICapacity*24;
			double estimatedRHIGen;

			if (newType == HEATING_TYPE.SOLAR_THERMAL)
			{
				estimatedRHIGen = ArrayUtils.sum(this.getHistoricalWaterHeatDemand());
			}
			else
			{
				// estimatedRHIGen = ArrayUtils.sum(this
				// .getDayHistoryHeatingEnergy());
				estimatedRHIGen = ArrayUtils.avg(this.getYearHistoryHeatingEnergy());
			}

			this.mainContext.logger.info("calculating benefit of adopting " + newType + " over existing " + oldType + " with heat demand "
					+ estimatedRHIGen + " kWh, setpoint " + this.setPoint + " temperature " + this.currentInternalTemp);
			double directSavings = (RHIContext.heatingCostPerkWh.get(oldType) - RHIContext.heatingCostPerkWh.get(newType))
					* estimatedRHIGen;
			double RHITariffPence = this.currentTariff() / 10; // Note that
																// tariffs
																// are stored as
																// integer
																// tenths of
																// pence to aid
																// precision
			double RHIincome = (estimatedRHIGen * RHITariffPence);
			this.mainContext.logger.info("Gives RHI income of " + RHIincome + " added to direct savings of " + directSavings);

			return (RHIincome + directSavings);
		}
	}

	/**
	 * @return
	 */
	private double[] getYearHistoryHeatingEnergy()
	{
		// TODO Auto-generated method stub
		return this.yearHistoryHeatingEnergy;
	}

	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateSmartControlBenefit()
	{

	}

}
