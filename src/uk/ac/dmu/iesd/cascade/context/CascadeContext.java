package uk.ac.dmu.iesd.cascade.context;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;

import java.io.File;

import repast.simphony.context.*;
import repast.simphony.engine.schedule.*;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.space.graph.Network;
import repast.simphony.space.projection.*;
import repast.simphony.ui.widget.SnapshotTaker;
import repast.simphony.util.collections.Pair;
import repast.simphony.visualization.IDisplay;
import uk.ac.dmu.iesd.cascade.Consts;
import repast.simphony.engine.environment.GUIRegistry;
import repast.simphony.engine.environment.GUIRegistryType;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;

/**
 * <em>CascadeContext</em> is the main context for the <em>Cascade</em> framework.
 * Context can be seen as container or a virtual environment that is populated 
 * with agents, proto-agents or other actors/components, etc.
 * The <code>CascadeContext</code> holds internal states (information) that could be 
 * useful for population (e.g. agents) about the world (environment) in which they live in.
 * At this moment, for instance, the information about the weather is provided to agents
 * through the public interface implemented by this class. These can be seen a global
 * information/behaviour. 
 *   
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.2 $ $Date: 2011/05/12 11:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.1 - File Initial scenario creator (??)
 * 1.2 - Class name has been changed
 *       Boolean verbose variable has been added
 *       Name of some variables changed
 *       Babak 
 */
public class CascadeContext extends DefaultContext{
	

	/*
	 * Context parameters
	 * 
	 * This is place to add any context-specific environment variables
	 * Used for (e.g.) weather, system base demand etc.
	 * things stored here should be of the type that are loaded once only, at simulation
	 * start and stored for the entire duration of the simulation.
	 * 
	 */

	// Note at the moment, no geographical info is needed to read the weather
	// this is because weather is a flat file and not spatially differentiated

	int weatherDataLength; // length of arrays - note that it is a condition that each row of the input file
	// represents one time step, but the model is agnostic to what time period each
	// tick represents.
	float[] insolationArray; //Note this is an integrated value in Wh per metre squared
	float[] windSpeedArray;// instantaneous value
	float[] windDirectionArray; // Direction in degrees from North.  May not be needed as yet, but useful to have potentially
	float[] airTemperatureArray; // instantaneous value
	float[] systemPriceSignalDataArray;
	int systemPriceSignalDataLength;
	public static boolean verbose = false;  // use to produce verbose output based on user choice (default is false)
	protected static boolean chartSnapshotOn = false;  // use
	protected int ticksPerDay;
	protected int chartSnapshotInterval;
	
	private Network socialNetwork;
	private Network economicNetwork;
	
	protected GregorianCalendar currentDate;
	
	SnapshotTaker snapshotTaker1;
	Collection chartCompCollection;
	ArrayList snapshotTakerArrList;
	String fileChartFormatExt = ".png";


	
	/**
	 * This method return the social network 
	 * @return <tt>socialNetwork</tt> associated to the context
	 * @see #setSocialNetwork
	 */
	public Network getSocialNetwork(){
		return this.socialNetwork;
	}
	
	/**
	 * Sets the social network associated to this context 
	 * @param n the social network
	 * @see #getSocialNetwork
	 */
	public void setSocialNetwork(Network n){
		this.socialNetwork = n;
	}
	
	
	/**
	 * This method return the social network 
	 * @return <tt>economicNetwork</tt> associated to the context
	 * @see #setEconomicNetwork
	 */
	public Network getEconomicNetwork(){
		return this.economicNetwork;
	}
	
	/**
	 * Sets the economic network associated to this context 
	 * @param n the economic network
	 * @see #getEconomicNetwork
	 */
	public void setEconomicNetwork(Network n){
		this.economicNetwork = n;
	}

	
	/**
	 * This method return the number of <tt> tickPerDay </tt>
	 * @return <code>tickPerDay</code>
	 */
	public int getTickPerDay() {
		return this.ticksPerDay;
	}
	
	public void setTickPerDay(int tick) {
		this.ticksPerDay = tick;
	}
	
	public void setChartSnapshotInterval(int interval) {
		this.chartSnapshotInterval = interval;
	}
	
	public int getChartSnapshotInterval() {
		return this.chartSnapshotInterval;
	}
	
	
	/**
	 * This method returns the current timeslot during a day (usually divided to 48 timeslot).
	 * @return urrent timeslot during a day 
	 */
	public int getCurrentTimeslotForDay() {
		return (int) RepastEssentials.GetTickCount();
	}
	
	
	/**
	 * This method returns the elapse of time in number of days.
	 * It depends on how a day is initially defined. If a day is divided up to 48 timeslots, 
	 * then the second day starts at timeslot 49. 
	 * However, in order to have it usefully workable with arrays, the first day is returned as 0, second day as 1 and so forth.
	 * @return the elapsed time in terms of number of day, starting from 0
	 */
	public int getCountDay() {
		return (int) RepastEssentials.GetTickCount()/this.getTickPerDay();
	}
	
	/**
	 * This method determines whether a day has changed since a given reference point.
	 * @param sinceDay a day reference from which the elapse of day is tested.
	 *  @return <code>true</code> if the day has changed since <tt>sinceDay</tt>
     *          <code>false</code> otherwise
	 * see {@link #getCountDay()}
	 */
	public boolean isDayChangedSince(int sinceDay) {
		boolean dayChanged = false;
		int daysSoFar = getCountDay();
		int daysSinceStart = daysSoFar - sinceDay;
		if (daysSinceStart >= 1)
			dayChanged = true;
		return dayChanged;
	}
	
	/**
	 * This method determines whether a given timeslot is the beginning of the day
	 * It is built rather for readability than its functionality.
	 * @param timeslot a timeslot of the day to be tested whether it indicates the beginning of the day
	 * @return <code>true</code> if given timeslot corresponds to the beginning of the day, <code>false</code> otherwise
	 */
	public boolean isBeginningOfDay(int timeslot) {
		if (timeslot == 0)
			return true;
		else return false;	
	}
	
	/**
	 * This method determines whether a given timeslot is the end of the day
	 * It is built rather for readability than its functionality.
	 * @param timeslot a timeslot of the day to be tested whether it indicates the end of the day
	 * @return <code>true</code> if given timeslot corresponds to the end of the day, <code>false</code> otherwise
	 */
	public boolean isEndOfDay(int timeslot) {
		if (timeslot == this.ticksPerDay-1)
			return true;
		else return false;	
	}
	
	
	/**
	 * This method determines whether it is the beginning of the day
	 * @return <code>true</code> if it is the beginning of the day, <code>false</code> otherwise
	 */
	public boolean isBeginningOfDay() {
		double time = RepastEssentials.GetTickCount();
		int timeOfDay = (int) (time % getTickPerDay());
		if (timeOfDay == 0)
			return true;
		else return false;	
	}
	
	/*
	 * Accesor methods to context variables
	 */
	/**
	 * @param time - the time in ticks for which to get the insolation
	 * @return the insolation at the time (in ticks) passed in
	 */
	public float getInsolation(int time)
	{
		return insolationArray[time % weatherDataLength];
	}

	/**
	 * @param time - the time in ticks for which to get the wind speed
	 * @return the wind speed at the time (in ticks) passed in
	 */
	public float getWindSpeed(int time)
	{
		return windSpeedArray[time % weatherDataLength];
	}
	
	/**
	 * @param time - the time in ticks for which to get the air temperature
	 * @return the air temperature at the time (in ticks) passed in
	 */
	public float getAirTemperature(int time)
	{
		return airTemperatureArray[time % weatherDataLength];
	}
	/**
	 * @param time - the time in ticks for which to get the insolation
	 * @return the insolation at the time (in ticks) passed in
	 */
	public float[] getInsolation(int time, int length)
	{
		int start = time % weatherDataLength;
		return Arrays.copyOfRange(insolationArray, start, start + length);
	
	}

	/**
	 * @param time - the time in ticks for which to get the wind speed
	 * @return the wind speed at the time (in ticks) passed in
	 */
	public float[] getWindSpeed(int time, int length)
	{
		int start = time % weatherDataLength;
		return Arrays.copyOfRange(windSpeedArray, start, start + length);
	
	}
	/**
	 * @param time - the time in ticks for which to get the air temperature
	 * @return the air temperature at the time (in ticks) passed in
	 */
	public float[] getAirTemperature(int time, int length)
	{
		int start = time % weatherDataLength;
		return Arrays.copyOfRange(airTemperatureArray, start, start + length);
	}
	
	
	/**
	 * @return the weatherDataLength
	 */
	public int getWeatherDataLength() {
		return weatherDataLength;
	}

	/**
	 * @param weatherDataLength the weatherDataLength to set
	 */
	public void setWeatherDataLength(int weatherDataLength) {
		this.weatherDataLength = weatherDataLength;
	}

	/**
	 * @return the insolation
	 */
	public float[] getInsolation() {
		return insolationArray;
	}

	/**
	 * @param insolation the insolation to set
	 */
	public void setInsolation(float[] insolation) {
		this.insolationArray = insolation;
	}

	/**
	 * @return the windSpeed
	 */
	public float[] getWindSpeed() {
		return windSpeedArray;
	}

	/**
	 * @param windSpeed the windSpeed to set
	 */
	public void setWindSpeed(float[] windSpeed) {
		this.windSpeedArray = windSpeed;
	}

	/**
	 * @return the windDirection
	 */
	public float[] getWindDirection() {
		return windDirectionArray;
	}

	/**
	 * @param windDirection the windDirection to set
	 */
	public void setWindDirection(float[] windDirection) {
		this.windDirectionArray = windDirection;
	}

	/**
	 * @return the airTemperature
	 */
	public float[] getAirTemperature() {
		return airTemperatureArray;
	}

	/**
	 * @param airTemperature the airTemperature to set
	 */
	public void setAirTemperature(float[] airTemperature) {
		this.airTemperatureArray = airTemperature;
	}

	/**
	 * @return the systemPriceSignalDataLength
	 */
	public int getSystemPriceSignalDataLength() {
		return systemPriceSignalDataLength;
	}

	/**
	 * @param systemPriceSignalDataLength the systemPriceSignalDataLength to set
	 */
	public void setSystemPriceSignalDataLength(int systemPriceSignalDataLength) {
		this.systemPriceSignalDataLength = systemPriceSignalDataLength;
	}

	/**
	 * @return the systemPriceSignalData
	 */
	public float[] getSystemPriceSignalData() {
		return systemPriceSignalDataArray;
	}

	/**
	 * @param systemPriceSignalData the systemPriceSignalData to set
	 */
	public void setSystemPriceSignalData(float[] systemPriceSignalData) {
		this.systemPriceSignalDataArray = systemPriceSignalData;
	}

	/*
	 * Have a nice toString() method to give good
	 * debug info
	 */
	public String toString() {
		String description;
		StringBuilder myDesc = new StringBuilder();
		myDesc.append("Instance of Cascade Context, hashcode = ");
		myDesc.append(this.hashCode());
		myDesc.append("\n contains arrays:");
		myDesc.append("\n insolation of length " + insolationArray.length);
		myDesc.append("\n windSpeed of length " + windSpeedArray.length);
		myDesc.append("\n airTemp of length " + airTemperatureArray.length);
		myDesc.append("\n and baseDemand of length " + systemPriceSignalDataArray.length);
		description = myDesc.toString();		
		return description;
	}
	
	private String getFileNameForChart(int chartNb) {
		String chartName;

		switch (chartNb) {
		 case 0:  chartName = "chart1_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 case 1:  chartName = "chart2_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 case 2:  chartName = "chart3_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 case 3:  chartName = "chart4_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 case 4:  chartName = "chart5_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 case 5:  chartName = "chart6_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 case 6:  chartName = "chart7_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 case 7:  chartName = "chart8_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 case 8:  chartName = "chart9_r"+getCurrentTimeslotForDay()+fileChartFormatExt;   break;
		 default: chartName = "chartDefaultName_"; break;
		}

		return chartName;	
	}

	public void takeSnapshot() {

		Iterator<SnapshotTaker> snapshotTakerIter = snapshotTakerArrList.iterator();
		//String path =System.getProperty("user.dir");
		//String path = new java.io.File(".").getCanonicalPath();
		try {
			for (int i=0; i<snapshotTakerArrList.size();i++) {
				SnapshotTaker snapshotTaker = (SnapshotTaker)snapshotTakerArrList.get(i);
				File file = new File(getFileNameForChart(i));
				snapshotTaker.save(file, "png");
			}

		} catch (IOException e) {
			// Print out the exception that occurred
			System.out.println("CascadeContext: Unable to takeSnapshot "+e.getMessage());
		}
	}

	public void setChartCompCollection(Collection c) {
		chartCompCollection=c;
		Iterator<JComponent> compIter= chartCompCollection.iterator();
		snapshotTakerArrList = new ArrayList();
		while ( compIter.hasNext() ){
			JComponent chartComp = (JComponent) compIter.next();
			SnapshotTaker snapshotTaker = new SnapshotTaker(chartComp);
			snapshotTakerArrList.add(snapshotTaker);
		}
	}
	
	public void buildCustomizedSchedules() {
		
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		//ScheduleParameters params = ScheduleParameters.createOneTime(1);
		ScheduleParameters params = ScheduleParameters.createRepeating(0, getChartSnapshotInterval());
		schedule.schedule(params, this, "takeSnapshot"); 
		
	}
	
	
	
	/******************
	 * This method steps the model's internal gregorian calendar on each model tick
	 *  
	 * Input variables: none
	 * 
	 ******************/
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void calendarStep() {
		currentDate.add(GregorianCalendar.MINUTE, Consts.MINUTES_PER_DAY / ticksPerDay);
	}

	/**
     * Constructs the cascade context 
     * 
     */
	public CascadeContext(Context context)
	{
		super(context.getId(), context.getTypeID());
		if (verbose)
			System.out.println("CascadeContext created with context " + context.getId() + " and type " + context.getTypeID());

		Iterator<Projection<?>> projIterator = context.getProjections().iterator();

		while (projIterator.hasNext()) {
			Projection proj = projIterator.next();
			this.addProjection(proj);
			if (verbose)
				System.out.println("Added projection: "+ proj.getName());
		}

		this.setId(context.getId());
		this.setTypeID(context.getTypeID());

	
		//tttttttttttttt   Babak test ttttttttttttttttttttt
		RunState runState = RunState.getInstance();
		GUIRegistry guiRegis = runState.getGUIRegistry();
		//List<IDisplay> listOfDisplays =  guiRegis.getDisplays();		
		//System.out.println("list of displays size: "+listOfDisplays.size());
		//tttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt

		// ------------Custom global schedule action --------------------

		
		// ----------------------------------------------------------------

	}
}
