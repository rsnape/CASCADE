/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological;

/**
 * @author jsnape
 *
 */
public interface Relationship {

	public float getWeight();
	
	public boolean getDirected();
	
	public Construct getFromConstruct();
	
	public Construct getToConstruct();
}
