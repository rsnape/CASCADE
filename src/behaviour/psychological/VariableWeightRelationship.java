/**
 * 
 */
package behaviour.psychological;

/**
 * @author jsnape
 *
 */
public class VariableWeightRelationship extends SimpleRelationship {

	public boolean setWeight(float newWeight)
	{
		boolean updateSuccess = true;
		// TODO - this could be more or less complicated update procedure
		this.weight = newWeight;
		return updateSuccess;
		
	}
	
	/**
	 * Constructor simply passes variables through - only difference here is that
	 * the weight can vary dynamically within the use of the model
	 */
	public VariableWeightRelationship(Construct from, Construct to, float initialWeight, boolean directed) {
		// TODO Auto-generated constructor stub
		super(from, to, initialWeight, directed);
	}
	
}
