package uk.ac.dmu.iesd.cascade.context;

import java.io.*;
import java.math.*;
import java.util.*;


import javax.measure.unit.*;
import org.jscience.mathematics.number.*;
import org.jscience.mathematics.vector.*;
import org.jscience.physics.amount.*;


import repast.simphony.adaptation.neural.*;
import repast.simphony.adaptation.regression.*;
import repast.simphony.context.*;
import repast.simphony.context.space.continuous.*;
import repast.simphony.context.space.gis.*;
import repast.simphony.context.space.graph.*;
import repast.simphony.context.space.grid.*;
import repast.simphony.dataLoader.*;
import repast.simphony.engine.environment.*;
import repast.simphony.engine.schedule.*;
import repast.simphony.engine.watcher.*;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.groovy.math.*;
import repast.simphony.integration.*;
import repast.simphony.matlab.link.*;
import repast.simphony.query.*;
import repast.simphony.query.space.continuous.*;
import repast.simphony.query.space.gis.*;
import repast.simphony.query.space.graph.*;
import repast.simphony.query.space.grid.*;
import repast.simphony.query.space.projection.*;
import repast.simphony.parameter.*;
import repast.simphony.random.*;
import repast.simphony.space.continuous.*;
import repast.simphony.space.gis.*;
import repast.simphony.space.graph.*;
import repast.simphony.space.grid.*;
import repast.simphony.space.projection.*;
import repast.simphony.ui.probe.*;
import repast.simphony.util.*;
import simphony.util.messages.*;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;

/**
 * @author J. Richard Snape
 * @version $Revision: 1.2 $ $Date: 2011/05/12 11:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.1 - File Initial scenario creator
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
	private int ticksPerDay;


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
	}
	
	
	@ScheduledMethod(start = 1)
	 public void step() {
	   	// Can put any "global" behavior here
		// Use with great caution
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

}
