package uk.ac.cranfield.cascade.market;

public class Configs
{

	// 48 half hour slots
	// 24 hour slots
	// 12 2 hour slots
	// 6 4 hour slots
	// 4 12 hour slots
	// 2 1 day slots
	// 2 1 week slots
	// 2 28 day slots h 1 2 4 12 24 w 28d
	// public int[] tradingWindow = {48,24,12,6,4, 2, 2,2};
	// public int[] tradingWindowSizes = {2, 2, 2,3,2, 7, 4,0}; // 2*half hour =
	// 1 hour; 7*1day = 1 week etc

	public int[] tradingWindow =
	{ 48 };
	public int[] tradingWindowSizes =
	{ 0 }; // 2*half hour = 1 hour; 7*1day = 1 week etc

	public double upRamp = 1000;
	public double downRamp = -1000;

	public double biggestTradingProduct = 4096;
	public double TradingProductFactor = 0.5;
	public double smallestTradingProduct = 4;
	public int maxMarketIterationsAtOneProductSize = 3;
	public int numberOfMarketRunsPerTick = 4;
	public double penatlyChargeForBreachingRampRates = 5.0;

	public double biddingSpread = 0.01;
	public int processors = 8;

}
