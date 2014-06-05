package uk.ac.dmu.iesd.cascade.market.astem.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;

import repast.simphony.engine.schedule.ScheduledMethod;
import uk.ac.dmu.iesd.cascade.agents.aggregators.BOD;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IBMTrader;
import uk.ac.dmu.iesd.cascade.market.ITrader;
import uk.ac.dmu.iesd.cascade.market.astem.test.TestHelper;

/**
 *
 * <em>SettlementCompany</em> is used by <em>SystemOperator</em> ({@link SystemOperator})
 *  as settlement agent in the electricity market. 
 * In the UK context, this role is played by the <tt>Elexon</tt>.  
 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/09
 */

public class SettlementCompany {
	
	private CascadeContext mainContext;
	private ArrayList<ITrader> list_ITrader;
	//private ArrayList<BOD> list_histBOA;
	//private HashMap map_dmuID2ListOf_BODs;
	
	private LinkedHashMap <Integer, double[]> map_IBMTraderID2PreviousDayPNs;
	private LinkedHashMap <IBMTrader, ArrayList<BOD>> map_IBMTrader2listBOAs;
	private double[] arr_previousDayIMBAL;
	private double[] arr_mainPrice;
	private double[] arr_reversePrice;
	private double[] arr_SSP;
	private double[] arr_SBP;
	
	class SysPriceVolData {
		double price;
		double totalImbal;
		SysPriceVolData(double p, double tImbal){
			price =p;
			totalImbal = tImbal;
		}
	}
	
	
	//private SystemOperator so;
	

	/*private ArrayList<BMU> getBMUList() {
		ArrayList<BMU> aListOfBMUs = new ArrayList<BMU>();

		Network bmuNet = mainContext.getNetworkOfRegisteredBMUs();
		Iterable<RepastEdge> edgeIter = bmuNet.getEdges();
		if(Consts.VERBOSE) 
			this.mainContext.logger.debug("SCo has access to "+ bmuNet.size() + " registered BMUs");
		for (RepastEdge edge : edgeIter) {
			Object obj = edge.getTarget();
			if (obj instanceof BMU)
				aListOfBMUs.add((BMU) obj);    		
			else
				System.err.println("SCo:: Wrong Class Type: BMU agent is expected");
		}
		return aListOfBMUs;
	} */
	
	public double getSSPforEachSP() {
		return this.arr_SSP[mainContext.getSettlementPeriod()];
	}
	public double getSBPforEachSP() {
		return this.arr_SBP[mainContext.getSettlementPeriod()];
	}
	
	public double getPrevDayIMBALforEachSP() {
		return this.arr_previousDayIMBAL[mainContext.getSettlementPeriod()];
	}
	
	
	public LinkedHashMap<Integer, double[]> fetchPreviousDayPNs(ArrayList<ITrader> listOfTraders){
		
		this.mainContext.logger.trace("sc:: fetchPreviousDayPNs()");
		
		LinkedHashMap<Integer, double[]> mapOfOldPNs = new LinkedHashMap<Integer, double[]>();
		for (ITrader bmu : listOfTraders)
			mapOfOldPNs.put(bmu.getID(), bmu.getPreviousDayPN());
		
		return mapOfOldPNs;
	}
	
	public void calculateOfferVolumes(){
		//used every sp
	}
	
	public void broadcastCost() {
		//called every sp
	}
	
	/*public void registerSystemOperator (SystemOperator sysOp) {
		this.so= so;
	} */
	
	/*private LinkedHashMap<BMU, ArrayList<BOD>>  fetchHistoricalBOA(ArrayList<BMU> listOfBMUs) {
		this.mainContext.logger.debug("SC: fetchHistoricalBOA() is called");
		
		LinkedHashMap<BMU, ArrayList<BOD>> mapOfListOfHistBOAs = new LinkedHashMap<BMU, ArrayList<BOD>>();
				
		for (BMU bmu : listOfBMUs)
			mapOfListOfHistBOAs.put(bmu, bmu.getHistoricalBOA());
				
		return mapOfListOfHistBOAs;
	} */
	
	public void recieveBOAs(LinkedHashMap<IBMTrader, ArrayList<BOD>> mapOfIBMTrader2ListOfBOAs) {
		map_IBMTrader2listBOAs = mapOfIBMTrader2ListOfBOAs;
	}
	
	
	private SysPriceVolData calculateSSP(LinkedHashMap <IBMTrader, ArrayList<BOD>> mapOfIBMTraders2ListOfBOAs) {
		
		ArrayList<BOD> listOfBODs = new ArrayList<BOD>();
		
	    //TestHelper.printMapListOfBODs(mapOfIBMTraders2ListOfBOAs);
		this.mainContext.logger.trace("SC: mapOfIBMTraders2ListOfBOAs.size: "+mapOfIBMTraders2ListOfBOAs.size());

		Collection<ArrayList <BOD>> valCol = mapOfIBMTraders2ListOfBOAs.values();
		for (ArrayList<BOD> arrBOD : valCol) 
			listOfBODs.addAll(arrBOD);
		
		double totalBidsVol=0;
		double sumOfProducts=0;
		for (BOD bod : listOfBODs) {
			if(bod.getPairID() < 0 && bod.isAccepted == true)  {
				totalBidsVol = totalBidsVol + bod.getLevel();
				sumOfProducts = sumOfProducts + (bod.getLevel() * bod.getSubmittedBO());
				this.mainContext.logger.trace("SC: totalBidsVol: "+totalBidsVol);
				this.mainContext.logger.trace("SC: sumOfProducts: "+sumOfProducts);
			}
		}
		
		double ssp = sumOfProducts/totalBidsVol;
		
		return new SysPriceVolData(ssp, totalBidsVol);	

	}
	
	
	private SysPriceVolData calculateSBP(LinkedHashMap <IBMTrader, ArrayList<BOD>> mapOfIBMTraders2ListOfBOAs) {
		
	ArrayList<BOD> listOfBODs = new ArrayList<BOD>();
		
		Collection<ArrayList <BOD>> valCol = mapOfIBMTraders2ListOfBOAs.values();
		for (ArrayList<BOD> arrBOD : valCol) 
			listOfBODs.addAll(arrBOD);
		
		double totalOffersVol=0;
		double sumOfProducts=0;
		for (BOD bod : listOfBODs) {
			
			if(bod.getPairID() > 0 && bod.isAccepted == true)  {
				
				totalOffersVol = totalOffersVol + bod.getLevel();
				sumOfProducts = sumOfProducts + (bod.getLevel() * bod.getSubmittedBO());
			
			}
		}
		
		double sbp = sumOfProducts/totalOffersVol;
		return new SysPriceVolData(sbp, totalOffersVol);	
		
	}
	//int da=5;
	
	@ScheduledMethod(start = Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP, interval = 1, shuffle = false, priority = Consts.SC_PRIORITY_THIRD)
	public void step() {

		this.mainContext.logger.trace("--SC: "+TestHelper.getEnvInfoInString(mainContext));

		int settlementPeriod = mainContext.getSettlementPeriod();
		
		if (mainContext.isMarketFirstDay() && settlementPeriod ==0) { //things to do once at day 0 and sp=0 
			//this.list_IBMTrader = mainContext.getListOfRegisteredBMTraders();
			this.list_ITrader = mainContext.getListOfTraders();
		}

		if (settlementPeriod == 47) {
			map_IBMTraderID2PreviousDayPNs = fetchPreviousDayPNs(list_ITrader);  
			arr_previousDayIMBAL = MarketMessageBoard.getPreviousDayIMBAL();

		}

		if (mainContext.getDayCount() > ((Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP)/48)) { // second day
			if (settlementPeriod == 0)
				arr_reversePrice = PowerExchange.getReversePrice();  //JRS TODO: Why do we get this from PowerExchange, not MessageBoard?
			
			SysPriceVolData spvd_SBP = null; 
			SysPriceVolData spvd_SSP = null;
				
			if (arr_previousDayIMBAL[settlementPeriod] >= 0) {
				spvd_SSP = calculateSSP(map_IBMTrader2listBOAs);
				arr_mainPrice[settlementPeriod] = spvd_SSP.price;
			}
			else if (arr_previousDayIMBAL[settlementPeriod] < 0) {
				spvd_SBP = calculateSBP(map_IBMTrader2listBOAs);
				arr_mainPrice[settlementPeriod] = spvd_SBP.price;
			}
			
			
			if (arr_previousDayIMBAL[settlementPeriod] >= 0) {
				arr_SSP[settlementPeriod] = arr_mainPrice[settlementPeriod];
				arr_SBP[settlementPeriod] = arr_reversePrice[settlementPeriod];

			}
			else if (arr_previousDayIMBAL[settlementPeriod] < 0) {
				arr_SSP[settlementPeriod] = arr_reversePrice[settlementPeriod];
				arr_SBP[settlementPeriod] = arr_mainPrice[settlementPeriod];

			}

			
			/***
			 * this makes sure that if the sell price is higher than the buy price, they equalise
			 * if either goes to zero, they both go to zero.
			 * 
			 * If they're both happen to be zero - they're both set to an average of the prior two periods.
			 */
			if (arr_SSP[settlementPeriod] > arr_SBP[settlementPeriod] && arr_SBP[settlementPeriod] != 0) 
				arr_SSP[settlementPeriod] = arr_SBP[settlementPeriod];
			else if (arr_SBP[settlementPeriod] == 0.0 && arr_SSP[settlementPeriod] != 0.0)
				arr_SBP[settlementPeriod] = arr_SSP[settlementPeriod];
			else if (arr_SSP[settlementPeriod] == 0.0 && arr_SBP[settlementPeriod] != 0.0)
				arr_SBP[settlementPeriod] = arr_SSP[settlementPeriod];
			else if (arr_SBP[settlementPeriod] == 0.0 && arr_SSP[settlementPeriod] == 0.0){
				arr_SBP[settlementPeriod] = (arr_SBP[settlementPeriod-1]+arr_SBP[settlementPeriod-2])/2; 
				arr_SSP[settlementPeriod] = arr_SBP[settlementPeriod]; 
			}
				
			//System.out.print(this.getSBPforEachSP());
			
			this.mainContext.logger.trace(", "+this.getSSPforEachSP());
			if (settlementPeriod == 47) {
				this.mainContext.logger.debug("SSP" + mainContext.getDayCount() + ": " + Arrays.toString(arr_SSP));
				this.mainContext.logger.debug("SBP" + mainContext.getDayCount() + ": " + Arrays.toString(arr_SBP));
			}
		}
		
		//TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

	}

	
	public SettlementCompany(CascadeContext context){
		this.mainContext = context;	
		arr_mainPrice = new double[context.ticksPerDay];
		arr_SSP = new double[context.ticksPerDay];
		arr_SBP = new double[context.ticksPerDay];
		Arrays.fill(arr_mainPrice, 0);
		Arrays.fill(arr_SSP, 0);
		Arrays.fill(arr_SBP, 0);

	}

}
