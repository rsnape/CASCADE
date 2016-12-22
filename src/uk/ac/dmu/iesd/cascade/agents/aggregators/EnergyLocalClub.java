/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.collections.ListUtils;

import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;

/**
 * @author Richard
 *
 */
public class EnergyLocalClub extends SupplierCoAdvancedModel
{
	
	double[] usePerDay;
	double[] genPerDay;
	double[] importPerDay;
	double[] exportPerDay;
	
	double[] tariff;
	private double localGenPrice;
	private double originalPrice;
	ArrayList<Double> dayCost;
	
	/*
	 * This function implements the Energy Local sharing model - specifically
	 * Local generation is first netted off within the prosumer i.e.
	 * if someone uses their own PV, that never "makes it" into the Energy Local model.
	 * 
	 * Next, we find all prosumers with net +ve demand > 0.01 kWh per half hour (i.e. non-noise)
	 * 
	 * We do an equal share of generation between all those prosumers and net off demand. HOWEVER!
	 * If a particular household cannot "soak up" that amount, we keep it in the pool and re-run the procedure.
	 * 
	 * Obviously generators effectively do not participate in the sharing rounds as they are already netted off.
	 */
	public ArrayList<Double> calculateShare()
	{
		ArrayList<Double> netDemands = new ArrayList<Double>();
		ArrayList<Double> monetaryBenefits = new ArrayList<Double>();
		double total_to_share = 0;
		int sharer_count = 0;
		
		for (ProsumerAgent c : this.customers)
		{
			double nd = c.getNetDemand();
			netDemands.add(nd);
			monetaryBenefits.add(0.0);
			
			if (nd < 0)
			{
				/* There is something to share here */
				total_to_share += -nd;
			}
			else if (nd > 0.01)
			{
				sharer_count++;
			}
		}
		
		boolean all_gen_used = this.getNetDemand() <= 0;
		int share_rounds =1;
		
		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Trying to share " + total_to_share + " between " + sharer_count + " in round " + share_rounds);
		}
		
		ArrayList<Double> originalNetDemands = new ArrayList<Double>(netDemands); //Create a new copy
		
		while (total_to_share > 0.0001 && sharer_count > 0) //Arbitrary precision constant ;p
		{
			double sharing_proposal = total_to_share / sharer_count;
			ArrayList<Double> postShareNetDemands = new ArrayList<Double>();

			for (double nd : netDemands)
			{
				if (nd >= sharing_proposal)
				{
					postShareNetDemands.add(nd-sharing_proposal);
					total_to_share -= sharing_proposal;
				}
				else
				{
					/*
					 * This share completely covers this household's net Demand and therefore they can
					 * no longer share more and their Net demand is zero
					 */
					postShareNetDemands.add(0.0);
					total_to_share -= nd;
					sharer_count -= 1;
				}
				
				netDemands = postShareNetDemands;
			}
			
			share_rounds += 1;
		}
	
		return (ArrayList<Double>) ListUtils.subtract(originalNetDemands, netDemands);
	}
	
	
	
	public double[] calculateDailySavings()
	{
		double[] retArr = null;
		return retArr;
	}
	
	/*
	 * Add in Energy Local specific functionality here
	 * 
	 * (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCoAdvancedModel#bizPreStep()
	 */
	@Override
	public void bizPreStep() {
		super.bizPreStep();
		
	}
	
	
	/*
	 * Add in Energy Local specific functionality here
	 * 
	 * (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCoAdvancedModel#bizPreStep()
	 */
	@Override
	public void bizStep() {
		super.bizStep();
		this.calculateShare();
	}

	/**
	 * @param context
	 * @param mb
	 * @param cat
	 * @param type
	 * @param maxDem
	 * @param minDem
	 * @param baseDemand
	 */
	public EnergyLocalClub(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxDem, double minDem,
			double[] baseDemand)
	{
		super(context, mb, cat, type, maxDem, minDem, baseDemand);
		this.init();
	}

	/**
	 * @param context
	 */
	public EnergyLocalClub(CascadeContext context)
	{
		super(context);
		this.init();
	}



	/**
	 * 
	 */
	private void init()
	{
		this.originalPrice = 12.0;
        this.tariff = new double[]{7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25,
        		 12,12,12,12,12,12,12,12,12,12,
        		 10,10,10,10,10,10,10,10,10,10,
        		 14,14,14,14,14,14,14,14,
        		 7.25,7.25,7.25,7.25,7.25,7.25,7.25,7.25};		
        this.localGenPrice = 7.0;
	}

}
