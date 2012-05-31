/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.ArrayList;


/**
 * Interface IProsumerFactory defines possible ways where prosumer factory 
 * can/should create (/initiate) different instances of prosumer concrete subclasses 
 * (such as <code>HouseholdProsumer</code>)
 * 
 * @author Babak Mahdavi 
 * @version $Revision: 1.00 $ $Date: 2011/05/16 10:00:00
 * 
 */
public interface IProsumerFactory {
	//public ProsumerAgent creatProsumer();
	public HouseholdProsumer createHouseholdProsumer(double[] otherDemandProfile, int numOfOccupant, boolean addNoise, boolean hasGas);

	//public ArrayList<ProsumerAgent> createDEFRAHouseholds(int number, String categoryFile, String profileFile);
}
