package uk.ac.dmu.iesd.cascade;

import repast.simphony.engine.environment.RunEnvironment;
import simphony.util.messages.MessageCenter;

/**
 * Class to hold all the variable structures etc that are available to 
 * all code in the model - not features of a given environment (they should be in the context)
 * but rather enumerations, lookup tables, truly global variables etc.
 * 
 * @author J. Richard Snape
 * @version $Revision: 1.1 $ $Date: 2011/05/11 17:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history)
 * 
 * 1.1 - Class name changed; class becomes final; constants' names with capital characters (Babak)
 * 
 *
 */
public final class Consts {
	public static enum GENERATOR_TYPE {
		COAL, GAS, OIL, NUCLEAR, BIOMASS, WIND, HYDRO, CHP, SOLAR
	}

	public static enum STORAGE_TYPE {
		BATTERY, HYDRO, HEAT, FLYWHEEL, EV
	}

	public static final float MAX_GENERATOR_CAPACITY_GWATTS = 5f;	
	public static boolean DEBUG = false;  //use for debugging, default is false
	public static final float MAX_SUPPLY_CAPACITY_GWATTS = 45f;
	public static final int NUM_DEMAND_COLUMNS = 1;
	public static final float MAX_SYSTEM_BUY_PRICE_PNDSPERMWH = 1000f;
	public static final int TOTAL_SYSTEM_CUSTOMERS = 23000000;
}

