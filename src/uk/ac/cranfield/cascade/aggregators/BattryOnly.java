/**
 * 
 */
package uk.ac.cranfield.cascade.aggregators;

import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import uk.ac.cranfield.cascade.market.Aggregator;
import uk.ac.cranfield.cascade.market.Market;
import uk.ac.cranfield.cascade.market.Parameters;
import uk.ac.cranfield.cascade.market.Prediction;

/**
 * @author grahamfletcher
 *
 */
public class BattryOnly extends Aggregator{

	//This class implements an aggrigator that has a simple linear demand profile, but no generation
	//it has access to a battry store to buffer demand/supply

	
	private double maxDemand;
	/**
	 * @return the currentBattryPower
	 */
	public double getCurrentBattryPower() {
		return currentBattryPower/1000;
	}

	/**
	 * @param currentBattryPower the currentBattryPower to set
	 */
	public void setCurrentBattryPower(double currentBattryPower) {
		this.currentBattryPower = currentBattryPower;
	}

	/**
	 * @return the stored
	 */
	public double getStored() {
		return stored/100000;
	}


	private double maxDemandPrice;
	private double minDemand;
	private double minDemandPrice;
	private double currentDemand;
	private double currentBattryPower;

	private double tar1d, tard; //how fast do the gemoetric averages for demand change.
	private double stored = 0.0;
	private double storageCapacity;
	private double battryPower = 0.0;
	private double battryEfficiency = 0.8;
	private int battyOnAt=600;
	public double spread=2.0;
	private double[] battryUsage = new double[Parameters.tradingHorizon];
	private ArrayList<Prediction> demandPrediction;
	
	
	public BattryOnly (double minDemandPrice,double maxDemandPrice,
            			double minDemand, double maxDemand,
						double storageCapacity, double battryPower){
		super(0.0);

		this.maxDemand = maxDemand;
		this.maxDemandPrice = maxDemandPrice;
		this.minDemand = minDemand;
		this.minDemandPrice = minDemandPrice;
		this.currentDemand = minDemand;
		this.storageCapacity = storageCapacity;
		this.battryPower = battryPower;
		
		
		//tar1d = RandomHelper.nextDoubleFromTo(0.01, 1.0);
		tar1d = 0.15;
		tar1d = tar1d * tar1d *tar1d * tar1d;
		tard = 1-tar1d;
		
		//Zero forward battry usage figures
		for(int i = 0; i < Parameters.tradingHorizon; i++)
			battryUsage[i] = 0.0;
	}
	
	public ArrayList<Prediction> getPrediction()
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();

		if(demandPrediction == null)
			return new ArrayList<Prediction>();
		
		//System.out.println("kk");
		
		ArrayList<Prediction> p = (ArrayList<Prediction>)demandPrediction.clone();
		
		int i = 0;
		for(Prediction pp : p)
		{
			if(i + currentTick > battyOnAt)
			{
				double balance =pp.prediction.value - pp.prediction2.value - battryUsage[i++];
				if(balance < 0)
				{
					pp.prediction.value = 0.0;
					pp.prediction2.value = -balance;
				}
				else
				{
					pp.prediction2.value = 0.0;
					pp.prediction.value = -balance;
				}
				
			}
		}
		return p;
	}
	
	public void updateSupplyDemand()
	{
		//System.out.println("updateSupplyDemand");
		
		demandPrediction = new ArrayList<Prediction>();
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		
		
		//Calculate the price point on which future demand will be based		
		double powerPriceD = avgBuyPrice*0.899 + avgSellPrice*0.1 + Market.defaultM.getAverageCost() * 0.001;

		//System.out.print(toString()+" "+powerPriceD);
		
		//Calulate the eventual demand based on that price point
		double nextDemand;
		if(powerPriceD >= minDemandPrice)
			nextDemand = minDemand;
		else if (powerPriceD <= maxDemandPrice)
			nextDemand = maxDemand;
		else
			nextDemand = 
				(((powerPriceD - minDemandPrice) / (maxDemandPrice - minDemandPrice))
				* (maxDemand - minDemand))
				+ minDemand+RandomHelper.nextDoubleFromTo(-100, 100);
		if(nextDemand < currentDemand * 0.999) nextDemand=currentDemand * 0.999;
		if(nextDemand > currentDemand * 1.001) nextDemand=currentDemand * 1.001;

		//System.out.println(currentDemand+" "+nextDemand);
		
		//calculate the predicted battry power usage and the forward predictions for demand
		double cStored = stored;
		double cDemand = currentDemand;
		for(int i = 0; i < Parameters.tradingHorizon; i++)
		{
			cDemand =cDemand * tard + nextDemand * tar1d;
			
			//Demand has a sinusoidal element to it
			double demandFactor = Math.sin(((currentTick%48)*1.0/48.0)*2*Math.PI)*0.02+0.99; 
			
			//estimate the standard devieation
			double sdD = (i/200.0); 
			
			//Estimate the demand
			double thisDemand = cDemand;
			double thisSupply = 0.0;
			
			//Estimate any supply
			if(thisDemand < 0.0)
			{
				thisDemand = 0.0;
				thisSupply = -thisDemand;
			}
			demandPrediction.add( new Prediction(i,thisSupply,thisSupply*sdD,thisDemand,thisDemand*sdD));
		
			
			
			
			double incr;
			if( i < Parameters.tradingHorizon -1 )
				incr = battryUsage[i+1];
			else
				incr = 0.0;
			
			//If the price is predicted to be high then use some battry power
			if((buyPPUpredictions.get(i) > avgBuyPriceLT + spread))
				incr -=  battryPower/48;
			
			//If the price is low then buy some power
			//If the price is low enough then we'll want something for the battry
			if((buyPPUpredictions.get(i) < avgBuyPriceLT - spread))
				incr +=  battryPower/148;
			
			//Limit the power of the battry
			if (incr > battryPower) incr = battryPower;
			if (incr < -battryPower) incr = -battryPower;
			
			//Limit the energy in the battry
			if (cStored + incr < 0) incr = -cStored;
			if (cStored + incr > storageCapacity) incr = storageCapacity - cStored;
			
			//Dont use the battry if the demand wasn't there
			if(incr < -thisDemand)
				incr = -thisDemand;
			
			//Extra rules based on the currentPrice for time stamp 0
			if(i==0)
			{
				if((incr <0) && (buyPrice < avgBuyPriceLT - spread))
					incr = 0;
				
				if((incr > 0) && (buyPrice > avgBuyPriceLT + spread))
					incr = 0;
			}
			
			cDemand = demandPrediction.get(0).prediction2.value - demandPrediction.get(0).prediction.value;
			cStored += incr;
			battryUsage[i]=incr;
			//System.out.print(""+incr+",");
		}
		//System.out.println();
		//Update the energy in the battry 
		if(currentTick > battyOnAt)
		    stored += battryUsage[0];	
		
		//System.out.println("updateSupplyDemand XX");
	}
	
	public double currentDemand()
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		double demandFactor = Math.sin(((currentTick%48)*1.0/48.0)*2*Math.PI)*0.02+0.99;
		
        double balance = (currentDemand * demandFactor);
        if(currentTick > battyOnAt) balance += battryUsage[0];
        if(balance > 0) return balance;
        else
        	return 0.0;
	}
	

	public double currentSupply()
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		double demandFactor = Math.sin(((currentTick%48)*1.0/48.0)*2*Math.PI)*0.02+0.99;
		
		double balance = (currentDemand * demandFactor);
        if(currentTick > battyOnAt) balance += battryUsage[0];
        if(balance < 0) return -balance;
        else
        	return 0.0;
	}
}
