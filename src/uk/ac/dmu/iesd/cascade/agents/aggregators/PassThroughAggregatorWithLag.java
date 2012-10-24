/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import repast.simphony.essentials.RepastEssentials;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.NetworkEvent;
import repast.simphony.space.graph.NetworkListener;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.projection.ProjectionEvent;
import repast.simphony.space.projection.ProjectionEvent.Type;
import repast.simphony.space.projection.ProjectionListener;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.market.astem.test.TestHelper;
import uk.ac.dmu.iesd.cascade.util.WrongCustomerTypeException;

/**
 * @author jsnape
 *
 */
public class PassThroughAggregatorWithLag extends BMPxTraderAggregator implements ProjectionListener  {
	
	/**
	 * Parameters characterising this aggregator's behaviour
	 */
	double[] laggedPrice;
	
	protected String paramStringReport(){
		String str="";
		return str;
	}	
	
	public double getCurrPrice()
	{
		return this.laggedPrice[0];
	}
	
	public String priceTitle()
	{
		return this.getAgentName()+" current price";
	}
	
	public String demandTitle()
	{
		return this.getAgentName()+" current demand";
	}
	
	private boolean broadcastSignalToCustomers(double val, List<ProsumerAgent> customerList) {
		double[] tmp = new double[]{val};
		return broadcastSignalToCustomers(tmp,customerList);
	}
	/**
	 * This method broadcasts a passed signal array (of double values) to a list of passed customers (e.g. Prosumers)
	 * @param signalArr signal (array of real/double numbers) to be broadcasted
	 * @param customerList the list of customers (of ProsumerAgent type)
	 * @return true if signal has been sent and received successfully by the receiver, false otherwise 
	 */
	private boolean broadcastSignalToCustomers(double[] signalArr, List<ProsumerAgent> customerList) {

		boolean isSignalSentSuccessfully = false;
		boolean allSignalsSentSuccesfully = true;
		//Next line only needed for GUI output at this stage
		this.priceSignal = new double[signalArr.length];
		System.arraycopy(signalArr, 0, this.priceSignal, 0, signalArr.length);
		//List  aList = broadcasteesList;
		//List <ProsumerAgent> paList = aList;	

		for (ProsumerAgent agent : customerList){			
			isSignalSentSuccessfully = agent.receiveValueSignal(signalArr, signalArr.length);
			if (!isSignalSentSuccessfully)
			{
				allSignalsSentSuccesfully = false;
			}
		}
		return allSignalsSentSuccesfully;
	}
	
	public void bizPreStep() {
		
		System.out.println("At tick "+RepastEssentials.GetTickCount()+" demand = "+this.getNetDemand());
		ArrayList<ProsumerAgent> customers = getCustomersList();
		broadcastSignalToCustomers(this.getCurrPrice(), customers);
		for (int i = 0; i < this.laggedPrice.length - 1; i++)
		{
			this.laggedPrice[i] = this.laggedPrice[i+1];
			
		}
		double[] mip = this.messageBoard.getMIP();
		if (mip != null)
		{
		this.laggedPrice[this.laggedPrice.length - 1] = mip[this.mainContext.getTickCount() % this.mainContext.ticksPerDay];
		System.out.println("Added MIP to lagged array = "+mip[this.mainContext.getTickCount() % this.mainContext.ticksPerDay]);
		System.out.println("BMP = "+this.messageBoard.getBMP());
		}
	}
	
	public void bizStep() {
		ArrayList<? extends ProsumerAgent> customers = getCustomersList();
		float totalDemand = 0;
		for (ProsumerAgent c : customers)
		{
			totalDemand += c.getNetDemand();
		}
		System.out.println(this.getAgentName() + this.getID()+" has total demand "+totalDemand);

		this.setNetDemand(totalDemand);
	}
	
	public PassThroughAggregatorWithLag(CascadeContext context, MarketMessageBoard mb, double maxGen, double[] baselineProfile) {
		//Default to zero lag
		this(context, mb, maxGen, baselineProfile,0);
	}
	
	public PassThroughAggregatorWithLag(CascadeContext context, MarketMessageBoard mb, double maxDem, double minDem, double[] baselineProfile) {
		//default to zero lag
		this(context, mb, maxDem, minDem, baselineProfile,0);
	}
	
	public PassThroughAggregatorWithLag(CascadeContext context, MarketMessageBoard mb, double maxGen, double[] baselineProfile, int lag) {
		super(context, mb, BMU_CATEGORY.DEM_S, BMU_TYPE.DEM_SMALL, maxGen, baselineProfile);
		this.laggedPrice = new double [lag+1];
		Arrays.fill(this.laggedPrice, 125);
		context.add(this);
	}
	
	public PassThroughAggregatorWithLag(CascadeContext context, MarketMessageBoard mb, double maxDem, double minDem, double[] baselineProfile, int lag) {
		super(context, mb, BMU_CATEGORY.DEM_S, BMU_TYPE.DEM_SMALL, maxDem, minDem, baselineProfile);
		this.laggedPrice = new double [lag+1];
		Arrays.fill(this.laggedPrice, 40);
		context.add(this);
	}

	ArrayList<ProsumerAgent> c;
	boolean custListDirty = true;
	
	/**
	 * @return ArrayList of ProsumerAgents that are this aggregators' customers
	 */
	private ArrayList<ProsumerAgent> getCustomersList() {
		
		if (!custListDirty)
		{
			return c;
		}
		
		c = new ArrayList<ProsumerAgent>();
		Network economicNet = this.mainContext.getEconomicNetwork();
		
		Iterable<RepastEdge> iter = economicNet.getEdges(this);
		if(Consts.DEBUG) {
			//System.out.println(This.class+" " +this.toString()+ " has "+ economicNet.size() + " links in economic network");
		}

		for (RepastEdge edge : iter) {
			Object linkSource = edge.getTarget();
			//if (Consts.DEBUG) System.out.println("RECO linkSource " + linkSource);
			if (linkSource instanceof ProsumerAgent){
				c.add((ProsumerAgent) linkSource);    		
			}
			else	{
				throw (new WrongCustomerTypeException(linkSource));
			}
		}
		custListDirty = false;
		
		return c;
	}

	/* (non-Javadoc)
	 * @see repast.simphony.space.projection.ProjectionListener#projectionEventOccurred(repast.simphony.space.projection.ProjectionEvent)
	 */
	@Override
	public void projectionEventOccurred(ProjectionEvent evt) {
		if (evt.getType()== Type.EDGE_ADDED || evt.getType()==Type.EDGE_REMOVED)
		{
			RepastEdge subject = (RepastEdge) evt.getSubject();
			if (subject.getSource()==this || subject.getTarget()==this)
			{
				custListDirty = true;
			}
		}
			
		
	}
	
	public String getINDMARTitle()
	{
		return "INDMAR";
	}
	
	public String getIMBALTitle()
	{
		return "IMBAL";
	}
	
	public double getINDMAR()
	{
		return messageBoard.getINDMAR()[this.mainContext.getTickCount() % this.mainContext.ticksPerDay];
		
	}
	
	public double getIMBAL()
	{
		return messageBoard.getIMBAL()[this.mainContext.getTickCount() % this.mainContext.ticksPerDay];
		
	}


}