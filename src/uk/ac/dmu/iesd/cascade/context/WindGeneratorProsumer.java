package uk.ac.dmu.iesd.cascade.context;

import java.util.*;
import repast.simphony.engine.schedule.*;
import repast.simphony.essentials.RepastEssentials;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.1 $ $Date: 2011/05/18 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 1.0 - Initial split of categories of prosumer from the abstract
 * class representing all prosumers
 * 1.1 - eliminated redundant methods (inherited from superclass ProsumerAgent)
 */
public class WindGeneratorProsumer extends GeneratorProsumer {

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */
	int numTurbines;

	/*
	 * TODO - need some operating characteristic parameters here - e.g. time to
	 * start ramp up generation etc. etc.
	 */
	/*
	 * Accessor functions (NetBeans style) TODO: May make some of these private
	 * to respect agent conventions of autonomy / realistic simulation of humans
	 */


	
	  /**
     * Returns a string representing the state of this agent. This 
     * method is intended to be used for debugging purposes, and the 
     * content and format of the returned string should include the states (variables/parameters)
     * which are important for debugging purpose.
     * The returned string may be empty but may not be <code>null</code>.
     * 
     * @return  a string representation of this agent's state parameters
     */
    protected String paramStringReport(){
    	String str="";
    	return str;
    	
    }


	public float getUnadaptedDemand() {
		// Cope with tick count being null between project initialisation and
		// start.
		int index = Math
				.max(((int) RepastEssentials.GetTickCount() % baseDemandProfile.length),
						0);
		return (baseDemandProfile[index]) - currentGeneration();
	}


	/*
	 * Step behaviour
	 */

	/******************
	 * This method defines the step behaviour of a wind generator agent This
	 * represents a farm of 1 or more turbines i.e. anything that is purely wind
	 * - can be a single turbine or many
	 * 
	 * Input variables: none
	 * 
	 * Return variables: boolean returnValue - returns true if the method
	 * executes successfully
	 ******************/
	@ScheduledMethod(start = 1, interval = 1, shuffle = true)
	public void step() {

		// Define the return value variable. Set this false if errors
		// encountered.
		boolean returnValue = true;

		// Note the simulation time if needed.
		// Note - Repast can cope with fractions of a tick (a double is
		// returned)
		// but I am assuming here we will deal in whole ticks and alter the
		// resolution should we need
		int time = (int) RepastEssentials.GetTickCount();
		int timeOfDay = (time % ticksPerDay);
		CascadeContext myContext = this.getContext();

		checkWeather(time);

		// Do all the "once-per-day" things here
		if (timeOfDay == 0) {
			inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);
		}

		/*
		 * As wind is non-dispatchable, simply return the current generation
		 * each time. The only adaptation possible would be shutting down some /
		 * all turbines
		 * 
		 * TODO - implement turbine shutdown
		 */
		setNetDemand(0 - currentGeneration());

		// Return (this will be false if problems encountered).
		//return returnValue;

	}

	/**
	 * @return
	 */
	private float currentGeneration() {
		float returnAmount = 0;

		returnAmount = returnAmount + windGeneration();
		if (Consts.DEBUG) {
			if (returnAmount != 0) {
				System.out.println("Generating " + returnAmount);
			}
		}
		return returnAmount;
	}

	/**
	 * @return - float containing the current generation for this wind generator
	 */
	private float windGeneration() {
		if (hasWind) {
			// TODO: get a realistic model of wind production - this just linear
			// between
			// 2.5 and 12.5 metres per second (5 to 25 mph / knots roughly),
			// zero below, max power above
			return (Math.max((Math.min(getWindSpeed(), 12.5f) - 2.5f), 0)) / 20
					* ratedPowerWind;
		} else {
			return 0;
		}
	}

	/**
	 * @param time
	 * @return float giving sum of baseDemand for the day.
	 */
	private float calculateFixedDayTotalDemand(int time) {
		int baseProfileIndex = time % baseDemandProfile.length;
		return ArrayUtils.sum(Arrays.copyOfRange(baseDemandProfile,
				baseProfileIndex, baseProfileIndex + ticksPerDay - 1));
	}



	/*
	 * Constructor function(s)
	 */
	public WindGeneratorProsumer(CascadeContext context, float[] baseDemand, float capacity) {
		/*
		 * If number of wind turbines not specified, assume 1
		 */
		this(context, baseDemand, capacity, 1);
	}

	public WindGeneratorProsumer(CascadeContext context, float[] baseDemand,
			float capacity, int turbines) {
		super(context);
		this.hasWind = true;
		this.ratedPowerWind = capacity;
		this.numTurbines = turbines;
		this.percentageMoveableDemand = 0;
		this.maxTimeShift = 0;
		this.ticksPerDay = context.getTickPerDay();
		if (baseDemand.length % ticksPerDay != 0) {
			System.err.println("baseDemand array not a whole number of days");
			System.err
					.println("Will be truncated and may cause unexpected behaviour");
		}
		this.baseDemandProfile = new float[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.baseDemandProfile, 0,
				baseDemand.length);
		// Initialise the smart optimised profile to be the same as base demand
		// smart controller will alter this
		this.smartOptimisedProfile = new float[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0,
				smartOptimisedProfile.length);
	}

}
