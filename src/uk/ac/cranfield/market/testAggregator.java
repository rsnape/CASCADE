package uk.ac.cranfield.market;
import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

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
	
	public testAggregator(double minGenerationPrice,double maxGenerationPrice,double maxGeneration,
						double minGeneration, double maxDemand, double maxDemandPrice,
						double minDemand,double minDemandPrice){
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
		
		System.out.println(minGenerationPrice+" "+ minGeneration);
		//System.out.println(maxGenerationPrice+" "+ maxGeneration);
		//System.out.println(minDemandPrice+" "+minDemand);
		//System.out.println(maxDemandPrice+" "+maxDemand);
		//System.out.println();
		this.agentName = "testAggregator" + IDbase++;
	}
	
	
	@Override
	public double getGeneration() {
		return currentGeneration;
	}

	
	@Override
	public double getDemand() {
		return currentDemand;
	}
	
	private static int IDbase = 0;
	
	private String agentName;
	
	
	public String getAgentName()
	{
		return this.agentName;
	}
	
	public ArrayList<SupplyPrediction> getPrediction()
	{
		ArrayList<SupplyPrediction> result = new ArrayList<SupplyPrediction>();
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		double cd = currentDemand;
		double cg = currentGeneration;
		double nextGeneration;
		double nextDemand;
		
		
		
		double powerPriceG = getAverageSaleCost();
		double powerPriceD = getAveragePurchaseCost();
		
		
		
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
		currentDemand = currentDemand * 0.99 + nextDemand * 0.01;
		for(int i = currentTick; i < currentTick+1000; i++)
		{
		      	cd = cd  * 0.999 + nextDemand * 0.001;
		      	cg = cg  * 0.999 + nextGeneration * 0.001;
		      	
		      	double sdD = ((i+1-currentTick)/1000.0)  *    (Math.abs(cd-nextDemand)/2); 
			    double sdG = ((i+1-currentTick)/1000.0)  *    (Math.abs(cd-nextGeneration)/2);
			    
			    if((sdD < 0) || (sdG < 0)) System.exit(1);
			    int j = 1-currentTick;
			    
			    if((j <2) ||
			       ((j < 100) && (j%10 == 0)) ||
			       ((j < 1001) && (j%100 == 0)))
			    {
			    	SupplyPrediction nsp = new SupplyPrediction(i,cd,sdD,cg,sdG);
			    	//System.out.println(i+" "+cd+" "+sdD+" "+cg+" "+sdG);
			    	result.add(nsp);
			    }
		}
		
		return result;	
	}

	@ScheduledMethod(start = 0, interval = 1, shuffle = true)
	public void updateSupplyDemand()
	{
		System.out.println(" updateSupplyDemand() called");
		double nextGeneration;
		double nextDemand;
		
		double powerPriceG = (getAverageSaleCost()+getAveragePurchaseCost())/2;
		double powerPriceD = (getAverageSaleCost()+getAveragePurchaseCost())/2;
		
		//double powerPriceG = getAverageSaleCost();
		//double powerPriceD = getAveragePurchaseCost();
		
		
		
		if(powerPriceG <= minGenerationPrice)
			nextGeneration = 0.0;
		else if (powerPriceG >= maxGenerationPrice)
			nextGeneration = maxGeneration;
		else
			nextGeneration = 		
				(((powerPriceG - minGenerationPrice) / (maxGenerationPrice - minGenerationPrice))
				* (maxGeneration - minGeneration))
				+ minGeneration;
		currentGeneration = currentGeneration * 0.95 + nextGeneration * 0.05;
		
		
		if(powerPriceD >= minDemandPrice)
			nextDemand = minDemand;
		else if (powerPriceD <= maxDemandPrice)
			nextDemand = maxDemand;
		else
			nextDemand = 
				(((powerPriceD - minDemandPrice) / (maxDemandPrice - minDemandPrice))
				* (maxDemand - minDemand))
				+ minDemand;
		currentDemand = currentDemand * 0.99 + nextDemand * 0.01;	
		
		//System.out.print(" powerPriceG="+powerPriceG+" D="+powerPriceD);
		//System.out.print(" bal="+(currentGeneration-currentDemand));
		//System.out.print(" nbal="+(nextGeneration-nextDemand));
		//System.out.println();
	
	}

}

