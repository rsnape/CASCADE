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
	 * Maximum domestic hot water storage capacity (litres)
	 */
	public static final int MAX_HOUSHOLD_HOT_WATER_USE = 50;
	
	/**
	 * Minimum domestic hot water storage capacity (litres)
	 */
	public static final int MIN_HOUSHOLD_HOT_WATER_USE = 20;
	
	/**
	 * Mimimum hot water temperature for safety (degrees C)
	 */
	public static final int DOMESTIC_SAFE_WATER_TEMP = 50;
	
	/**
	 * The typical heat pump coefficient of performance for a domestic dwelling
	 */
	public static final float DOMESTIC_HEAT_PUMP_COP = 2.4f;

	/**
	 * System exit error codes for different types of error
	 */
	public static final int BAD_FILE_ERR_CODE = 1;


	public static final double COMMON_INTEREST_GROUP_EDGE_WEIGHT = 0.95;  
	
	public static final Integer[] NUM_OF_OCCUPANTS_ARRAY = {0,1,2,3,4,5,6,7,8};
	public static final double[] OCCUPANCY_PROBABILITY_ARRAY = {0.300206371,0.341735335,0.15506365,0.133630415,0.049359587,0.014498622,0.003377304,0.002128716};
	
	public static final float[] BASIC_AVERAGE_SET_POINT_PROFILE = {19.5f,19,18.5f,18,18,18,18,18,18,18,18,18,18.25f,18.5f,18.75f,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19.2f,19.4f,19.6f,19.8f,20,20.2f,20.4f,20.6f,20.8f,21,21,21,21,21,21,21,20.5f,20};


	public static final float HEAT_PUMP_THRESHOLD_TEMP_DIFF = 3;


	public static final float[] MONTHLY_MAINS_WATER_TEMP = {5,5,6,8,10,12,12,12,10,8,6,5};


	public static final float MAX_INSOLATION = 170f;
	
	public static final int HEAT_PUMP_MIN_SWITCHOFF = 1;
	public static final int HEAT_PUMP_MAX_SWITCHOFF = 8;


	public static final int MELODY_MODELS_TICKS_PER_DAY = 48;


	public static final int DAYS_PER_YEAR = 365;


	public static final float DOMESTIC_HEAT_PUMP_COP_HEAT_RECOVERY = 2.0f;


	public static final int SECONDS_PER_HALF_HOUR = 1800;


	public static final float DAYTIME_TEMP_LOSS_THRESHOLD = 0.5f;


	public static final int DAYS_PER_WEEK = 7;


	public static final float[] ZERO_COST_SIGNAL = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	
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

