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

	boolean autoControl;
	String contextName;
	/*
	 * This is net demand, may be +ve (consumption), 0, or 
	 * -ve (generation)
	 */
	float netDemand;
	float[] predictedCustomerDemand;
	int predictedCustomerDemandLength;
	float[] priceSignal;
	int priceSignalLength;
	boolean priceSignalChanged = true;  //set true when we wish to send a new and different price signal.  
	//True by default as it will always be new until the first broadcast
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
	
	public float getCurrentPriceSignal()
	{
		double time = RepastEssentials.GetTickCount();
		return priceSignal[(int) time % priceSignalLength];
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
	@ScheduledMethod(start = 1, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
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

		//This is where we may alter the signal based on the demand
		// In this simple implementation, we simply scale the signal based on deviation of 
		// actual demand from projected demand for use next time round.

		int broadcastLength; // we may choose to broadcast a subset of the price signal, or a repeated pattern
		broadcastLength = priceSignal.length; // but in this case we choose not to
		float predictedInstantaneousDemand = predictedCustomerDemand[(int) time % predictedCustomerDemandLength];
		//This is the real guts - adapt the price signal by a fraction of the 
		//departure of actual demand from predicted
		priceSignal[(int) time % priceSignalLength] = priceSignal[(int) time % priceSignalLength] * ( 1 + ((netDemand - predictedInstantaneousDemand)/predictedInstantaneousDemand));
		priceSignalChanged = true;

		//Here, we simply broadcast the electricity value signal each midnight
		if (timeOfDay == 0) {
			broadcastDemandSignal(customers, time, broadcastLength);
		}    	

		// Return the results.
		return returnValue;

	}
	/*
	 * Logic helper methods
	 */
	private void broadcastDemandSignal(List<ProsumerAgent> broadcastCusts, double time, int broadcastLength) {


		// To avoid computational load (and realistically model a reasonable broadcast strategy)
		// only prepare and transmit the price signal if it has changed.
		if(priceSignalChanged)
		{
			//populate the broadcast signal with the price signal starting from now and continuing for
			//broadcastLength samples - repeating copies of the price signal if necessary to pad the
			//broadcast signal out.
			float[] broadcastSignal= new float[broadcastLength];
			int numCopies = (int) Math.floor((broadcastLength - 1) / priceSignalLength);
			int startIndex = (int) time % priceSignalLength;
			System.arraycopy(priceSignal,startIndex,broadcastSignal,0,priceSignalLength - startIndex);
			for (int i = 1; i <= numCopies; i++)
			{
				int addIndex = (priceSignalLength - startIndex) * i;
				System.arraycopy(priceSignal, 0, broadcastSignal, addIndex, priceSignalLength);
			}

			if (broadcastLength > (((numCopies + 1) * priceSignalLength) - startIndex))
			{
				System.arraycopy(priceSignal, 0, broadcastSignal, ((numCopies + 1) * priceSignalLength) - startIndex, broadcastLength - (((numCopies + 1) * priceSignalLength) - startIndex));
			}

			for (ProsumerAgent a : broadcastCusts){
				a.receiveValueSignal(broadcastSignal, broadcastLength);
			}
		}

		priceSignalChanged = false;
	}
	
    /**
    *
    * This value is used to automatically generate agent identifiers.
    * @field serialVersionUID
    *
    */
   private static final long serialVersionUID = 1L;

   /**
    *
    * This value is used to automatically generate agent identifiers.
    * @field agentIDCounter
    *
    */
   protected static long agentIDCounter = 1;

   /**
    *
    * This value is the agent's identifier.
    * @field agentID
    *
    */
   protected String agentID = "Aggregator " + (agentIDCounter++);
	
    /**
    *
    * This method provides a human-readable name for the agent.
    * @method toString
    *
    */
   @ProbeID()
   public String toString() {
       // Set the default agent identifier.
       String returnValue = this.agentID;
       // Return the results.
       return returnValue;

   }
   
   public String getAgentID()
   {
	   return this.agentID;
   }
	

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
		
		//Very basic configuration of predicted customer demand as 
		// a constant.  We could be more sophisticated than this or 
		// possibly this gives us an aspirational target...
		this.predictedCustomerDemand = new float[ticksPerDay];
		Arrays.fill(this.predictedCustomerDemand, 25);
		this.predictedCustomerDemandLength = ticksPerDay;
	}
}
