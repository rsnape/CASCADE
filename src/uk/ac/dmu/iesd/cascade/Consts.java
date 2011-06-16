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
	
	/**
	 * Model level constants
	 */
	public static boolean DEBUG = false;  //use for debugging, default is false
	
	
	/**
	 * System level constants
	 */
	public static final int TOTAL_SYSTEM_CUSTOMERS = 23000000;
	public static final float MAX_GENERATOR_CAPACITY_GWATTS = 5f;	
	public static final float MAX_SUPPLY_CAPACITY_GWATTS = 45f;
	
	
	/**
	 * Aggregator constants
	 * 
	 */
	public static final float MAX_SYSTEM_BUY_PRICE_PNDSPERMWH = 1000f;
	
	
	/**
	 * Prosumer constants
	 */
	
	/*
	 * Domestic
	 */
	/**
	 * Defines is the maximum fraction of any domestic load that is moveable.
	 */
	public static final double MAX_DOMESTIC_MOVEABLE_LOAD_FRACTION = 0.5f;

	/**
	 * Threshold in price signal at which behaviour change is prompted (if agent is willing)
	 * TODO: Define units and get price units consistent throughout model.
	 */
	public static final float HOUSEHOLD_COST_THRESHOLD = 125;

	/**
	 * Minimum allowable temperature that may be set for a household in degrees centigrade.
	 */
	public static final float HOUSEHOLD_MIN_SETPOINT = 15;  
	
	/**
	 * Maximum allowable temperature that may be set for a household in degrees centigrade.
	 */
	public static final float HOUSEHOLD_MAX_SETPOINT = 28;

	/**
	 * Fraction of households who are willing to exercise behaviour change.
	 */
	public static final double HOUSEHOLDS_WILLING_TO_CHANGE_BEHAVIOUR = 0.5;


	/**
	 * Initial fraction of households who have a smart controller.
	 */
	public static final double HOUSEHOLDS_WITH_SMART_CONTROL = 0.1;


	/**
	 * System exit error codes for different types of error
	 */
	public static final int BAD_FILE_ERR_CODE = 1;


	public static final double COMMON_INTEREST_GROUP_WEIGHT = 0.95;  
	
	/*
	 * Generators
	 */
	/**
	 * Defines the type of a generator Prosumer, can take values
	 * COAL, GAS, OIL, NUCLEAR, BIOMASS, WIND, HYDRO, CHP, SOLAR
	 */
	public static enum GENERATOR_TYPE {
		COAL, GAS, OIL, NUCLEAR, BIOMASS, WIND, HYDRO, CHP, SOLAR
	}

	/*
	 * Storage
	 */
	/**
	 * Defines the type of a storage Prosumer, can take values
	 * BATTERY, HYDRO, HEAT, FLYWHEEL, EV
	 */
	public static enum STORAGE_TYPE {
		BATTERY, HYDRO, HEAT, FLYWHEEL, EV
	}	
}

