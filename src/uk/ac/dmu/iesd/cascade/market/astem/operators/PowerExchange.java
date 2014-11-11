package uk.ac.dmu.iesd.cascade.market.astem.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import repast.simphony.engine.schedule.ScheduledMethod;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IPxTrader;
import uk.ac.dmu.iesd.cascade.market.astem.test.TestHelper;
import uk.ac.dmu.iesd.cascade.market.astem.util.SortComparatorUtils;
import uk.ac.dmu.iesd.cascade.market.data.BSOD;
import uk.ac.dmu.iesd.cascade.market.data.PxPD;

/**
 * The <em>PowerExchange</em> class embodies another important actor (agent) in
 * the electricity market. This is where the short term bilateral transactions
 * take place. It also uses <em>MessageBoard</em> ({@link MarketMessageBoard})
 * to broadcast pricing information used by players in the electricity market.
 * 
 * @see uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/09
 * 
 */

public class PowerExchange
{
	private CascadeContext mainContext;
	private MarketMessageBoard messageBoard;
	private ArrayList<IPxTrader> list_IPxTrader;
	private ArrayList<BSOD> list_BSOD;

	private ArrayList<PxPD> list_PxPD;
	// private HashMap<Integer, ArrayList<BSOD>> map_dmuID2ListOfBSOD;

	private LinkedHashMap<IPxTrader, ArrayList<BSOD>> map_dmu2ListOfBSOD;

	private ArrayList<PxPD> list_PX_products; // make sure if needed

	private static double[] arr_reversePrice;
	private double[] arr_sumOfProductsTraded;
	private double[] arr_sumOfVolumesTraded;

	/*
	 * private ArrayList<BMU> getBMUList() { ArrayList<BMU> aListOfBMUs = new
	 * ArrayList<BMU>();
	 * 
	 * Network bmuNet = mainContext.getNetworkOfRegisteredBMUs();
	 * Iterable<RepastEdge> edgeIter = bmuNet.getEdges(); if(Consts.VERBOSE)
	 * this.mainContext.logger.debug("PX has access to "+ bmuNet.size() +
	 * " registered BMUs"); for (RepastEdge edge : edgeIter) { Object obj =
	 * edge.getTarget(); if (obj instanceof BMU) aListOfBMUs.add((BMU) obj);
	 * else System.err.println("PX:: Wrong Class Type: BMU agent is expected");
	 * } return aListOfBMUs; }
	 */

	private LinkedHashMap<IPxTrader, ArrayList<BSOD>> fetchBSOfromIPxTraders(List<IPxTrader> listOfIPxTraders)
	{
		this.mainContext.logger.trace("------ PX: fetchBSOD");
		// Received BSOD are in the form of list
		LinkedHashMap<IPxTrader, ArrayList<BSOD>> mapOfIPxTrader2ListOfBSOD = new LinkedHashMap<IPxTrader, ArrayList<BSOD>>();

		for (IPxTrader bmu : listOfIPxTraders)
		{
			mapOfIPxTrader2ListOfBSOD.put(bmu, bmu.getListOfBSOD());
		}

		return mapOfIPxTrader2ListOfBSOD;
	}

	/*
	 * private ArrayList<BSOD> fetchBSOfromBMUs_old(List<BMU> listOfBMUs,
	 * HashMap<Integer, ArrayList<BSOD>> mapListOfBSODs){
	 * this.mainContext.logger.trace("------ PX: fetchBSOD"); //Received BSOD
	 * are in the form of list ArrayList<BSOD> bsodList = new ArrayList<BSOD>();
	 * if (!mapListOfBSODs.isEmpty()) mapListOfBSODs.clear();
	 * 
	 * for (BMU bmu : listOfBMUs){ ArrayList<BSOD> bmuBSOD = bmu.getListOfBSO();
	 * mapListOfBSODs.put(bmu.getID(), bmuBSOD); for (BSOD bsod : bmuBSOD){
	 * bsodList.add(bsod); } } return bsodList; }
	 */

	private LinkedHashMap<IPxTrader, ArrayList<BSOD>> generateAcceptance(
			LinkedHashMap<IPxTrader, ArrayList<BSOD>> mapOfIPxTraders2ListOfBSODs)
	{

		this.mainContext.logger.trace("Px:: generateAcceptance()");
		ArrayList<ArrayList<BSOD>> buyersListOfBSODList = new ArrayList<ArrayList<BSOD>>();
		ArrayList<ArrayList<BSOD>> sellersListOfBSODList = new ArrayList<ArrayList<BSOD>>();

		Set<IPxTrader> bmuSet = mapOfIPxTraders2ListOfBSODs.keySet();
		for (IPxTrader bmu : bmuSet)
		{
			/*
			 * if (bmu.getType() == Consts.BMU_TYPE_T_GEN)
			 * sellersListOfBSODList.add(mapOfBMUs2ListOfBSODs.get(bmu)); else
			 * if (bmu.getType() == Consts.BMU_TYPE_S_DEM)
			 * buyersListOfBSODList.add(mapOfBMUs2ListOfBSODs.get(bmu));
			 */
			switch (bmu.getCategory())
			{
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
		{
			buyersBSODList.addAll(bsodList);
		}

		for (ArrayList<BSOD> bsodList : sellersListOfBSODList)
		{
			sellersBSODList.addAll(bsodList);
		}

		ArrayList<BSOD> buyersBSODListPosVol = new ArrayList<BSOD>();
		ArrayList<BSOD> buyersBSODListNegVol = new ArrayList<BSOD>();

		double sumPosVolBuyers = 0;
		double sumNegVolBuyers = 0;

		for (BSOD bsod : buyersBSODList)
		{
			if (bsod.getVolume() >= 0)
			{
				buyersBSODListPosVol.add(bsod);
				sumPosVolBuyers += bsod.getVolume();
			}
			else
			{
				buyersBSODListNegVol.add(bsod);
				sumNegVolBuyers += bsod.getVolume();
			}
		}

		double sumPosVolSellers = 0;
		double sumNegVolSellers = 0;

		ArrayList<BSOD> sellersBSODListPosVol = new ArrayList<BSOD>();
		ArrayList<BSOD> sellersBSODListNegVol = new ArrayList<BSOD>();

		for (BSOD bsod : sellersBSODList)
		{
			if (bsod.getVolume() >= 0)
			{
				sellersBSODListPosVol.add(bsod);
				sumPosVolSellers += bsod.getVolume();
			}
			else
			{
				sellersBSODListNegVol.add(bsod);
				sumNegVolSellers += bsod.getVolume();
			}
		}

		this.mainContext.logger.trace("BEfor sort: buyersBSODListPosVol:");
		// TestHelper.printListOfBSOD(buyersBSODListPosVol);

		this.mainContext.logger.trace("before sort: buyersBSODListNegVol:");
		// TestHelper.printListOfBSOD(buyersBSODListNegVol);

		Collections.sort(buyersBSODListPosVol, SortComparatorUtils.PX_PRICE_ASCENDING_ORDER);
		Collections.sort(buyersBSODListNegVol, SortComparatorUtils.PX_PRICE_ASCENDING_ORDER);

		this.mainContext.logger.trace("AFter sort (buyers):");
		// TestHelper.printListOfBSOD(buyersBSODListPosVol);
		// TestHelper.printListOfBSOD(buyersBSODListNegVol);

		this.mainContext.logger.trace("BEfor sort: sellersBSODListPosVol:");
		// TestHelper.printListOfBSOD(sellersBSODListPosVol);

		this.mainContext.logger.trace("before sort: sellersBSODListNegVol:");
		// TestHelper.printListOfBSOD(sellersBSODListNegVol);

		Collections.sort(sellersBSODListPosVol, SortComparatorUtils.PX_PRICE_DESCENDING_ORDER);
		Collections.sort(sellersBSODListNegVol, SortComparatorUtils.PX_PRICE_DESCENDING_ORDER);

		this.mainContext.logger.trace("AFter sort (sellers):");
		// TestHelper.printListOfBSOD(sellersBSODListPosVol);
		// TestHelper.printListOfBSOD(sellersBSODListNegVol);

		double volToBeBought = 0;
		double volToBeSold = 0;

		if (sumPosVolBuyers <= sumPosVolSellers)
		{
			volToBeBought = sumPosVolBuyers;
		}
		else
		{
			volToBeBought = sumPosVolSellers;
		}

		if (Math.abs(sumNegVolBuyers) <= Math.abs(sumNegVolSellers))
		{
			volToBeSold = sumNegVolBuyers;
		}
		else
		{
			volToBeSold = sumNegVolSellers;
		}

		for (BSOD bsod : buyersBSODListNegVol)
		{
			if (volToBeSold < 0)
			{
				bsod.accepted = true;
				volToBeSold = volToBeSold - bsod.getVolume();
				if (volToBeSold > 0)
				{
					bsod.setVolume(bsod.getVolume() + volToBeSold);
				}
			}
		}

		for (BSOD bsod : sellersBSODListPosVol)
		{
			if (volToBeBought > 0)
			{
				bsod.accepted = true;
				volToBeBought = volToBeBought - bsod.getVolume();
				if (volToBeBought < 0)
				{
					bsod.setVolume(bsod.getVolume() + volToBeBought);
				}
			}
		}

		this.mainContext.logger.trace(" Accepted BSODs: ");
		// TestHelper.printMapListOfBSODs(mapOfBMUs2ListOfBSODs);

		return mapOfIPxTraders2ListOfBSODs;

	}

	private void sendAcceptanceToEachIPxTrader(LinkedHashMap<IPxTrader, ArrayList<BSOD>> mapOfIPxTrader2ListOfBSODs)
	{
		this.mainContext.logger.trace("PX: sendAcceptanceToEachBMU() called");
		this.mainContext.logger.trace("size of listOfBMUs: " + this.list_IPxTrader.size());
		this.mainContext.logger.trace("size of sendAcceptanceToEachBMU: " + this.map_dmu2ListOfBSOD.size());

		Set<IPxTrader> bmuSet = mapOfIPxTrader2ListOfBSODs.keySet();

		for (IPxTrader bmu : bmuSet)
		{
			bmu.recieveBSOA(mapOfIPxTrader2ListOfBSODs.get(bmu));
		}

	}

	private ArrayList<PxPD> getImbalDataFromSOMessageBoard()
	{

		return this.messageBoard.getPxProductList();

	}

	/***
	 * calculates the sum of price*volume and volumes already traded in this
	 * round of the market operation (i.e. this day for the day ahead as
	 * currently configured
	 * 
	 * these are needed to calculate the Market index price for each settlement
	 * period of the following day as well as the imbalance to be resolved by
	 * the balancing market.
	 * 
	 * @param mapOfIPxTraders2ListOfBSODs
	 * @param sumOfProducts
	 * @param sumOfVolumes
	 */
	private void calculatePartialMIP(LinkedHashMap<IPxTrader, ArrayList<BSOD>> mapOfIPxTraders2ListOfBSODs, double[] sumOfProducts,
			double[] sumOfVolumes)
	{

		ArrayList<BSOD> listOfBSODs = new ArrayList<BSOD>();

		Collection<ArrayList<BSOD>> valCollection = mapOfIPxTraders2ListOfBSODs.values();
		for (ArrayList<BSOD> listBSOD : valCollection)
		{
			listOfBSODs.addAll(listBSOD);
		}
		for (int sp = 0; sp < this.mainContext.ticksPerDay; sp++)
		{
			for (BSOD bsod : listOfBSODs)
			{
				if (bsod.getStartSPIndex() <= sp && (bsod.getStartSPIndex() + bsod.getProductID()) >= sp && bsod.accepted == true)
				{
					sumOfProducts[sp] += (Math.abs(bsod.getVolume()) * bsod.getPrice());
					sumOfVolumes[sp] += Math.abs(bsod.getVolume());
				}
			}
		}
	}

	/***
	 * This calculates the Market index prices based on the volumes of accepted
	 * bids and offers and the volume*price
	 * 
	 * We get a volume-weighted average market price of electricity. This is
	 * used at the end of one day to give the market price of electricity in
	 * each settlement period of the following day.
	 * 
	 * @param sumOfProducts
	 * @param sumOfVolumes
	 * @return
	 */
	private double[] calculateMIP(double[] sumOfProducts, double[] sumOfVolumes)
	{

		double[] arr_MIP = new double[this.mainContext.ticksPerDay];
		for (int i = 0; i < this.mainContext.ticksPerDay; i++)
		{
			if (sumOfVolumes[i] != 0)
			{
				arr_MIP[i] = sumOfProducts[i] / sumOfVolumes[i];
			}
		}
		this.mainContext.logger.trace("arr_MIP: " + Arrays.toString(arr_MIP));

		return arr_MIP;
	}

	public static double[] getReversePrice()
	{
		return PowerExchange.arr_reversePrice;
	}

	@ScheduledMethod(start = Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP, interval = 1, shuffle = false, priority = Consts.PX_PRIORITY_FOURTH)
	public void step()
	{

		this.mainContext.logger.trace("--Px: " + TestHelper.getEnvInfoInString(this.mainContext));

		int settlementPeriod = this.mainContext.getSettlementPeriod();

		if (this.mainContext.isMarketFirstDay() && settlementPeriod == 0)
		{ // things to do once at day 0 and sp=0
			// this.list_IPxTrader = mainContext.getListOfRegisteredPxTraders();
			this.list_IPxTrader = this.mainContext.getListOfPxTraders();
		}

		switch (settlementPeriod)
		{

		case 15: // test: to check whether is correct
			this.list_PxPD = this.getImbalDataFromSOMessageBoard();
			// TestHelper.printListOfPxPD(list_PxPD);
			break;
		case 20:
			// check this later
			// list_BSOD = fetchBSOfromBMUs(list_BMU, map_dmuID2ListOfBSOD);
			this.map_dmu2ListOfBSOD = this.fetchBSOfromIPxTraders(this.list_IPxTrader);
			// TestHelper.printMapListOfBSODs(map_dmu2ListOfBSOD);
			this.map_dmu2ListOfBSOD = this.generateAcceptance(this.map_dmu2ListOfBSOD);
			this.sendAcceptanceToEachIPxTrader(this.map_dmu2ListOfBSOD);
			this.calculatePartialMIP(this.map_dmu2ListOfBSOD, this.arr_sumOfProductsTraded, this.arr_sumOfVolumesTraded);
			break;
		case 23:
			this.list_PxPD = this.getImbalDataFromSOMessageBoard();
			break;
		case 28:
			// list_BSOD = fetchBSOfromBMUs(list_BMU, map_dmuID2ListOfBSOD);
			this.map_dmu2ListOfBSOD = this.fetchBSOfromIPxTraders(this.list_IPxTrader);
			this.map_dmu2ListOfBSOD = this.generateAcceptance(this.map_dmu2ListOfBSOD);
			this.sendAcceptanceToEachIPxTrader(this.map_dmu2ListOfBSOD);
			this.calculatePartialMIP(this.map_dmu2ListOfBSOD, this.arr_sumOfProductsTraded, this.arr_sumOfVolumesTraded);

			break;
		case 31:
			this.list_PxPD = this.getImbalDataFromSOMessageBoard();
			break;
		case 36:
			this.map_dmu2ListOfBSOD = this.fetchBSOfromIPxTraders(this.list_IPxTrader);
			this.map_dmu2ListOfBSOD = this.generateAcceptance(this.map_dmu2ListOfBSOD);
			this.sendAcceptanceToEachIPxTrader(this.map_dmu2ListOfBSOD);
			this.calculatePartialMIP(this.map_dmu2ListOfBSOD, this.arr_sumOfProductsTraded, this.arr_sumOfVolumesTraded);

			break;
		case 47:
			/***
			 * this step calculates the market index price at the end of the day
			 * to give the Market index price (a.k.a. the reverse price) for
			 * each settlement period of the following day. This is used as the
			 * reverse price by the settlement company to cash out following the
			 * balancing mechanism
			 */
			PowerExchange.arr_reversePrice = this.calculateMIP(this.arr_sumOfProductsTraded, this.arr_sumOfVolumesTraded);
			this.messageBoard.setMIP(PowerExchange.arr_reversePrice);
			this.mainContext.logger.trace(PowerExchange.arr_reversePrice[settlementPeriod]);

			Arrays.fill(this.arr_sumOfProductsTraded, 0);
			Arrays.fill(this.arr_sumOfVolumesTraded, 0);

			break;
		}
	}

	public PowerExchange(CascadeContext context, MarketMessageBoard mb)
	{
		this.mainContext = context;
		this.messageBoard = mb;
		PowerExchange.arr_reversePrice = new double[context.ticksPerDay];
		this.arr_sumOfProductsTraded = new double[context.ticksPerDay];
		this.arr_sumOfVolumesTraded = new double[context.ticksPerDay];
	}

}
