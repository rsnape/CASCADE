/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.util.List;
import java.util.Vector;

import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.WindGeneratorProsumer;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.WrongCustomerTypeException;

/**
 * @author ssmith00
 *
 */
public class WindFarmAggregator extends BMPxTraderAggregator {
	
	List<ProsumerAgent> customers;
	int timeTick;
	int timeslotOfDay;
	int dayOfWeek;

	
	/*
	 * To create the wind farm prosumers held by the aggregator
	 */
	
	
	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.context.AggregatorAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * This method returns the list of wind farms (prosumers) 
	 * in the economic network of this aggregator
	 * @return List of customers of type <tt> ProsumerAgent</tt>  
	 */
	private List<ProsumerAgent> getCustomersList() {
		List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
		// TODO: This currently adds every wind farm prosumer to the windfarm aggregator.
		// This needs to be recoded to allow for multiple aggregators owning different wind farms.
		Network windFarmNet = this.mainContext.getEconomicNetwork();
		Iterable<RepastEdge> iter = windFarmNet.getEdges();
/*		if(Consts.DEBUG) {
				this.mainContext.logger.debug(this.getAgentName()+" " +this.toString()+ " has "+ windFarmNet.size() + " links in wind farm network");
			}
*/		
		for (RepastEdge edge : iter) {
			Object linkSource = edge.getTarget();
			if (linkSource instanceof ProsumerAgent){
				//This network is currently just that of the wind farm(s) linked to the 
				customers.add((ProsumerAgent) linkSource);	    		
			}
			else
			{
				throw (new WrongCustomerTypeException(linkSource));
			}
		}
		return customers;
		
	}
	
	private double calculateAndSetNetDemand(List customersList) {	

		List<ProsumerAgent> customers = customersList;
		double sumDemand = 0;
		this.mainContext.logger.trace(" customers list size: "+customers.size());
		for (ProsumerAgent a : customers)	{
			this.mainContext.logger.trace(" id: "+a.agentID+" ND: "+a.getNetDemand());
			if (a instanceof WindGeneratorProsumer){
				sumDemand = sumDemand + a.getNetDemand();
			}
			//sum_e = sum_e+a.getElasticityFactor();
		}
		//The Aggregators deal in MW, but the Wind Farm Prosumers calculate generation in Watts.
		//A conversion is therefore carried out here.
		setNetDemand((sumDemand/1E6));
		this.mainContext.logger.trace("RECO:: calculateAndSetNetDemand: NetDemand set to: " + sumDemand);
		
		return sumDemand;
	}
	
	/**
	 * This method broadcasts a passed signal array (of double values) to a list of passed customers (e.g. Prosumers)
	 * @param signalArr signal (array of real/double numbers) to be broadcasted
	 * @param customerList the list of customers (of ProsumerAgent type)
	 * @return true if signal has been sent and received successfully by the receiver, false otherwise 
	 */
	private boolean broadcastSignalToCustomers(double[] signalArr, List<ProsumerAgent> customerList) {

		boolean isSignalSentSuccessfully = false;
		//Next line only needed for GUI output at this stage
		this.priceSignal = new double[signalArr.length];
		System.arraycopy(signalArr, 0, this.priceSignal, 0, signalArr.length);
		//List  aList = broadcasteesList;
		//List <ProsumerAgent> paList = aList;	

		for (ProsumerAgent agent : customerList){			
			isSignalSentSuccessfully = agent.receiveValueSignal(signalArr, signalArr.length);
		}
		return isSignalSentSuccessfully;
	}
	
	public void marketPreStep() {
		this.mainContext.logger.trace(" initializeMarketStep (SupplierCo) "+this.id);
		int settlementPeriod = mainContext.getSettlementPeriod();
	
		switch (settlementPeriod) {
		
		case 13: 
			if (mainContext.isMarketFirstDay())
				System.arraycopy(ArrayUtils.negate(arr_i_B), 0, arr_PN, 0, arr_i_B.length);
			else 
				//System.arraycopy(ArrayUtils.negate(arr_hist_day_D), 0, arr_PN, 0, arr_hist_day_D.length);
		    	System.arraycopy(ArrayUtils.negate(this.getDayNetDemands()), 0, arr_PN, 0, this.getDayNetDemands().length);
			break;
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.context.AggregatorAgent#step_pre()
	 */
	@Override
	public void bizPreStep() {
		// TODO Auto-generated method stub
this.mainContext.logger.trace(" ============ WindFarmAggregator pre_step ========= DayCount: "+ mainContext.getDayCount()+",Timeslot: "+mainContext.getTimeslotOfDay()+",TickCount: "+mainContext.getTickCount() );
		timeTick = mainContext.getTickCount();	
		timeslotOfDay = mainContext.getTimeslotOfDay();
		customers = getCustomersList();
		
		if (mainContext.getDayCount() == Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) {
			arr_i_B = new double [] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		}
		
		/*
		 * TODO: Inform wind farms what they should be generating..
		 */
		
		//Calculate and set the maximum capacity of all the wind farms and the minimum capacity (i.e. 1 wind turbine on at one wind farm)
		//Also calculate and set the ramp rate - not sure how best to do this (max - min capacity / num of turbines perhaps??)

	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.context.AggregatorAgent#step()
	 */
	@Override
	public void bizStep() {
		// TODO Auto-generated method stub
		calculateAndSetNetDemand(customers);
		
/*		if (mainContext.getTickCount() % 48 == 47) {
			System.out.print("WD"+ mainContext.getTickCount() % 48 + ", ");
			
			for (int i=0; i<this.arr_day_D.length; i++) {
				System.out.print(this.arr_day_D[i] + ",");
				}
			this.mainContext.logger.debug("");
		}
*/		
	}

	/**
	 * Constructs a WindFarmAggregator agent with the context in which it is created. 
	 * 
	 * @param context the context in which this agent is situated 
	 */
	public WindFarmAggregator(CascadeContext context, MarketMessageBoard messageBoard, double maxGen) {

		super(context, messageBoard, BMU_CATEGORY.GEN_T, BMU_TYPE.GEN_WIND, maxGen);
		this.mainContext.logger.debug("Wind Farm Aggregator created ");

		this.ticksPerDay = context.getNbOfTickPerDay();

		this.mainContext.logger.debug("WindFarmAggregator ticksPerDay "+ ticksPerDay);
		
		this.arr_day_D = new double[ticksPerDay];
		
	}


}
