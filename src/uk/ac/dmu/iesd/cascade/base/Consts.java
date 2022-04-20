package uk.ac.dmu.iesd.cascade.base;

/**
 * Class to hold all the variable structures etc that are available to all code
 * in the model - not features of a given environment (they should be in the
 * context) but rather enumerations, lookup tables, truly global variables etc.)
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.3 $ $Date: 2012/05/16 $
 * 
 *          Version history (for intermediate steps see Git repository history)
 * 
 *          1.1 - Class name changed; class becomes final; constants' names with
 *          capital characters (Babak) 
 *          1.2 - added AGGREGATOR_LEARNING_PERIODE;
 *          1.3 - added Category and Type constants, defining aggregators' types
 *          and categories (as BMUs), (Babak)
 * 
 * 
 */
public final class Consts
{

	/*----------------------------
	 * Model level constants
	 *----------------------------*/
	public static String CASCADE_LOGGER_NAME = "CascadeLogger"; // use for
																// debugging,
																// default is
																// false

	public static String FILE_CHART_FORMAT_EXT = ".png";

	/*
	 * This is to control the length of initialized demand profileit can be set
	 * to 1 day [presumably necessary for smart signal] up to one year (365
	 * days) [usually by default] this currently affects the size of all profile
	 * arrays, except heat space and hot water (along with historical arrays)
	 * TODO: Remove this - should be gleaned from the input data - shouldn't
	 * have to set it!!
	 */
	public static int NB_OF_DAYS_LOADED_DEMAND = 1; // should be ideally 365
													// days (1 year)

	// Controlling HHProsumers electricity consumption/usage
	public static boolean HHPRO_HAS_ELEC_SPACE_HEAT = true;
	public static boolean HHPRO_HAS_ELEC_WATER_HEAT = true;
	public static boolean HHPRO_HAS_COLD_APPL = true;
	public static boolean HHPRO_HAS_WET_APPL = true;
	public static boolean HHPRO_HAS_ELEC_VEHICLE = true;

	// Controlling REEA operation
	public static boolean AGG_RECO_REEA_ON = false;

	// Controlling the generation of snapshots:
	public static boolean TAKE_SNAPSHOT_OF_CHART_0_Insol = false;
	public static boolean TAKE_SNAPSHOT_OF_CHART_1_AirTemp = false;
	public static boolean TAKE_SNAPSHOT_OF_CHART_2_WindSpeed = false;
	public static boolean TAKE_SNAPSHOT_OF_CHART_3_AggSumOfD = false;
	public static boolean TAKE_SNAPSHOT_OF_CHART_5_SmartAdapt = false;
	public static boolean TAKE_SNAPSHOT_OF_CHART_8_Market = false;

	/*------------------------
	 * System level constants
	 *------------------------*/
	public static final int TOTAL_SYSTEM_CUSTOMERS = 23000000;
	public static final double MAX_GENERATOR_CAPACITY_GWATTS = 5d;
	public static final double MAX_SUPPLY_CAPACITY_GWATTS = 45d;
	public static final double FLOATING_POINT_TOLERANCE = 1e-29;

	public static final int NIGHT_TO_DAY_TRANSITION_TICK = 16; // 8h

	public static final int RANDOM = -1;

	// Define different types of agents
	public static enum AGENT_TYPE
	{
		PROSUMER, AGGREGATOR
	}

	/*----------------
	 * Signal Mode
	 *----------------*/
	public static int SIGNAL_MODE_FIXED = 0;
	public static int SIGNAL_MODE_PRICE = 1;
	public static int SIGNAL_MODE_SMART = 2;

	/*--------------------------------------
	 * System exit error codes for different types of error
	 *---------------------------------------*/
	public static final int BAD_FILE_ERR_CODE = 1;

	/*-----------------------------
	 * Units of Measurement Constants
	 *------------------------------*/
	public static final int HOURS_PER_DAY = 24;
	public static final int DAYS_PER_WEEK = 7;
	public static final int DAYS_PER_YEAR = 365;
	public static final int MINUTES_PER_DAY = 1440;
	public static int SECONDS_PER_DAY = 86400;
	public static final int SECONDS_PER_HALF_HOUR = 1800;

	// factor to convert energy expressed in kWh to Joules.
	public static final double KWH_TO_JOULE_CONVERSION_FACTOR = 3600000;

	/*------------------------
	 * Scheduling Priorities  
	 *-------------------------*/
	public static final double PRE_INITIALISE_FIRST_TICK = 700;
	public static final double AGGREGATOR_PRE_STEP_PRIORITY_FIRST = 600;
	public static final double AGGREGATOR_INIT_MARKET_STEP_PRIORITY_FIRST = 570;
	public static final double AGGREGATOR_MARKET_STEP_PRIORITY_FIRST = 550;
	public static final double SO_PRIORITY_SECOND = 500; // so
	public static final double SC_PRIORITY_THIRD = 400; // sc
	public static final double PX_PRIORITY_FOURTH = 300; // px
	public static final double PROSUMER_PRIORITY_FIFTH = 200;
	public static final double AGGREGATOR_STEP_PRIORITY_SIXTH = 100;

	// Schedule constants - Probe display update priority. Set to a large
	// negative number, so it goes after most actions
	public static final double PROBE_PRIORITY = -100;

	/**
	 * ============================= Aggregator Constants
	 * ==============================
	 */

	public static enum BMU_CATEGORY
	{
		GEN_T, DEM_S, GENDEM_E, GENDEM_I,
	}

	public static enum BMU_TYPE
	{
		GEN_COAL, GEN_CCGT, GEN_WIND, DEM_LARGE, DEM_SMALL
	}

	// Define different types of signals,
	// TODO: if also used by prosumers, then should be perhaps defined in sys.
	// level section
	public static enum SIGNAL_TYPE
	{
		S, S_TRAINING
	}

	/*----------------
	 * RECO constants
	 *----------------*/
	/**
	 * This is the aggregator profile building period time in terms of day,
	 * assuming it is set manually During this period, aggregator do not send
	 * any signal and simply get the the normal usage of it customers baseline
	 * demand and at the end of this period, it will build an average usage
	 * profile, which will used during training period. The way it is currently
	 * used, it should basically correspond to the number of demand profiles
	 * (e.g. days of weeks: Monday, Tuesday, etc., or two profiles: weekdays and
	 * weekend) If the models continues to use them in its current way, it would
	 * be more proper to name this as NB_OF_DEMAND_PROFILES;
	 * 
	 * @see AGGREGATOR_TRAINING_PERIODE
	 **/

	public static final int AGGREGATOR_PROFILE_BUILDING_PERIODE = 7;
	// public static final int AGGREGATOR_PROFILE_BUILDING_PERIODE =
	// DAYS_PER_YEAR; // in terms of days

	/**
	 * This is the aggregator training period time in terms of day (assuming it
	 * is set manually) During this time, aggregator send S=1 signal to test
	 * customers behaviors and hence estimate k and e. At minimum, it needs to
	 * be 48 days in order to complete sending S=1 signals, one for each 48
	 * timeslote of the day. This time is above the time that aggregator needs
	 * to gather and build an average baseline profile of its customers
	 * (prosumers), usually between 4-7 days
	 * 
	 * @see #AGGREGATOR_PROFILE_BUILDING_PERIODE
	 **/
	public static final int AGGREGATOR_TRAINING_PERIODE = 48; // in terms of
																// days

	public static final int AGGREGATOR_PROFILE_BUILDING_SP = 336;
	public static final int AGGREGATOR_TRAINING_SP = 2304; // in terms of ticks

	// public static final int MARKET_INACTIVE_PERIODE = 2640; // 48 + 7 days

	public static final double MAX_SYSTEM_BUY_PRICE_PNDSPERMWH = 1000d;

	/**
	 * ============================= Prosumers Constants
	 * ==============================
	 */

	public static final double COMMON_INTEREST_GROUP_EDGE_WEIGHT = 0.95;

	public static final int MELODY_MODELS_TICKS_PER_DAY = 48; // used for cold
																// and wet
																// appliances

	public static final double MAX_INSOLATION = 1000d; // Used for PV generation

	// Defines the type of a storage Prosumer, can take values (NOT Currently
	// used!!!)
	public static enum STORAGE_TYPE
	{
		BATTERY, HYDRO, HEAT, FLYWHEEL, EV
	}

	// Defines the type of a generator Prosumer, can take value (NOT Currently
	// used!!!)
	public static enum GENERATOR_TYPE
	{
		COAL, GAS, OIL, NUCLEAR, BIOMASS, WIND, HYDRO, CHP, SOLAR
	}

	/*---------------------
	 * Household Prosumers
	 *---------------------*/

	public static final String COLD_APP_FRIDGE = "Fridge";
	public static final String COLD_APP_FREEZER = "Freezer";
	public static final String COLD_APP_FRIDGEFREEZER = "FridgeFreezer";
	public static final String COLD_APP_FRIDGE_ORIGINAL = "FridgeOrig"; // initial/original
																		// Values
	public static final String COLD_APP_FREEZER_ORIGINAL = "FreezerOrig";
	public static final String COLD_APP_FRIDGEFREEZER_ORIGINAL = "FridgeFreezerOrig";

	// these are some scaling factor used in Melody model, so that the values
	// (for cold and wet) won't be too high
	public static final double COLD_APP_SCALE_FACTOR_FRIDGE = 0.049;
	public static final double COLD_APP_SCALE_FACTOR_FREEZER = 0.082;
	public static final double COLD_APP_SCALE_FACTOR_FRIDGEFREEZER = 0.105;

	public static final double WET_APP_SCALE_FACTOR_WASHER = 0.197;
	public static final double WET_APP_SCALE_FACTOR_DRYER = 0.321;
	public static final double WET_APP_SCALE_FACTOR_DISH = 0.413;

	public static final String WET_APP_WASHER = "Washer";
	public static final String WET_APP_DRYER = "Dryer";
	public static final String WET_APP_DISHWASHER = "Dishwasher";
	public static final String WET_APP_WASHER_ORIGINAL = "WasherOrig";
	public static final String WET_APP_DRYER_ORIGINAL = "DryerOrig";
	public static final String WET_APP_DISHWASHER_ORIGINAL = "DishwasherOrig";

	// Defines is the maximum fraction of any domestic load that is moveable
	public static final double MAX_DOMESTIC_MOVEABLE_LOAD_FRACTION = 0.5d;

	// Threshold in price signal at which behaviour change is prompted (if agent
	// is willing)
	// TODO: Define units and get price units consistent throughout model.
	public static final double HOUSEHOLD_COST_THRESHOLD = 125;

	// Fraction of households who are willing to exercise behaviour change.
	public static final double HOUSEHOLDS_WILLING_TO_CHANGE_BEHAVIOUR = 0.5;

	// Initial fraction of households who have a smart controller.
	public static final double HOUSEHOLDS_WITH_SMART_CONTROL = 0.1;

	public static final double[] ZERO_COST_SIGNAL =
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0 };

	// Minimum allowable temperature that may be set for a household in degrees
	// centigrade.
	// Babak: used for behavioral change purpose (not Space Heating), although
	// currently does nothing!
	public static final double HOUSEHOLD_MIN_SETPOINT = 15;

	// Maximum allowable temperature that may be set for a household in degrees
	// centigrade.
	// Babak: used for behavioral change purpose (not Space Heating), although
	// currently does nothing!
	public static final double HOUSEHOLD_MAX_SETPOINT = 28;

	// Electric vehicle constants
	public static final double[] CAR_ARRIVAL_PROBABILITY =
	{ 0.003042436, 0.003042436, 0.000368881, 0.000368881, 0.000131171, 0.000131171, 9.09E-05, 9.09E-05, 0.000206242, 0.000206242,
			0.000463641, 0.000463641, 0.001392982, 0.001392982, 0.003822591, 0.003822591, 0.007449823, 0.007449823, 0.010642317,
			0.010642317, 0.012474827, 0.012474827, 0.012970487, 0.012970487, 0.01916083, 0.01916083, 0.030109165, 0.030109165, 0.049430865,
			0.049430865, 0.068433727, 0.068433727, 0.072868541, 0.072868541, 0.071423073, 0.071423073, 0.052685389, 0.052685389,
			0.028696649, 0.028696649, 0.019140672, 0.019140672, 0.01324127, 0.01324127, 0.012780752, 0.012780752, 0.008972774, 0.008972774 };
	public static final double MEAN_JOURNEY_LENGTH = 27;
	public static final double JOURNEY_MADE_PROBABILITY = 0.74;

	/**
	 * ++++++++++++++++++++++ Electrical Space Heat +++++++++++++++++++++++
	 */

	// The typical heat pump coefficient of performance for space heating a
	// domestic dwelling
	public static final double DOMESTIC_HEAT_PUMP_SPACE_COP = 2.4d;

	// The degradation of heat pump coefficient of performance when increasing
	// rather than maintaining temperature.
	public static final double DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE = 0.9d;

	// The typical heat pump coefficient of performance for heating water in a
	// domestic dwelling
	public static final double DOMESTIC_HEAT_PUMP_WATER_COP = 2.0d; //

	// Maximum domestic heat pump power (in kW)
	public static final double TEMP_CHANGE_TOLERANCE = 0d;
	public static final double NORMALIZING_MAX_COST = 1;

	//This is a threshold for difference between outside and inside temperature to 
	//run heat pump
	public static final double HEAT_PUMP_THRESHOLD_TEMP_DIFF = 0.5d;

	public static final int HEAT_PUMP_MIN_SWITCHOFF = 1;
	public static final int HEAT_PUMP_MAX_SWITCHOFF = 8;

	// Don't use the constant below. Rather, use DOMESTIC_HEAT_PUMP_SPACE_COP *
	// DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE
	// 24/01/2012 This should be removed. TODO
	// public static final double DOMESTIC_HEAT_PUMP_COP_HEAT_RECOVERY = 2.0d;

	// Typical domestic heat pump rating (electrical) in kW
	public static final double TYPICAL_HEAT_PUMP_ELEC_RATING = 4d;

	public static final double NIGHT_TEMP_LOSS_THRESHOLD = 1d;
	public static final double DAYTIME_TEMP_LOSS_THRESHOLD = 0.5d;

	// used by Wattbox
	public static final double[] MAX_PERMITTED_TEMP_DROPS =
	{ 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d,
			0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d };

	// The threshold defining the minimum difference in costs that we consider
	// different in the model
	// WARNING: Be very careful in using this - can mask some nasty algorithmic
	// errors
	public static final double COST_DECISION_THRESHOLD = 1e-3d; // used by
																// Wattbox

	// The temperature used to represent the prior day external temperature at
	// simulation initialisation
	public static final double INITIALISATION_EXTERNAL_TEMP = 7;

	/**
	 * ++++++++++++++++++++++++++++++++ Hot Water/ Electrical Water Heat
	 * +++++++++++++++++++++++++++++++++
	 */

	// Maximum domestic hot water storage capacity (litres)
	public static final int MAX_HOUSHOLD_HOT_WATER_USE = 50;

	// Minimum domestic hot water storage capacity (litres)
	public static final int MIN_HOUSHOLD_HOT_WATER_USE = 20;

	// Mimimum hot water temperature for safety (degrees C)
	public static final int DOMESTIC_SAFE_WATER_TEMP = 50;

	public static final double[] MONTHLY_MAINS_WATER_TEMP =
	{ 5, 5, 6, 8, 10, 12, 12, 12, 10, 8, 6, 5 };

	public static final double WATER_HEAT_PUMP_MAX_HEAT_POWER = 6.0d;

	public static final double WATER_TEMP_LOSS_PER_SECOND = 1d / 1860d;

	// (Approximate) Specific heat capacity of water in Joules per litre per
	// Kelvin
	public static final double WATER_SPECIFIC_HEAT_CAPACITY = 4200;

	public static final int EST_INTERCEPT = 46;
	public static final int EST_SLOPE = 26;
	public static final double EST_STD_DEV = 7;
	public static final double[] EST_DRAWOFF =
	{ 2.25d, 2.25d, 1.4d, 1.4d, 1.15d, 1.15d, 0.95d, 0.95d, 1.6d, 1.6d, 2.15d, 2.15d, 5.3d, 5.3d, 10.8d, 10.8d, 9.1d, 9.1d, 7.25d, 7.25d,
			6.85d, 6.85d, 6d, 6d, 5.25d, 5.25d, 4.35d, 4.35d, 3.6d, 3.6d, 3.65d, 3.65d, 4.45d, 4.45d, 6.5d, 6.5d, 9d, 9d, 8.7d, 8.7d, 7.2d,
			7.2d, 6.1d, 6.1d, 5d, 5d, 3d, 3d };

	public static final double IMMERSION_HEATER_COP = 0.9d; // Used by Wattbox
	public static final double MAX_DOMESTIC_IMMERSION_POWER = 3;

	/**
	 * ++++++++++++++++ Wet Appliances +++++++++++++++++
	 */

	public static final double[] WET_APPLIANCE_PDF =
	{ 18.91, 16.45, 13.49, 12.52, 16.80, 14.41, 11.13, 9.99, 13.90, 10.18, 13.30, 15.53, 18.79, 17.65, 21.79, 25.72, 36.83, 43.13, 43.94,
			46.43, 49.61, 52.02, 49.30, 45.71, 42.85, 42.42, 39.08, 39.67, 41.19, 40.16, 37.68, 37.56, 37.67, 38.10, 38.19, 37.10, 36.46,
			37.32, 39.44, 37.77, 37.05, 35.09, 35.13, 34.19, 29.75, 26.68, 26.01, 21.30 };
	public static final int MAX_ALLOWED_WET_APP_MOVE = 12;

	/**
	 * +++++++++++ Occupancy ++++++++++++
	 */
	// public static final Integer[] NUM_OF_OCCUPANTS_ARRAY =
	// {0,1,2,3,4,5,6,7,8}; // NOT currently used
	public static final double[] OCCUPANCY_PROBABILITY_ARRAY =
	{ 0.300206371, 0.341735335, 0.15506365, 0.133630415, 0.049359587, 0.014498622, 0.003377304, 0.002128716 };

	/**
	 * ++++++++++++++++++++++++++ Energy Consumption Control
	 * +++++++++++++++++++++++++++
	 */
	// public static boolean CONSUME_HOT_WATER = false;

	public static final double[] AVERAGE_GRID_CO2_INTENSITY =
	{ 397, 394.5, 392, 389.5, 387, 385.5, 384, 386, 388, 397, 406, 418.5, 431, 440.5, 450, 454, 458, 460.5, 463, 463.5, 464, 464, 464, 464,
			464, 463, 462, 461, 460, 460.5, 461, 460.5, 460, 459, 458, 456, 454, 453.5, 453, 451, 449, 444, 439, 428.5, 418, 409.5, 401,
			399 };

	/****************************************
	 * Not currently used (To Delete?)
	 *******************************************/

	// public static final double[] BASIC_AVERAGE_SET_POINT_PROFILE =
	// {19.5d,19,18.5d,18,18,18,18,18,18,18,18,18,18.25d,18.5d,18.75d,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19.2d,19.4d,19.6d,19.8d,20,20.2d,20.4d,20.6d,20.8d,21,21,21,21,21,21,21,20.5d,20};

	// public static final int MAX_GENERATOR_CAPACITY = 5; //Not currently used
	// public static final int MAX_SUPPLY_CAPACITY = 45; //Not currently used
	// public static final int NUM_DEMAND_COLUMNS = 1; //Not currently used

	/**
	 * ++++++++++++++++++++++++++ Single Non Domestic Constant
	 * +++++++++++++++++++++++++++
	 */

	public static final boolean USE_SINGLE_NON_DOMESTIC_AGGREGATOR = false;
	public static final boolean UPDATE_PHYSICAL_NODE_TO_MARKET = true;
	public static final double RATIO_AGANIST_HOUSEHOLD = 2d;
	public static final double PV_KWH_PER_KWP = 750;
	public static final double INSTALLER_MIN_PROFIT = 0.05;
	public static final double INSTALLER_MAX_PROFIT = 0.2;
	public static final int MSECS_PER_DAY = 24 * 60 * 60 * 1000;

	public static final int UK_HOUSEHOLD_COUNT = 23000000; // TODO: this is
															// estimate to be
															// checked!!!

	public static final String HEAT_TYPE_HP = "HEATPUMP";
	public static final String HEAT_TYPE_STORAGE = "STORAGE";

	public static final double[] BASIC_AVERAGE_SET_POINT_PROFILE = {14,14,14,14,14,14,14,14,14,14,14,14,14,14,20,20,20,20,20,20,18,18,18,18,
																	18,18,18,18,18,18,18,18,18,18,18,18,20,20,20,20,22,22,22,22,22,22,16,16};

	public static enum HEATING_TYPE
	{
		GRID_GAS, CALOR_GAS, OIL, ELECTRIC_STORAGE, BIOMASS, GND_SOURCE_HP, AIR_SOURCE_HP, SOLAR_THERMAL
	};

}
