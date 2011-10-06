/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import javax.accessibility.AccessibleContext;

/**
 * Interface IAggregatorFactory defines possible ways where an 
 * aggregator factory can/should create (and initiate) different instances of 
 * aggregator concrete subclasses (such as <code>RECO</code>)
 * 
 * @author Babak Mahdavi 
 * @version $Revision: 1.00 $ $Date: 2011/05/19 1:00:00
 * 
 */

public interface IAggregatorFactory {
	/**
     * This method is used to create an instance of <code>RECO</code> (Retail Company) agent 
     * @param   baseProfile   an array of double values
     */
	public RECO createRECO(double[] baseProfile);

}
