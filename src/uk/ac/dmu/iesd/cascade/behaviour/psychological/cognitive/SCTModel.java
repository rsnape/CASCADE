/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological.cognitive;

/**
 * @author jsnape
 *
 */
public class SCTModel {
	
	//constructs
	private double outcome_expectation;
	private double perception_of_others;
	private double self_efficacy;
	private double socio_structural_factors;
	private double goal;
	private double behaviour;	
	private double outcome;
	
	
	//relationships
	private double weight_socio_structural_to_perception_of_others;
	private double weight_socio_structural_to_outcome_peception;
	
	private double weight_outcome_expectation_to_goal;
	private double weight_perception_of_others_to_goal;
	private double weight_self_efficacy_to_goal;
	private double weight_socio_structural_factors_to_goal;
	
	private double weight_outcome_expectation_to_behaviour;
	private double weight_perception_of_others_to_behaviour;
	private double weight_self_efficacy_to_behaviour;
	private double weight_socio_structural_factors_to_behaviour;
	
	private double weight_efficacy_to_outcome_peception;
	/**
	 * @return the outcome_expectation
	 */
	public double getOutcome_expectation() {
		return outcome_expectation;
	}
	/**
	 * @param outcome_expectation the outcome_expectation to set
	 */
	public void setOutcome_expectation(double outcome_expectation) {
		this.outcome_expectation = outcome_expectation;
	}
	/**
	 * @return the perception_of_others
	 */
	public double getPerception_of_others() {
		return perception_of_others;
	}
	/**
	 * @param perception_of_others the perception_of_others to set
	 */
	public void setPerception_of_others(double perception_of_others) {
		this.perception_of_others = perception_of_others;
	}
	/**
	 * @return the self_efficacy
	 */
	public double getSelf_efficacy() {
		return self_efficacy;
	}
	/**
	 * @param self_efficacy the self_efficacy to set
	 */
	public void setSelf_efficacy(double self_efficacy) {
		this.self_efficacy = self_efficacy;
	}
	/**
	 * @return the socio_structural_factors
	 */
	public double getSocio_structural_factors() {
		return socio_structural_factors;
	}
	/**
	 * @param socio_structural_factors the socio_structural_factors to set
	 */
	public void setSocio_structural_factors(double socio_structural_factors) {
		this.socio_structural_factors = socio_structural_factors;
	}
	/**
	 * @return the goal
	 */
	public double getGoal() {
		return goal;
	}
	/**
	 * @param goal the goal to set
	 */
	public void setGoal(double goal) {
		this.goal = goal;
	}
	/**
	 * @return the behaviour
	 */
	public double getBehaviour() {
		return behaviour;
	}
	/**
	 * @param behaviour the behaviour to set
	 */
	public void setBehaviour(double behaviour) {
		this.behaviour = behaviour;
	}
	/**
	 * @return the outcome
	 */
	public double getOutcome() {
		return outcome;
	}
	/**
	 * @param outcome the outcome to set
	 */
	public void setOutcome(double outcome) {
		this.outcome = outcome;
	}
	/**
	 * @return the weight_efficacy_to_outcome_peception
	 */
	public double getWeight_efficacy_to_outcome_peception() {
		return weight_efficacy_to_outcome_peception;
	}
	/**
	 * @param weight_efficacy_to_outcome_peception the weight_efficacy_to_outcome_peception to set
	 */
	public void setWeight_efficacy_to_outcome_peception(
			double weight_efficacy_to_outcome_peception) {
		this.weight_efficacy_to_outcome_peception = weight_efficacy_to_outcome_peception;
	}
	/**
	 * @return the weight_socio_structural_to_perception_of_others
	 */
	public double getWeight_socio_structural_to_perception_of_others() {
		return weight_socio_structural_to_perception_of_others;
	}
	/**
	 * @param weight_socio_structural_to_perception_of_others the weight_socio_structural_to_perception_of_others to set
	 */
	public void setWeight_socio_structural_to_perception_of_others(
			double weight_socio_structural_to_perception_of_others) {
		this.weight_socio_structural_to_perception_of_others = weight_socio_structural_to_perception_of_others;
	}
	/**
	 * @return the weight_socio_structural_to_outcome_peception
	 */
	public double getWeight_socio_structural_to_outcome_peception() {
		return weight_socio_structural_to_outcome_peception;
	}
	/**
	 * @param weight_socio_structural_to_outcome_peception the weight_socio_structural_to_outcome_peception to set
	 */
	public void setWeight_socio_structural_to_outcome_peception(
			double weight_socio_structural_to_outcome_peception) {
		this.weight_socio_structural_to_outcome_peception = weight_socio_structural_to_outcome_peception;
	}
	/**
	 * @return the weight_outcome_expectation_to_goal
	 */
	public double getWeight_outcome_expectation_to_goal() {
		return weight_outcome_expectation_to_goal;
	}
	/**
	 * @param weight_outcome_expectation_to_goal the weight_outcome_expectation_to_goal to set
	 */
	public void setWeight_outcome_expectation_to_goal(
			double weight_outcome_expectation_to_goal) {
		this.weight_outcome_expectation_to_goal = weight_outcome_expectation_to_goal;
	}
	/**
	 * @return the weight_perception_of_others_to_goal
	 */
	public double getWeight_perception_of_others_to_goal() {
		return weight_perception_of_others_to_goal;
	}
	/**
	 * @param weight_perception_of_others_to_goal the weight_perception_of_others_to_goal to set
	 */
	public void setWeight_perception_of_others_to_goal(
			double weight_perception_of_others_to_goal) {
		this.weight_perception_of_others_to_goal = weight_perception_of_others_to_goal;
	}
	/**
	 * @return the weight_self_efficacy_to_goal
	 */
	public double getWeight_self_efficacy_to_goal() {
		return weight_self_efficacy_to_goal;
	}
	/**
	 * @param weight_self_efficacy_to_goal the weight_self_efficacy_to_goal to set
	 */
	public void setWeight_self_efficacy_to_goal(double weight_self_efficacy_to_goal) {
		this.weight_self_efficacy_to_goal = weight_self_efficacy_to_goal;
	}
	/**
	 * @return the weight_socio_structural_factors_to_goal
	 */
	public double getWeight_socio_structural_factors_to_goal() {
		return weight_socio_structural_factors_to_goal;
	}
	/**
	 * @param weight_socio_structural_factors_to_goal the weight_socio_structural_factors_to_goal to set
	 */
	public void setWeight_socio_structural_factors_to_goal(
			double weight_socio_structural_factors_to_goal) {
		this.weight_socio_structural_factors_to_goal = weight_socio_structural_factors_to_goal;
	}
	/**
	 * @return the weight_outcome_expectation_to_behaviour
	 */
	public double getWeight_outcome_expectation_to_behaviour() {
		return weight_outcome_expectation_to_behaviour;
	}
	/**
	 * @param weight_outcome_expectation_to_behaviour the weight_outcome_expectation_to_behaviour to set
	 */
	public void setWeight_outcome_expectation_to_behaviour(
			double weight_outcome_expectation_to_behaviour) {
		this.weight_outcome_expectation_to_behaviour = weight_outcome_expectation_to_behaviour;
	}
	/**
	 * @return the weight_perception_of_others_to_behaviour
	 */
	public double getWeight_perception_of_others_to_behaviour() {
		return weight_perception_of_others_to_behaviour;
	}
	/**
	 * @param weight_perception_of_others_to_behaviour the weight_perception_of_others_to_behaviour to set
	 */
	public void setWeight_perception_of_others_to_behaviour(
			double weight_perception_of_others_to_behaviour) {
		this.weight_perception_of_others_to_behaviour = weight_perception_of_others_to_behaviour;
	}
	/**
	 * @return the weight_self_efficacy_to_behaviour
	 */
	public double getWeight_self_efficacy_to_behaviour() {
		return weight_self_efficacy_to_behaviour;
	}
	/**
	 * @param weight_self_efficacy_to_behaviour the weight_self_efficacy_to_behaviour to set
	 */
	public void setWeight_self_efficacy_to_behaviour(
			double weight_self_efficacy_to_behaviour) {
		this.weight_self_efficacy_to_behaviour = weight_self_efficacy_to_behaviour;
	}
	/**
	 * @return the weight_socio_structural_factors_to_behaviour
	 */
	public double getWeight_socio_structural_factors_to_behaviour() {
		return weight_socio_structural_factors_to_behaviour;
	}
	/**
	 * @param weight_socio_structural_factors_to_behaviour the weight_socio_structural_factors_to_behaviour to set
	 */
	public void setWeight_socio_structural_factors_to_behaviour(
			double weight_socio_structural_factors_to_behaviour) {
		this.weight_socio_structural_factors_to_behaviour = weight_socio_structural_factors_to_behaviour;
	}


	
}
