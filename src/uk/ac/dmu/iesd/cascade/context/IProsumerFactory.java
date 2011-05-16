/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

/**
 * @author Babak Mahdavi 
 * @version $Revision: 1.00 $ $Date: 2011/05/16 10:00:00
 * 
 */
public interface IProsumerFactory {
	//public ProsumerAgent creatProsumer();
	public HouseholdProsumer createHouseholdProsumer(float[] baseProfile, boolean addNoise);

}
