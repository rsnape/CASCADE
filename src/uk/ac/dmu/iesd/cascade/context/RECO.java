/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import static repast.simphony.essentials.RepastEssentials.FindNetwork;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import uk.ac.dmu.iesd.cascade.Consts;

/**
 * A <em>RECO</em> or a Retail Company is a concrete object that represents 
 * a commercial/business electricity/energy company involved in retail trade with
 * prosumer agents (<code>ProsumerAgent</code>), such as household prosumers (<code>HouseholdProsumer</code>)
 * In other words, a <code>RECO</code> provides electricity/energy to certain types of prosumers (e.g. households)
 * It will be also involved in wholesale trade market.<p>
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/18 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history)
 * 
 * 1.0 -  created the concrete split of categories of prosumer from the abstract class representing all prosumers
 * 
 */

public class RECO extends AggregatorAgent{

	/**
	 * the aggregator agent's base name  
	 **/	
	protected static String agentBaseName = "RECO";
	
	/**
	 * Constructs a RECO agent with the context in which is created and its
	 * base demand.
	 * @param context the context in which this agent is situated
	 * @param baseDemand an array containing the base demand  
	 */
	public RECO(CascadeContext context, float[] baseDemand) {

		super(context);
		//System.out.println("RECO created ");
		this.ticksPerDay = context.getTickPerDay();
		//this.contextName = myContext;
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
		// a Conssant.  We could be more sophisticated than this or 
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

	/**
	 * This method defines how this object behaves (what it does)
	 * at at a given scheduled time throughout the simulation. 
	 */
	//@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void step() {

		// Define the return value variable.
		//boolean returnValue = true;

		// Note the simulation time if needed.
		double time = RepastEssentials.GetTickCount();
		int timeOfDay = (int) (time % ticksPerDay);

		List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
		//List<RepastEdge> linkages = RepastEssentials.GetOutEdges("CascadeContextMain/economicNetwork", this); //Ideally this must be avoided, changing the context name, will create a bug difficult to find
		Network economicNet = this.mainContext.getEconomicNetwork();
		Iterable<RepastEdge> iter = economicNet.getEdges();


		if(Consts.DEBUG) {
			//System.out.println("Agent " + agentID + " has " + linkages.size() + "links in economic network");

		}

		for (RepastEdge edge : iter) {
			Object linkSource = edge.getTarget();
			//System.out.println("RECO linkSource " + linkSource);
			if (linkSource instanceof ProsumerAgent){
				customers.add((ProsumerAgent) linkSource);    		
			}
			else
			{
				throw (new WrongCustomerTypeException(linkSource));
			}
		}

		/*
		for (RepastEdge edge : linkages) {
			Object linkSource = edge.getTarget();
			if (linkSource instanceof ProsumerAgent){
				customers.add((ProsumerAgent) linkSource);    		
			}
			else
			{
				throw (new WrongCustomerTypeException(linkSource));
			}
		} */

		float sumDemand = 0;
		for (ProsumerAgent a : customers)
		{
			sumDemand = sumDemand + a.getNetDemand();
		}

		setNetDemand(sumDemand);
		//Set the predicted demand for next day to the sum of the demand at this time today.
		//TODO: This is naive

		//System.out.println("Setting predicted demand at " + timeOfDay + " to " + sumDemand);
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
		
		//----- Babak Network test ----------------------------------------------
		Network costumerNetwork = FindNetwork("BabakTestNetwork");
		
		costumerNetwork.getEdges(this);
		//System.out.println("costumerNework: "+ costumerNetwork.getName());
		Iterable costumersIter = costumerNetwork.getEdges(this);
		
		//System.out.println("costumerIter: "+ costumersIter.toString());
		for (Object thisConn: costumersIter)
		{
			RepastEdge linkEdge = ((RepastEdge) thisConn);
			HouseholdProsumer hhPro = (HouseholdProsumer) linkEdge.getTarget();
			System.out.println(this.toString()+ " costumer is: "+ hhPro.toString()); 
			if (hhPro.getAgentName().matches("HH-Pro1")) 
				costumerNetwork.removeEdge(linkEdge);
			
		}
		System.out.println("===================");
		// -- End of test --------------------------------------------------------
	}

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
				if (Consts.DEBUG)
				{
					//System.out.println("Broadcasting to " + a.sAgentID);
				}
				a.receiveValueSignal(broadcastSignal, broadcastLength);
			}
		}

		priceSignalChanged = false;
	}


}
