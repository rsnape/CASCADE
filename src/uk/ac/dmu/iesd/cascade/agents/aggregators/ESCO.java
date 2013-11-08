/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

//import uk.ac.cranfield.market.SupplyPrediction;

/**
 * A <em>ESCO</em> or an Electricity Service Company is a concrete object that represents 
 * a commercial/business electricity/energy company involved in retail trade with
 * prosumer agents (<code>ProsumerAgent</code>), such as household prosumers (<code>HouseholdProsumer</code>)
 * In other words, an <code>ESCO</code> provides electricity/energy services to certain types of prosumers (e.g. households)
 * Unlike typical retail companies, (<code>RECO</code>s) will not provide simply electricity, but the
 * service which may include the intelligent management of energy need. <p>
 * It may be also involved in the electricity market.
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/18 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history)
 * 
 * 1.0 -  created the concrete class of AggregatorAgent kind after the latter was made abstract following my suggestion 
 * 
 */

public class ESCO extends AggregatorAgent{


	/**
	 * This method defines how this object behaves (what it does)
	 * at at a given scheduled time throughout the simulation. 
	 * this is called before 'step' method, usually to send a (first) signal. 
	 * Using this method, this is how aggregator takes the first step (before prosumers start) 
	 */
	public void bizPreStep() {

	}

	/**
	 * This method defines how this object behaves (what it does)
	 * at at a given scheduled time throughout the simulation. 
	 */
	public void bizStep() {

	}

	/**
	 * Returns a string representing the state of this agent. This 
	 * method is intended to be used for debugging purposes, and the 
	 * content and format of the returned string should include the states (variables/parameters)
	 * which are important for debugging purpose.
	 * The returned string may be empty but may not be <code>null</code>.
	 * 
	 * @return  a string representation of this agent's state parameters
	 */
	protected String paramStringReport(){
		String str="";
		return str;

	}
	
	/**
	 * Constructs a ESCO agent with the context in which is created and its
	 * base demand.
	 * @param context the context in which this agent is situated
	 * @param baseDemand an array containing the base demand  
	 */
	public ESCO(CascadeContext context, double[] baseDemand) {

		super(context);
		
		// By definition, an ESCO will be small demand
		this.category = Consts.BMU_CATEGORY.DEM_S;
		this.type = Consts.BMU_TYPE.DEM_SMALL;
	}


	public ESCO()
	{
		System.out.println("Constructed an unparameterised ESCO");
	}
	

}
