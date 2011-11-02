/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import java.util.ArrayList;

/**
 * @author jsnape
 *
 */
public abstract class MarketAggregator {
		
	protected ArrayList<PowerPrediction> predictedDemand;
	protected ArrayList<PowerPrediction> predictedGeneration;
	
	protected ArrayList<PowerPrice> sellPrice;
	protected ArrayList<PowerPrice> buyPrice;
	
	protected void addDemandPrediction(PowerPrediction prediction)
	{
		this.predictedDemand.add(prediction);
	}

	protected void addGenerationPrediction(PowerPrediction prediction)
	{
		this.predictedGeneration.add(prediction);
	}
	
	protected void addSellPrice(PowerPrice price)
	{
		this.sellPrice.add(price);
	}
	
	protected void addBuyPrice(PowerPrice price)
	{
		this.buyPrice.add(price);
	}

}
