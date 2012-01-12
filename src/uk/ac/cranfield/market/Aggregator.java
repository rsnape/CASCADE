package uk.ac.cranfield.market;
import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;

/**
 * 
 */

/**
 * @author grahamfletcher
 *
 */
public abstract class Aggregator {
	
	public double stDev = 3;
	
	//The cost of non supply
	private static double brownOutCost = 100;
	
	//List of all the Aggregator in the system
	static ArrayList<Aggregator> allAggregators = new ArrayList<Aggregator>();
	
	//List of the energy bought and sold by this Aggrigator;
	ArrayList<SupplyRecord> historicalBoughtSold = new ArrayList<SupplyRecord>();
	
	public Aggregator() {
		allAggregators.add(this);
	}

	
	private double currentGeneration = 0.0;
	private double currentDemand = 0.0;
	private double supplyBalance = 0.0; 
	private double averageSaleCost = 10.0;
	private double averagePurchaseCost = 10.0;
	
	public double getAveragePurchaseCost()
	{
		return averagePurchaseCost;
	}
	
	public double getAverageSaleCost()
	{
		return averageSaleCost;
	}
	
	static void runMarket()
	{
		for(Aggregator a : allAggregators) {
     		a.updateLocalPredictions();
		}
		for(int r = 0; r < 1; r++){
			
		
			for(Aggregator a : allAggregators) {		
				a.updateBalance();
			}
			
			for(Aggregator a : allAggregators) {
				offer o = a.generateOffer();
				if(o != null)
				{
					//System.out.println("offer");
					Aggregator bestAgg = null;
					double bestPrice = Double.MIN_VALUE;
					double secondPrice = Double.MIN_VALUE;
					
					for(Aggregator b : allAggregators){
						double price = b.bid(o);
						if(price > bestPrice)
						{
							secondPrice = bestPrice;
							bestPrice = price;
							bestAgg = b;
						} else if (price > secondPrice)
						{
							secondPrice = price;
						}
					}
					
					if(secondPrice > Double.MIN_VALUE)
						o.price = secondPrice;
					else
						o.price = bestPrice;
					
					if(o.price > Double.MIN_VALUE)
					{
						//System.out.println("Selling...");
						a.sell(o);
						bestAgg.buy(o);
					}
				}
				
			}
		}
		
		runMarketClosing();
	}
	 static double averageBalance = 1000;
	 static double averageShipped = 1000;
	static void runMarketClosing()
	{
		System.out.println("===================================");
		//Run calculation for every Aggregator
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	
		for(Aggregator a : allAggregators) {
			
			//Calculate the supply balance
			a.currentGeneration = a.getGeneration();
			a.currentDemand = a.getDemand();
			
			//System.out.println(a.currentGeneration+" "+a.currentDemand);
			
			//Get the bought and sold for this time stamp in the model
			//delete any old bought and sold records
			// if nothing for this time stamp create a new record.
			SupplyRecord thisBoughtSold;
			while((a.baughtSold.size() > 0) && (a.baughtSold.get(0).modelTick < currentTick))
			{
				a.baughtSold.remove(0);
			};
			
			if((a.baughtSold.size() > 0) && (a.baughtSold.get(0).modelTick == currentTick))
			{
				thisBoughtSold = a.baughtSold.get(0);
			}
			else
			{
				thisBoughtSold = new SupplyRecord(currentTick, 0.0, 0.0, 0.0, 0.0);
				a.baughtSold.add(0,thisBoughtSold);
			}
			
			
            //Calculate any mismatch	
			a.supplyBalance = a.currentGeneration - a.currentDemand + 
							  thisBoughtSold.generatedOrPurchased - thisBoughtSold.demandOrSold;
			
			System.out.print("Supply Balance = "+a.supplyBalance);
			System.out.println(" stdDev = "+a.stDev );
			System.out.print("Generated      = "+a.currentGeneration);
			System.out.println("  Demand         = "+a.currentDemand);
			System.out.print("Purchased      = "+thisBoughtSold.generatedOrPurchased);
			System.out.println(" Sold           = "+thisBoughtSold.demandOrSold);
			System.out.print("AvgPurchaseP   = "+a.averagePurchaseCost);
			System.out.println(" AvgSaleP   = "+a.averageSaleCost);
			System.out.println("--------------");
		}
		
		//Add up to system total balances
		double totalSupply = 0.0;
		double totalSupplyCost = 0.0;
		double totalDemand = 0.0;
		double totalBalance = 0.0;
		double totalDemandCost = 0.0;
		double totalShipped = 0.0;
		for(Aggregator a : allAggregators) {
			if(a.supplyBalance>0.0) {
				totalSupply += a.supplyBalance;
				totalSupplyCost += a.supplyBalance * a.averageSaleCost * 1.2; 
			}
			else
			{
				totalDemand -= a.supplyBalance;
				totalDemandCost -= a.supplyBalance * a.averagePurchaseCost * 0.8;
			}
			totalBalance += a.supplyBalance;
			totalShipped += Math.abs(a.supplyBalance);
		}
		
		System.out.println("total balance ="+totalBalance+" total in closing="+(totalShipped));
		averageBalance = averageBalance * 0.999 + totalBalance * 0.001;
		averageShipped = averageShipped * 0.999 + totalShipped * 0.001;
		System.out.println("average balance ="+averageBalance+" average total in closing="+(averageShipped));
		double purchasePrice;
		double salePrice;
		if(totalBalance > 0.0) {
			//The system has too much supply
			
			//Everybody gets what they wanted at average of the price they were willing to pay
			//Those selling get averagePrice * totalDemand / totalSupply as over supply is wasted
			if(totalDemand > 0)
			   purchasePrice = totalDemandCost / totalDemand;
			else
			   purchasePrice = 0;
			
			salePrice = purchasePrice * totalDemand / totalSupply;
		}
		else if (totalBalance < 0.0){
			//The system has not enough supply.
			
			//Everybody sells at what they wanted
			//purchase price is sale cost + cost of short fall / demand
			if(totalSupply > 0)
			  salePrice = totalSupplyCost / totalSupply;
			else
			  salePrice = 0;
			
			purchasePrice =(totalSupplyCost - (totalBalance * brownOutCost)) / totalDemand;
		}
		
		else
		{
			//The system is balanced the sale and purchase prices are the average of what every body wanted.
			salePrice = ((totalSupplyCost / totalSupply) + (totalDemandCost / totalDemand))/2;
			purchasePrice = salePrice; 
		}
		

		
		//Update the supply balance
		for(Aggregator a : allAggregators) {
			
			if(a.supplyBalance>0.0) {
				a.baughtSold.get(0).averageSoldCost = ((a.baughtSold.get(0).averageSoldCost * a.baughtSold.get(0).demandOrSold) +
				(a.supplyBalance * salePrice)) / (a.baughtSold.get(0).demandOrSold + a.supplyBalance);
		        a.baughtSold.get(0).demandOrSold += a.supplyBalance;
				//a.averageSaleCost = a.averageSaleCost * 0.99 + a.baughtSold.get(0).averageSoldCost * 0.01;
		        a.averageSaleCost = a.averageSaleCost * 0.99 + salePrice * 0.01;
				a.averagePurchaseCost = a.averagePurchaseCost * 0.999 + purchasePrice * 0.001;

			}
			else
			{
				a.baughtSold.get(0).averagePurchasedCost = ((a.baughtSold.get(0).averagePurchasedCost * a.baughtSold.get(0).generatedOrPurchased) -
				(a.supplyBalance * purchasePrice)) / (a.baughtSold.get(0).generatedOrPurchased - a.supplyBalance);
				a.baughtSold.get(0).generatedOrPurchased -=a.supplyBalance;
				//a.averagePurchaseCost = a.averagePurchaseCost * 0.99 + a.baughtSold.get(0).averagePurchasedCost * 0.01;
				a.averageSaleCost = a.averageSaleCost * 0.999 + salePrice * 0.001;
				a.averagePurchaseCost = a.averagePurchaseCost * 0.99 + purchasePrice * 0.01;
			}
//			System.out.println("v "+a.boughtSold.get(0));
//			System.out.println();
		}
		

	}
	
	private ArrayList<SupplyPrediction>localSupplyPrediction = new ArrayList<SupplyPrediction>();
	private ArrayList<SupplyRecord>baughtSold = new ArrayList<SupplyRecord>();
	ArrayList<Double> balance = new ArrayList<Double>();
	ArrayList<Double> sellableBalance = new ArrayList<Double>();
	public void updateBalance()
	{
		//Remove any out of date records from the baughtSold record
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	
		while((baughtSold.size()>0) && (baughtSold.get(0).modelTick < currentTick))
			baughtSold.remove(0);
		
		//Add any records to the end to bring it up to size;
		for(int i = baughtSold.size(); i < 1000; i++)
			baughtSold.add(new SupplyRecord(currentTick+i,0,0,0,0));
		
		//Calculate the balances
		balance = new ArrayList<Double>();
		sellableBalance = new ArrayList<Double>();
		for(int i = 0; i < 1000; i++)
		{
			balance.add(baughtSold.get(i).generatedOrPurchased + localSupplyPrediction.get(i).sValue -
					    baughtSold.get(i).demandOrSold - localSupplyPrediction.get(i).dValue);
			
			//Sellable supply assums demand is 1sd bigger and supply is 1 sd lower.
			sellableBalance.add(balance.get(i) - stDev*localSupplyPrediction.get(i).dDeviation - stDev*localSupplyPrediction.get(i).sDeviation);
            if(balance.get(i) < sellableBalance.get(i)){
            	System.out.println(localSupplyPrediction.get(i).dDeviation);
            	System.out.println(localSupplyPrediction.get(i).sDeviation);
                System.exit(1);
            }
		}
		//System.out.println(balance.size());
	}
	
	public double bid(offer off)
	{
		//Dont bid on own offers
		if (off.from == this)
			return Double.MIN_VALUE;
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	

		double value=0.0;
		
		for (int i = off.startTimeStamp-currentTick; i < off.endTimeStamp-currentTick; i++)
		{
			double needed = 0.0; 
			double unneeded = 0.0;
			
			if((balance.get(i)<0) //energy is needed
				&& (-balance.get(i) >= off.size)) // all this energy is needed
				{
				    needed = off.size;
				}
			else if((balance.get(i)<0) //energy is needed
					&& (-balance.get(i) < off.size)) // not all this energy is needed
					{
				        needed = -balance.get(i);
				        unneeded = off.size + balance.get(i);
					}
			else    { // no energy is needed
				        unneeded = off.size;
					}
			
			//the value of this energy is the cost of buying the needed 
			//linearly reduce this figure into the future.......
			double val = needed * averagePurchaseCost;
			
			//add on the value of any energy i may be able resell
			//linearly increase this figure into the future.......
			//include a 5% margin
			//val +=  unneeded * averageSaleCost *0.50;
			
			;
			
			value +=val;
		}
		return value;
	}
	
	public void sell(offer off)
	{
		if (off.from != this)
			return;
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	
		
		//for (int i = off.startTimeStamp-currentTick; i < off.endTimeStamp-currentTick; i++)
		//  System.out.print(baughtSold.get(i).demandOrSold);
		//System.out.println();
		
		for (int i = off.startTimeStamp-currentTick; i < off.endTimeStamp-currentTick; i++)
		{
			double oldV = baughtSold.get(i).demandOrSold * baughtSold.get(i).averageSoldCost;
			oldV += off.price / (off.startTimeStamp - off.endTimeStamp);
			baughtSold.get(i).demandOrSold += off.size;
			baughtSold.get(i).averageSoldCost = oldV/baughtSold.get(i).demandOrSold;
		}
		
		//for (int i = off.startTimeStamp-currentTick; i < off.endTimeStamp-currentTick; i++)
		//	  System.out.print(baughtSold.get(i).demandOrSold);
		//	System.out.println();
	}
	
	public void buy(offer off)
	{
		if (off.from == this)
			return;
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	
		
		for (int i = off.startTimeStamp-currentTick; i < off.endTimeStamp-currentTick; i++)
		{
			double oldV = baughtSold.get(i).generatedOrPurchased * baughtSold.get(i).averagePurchasedCost;
			oldV += off.price / (off.startTimeStamp - off.endTimeStamp);
			baughtSold.get(i).generatedOrPurchased += off.size;
			baughtSold.get(i).averagePurchasedCost = oldV/baughtSold.get(i).generatedOrPurchased;
		}
	}
	
	public offer generateOffer()
	{	
	    //Find a spot where the supply balance is positive
		int minOfferTimeStamp=0;
		int maxOfferTimeStamp=0;
		double offerSize;
		//Have ten goes at finding a place in the future to offer
		for(int i = 0; i < 10; i++){
		    minOfferTimeStamp = RandomHelper.nextIntFromTo(0, 99);
		    if(sellableBalance.get(minOfferTimeStamp) > 0)
		    	break;
		}
		
		if(sellableBalance.get(minOfferTimeStamp) > 0)
		{
			//System.out.println(sellableBalance.get(minOfferTimeStamp)+"<>"+balance.get(minOfferTimeStamp));
			//We are going to make an offer
			offerSize = sellableBalance.get(minOfferTimeStamp) * 0.9;
			maxOfferTimeStamp = minOfferTimeStamp;
			
			//move the start of the offer back in time as far as possible
			while((minOfferTimeStamp > 0) && (sellableBalance.get(minOfferTimeStamp) >= offerSize))
				minOfferTimeStamp-= 1;
			
			//move the end of the offer forward in time as far as possible
			while((maxOfferTimeStamp < (100 - 1)) && (sellableBalance.get(maxOfferTimeStamp) >= offerSize))
				maxOfferTimeStamp++;
			
			offerSize = offerSize * 0.1;

			//System.out.println(balance.get(minOfferTimeStamp)+"-"+balance.get(maxOfferTimeStamp));
			
			int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	
			return new offer(minOfferTimeStamp + currentTick, maxOfferTimeStamp+currentTick,offerSize,this);
		}
		
		return null;
	}
	
	
	public void updateLocalPredictions()
	{
		//Blank the old predictions
		localSupplyPrediction = new ArrayList<SupplyPrediction>();
		
		//Get the current tick and the predictions from the child class
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	
        ArrayList<SupplyPrediction> preds = getPrediction();
        
        SupplyPrediction before = null;
		for(int i = currentTick; i < currentTick+1000; i++)
		{
			//get the element in the prediction before the tick being analysed
			//System.out.println(i+" "+preds.size());
			
			while((preds.size()> 0) && (preds.get(0).modelTick <= i))
			{
				before = preds.remove(0);
                //System.out.println(before.modelTick+" removed");
			}
			
			if(preds.size() == 0)
			{
				localSupplyPrediction.add(new SupplyPrediction(i,before.dValue, before.dDeviation,
																 before.sValue, before.sDeviation));
			}
			else
			{
				//System.out.println(preds.get(0).modelTick);
				
			    SupplyPrediction after = preds.get(0);
			    if(before == null) System.exit(1);
			    
			    double th = (i-before.modelTick) * 1.0 / (after.modelTick - before.modelTick);
			    double dValue = ((after.dValue - before.dValue) * th) + before.dValue;
			    double dDeviation = ((after.dDeviation - before.dDeviation) * th) + before.dDeviation;
			    double sValue = ((after.sValue - before.sValue) * th) + before.sValue;
			    double sDeviation = ((after.sDeviation - before.sDeviation) * th) + before.sDeviation;
			    
			    if((dDeviation < 0) || (sDeviation<0)) System.exit(1);
			    
			    //System.out.print("("+dValue+")");
			    localSupplyPrediction.add(new SupplyPrediction(i,dValue,dDeviation,sValue,sDeviation));
			    
			}
		}
		//System.out.println();
	}
	
	//Abstract Methods
	
	//Get the generation and demand in this time step
	abstract public double getGeneration();
	abstract public double getDemand();
	abstract public ArrayList<SupplyPrediction> getPrediction();
	abstract public String getAgentName();
}

