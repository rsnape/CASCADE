package uk.ac.dmu.iesd.cascade.market.astem.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import repast.simphony.engine.schedule.ScheduledMethod;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IPxTrader;
import uk.ac.dmu.iesd.cascade.market.data.BSOD;
import uk.ac.dmu.iesd.cascade.market.data.PxPD;
import uk.ac.dmu.iesd.cascade.market.astem.test.TestHelper;
import uk.ac.dmu.iesd.cascade.market.astem.util.SortComparatorUtils;


/**
 * The <em>PowerExchange</em> class embodies another important actor (agent) in the electricity market.
 * This is where the short term bilateral transactions take place.
 * It also uses <em>MessageBoard</em> ({@link MarketMessageBoard}) to broadcast pricing 
 * information used by players in the electricity market. 
 * 
 * @see uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/09
 * 
 */

public class PowerExchange {
	private CascadeContext mainContext;
	private MarketMessageBoard messageBoard;
	private ArrayList<IPxTrader> list_IPxTrader;  
	private ArrayList<BSOD> list_BSOD;
	
	private ArrayList<PxPD> list_PxPD;
	//private HashMap<Integer, ArrayList<BSOD>> map_dmuID2ListOfBSOD;
	
	private WeakHashMap<IPxTrader, ArrayList<BSOD>> map_dmu2ListOfBSOD;

	private ArrayList<PxPD> list_PX_products; //make sure if needed 
	
	private static double[] arr_reversePrice;
	private double[] arr_sumOfProductsTraded;
	private double[] arr_sumOfVolumesTraded;
	
	/*private ArrayList<BMU> getBMUList() {
		ArrayList<BMU> aListOfBMUs = new ArrayList<BMU>();

		Network bmuNet = mainContext.getNetworkOfRegisteredBMUs();
		Iterable<RepastEdge> edgeIter = bmuNet.getEdges();
		if(Consts.VERBOSE) 
			System.out.println("PX has access to "+ bmuNet.size() + " registered BMUs");
		for (RepastEdge edge : edgeIter) {
			Object obj = edge.getTarget();
			if (obj instanceof BMU)
				aListOfBMUs.add((BMU) obj);    		
			else
				System.err.println("PX:: Wrong Class Type: BMU agent is expected");
		}
		return aListOfBMUs;
	} */

	private  WeakHashMap<IPxTrader, ArrayList<BSOD>> fetchBSOfromIPxTraders(List<IPxTrader> listOfIPxTraders){
		//System.out.println("------ PX: fetchBSOD");
		//Received BSOD are in the form of list
        WeakHashMap<IPxTrader, ArrayList<BSOD>> mapOfIPxTrader2ListOfBSOD = new  WeakHashMap<IPxTrader, ArrayList<BSOD>>();
		
		for (IPxTrader bmu : listOfIPxTraders)			
			mapOfIPxTrader2ListOfBSOD.put(bmu, bmu.getListOfBSOD());
		
		return mapOfIPxTrader2ListOfBSOD;
	}
	
	/*private ArrayList<BSOD> fetchBSOfromBMUs_old(List<BMU> listOfBMUs, HashMap<Integer, ArrayList<BSOD>> mapListOfBSODs){
		//System.out.println("------ PX: fetchBSOD");
		//Received BSOD are in the form of list
		ArrayList<BSOD> bsodList = new ArrayList<BSOD>();
		if (!mapListOfBSODs.isEmpty())
			mapListOfBSODs.clear();
		
		for (BMU bmu : listOfBMUs){	
			ArrayList<BSOD> bmuBSOD = bmu.getListOfBSO();
			mapListOfBSODs.put(bmu.getID(), bmuBSOD);
			for (BSOD bsod : bmuBSOD){	
				bsodList.add(bsod);
			}
		}
		return bsodList;
	} */
	
	private WeakHashMap<IPxTrader, ArrayList<BSOD>> generateAcceptance(WeakHashMap<IPxTrader, ArrayList<BSOD>> mapOfIPxTraders2ListOfBSODs){

		//System.out.println("Px:: generateAcceptance()");
		ArrayList<ArrayList <BSOD>> buyersListOfBSODList = new ArrayList <ArrayList <BSOD>>();
		ArrayList<ArrayList <BSOD>> sellersListOfBSODList = new ArrayList <ArrayList <BSOD>>();

		Set<IPxTrader> bmuSet = mapOfIPxTraders2ListOfBSODs.keySet();

		for (IPxTrader bmu : bmuSet) {
			/*if (bmu.getType() == Consts.BMU_TYPE_T_GEN) 
				sellersListOfBSODList.add(mapOfBMUs2ListOfBSODs.get(bmu));
			else if (bmu.getType() == Consts.BMU_TYPE_S_DEM) 
				buyersListOfBSODList.add(mapOfBMUs2ListOfBSODs.get(bmu)); */
			switch (bmu.getCategory()) {
			case GEN_T:
				sellersListOfBSODList.add(mapOfIPxTraders2ListOfBSODs.get(bmu));
				break;
			case DEM_S:
				buyersListOfBSODList.add(mapOfIPxTraders2ListOfBSODs.get(bmu)); 
				break;
			}
		}

		ArrayList<BSOD> buyersBSODList = new ArrayList<BSOD>();
		ArrayList<BSOD> sellersBSODList = new ArrayList<BSOD>();

		for (ArrayList<BSOD> bsodList : buyersListOfBSODList) 
			buyersBSODList.addAll(bsodList);

		for (ArrayList<BSOD> bsodList : sellersListOfBSODList) 
			sellersBSODList.addAll(bsodList);

		ArrayList<BSOD> buyersBSODListPosVol = new ArrayList<BSOD>();
		ArrayList<BSOD> buyersBSODListNegVol = new ArrayList<BSOD>();

		double sumPosVolBuyers=0;
		double sumNegVolBuyers=0;

		for (BSOD bsod : buyersBSODList) {
			if (bsod.getVolume() >=0) {
				buyersBSODListPosVol.add(bsod);
				sumPosVolBuyers+=bsod.getVolume();
			}
			else  {
				buyersBSODListNegVol.add(bsod);
				sumNegVolBuyers+=bsod.getVolume();
			}
		}

		double sumPosVolSellers=0;
		double sumNegVolSellers=0;

		ArrayList<BSOD> sellersBSODListPosVol = new ArrayList<BSOD>();
		ArrayList<BSOD> sellersBSODListNegVol = new ArrayList<BSOD>();

		for (BSOD bsod : sellersBSODList) {
			if (bsod.getVolume() >=0) {
				sellersBSODListPosVol.add(bsod);
				sumPosVolSellers += bsod.getVolume();
			}
			else  {
				sellersBSODListNegVol.add(bsod);
				sumNegVolSellers += bsod.getVolume();
			}
		}

		//System.out.println("BEfor sort: buyersBSODListPosVol:");
		//TestHelper.printListOfBSOD(buyersBSODListPosVol);

		//System.out.println("before sort: buyersBSODListNegVol:");
		//TestHelper.printListOfBSOD(buyersBSODListNegVol);

		

		Collections.sort(buyersBSODListPosVol, SortComparatorUtils.PX_PRICE_ASCENDING_ORDER); 
		Collections.sort(buyersBSODListNegVol, SortComparatorUtils.PX_PRICE_ASCENDING_ORDER); 

		//System.out.println("AFter sort (buyers):");
		//TestHelper.printListOfBSOD(buyersBSODListPosVol);
		//TestHelper.printListOfBSOD(buyersBSODListNegVol);

		//System.out.println("BEfor sort: sellersBSODListPosVol:");
		//TestHelper.printListOfBSOD(sellersBSODListPosVol);

		//System.out.println("before sort: sellersBSODListNegVol:");
		//TestHelper.printListOfBSOD(sellersBSODListNegVol);


		Collections.sort(sellersBSODListPosVol, SortComparatorUtils.PX_PRICE_DESCENDING_ORDER); 
		Collections.sort(sellersBSODListNegVol, SortComparatorUtils.PX_PRICE_DESCENDING_ORDER); 

		//System.out.println("AFter sort (sellers):");
		//TestHelper.printListOfBSOD(sellersBSODListPosVol);
		//TestHelper.printListOfBSOD(sellersBSODListNegVol);

		double volToBeBought =0;
		double volToBeSold =0;

		if (sumPosVolBuyers <= sumPosVolSellers) 
			volToBeBought = sumPosVolBuyers;
		else volToBeBought = sumPosVolSellers;

		if (Math.abs(sumNegVolBuyers) <= Math.abs(sumNegVolSellers)) 
			volToBeSold = sumNegVolBuyers;
		else volToBeSold = sumNegVolSellers;

		for (BSOD bsod : buyersBSODListNegVol) {
			if (volToBeSold <0) {
				bsod.accepted = true;
				volToBeSold = volToBeSold-bsod.getVolume();
				if (volToBeSold > 0) {
					bsod.setVolume(bsod.getVolume()+volToBeSold); 
				}
			}			 
		}

		for (BSOD bsod : sellersBSODListPosVol) {
			if (volToBeBought > 0) {
				bsod.accepted = true;
				volToBeBought = volToBeBought-bsod.getVolume();
				if (volToBeBought < 0) {
					bsod.setVolume(bsod.getVolume()+volToBeBought); 
				}
			}			 
		}

		//System.out.println(" Accepted BSODs: ");
		//TestHelper.printMapListOfBSODs(mapOfBMUs2ListOfBSODs);

		return mapOfIPxTraders2ListOfBSODs;

	}
	
	private void sendAcceptanceToEachIPxTrader(WeakHashMap<IPxTrader, ArrayList<BSOD>> mapOfIPxTrader2ListOfBSODs) {	
		//System.out.println("PX: sendAcceptanceToEachBMU() called");
		//System.out.println("size of listOfBMUs: "+listOfBMUs.size());
		//System.out.println("size of sendAcceptanceToEachBMU: "+mapOfBMU2ListOfBSODs.size());

		Set <IPxTrader> bmuSet =  mapOfIPxTrader2ListOfBSODs.keySet();
		
		for (IPxTrader bmu: bmuSet) 
			bmu.recieveBSOA((ArrayList<BSOD>)mapOfIPxTrader2ListOfBSODs.get(bmu));
	}
	
	private ArrayList<PxPD> getImbalDataFromSOMessageBoard() {
		
		return messageBoard.getPxProductList();
		
	}
	
	private void calculatePartialMIP(WeakHashMap<IPxTrader, ArrayList<BSOD>> mapOfIPxTraders2ListOfBSODs, double[] sumOfProducts, double[] sumOfVolumes) {
			
		ArrayList<BSOD> listOfBSODs = new ArrayList<BSOD>();
		
		Collection<ArrayList <BSOD>> valCollection = mapOfIPxTraders2ListOfBSODs.values();
		for (ArrayList<BSOD> listBSOD : valCollection) 
			listOfBSODs.addAll(listBSOD);
		for ( int sp = 0; sp < mainContext.ticksPerDay; sp++){
			for (BSOD bsod: listOfBSODs) {
				if (bsod.getStartSPIndex() <=sp && (bsod.getStartSPIndex()+bsod.getProductID())>=sp && bsod.accepted==true){
					sumOfProducts[sp]+= (Math.abs(bsod.getVolume()) * bsod.getPrice());
					sumOfVolumes[sp] += Math.abs(bsod.getVolume());
				}
			}	
		}		
	}
	
	private double[] calculateMIP(double[] sumOfProducts, double[] sumOfVolumes) {
		
		double[] arr_MIP= new double[mainContext.ticksPerDay];
		for (int i=0; i<mainContext.ticksPerDay; i++) {
			if ( sumOfVolumes[i] !=0)
				arr_MIP[i] = sumOfProducts[i] / sumOfVolumes[i];
		}
		//System.out.println("arr_MIP: "+Arrays.toString(arr_MIP));

		return arr_MIP;
	}
	
	public static double[] getReversePrice() {
		return arr_reversePrice;
	}
			
	
	@ScheduledMethod(start = Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP, interval = 1, shuffle = false, priority = Consts.PX_PRIORITY_FOURTH)
	public void step() {
		
		//if (Consts.DEBUG) System.out.println("--Px: "+TestHelper.getEnvInfoInString(mainContext));

		int settlementPeriod = mainContext.getSettlementPeriod();
		
		if (mainContext.isMarketFirstDay() && settlementPeriod ==0) { //things to do once at day 0 and sp=0 
			//this.list_IPxTrader = mainContext.getListOfRegisteredPxTraders();
			this.list_IPxTrader = mainContext.getListOfPxTraders();
		}

		switch (settlementPeriod) {
		
		case 15: // test: to check whether is correct
			list_PxPD = getImbalDataFromSOMessageBoard();
			//TestHelper.printListOfPxPD(list_PxPD);
			break;
		case 20: 
			//check this later
			//list_BSOD = fetchBSOfromBMUs(list_BMU, map_dmuID2ListOfBSOD);
			map_dmu2ListOfBSOD = fetchBSOfromIPxTraders(list_IPxTrader);
			//TestHelper.printMapListOfBSODs(map_dmu2ListOfBSOD);
			map_dmu2ListOfBSOD = generateAcceptance(map_dmu2ListOfBSOD);
			sendAcceptanceToEachIPxTrader(map_dmu2ListOfBSOD);
			calculatePartialMIP(map_dmu2ListOfBSOD, arr_sumOfProductsTraded, arr_sumOfVolumesTraded);	
			break;
		case 23: 
			list_PxPD = getImbalDataFromSOMessageBoard();
			break;
		case 28: 
			//list_BSOD = fetchBSOfromBMUs(list_BMU, map_dmuID2ListOfBSOD);
			map_dmu2ListOfBSOD = fetchBSOfromIPxTraders(list_IPxTrader);
			map_dmu2ListOfBSOD = generateAcceptance(map_dmu2ListOfBSOD);
			sendAcceptanceToEachIPxTrader(map_dmu2ListOfBSOD);
			calculatePartialMIP(map_dmu2ListOfBSOD, arr_sumOfProductsTraded, arr_sumOfVolumesTraded);
			
			break;
		case 31:
			list_PxPD = getImbalDataFromSOMessageBoard();
			break;
		case 36: 
			map_dmu2ListOfBSOD = fetchBSOfromIPxTraders(list_IPxTrader);
			map_dmu2ListOfBSOD = generateAcceptance(map_dmu2ListOfBSOD);
			sendAcceptanceToEachIPxTrader(map_dmu2ListOfBSOD);
			calculatePartialMIP(map_dmu2ListOfBSOD, arr_sumOfProductsTraded, arr_sumOfVolumesTraded);

			break;
		case 47: 
			this.arr_reversePrice = calculateMIP(arr_sumOfProductsTraded, arr_sumOfVolumesTraded);
			messageBoard.setMIP(arr_reversePrice);
			//System.out.println(arr_reversePrice[settlementPeriod]);
			
			Arrays.fill(arr_sumOfProductsTraded, 0);
			Arrays.fill(arr_sumOfVolumesTraded, 0);

			break;
		}
	}
	
	public PowerExchange(CascadeContext context, MarketMessageBoard mb) {
		this.mainContext = context;	
		this.messageBoard = mb;
		this.arr_reversePrice = new double[context.ticksPerDay];
		this.arr_sumOfProductsTraded = new double[context.ticksPerDay];
		this.arr_sumOfVolumesTraded = new double[context.ticksPerDay];
	}

}
