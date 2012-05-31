
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IBMTrader;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;


/**
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/05/16
 */

public abstract class BMTraderAggregator extends AggregatorAgent implements IBMTrader{
	
	
	protected MarketMessageBoard messageBoard;
	
	public BMTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxGen, double minDem) {
		super(context, cat, type, maxGen, minDem);
		messageBoard = mb;
	}
}
