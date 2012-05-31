package uk.ac.dmu.iesd.cascade.base;

import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorFactory;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerFactory;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;


/**
 * Class to find and create all factories in Cascade model (Susceptible to change) 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/16 16:00:00 $
 */


public class FactoryFinder {
	
	public static ProsumerFactory createProsumerFactory(CascadeContext context) {
		return new ProsumerFactory(context);
	}
	
	public static AggregatorFactory createAggregatorFactory(CascadeContext context) {
		return new AggregatorFactory(context);
	}
	
	public static AggregatorFactory createAggregatorTraderFactory(CascadeContext context, MarketMessageBoard mb) {
		return new AggregatorFactory(context, mb);
	}



}
