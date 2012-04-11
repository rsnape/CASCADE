/**
 * 
 */
package uk.ac.cranfield.cascade.market;

/**
 * @author ssmith00
 *
 */
public class MarginalPriceCurveValues {
	public int numberOfUnits;
	public double unitPrice;
	
	public MarginalPriceCurveValues(int numberOfUnits, double unitPrice) {
		this.numberOfUnits = numberOfUnits;
		this.unitPrice = unitPrice;
	}
	
	public int getUnits() {
		return numberOfUnits;
	}
	
	public double getPrice() {
		return unitPrice;
	}

}

