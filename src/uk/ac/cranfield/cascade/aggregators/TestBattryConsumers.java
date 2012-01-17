/**
 * 
 */
package uk.ac.cranfield.cascade.aggregators;

/**
 * @author grahamfletcher
 *
 */
public class TestBattryConsumers extends BattryOnly {
	public TestBattryConsumers (double minDemandPrice,double maxDemandPrice,
			double minDemand, double maxDemand,
			double storageCapacity, double battryPower){
		super(minDemandPrice,maxDemandPrice,minDemand,maxDemand,storageCapacity, battryPower);
	}
}
