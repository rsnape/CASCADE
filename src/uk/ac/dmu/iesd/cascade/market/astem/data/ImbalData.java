package uk.ac.dmu.iesd.cascade.market.astem.data;

/**
 * This data structure defines imbalance data.
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/04
 */

public class ImbalData
{
	private double volume = 0;
	public boolean flag = false;

	public double getVolume()
	{
		return this.volume;
	}

	public ImbalData(double vol)
	{
		this.volume = vol;
	}

	public ImbalData(double vol, boolean flag)
	{
		this.volume = vol;
		this.flag = flag;
	}
}
