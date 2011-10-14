/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological;

/**
 * @author jsnape
 *
 * A wrapper class to ensure that the behaviour in a model
 * implements the construct interface and therefore can integrate
 * into the construct / relationship framework
 */
public class BehaviourConstruct extends SimpleConstruct {

	/**
	 * @param name
	 * @param initialWeight
	 */
	public BehaviourConstruct(String name, double initialWeight) {
		// TODO Auto-generated constructor stub
		super(name, initialWeight);
	}

}
