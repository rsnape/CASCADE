package uk.ac.dmu.iesd.cascade.context;

//import javax.media.jai.WarpAffine;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.apache.commons.collections.IteratorUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;

import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Poisson;
import cern.jet.random.Uniform;

import repast.simphony.context.Context;
import repast.simphony.context.ContextEvent;
import repast.simphony.context.ContextEvent.EventType;
import repast.simphony.context.ContextListener;
import repast.simphony.context.DefaultContext;
import repast.simphony.engine.environment.GUIRegistry;
import repast.simphony.engine.environment.GUIRegistryType;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.query.PropertyEquals;
import repast.simphony.query.Query;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.space.projection.Projection;
import repast.simphony.util.collections.IndexedIterable;
import repast.simphony.visualization.DefaultDisplayData;
import repast.simphony.visualization.DisplayData;
import repast.simphony.visualization.IDisplay;
import repast.simphony.visualization.gis.DisplayGIS;
import repast.simphony.visualizationOGL2D.DisplayOGL2D;
import repast.simphony.visualizationOGL2D.StyledDisplayLayerOGL2D;
//import uk.ac.dmu.iesd.cascade.styles.HouseholdTwoDStyle;
import uk.ac.dmu.iesd.cascade.agents.prosumers.Household;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

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

	DateFormat ukDateParser = new SimpleDateFormat("dd/mm/yyyy");
	public Logger logger;
	DisplayGIS styledDisplay;
	SortedMap<Integer, Integer> PVFITs; // Holds PV feed in tarriffs in system
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
		simTime.add(GregorianCalendar.MINUTE, 30);
	}

	@ScheduledMethod(start = (48 * 365 * 4), interval = 0, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void endSim() {
		for (Object thisH : this.getObjects(Household.class)) {
			Household h = (Household) thisH;
			this.logger.trace(h.getAgentName() + " has had " + h.getNumThoughts());
		}
		this.logger = null; // remove reference to logger, so context can be gc'd
		RepastEssentials.EndSimulationRun();
	}

	public Date getDateTime() {
		return simTime.getTime();
	}
	
	@ScheduledMethod(start = 0, interval = 0, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void firstCut() {
		this.PVFITs = new TreeMap<Integer,Integer>();
		this.PVFITs.put(4, 210);
		this.PVFITs.put(10, 160);
		this.PVFITs.put(100, 130);
		this.PVFITs.put(5000,130);
	}
	
	@ScheduledMethod(start = 0, interval = 0, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void secondCut() {
		this.PVFITs = new TreeMap<Integer,Integer>();
		this.PVFITs.put(4, 160);
		this.PVFITs.put(10, 140);
		this.PVFITs.put(100, 100);
		this.PVFITs.put(5000,100);
	}
	
    public Date getTarriffAvailableUntil()
    {
    	Date returnDate = new Date();    	
    	Date now = this.getDateTime();
    	if (now.before(parseUKDate("31/10/2011")))
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
    	}
    	
    	return returnDate;
    }

    private Date parseUKDate(String d)
    {
    	Date returnDate = null;
    	try {
    		returnDate = ukDateParser.parse(d);
		} catch (ParseException e) {
			this.logger.warn("Asked to parse "+d+" which didn't parse as a uk format date (dd/mm/yyyy)");
		}
		return returnDate;
    }
    
	public Integer getPVTariff(double cap) {
    	Date now = this.getDateTime();
    	
    	this.logger.trace("Getting PV tariff for capacity" + cap + " on date "+ukDateParser.format(now));
    	
    	if (now.before(parseUKDate("01/04/2010")))
    	{
			return 0;
    	}   	
		
		Iterator<Integer> iterator = this.PVFITs.keySet().iterator();
		while (iterator.hasNext()) {
			Integer key = iterator.next();
			if (key >= cap) {
				this.logger.trace("Returning value for up to " + key);
				return this.PVFITs.get(key);
			}
		}

		this.logger.trace("Returning null");
		// No tariff exists for installation of this size
		return null;

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
		if (logger.exists("AdoptionLogger")!=null)
		{
			logger = Logger.getLogger("AdoptionLogger");
		}
		else
		{
			logger = Logger.getLogger("AdoptionLogger");
	
		logger.setLevel(Level.INFO); //Set this to TRACE for full log files
		ConsoleAppender console = new ConsoleAppender(new AdoptionLogLayout());
		console.setName("ConsoleOutput");
		console.setThreshold(Level.DEBUG);
		logger.addAppender(console);
		
		FileAppender traceFile;
		try {
			String parsedDate = (new SimpleDateFormat("yyyy.MMM.dd.HH_mm_ss_z")).format(new Date());
			traceFile = new FileAppender(new AdoptionLogLayout(),RepastEssentials.GetParameter("RootDir").toString() + "/output/myTrace"+parsedDate+".log");
			traceFile.setName("traceFileOutput");
			traceFile.setThreshold(Level.TRACE);
			logger.addAppender(traceFile);
		} catch (IOException e1) {
			this.logger.warn("Creating file appender for logger failed");
			e1.printStackTrace();
		}
		}

		logger.debug("Adoption Context instantiated and logger configured");


		simStartDate = parseUKDate(date);
		simTime.setTime(simStartDate);

		this.addContextListener(new CountUpdater());
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

}
