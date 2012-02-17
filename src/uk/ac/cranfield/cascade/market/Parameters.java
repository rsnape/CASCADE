/**
 * 
 */
package uk.ac.cranfield.cascade.market;

/**
 * @author grahamfletcher
 *
 */
public class Parameters {
	
	
	/*The cost of non supply of electricity. Can be considered as either the costs
	 * of using a suppler of last resort or the economic cost of non-supply
	 */
	static public double brownOutCost = 100.00;
	static public double surplasSupplyValue = 1.00;
	
	
	/*Traders can operate any distance into the future. The distance is measured in model ticks.
	 * 
	 */
	static public int tradingHorizon = 48; 
	
	/*Traders are allowed to perfrom this many trades per time stamp
	 * 
	 */
	static public int tradingActivitiy = 31;
	static public int tradesNow = 20;
	
	
	
	static public double reactiveness = 0.995;
	static public double reac1 = 1 -  reactiveness;
	static public double reactivnessG = 1 - (reac1/10);
	static public double reac2 = 1 - reactivnessG;
	
	
	//Factors used for the mapping to the predictors
	static public double supplyScale = 10000;
	static public double minCost = -0.0;
	static public double maxCost = 60.0;
	
	//The points in the trading horizon where predictions will be calculated
	static public int[] predictPoint = {47};
	
	//Number of aggs to graph. The rest will return 0 in all trends
	static public int aggsToGraph = 10;
}
