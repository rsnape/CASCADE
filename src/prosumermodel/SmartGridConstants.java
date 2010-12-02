/**
 * 
 */
package prosumermodel;

import repast.simphony.engine.environment.RunEnvironment;
import simphony.util.messages.MessageCenter;

/**
 * Class to hold all the variable structures etc that are available to 
 * all code in the model - not features of a given environment (they should be in the context)
 * but rather enumerations, lookup tables, truly global variables etc.
 * 
 * @author J. Richard Snape
 * @version $Revision: 1.0 $ $Date: 2010/12/02 17:00:00 $
 *
 */
public class SmartGridConstants {
	public static enum generatorType {
		COAL, GAS, OIL, NUCLEAR, BIOMASS, WIND, HYDRO, CHP, SOLAR
	}
	
	public static enum storageType {
		BATTERY, HYDRO, HEAT, FLYWHEEL, EV
	}
	
	/*
	 * Variable to store whether or not we wish to output debug information
	 */
	public static boolean debug = false;
}
