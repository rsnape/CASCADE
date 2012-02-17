/**
 * 
 */
package uk.ac.cranfield.cascade.market;

/**
 * @author grahamfletcher
 *
 */
public class tradeRecord {
	
	//Encapsulates the trading record for a trader in a time slot.
	public int modelTick;
	
	private double purchased = 0.0;
	private double sold = 0.0;
	private double totalPurchasedCost = 0.0;
	private double totalSoldCost = 0.0;
	
	public tradeRecord(int modelTick)
	{
		this.modelTick = modelTick;
	}
	
	void buy(double power, double cost)
	{
		purchased += power;
		totalPurchasedCost += cost;
	}
	
	void sell(double power, double cost)
	{
		if(cost/power > Parameters.brownOutCost+1)
		{
			//if (Consts.DEBUG) System.out.println("Sell cost too high");
			System.exit(1);
		}
		sold += power;
		totalSoldCost += cost;
	}
	
	double avgBuyPrice()
	{
		if (purchased > 0.0)
		return totalPurchasedCost / purchased;
		else
			return 0.0;
	}
	
	double avgSellPrice()
	{
		//if (Consts.DEBUG) System.out.println(totalSoldCost+" "+sold);
		if(sold > 0.0)
		return totalSoldCost / sold;
		else
			return 0.0;
	}
	
	double sold()
	{
		return sold;
	}
	
	double purchased()
	{
		return purchased;
	}




}
