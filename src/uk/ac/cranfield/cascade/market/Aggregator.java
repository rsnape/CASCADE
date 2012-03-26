package uk.ac.cranfield.cascade.market;
import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import uk.ac.dmu.iesd.cascade.Consts;


/**
 * 
 */

/**
 * @author grahamfletcher
 *
 */
public abstract class Aggregator {
	
	//How aggressive is this aggrigator.
	/*The traders can be aggressive or conservative in their trading. This is implemented
	 * as a number of standard deviations above or below the average supply/demand they will
	 * buy or sell. An aggressive trader will assume demand will be below mean and therefore buy later
	 * an that supply will be above mean and therefore sell earlier. Conservative traders will do the oposite
	 * +numbers represent aggressive and -numbers conservative
	 */
	private double attitude = 0;
	
	//private int lastPredictionsTimeStamp = -1;
	
	//Predictions for supply and demand provided by the real aggrigators
    public ArrayList<Prediction> localSupplyPrediction;
    
    //Predictions of supply and demand with and without the attitde modification
    //caclulated from localSupplyPrediction
    private ArrayList<Double> supplyPrediction = new ArrayList<Double>();
    private ArrayList<Double> demandPrediction = new ArrayList<Double>();
    private ArrayList<Double> supplyPredictionWithAtt = new ArrayList<Double>();
    private ArrayList<Double> demandPredictionWithAtt = new ArrayList<Double>();
    
    //The traders and market that power is bauget and sold by and in
    private Trader sellTrader;
    private Trader buyTrader;
    private Market market = Market.defaultM ;
    
    
    //Figures achived across both traders for buying and selling
    public double avgBuyPrice = 1.0;
    public double avgSellPrice = 1.0;
    public double avgBuyPriceLT = 10.0;
    public double avgSellPriceLT = 10.0;
    public double buyPrice = 1.0;
    public double sellPrice = 1.0;
    
    //Traded figures are the values achived by the buy and sell traders
    public double avgTBuyPrice = 1.0;
    public double avgTSellPrice = 1.0;
    public double avgTBuyPriceLT = 10.0;
    public double avgTSellPriceLT = 10.0;
    public double buyTPrice = 1.0;
    public double sellTPrice = 1.0;
    public double pred30PriceS = 1.0;
    public double pred30PriceD = 1.0;
    
    public double avgFactor = 1.0;
    public double sellQuantity;
    public double buyQuantity;
    public double avgPower = 1.0;
    
    private static int nextID=0;
    protected int ID = nextID++;
    
    
    private ArrayList<ArrayList<OldPreds>> oldP = new ArrayList<ArrayList<OldPreds>>(Parameters.tradingHorizon); 
    
    public String s1() {return toString()+".1";}
    public String s2() {return toString()+".2";}
    public String s3() {return toString()+".3";}
    public String s4() {return toString()+".4";}
    public String s5() {return toString()+".5";}
   
    private predictor pred30[] = new predictor[Parameters.predictPoint.length];
    public ArrayList<Double> buyPPUpredictions = new ArrayList<Double>();
    public ArrayList<Double> sellPPUpredictions = new ArrayList<Double>();
    
    public String toString()
    {
    	return "Agg"+ID;
    }
    public Aggregator(double attitude)
    {
    	this.attitude = attitude;
    	sellTrader = new Trader(this, this.toString()+":sell");
    	buyTrader = new Trader(this, this.toString()+":buy");
    	market.aggs.add(this);
    	
    	for(int i = 0; i < pred30.length; i++)
    		pred30[i] = new predictor();
    	
    	while(oldP.size()<(Parameters.tradingHorizon))
    	{
    		oldP.add( new ArrayList<OldPreds>());
    		buyPPUpredictions.add(0.0);
    		sellPPUpredictions.add(0.0);
    	}
    }
    
    public static double getGlobalprice()
    {

    	return Prices.getGlobalPowerPrice();
    }
    
    public double getAvgSellPrice()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return avgSellPrice;
    }
    public double getAvgBuyPrice()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return avgBuyPrice;
    }
    public double getSellPrice()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return sellPrice;
    }
    public double getBuyPrice()
    {
    	if(ID >= Parameters.aggsToGraph) return Double.NaN;
    	return buyPrice;
    }
    public double getAvgSellPriceLT()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return avgSellPriceLT;
    	
    }
    public double getAvgBuyPriceLT()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;

    	return avgBuyPriceLT;
    	
    }
    public double getTAvgSellPriceLT()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return avgTSellPriceLT;
    	
    }
    public double getTAvgBuyPriceLT()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return avgTBuyPriceLT;
    	
    }
    public double getTAvgSellPrice()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return avgTSellPrice;
    	
    }
    public double getTAvgBuyPrice()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return avgTBuyPrice;
    	
    }
    public double getTSellPrice()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return sellTPrice;
    	
    }
    public double getTBuyPrice()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return buyTPrice;
    	
    }
  
    public double getPred30PriceS()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return sellPPUpredictions.get(0);
    }
    public double getPred30PriceD()
    {
    	if(ID >= Parameters.aggsToGraph) return 0.0;
    	return buyPPUpredictions.get(0);
    }

	
	public double getBalance()
	{
		if(ID >= Parameters.aggsToGraph) return Double.NaN;
		else
		{
			if (Consts.DEBUG) System.out.println(toString()+" "+(currentSupply() - currentDemand()));
			return currentSupply() - currentDemand();
		}
	}
	
	
	
	
	
	
	
	public double currentTSupply(Trader T)
	{
	   // Allocate the current supply to the traders
		if(T == this.sellTrader )
			return currentSupply();
		else
			return 0.0;
	}
	
	public double currentTDemand(Trader T)
	{
		if(T == this.buyTrader )
			return currentDemand();
		else
			return 0.0;
		// Allocate the current demand to the traders
	}
	
	public void beforeTrade()
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	

	
		//Called before any trading begins to allow the aggrigator to update any internal data
		localSupplyPrediction = getPrediction();
		
		//Seperate Predictions
		supplyPrediction = new ArrayList<Double>();
	    demandPrediction = new ArrayList<Double>();
	    supplyPredictionWithAtt = new ArrayList<Double>();
	    demandPredictionWithAtt = new ArrayList<Double>();
		
		
		//Create a copy of the localSupplyPrediction that can be taken appart
		ArrayList<Prediction> tempPredictions = (ArrayList<Prediction>)localSupplyPrediction.clone();
		
		
		double lastPowerA = 0.0;
		double lastPower  = 0.0;	
		double lastPower2A = 0.0;
		double lastPower2  = 0.0;

		int lastTime = -1;
		
		//For each power prediction neededs
		for(int i = 0; i <Parameters.tradingHorizon; i++)
		{
		    //move on the lastPower and lastTime  
			while((tempPredictions.size()>0) &&
				  (tempPredictions.get(0).modelTick<= currentTick+i))
			{
				lastPower = tempPredictions.get(0).prediction.getWithAttitude(0);
				lastPowerA = tempPredictions.get(0).prediction.getWithAttitude(attitude);
				lastPower2 = tempPredictions.get(0).prediction2.getWithAttitude(0);
				lastPower2A = tempPredictions.get(0).prediction2.getWithAttitude(attitude);

				lastTime  = tempPredictions.get(0).modelTick;
				tempPredictions.remove(0);
			}
			
			double thisPower;
			double thisPowerA;
			double thisPower2;
			double thisPower2A;
			//If no further predictions then use the most recent prediction
			if(tempPredictions.size() == 0)
			{
				thisPower = lastPower;
				thisPowerA = lastPowerA;
				thisPower2 = lastPower2;
				thisPower2A = lastPower2A;

			}
			else
			{
				//Interpolate
				double factor = 1.0*(currentTick+i-lastTime)/(tempPredictions.get(0).modelTick-lastTime);
				thisPowerA = factor * (tempPredictions.get(0).prediction.getWithAttitude(attitude) - lastPowerA)+lastPowerA;
				thisPower = factor * (tempPredictions.get(0).prediction.getWithAttitude(0) - lastPower)+lastPower;
				thisPower2A = factor * (tempPredictions.get(0).prediction2.getWithAttitude(attitude) - lastPower2A)+lastPower2A;
				thisPower2 = factor * (tempPredictions.get(0).prediction2.getWithAttitude(0) - lastPower2)+lastPower2;
			}
			
			//Store the demand and supply details with attitude for the traders
			supplyPrediction.add(thisPower);
		    demandPrediction.add(-thisPower2);
		    supplyPredictionWithAtt.add(thisPowerA);
		    demandPredictionWithAtt.add(-thisPower2A);
		    
		    //Add these figures into the market wide global predictions
		    market.globalBalancePrediction.set(i, market.globalBalancePrediction.get(i)+thisPower-thisPower2);
		    market.globalBalancePredictionWithAtt.set(i, market.globalBalancePredictionWithAtt.get(i)+thisPowerA-thisPower2A);

		}
		
		sellTrader.predictions =(ArrayList<Double>) supplyPredictionWithAtt.clone();
		buyTrader.predictions =(ArrayList<Double>) demandPredictionWithAtt.clone();
	}
	
	public void afterTrade()
	{
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	


			
			avgPower = avgPower*Parameters.reactiveness  + Parameters.reac1 * (sellTrader.prices.avgQuantity - buyTrader.prices.avgQuantity );
				
			//double cost = (sellTrader.prices.avgPowerCost*Math.abs(sellTrader.prices.avgQuantity)/
				//		  (Math.abs(sellTrader.prices.avgQuantity)+Math.abs(buyTrader.prices.avgQuantity)))
							    //    +
		          //        (buyTrader.prices.avgPowerCost*Math.abs(buyTrader.prices.avgQuantity)/
					//      (Math.abs(sellTrader.prices.avgQuantity)+Math.abs(buyTrader.prices.avgQuantity)));
							
							
						
	
			
			//And do the average buy and sell price
			//buy and sell prices are the raw unit cost of power in this time slot.
			//buyT and sellT are the prices achived by the traders taking into account
			//all the trades they made.
			if(buyTrader.prices.qPurchased - buyTrader.prices.qSold != 0)
			buyTPrice = ((buyTrader.prices.purchase * buyTrader.prices.qPurchased)-
					    (buyTrader.prices.sale  * buyTrader.prices.qSold)) /
					    (buyTrader.prices.qPurchased - buyTrader.prices.qSold);
			else
				buyTPrice = Prices.getGlobalPowerPrice();
			
			if(sellTrader.prices.qSold - sellTrader.prices.qPurchased != 0)
			sellTPrice = ((sellTrader.prices.sale * sellTrader.prices.qSold)-
			            (sellTrader.prices.purchase  * sellTrader.prices.qPurchased))/
				    (sellTrader.prices.qSold - sellTrader.prices.qPurchased);
			else
				sellTPrice = Prices.getGlobalPowerPrice();;
				
			if(buyTrader.prices.qPurchased + sellTrader.prices.qPurchased!=0)
			buyPrice = ((buyTrader.prices.purchase * buyTrader.prices.qPurchased) +
			           (sellTrader.prices.purchase * sellTrader.prices.qPurchased))
			           /(buyTrader.prices.qPurchased + sellTrader.prices.qPurchased);
			else
				buyPrice = Prices.getGlobalPowerPrice();
			
			buyQuantity = buyTrader.prices.qPurchased + sellTrader.prices.qPurchased;
			if(buyTrader.prices.qSold + sellTrader.prices.qSold!=0)
			sellPrice = ((buyTrader.prices.sale * buyTrader.prices.qSold) +
			           (sellTrader.prices.sale * sellTrader.prices.qSold))
			           /(buyTrader.prices.qSold + sellTrader.prices.qSold);
			else
				sellPrice = Prices.getGlobalPowerPrice();
			sellQuantity = buyTrader.prices.qSold + sellTrader.prices.qSold;
			
			
			
			
			//Do the short and long term averages for the buy sell buyT and sellT prices
			double w = 2000;
			if(buyPrice < avgBuyPrice - Math.abs(avgBuyPrice*w)) buyPrice = avgBuyPrice- Math.abs(avgBuyPrice*w);
			if(buyPrice > avgBuyPrice + Math.abs(avgBuyPrice*w)) buyPrice = avgBuyPrice+ Math.abs(avgBuyPrice*w);
			if(sellPrice < avgSellPrice - Math.abs(avgSellPrice*w)) sellPrice = avgSellPrice- Math.abs(avgSellPrice*w);
			if(sellPrice > avgSellPrice + Math.abs(avgSellPrice*w)) sellPrice = avgSellPrice+ Math.abs(avgSellPrice*w);	
			avgBuyPrice = Parameters.reactiveness  * avgBuyPrice + Parameters.reac1  * buyPrice;
			avgSellPrice = Parameters.reactiveness * avgSellPrice + Parameters.reac1 * sellPrice;
			avgBuyPriceLT = avgBuyPriceLT * 0.999 + 0.001 * buyPrice;
			avgSellPriceLT = avgSellPriceLT * 0.999+ 0.001 * sellPrice;

			w=20;
			if(buyTPrice < avgTBuyPrice - Math.abs(avgTBuyPrice*w)) buyTPrice = avgTBuyPrice- Math.abs(avgTBuyPrice*w);
			if(buyTPrice > avgTBuyPrice + Math.abs(avgTBuyPrice*w)) buyTPrice = avgTBuyPrice+ Math.abs(avgTBuyPrice*w);
			if(sellTPrice < avgTSellPrice - Math.abs(avgTSellPrice*w)) sellTPrice = avgTSellPrice- Math.abs(avgTSellPrice*w);
			if(sellTPrice > avgTSellPrice + Math.abs(avgTSellPrice*w)) sellTPrice = avgTSellPrice+ Math.abs(avgTSellPrice*w);
					
			avgTBuyPrice = Parameters.reactiveness  * avgTBuyPrice + Parameters.reac1  * buyTPrice;
			avgTSellPrice = Parameters.reactiveness * avgTSellPrice + Parameters.reac1 * sellTPrice;
			avgTBuyPriceLT = avgTBuyPriceLT * 0.999 + 0.001 * buyTPrice;
			avgTSellPriceLT = avgTSellPriceLT * 0.999 + 0.001 * sellTPrice;

			//Update the forward price projections
			buyPPUpredictions.add(0.0);
			sellPPUpredictions.add(0.0);
			buyPPUpredictions.remove(0);
			sellPPUpredictions.remove(0);
			for(int pr = 0; pr < Parameters.predictPoint.length; pr++)
			{
				int i  = Parameters.predictPoint[pr];
				ArrayList<Double> p = pred30[pr].predict(market.globalBalancePrediction.get(i), 
							          supplyPrediction.get(i),
							          demandPrediction.get(i),
							          buyTrader.traded.get(i).avgBuyPrice(),
							          buyTrader.traded.get(i).avgSellPrice(),
							          sellTrader.traded.get(i).avgBuyPrice(),
							          sellTrader.traded.get(i).avgSellPrice());
				sellPPUpredictions.set(i, p.get(0));
				buyPPUpredictions.set(i, p.get(1));
			}
			
		    pred30PriceD = sellPPUpredictions.get(0);
		    pred30PriceS = buyPPUpredictions.get(0);
			//Old predictions are used to store what we thaught the balance would be so that
			// the neural network can be trained to predict prices forward
			if ((currentTick>54*48))
			{
			
				//Add a new record to the end of the predictions
				oldP.add(new ArrayList<OldPreds>());
				
				//Add a record of the predictions
				for( int i = 0; i < Parameters.tradingHorizon; i++)
				{
					OldPreds op = new OldPreds(market.globalBalancePrediction.get(i), 
							          supplyPrediction.get(i),
							          demandPrediction.get(i),
							          buyTrader.traded.get(i).avgBuyPrice(),
							          buyTrader.traded.get(i).avgSellPrice(),
							          sellTrader.traded.get(i).avgBuyPrice(),
							          sellTrader.traded.get(i).avgSellPrice());
					ArrayList<OldPreds> np = oldP.get(i);
		            np.add(op);
					oldP.set(i, np);
				}
					
				//The record at the front of the predictions will have a complete record of our expectation
				//at each time slot it passed back in time. Pop it off and add trining data to the predictors.
				ArrayList<OldPreds> np = oldP.remove(0);
				
				//if(RandomHelper.nextIntFromTo(0, 999)== 0)
				if(np.size()==Parameters.tradingHorizon )
				{
					for(int pr = 0; pr < Parameters.predictPoint.length; pr++)
					{
						int st = Parameters.predictPoint[pr];
						pred30[pr].addExample(buyPrice,
								          sellPrice,
								          np.get(st).gSupplyDemand , 
								          np.get(st).lSupply, 
								          np.get(st).lDemand,
								          np.get(st).buyTreaderPurcahsePPU,
								          np.get(st).buyTraderSellPPU,
								          np.get(st).sellTraderPurchasePPU,
								          np.get(st).sellTraderSellPPU);
						
						
						pred30[pr].train();
					}
				}
			}
	}
	
	//Abstract Methods
	
	//Get the generation and demand in this time step
	abstract public ArrayList<Prediction> getPrediction();
	abstract public ArrayList<Prediction> getPredictionShortMarket();
	abstract public double currentSupply();
	abstract public double currentDemand();
	
	
	private class OldPreds
	{
		double gSupplyDemand;
		double lSupply;
		double lDemand;
		double buyTreaderPurcahsePPU;
		double buyTraderSellPPU;
		double sellTraderPurchasePPU;
		double sellTraderSellPPU;
		ArrayList<Double> predictedValue;
		
		public OldPreds(double gSupplyDemand, double lSupply, double lDemand, double buyTreaderPurcahsePPU, double buyTraderSellPPU, double sellTraderPurchasePPU, double sellTraderSellPPU)
		{
			this.gSupplyDemand = gSupplyDemand;
			this.lSupply = lSupply;
			this.lDemand = lDemand;
			this.buyTreaderPurcahsePPU=buyTreaderPurcahsePPU;
			this.buyTraderSellPPU=buyTraderSellPPU;
			this.sellTraderPurchasePPU=sellTraderPurchasePPU;
			this.sellTraderSellPPU=sellTraderSellPPU;
		}
	}
	
}

