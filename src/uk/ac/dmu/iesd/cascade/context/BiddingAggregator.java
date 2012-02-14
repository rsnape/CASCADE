
package uk.ac.dmu.iesd.cascade.context;

import java.util.ArrayList;

import uk.ac.cranfield.cascade.market.Aggregator;
import uk.ac.cranfield.cascade.market.Prediction;

/**
 * @author jsnape
 *
 */

/*
public class BiddingAggregator extends Aggregator {
	
	private ArrayList<AggregatorAgent> subAggregators;
	
	*//**
	 * @param attitude
	 *//*
	public BiddingAggregator(double attitude) {
		super(attitude);
		// TODO Auto-generated constructor stub
	}
	
	public void addSubAggregatorAgent(AggregatorAgent thisAgent)
	{
		subAggregators.add(thisAgent);
	}
	
	public void removeSubAggregatorAgent(AggregatorAgent thisAgent)
	{
		subAggregators.remove(thisAgent);
	}

	 (non-Javadoc)
	 * @see uk.ac.cranfield.cascade.market.Aggregator#getPrediction()
	 
	@Override
	public ArrayList<Prediction> getPrediction() {
				
		// TODO Auto-generated method stub
		return null;
	}

	 (non-Javadoc)
	 * @see uk.ac.cranfield.cascade.market.Aggregator#currentSupply()
	 
	@Override
	public double currentSupply() {
		double supply = 0;
		for(AggregatorAgent a : subAggregators)
		{
			supply += a.getSupply();
		}
		
		return supply;
	}

	 (non-Javadoc)
	 * @see uk.ac.cranfield.cascade.market.Aggregator#currentDemand()
	 
	@Override
	public double currentDemand() {
		double demand = 0;
		for(AggregatorAgent a : subAggregators)
		{
			demand += a.getNetDemand();
		}
		
		return demand;
	}
	
	public String toString() {
		
		return ("Bidding Aggregator " + this.ID + " representing sub aggregators " + subAggregators.toString());
		
	}

}
*/