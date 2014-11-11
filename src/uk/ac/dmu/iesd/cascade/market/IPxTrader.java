package uk.ac.dmu.iesd.cascade.market;

import java.util.ArrayList;

import uk.ac.dmu.iesd.cascade.market.data.BSOD;

/**
 * <em>IPxTrader</em> interface defines the methods' signature of an actor
 * (agent) who wants to be a player in the <tt>Power Exchange</tt> electricity
 * market.
 * 
 * @see uk.ac.dmu.iesd.cascade.market.ITrader
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/05/09
 */

public interface IPxTrader extends ITrader
{

	public ArrayList<BSOD> getListOfBSOD();

	public void recieveBSOA(ArrayList<BSOD> listOfBSOD);

}
