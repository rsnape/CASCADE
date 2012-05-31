package uk.ac.dmu.iesd.cascade.agents.aggregators;

import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IPxTrader;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;

/**
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 *
 */
public class GenericBMPxTraderAggregator extends BMPxTraderAggregator  {
	
	protected String paramStringReport(){
		String str="";
		return str;
	}
	
	public void bizPreStep() {
	}
	
	public void bizStep() {		
	}
	
	public GenericBMPxTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type,  double maxGen, double[] baselineProfile) {

		super(context, mb, cat, type, maxGen, baselineProfile);
	}
	
	public GenericBMPxTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type,  double maxDem, double minDem, double[] baselineProfile) {

		super(context, mb, cat, type,maxDem, minDem, baselineProfile);
	}

}
