package uk.ac.dmu.iesd.cascade.market;

import java.util.ArrayList;

import uk.ac.dmu.iesd.cascade.agents.aggregators.BOD;

/**
 * <em>IBMTrader</em> interface defines the methods' signature of 
 * an agent actor player in the UK Balancing Mechanism (BM) electricity market.
 * 
 * @see uk.ac.dmu.iesd.cascade.market.ITrader
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/05/09
 * 
 */

public interface IBMTrader extends ITrader {
	
	public ArrayList<BOD>  getListOfBOD();
	public void recieveBOA(ArrayList<BOD> listOfBOA);

}
