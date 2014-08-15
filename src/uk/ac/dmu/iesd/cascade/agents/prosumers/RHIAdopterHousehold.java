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
import uk.ac.dmu.iesd.cascade.base.Consts.RHI_TYPE;
import uk.ac.dmu.iesd.cascade.behaviour.psychological.cognitive.SCTModel;
import uk.ac.dmu.iesd.cascade.context.AdoptionContext;
import uk.ac.dmu.iesd.cascade.context.RHIContext;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

public class RHIAdopterHousehold extends HouseholdProsumer {
	private int RHICapital = 5000000;        // Budget in tenths of pence
	private int smartContCapital = 750000; 
	
	private RHI_TYPE RHIEligibleHeatingTechnology;
	/**
	 * @param rHIEligibleHeatingTechnology the rHIEligibleHeatingTechnology to set
	 */
	public void setRHIEligibleHeatingTechnology(
			RHI_TYPE rHIEligibleHeatingTechnology) {
		RHIEligibleHeatingTechnology = rHIEligibleHeatingTechnology;
	}

	private boolean renewableHeatAdopted;
	
	public RHI_TYPE RHIEligibleHeatingOwned;
	
	/**
	 * @return the rHIEligibleHeatingOwned
	 */
	public RHI_TYPE getRHIEligibleHeatingOwned() {
		return RHIEligibleHeatingOwned;
	}

	/**
	 * @param rHIEligibleHeatingOwned the rHIEligibleHeatingOwned to set
	 */
	public void setRHIEligibleHeatingOwned(RHI_TYPE rHIEligibleHeatingOwned) {
		RHIEligibleHeatingOwned = rHIEligibleHeatingOwned;
	}

	private Geography<RHIAdopterHousehold> myGeography;
	private ArrayList<RHIAdopterHousehold> myNeighboursCache;
	private int numCachedNeighbours;

	private SCTModel RHIDecisionModel = new SCTModel();
	
	private RHIContext mainContext = (RHIContext) super.mainContext;
	
	private Date nextCogniscentDate;

	private int numThoughts;
	//public boolean hasSmartControl;

	//public boolean hasElectricVehicle;
	private double potentialRHICapacity = RandomHelper.nextDoubleFromTo(2, 4);
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
	public double RHIlikelihood;
	public double insulationPropensity;
	public double HEMSPropensity;
	//public double EVPropensity;
	//public double habit;
	private double adoptionThreshold;

	private double economicSensitivity;
	private double decisionUrgency = 0.001;
	private double perceivedRHIBenefit = 0;
	private double ratedPowerRHI;
	


	public boolean getHasRHI() {
		return renewableHeatAdopted;
	}
	
	public int getHasRHINum() {
		return getHasRHI()?1:0;
	}
	
	public int getHasSmartControlNum() {
		return hasSmartControl?1:0;
	}

	@ScheduledMethod(start = 3, interval = 48, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void checkTime() {
		if (mainContext == null) {
			Context testContext = ContextUtils.getContext(this);
			if (testContext instanceof AdoptionContext) {
				this.mainContext = (RHIContext) testContext;
			}
		}

		if (mainContext.getDateTime().getTime() > this.nextCogniscentDate.getTime()	&& mainContext.getDateTime().getTime() <= (this.nextCogniscentDate.getTime() + Consts.MSECS_PER_DAY)) {
			mainContext.logger.debug(this.getAgentName() + " Thinking with RHI ownership = "+this.getHasRHI()+"..."+this.RHIlikelihood);
			considerOptions(); //Could make the consideration further probabilistic...
			mainContext.logger.debug("Resulting in RHI ownership = "+this.getHasRHI()+", likelihood:"+this.RHIlikelihood+", neightbours:"+this.numCachedNeighbours);

			// this.myGeography.move(this, this.myGeography.getGeometry(this));
			numThoughts++;
			decisionUrgency = Math.exp(-((mainContext.dateToTick(mainContext.getRHIAvailableUntil()) - mainContext.getTickCount())/(mainContext.ticksPerDay*28)));
			mainContext.logger.debug("New decision urgency = " + decisionUrgency + " from tariff available until tick: " + mainContext.dateToTick(mainContext.getRHIAvailableUntil()));
			this.nextCogniscentDate.setTime(mainContext.getDateTime().getTime() + ((long) (mainContext.nextThoughtGenerator.nextDouble() * Consts.MSECS_PER_DAY)));
		}
	}

	private void considerOptions() {
		RHIlikelihood = microgenPropensity;
		mainContext.logger.trace(this.agentName
				+ " gathering information from baseline likelihood of "
				+ RHIlikelihood + "...");
		checkTariffs();
		calculateSCT();
	

		if (RandomHelper.nextDouble() > habit) {
			// habit change - introducing a longer term feedback, 
			// should be equivalent to norm shifting over the population.
			microgenPropensity = RHIlikelihood;
			mainContext.logger
					.trace("Updating propensity, i.e. changing habit, based on likelihood");
		}
		
		mainContext.logger.trace(this.agentName
				+ " making decision based on likelihood of " + RHIlikelihood
				+ "...");
		makeDecision();
	}

	/**
	 * 
	 */
	private void calculateSCT() {

		/*
		 * Set perception of others' behaviour based on observed percentage
		 */
		this.RHIDecisionModel.setPerceptionOfOthers(this.observeNeighbours());		
		this.RHIDecisionModel.setSelfEfficacy(this.economicAbility); //Could do this one off at initialisation
		this.RHIDecisionModel.setOutcomeExpectation(this.decisionUrgency * this.economicSensitivity * this.perceivedRHIBenefit);
		
		/*
		 * Set economic factors in socio structural construct.  Note that a value of 0
		 * is an absolute veto
		 */
		double quote = this.mainContext.getRHISystemPrice(this.potentialRHICapacity, this.RHIEligibleHeatingTechnology);
		if (this.RHICapital < quote || this.potentialRHICapacity == 0)
		{
			this.RHIDecisionModel.setSocioStructural(0);			
		}
		else
		{
			double socioStructuralFactors = 0; //0 = absolutely not, 1 = maximum likelihood
			socioStructuralFactors = (this.RHICapital - quote) / this.RHICapital; // Cheaper it is, the more attractive - free = socio structural of 1
			this.RHIDecisionModel.setSocioStructural(socioStructuralFactors);
		}
		
		this.RHIlikelihood = this.RHIDecisionModel.getGoal();	
	}

	private void makeDecision() {
		mainContext.logger.trace(this.getAgentName()
				+ " has microgen propensity " + this.microgenPropensity
				+ " and RHI adoption likelihood " + RHIlikelihood);
		
		this.RHIDecisionModel.setAbsoluteBehaviourThreshold(this.getAdoptionThreshold());
		this.RHIDecisionModel.calculateBehaviour();
		
		this.mainContext.logger.debug("Calculated behaviour from SCT" + this.RHIDecisionModel.getBehaviour());
		
		//if (RHIlikelihood > getAdoptionThreshold()) 
		if (this.RHIDecisionModel.getBinaryBehaviourDecisionHardThreshold()) 		
		{
			mainContext.logger.debug(this.agentName + " Adopted RHI");
			this.setRHI();

		}		
		
		if (false)//(ArrayUtils.sum(this.baseProfile)*RandomHelper.nextDouble()*365*100 > smartContCapital)
		{
			this.setWattboxController();			
		}

	}
	
	/**
	 * 
	 */
	public void setRHI() {
		this.renewableHeatAdopted = true;
		this.ratedPowerRHI=this.potentialRHICapacity;
		this.RHIEligibleHeatingOwned = this.RHIEligibleHeatingTechnology;
	}


	private void checkTariffs() {
		double RHITariffPence = currentTariff();	
	
		RHIlikelihood += this.economicSensitivity * RHITariffPence * (decisionUrgency*1000);
	}
	
	private double currentTariff()
	{
		return ((double) mainContext
				.getRHITariff(this.potentialRHICapacity, this.RHIEligibleHeatingTechnology)) / 1000; // note tariffs
																// stored as
																// integer
																// tenths of
																// pence
		// Add the weight of the tariff influence to the adoption likelihood
	}

	private double observeNeighbours() {
		mainContext.logger.trace("Observing neighbours");
		ArrayList<RHIAdopterHousehold> neighbours = getNeighbours();
		int observedAdoption = 0;
		int observed = 0;
		for (RHIAdopterHousehold h : neighbours) {
			mainContext.logger.trace("Into observation loop");
			boolean observe = (RandomHelper.nextDouble() > 0.5);
			observe = true; // for testing

			if (observe) {
				if (h.getHasRHI()) {
					observedAdoption++;
				}
				observed++;
			}
		}
		
		mainContext.logger.trace("Returninglikelihood to agent "
				+ this.getAgentName() + " based on " + observedAdoption
				+ " of " + numCachedNeighbours
				+ " neighbours observed to have RHI (" + observed
				+ " observed this round)");

		// Likelihood of adopting now - based on observation alone
		// Note that the 0.5 is an arbitrary and tunable parameter.
		return observed == 0 ? 0 : ((double) observedAdoption) / observed;

	}

	/*
	 * Note - OK to cache neighbours as static in this simulation. If households
	 * may move to different physical houses, this would have to change.
	 */
	private ArrayList<RHIAdopterHousehold> getNeighbours() {

		if (this.myNeighboursCache == null) 
		{
			GeographyWithin<RHIAdopterHousehold> neighbourhood = new GeographyWithin<RHIAdopterHousehold>(myGeography, observedRadius, this);
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
	public RHIAdopterHousehold(RHIContext context) {
		this(context, new double[48]);
		//Date startTime = (Date) context.simStartDate.clone();
	//	startTime.setTime(startTime.getTime() + ((long) (context.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
	//	this.nextCogniscentDate = startTime;

	}
	
	/**
	 * Test for Richard
	 * need no arg constructor for shapefile load
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
	public RHIAdopterHousehold(RHIContext context, double[] otherDemandProfile) {
		super(context,otherDemandProfile);
		this.agentName = "Household_" + this.agentID;
		this.mainContext = context;
		setStartDateAndFirstThought();
		initialiseSCT();
		context.logger.debug(this.agentName+" initialised, first thought at "+nextCogniscentDate.toGMTString());
		}
	
	public void setStartDateAndFirstThought()
	{
		Date startTime = null;
		try {
			startTime = (new SimpleDateFormat("dd/MM/yyyy"))
					.parse("01/08/2014"); // TODO: Nasty - nasty - hard coded start date :(
		} catch (ParseException e) {
			System.err.println("Weird - I can't parse this fixed string that I've always parsed before");
		}
		
		//Date startTime = (Date) this.mainContext.simStartDate.clone();
	//	startTime.setTime(startTime.getTime() + ((long) (this.mainContext.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
	//	this.nextCogniscentDate = startTime;
		
		startTime
				.setTime(startTime.getTime()
						+ ((long) (this.mainContext.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
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

	public void setContext(RHIContext c) {
		this.mainContext = c;
		super.mainContext = c;
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
		
		//dailySaving += currentTariff() * this.getRHIGen();
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

	@ScheduledMethod(start = 49, interval = 48, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateDailyRHIBenefit()
	{
	
		double estimatedRHIGen = this.potentialRHICapacity;
		this.perceivedRHIBenefit = estimatedRHIGen * currentTariff();
	}
	
	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateSmartControlBenefit()
	{
		
	}
	
	private void initialiseSCT()
	{
		this.RHIDecisionModel.setWeightGoalToBehaviour(5);
		this.RHIDecisionModel.setWeightSocioStructuralToOutcomeExp(4);
		this.RHIDecisionModel.setWeightSocioStructuralToPerceptionOfOthers(2) ;
		this.RHIDecisionModel.setWeightSelfEfficacyToOutcomeExp(5) ;
		this.RHIDecisionModel.setWeightOutcomeExpToGoal(5) ;
		this.RHIDecisionModel.setWeightPerceptionOfOthersToGoal(3); 
		this.RHIDecisionModel.setWeightSelfEfficacyToGoal(4); 
		this.RHIDecisionModel.setWeightSocioStructuralToGoal(5); 
		this.RHIDecisionModel.setWeightOutcomeExpToBehaviour(5) ;
		this.RHIDecisionModel.setWeightPerceptionOfOthersToBehaviour(1) ;
		this.RHIDecisionModel.setWeightSelfEfficacyToBehaviour(5) ;
		this.RHIDecisionModel.setWeightSocioStructuralToBehaviour(5) ;
		this.RHIDecisionModel.setWeightOutcomeToSelfEfficacy(4) ;
		this.RHIDecisionModel.setWeightOutcomeToSocioStructural(1) ;
		this.RHIDecisionModel.setWeightOutcomeToOutcomeExp(5);
	}
}
