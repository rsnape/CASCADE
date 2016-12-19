package uk.ac.dmu.iesd.cascade.context;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.swing.JComponent;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.IllegalParameterException;
import repast.simphony.space.graph.Network;
import repast.simphony.space.projection.Projection;
import repast.simphony.ui.widget.SnapshotTaker;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.market.IBMTrader;
import uk.ac.dmu.iesd.cascade.market.IPxTrader;
import uk.ac.dmu.iesd.cascade.market.ITrader;
import uk.ac.dmu.iesd.cascade.util.ChartUtils;
import cern.jet.random.Binomial;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Poisson;
import cern.jet.random.Uniform;

/**
 * <em>CascadeContext</em> is the main context for the <em>Cascade</em>
 * framework. Context can be seen as container or a virtual environment that is
 * populated with agents, proto-agents or other actors/components, etc. The
 * <code>CascadeContext</code> holds internal states (information) that could be
 * useful for population (e.g. agents) about the world (environment) in which
 * they live in. At this moment, for instance, the information about the weather
 * is provided to agents through the public interface implemented by this class.
 * These can be seen a global information/behaviour.
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.2 $ $Date: 2011/05/12 11:00:00 $
 * 
 *          Version history (for intermediate steps see Git repository history
 * 
 *          1.1 - File Initial scenario creator (??) 1.2 - Class name has been
 *          changed Boolean verbose variable has been added Name of some
 *          variables changed Babak 1.3 - float values changed to double
 * 
 * 
 */
public class CascadeContext extends DefaultContext
{

	/*
	 * Context parameters
	 * 
	 * This is place to add any context-specific environment variables Used for
	 * (e.g.) weather, system base demand etc. things stored here should be of
	 * the type that are loaded once only, at simulation start and stored for
	 * the entire duration of the simulation.
	 */

	public Logger logger;

	// Note at the moment, no geographical info is needed to read the weather
	// this is because weather is a flat file and not spatially differentiated

	int weatherDataLength; // length of arrays - note that it is a condition
							// that each row of the input file
							// represents one time step, but the model is
							// agnostic to what time period each tick
							// represents.
	double[] insolationArray; // Note this is an integrated value in Wh per
								// metre squared
	double[] windSpeedArray;// instantaneous value
	double[] windDirectionArray; // Direction in degrees from North. May not be
									// needed as yet, but useful to have
									// potentially
	double[] airTemperatureArray; // instantaneous value
	double[] airDensityArray; // instantaneous value
	double[] systemPriceSignalDataArray;
	int systemPriceSignalDataLength;

	int totalNbOfProsumers;
	int randomSeed;

	public static boolean verbose = false; // use to produce verbose output
											// based on user choice (default is
											// false)
	protected static boolean chartSnapshotOn = false; // use
	public int ticksPerDay;
	protected int chartSnapshotInterval;

	public int signalMode = -1;

	private Network<?> socialNetwork;
	protected Network<?> economicNetwork;
	@SuppressWarnings("unused")
	private Network<?> windNetwork;

	public GregorianCalendar simulationCalendar;

	SnapshotTaker snapshotTaker1;
	Collection<JComponent> chartCompCollection;
	ArrayList<SnapshotTaker> snapshotTakerArrList;
	public EmpiricalWalker drawOffGenerator;
	public EmpiricalWalker occupancyGenerator;
	public EmpiricalWalker vehicleArrivalGenerator;
	public Poisson journeyLengthGenerator;
	public Normal waterUsageGenerator;

	public Normal buildingLossRateGenerator;
	public Normal thermalMassGenerator;

	public Uniform coldAndWetApplTimeslotDelayRandDist;
	public EmpiricalWalker wetApplProbDistGenerator;

	public Binomial hhProsumerElasticityTest;

	/**
	 * This method return the social network
	 * 
	 * @return <tt>socialNetwork</tt> associated to the context
	 * @see #setSocialNetwork
	 */
	public Network<?> getSocialNetwork()
	{
		return this.socialNetwork;
	}

	/**
	 * Sets the social network associated to this context
	 * 
	 * @param n
	 *            the social network
	 * @see #getSocialNetwork
	 */
	public void setSocialNetwork(Network<?> n)
	{
		this.socialNetwork = n;
	}

	/**
	 * This method return the economic network
	 * 
	 * @return <tt>economicNetwork</tt> associated to the context
	 * @see #setEconomicNetwork
	 */
	public Network getEconomicNetwork()
	{
		return this.economicNetwork;
	}

	/**
	 * Sets the economic network associated to this context
	 * 
	 * @param n
	 *            the economic network
	 * @see #getEconomicNetwork
	 */
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
	public int getTickCount()
	{
		return (int) RepastEssentials.GetTickCount();
	}

	public int getTimeslotOfDay()
	{
		return (int) RepastEssentials.GetTickCount() % this.ticksPerDay;
	}

	/**
	 * This method return the number of <tt> tickPerDay </tt>
	 * 
	 * @return <code>tickPerDay</code>
	 */
	public int getNbOfTickPerDay()
	{
		return this.ticksPerDay;
	}

	public void setNbOfTickPerDay(int tick)
	{
		this.ticksPerDay = tick;

	}

	/*
	 * public int getLengthOfDemandProfiles() { return
	 * this.lengthOfDemandProfiles; }
	 */

	public void setChartSnapshotInterval(int interval)
	{
		this.chartSnapshotInterval = interval;
	}

	public int getChartSnapshotInterval()
	{
		return this.chartSnapshotInterval;
	}

	public void setTotalNbOfProsumers(int nbOfPros)
	{
		this.totalNbOfProsumers = nbOfPros;
	}

	public int getTotalNbOfProsumers()
	{
		return this.totalNbOfProsumers;
	}

	public void setRandomSeedValue(int rs)
	{
		this.randomSeed = rs;
	}

	public int getRandomSeedValue()
	{
		return this.randomSeed;
	}

	/**
	 * This method returns the elapse of time in number of days. It depends on
	 * how a day is initially defined. If a day is divided up to 48 timeslots,
	 * then the second day starts at timeslot 49. However, in order to have it
	 * usefully workable with arrays, the first day is returned as 0, second day
	 * as 1 and so forth.
	 * 
	 * @return the elapsed time in terms of number of day, starting from 0
	 */
	public int getDayCount()
	{
		return (int) RepastEssentials.GetTickCount() / this.getNbOfTickPerDay();
	}

	/**
	 * This method determines whether a day has changed since a given reference
	 * point.
	 * 
	 * @param sinceDay
	 *            a day reference from which the elapse of day is tested.
	 * @return <code>true</code> if the day has changed since <tt>sinceDay</tt>
	 *         <code>false</code> otherwise see {@link #getDayCount()}
	 */
	public boolean isDayChangedSince(int sinceDay)
	{
		boolean dayChanged = false;
		int daysSoFar = this.getDayCount();
		int daysSinceStart = daysSoFar - sinceDay;
		if (daysSinceStart >= 1)
		{
			dayChanged = true;
		}
		return dayChanged;
	}

	/**
	 * This method determines whether a given timeslot is the beginning of the
	 * day It is built rather for readability than its functionality.
	 * 
	 * @param timeslot
	 *            a timeslot of the day to be tested whether it indicates the
	 *            beginning of the day
	 * @return <code>true</code> if given timeslot corresponds to the beginning
	 *         of the day, <code>false</code> otherwise
	 */
	public boolean isBeginningOfDay(int timeslot)
	{
		if (timeslot == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * This method determines whether a given timeslot is the end of the day It
	 * is built rather for readability than its functionality.
	 * 
	 * @param timeslot
	 *            a timeslot of the day to be tested whether it indicates the
	 *            end of the day
	 * @return <code>true</code> if given timeslot corresponds to the end of the
	 *         day, <code>false</code> otherwise
	 */
	public boolean isEndOfDay(int timeslot)
	{
		if (timeslot == this.ticksPerDay - 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * This method determines whether it is the beginning of the day
	 * 
	 * @return <code>true</code> if it is the beginning of the day,
	 *         <code>false</code> otherwise
	 */
	public boolean isBeginningOfDay()
	{
		double time = RepastEssentials.GetTickCount();
		int timeOfDay = (int) (time % this.getNbOfTickPerDay());
		if (timeOfDay == 0)
		{
			return true;
		}
		else
		{
			return false;
		}

	}

	/*
	 * Accesor methods to context variables
	 */
	/**
	 * @param time
	 *            - the time in ticks for which to get the insolation
	 * @return the insolation at the time (in ticks) passed in
	 */
	public double getInsolation(int time)
	{
		double retVal = this.insolationArray[time % this.weatherDataLength];
		return retVal;
	}

	/**
	 * @param time
	 *            - the time in ticks for which to get the wind speed
	 * @return the wind speed at the time (in ticks) passed in
	 */
	public double getWindSpeed(int time)
	{
		return this.windSpeedArray[time % this.weatherDataLength];
	}

	/**
	 * @param time
	 *            - the time in ticks for which to get the air temperature
	 * @return the air temperature at the time (in ticks) passed in
	 */
	public double getAirTemperature(int time)
	{
		return this.airTemperatureArray[time % this.weatherDataLength];
	}

	/**
	 * @param time
	 *            - the time in ticks for which to get the air density
	 * @return the air density at the time (in ticks) passed in
	 */
	public double getAirDensity(int time)
	{
		return this.airDensityArray[time % this.weatherDataLength];
	}

	/**
	 * @param time
	 *            - the time in ticks for which to get the insolation
	 * @return the insolation at the time (in ticks) passed in
	 */
	public double[] getInsolation(int time, int length)
	{
		int start = time % this.weatherDataLength;
		return Arrays.copyOfRange(this.insolationArray, start, start + length);

	}

	/**
	 * @param time
	 *            - the time in ticks for which to get the wind speed
	 * @return the wind speed at the time (in ticks) passed in
	 */
	public double[] getWindSpeed(int time, int length)
	{
		int start = time % this.weatherDataLength;
		return Arrays.copyOfRange(this.windSpeedArray, start, start + length);

	}

	/**
	 * @param time
	 *            - the time in ticks for which to get the air temperature
	 * @return the air temperature at the time (in ticks) passed in
	 */
	public double[] getAirTemperature(int time, int length)
	{
		int start = time % this.weatherDataLength;
		return Arrays.copyOfRange(this.airTemperatureArray, start, start + length);
	}

	/**
	 * @param time
	 *            - the time in ticks for which to get the air density
	 * @return the air density at the time (in ticks) passed in
	 */
	public double[] getAirDensity(int time, int length)
	{
		int start = time % this.weatherDataLength;
		return Arrays.copyOfRange(this.airDensityArray, start, start + length);
	}

	/**
	 * @return the weatherDataLength
	 */
	public int getWeatherDataLength()
	{
		return this.weatherDataLength;
	}

	/**
	 * @param weatherDataLength
	 *            the weatherDataLength to set
	 */
	public void setWeatherDataLength(int weatherDataLength)
	{
		this.weatherDataLength = weatherDataLength;
	}

	/**
	 * @return the insolation
	 */
	public double[] getInsolation()
	{
		return this.insolationArray;
	}

	/**
	 * @param insolation
	 *            the insolation to set
	 */
	public void setInsolation(double[] insolation)
	{
		this.insolationArray = insolation;
	}

	/**
	 * @return the windSpeed
	 */
	public double[] getWindSpeed()
	{
		return this.windSpeedArray;
	}

	/**
	 * @param windSpeed
	 *            the windSpeed to set
	 */
	public void setWindSpeed(double[] windSpeed)
	{
		this.windSpeedArray = windSpeed;
	}

	/**
	 * @return the windDirection
	 */
	public double[] getWindDirection()
	{
		return this.windDirectionArray;
	}

	/**
	 * @param windDirection
	 *            the windDirection to set
	 */
	public void setWindDirection(double[] windDirection)
	{
		this.windDirectionArray = windDirection;
	}

	/**
	 * @return the airTemperature
	 */
	public double[] getAirTemperature()
	{
		return this.airTemperatureArray;
	}

	/**
	 * @param airTemperature
	 *            the airTemperature to set
	 */
	public void setAirTemperature(double[] airTemperature)
	{
		this.airTemperatureArray = airTemperature;
	}

	/**
	 * @return the systemPriceSignalDataLength
	 */
	public int getSystemPriceSignalDataLength()
	{
		return this.systemPriceSignalDataLength;
	}

	/**
	 * @param systemPriceSignalDataLength
	 *            the systemPriceSignalDataLength to set
	 */
	public void setSystemPriceSignalDataLength(int systemPriceSignalDataLength)
	{
		this.systemPriceSignalDataLength = systemPriceSignalDataLength;
	}

	/**
	 * @return the systemPriceSignalData
	 */
	public double[] getSystemPriceSignalData()
	{
		return this.systemPriceSignalDataArray;
	}

	/**
	 * @param systemPriceSignalData
	 *            the systemPriceSignalData to set
	 */
	public void setSystemPriceSignalData(double[] systemPriceSignalData)
	{
		this.systemPriceSignalDataArray = systemPriceSignalData;
	}

	/*
	 * Have a nice toString() method to give good debug info
	 */
	@Override
	public String toString()
	{
		String description;
		StringBuilder myDesc = new StringBuilder();
		myDesc.append("Instance of Cascade Context, hashcode = ");
		myDesc.append(this.hashCode());
		myDesc.append("\n contains arrays:");
		myDesc.append("\n insolation of length " + this.insolationArray.length);
		myDesc.append("\n windSpeed of length " + this.windSpeedArray.length);
		myDesc.append("\n airTemp of length " + this.airTemperatureArray.length);
		myDesc.append("\n and baseDemand of length " + this.systemPriceSignalDataArray.length);
		description = myDesc.toString();
		return description;
	}

	private String getFileNameForChart(int chartNb)
	{
		String chartName;

		switch (chartNb)
		{
		case 0:
			if (Consts.TAKE_SNAPSHOT_OF_CHART_0_Insol)
			{
				chartName = "chart0_Insol_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			}
			else
			{
				chartName = "";
			}
			break;
		case 1:
			if (Consts.TAKE_SNAPSHOT_OF_CHART_1_AirTemp)
			{
				chartName = "chart1_AirTemp_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			}
			else
			{
				chartName = "";
			}
			break;
		case 2:
			if (Consts.TAKE_SNAPSHOT_OF_CHART_2_WindSpeed)
			{
				chartName = "chart2_WindSpeed_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			}
			else
			{
				chartName = "";
			}
			break;
		case 3:
			if (Consts.TAKE_SNAPSHOT_OF_CHART_3_AggSumOfD)
			{
				chartName = "chart3_AggSumOfD_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			}
			else
			{
				chartName = "";
			}
			break;
		case 4:
			chartName = "chart4_SvsC_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			break;
		case 5:
			if (Consts.TAKE_SNAPSHOT_OF_CHART_5_SmartAdapt)
			{
				chartName = "chart5_SmartAdapt_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			}
			else
			{
				chartName = "";
			}
			break;
		case 6:
			chartName = "chart6_AggCost_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			break;
		case 7:
			chartName = "chart7_BvsD_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			break;
		case 8:
			if (Consts.TAKE_SNAPSHOT_OF_CHART_8_Market)
			{
				chartName = "chart8_Market_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			}
			else
			{
				chartName = "";
			}
			break;
		case 9:
			chartName = "chart9_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			break;
		case 10:
			chartName = "chart10_r" + this.getTickCount() + Consts.FILE_CHART_FORMAT_EXT;
			break;

		default:
			chartName = "chartDefaultName_" + Consts.FILE_CHART_FORMAT_EXT;
			;
			break;
		}

		return chartName;
	}

	public void takeSnapshot()
	{

		if (this.getDayCount() > Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE - 2)
		{

			try
			{
				for (int i = 0; i < this.snapshotTakerArrList.size(); i++)
				{
					SnapshotTaker snapshotTaker = this.snapshotTakerArrList.get(i);
					String fileName = this.getFileNameForChart(i);
					if (fileName != "")
					{
						if (this.logger.isTraceEnabled())
						{
							this.logger.trace("takeSnapshot: fileName is empty");
						}
						File file = new File(fileName);
						snapshotTaker.save(file, "png");
					}
				}

			}
			catch (IOException e)
			{
				// Print out the exception that occurred
				if (this.logger.isDebugEnabled())
				{
					this.logger.debug("CascadeContext: Unable to takeSnapshot " + e.getMessage());
				}
			}
		}

	}

	public void setChartCompCollection(Collection<JComponent> c)
	{
		this.chartCompCollection = c;
		if (CascadeContext.chartSnapshotOn)
		{
			this.snapshotTakerArrList = ChartUtils.buildChartSnapshotTakers(c);
			ChartUtils.buildChartSnapshotSchedule(this, this.getChartSnapshotInterval());
		}
	}

	public Collection<JComponent> getChartCompCollection()
	{
		return this.chartCompCollection;
	}

	/******************
	 * This method steps the model's internal gregorian calendar on each model
	 * tick
	 * 
	 * Input variables: none
	 * 
	 ******************/
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void calendarStep()
	{
		if (this.logger.isTraceEnabled())
		{
			this.logger.trace("calendarStep()");
		}
		this.simulationCalendar.add(Calendar.MINUTE, Consts.MINUTES_PER_DAY / this.ticksPerDay);
	}

	public Date getDateTime()
	{
		return this.simulationCalendar.getTime();
	}

	// ----------------

	private Network networkOfRegisteredPxTraders;
	private Network networkOfRegisteredBMTraders;
	private int gasHeatedPercentage;

	public boolean isFirstDay()
	{
		if (this.getDayCount() == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean isMarketFirstDay()
	{
		if ((this.getDayCount() - ((Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP) / 48)) == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean isSecondDay()
	{
		if (this.getDayCount() == 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean isMarketSecondDay()
	{
		if (this.getDayCount() == ((Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP) / 48) + 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public int getSettlementPeriod()
	{
		return this.getTimeslotOfDay();
	}

	public ArrayList<IPxTrader> getListOfPxTraders()
	{
		ArrayList<IPxTrader> aListOfPxTraders = new ArrayList<IPxTrader>();
		Iterable<IPxTrader> pxTraderIter = (this.getObjects(IPxTrader.class));
		for (IPxTrader pxTrader : pxTraderIter)
		{
			aListOfPxTraders.add(pxTrader);
		}

		return aListOfPxTraders;
	}

	public ArrayList<IBMTrader> getListOfBMTraders()
	{
		ArrayList<IBMTrader> aListOfBMTraders = new ArrayList<IBMTrader>();
		Iterable<IBMTrader> bmTraderIter = (this.getObjects(IBMTrader.class));
		for (IBMTrader bmTrader : bmTraderIter)
		{
			aListOfBMTraders.add(bmTrader);
		}

		return aListOfBMTraders;
	}

	public ArrayList<ITrader> getListOfTraders()
	{
		ArrayList<ITrader> aListOfTraders = new ArrayList<ITrader>();
		Iterable<ITrader> traderIter = (this.getObjects(ITrader.class));
		for (ITrader trader : traderIter)
		{
			aListOfTraders.add(trader);
		}

		return aListOfTraders;
	}

	/**
	 * @param percentageOfHHProsWithGas
	 */
	public void setGasPercentage(int percentageOfHHProsWithGas)
	{
		this.gasHeatedPercentage = percentageOfHHProsWithGas;
	}

	public int getGasPercentage()
	{
		return this.gasHeatedPercentage;
	}

	void initDedicatedLogger(String name)
	{
		String s_logLevel = "";
		try
		{
			s_logLevel = (String) RepastEssentials.GetParameter("loggingLevel");
		}
		catch (IllegalParameterException e)
		{
			s_logLevel = "INFO";
		}
		
		Level logLevel = Level.toLevel(s_logLevel);
		// set up a this.logger for the adoption context
		this.logger = Logger.getLogger(name);
		this.logger.removeAllAppenders();
		this.logger.setLevel(logLevel); // Set this to TRACE for full log files.
										// Can filter what is actually output
										// below
		this.logger.setAdditivity(false);

		ConsoleAppender console = new ConsoleAppender(new CascadeLogLayout());
		console.setName("ConsoleOutput");
		console.setThreshold(Level.INFO); // Never output more detail than
											// "INFO" to the console. May get
											// less,
											// if the main level above is set to
											// e.g. WARN, FATAL or OFF
		console.activateOptions(); // Needed or the appender appends everything
									// from the this.logger

		this.logger.addAppender(console);

		FileAppender traceFile;
		String parsedDate = (new SimpleDateFormat("yyyy.MMM.dd.HH_mm_ss_z")).format(new Date());
		traceFile = new FileAppender();
		traceFile.setLayout(new CascadeLogLayout());
		File logFile = new File("logs", "cascade_" + parsedDate + "_" + logLevel.toString() + ".log");
		traceFile.setFile(logFile.getAbsolutePath());
		// traceFile.setMaxFileSize("1024000");
		// traceFile.setMaxBackupIndex(0);
		traceFile.setName("traceFileOutput");
		traceFile.setThreshold(logLevel);
		traceFile.activateOptions(); // Needed or the appender appends
										// everything from the this.logger

		this.logger.addAppender(traceFile);

		this.logger.info("Cascade Context instantiated and this.logger configured");
		this.logger.info(this.logger);

		this.logger.setAdditivity(false);
	}

	void initPipeToNullLogger()
	{
		this.logger = Logger.getLogger("nullLog");
		this.logger.removeAllAppenders();
		this.logger.setLevel(Level.OFF);
		this.logger.setAdditivity(false);
	}

	/**
	 * Constructs the cascade context
	 * 
	 */
	public CascadeContext(Context<?> context)
	{
		super(context.getId(), context.getTypeID());
		this.initDedicatedLogger(Consts.CASCADE_LOGGER_NAME);

		if (this.logger.isTraceEnabled())
		{
			this.logger.trace("CascadeContext created with context " + context.getId() + " and type " + context.getTypeID());
		}

		Iterator<Projection<?>> projIterator = context.getProjections().iterator();

		while (projIterator.hasNext())
		{
			Projection<?> proj = projIterator.next();
			this.addProjection(proj);
			if (this.logger.isTraceEnabled())
			{
				this.logger.trace("CascadeContext: Added projection: " + proj.getName());
			}
		}

		this.setId(context.getId());
		this.setTypeID(context.getTypeID());
	}

	class CascadeLogLayout extends SimpleLayout
	{
		@Override
		public String format(LoggingEvent ev)
		{
			return "[Tick " + RepastEssentials.GetTickCount() + "; " + (ev.timeStamp - LoggingEvent.getStartTime()) + "] : "
					+ ev.getLevel().toString() + " - " + ev.getRenderedMessage() + "\n";
		}
	}

}
