package uk.ac.dmu.iesd.cascade;

import repast.simphony.engine.environment.RunEnvironment;
import simphony.util.messages.MessageCenter;

/**
 * Class to hold all the variable structures etc that are available to 
 * all code in the model - not features of a given environment (they should be in the context)
 * but rather enumerations, lookup tables, truly global variables etc.
 * 
 * @author J. Richard Snape
 * @author Baba Mahdavi
 * @version $Revision: 1.2 $ $Date: 2011/06/15 17:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history)
 * 
 * 1.1 - Class name changed; class becomes final; constants' names with capital characters (Babak)
 * 1.2 - added AGGREGATOR_LEARNING_PERIODE;  
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

	public static final int MAX_GENERATOR_CAPACITY = 5;	
	public static boolean DEBUG = false;  //use for debugging, default is false
	public static final int MAX_SUPPLY_CAPACITY = 45;
	public static final int NUM_DEMAND_COLUMNS = 1;
	

	/**
	 * This is the aggregator profile building period time in terms of day, assuming it is set manually
	 * During this period, aggregator do not send any signal and simply get the the normal usage of
	 * it customers baseline demand and at the end of this period, it will build an average usage
	 * profile, which will used during training period. This period should at least take
	 * sometimes about 3 to 7 days. 
	 * @see AGGREGATOR_TRAINING_PERIODE
	 **/
	public static final int AGGREGATOR_PROFILE_BUILDING_PERIODE = 7; // in terms of day
	
	/**
	 * This is the aggregator training period time in terms of day (assuming it is set manually)
	 * During this time, aggregator send S=1 signal to test customers behaviors and hence
	 * estimate k and e. At minimum, it needs to be 48 days in order to complete sending S=1
	 * signals, one for each 48 timeslote of the day.  
	 * This time is above the time that aggregator needs to gather and build an average 
	 * baseline profile of its customers (prosumers), usually between 4-7 days 
	 * @see AGGREGATOR_PROFILE_BUILDING_PERIODE
	 **/
	public static final int AGGREGATOR_TRAINING_PERIODE = 48; // in terms of day
	
	
	/**
	 * Define different types of signals 
	 **/		
	public static enum SIGNAL_TYPE {
		S, S_TRAINING
	}

	
	/**
	 * Define different types of agents 
	 **/	
	public static enum AGENT_TYPE {
		PROSUMER,AGGREGATOR
	}



}

