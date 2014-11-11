package uk.ac.dmu.iesd.cascade.market.data;

import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;

/**
 * The <em>BSOD</em> class encapsulates a data structure which defines
 * buy-sell-offer-data used for trading in the <em>PowerExchange</em> (Px)
 * market.
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/06
 * 
 */

public class BSOD
{ // Buy-Sell-Offer-Data
	private int ownerID; // BMU ID
	private double volume = 0;
	private double price = 0;
	private int productID;
	private int startSPIndex;
	public boolean accepted = false;

	public int getOwnerID()
	{
		return this.ownerID;
	}

	public double getVolume()
	{
		return this.volume;
	}

	public void setVolume(double vol)
	{
		this.volume = vol;
	}

	public double getPrice()
	{
		return this.price;
	}

	public int getProductID()
	{
		return this.productID;
	}

	public String getProductIDInHour()
	{
		String pIDinHour = "";
		if (this.productID == ASTEMConsts.PX_PRODUCT_ID_2H)
		{
			pIDinHour = "2H";
		}
		else if (this.productID == ASTEMConsts.PX_PRODUCT_ID_4H)
		{
			pIDinHour = "4H";
		}
		else if (this.productID == ASTEMConsts.PX_PRODUCT_ID_8H)
		{
			pIDinHour = "8H";
		}

		return pIDinHour;
	}

	public int getStartSPIndex()
	{
		return this.startSPIndex;
	}

	public BSOD(int oID, double vol, double price, int pID, int sp)
	{
		this.ownerID = oID;
		this.volume = vol;
		this.price = price;
		this.productID = pID;
		this.startSPIndex = sp;
	}

	public BSOD(int oID, double vol, double price, int pID, int sp, boolean acc)
	{
		this(oID, vol, price, pID, sp);
		this.accepted = acc;
	}

}
