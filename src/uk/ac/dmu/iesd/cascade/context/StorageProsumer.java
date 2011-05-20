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
import static java.lang.Math.*;
import static repast.simphony.essentials.RepastEssentials.*;


/**
 * TODO: Determine the role of this class; prosumers supposed to range from 
 * pure generators to pure consumers, but not storage (is this a tool)? 
 * May need to be eliminated. 
 * 
 * @author J. Richard Snape

 * @version $Revision: 1.00 $ $Date: 2011/03/17 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial split of categories of prosumer from the abstract class representing all prosumers
 * 
 * 
 */
public class StorageProsumer extends ProsumerAgent{

	/*
	 * NOTE 
	 * TODO: - It is possible that we should have some kind of hierarchical
	 * inheritance and this should also be abstract, sub-classed to specific types
	 * of non-dom consumer.  For now, it is a placeholder.
	 */
	
	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */

	boolean hasElectricalStorage = false; // Do we need to break out various storage technologies?
	boolean hasHotWaterStorage = false;
	boolean hasSpaceHeatStorage = false;


	/*
	 * the rated power of the various technologies / appliances we are interested in
	 * 
	 * Do not initialise these initially.  They should be initialised when an
	 * instantiated agent is given the boolean attribute which means that they
	 * have access to one of these technologies.
	 */
	float ratedCapacityElectricalStorage;   // Note kWh rather than kW
	float ratedCapacityHotWaterStorage;
	float ratedCapacitySpaceHeatStorage; // Note - related to thermal mass

	/*
	 * Specifically, a household may have a certain percentage of demand
	 * that it believes is moveable and / or a certain maximum time shift of demand
	 */
	float percentageMoveableDemand;  // This can be set constant, or calculated from available appliances
	int maxTimeShift; // The "furthest" a demand may be moved in time.  This can be constant or calculated dynamically.

	/*
	 * temperature control parameters
	 */
	float minSetPoint;  // The minimum temperature for this Household's building in centigrade (where relevant)
	float maxSetPoint;  // The maximum temperature for this Household's building in centigrade (where relevant)
	float currentInternalTemp;
	
	/*
	 * This may or may not be used, but is a threshold cost above which actions
	 * take place for the household
	 */
	float costThreshold;

	/*
	 * Accessor functions (NetBeans style)
	 * TODO: May make some of these private to respect agent conventions of autonomy / realistic simulation of humans
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

	//@ScheduledMethod(start = 1, interval = 1, shuffle = true)
	public void step() {

	}


	/**
	 * Constructor
	 */
	public StorageProsumer(CascadeContext context, float[] baseDemand) {
		super(context);
		this.percentageMoveableDemand = (float) RandomHelper.nextDoubleFromTo(0, 0.5);
		this.ticksPerDay = context.getTickPerDay();
		if (baseDemand.length % ticksPerDay != 0)
		{
			System.err.println("baseDemand array not a whole number of days");
			System.err.println("Will be truncated and may cause unexpected behaviour");
		}
		this.baseDemandProfile = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.baseDemandProfile, 0, baseDemand.length);
		//Initialise the smart optimised profile to be the same as base demand
		//smart controller will alter this
		this.smartOptimisedProfile = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0, smartOptimisedProfile.length);
	}

}
