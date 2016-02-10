package uk.ac.dmu.iesd.cascade.agents.aggregators;

import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;

/**
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * 
 */
public class GenericBMPxTraderAggregator extends BMPxTraderAggregator
{

	@Override
	protected String paramStringReport()
	{
		String str = "";
		return str;
	}

	@Override
	public void bizPreStep()
	{
		this.setNetDemand(this.arr_oldPN[this.settlementPeriod]); // TODO: Check
																	// - added
																	// by
																	// Richard
																	// to get
																	// graphical
																	// output.

	}

	@Override
	public void bizStep()
	{
	}

	public GenericBMPxTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxGen,
			double[] baselineProfile)
	{

		super(context, mb, cat, type, maxGen, baselineProfile);
		this.agentName = type.toString() + " Aggregator (Agg" + this.getID() + ")";
		this.nameExplicitlySet = true;
	}

	public GenericBMPxTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxDem,
			double minDem, double[] baselineProfile)
	{
		super(context, mb, cat, type, maxDem, minDem, baselineProfile);
		this.agentName = type.toString() + " Aggregator (Agg" + this.getID() + ")";
		this.nameExplicitlySet = true;
	}

}
