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
public class WindFarmAggregator extends BMPxTraderAggregator
{

	List<ProsumerAgent> customers;
	int timeTick;
	int timeslotOfDay;
	int dayOfWeek;

	/*
	 * To create the wind farm prosumers held by the aggregator
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.dmu.iesd.cascade.context.AggregatorAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * This method returns the list of wind farms (prosumers) in the economic
	 * network of this aggregator
	 * 
	 * @return List of customers of type <tt> ProsumerAgent</tt>
	 */
	private List<ProsumerAgent> getCustomersList()
	{
		List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
		// TODO: This currently adds every wind farm prosumer to the windfarm
		// aggregator.
		// This needs to be recoded to allow for multiple aggregators owning
		// different wind farms.
		Network windFarmNet = this.mainContext.getEconomicNetwork();
		Iterable<RepastEdge> iter = windFarmNet.getEdges();
		/*
		 * if(Consts.DEBUG) {
		 * this.mainContext.logger.debug(this.getAgentName()+" "
		 * +this.toString()+ " has "+ windFarmNet.size() +
		 * " links in wind farm network"); }
		 */
		for (RepastEdge edge : iter)
		{
			Object linkSource = edge.getTarget();
			if (linkSource instanceof ProsumerAgent)
			{
				// This network is currently just that of the wind farm(s)
				// linked to the
				customers.add((ProsumerAgent) linkSource);
			}
			else
			{
				throw (new WrongCustomerTypeException(linkSource));
			}
		}
		return customers;

	}

	private double calculateAndSetNetDemand(List customersList)
	{

		List<ProsumerAgent> customers = customersList;
		double sumDemand = 0;
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" customers list size: " + customers.size());
		}
		for (ProsumerAgent a : customers)
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(" id: " + a.getAgentID() + " ND: " + a.getNetDemand());
			}
			if (a instanceof WindGeneratorProsumer)
			{
				sumDemand = sumDemand + a.getNetDemand();
			}
			// sum_e = sum_e+a.getElasticityFactor();
		}
		// The Aggregators deal in MW, but the Wind Farm Prosumers calculate
		// generation in Watts.
		// A conversion is therefore carried out here.
		this.setNetDemand((sumDemand / 1E6));
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("RECO:: calculateAndSetNetDemand: NetDemand set to: " + sumDemand);
		}

		return sumDemand;
	}

	@Override
	public void marketPreStep()
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" initializeMarketStep (SupplierCo) " + this.id);
		}
		int settlementPeriod = this.mainContext.getSettlementPeriod();

		switch (settlementPeriod)
		{

		case 13:
			if (this.mainContext.isMarketFirstDay())
			{
				System.arraycopy(ArrayUtils.negate(this.arr_i_B), 0, this.arr_PN, 0, this.arr_i_B.length);
			}
			else
			{
				// System.arraycopy(ArrayUtils.negate(arr_hist_day_D), 0,
				// arr_PN, 0, arr_hist_day_D.length);
				System.arraycopy(ArrayUtils.negate(this.getDayNetDemands()), 0, this.arr_PN, 0, this.getDayNetDemands().length);
			}
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.dmu.iesd.cascade.context.AggregatorAgent#step_pre()
	 */
	@Override
	public void bizPreStep()
	{
		// TODO Auto-generated method stub
if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(" ============ WindFarmAggregator pre_step ========= DayCount: " + this.mainContext.getDayCount()
				+ ",Timeslot: " + this.mainContext.getTimeslotOfDay() + ",TickCount: " + this.mainContext.getTickCount());
}
		this.timeTick = this.mainContext.getTickCount();
		this.timeslotOfDay = this.mainContext.getTimeslotOfDay();
		this.customers = this.getCustomersList();

		if (this.mainContext.getDayCount() == Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
		{
			this.arr_i_B = new double[]
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0 };
		}

		/*
		 * TODO: Inform wind farms what they should be generating..
		 */

		// Calculate and set the maximum capacity of all the wind farms and the
		// minimum capacity (i.e. 1 wind turbine on at one wind farm)
		// Also calculate and set the ramp rate - not sure how best to do this
		// (max - min capacity / num of turbines perhaps??)

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.dmu.iesd.cascade.context.AggregatorAgent#step()
	 */
	@Override
	public void bizStep()
	{
		// TODO Auto-generated method stub
		this.calculateAndSetNetDemand(this.customers);

		/*
		 * if (mainContext.getTickCount() % 48 == 47) { System.out.print("WD"+
		 * mainContext.getTickCount() % 48 + ", ");
		 * 
		 * for (int i=0; i<this.arr_day_D.length; i++) {
		 * System.out.print(this.arr_day_D[i] + ","); }
		 * this.mainContext.logger.debug(""); }
		 */
	}

	/**
	 * Constructs a WindFarmAggregator agent with the context in which it is
	 * created.
	 * 
	 * @param context
	 *            the context in which this agent is situated
	 */
	public WindFarmAggregator(CascadeContext context, MarketMessageBoard messageBoard, double maxGen)
	{

		super(context, messageBoard, BMU_CATEGORY.GEN_T, BMU_TYPE.GEN_WIND, maxGen);
		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Wind Farm Aggregator created ");
		}

		this.ticksPerDay = context.getNbOfTickPerDay();

		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("WindFarmAggregator ticksPerDay " + this.ticksPerDay);
		}

		this.arr_day_D = new double[this.ticksPerDay];

	}

}
