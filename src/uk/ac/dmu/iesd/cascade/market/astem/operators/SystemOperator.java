package uk.ac.dmu.iesd.cascade.market.astem.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import uk.ac.dmu.iesd.cascade.agents.aggregators.BOD;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IBMTrader;
import uk.ac.dmu.iesd.cascade.market.ITrader;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.data.ImbalData;
import uk.ac.dmu.iesd.cascade.market.astem.test.TestHelper;
import uk.ac.dmu.iesd.cascade.market.astem.util.CollectionUtils;
import uk.ac.dmu.iesd.cascade.market.astem.util.SortComparatorUtils;
import uk.ac.dmu.iesd.cascade.market.astem.util.ArraysUtils;

/**
 * 
 * The <em>SystemOperator</em> class embodies a major actor (agent) in the electricity market.
 * In the UK context, this role is played by the <tt>National Grid</tt>.  
 * It uses /interacts with  <em>SettlementCompany</em> ({@link SettlementCompany}), which in the UK context, 
 * its operations are performed by <tt>Elexon</tt>.
 * It also uses <em>MessageBoard</em> ({@link MarketMessageBoard}) as a 'Reporting Service' tool
 * to broadcast information used by the players in the electricity market. 
 *  
 *  Another important operator in the electricity market is <em>PowerExchange</em>:
 *  @see uk.ac.dmu.iesd.cascade.market.astem.operators.PowerExchange
 *  
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/09
 * 
 */


public class SystemOperator {

	private CascadeContext mainContext;
	private SettlementCompany settlementCo;
	private MarketMessageBoard messageBoard;
	private double[] arr_NDF; //National Demand Forecast
	private double[] arr_INDGEN; //Indicated Generation
	private double[] arr_IMBAL; //Imbalance
	private double[] arr_oldIMBAL; // old/previous day Imbalance

	private double[] arr_INDMAR; //Indicator Margin 
	private ArrayList<ITrader> list_ITrader;  
	private ArrayList<BOD> list_BOD;

	private WeakHashMap <Integer, double[]> map_dmuID2PN;
	private WeakHashMap <IBMTrader, ArrayList<BOD>> map_IBMTrader2ListOfBODs;	
	private WeakHashMap <IBMTrader, ArrayList<BOD>> map_IBMTrader2ListOfBOAs; //BOA are the same as BODs, except processed for acceptance (T or F)

	private WeakHashMap <Integer, ArrayList<ImbalData>> map_imbalType2ImbalData;
		
	public double[] getPublishedNDF() {
		return arr_NDF;
	}

	/*private ArrayList<BMU> getBMUList() {
		ArrayList<BMU> aListOfBMUs = new ArrayList<BMU>();

		Network bmuNet = mainContext.getNetworkOfRegisteredBMUs();
		Iterable<RepastEdge> edgeIter = bmuNet.getEdges();
		if(Consts.VERBOSE) 
			System.out.println("SO has access to "+ bmuNet.size() + " registered BMUs");
		for (RepastEdge edge : edgeIter) {
			Object obj = edge.getTarget();
			if (obj instanceof BMU)
				aListOfBMUs.add((BMU) obj);    		
			else
				System.err.println("SO:: Wrong Class Type: BMU agent is expected");
		}
		return aListOfBMUs;
	} */

	private WeakHashMap fetchPNs(ArrayList<ITrader> listOfITraders) {
		//System.out.println("------ SO: recievePNs");
        WeakHashMap mapOfPNs = new WeakHashMap();
		for (ITrader bmu : listOfITraders){	
			mapOfPNs.put(bmu.getID(), bmu.getPN());
		}
		return mapOfPNs;
	}
	
	
	private double[] calculateNDF(WeakHashMap <Integer, double[]> mapOfPNs) {
		
		//System.out.println("calculateNDF()");
		
		double [][] arr_ij_pn = new double [CollectionUtils.numOfRowsWithNegValInCollection(mapOfPNs.values())][mainContext.ticksPerDay];
		int i=0;

		for (double[] pn : mapOfPNs.values()) {
			if (ArraysUtils.isContainNegative(pn)) {
				arr_ij_pn[i]= pn;
				i++;
			}	
		}	

		return ArraysUtils.sumOfCols2DDoubleArray(arr_ij_pn);
	}

	private double[] calculateINDGEN(WeakHashMap <Integer, double[]> mapOfPNs) {
		
		//System.out.println("calculateINDGEN()");

		double [][] arr_ij_pn = new double [CollectionUtils.numOfRowsWithPosValInCollection(mapOfPNs.values())][mainContext.ticksPerDay];
		int i=0;

		for (double[] pn : mapOfPNs.values()) {
			if (!ArraysUtils.isContainNegative(pn)) {
				arr_ij_pn[i]= pn;
				i++;
			}	
		}
		
		//System.out.println(ArrayUtils.toString(arr_ij_pn));
		
		return ArraysUtils.sumOfCols2DDoubleArray(arr_ij_pn);
	}
	
	private double[] calculateIMBAL(double[] ndf, double[] indGen) {
		return ArraysUtils.add(ndf, indGen);
	}

	private double[] calculateINDMAR(ArrayList<ITrader> listOfITraders, double[] imbalArray) {

		//sum of maxGenCap - INDGEN
		double [] indMargin= new double[imbalArray.length];
		double sumOfMaxGen= 0;
		for (ITrader bmu : listOfITraders){	
			sumOfMaxGen += bmu.getMaxGenCap();
			//System.out.println("bmuMaxGenCap: "+bmu.getMaxGenCap());
		}
		//System.out.println("SumOfBmuMaxGenCap: "+sumOfMaxGen);

		for (int i=0; i<indMargin.length; i++)
			indMargin[i]= sumOfMaxGen-imbalArray[i];

		return indMargin;
	}
	
	private void broadcastIMBALandINDMARInfo(double[] imbalArray, double[] oldImbalArray, double[] indMarArray) {
		//pass INDMAR and IMBAL to all BMUs 
		this.messageBoard.setIMBAL(imbalArray);
		this.messageBoard.setINDMAR(indMarArray);
		this.messageBoard.setPreviousDayIMBAL(oldImbalArray);
	}
	
	private WeakHashMap<IBMTrader, ArrayList<BOD>> fetchBODs(List<ITrader> listOfITraders) {
		//System.out.println("------ SO: fetchBOD");
		//Received BOD are in the form of list
		WeakHashMap<IBMTrader, ArrayList<BOD>> map_IBMTrader2bod = new WeakHashMap <IBMTrader, ArrayList<BOD>>();
		
		for (int i=0; i< listOfITraders.size(); i++) {
			IBMTrader bmu =(IBMTrader) listOfITraders.get(i);
			map_IBMTrader2bod.put(bmu, bmu.getListOfBOD());

		}		
		//for (IBMTrader bmu : listOfITraders) 
			//map_IBMTrader2bod.put(bmu, bmu.getListOfBOD());
			
		return map_IBMTrader2bod;
	}
	
	private WeakHashMap<IBMTrader, ArrayList<BOD>> generateBOA(WeakHashMap<IBMTrader, ArrayList<BOD>> mapOfIBMTrader2ListOfBODs, int sp, double[] prevDayIMBALArray) {
		
		ArrayList<BOD> listOfBODs = new ArrayList<BOD>();
		
		Collection<ArrayList <BOD>> valCol = mapOfIBMTrader2ListOfBODs.values();
		for (ArrayList<BOD> arrBOD : valCol) 
			listOfBODs.addAll(arrBOD);
		
		ArrayList<BOD> listOfOffers = new ArrayList<BOD>();
		ArrayList<BOD> listOfBids = new ArrayList<BOD>();
		for (BOD bod : listOfBODs){	
			if (bod.getPairID() > 0)
				listOfOffers.add(bod);
			else listOfBids.add(bod);
		}
				
		Collections.sort(listOfOffers, SortComparatorUtils.SO_SUBMITTED_BO_TOPDOWN_ASCENDING_ORDER); 
		
		Collections.sort(listOfBids, SortComparatorUtils.SO_SUBMITTED_BO_TOPDOWN_DESCENDING_ORDER);

		Iterator <BOD> iterOffers = listOfOffers.iterator();
		
		if (prevDayIMBALArray[sp] <0) {
			double remainingIMBAL = prevDayIMBALArray[sp];
		
			while(remainingIMBAL<0 && iterOffers.hasNext()) {
				BOD bod = iterOffers.next();
				bod.isAccepted=true;
				remainingIMBAL = remainingIMBAL + bod.getLevel();
				if (remainingIMBAL>0) {
					bod.setLevel(bod.getLevel()-remainingIMBAL);
					remainingIMBAL=0;
				}
			}
		}
		
		Iterator <BOD> iterBids = listOfBids.iterator();

		if (prevDayIMBALArray[sp] >0 ) {
			double remainingIMBAL = prevDayIMBALArray[sp];
			while(remainingIMBAL>0 && iterBids.hasNext()) {
				BOD bod = iterBids.next();
				bod.isAccepted=true;
				remainingIMBAL = remainingIMBAL + bod.getLevel();
				if (remainingIMBAL<0){
					bod.setLevel(bod.getLevel()-remainingIMBAL);
					remainingIMBAL=0;						
				}
			}
		}
		
		return mapOfIBMTrader2ListOfBODs;
	}


	private void sendBOAtoEachIBMTrader(WeakHashMap<IBMTrader, ArrayList<BOD>> mapOfIBMTrader2ListOfBOAs) {	
		//System.out.println("SO: sendBOAtoEachBMU() called");
		//System.out.println("size of mapOfBMUID2ListOfBOAs: "+mapOfBMU2ListOfBOAs.size());

		Set <IBMTrader> bmuSet =  mapOfIBMTrader2ListOfBOAs.keySet();
		
		for (IBMTrader bmu: bmuSet) 
			bmu.recieveBOA((ArrayList<BOD>)mapOfIBMTrader2ListOfBOAs.get(bmu));
	
	}
	
	private void sendBOAtoSettlementCo(WeakHashMap<IBMTrader, ArrayList<BOD>> mapOfIBMTrader2ListOfBOAs) {	
		//System.out.println("SO: sendBOAtoSettlementCo() called");
		this.settlementCo.recieveBOAs(mapOfIBMTrader2ListOfBOAs); // @TODO: send a clone/copy not the original ref
	}
	
	/*
	private void sendBOAtoEachBMU_old(List<BMU> listOfBMUs, HashMap<Integer, ArrayList<BOD>> mapBMU_ID2ListOfBODs) {	
		System.out.println("SO: sendBOAtoEachBMU() called");
		System.out.println("size of listOfBMUs: "+listOfBMUs.size());
		System.out.println("size of mapBMU_ID2ListOfBODs: "+mapBMU_ID2ListOfBODs.size());

		for (BMU bmu : listOfBMUs){	
			System.out.println(bmu.getID());
		}
		
		Set<Integer> keySet = mapBMU_ID2ListOfBODs.keySet();
			
		//TestUtils.printListOfBODs(this.map_ListOf_BOD);

		for (BMU bmu : listOfBMUs){	
			Object obj = mapBMU_ID2ListOfBODs.get(bmu.getID());
			if (obj != null) {
				bmu.recieveBOA((ArrayList<BOD>)obj);
			}			
		}		
	} */
	
	private void calculateFPN() {	
	}
	
	private void sendFPNtoSettlementCo() {
	}
	
	private void sendBOAtoSettlementCo() {
	}
	
	private void recieveFPN() {
	}
	
private ArrayList<ImbalData> calculate2hImbalance(double[] imbalArray, double totalImbal) {
		
		ArrayList<ImbalData> list_2hImbal = new ArrayList<ImbalData>();

		for (int i=0, j=0;  i<imbalArray.length; i++, j++) {
			double vol = ArraysUtils.sum(Arrays.copyOfRange(imbalArray, i, i+1));
			ImbalData imbalData = new ImbalData(vol/1);
			if (Math.abs(vol) >= totalImbal)
				imbalData.flag = true;
		
			list_2hImbal.add(imbalData);
		}
				
		return list_2hImbal;
		
	}
	
    private ArrayList<ImbalData> calculate4hImbalance(ArrayList<ImbalData> list_2hImbal, double totalImbal) {
		
		ArrayList<ImbalData> list_4hImbal = new ArrayList<ImbalData>();
		Object[] arr_2hImbal = list_2hImbal.toArray();
		
		for (int i=0, j=0;  i<arr_2hImbal.length; i+=4, j++) {
			
			double vol = ((ImbalData)arr_2hImbal[i]).getVolume() + ((ImbalData)arr_2hImbal[i+1]).getVolume() 
							+ ((ImbalData)arr_2hImbal[i+2]).getVolume() + ((ImbalData)arr_2hImbal[i+3]).getVolume();
			ImbalData imbalData = new ImbalData(vol/4);

			if (((ImbalData)arr_2hImbal[i]).flag == false && ((ImbalData)arr_2hImbal[i+1]).flag == false 
					&& ((ImbalData)arr_2hImbal[i+2]).flag == false && ((ImbalData)arr_2hImbal[i+3]).flag == false 
					&& Math.abs(vol) >= totalImbal) {
				imbalData.flag = true;
			}
			list_4hImbal.add(imbalData);
		}
		return list_4hImbal;
	}
    
	
    private ArrayList<ImbalData> calculate8hImbalance(ArrayList<ImbalData> list_2hImbal, ArrayList<ImbalData> list_4hImbal, double totalImbal) {

    	ArrayList<ImbalData> list_8hImbal = new ArrayList<ImbalData>();
    	Object[] arr_4hImbal = list_4hImbal.toArray();
    	Object[] arr_2hImbal = list_2hImbal.toArray();

    	ImbalData imbalData;
    	for (int i=0, j=0;  i<arr_4hImbal.length; i+=2, j++) {

    		double vol = ((ImbalData)arr_4hImbal[i]).getVolume() + ((ImbalData)arr_4hImbal[i+1]).getVolume();
    		imbalData = new ImbalData(vol/8);

    		list_8hImbal.add(imbalData);
    	}

    	for (int i=0;  i<list_8hImbal.size(); i++) {

  			
    		if (((ImbalData)arr_4hImbal[2*i]).flag == false && ((ImbalData)arr_4hImbal[2*i+1]).flag == false
    				&& ((ImbalData)arr_2hImbal[4*i]).flag == false && ((ImbalData)arr_2hImbal[4*i+1]).flag == false 
        			&& ((ImbalData)arr_2hImbal[4*i+2]).flag == false && ((ImbalData)arr_2hImbal[4*i+3]).flag == false 
        			&& ((ImbalData)arr_2hImbal[4*i+4]).flag == false && ((ImbalData)arr_2hImbal[4*i+5]).flag == false 
        			&& ((ImbalData)arr_2hImbal[4*i+6]).flag == false && ((ImbalData)arr_2hImbal[4*i+7]).flag == false 
        			&& Math.abs(list_8hImbal.get(i).getVolume()) >= totalImbal) {

    			list_8hImbal.get(i).flag = true;
    		}

    	}

    	return list_8hImbal;
    }
	
	public void updateOldIMBAL(double[] currentIMBALArray, double[] oldIMBALArray) { //PN for previous day
		System.arraycopy(currentIMBALArray, 0, oldIMBALArray, 0, currentIMBALArray.length);
	}  
	
	private WeakHashMap <Integer, ArrayList<ImbalData>> calculatePXImbalance(double[] imbalArray) {

		double totalImbal= Math.abs(ArraysUtils.sum(imbalArray))/ASTEMConsts.PX_IMBAL_FACTOR; // forTest
		
		ArrayList<ImbalData> list_2hImbal = calculate2hImbalance(imbalArray, totalImbal);
		ArrayList<ImbalData> list_4hImbal = calculate4hImbalance(list_2hImbal, totalImbal);
		ArrayList<ImbalData> list_8hImbal = calculate8hImbalance(list_2hImbal, list_4hImbal, totalImbal);
		
		WeakHashMap <Integer, ArrayList<ImbalData>> map_imbalances = new WeakHashMap <Integer, ArrayList<ImbalData>> (); 
		
		map_imbalances.put(ASTEMConsts.PX_PRODUCT_ID_2H, list_2hImbal);
		map_imbalances.put(ASTEMConsts.PX_PRODUCT_ID_4H, list_4hImbal);
		map_imbalances.put(ASTEMConsts.PX_PRODUCT_ID_8H, list_8hImbal);

		return map_imbalances;
	}
	
	private void broadcastPXImbalance(WeakHashMap<Integer, ArrayList<ImbalData>> mapOfImbalType2ImbalData) {
		this.messageBoard.setPxProductList(mapOfImbalType2ImbalData);
	}
		

	@ScheduledMethod(start = Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP, interval = 1, shuffle = false, priority = Consts.SO_PRIORITY_SECOND)
	public void step() {

		//if (Consts.DEBUG) System.out.println("--SO: "+TestHelper.getEnvInfoInString(mainContext));
		
		//TestUtils.printPNs(map_PN);
	
		int settlementPeriod = mainContext.getSettlementPeriod();
		
		if (mainContext.isMarketFirstDay() && settlementPeriod ==0) { //things to do once at day 0 and sp=0 
			//this.list_BMU = getBMUList();
			//this.list_IBMTrader = mainContext.getListOfRegisteredBMTraders();
			this.list_ITrader = mainContext.getListOfTraders();
		     //   System.out.println("list_ITrader.size: "+ list_ITrader.size());
		}
		
		//String[] labels={"NDF:", "INDGEN:", "IMBAL:", "INDMAR:"};
		
		switch (settlementPeriod) {
		
		case  14: 
			map_dmuID2PN = fetchPNs(list_ITrader);
			//TestHelper.printMapOfPNs(map_dmuID2PN);
			arr_NDF = calculateNDF(map_dmuID2PN);
			//System.out.println("NDF" + Arrays.toString(arr_NDF));
			arr_INDGEN= calculateINDGEN(map_dmuID2PN); //indicated generation
			//System.out.println("indGEN" + Arrays.toString(arr_INDGEN));

			arr_IMBAL = calculateIMBAL(arr_NDF, arr_INDGEN);  //imbalance 
			//System.out.println("IMBAL" + Arrays.toString(arr_IMBAL));

			arr_INDMAR = calculateINDMAR(list_ITrader, arr_IMBAL); //idicator margine
			//System.out.println("IndMar" + Arrays.toString(arr_INDMAR));
					
			//TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

			broadcastIMBALandINDMARInfo(arr_IMBAL, arr_oldIMBAL, arr_INDMAR);
			map_imbalType2ImbalData = calculatePXImbalance(arr_IMBAL);
			broadcastPXImbalance(map_imbalType2ImbalData);

		break;
		
		case  22: 
			map_dmuID2PN = fetchPNs(list_ITrader);
			arr_NDF = calculateNDF(map_dmuID2PN);
			arr_INDGEN = calculateINDGEN(map_dmuID2PN);
			arr_IMBAL = calculateIMBAL(arr_NDF, arr_INDGEN);
			//System.out.println("IMBAL" + Arrays.toString(arr_IMBAL));

			arr_INDMAR = calculateINDMAR(list_ITrader, arr_IMBAL);
			//TestHelper.writeOutput("SO_sp", true, labels, arr_NDF, arr_INDGEN, arr_IMBAL, arr_INDMAR);
			//TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

			broadcastIMBALandINDMARInfo(arr_IMBAL, arr_oldIMBAL, arr_INDMAR);
			map_imbalType2ImbalData = calculatePXImbalance(arr_IMBAL);
			broadcastPXImbalance(map_imbalType2ImbalData);

		break;
		
		case  30: 
			map_dmuID2PN = fetchPNs(list_ITrader);
			arr_NDF = calculateNDF(map_dmuID2PN);
			arr_INDGEN = calculateINDGEN(map_dmuID2PN);
			arr_IMBAL = calculateIMBAL(arr_NDF, arr_INDGEN);
			//System.out.println("IMBAL" + Arrays.toString(arr_IMBAL));

			arr_INDMAR = calculateINDMAR(list_ITrader, arr_IMBAL);
			//TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

			broadcastIMBALandINDMARInfo(arr_IMBAL, arr_oldIMBAL, arr_INDMAR);
			map_imbalType2ImbalData = calculatePXImbalance(arr_IMBAL);
			broadcastPXImbalance(map_imbalType2ImbalData);

		break;
		
		case  38: 
			map_dmuID2PN = fetchPNs(list_ITrader);
			arr_NDF = calculateNDF(map_dmuID2PN);
			arr_INDGEN = calculateINDGEN(map_dmuID2PN);
			arr_IMBAL = calculateIMBAL(arr_NDF, arr_INDGEN);
			//System.out.println("IMBAL" + Arrays.toString(arr_IMBAL));

			arr_INDMAR = calculateINDMAR(list_ITrader, arr_IMBAL);
			//TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

			broadcastIMBALandINDMARInfo(arr_IMBAL, arr_oldIMBAL, arr_INDMAR);
		break;

		case 47:
			updateOldIMBAL(arr_IMBAL, arr_oldIMBAL);
			broadcastIMBALandINDMARInfo(arr_IMBAL, arr_oldIMBAL, arr_INDMAR);


			break;
		
		}//end of switch		

		if (!mainContext.isMarketFirstDay()) {					

			map_IBMTrader2ListOfBODs = fetchBODs(list_ITrader); 
			//System.out.println(" ----printBOD before generatingBOAs ");
			//TestHelper.printMapListOfBODs(map_IBMTrader2ListOfBODs);
			//System.out.println("arr_oldIMBAL" + Arrays.toString(arr_oldIMBAL));
			map_IBMTrader2ListOfBOAs = generateBOA(map_IBMTrader2ListOfBODs, settlementPeriod, arr_oldIMBAL);
			//System.out.println(" ***printBOD AFTER generatingBOAs ");
			//TestHelper.printMapListOfBODs(map_IBMTrader2ListOfBODs);
			sendBOAtoEachIBMTrader(map_IBMTrader2ListOfBODs);
			sendBOAtoSettlementCo(map_IBMTrader2ListOfBODs);

			//sendBOAtoEachBMU(list_BMU, map_dmuID2ListOf_BODs);
		}

	}
	
	public SystemOperator(CascadeContext context, SettlementCompany sc, MarketMessageBoard mb) {
		this.mainContext = context;	
		this.settlementCo = sc;
		//this.messageBoard = new MessageBoard();
		this.messageBoard = mb;
		arr_oldIMBAL = new double[context.ticksPerDay];

	}	
	
}
