/**
 * 
 */
package uk.ac.dmu.iesd.cascade;

import repast.simphony.context.Context;
import repast.simphony.parameter.Parameters;
import uk.ac.dmu.iesd.cascade.context.ProsumerFactory;


/**
 * Class to find and create all factories in Cascade model (Susceptible to change) 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/16 16:00:00 $
 */


public class FactoryFinder {
	
	public static ProsumerFactory createProsumerFactory(Context context, Parameters par) {
		return new ProsumerFactory(context, par);
	}

}
