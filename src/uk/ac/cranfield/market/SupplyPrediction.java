package uk.ac.cranfield.market;
/**
 * 
 */

/**
 * @author grahamfletcher
 *
 */
public class SupplyPrediction {
	public int modelTick;
	public double dDeviation = 0.0;
	public double dValue = 0.0;
	public double sDeviation = 0.0;
	public double sValue = 0.0;
	
	
	
	public SupplyPrediction(int modelTick, double dValue, double dDeviation,
			double sValue, double sDeviation) {
		this.modelTick = modelTick;
		this.dValue = dValue;
		this.dDeviation = dDeviation;
		this.sValue = sValue;
		this.sDeviation = sDeviation;
	}
	
	public String toString()
	{
		return modelTick+" value="+dValue+" "+ "deviation="+dDeviation;
	}

}

