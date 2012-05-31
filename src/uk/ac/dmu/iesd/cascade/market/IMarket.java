package uk.ac.dmu.iesd.cascade.market;

import java.util.ArrayList;

import uk.ac.dmu.iesd.cascade.market.data.PxPD;

/**
 * <em>IMarket</em> interface defines the methods' signature of 
 * a component which represents the electricity market. 
 * Players in the market use this market component (via this interface) to 
 * interact with the market (e.g. obtain MIP, MCP, BMP prices or list of <em>PowerExchange</em>
 * products etc).
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/05/09
 */

public interface IMarket {
	
	public double[] getMIP();
	public double getBMP();
	public ArrayList<PxPD> getPxProductList();

}
