/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological;

/**
 * @author jsnape
 *
 */
public interface Construct {

	public double evaluate();
	
	public String getName();
	
	public double getCurrentValue();
}
