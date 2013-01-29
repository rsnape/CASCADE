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
import uk.ac.dmu.iesd.cascade.context.AdoptionContext;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;
import uk.ac.dmu.iesd.cascade.base.*;

public class Household extends HouseholdProsumer {
	private int PVCapital = 5000000;        // Cost in tenths of pence
	private int smartContCapital = 750000; 
	
	private Geography<Household> myGeography;
	private ArrayList<Household> myNeighboursCache;
	private int numCachedNeighbours;

	private AdoptionContext mainContext = (AdoptionContext) super.mainContext;
	
	private Date nextCogniscentDate;

	private int numThoughts;
	//public boolean hasSmartControl;

	//public boolean hasElectricVehicle;
	private double potentialPVCapacity = 3.0;
	public double observedRadius;

	private double perceivedSmartControlBenefit;

	/**
	 * A prosumer agent's base name it can be reassigned (renamed) properly by
	 * descendants of this class
	 **/
	protected static String agentBaseName = "Household";
	
	
	
	//private CascadeContext mainContext;
	//public int defraCategory;
	public double microgenPropensity;
	public double economicAbility;
	public double PVlikelihood;
	public double insulationPropensity;
	public double HEMSPropensity;
	//public double EVPropensity;
	//public double habit;
	private double adoptionThreshold;

	private double economicSensitivity;
	private double decisionUrgency = 0.001;

	public boolean getHasPV() {
		return hasPV;
	}
	
	public int getHasPVNum() {
		return hasPV?1:0;
	}
	
	public int getHasSmartControlNum() {
		return hasSmartControl?1:0;
	}

	@ScheduledMethod(start = 3, interval = 48, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void checkTime() {
		if (mainContext == null) {
			Context testContext = ContextUtils.getContext(this);
			if (testContext instanceof AdoptionContext) {
				this.mainContext = (AdoptionContext) testContext;
			}
		}

		if (mainContext.getDateTime().getTime() > this.nextCogniscentDate.getTime()	&& mainContext.getDateTime().getTime() <= (this.nextCogniscentDate.getTime() + 24 * 60 * 60 * 1000)) {
			mainContext.logger.debug(this.getAgentName() + " Thinking with PV ownership = "+this.getHasPV()+"..."+this.PVlikelihood);
			considerOptions();
			// this.myGeography.move(this, this.myGeography.getGeometry(this));
			numThoughts++;
			decisionUrgency = 1.0 / (mainContext.dateToTick(mainContext.getTarriffAvailableUntil()) - mainContext.getTickCount());
			mainContext.logger.debug("Resulting in PV ownership = "+this.getHasPV()+", likelihood:"+this.PVlikelihood+", neightbours:"+this.numCachedNeighbours);
			this.nextCogniscentDate.setTime(mainContext.getDateTime().getTime() + ((long) (mainContext.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
		}
	}

	private void considerOptions() {
		PVlikelihood = microgenPropensity;
		mainContext.logger.trace(this.agentName
				+ " gathering information from baseline likelihood of "
				+ PVlikelihood + "...");
		observeNeighbours();
		checkTariffs();
		mainContext.logger.trace(this.agentName
				+ " making decision based on likelihood of " + PVlikelihood
				+ "...");
		makeDecision();
	}

	private void makeDecision() {
		mainContext.logger.trace(this.getAgentName()
				+ " has microgen propensity " + this.microgenPropensity
				+ " and PV adoption likelihood " + PVlikelihood);
		if (PVlikelihood > getAdoptionThreshold()) {
			mainContext.logger.trace(this.agentName + " Adopted PV");
			this.hasPV = true;
			this.ratedPowerPV=3;

		}
		
		
		if (ArrayUtils.sum(this.baseProfile)*RandomHelper.nextDouble()*365*100 > smartContCapital)
		{
			this.hasSmartControl = true;
			this.setWattboxController();
			
		}
		
		
		//Tests for ECEEE scenarios to check PV generation bit working

		//this.hasPV=false;
		
		/*if (mainContext.getTickCount() > 0)//(Consts.AGGREGATOR_TRAINING_SP+Consts.AGGREGATOR_PROFILE_BUILDING_SP))
		{
			this.hasPV=true;
			this.ratedPowerPV=3;
		}*/

	}
	
	public double getPVGen()
	{
		return this.PVGeneration();
	}

	private void checkTariffs() {
		double PVTariffPence = currentTariff();	
	
		PVlikelihood += this.economicSensitivity * PVTariffPence * (decisionUrgency*1000);
	}
	
	private double currentTariff()
	{
		return ((double) mainContext
				.getPVTariff(this.potentialPVCapacity)) / 1000; // note tariffs
																// stored as
																// integer
																// tenths of
																// pence
		// Add the weight of the tariff influence to the adoption likelihood
	}

	private void observeNeighbours() {
		mainContext.logger.trace("Observing neighbours");
		ArrayList<Household> neighbours = getNeighbours();
		int observedAdoption = 0;
		int observed = 0;
		for (Household h : neighbours) {
			mainContext.logger.trace("Into observation loop");
			boolean observe = (RandomHelper.nextDouble() > 0.5);
			// observe = true; // for testing

			if (observe) {
				if (h.getHasPV()) {
					observedAdoption++;
				}
				observed++;
			}

			// Likelihood of adopting now - based on observation alone
			// Note that the 0.5 is an arbitrary and tunable parameter.
			PVlikelihood += (0.4 * observedAdoption);
			mainContext.logger.trace("Adding likelihood to agent "
					+ this.getAgentName() + " based on " + observedAdoption
					+ " of " + numCachedNeighbours
					+ " neighbours observed to have PV (" + observed
					+ " observed this round)");

			if (RandomHelper.nextDouble() > habit) {
				// habit change
				microgenPropensity = PVlikelihood;
				mainContext.logger
						.trace("Updating propensity, i.e. changing habit, based on likelihood");
			}

		}
	}

	/*
	 * Note - OK to cache neighbours as static in this simulation. If households
	 * may move to different physical houses, this would have to change.
	 */
	private ArrayList<Household> getNeighbours() {

		if (this.myNeighboursCache == null) {
			GeographyWithin<Household> neighbourhood = new GeographyWithin<Household>(
					myGeography, observedRadius, this);
			this.myNeighboursCache = IterableUtils.Iterable2ArrayList(neighbourhood.query());
			this.numCachedNeighbours = myNeighboursCache.size();
			mainContext.logger.trace(this.getAgentName()
					+ " found neighbours : " + myNeighboursCache.toString());
		}

		mainContext.logger.trace(this.getAgentName() + " has "
				+ this.numCachedNeighbours + " neighbours : "
				+ myNeighboursCache.toString());

		return this.myNeighboursCache;
	}

	@Override
	public String getAgentName() {
		return this.agentName;
	}

	
	/**
	 * Constructs a prosumer agent with the context in which is created
	 * 
	 * @param context
	 *            the context in which this agent is situated
	 */
	public Household(AdoptionContext context) {
		this(context, new double[48]);
		this.mainContext = context;
		Date startTime = (Date) context.simStartDate.clone();
		startTime.setTime(startTime.getTime() + ((long) (context.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
		this.nextCogniscentDate = startTime;
		context.logger.debug(this.agentName+" first thought at "+nextCogniscentDate.toGMTString());
	}

	/**
	 * Constructs a prosumer agent with the context in which is created.
	 * 
	 * Needs some work - note the hard coded time on which to base all next
	 * decision times.
	 * 
	 */
	public Household(AdoptionContext context, double[] otherDemandProfile) {
		super(context,otherDemandProfile);
		this.agentName = "Household_" + this.agentID;
		Date startTime = null;
		try {
			startTime = (new SimpleDateFormat("dd/mm/yyyy"))
					.parse("01/04/2010");
		} catch (ParseException e) {
			System.err.println("Can't parse this");
		}
		startTime
				.setTime(startTime.getTime()
						+ ((long) (context.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
		this.nextCogniscentDate = startTime;
		this.economicSensitivity = RandomHelper.nextDouble();
		this.perceivedSmartControlBenefit = RandomHelper.nextDouble(); // comment this out for non-smart adoption scenarios
	}

	public int getNumThoughts() {
		return numThoughts;
	}
	
	@Override
	public int getDefraCategory()
	{
		return defraCategory;
	}

	public void setContext(AdoptionContext c) {
		this.mainContext = c;
	}

	public void setGeography(Geography g) {
		this.myGeography = g;
	}

	/**
	 * @param adoptionThreshold the adoptionThreshold to set
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
		return adoptionThreshold;
	}
	
	double[] baseProfile = new double[48];
	double dailySaving = 0;
	ArrayList<Double> dailySavings = new ArrayList<Double>();
	private double dailyBaseTotal;
	
	
	/*
	 * Note - scheduled methods must be public in order that the scheduler can call them!!
	 */
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void determineBaseline()
	{
		int now = this.mainContext.getTickcount();
		if (now < 48)
		{
			baseProfile[now]=this.getNetDemand();
		}

	}


	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void calculateSaving()
	{
		double d = this.getNetDemand();
		int hh = mainContext.getTimeslotOfDay();
	
		if (hh == 0)
		{
			dailySavings.add(dailySaving);
			dailySaving = 0;
		}
		
		dailySaving += currentTariff() * this.getPVGen();
		//all avoided consumption presently valued at 10p per unit.  Future implementations
		//should translate the current price prediction to a cost
		double avoidedkWhprice = 100;
		avoidedkWhprice = 100+50*this.getPredictedCostSignal()[hh];
		dailySaving += avoidedkWhprice * (baseProfile[hh] - Math.max(0,d));
		if (d < 0)
		{
			//Exporting power, add an extra benefit at 3.1p per unit
			dailySaving += AdoptionContext.FIT_EXPORT_TARIFF * Math.abs(d);
		}

	}
	
	
	public double getDailySaving()
	{
		if (dailySavings.size() > 0)
		{
			return dailySavings.get(dailySavings.size()-1);
		}
		return 0;
	}

/*	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimatePVBenefit()
	{
		
	}
	
	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateSmartControlBenefit()
	{
		
	}*/
}
