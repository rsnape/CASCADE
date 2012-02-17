/**
 * 
 */
package uk.ac.cranfield.cascade.market;

import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;

/**
 * @author grahamfletcher
 *
 */
public class Trader {

	/*Implements the main trading functions of the Aggrigators*/ 
	
	/*The list of markets in which this Trader operates.
	  Traders can operate in a number of Markets, obtaining quotes and selling
	  to the highest bidder across the whole list*/
	//private ArrayList<Market> markets = new ArrayList<Market>();
	
	public String name="";
	
	
	
	
	/*Traders are informed of their predicted supply/demand balance. They attempt to trade this 
	 * away to achieve a running zero balance. The totals traded are stored in "traded" and the
	 * time stamp of the head element of the list in "tradedTimeStamp". All other elements
	 * of the list are assumed to relate to consecutive subsequent model ticks.
	 */
	public ArrayList<tradeRecord> traded = new ArrayList<tradeRecord>();
	private int tradedTimeStamp = 0;
	
	/*The model also maintains a tick based representation of the balance prediction that it must
	 * meet This is derived from the predictions array and the trading record.
	 *  
	 */
	public ArrayList<Double> balance = new ArrayList<Double>();
    public double currentBalance()
    {//if(balance.size()>0)return balance.get(0); //else return -1.0;}
    	//return this.owningAggrigator.currentTSupply(this)-owningAggrigator.currentTDemand(this);
    	return balance.get(0);
    }
    public double currentTBalance()
    {//if(balance.size()>0)return balance.get(0); //else return -1.0;}
    	
    	//if (Consts.DEBUG) System.out.println("demand "+ owningAggrigator.currentTDemand(this)+" sold "+traded.get(0).sold());
    	//if (Consts.DEBUG) System.out.println(" supply "+ owningAggrigator.currentTSupply(this)+ "purc "+ traded.get(0).purchased());
    	//if (Consts.DEBUG) System.out.println(" tot "+ (this.owningAggrigator.currentTSupply(this)-owningAggrigator.currentTDemand(this)+
    	      // traded.get(0).purchased()-traded.get(0).sold())+" cb"+currentBalance());
    	return this.owningAggrigator.currentTSupply(this)-owningAggrigator.currentTDemand(this)+
    	       traded.get(0).purchased()-traded.get(0).sold();
    }
	//The array of predictions that the model will attemt to balance
	public ArrayList<Double> predictions = new ArrayList<Double>();
	
	//All traders work for an Aggregator who will update their predictions;
	public Aggregator owningAggrigator;
	
	public Prices prices = new Prices(this.toString());
	/*UTILITY FUNCTIONS
	 * 
	 */
	
	static private int nextID=0;
	public int ID = nextID++;
	
	public String toString()
	{
		return "Trader"+ID+" "+name;
	}
	
	public void updateStoredData()
	/*Updates the current state of all stored data by:
	 *   1) moving any time stamps on to the current time
	 *   2) fills and missing data at the end of the time series with zeros
	 */
	{
		
		//Get the current model time
		//All these tasks are for a whole tick....
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	

		//remove any old information from the front of traded
		while((traded.size() > 0) && (traded.get(0).modelTick  < currentTick))
		{
			traded.remove(0);
		}	
		//Bring the traded record up to size so that it covers the whole trading window
		int nextStamp;
		if(traded.size() == 0)
			nextStamp = currentTick;
		else
			nextStamp = traded.get(traded.size()-1).modelTick+1;
		while(nextStamp < currentTick+Parameters.tradingHorizon +1)
		{
			traded.add(new tradeRecord(nextStamp++));
		}
		

		balance = new ArrayList<Double>();
		for(int i = 0; i <Parameters.tradingHorizon; i++)
			balance.add(predictions.get(i)+traded.get(i).purchased()-traded.get(i).sold());
	}
	

	
	public double valueOffer(Offer o)
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	
		double v = 0;
		if(o.from==this) return Double.MIN_VALUE;
		
		//if (Consts.DEBUG) System.out.println("o.size="+o.size);
		//if (Consts.DEBUG) System.out.println("prices.avgSale="+prices.avgSale);
		double ppu = (prices.avgGPurchase*1.0+prices.avgGSale*1.0+prices.avgPurchase*1.0+prices.avgSale*1.0)/4.10;
        double ppu1 = prices.avgClosingPurchase;
         ppu1 = ppu;
        
		if (ppu>Parameters.brownOutCost ) ppu = Parameters.brownOutCost;
		if (ppu<Parameters.surplasSupplyValue ) ppu = Parameters.surplasSupplyValue;
		
		if (ppu1>Parameters.brownOutCost ) ppu1 = Parameters.brownOutCost;
		if (ppu1<Parameters.surplasSupplyValue ) ppu1 = Parameters.surplasSupplyValue;
		
		//if (Consts.DEBUG) System.out.print("ppu="+ppu);
		for(int i = o.startTimeStamp; i <= o.endTimeStamp;i++)
		{
			double price = ((i-currentTick)* ppu  + (currentTick+48 - i) * ppu1)/48;
			
			double bal = balance.get(i-currentTick);
			//if (Consts.DEBUG) System.out.println("bal="+bal);
			if(bal<0)
			{
				bal = -bal;
				if(o.size<bal) 
						v+= o.size * price;
				else
						v+= bal * price + (o.size-bal)*Parameters.surplasSupplyValue;
				//if (Consts.DEBUG) System.out.println(v);
				//v += bal * prices.avgPowerCost * 0.8;
				//v+= bal* (Prices.getGlobalPowerPrice()+prices.avgPowerCost)/2.1;
			}
			else
				v+= o.size*Parameters.surplasSupplyValue;
		}
		//if (Consts.DEBUG) System.out.println("ppuR="+v/o.power);
		
		return v;
	}
	
	public Offer biggestOffer(boolean nowOnly)
	//Create the biggest offer possible in the balance array
	{
		Offer o = null;
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	

		if (nowOnly)
		{
		    if(balance.get(0)>0)
		    {
		    	o = new Offer(currentTick,currentTick,balance.get(0)*0.95,this);
		    }
		}
		else
		{
			for(int i = 0; i < Parameters.tradingHorizon; i++)
			//Work out from every possible time stamp 
			{
				for(int j = 0; j < 5; j++)
				//Have five goes as the size will be random
				{
					Offer p = Offer.newFromTS(i+currentTick, this);
					if((o == null) || ((p != null) && (p.power > o.power)))
						o = p;
				}
			}
		}
		return o;
	}
	
	public Trader(Aggregator owner, String name)
	{
		Market.defaultM.traders.add(this);
		this.owningAggrigator = owner;
		this.name=name;
	}
	
	public void buy(Offer o)
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	

		for(int i = o.startTimeStamp-currentTick; i <= o.endTimeStamp-currentTick;i++)
		{
			if((i >=0) && (i < Parameters.tradingHorizon))
			{
				tradeRecord tr = traded.get(i);
				tr.buy(o.size, o.unitPrice * o.size);
				traded.set(i,tr);
			}
		}
		prices.setContractPurchase(o.unitPrice);
	}
	
	public void sell(Offer o)
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	

		for(int i = o.startTimeStamp-currentTick; i <= o.endTimeStamp-currentTick;i++)
		{
			if((i >=0) && (i < Parameters.tradingHorizon))
			{
				tradeRecord tr = traded.get(i);
				tr.sell(o.size, o.unitPrice * o.size );
				traded.set(i,tr);
			}
		}
		prices.setContractSale(o.unitPrice);
		
	}
	
	public double closingSalePrice()
	//Calculate the price this trader would like to sell at in closing
	{
		
		
		//return Math.max(Prices.avgGClosingSale*1.2,prices.avgContractPurchase*1.4) ;
		double ppu = (prices.avgGPurchase*0.5+prices.avgGSale*1.0+prices.avgPurchase*0.5+prices.avgSale*1.0)/3.00;
		return ppu*1.001;
	}
	
	public double closingPurchasePrice()
	//Calculate the price this trader would like to buy at in closing
	{
	
		double ppu = (prices.avgGPurchase*1.0+prices.avgGSale*0.5+prices.avgPurchase*1.0+prices.avgSale*0.5)/3.00;
		return ppu * 0.999;
		//return Prices.avgGClosingPurchase*0.8;
		

	}
	
	public void closed(double purchasePrice, double salePrice)
	//Called to tell the trader how much power was traded for it and the price at closing
	//+ve power is a buy price will be positive
	//-ve power is a sell price will be negative
	{
		double power = Math.abs(currentTBalance());
		double price = 0.0;
		
		prices.setClosingSale(salePrice);
		prices.setClosingPurchese(purchasePrice);
		if(currentTBalance() > 0.0)
		{
			
			
			if(Math.abs(currentTBalance()+traded.get(0).sold())>0.01)
			{
				//if (Consts.DEBUG) System.out.println("1:"+salePrice+ " "+currentTBalance()+" "+Math.abs(traded.get(0).sold())+" "+traded.get(0).avgSellPrice());
				prices.setSale((salePrice*currentTBalance()+
						             Math.abs(traded.get(0).sold())*traded.get(0).avgSellPrice())
						             /(currentTBalance()+traded.get(0).sold()));
			}
			prices.setQSold(currentTBalance()+traded.get(0).sold());
			if(Math.abs(traded.get(0).purchased())> 0.01)
			{
				prices.setPurchase(traded.get(0).avgBuyPrice());
			}
			prices.setQPurchased(traded.get(0).purchased());
			
		}
		else if(currentTBalance() < 0.0)
		{
			//if (Consts.DEBUG) System.out.println(toString()+" update purchase");
			
			//if (Consts.DEBUG) System.out.println("2:s="+salePrice+ " p="+purchasePrice+" "+currentTBalance()+" "+Math.abs(traded.get(0).sold())+" "+traded.get(0).avgSellPrice());
					
			if(Math.abs(currentTBalance())+traded.get(0).purchased()> 0.01)
			{
				prices.setPurchase((purchasePrice*Math.abs(currentTBalance())+
		             traded.get(0).purchased()*traded.get(0).avgBuyPrice())
		             /(Math.abs(currentTBalance())+traded.get(0).purchased()));
			}
			prices.setQPurchased(Math.abs(currentTBalance())+traded.get(0).purchased());
			
			if(Math.abs(traded.get(0).sold())>0.01)
			{
				prices.setSale(traded.get(0).avgSellPrice());
			}
			prices.setQSold(traded.get(0).sold());
		}
		else
		{
			//if (Consts.DEBUG) System.out.println("3:"+salePrice+ " "+currentTBalance()+" "+Math.abs(traded.get(0).sold())+" "+traded.get(0).avgSellPrice());
			
			if(Math.abs(traded.get(0).sold())>0.01)
			{
				prices.setSale(traded.get(0).avgSellPrice());
			}
			prices.setQSold(traded.get(0).sold());
			if(Math.abs(traded.get(0).purchased())> 0.01)
			{
				prices.setPurchase(traded.get(0).avgBuyPrice());
			}
			prices.setQPurchased(traded.get(0).purchased());
		}
		
		
	}
	
}
