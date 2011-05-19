package uk.ac.dmu.iesd.cascade.context;

import java.io.*;
import java.math.*;
import java.util.*;
import javax.measure.unit.*;

import org.apache.tools.ant.taskdefs.Sync.MyCopy;
import org.hsqldb.lib.ArrayUtil;
import org.jfree.util.ArrayUtilities;
import org.jscience.mathematics.number.*;
import org.jscience.mathematics.vector.*;
import org.jscience.physics.amount.*;



//import cern.colt.Arrays;
import repast.simphony.adaptation.neural.*;
import repast.simphony.adaptation.regression.*;
import repast.simphony.context.*;
import repast.simphony.context.space.continuous.*;
import repast.simphony.context.space.gis.*;
import repast.simphony.context.space.graph.*;
import repast.simphony.context.space.grid.*;
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
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.Consts.*;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;


/**
 * @author J. Richard Snape
 * 
 * @version $Revision: 1.00 $ $Date: 2011/03/17 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial split of categories of prosumer from the abstract class representing all prosumers
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
