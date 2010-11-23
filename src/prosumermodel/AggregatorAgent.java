package prosumermodel;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.Vector;

import javax.measure.unit.*;
import org.jscience.mathematics.number.*;
import org.jscience.mathematics.vector.*;
import org.jscience.physics.amount.*;
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
 * @author jsnape
 * @version $Revision: 1.0 $ $Date: 2010/11/17 17:00:00 $
 * 
 */
public class AggregatorAgent {

/**
	 * @param Parameters for this setup
	 */
	public AggregatorAgent(String myContext, float[] baseDemand, Parameters parm) {
		super();
		this.ticksPerDay = (Integer) parm.getValue("ticksPerDay");
		this.contextName = myContext;
		this.priceSignalLength = baseDemand.length;
		if (priceSignalLength % ticksPerDay != 0)
		{
			System.err.println("baseDemand array not a whole number of days");
			System.err.println("Will be truncated and may cause unexpected behaviour");
		}
		this.priceSignal = new float [priceSignalLength];
		System.arraycopy(baseDemand, 0, this.priceSignal, 0, priceSignalLength);
	}
boolean autoControl;
String contextName;
/*
 * This is net demand, may be +ve (consumption), 0, or 
 * -ve (generation)
 */
float netDemand;
float[] priceSignal;
int priceSignalLength;
int ticksPerDay;

/*
 * Accessor functions (NetBeans style)
 */

public float getNetDemand()
{
	return netDemand;
}

public void setNetDemand(float newDemand)
{
	netDemand = newDemand;
}

/*
 * Communication functions
 */

/*
 * This method receives the centralised value signal
 * and stores it to the Prosumer's memory.
 */
public boolean receiveValueSignal()
{
	boolean success = true;
		
	return success;
}

public boolean receiveInfluence()
{
	boolean success = true;
		
	return success;
}

/*
 * Step behaviour
 */

/******************
 * This method defines the step behaviour of a prosumer agent
 * 
 * Input variables: 	none
 * 
 * Return variables: 	boolean returnValue - returns true if the 
 * 						method executes succesfully
 ******************/
@ScheduledMethod(start = 1, interval = 1, shuffle = true)
public boolean step() {

    // Define the return value variable.
    boolean returnValue = true;

    // Note the simulation time if needed.
    double time = RepastEssentials.GetTickCount();
    int timeOfDay = (int) (time % ticksPerDay);
    
    List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
    List<RepastEdge> linkages = RepastEssentials.GetInEdges("Prosumermodel/economicNetwork", this);
    for (RepastEdge edge : linkages) {
    	Object linkSource = edge.getTarget();
    	if (linkSource instanceof ProsumerAgent){
    		customers.add((ProsumerAgent) linkSource);    		
    	}
    	else
    	{
    		throw (new WrongCustomerTypeException(linkSource));
    	}
    }
    
    float sumDemand = 0;
    for (ProsumerAgent a : customers)
    {
    	sumDemand = sumDemand + a.getNetDemand();
    }
    
    setNetDemand(sumDemand);
   
    //This is where we may alter the signal based on the demand if we go realtime
    
    //Here, we simply broadcast the electricity value signal each midnight
    if (timeOfDay == 0) {
    	broadcastDemandSignal(customers);
    }    	
    
    // Return the results.
    return returnValue;

}
/*
 * Logic helper methods
 */
private void broadcastDemandSignal(List<ProsumerAgent> broadcastCusts) {
	for (ProsumerAgent a : broadcastCusts){
		a.receiveValueSignal(priceSignal, priceSignalLength);
	}
}
}
