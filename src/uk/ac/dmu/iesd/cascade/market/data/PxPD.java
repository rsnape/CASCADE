package uk.ac.dmu.iesd.cascade.market.data;

/**
 * 
 * The <em>PxPD</em> class encapsulates a data structure which defines
 * <em>PowerExchange</em> (Px) Product Data.
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/04
 */

public class PxPD
{
	private int productID;
	private int sp_startIndex;
	private double volume;

	public int getProductID()
	{
		return this.productID;
	}

	public int getStartSPIndex()
	{
		return this.sp_startIndex;
	}

	public double getVolume()
	{
		return this.volume;
	}

	public PxPD(int pID, int sp, double vol)
	{
		this.productID = pID;
		this.sp_startIndex = sp;
		this.volume = vol;
	}

}
