package uk.ac.dmu.iesd.cascade.context;

//import javax.media.jai.WarpAffine;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;

import repast.simphony.context.Context;
import repast.simphony.context.ContextEvent;
import repast.simphony.context.ContextEvent.EventType;
import repast.simphony.context.ContextListener;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.query.PropertyEquals;
import repast.simphony.query.Query;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.visualization.gis.DisplayGIS;
import uk.ac.dmu.iesd.cascade.agents.prosumers.Household;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.util.DatedTimeSeries;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;
import cern.jet.random.Poisson;

public class AdoptionContext extends CascadeContext{
	
	public static final double FIT_EXPORT_TARIFF = 45;
	public Poisson nextThoughtGenerator = RandomHelper.createPoisson(30.0);
	
	private class CountUpdater implements ContextListener {

		public void eventOccured(ContextEvent ev) {
			if (ev.getType().equals(EventType.AGENT_ADDED)
					|| ev.getType().equals(EventType.AGENT_REMOVED)) {
				AdoptionContext.this.logger
						.trace("Context listener called for agent add / remove");
				String className = ev.getTarget().getClass().getName();
				if (agentCounts.containsKey(className)) {
					int oldC = agentCounts.get(className);

					agentCounts.remove(className);
					agentCounts.put(className, oldC + 1);
				}
			}
		}
	}

	DateFormat ukDateParser = new SimpleDateFormat("dd/MM/yyyy");
	public Logger logger;
	DisplayGIS styledDisplay;
	DatedTimeSeries<TreeMap<Integer,Integer>> PVFITs = new DatedTimeSeries<TreeMap<Integer,Integer>>(); // Holds PV feed in tarriffs in system
										// capacity vs. tenths of
										// pence / eurocents per kWh
	
	DatedTimeSeries<Integer> PVCosts = new DatedTimeSeries<Integer>(); // Holds PV feed in tarriffs in system
	// capacity vs. tenths of
	// pence / eurocents per kWh
	
	WeakHashMap<String,Integer> agentCounts = new WeakHashMap<String,Integer>();
	Calendar simTime = new GregorianCalendar();
	public Date simStartDate;
/*	public double[] insolationArray;
	public double[] windSpeedArray;
	public double[] airTemperatureArray;
	public double[] airDensityArray;
	public int weatherDataLength;
	private int ticksPerDay;
	public EmpiricalWalker drawOffGenerator;
	public EmpiricalWalker occupancyGenerator;
	public Normal waterUsageGenerator;
	public Normal buildingLossRateGenerator;
	public Normal thermalMassGenerator;
	public Uniform coldAndWetApplTimeslotDelayRandDist;
	public EmpiricalWalker wetApplProbDistGenerator;
	private Network<?> economicNetwork;*/

	/******************
	 * This method steps the model's internal gregorian calendar on each model
	 * tick
	 * 
	 * Input variables: none
	 * 
	 ******************/
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void calendarStep() {
		this.logger.trace("Incrementing simulation date and time");
		simTime.add(GregorianCalendar.MINUTE, 30);
		this.logger.trace("Advancing date to " + ukDateParser.format(simTime.getTime()));
	}

	@ScheduledMethod(start = (48 * 365 * 4), interval = 0, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void endSim() {
		for (Object thisH : this.getObjects(Household.class)) {
			Household h = (Household) thisH;
			this.logger.trace(h.getAgentName() + " has had " + h.getNumThoughts());
		}
		RepastEssentials.EndSimulationRun();
		this.logger = null; // remove reference to logger, so context can be gc'd

	}

	public Date getDateTime() {
		return simTime.getTime();
	}
	
	
    public Date getTarriffAvailableUntil()
    {
    	Date returnDate = new Date();    	
    	Date now = this.getDateTime();
    	
    	Date retVal = this.PVFITs.getFirstKeyFollowing(now);
    	if (retVal == null)
    	{
    		this.logger.debug("return last valid date as appears " + ukDateParser.format(now) + " is past last key");
    		retVal = this.PVFITs.getLastValidDate();
    	}
    	
    	
/*    	if (now.before(parseUKDate("31/10/2011")))
    	{
			returnDate = parseUKDate("01/04/2035");
    	}
    	else if (now.after(parseUKDate("31/10/2011")) && now.before(parseUKDate("12/12/2011")))
    	{
    		returnDate = parseUKDate("12/12/2011");
    	}
    	else if (now.after(parseUKDate("12/12/2011")) && now.before(parseUKDate("19/01/2012")))
    	{
    		returnDate = parseUKDate("01/08/2012");
    	}
    	else
    	{
    		//tariffs reviewed every 3 months
    		Date d = new Date();
    		d.setMonth((now.getMonth() / 3)*3);
    		d.setYear(now.getYear());
    		d.setDate(1);
    		Calendar cal = Calendar.getInstance();
    		cal.setTime(d);
    		cal.add( Calendar.MONTH, 3);
    		returnDate = cal.getTime();
    	}*/
    	
    	return retVal;
    }

    /**
     * Return a date object for an input string in the UK date format (dd/MM/yyyy)
     * @param d
     * @return
     */
    Date parseUKDate(String d)
    {
    	Date returnDate = null;
    	try {
    		returnDate = ukDateParser.parse(d);
		} catch (ParseException e) {
			this.logger.warn("Asked to parse "+d+" which didn't parse as a uk format date (dd/MM/yyyy)");
		}
		return returnDate;
    }
    
	public Integer getPVTariff(double cap) {
    	Date now = this.getDateTime();
    	
    	this.logger.trace("Getting PV tariff for capacity" + cap + " on date " + ukDateParser.format(now));
    	
    	if (now.before(this.PVFITs.getFirstDate()))
    	{
			return 0;
    	}   	
		
    	SortedMap<Integer,Integer> tariffNow = this.PVFITs.getValue(now);
    	
    	tariffNow = tariffNow.tailMap((int)(cap+0.5));
    	Integer retVal;
    	if (tariffNow.size()==0)
    	{
    		retVal = 0;
    	}
    	else
    	{
    		retVal = tariffNow.get(tariffNow.firstKey());
    	}
    	
    	this.logger.trace("returning fit for capacity " + cap + " = " + retVal);
    	return retVal;
    	
		/*Iterator<Integer> iterator = tariffNow.keySet().iterator();
		while (iterator.hasNext()) {
			Integer key = iterator.next();
			if (key >= cap) {
				this.logger.trace("Returning value for up to " + key);
				return tariffNow.get(key);
			}
		}*/
	}

	int getAgentCount(Class clazz) {
		this.logger.trace("Get Agent count for " + clazz.getName()+" called.");
		String className = clazz.getName();
		if (agentCounts.containsKey(className))
		{
			this.logger.trace("Returning cached value");
			return agentCounts.get(className);
		}
		
		this.logger.trace("Counting and adding to cache");
		int count = this.getObjects(clazz).size();
		agentCounts.put(className, count);
		return count;
	}

	/**
	 * Must be a quicker way to count agents with a property than this...
	 * 
	 * Very slow
	 * 
	 * @return
	 */
	public double getAdoptionPercentage() {
		this.logger.trace("Get percentage called at " + System.nanoTime());
		Query<Household> adoptionQuery = new PropertyEquals<Household>(this,
				"hasPV", true);
		Iterable<Household> agentsWithPV = adoptionQuery.query();
		double ret = IterableUtils.count(agentsWithPV);
		this.logger.trace("Got count at " + System.nanoTime());
		
		this.logger.trace("Agents with PV count = " + ret);
		this.logger.trace("Total = " + getAgentCount(Household.class));
		ret*=100;
		ret /= getAgentCount(Household.class);
		this.logger.trace("percentage = " + ret);
		return ret;
	}

	
	public AdoptionContext(Context context){
		this(context, "01/01/2010");
	}
	
	public AdoptionContext(Context context,String date) {
		super(context);
		
		
		// set up a logger for the adoption context
		logger = Logger.getLogger("AdoptionLogger");
		logger.removeAllAppenders();
		logger.setLevel(Level.DEBUG); //Set this to TRACE for full log files.  Can filter what is actually output below
		ConsoleAppender console = new ConsoleAppender(new AdoptionLogLayout());
		console.setName("ConsoleOutput");
		console.setThreshold(Level.INFO);
		console.activateOptions(); // Needed or the appender appends everything from the logger
		
		logger.addAppender(console);
		logger.setAdditivity(false);
		
		FileAppender traceFile;
		try 
		{
			String parsedDate = (new SimpleDateFormat("yyyy.MMM.dd.HH_mm_ss_z")).format(new Date());
			traceFile = new FileAppender(new AdoptionLogLayout(),"output/myTrace"+parsedDate+".log");
			//traceFile.setMaxFileSize("1024000");
			//traceFile.setMaxBackupIndex(0);
			traceFile.setName("traceFileOutput");
			traceFile.setThreshold(Level.DEBUG);
			traceFile.activateOptions(); // Needed or the appender appends everything from the logger
			
			logger.addAppender(traceFile);

		} catch (IOException e1) {
			this.logger.warn("Creating file appender for logger failed");
			e1.printStackTrace();
		}

		logger.info("Adoption Context instantiated and logger configured");
		logger.info(logger);
		
		logger.setAdditivity(false);
		
		simStartDate = parseUKDate(date);
		simTime.setTime(simStartDate);

		//this.addContextListener(new CountUpdater());
	}

	// @ScheduledMethod(start=0,interval=0)
	// public void registerDisplayStyle()
	// {
	// GUIRegistry reg = RunState.getInstance().getGUIRegistry();
	//
	// DefaultDisplayData<Household> displayData = new
	// DefaultDisplayData<Household>(this);
	// for (Object proj : this.getProjections())
	// {
	// displayData.addProjection(((Projection)proj).getName());
	// }
	// styledDisplay = new DisplayGIS(displayData);
	// styledDisplay.registerAgentStyle(Household.class.getCanonicalName(), new
	// HouseholdTwoDStyle(), 0);
	// styledDisplay.init();
	// reg.addDisplay("New 2D display", GUIRegistryType.DISPLAY, styledDisplay);
	// styledDisplay.createPanel();
	// }
	//
	// @ScheduledMethod(start=1,interval=1)
	// public void updateDisplay()
	// {
	// styledDisplay.update();
	// }
	
	class AdoptionLogLayout extends SimpleLayout
	{
		@Override
		public String format(LoggingEvent ev)
		{
			return "[Tick " + RepastEssentials.GetTickCount() + "; "  + (ev.timeStamp-ev.getStartTime()) + "] : " + ev.getLevel().toString() + " - " + ev.getRenderedMessage() + "\n"; 
		}
	}
	
	public int dateToTick(Date d)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(simStartDate);
		int msecDiff = cal.compareTo(cal1);
		return msecDiff / (30*60*1000);
	}
	
	public Date tickToDate(int t)
	{
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(simStartDate);
		cal1.add(Calendar.MINUTE,t*30);
		return cal1.getTime();
	}

	public int getTickcount() {
		return (int) RepastEssentials.GetTickCount();
	}
	
	public int getRandomSeed()
	{
		return RandomHelper.getSeed();
	}

	/**
	 * @param ticksPerDay
	 */
	public void setNbOfTickPerDay(int ticksPerDay)
	{
		this.ticksPerDay =ticksPerDay;	
	}

	/**
	 * @return
	 */
	public int getNbOfTickPerDay()
	{
		return this.ticksPerDay;
	}

	/**
	 * Sets the economic network associated to this context 
	 * @param n the economic network
	 * @see #getEconomicNetwork
	 */
	public void setEconomicNetwork(Network<?> n){
		this.economicNetwork = n;
	}
	
	/**
	 * This method returns the tick time. 
	 * It is a wrapper around RepastEssential.GgetTickCount method, which returns the tick count as integer.
	 * @return current tick count of the model 
	 */
	public int getTickCount() {
		return (int) RepastEssentials.GetTickCount();
	}
	
	public int getTimeslotOfDay() {
		return (int) RepastEssentials.GetTickCount() % ticksPerDay;
	}
	
	public int getPricePerkWp()
	{
		Date now = this.getDateTime();
    	
    	this.logger.trace("Getting PV price per kWh on date " + ukDateParser.format(now));
    	
    	if (now.before(this.PVCosts.getFirstDate()))
    	{
			return 0;
    	}   	
		
    	return this.PVCosts.getValue(now);
    	
	}

	/**
	 * @param potentialPVCapacity
	 * @return
	 */
	public double getPVSystemPrice(double potentialPVCapacity) {
		double quote = this.getPricePerkWp() * potentialPVCapacity;
		double profit = RandomHelper.nextDoubleFromTo(Consts.INSTALLER_MIN_PROFIT, Consts.INSTALLER_MIN_PROFIT);
		double installationPrice = 500000; //£500 in tenths of pence (estimated for 2 people, 1 day)
		quote = quote + installationPrice * (1+profit);
		
		return quote;
		
		
	}

}
