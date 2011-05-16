/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological.cognitive;

import uk.ac.dmu.iesd.cascade.behaviour.psychological.*;

/**
 * @author jsnape
 *
 */
public class TPBModel extends SimpleModel {

	/**
	 * Create a generic TPB model with constructs and relationships
	 * 
	 * This could be extended to add a richer model which incorporates
	 * empirical data
	 */
	TPBModel() {
		super();
		
		Construct attitude = new SimpleConstruct("Attitude",0.7f);
		Construct sn = new SimpleConstruct("Subjective Norm",0.1f);
		Construct pbc = new SimpleConstruct("Perceived Behavioural Control",0.2f);
		Construct intention = new SimpleConstruct("Intention",0.2f);
		Construct behaviour = new SimpleConstruct("Behaviour",0.0f);
		this.addConstruct(attitude);
		this.addConstruct(sn);
		this.addConstruct(pbc);
		this.addConstruct(intention);
		this.addConstruct(behaviour);
		this.addRelationship(new SimpleRelationship(attitude, intention, 0.7f, true));
		this.addRelationship(new SimpleRelationship(sn, intention, 0.7f, true));
		this.addRelationship(new SimpleRelationship(pbc, intention, 0.7f, true));
		this.addRelationship(new SimpleRelationship(pbc, behaviour, 0.7f, true));
		this.addRelationship(new SimpleRelationship(intention, behaviour, 1.0f, true));
		this.addRelationship(new SimpleRelationship(attitude, sn, 0.7f, false));
		this.addRelationship(new SimpleRelationship(attitude, pbc, 0.7f, false));
	}

}
