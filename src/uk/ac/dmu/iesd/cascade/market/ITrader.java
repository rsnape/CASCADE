package uk.ac.dmu.iesd.cascade.market;

import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;

/**
 *  <em>ITrader</em> interface defines the base methods' signature of 
 * an actor (agent) who wants to be a player in either the balancing mechanism
 * and/or the <tt>Power Exchange</tt> electricity market. 
 * 
 * @see uk.ac.dmu.iesd.cascade.market.IBMTrader
 * @see uk.ac.dmu.iesd.cascade.market.IPxTrader
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/05/09
 */

public interface ITrader {
	
	public int getID();
	public BMU_CATEGORY getCategory();
	public BMU_TYPE getType();
	public String getCategoryAsString();
	public double getMaxGenCap();
	public double getMinDemCap();
	public double[] getPN();
	public double[] getPreviousDayPN();
	
	public void marketPreStep();
	public void marketStep();
	

}
