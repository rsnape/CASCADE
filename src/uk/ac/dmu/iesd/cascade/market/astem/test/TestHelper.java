package uk.ac.dmu.iesd.cascade.market.astem.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.WeakHashMap;

import uk.ac.dmu.iesd.cascade.agents.aggregators.BOD;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.market.IBMTrader;
import uk.ac.dmu.iesd.cascade.market.ITrader;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.data.ImbalData;
import uk.ac.dmu.iesd.cascade.market.data.BSOD;
import uk.ac.dmu.iesd.cascade.market.data.PxPD;


/**
 * This is a helper class for testing the ASTEM market model
 * 
 * @author Babak Mahdavi Ardestani
 * @version 1.0 $ $Date: 2012/02/09
 */


public class TestHelper {

	public static void printMapOfPNs(WeakHashMap mapOfPNs) {
		this.mainContext.logger.debug("TestUtils:: map of PNs (size="+mapOfPNs.size()+"):");
		Set keySet = mapOfPNs.keySet();

		for (Object key : keySet){	
			mapOfPNs.get(key);	
			this.mainContext.logger.debug("key" +key.toString()+" "+Arrays.toString((double[])mapOfPNs.get(key)));
		}
	}

	public static void printListOfBODs(ArrayList<BOD> listOfBODs) {
		this.mainContext.logger.debug("TestUtilstils:: list of BODs (size="+listOfBODs.size()+"):");
		for (BOD bod : listOfBODs){	
			this.mainContext.logger.debug(bod.toString());		
		}
	}
	
	/*
	public static void printMapListOfBODs(WeakHashMap<BMU, ArrayList<BOD>> mapListOfBODs) {

		this.mainContext.logger.debug("TestUtils:: map of BODs (size="+mapListOfBODs.size()+"):");
		Set<BMU> bmuSet = mapListOfBODs.keySet();

		for (BMU bmu : bmuSet){	
			System.out.print("bmuID:" +bmu.getID()+", type: "+bmu.getCategoryAsString());
			TestHelper.printListOfBODs((ArrayList<BOD>) mapListOfBODs.get(bmu));	
		}
	} */


	public static void printMapListOfBODs(WeakHashMap<IBMTrader, ArrayList<BOD>> mapListOfBODs) {

		this.mainContext.logger.debug("TestUtils:: map of BODs (size="+mapListOfBODs.size()+"):");
		Set<IBMTrader> bmuSet = mapListOfBODs.keySet();

		for (IBMTrader bmu : bmuSet){	
			System.out.print("bmuID:" +bmu.getID()+", type: "+bmu.getCategoryAsString());
			TestHelper.printListOfBODs((ArrayList<BOD>) mapListOfBODs.get(bmu));	
		}
	}

	public static void printListOfImbalData(ArrayList<ImbalData> list_imbalData) {
		this.mainContext.logger.debug("TestHelper:: list of ImbalDs (size="+list_imbalData.size()+"):");

		for (ImbalData imbalData: list_imbalData)
			this.mainContext.logger.debug("vol: "+imbalData.getVolume()+ ", flag: "+ imbalData.flag);
	}

	public static void printListOfPxPD(ArrayList<PxPD> list_PxPD) {
		this.mainContext.logger.debug("list of PxPD (size="+list_PxPD.size()+"):");
		for (PxPD pxPD: list_PxPD)
			this.mainContext.logger.debug("pID: "+ pxPD.getProductID()+ ", sp_sIndex: "+ pxPD.getStartSPIndex()+ ", vol: "+pxPD.getVolume());
	}

	public static void printListOfBSOD(ArrayList<BSOD> list_BSOD) {
		this.mainContext.logger.debug("list of BSODs (size="+list_BSOD.size()+"):");
		for (BSOD bsod : list_BSOD)
			this.mainContext.logger.debug("ownerID: "+ bsod.getOwnerID()+ ", vol: "+ bsod.getVolume()+
					", price: "+bsod.getPrice() + ", productID: "+bsod.getProductID() + 
					"("+bsod.getProductIDInHour()+")" +
					", startSPIndex: "+ bsod.getStartSPIndex() + ", accepted: "+ bsod.accepted);
	}

	/*public static void printMapListOfBSODs(WeakHashMap<BMU, ArrayList<BSOD>> mapListOfBSODs) {	
		this.mainContext.logger.debug("TestUtils:: map of BOSDs (size="+mapListOfBSODs.size()+"):");
		Set<BMU> bmuSet = mapListOfBSODs.keySet();

		for (BMU bmu : bmuSet){	
			mapListOfBSODs.get(bmu);	
			System.out.print("bmuID=" +bmu.getID()+", ");
			TestHelper.printListOfBSOD((ArrayList<BSOD>) mapListOfBSODs.get(bmu));	
		}
	} */
	
	public static void printMapListOfBSODs(WeakHashMap<ITrader, ArrayList<BSOD>> mapListOfBSODs) {	
		this.mainContext.logger.debug("TestUtils:: map of BOSDs (size="+mapListOfBSODs.size()+"):");
		Set<ITrader> bmuSet = mapListOfBSODs.keySet();

		for (ITrader bmu : bmuSet){	
			mapListOfBSODs.get(bmu);	
			System.out.print("bmuID=" +bmu.getID()+", ");
			TestHelper.printListOfBSOD((ArrayList<BSOD>) mapListOfBSODs.get(bmu));	
		}
	}


	public static void writeOutput(String fileName, boolean append, double[]... arrays) {

		int[] arr_sp = new int[ASTEMConsts.T_PER_DAY];

		for (int i=0; i<arr_sp.length; i++)
			arr_sp[i] = i;	

		String outputFileName = fileName+".csv";

		CSVWriter resCSVWriter;

		File resFile = new File(outputFileName);

		if (resFile.exists() && append) 
			resCSVWriter = new CSVWriter(outputFileName, true);
		else 
			resCSVWriter = new CSVWriter(outputFileName, false);

		//resCSVWriter.appendText("SettlementPeriods:");
		//resCSVWriter.appendRow(arr_sp);

		for (double[] nextArray : arrays) 
			resCSVWriter.appendRow(nextArray);

		resCSVWriter.close(); 	
	}

	public static void writeOutput(String fileName, boolean append, String[] labels, double[]... arrays) {

		int[] arr_sp = new int[ASTEMConsts.T_PER_DAY];

		for (int i=0; i<arr_sp.length; i++)
			arr_sp[i] = i;	

		String outputFileName = fileName+".csv";

		CSVWriter resCSVWriter;

		File resFile = new File(outputFileName);

		if (resFile.exists() && append) 
			resCSVWriter = new CSVWriter(outputFileName, true);
		else 
			resCSVWriter = new CSVWriter(outputFileName, false);

		resCSVWriter.appendText("SettlementPeriods:");
		resCSVWriter.appendRow(arr_sp);
		int i=0;
		for (double[] nextArray : arrays) {
			resCSVWriter.appendText(labels[i]);
			i++;
			resCSVWriter.appendRow(nextArray);
		}

		resCSVWriter.close(); 
	}
	
	public static String getEnvInfoInString(CascadeContext mainContext) {
		String sInfo="[tick="+mainContext.getTickCount()+ 
		", sp=" +	mainContext.getSettlementPeriod()+
		" ,cumDays="+mainContext.getDayCount()+
		"]";

		return sInfo;
	}

}
