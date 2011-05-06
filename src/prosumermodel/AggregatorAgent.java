package prosumermodel;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.Vector;

import prosumermodel.SmartGridConstants.*;

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
 * @author J. Richard Snape
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
	float[] overallSystemDemand;
	int overallSystemDemandLength;
	// priceSignal units are £/MWh which translates to p/kWh if divided by 10
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
	 * This method defines the step behaviour of an aggregator agent
	 * 
	 * Input variables: 	none
	 * 
	 * Return variables: 	boolean returnValue - returns true if the 
	 * 						method executes succesfully
	 ******************/
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public boolean step() {

		// Define the return value variable.
		boolean returnValue = true;

		// Note the simulation time if needed.
		double time = RepastEssentials.GetTickCount();
		int timeOfDay = (int) (time % ticksPerDay);

		List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
		List<RepastEdge> linkages = RepastEssentials.GetOutEdges("Prosumermodel/economicNetwork", this);
		if(SmartGridConstants.debug) {
			System.out.println("Agent " + agentID + " has " + linkages.size() + "links in economic network");
		}
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
		//Set the predicted demand for next day to the sum of the demand at this time today.
		//TODO: This is naive
		
		System.out.println("Setting predicted demand at " + timeOfDay + " to " + sumDemand);
		predictedCustomerDemand[timeOfDay] = sumDemand;

		//TODO I've started too complicated here - first put out flat prices (as per today), then E7, then stepped ToU, then a real dynamic one like this...
		
		//setPriceSignalFlatRate(125f);
		//setPriceSignalEconomySeven(125f, 48f);
		
		// Co-efficients estimated from Figure 4 in Roscoe and Ault
		setPriceSignalRoscoeAndAult(0.0006f, 12f, 40f);
		
		//Here, we simply broadcast the electricity value signal each midnight
		if (timeOfDay == 0) {

			int broadcastLength; // we may choose to broadcast a subset of the price signal, or a repeated pattern
			broadcastLength = priceSignal.length; // but in this case we choose not to

			broadcastDemandSignal(customers, time, broadcastLength);
		}    	

		// Return the results.
		return returnValue;

	}
	
	void setPriceSignalFlatRate(float price)
	{
		float[] oldPrice = priceSignal;
		Arrays.fill(priceSignal, price);
		priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
	}
	
	void setPriceSignalEconomySeven(float highprice, float lowprice)
	{
		float[] oldPrice = priceSignal;
		Arrays.fill(priceSignal, 0, 16, lowprice);
		Arrays.fill(priceSignal, 17, 46, highprice);
		priceSignal[47] = lowprice;
		priceSignalChanged = Arrays.equals(priceSignal, oldPrice);;
	}
	
	void setPriceSignalRoscoeAndAult(float A, float B, float C)
	{
		float price;
		float x;
		
		for (int i = 0; i < priceSignalLength; i++)
		{			
			x = (predictedCustomerDemand[i % ticksPerDay] / 10 ) / (SmartGridConstants.maxSupplyCapacity - SmartGridConstants.biggestGeneratorCapacity);
			price = (float) (A * Math.exp(B * x) + C);
			System.out.println("Price at tick" + i + " is " + price);
			if (price > 1000) 
			{
				price = 1000f;
			}
			priceSignal[i] = price;
		}
		priceSignalChanged = true;
	}
	
	void setPriceSignalExpIncreaseOnOverCapacity(int time)
	{
		//This is where we may alter the signal based on the demand
		// In this simple implementation, we simply scale the signal based on deviation of 
		// actual demand from projected demand for use next time round.
		
		//Define a variable to hold the aggregator's predicted demand at this instant.
		float predictedInstantaneousDemand;
		// There are various things we may want the aggregator to do - e.g. learn predicted instantaneous
		// demand, have a less dynamic but still non-zero predicted demand 
		// or predict zero net demand (i.e. aggregators customer base is predicted self-sufficient
		
		//predictedInstantaneousDemand = predictedCustomerDemand[(int) time % predictedCustomerDemandLength];
		predictedInstantaneousDemand = 0;
		
		if (netDemand > predictedInstantaneousDemand) {
			priceSignal[(int) time % priceSignalLength] = (float) (priceSignal[(int) time % priceSignalLength] * ( 1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			// Now introduce some prediction - it was high today, so moderate tomorrow...
			if (priceSignalLength > ((int) time % priceSignalLength + ticksPerDay))
			{
				priceSignal[(int) time % priceSignalLength + ticksPerDay] = (float) (priceSignal[(int) time % priceSignalLength + ticksPerDay] * ( 1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			}
			priceSignalChanged = true; }
	}
	
	/*
	 * helper methods
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
				// Broadcast signal to all customers - note we simply say that the signal is valid
				// from now currently, in future implementations we may want to be able to insert
				// signals valid at an offset from now.
				if (SmartGridConstants.debug)
				{
					System.out.println("Broadcasting to " + a.agentID);
				}
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
		this.overallSystemDemandLength = baseDemand.length;
		this.priceSignalLength = baseDemand.length;
		
		if (overallSystemDemandLength % ticksPerDay != 0)
		{
			System.err.println("baseDemand array imported to aggregator not a whole number of days");
			System.err.println("May cause unexpected behaviour - unless you intend to repeat the signal within a day");
		}
		this.priceSignal = new float [priceSignalLength];
		this.overallSystemDemand = new float [overallSystemDemandLength];
		System.arraycopy(baseDemand, 0, this.overallSystemDemand, 0, overallSystemDemandLength);
		//Start initially with a flat price signal of 12.5p per kWh
		Arrays.fill(priceSignal,125f);
		
		//Very basic configuration of predicted customer demand as 
		// a constant.  We could be more sophisticated than this or 
		// possibly this gives us an aspirational target...
		this.predictedCustomerDemand = new float[ticksPerDay];
		//Put in a constant predicted demand
		//Arrays.fill(this.predictedCustomerDemand, 5);
		//Or - put in a variable one
		for (int j = 0; j < ticksPerDay; j++)
		{
			this.predictedCustomerDemand[j] = baseDemand[j] / 7000;
		}
		this.predictedCustomerDemandLength = ticksPerDay;
	}
}
