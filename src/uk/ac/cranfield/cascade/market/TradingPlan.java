package uk.ac.cranfield.cascade.market;

public class TradingPlan {
    public int Time;
    public int TickSize;
    public double plannedBalance;
    public MarginalPriceCurve MPC;
    public double lastSellOfferPrice;
    public double lastBuyOfferPrice;
    public double lastTradePrice;
    public double avgLastTradePrice;
    

	public TradingPlan(int time, int tickSize, double plannedBalance,
			MarginalPriceCurve mPC, double lastSellOfferPrice,
			double lastBuyOfferPrice, double lastTradePrice,
			double avgLastTradePrice) {

		Time = time;
		TickSize = tickSize;
		this.plannedBalance = plannedBalance;
		MPC = mPC;
		this.lastSellOfferPrice = lastSellOfferPrice;
		this.lastBuyOfferPrice = lastBuyOfferPrice;
		this.lastTradePrice = lastTradePrice;
		this.avgLastTradePrice = avgLastTradePrice;
	}
    
    
}
