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

public class Household extends HouseholdProsumer {
	private int PVCapital = 5000000;        // Budget in tenths of pence
	private int smartContCapital = 750000; 
	
	private Geography<Household> myGeography;
	private ArrayList<Household> myNeighboursCache;
	private int numCachedNeighbours;

	private SCTModel PVDecisionModel = new SCTModel();
	
	private AdoptionContext mainContext = (AdoptionContext) super.mainContext;
	
	private Date nextCogniscentDate;

	private int numThoughts;
	//public boolean hasSmartControl;

	//public boolean hasElectricVehicle;
	private double potentialPVCapacity = RandomHelper.nextDoubleFromTo(2, 4);
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
	private double perceivedPVBenefit = 0;
	


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

		if (mainContext.getDateTime().getTime() > this.nextCogniscentDate.getTime()	&& mainContext.getDateTime().getTime() <= (this.nextCogniscentDate.getTime() + Consts.MSECS_PER_DAY)) {
			mainContext.logger.debug(this.getAgentName() + " Thinking with PV ownership = "+this.getHasPV()+"..."+this.PVlikelihood);
			considerOptions(); //Could make the consideration further probabilistic...
			mainContext.logger.debug("Resulting in PV ownership = "+this.getHasPV()+", likelihood:"+this.PVlikelihood+", neightbours:"+this.numCachedNeighbours);

			// this.myGeography.move(this, this.myGeography.getGeometry(this));
			numThoughts++;
			decisionUrgency = Math.exp(-((mainContext.dateToTick(mainContext.getTarriffAvailableUntil()) - mainContext.getTickCount())/(mainContext.ticksPerDay*28)));
			mainContext.logger.debug("New decision urgency = " + decisionUrgency + " from tariff available until tick: " + mainContext.dateToTick(mainContext.getTarriffAvailableUntil()));
			this.nextCogniscentDate.setTime(mainContext.getDateTime().getTime() + ((long) (mainContext.nextThoughtGenerator.nextDouble() * Consts.MSECS_PER_DAY)));
		}
	}

	private void considerOptions() {
		PVlikelihood = microgenPropensity;
		mainContext.logger.trace(this.agentName
				+ " gathering information from baseline likelihood of "
				+ PVlikelihood + "...");
		checkTariffs();
		calculateSCT();
	

		if (RandomHelper.nextDouble() > habit) {
			// habit change - introducing a longer term feedback, 
			// should be equivalent to norm shifting over the population.
			microgenPropensity = PVlikelihood;
			mainContext.logger
					.trace("Updating propensity, i.e. changing habit, based on likelihood");
		}
		
		mainContext.logger.trace(this.agentName
				+ " making decision based on likelihood of " + PVlikelihood
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
		this.PVDecisionModel.setPerceptionOfOthers(this.observeNeighbours());		
		this.PVDecisionModel.setSelfEfficacy(this.economicAbility); //Could do this one off at initialisation
		this.PVDecisionModel.setOutcomeExpectation(this.decisionUrgency * this.economicSensitivity * this.perceivedPVBenefit);
		
		/*
		 * Set economic factors in socio structural construct.  Note that a value of 0
		 * is an absolute veto
		 */
		double quote = this.mainContext.getPVSystemPrice(this.potentialPVCapacity);
		if (this.PVCapital < quote || this.potentialPVCapacity == 0)
		{
			this.PVDecisionModel.setSocioStructural(0);			
		}
		else
		{
			double socioStructuralFactors = 0; //0 = absolutely not, 1 = maximum likelihood
			socioStructuralFactors = (this.PVCapital - quote) / this.PVCapital; // Cheaper it is, the more attractive - free = socio structural of 1
			this.PVDecisionModel.setSocioStructural(socioStructuralFactors);
		}
		
		this.PVlikelihood = this.PVDecisionModel.getGoal();	
	}

	private void makeDecision() {
		mainContext.logger.trace(this.getAgentName()
				+ " has microgen propensity " + this.microgenPropensity
				+ " and PV adoption likelihood " + PVlikelihood);
		
		this.PVDecisionModel.setAbsoluteBehaviourThreshold(this.getAdoptionThreshold());
		this.PVDecisionModel.calculateBehaviour();
		
		this.mainContext.logger.debug("Calculated behaviour from SCT" + this.PVDecisionModel.getBehaviour());
		
		//if (PVlikelihood > getAdoptionThreshold()) 
		if (this.PVDecisionModel.getBinaryBehaviourDecisionHardThreshold()) 		
		{
			mainContext.logger.debug(this.agentName + " Adopted PV");
			this.setPV();

		}		
		
		if (false)//(ArrayUtils.sum(this.baseProfile)*RandomHelper.nextDouble()*365*100 > smartContCapital)
		{
			this.setWattboxController();			
		}

	}
	
	/**
	 * 
	 */
	public void setPV() {
		this.hasPV = true;
		this.ratedPowerPV=this.potentialPVCapacity;
		
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

	private double observeNeighbours() {
		mainContext.logger.trace("Observing neighbours");
		ArrayList<Household> neighbours = getNeighbours();
		int observedAdoption = 0;
		int observed = 0;
		for (Household h : neighbours) {
			mainContext.logger.trace("Into observation loop");
			boolean observe = (RandomHelper.nextDouble() > 0.5);
			observe = true; // for testing

			if (observe) {
				if (h.getHasPV()) {
					observedAdoption++;
				}
				observed++;
			}
		}
		
		mainContext.logger.trace("Returninglikelihood to agent "
				+ this.getAgentName() + " based on " + observedAdoption
				+ " of " + numCachedNeighbours
				+ " neighbours observed to have PV (" + observed
				+ " observed this round)");

		// Likelihood of adopting now - based on observation alone
		// Note that the 0.5 is an arbitrary and tunable parameter.
		return observed == 0 ? 0 : ((double) observedAdoption) / observed;

	}

	/*
	 * Note - OK to cache neighbours as static in this simulation. If households
	 * may move to different physical houses, this would have to change.
	 */
	private ArrayList<Household> getNeighbours() {

		if (this.myNeighboursCache == null) 
		{
			GeographyWithin<Household> neighbourhood = new GeographyWithin<Household>(myGeography, observedRadius, this);
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
		//Date startTime = (Date) context.simStartDate.clone();
	//	startTime.setTime(startTime.getTime() + ((long) (context.nextThoughtGenerator.nextDouble() * 24 * 60 * 60 * 1000)));
	//	this.nextCogniscentDate = startTime;

	}
	
	/**
	 * Test for Richard
	 * need no arg constructor for shapefile load
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
	public Household(AdoptionContext context, double[] otherDemandProfile) {
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
					.parse("01/04/2010");
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

	public void setContext(AdoptionContext c) {
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

	@ScheduledMethod(start = 49, interval = 48, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateDailyPVBenefit()
	{
		//EST generation factor = 833 kWh per kWp per year for South facing - 640 for East facing, 661 West facing
		//Heavy shading attenuates by half.  For now - use 750 as reasonable estimate
		double estimatedPVGen = this.potentialPVCapacity * (Consts.PV_KWH_PER_KWP / 365);
		this.perceivedPVBenefit = estimatedPVGen * currentTariff();
	}
	
	@ScheduledMethod(start = 2600, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void estimateSmartControlBenefit()
	{
		
	}
	
	private void initialiseSCT()
	{
		this.PVDecisionModel.setWeightGoalToBehaviour(5);
		this.PVDecisionModel.setWeightSocioStructuralToOutcomeExp(4);
		this.PVDecisionModel.setWeightSocioStructuralToPerceptionOfOthers(2) ;
		this.PVDecisionModel.setWeightSelfEfficacyToOutcomeExp(5) ;
		this.PVDecisionModel.setWeightOutcomeExpToGoal(5) ;
		this.PVDecisionModel.setWeightPerceptionOfOthersToGoal(3); 
		this.PVDecisionModel.setWeightSelfEfficacyToGoal(4); 
		this.PVDecisionModel.setWeightSocioStructuralToGoal(5); 
		this.PVDecisionModel.setWeightOutcomeExpToBehaviour(5) ;
		this.PVDecisionModel.setWeightPerceptionOfOthersToBehaviour(1) ;
		this.PVDecisionModel.setWeightSelfEfficacyToBehaviour(5) ;
		this.PVDecisionModel.setWeightSocioStructuralToBehaviour(5) ;
		this.PVDecisionModel.setWeightOutcomeToSelfEfficacy(4) ;
		this.PVDecisionModel.setWeightOutcomeToSocioStructural(1) ;
		this.PVDecisionModel.setWeightOutcomeToOutcomeExp(5);
	}
}
