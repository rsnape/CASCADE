package uk.ac.dmu.iesd.cascade.context;

//import javax.media.jai.WarpAffine;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

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

public class AdoptionContext extends CascadeContext
{

	public static final double FIT_EXPORT_TARIFF = 45;
	public Poisson nextThoughtGenerator = RandomHelper.createPoisson(100.0); // Mean
																				// length
																				// of
																				// time
																				// between
																				// thoughts
																				// (days)
	private int msecsPerTick;

	/**
	 * @return the msecsPerTick
	 */
	public int getMsecsPerTick()
	{
		return this.msecsPerTick;
	}

	/**
	 * @param msecsPerTick
	 *            the msecsPerTick to set
	 */
	public void setMsecsPerTick(int msecsPerTick)
	{
		this.msecsPerTick = msecsPerTick;
	}

	private class CountUpdater implements ContextListener
	{

		@Override
		public void eventOccured(ContextEvent ev)
		{
			if (ev.getType().equals(EventType.AGENT_ADDED) || ev.getType().equals(EventType.AGENT_REMOVED))
			{
				AdoptionContext.this.logger.trace("Context listener called for agent add / remove");
				String className = ev.getTarget().getClass().getName();
				if (AdoptionContext.this.agentCounts.containsKey(className))
				{
					int oldC = AdoptionContext.this.agentCounts.get(className);

					AdoptionContext.this.agentCounts.remove(className);
					AdoptionContext.this.agentCounts.put(className, oldC + 1);
				}
			}
		}
	}

	DateFormat ukDateParser = new SimpleDateFormat("dd/MM/yyyy");
	DisplayGIS styledDisplay;
	DatedTimeSeries<TreeMap<Integer, Integer>> PVFITs = new DatedTimeSeries<TreeMap<Integer, Integer>>(); // Holds
																											// PV
																											// feed
																											// in
																											// tarriffs
																											// in
																											// system
	// capacity vs. tenths of
	// pence / eurocents per kWh

	DatedTimeSeries<Integer> PVCosts = new DatedTimeSeries<Integer>(); // Holds
																		// PV
																		// feed
																		// in
																		// tarriffs
																		// in
																		// system
	// capacity vs. tenths of
	// pence / eurocents per kWh

	WeakHashMap<String, Integer> agentCounts = new WeakHashMap<String, Integer>();
	Calendar simTime = new GregorianCalendar();
	public Date simStartDate;

	/******************
	 * This method steps the model's internal gregorian calendar on each model
	 * tick
	 * 
	 * Input variables: none
	 * 
	 ******************/
	@Override
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void calendarStep()
	{
		this.logger.trace("Incrementing simulation date and time");
		this.simTime.add(Calendar.MINUTE, this.getMsecsPerTick() / (1000 * 60));
		this.logger.trace("Advancing date to " + this.ukDateParser.format(this.simTime.getTime()));
	}

	@ScheduledMethod(start = (48 * 365 * 7), interval = 0, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void endSim()
	{
		for (Object thisH : this.getObjects(Household.class))
		{
			Household h = (Household) thisH;
			this.logger.trace(h.getAgentName() + " has had " + h.getNumThoughts());
		}
		RepastEssentials.EndSimulationRun();
		this.logger = null; // remove reference to logger, so context can be
							// gc'd

	}

	@Override
	public Date getDateTime()
	{
		return this.simTime.getTime();
	}

	public Date getTarriffAvailableUntil()
	{
		Date retVal = new Date();
		Date now = this.getDateTime();

		retVal = this.PVFITs.getFirstKeyFollowing(now);
		this.logger.debug("Finding key following " + this.ukDateParser.format(now));

		if (retVal == null)
		{
			this.logger.debug("return last valid date as appears " + this.ukDateParser.format(now) + " is past last key");
			retVal = this.PVFITs.getLastValidDate();
		}

		this.logger.debug("Returning  = " + this.ukDateParser.format(retVal));

		return retVal;
	}

	/**
	 * Return a date object for an input string in the UK date format
	 * (dd/MM/yyyy)
	 * 
	 * @param d
	 * @return
	 */
	Date parseUKDate(String d)
	{
		Date returnDate = null;
		try
		{
			returnDate = this.ukDateParser.parse(d);
		}
		catch (ParseException e)
		{
			this.logger.warn("Asked to parse " + d + " which didn't parse as a uk format date (dd/MM/yyyy)");
		}
		return returnDate;
	}

	public Integer getPVTariff(double cap)
	{
		Date now = this.getDateTime();

		this.logger.trace("Getting PV tariff for capacity" + cap + " on date " + this.ukDateParser.format(now));

		if (now.before(this.PVFITs.getFirstDate()))
		{
			return 0;
		}

		SortedMap<Integer, Integer> tariffNow = this.PVFITs.getValue(now);

		tariffNow = tariffNow.tailMap((int) (cap + 0.5));
		Integer retVal;
		if (tariffNow.size() == 0)
		{
			retVal = 0;
		}
		else
		{
			retVal = tariffNow.get(tariffNow.firstKey());
		}

		this.logger.trace("returning fit for capacity " + cap + " = " + retVal);
		return retVal;
	}

	int getAgentCount(Class clazz)
	{
		this.logger.trace("Get Agent count for " + clazz.getName() + " called.");
		String className = clazz.getName();
		if (this.agentCounts.containsKey(className))
		{
			this.logger.trace("Returning cached value");
			return this.agentCounts.get(className);
		}

		this.logger.trace("Counting and adding to cache");
		int count = this.getObjects(clazz).size();
		this.agentCounts.put(className, count);
		return count;
	}

	/**
	 * Must be a quicker way to count agents with a property than this...
	 * 
	 * Very slow
	 * 
	 * @return
	 */
	public double getAdoptionPercentage()
	{
		this.logger.trace("Get percentage called at " + System.nanoTime());
		Query<Household> adoptionQuery = new PropertyEquals<Household>(this, "hasPV", true);
		Iterable<Household> agentsWithPV = adoptionQuery.query();
		double ret = IterableUtils.count(agentsWithPV);
		this.logger.trace("Got count at " + System.nanoTime());

		this.logger.trace("Agents with PV count = " + ret);
		this.logger.trace("Total = " + this.getAgentCount(Household.class));
		ret *= 100;
		ret /= this.getAgentCount(Household.class);
		this.logger.trace("percentage = " + ret);
		return ret;
	}

	public AdoptionContext(Context context)
	{
		this(context, "01/01/2010");
	}

	public AdoptionContext(Context context, String date)
	{
		super(context);
		this.simStartDate = this.parseUKDate(date);
		this.simTime.setTime(this.simStartDate);

		// this.addContextListener(new CountUpdater());
	}

	public int dateToTick(Date d)
	{
		long msecDiff = d.getTime() - this.simStartDate.getTime();
		this.logger.debug("simStartDate = " + this.ukDateParser.format(this.simStartDate) + " : date to test = "
				+ this.ukDateParser.format(d) + " : msecsPerTick = " + this.msecsPerTick + " difference being " + msecDiff);
		return (int) (msecDiff / this.msecsPerTick);
	}

	public Date tickToDate(int t)
	{
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(this.simStartDate);
		cal1.add(Calendar.MINUTE, t * Consts.MINUTES_PER_DAY / this.ticksPerDay);
		return cal1.getTime();
	}

	public int getTickcount()
	{
		return (int) RepastEssentials.GetTickCount();
	}

	public int getRandomSeed()
	{
		return RandomHelper.getSeed();
	}

	/**
	 * @param ticksPerDay
	 */
	@Override
	public void setNbOfTickPerDay(int ticksPerDay)
	{
		this.ticksPerDay = ticksPerDay;
		this.setMsecsPerTick(Consts.MSECS_PER_DAY / this.ticksPerDay);
	}

	/**
	 * @return
	 */
	@Override
	public int getNbOfTickPerDay()
	{
		return this.ticksPerDay;
	}

	/**
	 * Sets the economic network associated to this context
	 * 
	 * @param n
	 *            the economic network
	 * @see #getEconomicNetwork
	 */
	@Override
	public void setEconomicNetwork(Network<?> n)
	{
		this.economicNetwork = n;
	}

	/**
	 * This method returns the tick time. It is a wrapper around
	 * RepastEssential.GgetTickCount method, which returns the tick count as
	 * integer.
	 * 
	 * @return current tick count of the model
	 */
	@Override
	public int getTickCount()
	{
		return (int) RepastEssentials.GetTickCount();
	}

	@Override
	public int getTimeslotOfDay()
	{
		return (int) RepastEssentials.GetTickCount() % this.ticksPerDay;
	}

	public int getPricePerkWp()
	{
		Date now = this.getDateTime();

		this.logger.trace("Getting PV price per kWh on date " + this.ukDateParser.format(now));

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
	public double getPVSystemPrice(double potentialPVCapacity)
	{
		double quote = this.getPricePerkWp() * potentialPVCapacity;
		double profit = RandomHelper.nextDoubleFromTo(Consts.INSTALLER_MIN_PROFIT, Consts.INSTALLER_MIN_PROFIT);
		double installationPrice = 500000; // £500 in tenths of pence (estimated
											// for 2 people, 1 day)
		quote = quote + installationPrice * (1 + profit);

		return quote;

	}

}
