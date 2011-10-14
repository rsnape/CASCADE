/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological;

/**
 * @author jsnape
 *
 */
public interface Relationship {

	public double getWeight();
	
	public boolean getDirected();
	
	public Construct getFromConstruct();
	
	public Construct getToConstruct();
}
