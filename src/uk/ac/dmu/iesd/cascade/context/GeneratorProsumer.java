package uk.ac.dmu.iesd.cascade.context;

import java.util.*;
import repast.simphony.essentials.RepastEssentials;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * 
 * @version $Revision: 1.1 $ $Date: 2011/05/19 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial split of categories of prosumer from the abstract class representing all prosumers
 * 1.1 - Factored out (eliminated) those methodes already defined in the superclass (Babak)
 * 
 * 
 */
public abstract class GeneratorProsumer extends ProsumerAgent{

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */

	boolean hasCHP = false;
	boolean hasWind = false;
	boolean hasHydro = false;
	// Thermal Generation included so that Household can represent
	// Biomass, nuclear or fossil fuel generation in the future
	// what do we think?
	boolean hasThermalGeneration = false;
	boolean hasPV = false;
	
	/*
	 * the rated power of the various technologies / appliances we are interested in
	 * 
	 * Do not initialise these initially.  They should be initialised when an
	 * instantiated agent is given the boolean attribute which means that they
	 * have access to one of these technologies.
	 */
	float ratedPowerCHP;
	float ratedPowerWind;
	float ratedPowerHydro;
	float ratedPowerThermalGeneration;
	float ratedPowerPV;

/*
 * TODO - need some operating characteristic parameters here - e.g. time to start
 * ramp up generation etc. etc.
 */
	
	float percentageMoveableDemand;  // This can be set constant, or calculated from available appliances
	int maxTimeShift; // The "furthest" a demand may be moved in time.  This can be constant or calculated dynamically.

	

	/**
	 *  constructor
	 */
	public GeneratorProsumer(CascadeContext context) {
		super(context);
	}
	/*
	 * Accessor functions (NetBeans style)
	 * TODO: May make some of these private to respect agent conventions of autonomy / realistic simulation of humans
	 */



	public float getUnadaptedDemand(){
		// Cope with tick count being null between project initialisation and start.
		int index = Math.max(((int) RepastEssentials.GetTickCount() % baseDemandProfile.length), 0);
		return (baseDemandProfile[index]) - currentGeneration();
	}



	/**
	 * @return
	 */
	private float currentGeneration() {
		float returnAmount = 0;

		returnAmount = returnAmount + CHPGeneration() + windGeneration() + hydroGeneration() + thermalGeneration() + PVGeneration();
		if (Consts.DEBUG)
		{
			if (returnAmount != 0)
			{
				System.out.println("Generating " + returnAmount);
			}
		}
		return returnAmount;
	}

	/**
	 * @return
	 */
	private float PVGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private float thermalGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private float hydroGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private float windGeneration() {
		if(hasWind){
			//TODO: get a realistic model of wind production - this just linear between 
			//5 and 25 metres per second, zero below, max power above
			return (Math.max((Math.min(getWindSpeed(),25) - 5),0))/20 * ratedPowerWind;
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @return
	 */
	private float CHPGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param time
	 * @return float giving sum of baseDemand for the day.
	 */
	private float calculateFixedDayTotalDemand(int time) {
		int baseProfileIndex = time % baseDemandProfile.length;
		return ArrayUtils.sum(Arrays.copyOfRange(baseDemandProfile,baseProfileIndex,baseProfileIndex+ticksPerDay - 1));
	}

}
