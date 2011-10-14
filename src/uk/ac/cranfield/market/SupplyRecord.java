package uk.ac.cranfield.market;

public class SupplyRecord {
	public int modelTick;
	public double demandOrSold = 0.0;
	public double generatedOrPurchased = 0.0;
	public double averageSoldCost = 0.0;
	public double averagePurchasedCost = 0.0;
	
	public SupplyRecord(int modelTick, double demandOrSold, double generatedOrPurchased,
			                           double averageSoldCost, double averagePurchasedCost) {
		this.modelTick = modelTick;
		this.demandOrSold = demandOrSold;
		this.generatedOrPurchased = generatedOrPurchased;
		this.averageSoldCost = averageSoldCost;
		this.averagePurchasedCost = averagePurchasedCost;
	}
	
	public String toString()
	{
		return modelTick+" sold="+demandOrSold+" soldcost="+averageSoldCost+" purch="+generatedOrPurchased+" purchCost="+averagePurchasedCost;
	}

}
