/**
 * 
 */
package uk.ac.cranfield.cascade.market;

/**
 * @author grahamfletcher
 *
 */
public class ValueSD {
	public double value;
	double sd;
	
	public ValueSD(double value, double sd)
	{
		this.value = value;
		this.sd = sd;
	}
	
	public double getWithAttitude(double attitude)
	{
		//Returns a figuure that represents the amount to be traded including the 
		//attitude of the trader.   value + (attitude*sd).
		
		//for + values of attitude aim to sell too much or not buy enough unitl later on...
		
		
		return value + (attitude * sd);
		
	}

}
