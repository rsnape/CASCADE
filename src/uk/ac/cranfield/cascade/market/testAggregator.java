package uk.ac.cranfield.cascade.market;
import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;

/**
 * 
 */

/**
 * @author grahamfletcher
 *
 */
public class testAggregator extends Aggregator {

	double minGenerationPrice;
	double maxGenerationPrice;
	double maxGeneration;
	double minGeneration;
	
	double maxDemand;
	double maxDemandPrice;
	double minDemand;
	double minDemandPrice;
	
	double currentDemand;
	double currentGeneration;
	
	private double tar = 0.85;
	private double tar1 = 1.0 - tar;
	private double tard = 0.85;
	private double tar1d = 1.0 - tar;
	public testAggregator(double minGenerationPrice,double maxGenerationPrice,
			              double minGeneration, double maxGeneration,
			              double minDemandPrice,double maxDemandPrice,
			              double minDemand, double maxDemand,double attitude)
	{
		super(attitude);
		this.minGenerationPrice = minGenerationPrice;
		this.maxGenerationPrice = maxGenerationPrice;
		this.maxGeneration = maxGeneration;
		this.minGeneration = minGeneration;
		this.maxDemand = maxDemand;
		this.maxDemandPrice = maxDemandPrice;
		this.minDemand = minDemand;
		this.minDemandPrice = minDemandPrice;
		this.currentDemand = minDemand;
		this.currentGeneration = minGeneration;
		
		//tar1 = RandomHelper.nextDoubleFromTo(0.01, 0.3);
		// works tar1 = 0.2;
		tar1 = RandomHelper.nextDoubleFromTo(0.01, 1.0);
		tar1 = 0.15;
		tar1 = tar1 * tar1;
		tar = 1-tar1;
		
		tar1d = tar1 * tar1;
		tard = 1-tar1d;
		
		//System.out.println(minGenerationPrice+" "+ minGeneration);
		//System.out.println(maxGenerationPrice+" "+ maxGeneration);
		//System.out.println(minDemandPrice+" "+minDemand);
		//System.out.println(maxDemandPrice+" "+maxDemand);
		//System.out.println();
	}
	
	
	public ArrayList<Prediction> getPrediction()
	{
		ArrayList<Prediction> result = new ArrayList<Prediction>();
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		double cd = currentDemand;
		double cg = currentGeneration;
		double nextGeneration;
		double nextDemand;
		
		
		
		double powerPriceG = avgBuyPrice*0.1 + avgSellPrice*0.899 + Market.defaultM.getAverageCost() * 0.001;
		double powerPriceD = avgBuyPrice*0.899 + avgSellPrice*0.1 + Market.defaultM.getAverageCost() * 0.001;
		//powerPriceG=Market.defaultM.getAverageCost();
		//powerPriceD=powerPriceG;
		
		//System.out.println("q "+cd+" "+cg+" "+avgPrice);
		
		if(powerPriceG <= minGenerationPrice)
			nextGeneration = 0.0;
		else if (powerPriceG >= maxGenerationPrice)
			nextGeneration = maxGeneration;
		else
			nextGeneration = 		
				(((powerPriceG - minGenerationPrice) / (maxGenerationPrice - minGenerationPrice))
				* (maxGeneration - minGeneration))
				+ minGeneration;
		
		
		if(powerPriceD >= minDemandPrice)
			nextDemand = minDemand;
		else if (powerPriceD <= maxDemandPrice)
			nextDemand = maxDemand;
		else
			nextDemand = 
				(((powerPriceD - minDemandPrice) / (maxDemandPrice - minDemandPrice))
				* (maxDemand - minDemand))
				+ minDemand;
		if(nextDemand < currentDemand * 0.999) nextDemand=currentDemand * 0.999;
		if(nextDemand > currentDemand * 1.001) nextDemand=currentDemand * 1.001;
		//if(nextGeneration < cg * 0.99) nextGeneration=cg * 0.99;
		//if(nextGeneration > cg * 1.01) nextGeneration=cg * 1.01;

		//System.out.println("q currentGen="+cg+" currentDemand="+cd+" price="+powerPriceG+" nextGen="+nextGeneration+" nextDemand="+nextDemand);

		for(int i = currentTick; i < currentTick+Parameters.tradingHorizon +5; i++)
		{
			double demandFactor = Math.sin(((currentTick%48)*1.0/48.0)*2*Math.PI)*0.02+0.99; 
			//demandFactor = 1;

	      	
		      	double sdD = ((i+1-currentTick)/1000.0)  *    (Math.abs(cd-nextDemand)/2); 
			    double sdG = ((i+1-currentTick)/1000.0)  *    (Math.abs(cd-nextGeneration)/2);
			    int j = i-currentTick;
			    if((j <2) ||
					       ((j < 100) && (j%10 == 0)) ||
					       ((j < 1001) && (j%100 == 0)))
					    {
					    	Prediction nsp = new Prediction(i,cg,sdG,cd*demandFactor,sdD);
					    	//System.out.println(cg+" "+sdG+" "+cd+" "+sdD);
					    	result.add(nsp);
					    }
			    
			    cd = cd  * tard + nextDemand * tar1d;
		      	cg = cg  * tar + nextGeneration *tar1;
		      	

			    
			    
			   
		}
		
		return result;	
	}

	public double currentDemand()
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		double demandFactor = Math.sin(((currentTick%48)*1.0/48.0)*2*Math.PI)*0.02+0.99;
		//demandFactor = 1;
		
		return currentDemand*demandFactor;
	}
	
	private int qwert = -1;
	private double qwerty = 0.0;
	public double currentSupply()
	{
	   if (ID > -1)
		return currentGeneration;
	   
	    int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
        if(qwert < currentTick)
        {
        	qwert = currentTick;
        	qwerty = currentGeneration+RandomHelper.nextDoubleFromTo(-27000, +27000);
        }
        return qwerty;
	}
	
	//@ScheduledMethod(start = 0, interval = 1, shuffle = true)
	public void updateSupplyDemand()
	{
		double nextGeneration;
		double nextDemand;
		
		double powerPriceG = avgBuyPrice*0.1 + avgSellPrice*0.899 + Market.defaultM.getAverageCost() * 0.001;
		double powerPriceD = avgBuyPrice*0.899 + avgSellPrice*0.1 + Market.defaultM.getAverageCost() * 0.001;
		//powerPriceG=Market.defaultM.getAverageCost();
		//powerPriceD=powerPriceG;
		
		if(powerPriceG <= minGenerationPrice)
			nextGeneration = 0.0;
		else if (powerPriceG >= maxGenerationPrice)
			nextGeneration = maxGeneration;
		else
			nextGeneration = 		
				(((powerPriceG - minGenerationPrice) / (maxGenerationPrice - minGenerationPrice))
				* (maxGeneration - minGeneration))
				+ minGeneration;
		//if(nextGeneration < currentGeneration * 0.99) nextGeneration=currentGeneration * 0.99;
		//if(nextGeneration > currentGeneration * 1.01) nextGeneration=currentGeneration * 1.01;

		currentGeneration = currentGeneration * tar + nextGeneration * tar1;
		
		
		if(powerPriceD >= minDemandPrice)
			nextDemand = minDemand;
		else if (powerPriceD <= maxDemandPrice)
			nextDemand = maxDemand;
		else
			nextDemand = 
				(((powerPriceD - minDemandPrice) / (maxDemandPrice - minDemandPrice))
				* (maxDemand - minDemand))
				+ minDemand;
		if(nextDemand < currentDemand * 0.999) nextDemand=currentDemand * 0.999;
		if(nextDemand > currentDemand * 1.001) nextDemand=currentDemand * 1.001;

		currentDemand = currentDemand * tard + nextDemand * tar1d;	
		
	
	}

}

