/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological;

/**
 * @author jsnape
 *
 */
public interface Construct {

	public float evaluate();
	
	public String getName();
	
	public float getCurrentValue();
}
