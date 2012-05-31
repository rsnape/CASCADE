package uk.ac.dmu.iesd.cascade.market.astem.operators;

import java.util.ArrayList;
import java.util.WeakHashMap;
import repast.simphony.engine.schedule.ScheduledMethod;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.market.IMarket;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.data.ImbalData;
import uk.ac.dmu.iesd.cascade.market.data.PxPD;

/**
 * This class define the MessageBoard which is a component under the control
 * of the System Operator and used by other operators as well as ITraders (e.g. BMUs)
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/09
 */

public class MarketMessageBoard implements IMarket {
	
	private static ArrayList<PxPD> list_PX_products;
	private static double[] arr_IMBAL;
	private static double[] arr_oldIMBAL; //previous day IMBAL
	private static double[] arr_INDMAR;
	private double[] arr_mip;
	
	public ArrayList<PxPD> getPxProductList() {
		return list_PX_products;
	}
	
	public double[] getMIP() {
		return arr_mip;
	}
	
	public double getBMP() {
		return 0;
	}

	
	public void setMIP(double[] mip) {
		this.arr_mip = mip;
	}
	
	public static double[] getIMBAL(){
		return arr_IMBAL;
	}

	public static double[] getPreviousDayIMBAL(){
		return arr_oldIMBAL;
	}
	
	protected void setIMBAL(double[] imbalArray) {
		System.arraycopy(imbalArray, 0, arr_IMBAL, 0, arr_IMBAL.length);
	}
	
	protected void setPreviousDayIMBAL(double[] oldImbalArray) {
		System.arraycopy(oldImbalArray, 0, arr_oldIMBAL, 0, arr_IMBAL.length);
	}
	
	public static double[] getINDMAR() {
		return arr_INDMAR;
	}
	
	protected void setINDMAR(double[] indMarArray) {
		//arr_INDMAR = indMarArray;
		System.arraycopy(indMarArray, 0, arr_INDMAR, 0, arr_INDMAR.length);
	}
	

	protected void setPxProductList(WeakHashMap<Integer, ArrayList<ImbalData>> mapOfImbalType2ImbalData) {
		list_PX_products = new ArrayList<PxPD>();
		
		ArrayList<ImbalData> listOfImbalDataFor2h = mapOfImbalType2ImbalData.get(ASTEMConsts.PX_PRODUCT_ID_2H);
		int sp=0;
		for (ImbalData imbalData : listOfImbalDataFor2h) {
			if (imbalData.flag) 
				list_PX_products.add(new PxPD(ASTEMConsts.PX_PRODUCT_ID_2H, sp, imbalData.getVolume()/ASTEMConsts.PX_PRODUCT_ID_2H ));
			sp+=ASTEMConsts.PX_PRODUCT_ID_2H; //+4
		}
		
		ArrayList<ImbalData> listOfImbalDataFor4h = mapOfImbalType2ImbalData.get(ASTEMConsts.PX_PRODUCT_ID_4H);
		sp=0;
		for (ImbalData imbalData : listOfImbalDataFor4h) {
			if (imbalData.flag) 
				list_PX_products.add(new PxPD(ASTEMConsts.PX_PRODUCT_ID_4H, sp, imbalData.getVolume()/ASTEMConsts.PX_PRODUCT_ID_4H  ));
			sp+=ASTEMConsts.PX_PRODUCT_ID_4H; //+8
		}
		
		ArrayList<ImbalData> listOfImbalDataFor8h = mapOfImbalType2ImbalData.get(ASTEMConsts.PX_PRODUCT_ID_8H);
		sp=0;
		for (ImbalData imbalData : listOfImbalDataFor8h) {
			if (imbalData.flag) 
				list_PX_products.add(new PxPD(ASTEMConsts.PX_PRODUCT_ID_8H, sp, imbalData.getVolume()/ASTEMConsts.PX_PRODUCT_ID_8H ));
			sp+=ASTEMConsts.PX_PRODUCT_ID_8H; //+16
		}
		
		//System.out.println("MB: PxProducts");
		//TestHelper.printListOfPxPD(list_PX_products);
	}
	
	public MarketMessageBoard() {

		this.arr_IMBAL = new double[ASTEMConsts.T_PER_DAY];
		this.arr_INDMAR = new double[ASTEMConsts.T_PER_DAY];
		this.arr_oldIMBAL= new double[ASTEMConsts.T_PER_DAY];
	}
}
