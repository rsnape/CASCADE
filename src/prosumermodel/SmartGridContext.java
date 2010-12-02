/**
 * 
 */
package prosumermodel;

import java.io.*;
import java.math.*;
import java.util.*;

import prosumermodel.SmartGridConstants.*;

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
 * @version $Revision: 1.0 $ $Date: 2010/11/16 17:00:00 $
 */
public class SmartGridContext extends DefaultContext{
	
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
	float[] insolation; //Note this is an integrated value in Wh per metre squared
	float[] windSpeed;// instantaneous value
	float[] windDirection; // Direction in degrees from North.  May not be needed as yet, but useful to have potentially
	float[] airTemperature; // instantaneous value
	int systemPriceSignalDataLength;
	float[] systemPriceSignalData;
	
	@ScheduledMethod(start = 1)
	 public void step() {
	   	// Can put any "global" behavior here
		// Use with great caution
	 }
	
	/*
	 * Accesor methods to context variables
	 */
	/**
	 * @param time - the time in ticks for which to get the insolation
	 * @return the insolation at the time (in ticks) passed in
	 */
	float getInsolation(int time)
	{
		return insolation[time % weatherDataLength];
	}

	/**
	 * @param time - the time in ticks for which to get the wind speed
	 * @return the wind speed at the time (in ticks) passed in
	 */
	float getWindSpeed(int time)
	{
		return windSpeed[time % weatherDataLength];
	}
	
	/**
	 * @param time - the time in ticks for which to get the air temperature
	 * @return the air temperature at the time (in ticks) passed in
	 */
	float getAirTemperature(int time)
	{
		return airTemperature[time % weatherDataLength];
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
		return insolation;
	}

	/**
	 * @param insolation the insolation to set
	 */
	public void setInsolation(float[] insolation) {
		this.insolation = insolation;
	}

	/**
	 * @return the windSpeed
	 */
	public float[] getWindSpeed() {
		return windSpeed;
	}

	/**
	 * @param windSpeed the windSpeed to set
	 */
	public void setWindSpeed(float[] windSpeed) {
		this.windSpeed = windSpeed;
	}

	/**
	 * @return the windDirection
	 */
	public float[] getWindDirection() {
		return windDirection;
	}

	/**
	 * @param windDirection the windDirection to set
	 */
	public void setWindDirection(float[] windDirection) {
		this.windDirection = windDirection;
	}

	/**
	 * @return the airTemperature
	 */
	public float[] getAirTemperature() {
		return airTemperature;
	}

	/**
	 * @param airTemperature the airTemperature to set
	 */
	public void setAirTemperature(float[] airTemperature) {
		this.airTemperature = airTemperature;
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
		return systemPriceSignalData;
	}

	/**
	 * @param systemPriceSignalData the systemPriceSignalData to set
	 */
	public void setSystemPriceSignalData(float[] systemPriceSignalData) {
		this.systemPriceSignalData = systemPriceSignalData;
	}

	/*
	 * Constructors required to override DefaultContext
	 */
	public SmartGridContext() {
		super();
		System.out.println("SmartGridContext created");
	}

	public SmartGridContext(Object name) {
		super(name);
		System.out.println("SmartGridContext created with name " + name.toString());
	}

	public SmartGridContext(Object name, Object typeID) {
		super(name, typeID);
		System.out.println("SmartGridContext created with name " + name.toString() + " and type " + typeID.toString());
	}
	
	public SmartGridContext(Context context)
	{
		super(context.getId(), context.getTypeID());
		
		Iterator<Projection<?>> projIterator = context.getProjections().iterator();
		
		while (projIterator.hasNext()) {
			this.addProjection(projIterator.next());
			System.out.println("Added a projection");
		}
		
		this.setId(context.getId());
		this.setTypeID(context.getTypeID());
			
	}
	
	/*
	 * Have a nice toString() method to give good
	 * debug info
	 */
	public String toString() {
		String description;
		StringBuilder myDesc = new StringBuilder();
		myDesc.append("Instance of SmartGrid Context, hashcode = ");
		myDesc.append(this.hashCode());
		myDesc.append("\n contains arrays:");
		myDesc.append("\n insolation of length " + insolation.length);
		myDesc.append("\n windSpeed of length " + windSpeed.length);
		myDesc.append("\n airTemp of length " + airTemperature.length);
		myDesc.append("\n and baseDemand of length " + systemPriceSignalData.length);
		description = myDesc.toString();		
		return description;
	}

}
