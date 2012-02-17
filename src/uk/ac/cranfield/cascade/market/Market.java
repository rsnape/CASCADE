/**
 * 
 */
package uk.ac.cranfield.cascade.market;

import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;

/**
 * @author grahamfletcher
 *
 */
public class Market {
	
	public static Market defaultM = new Market();
	public double averageBalance = 0.0;
	public double averageShipped = 0.0;
	public double averageDemand = 0.0;
	public double averageSupply = 0.0;
	public double averageCost = 0.0;
	
	/**
	 * @return the averageDemand
	 */
	public double getAverageDemand() {
		return averageDemand;
	}
	/**
	 * @return the averageSupply
	 */
	public double getAverageSupply() {
		return averageSupply;
	}
	public ArrayList<Trader> traders = new ArrayList<Trader>();
	public ArrayList<Prediction> predictions = new ArrayList<Prediction>();
	public String s1(){return "1";}
	public String s2(){return "2";}
	public String s3(){return "3";}
	public String s4(){return "4";}
	public String s5(){return "5";}
	public ArrayList<Double> globalBalancePrediction = new ArrayList<Double>();
	public ArrayList<Double> globalBalancePredictionWithAtt = new ArrayList<Double>(); 
	public ArrayList<Aggregator> aggs = new ArrayList<Aggregator>();

	
	
	public double getAverageBalance()
	{
		return averageBalance;
	}
	
	public double getAverageShipped()
	{
		return averageShipped;
	}
	
	public double getGPowerPrice()
	{
		return Prices.getGlobalPowerPrice();
	}
	
	//@ScheduledMethod(start = 0, interval = 1, shuffle = true)
	public void runMarket()
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	

		//if (Consts.DEBUG) System.out.println("run market");
		
		//Clear the global balance predictions
		//Set the global supply predictions at the correct size;
		globalBalancePrediction = new ArrayList<Double>();
		while(globalBalancePrediction.size() < Parameters.tradingHorizon)
			globalBalancePrediction.add(0.0);
		globalBalancePredictionWithAtt = (ArrayList<Double>) globalBalancePrediction.clone();
		
		//Allow all aggregators to update their data
			for(Aggregator a : aggs)
				a.beforeTrade();
		//Update the internal balances of all the traders
		for(Trader t : traders)
			t.updateStoredData();
		
		//For the number of trades allowed by the traders
		if(currentTick > 200)
		{
		  for(int r = 0; r < Parameters.tradingActivitiy; r++){
			
			ArrayList<Trader> tt1 = (ArrayList<Trader>)traders.clone();
			for(int i = 0; i < tt1.size()*3; i++)	
			{
				int rr1 = RandomHelper.nextIntFromTo(0, tt1.size()-1);
				int ss1 = RandomHelper.nextIntFromTo(0, tt1.size()-1);
				
				Trader p1 = tt1.get(rr1);
				tt1.set(rr1,tt1.get(ss1));
				tt1.set(ss1,p1);
			}
			for(Trader a : tt1) {
				Offer o = a.biggestOffer(r < Parameters.tradesNow);
				if(o != null)
				{//
					//if (Consts.DEBUG) System.out.println("offer "+o.from+" from:"+o.startTimeStamp+" to:"+o.endTimeStamp+" pow="+o.size);
			
					Trader bestT = null;
					double bestPrice = Double.MIN_VALUE;
					double secondPrice = Double.MIN_VALUE;
					
					
					ArrayList<Trader> tt = (ArrayList<Trader>)traders.clone();
					for(int i = 0; i < tt.size()*3; i++)	
					{
						int rr = RandomHelper.nextIntFromTo(0, tt.size()-1);
						int ss = RandomHelper.nextIntFromTo(0, tt.size()-1);
						
						Trader p = tt.get(rr);
						tt.set(rr,tt.get(ss));
						tt.set(ss,p);
					}
					for(Trader b : tt){
						if(b != a)
						{
							double price = b.valueOffer(o);
							//if (Consts.DEBUG) System.out.print("<"+price/o.power+">");
							if(price > bestPrice)
							{
								secondPrice = bestPrice;
								bestPrice = price;
								bestT = b;
							} else if (price > secondPrice)
							{
								secondPrice = price;
							}
						}
						//else if (Consts.DEBUG) System.out.print("<----->");
					}
					//if (Consts.DEBUG) System.out.println();
					
					if(secondPrice > Double.MIN_VALUE)
						o.price = secondPrice;
					else
						o.price = bestPrice;
					o.unitPrice = o.price/o.power;
					
					if(o.unitPrice < Parameters.surplasSupplyValue - 0.1)
					{
						System.exit(1);
					}
					if((bestT != null) && (o.unitPrice > Parameters.surplasSupplyValue+0.01))
					{
						//if (Consts.DEBUG) System.out.println("Selling from "+a+" to "+bestT+" at "+o.unitPrice);
						a.sell(o);
						bestT.buy(o);
						a.updateStoredData();
						bestT.updateStoredData();
					}
					
					
					
				}
				
			}
			  
		  }
		//Update all the stored data again
		  for(Trader a : traders) {		
			a.updateStoredData();
		  }
		}
		
		runMarketClosing();
		
		//Allow all aggrigators to update their data
		for(Aggregator a : aggs)
			a.afterTrade();
		
		  
		  double Cost = 0.0;
		  double quant = 0.0;
		  for(Aggregator a : aggs)
		  {
			  Cost += a.sellPrice*a.sellQuantity+ a.buyPrice*a.buyQuantity;
			  quant += a.sellQuantity+a.buyQuantity;
		  }
			  averageCost = Cost/quant;
	}
	
	/**
	 * @return the averageCost
	 */
	public double getAverageCost() {
		return averageCost;
	}
	public void runMarketClosing()
	{
		//if (Consts.DEBUG) System.out.println("===================================");
		//Run calculation for every trader		
		double totalSupply = 0.0;
		double totalSupplyCost = 0.0;
		double totalDemand = 0.0;
		double totalBalance = 0.0;
		double totalDemandCost = 0.0;
		double totalShipped = 0.0;
		double totalSupplyRepCost = 0.0;
		double totalDemandRepCost = 0.0;
		
		
		double RSupply = 0.0;
		double RDemand = 0.0;
		for(Aggregator a : aggs)
		{
			RSupply += a.currentSupply();
			RDemand += a.currentDemand();
		}
		
		//Calculate the amount shipped around the system at closing
		for(Trader a : traders) {	
			//if (Consts.DEBUG) System.out.println(a.toString()+"---:"+a.currentTBalance());
			if(a.currentTBalance()>0.0) {
				totalSupply += a.currentTBalance();
				totalSupplyCost += a.currentTBalance() * a.closingSalePrice();
				totalSupplyRepCost += a.currentTBalance() * a.closingPurchasePrice();
			}
			else
			{
				totalDemand -= a.currentTBalance();
				totalDemandCost -= a.currentTBalance() * a.closingPurchasePrice();
				totalDemandRepCost -= a.currentTBalance()*a.closingSalePrice();
			}
			totalBalance += a.currentTBalance();
			totalShipped += Math.abs(a.currentTBalance());
		}
		//
		//if (Consts.DEBUG) System.out.println("total balance ="+totalBalance+" total in closing="+(totalShipped));
		//if (Consts.DEBUG) System.out.println("total demand ="+totalDemand+" at "+totalDemandCost);
		//if (Consts.DEBUG) System.out.println("total supply ="+totalSupply+" at "+totalSupplyCost);
		averageBalance = averageBalance * 0.5 + totalBalance * 0.5;
		averageShipped = averageShipped * 0.5 + totalShipped * 0.5;
        averageDemand = RDemand * 0.5 + averageDemand * 0.5;
        averageSupply = RSupply * 0.5 + averageSupply * 0.5;
        
		double purchasePrice;
		double salePrice;
		
		//Nominal transfer price
		double tPrice = 0.0;
		if(totalSupply> 0)
			if(totalDemand > 0)
					tPrice = ((totalSupplyCost / totalSupply) + (totalDemandCost / totalDemand))/2;
			else
				tPrice = totalSupplyCost / totalSupply;
		else
			if(totalDemand > 0)
				tPrice = totalDemandCost / totalDemand;
		
		
        if(totalBalance > 0)
        {
        	purchasePrice = tPrice;
        		salePrice = ((tPrice * totalDemand) + (totalSupply-totalDemand)*Parameters.surplasSupplyValue )/totalSupply;
        }
        else if(totalBalance < 0)
        {
        	salePrice = tPrice;
        	purchasePrice = ((tPrice * totalSupply) + (totalDemand-totalSupply)*Parameters.brownOutCost )/totalDemand;
        }
        else
        	salePrice = purchasePrice = tPrice;
		
        //if (Consts.DEBUG) System.out.print(" Closing SalePrcie="+salePrice);
        //if (Consts.DEBUG) System.out.println(" Closing PurchasePrice="+purchasePrice);
		Prices.setGClosingPurchese(purchasePrice);
		Prices.setGClosingSale(salePrice);
        

		//Update the closing prices experienced by the traders
		for(Trader a : traders) {
			a.closed(purchasePrice, salePrice);
		
		}
	}
}
