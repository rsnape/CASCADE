package uk.ac.dmu.iesd.cascade.market.astem.operators;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IMarket;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.data.ImbalData;
import uk.ac.dmu.iesd.cascade.market.data.PxPD;

/**
 * This class define the MessageBoard which is a component under the control of
 * the System Operator and used by other operators as well as ITraders (e.g.
 * BMUs)
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/09
 */

public class MarketMessageBoard implements IMarket
{

	CascadeContext mainContext;

	private static ArrayList<PxPD> list_PX_products;
	private static double[] arr_IMBAL;
	private static double[] arr_oldIMBAL; // previous day IMBAL
	private static double[] arr_INDMAR;
	private double[] arr_mip;

	@Override
	public ArrayList<PxPD> getPxProductList()
	{
		return MarketMessageBoard.list_PX_products;
	}

	@Override
	public double[] getMIP()
	{
		return this.arr_mip;
	}

	/*
	 * 
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.dmu.iesd.cascade.market.IMarket#getBMP()
	 */
	@Override
	public double getBMP()
	{
		return 0;
	}

	/***
	 * set the Market index price array.
	 * 
	 * As configured (Day ahead market), this is called at the end of each day
	 * to set the MIP for each settlement period of the following day. This is
	 * then available to all while that day executes before being reset again at
	 * the end of the day.
	 */
	public void setMIP(double[] mip)
	{
		this.arr_mip = mip;
	}

	public static double[] getIMBAL()
	{
		return MarketMessageBoard.arr_IMBAL;
	}

	public static double[] getPreviousDayIMBAL()
	{
		return MarketMessageBoard.arr_oldIMBAL;
	}

	protected void setIMBAL(double[] imbalArray)
	{
		System.arraycopy(imbalArray, 0, MarketMessageBoard.arr_IMBAL, 0, MarketMessageBoard.arr_IMBAL.length);
	}

	protected void setPreviousDayIMBAL(double[] oldImbalArray)
	{
		System.arraycopy(oldImbalArray, 0, MarketMessageBoard.arr_oldIMBAL, 0, MarketMessageBoard.arr_IMBAL.length);
	}

	public static double[] getINDMAR()
	{
		return MarketMessageBoard.arr_INDMAR;
	}

	protected void setINDMAR(double[] indMarArray)
	{
		// arr_INDMAR = indMarArray;
		System.arraycopy(indMarArray, 0, MarketMessageBoard.arr_INDMAR, 0, MarketMessageBoard.arr_INDMAR.length);
	}

	protected void setPxProductList(LinkedHashMap<Integer, ArrayList<ImbalData>> mapOfImbalType2ImbalData)
	{
		MarketMessageBoard.list_PX_products = new ArrayList<PxPD>();

		ArrayList<ImbalData> listOfImbalDataFor2h = mapOfImbalType2ImbalData.get(ASTEMConsts.PX_PRODUCT_ID_2H);
		int sp = 0;
		for (ImbalData imbalData : listOfImbalDataFor2h)
		{
			if (imbalData.flag)
			{
				MarketMessageBoard.list_PX_products.add(new PxPD(ASTEMConsts.PX_PRODUCT_ID_2H, sp, imbalData.getVolume()
						/ ASTEMConsts.PX_PRODUCT_ID_2H));
			}
			sp += ASTEMConsts.PX_PRODUCT_ID_2H; // +4
		}

		ArrayList<ImbalData> listOfImbalDataFor4h = mapOfImbalType2ImbalData.get(ASTEMConsts.PX_PRODUCT_ID_4H);
		sp = 0;
		for (ImbalData imbalData : listOfImbalDataFor4h)
		{
			if (imbalData.flag)
			{
				MarketMessageBoard.list_PX_products.add(new PxPD(ASTEMConsts.PX_PRODUCT_ID_4H, sp, imbalData.getVolume()
						/ ASTEMConsts.PX_PRODUCT_ID_4H));
			}
			sp += ASTEMConsts.PX_PRODUCT_ID_4H; // +8
		}

		ArrayList<ImbalData> listOfImbalDataFor8h = mapOfImbalType2ImbalData.get(ASTEMConsts.PX_PRODUCT_ID_8H);
		sp = 0;
		for (ImbalData imbalData : listOfImbalDataFor8h)
		{
			if (imbalData.flag)
			{
				MarketMessageBoard.list_PX_products.add(new PxPD(ASTEMConsts.PX_PRODUCT_ID_8H, sp, imbalData.getVolume()
						/ ASTEMConsts.PX_PRODUCT_ID_8H));
			}
			sp += ASTEMConsts.PX_PRODUCT_ID_8H; // +16
		}

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("MB: PxProducts");
		}
		// TestHelper.printListOfPxPD(list_PX_products);
	}

	public void setContext(CascadeContext c)
	{
		this.mainContext = c;
	}

	public MarketMessageBoard()
	{

		MarketMessageBoard.arr_IMBAL = new double[ASTEMConsts.T_PER_DAY];
		MarketMessageBoard.arr_INDMAR = new double[ASTEMConsts.T_PER_DAY];
		MarketMessageBoard.arr_oldIMBAL = new double[ASTEMConsts.T_PER_DAY];
	}
}
