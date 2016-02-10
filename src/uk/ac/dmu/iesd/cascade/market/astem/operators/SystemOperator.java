package uk.ac.dmu.iesd.cascade.market.astem.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import repast.simphony.engine.schedule.ScheduledMethod;
import uk.ac.dmu.iesd.cascade.agents.aggregators.BOD;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IBMTrader;
import uk.ac.dmu.iesd.cascade.market.ITrader;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.data.ImbalData;
import uk.ac.dmu.iesd.cascade.market.astem.test.TestHelper;
import uk.ac.dmu.iesd.cascade.market.astem.util.ArraysUtils;
import uk.ac.dmu.iesd.cascade.market.astem.util.CollectionUtils;
import uk.ac.dmu.iesd.cascade.market.astem.util.SortComparatorUtils;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * 
 * The <em>SystemOperator</em> class embodies a major actor (agent) in the
 * electricity market. In the UK context, this role is played by the
 * <tt>National Grid</tt>. It uses /interacts with <em>SettlementCompany</em> (
 * {@link SettlementCompany}), which in the UK context, its operations are
 * performed by <tt>Elexon</tt>. It also uses <em>MessageBoard</em> (
 * {@link MarketMessageBoard}) as a 'Reporting Service' tool to broadcast
 * information used by the players in the electricity market.
 * 
 * Another important operator in the electricity market is
 * <em>PowerExchange</em>:
 * 
 * @see uk.ac.dmu.iesd.cascade.market.astem.operators.PowerExchange
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/09
 * 
 */

public class SystemOperator
{

	private CascadeContext mainContext;
	private SettlementCompany settlementCo;
	private MarketMessageBoard messageBoard;
	private double[] arr_NDF; // National Demand Forecast
	private double[] arr_INDGEN; // Indicated Generation
	private double[] arr_IMBAL; // Imbalance
	private double[] arr_oldIMBAL; // old/previous day Imbalance

	private double[] arr_INDMAR; // Indicator Margin across whole system - i.e.
									// amount of potential generation across the
									// system (maxCap - PNs) with the remaining
									// IMBALANCE also subtracted
	private ArrayList<ITrader> list_ITrader;
	private ArrayList<BOD> list_BOD;

	private LinkedHashMap<Integer, double[]> map_dmuID2PN;
	private LinkedHashMap<IBMTrader, ArrayList<BOD>> map_IBMTrader2ListOfBODs;
	private LinkedHashMap<IBMTrader, ArrayList<BOD>> map_IBMTrader2ListOfBOAs; // BOA
																				// are
																				// the
																				// same
																				// as
																				// BODs,
																				// except
																				// processed
																				// for
																				// acceptance
																				// (T
																				// or
																				// F)

	private LinkedHashMap<Integer, ArrayList<ImbalData>> map_imbalType2ImbalData;

	public double[] getPublishedNDF()
	{
		return this.arr_NDF;
	}

	/*
	 * private ArrayList<BMU> getBMUList() { ArrayList<BMU> aListOfBMUs = new
	 * ArrayList<BMU>();
	 * 
	 * Network bmuNet = mainContext.getNetworkOfRegisteredBMUs();
	 * Iterable<RepastEdge> edgeIter = bmuNet.getEdges(); if(Consts.VERBOSE)
	 * this.mainContext.logger.debug("SO has access to "+ bmuNet.size() +
	 * " registered BMUs"); for (RepastEdge edge : edgeIter) { Object obj =
	 * edge.getTarget(); if (obj instanceof BMU) aListOfBMUs.add((BMU) obj);
	 * else System.err.println("SO:: Wrong Class Type: BMU agent is expected");
	 * } return aListOfBMUs; }
	 */

	private LinkedHashMap fetchPNs(ArrayList<ITrader> listOfITraders)
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("------ SO: recievePNs");
		}
		LinkedHashMap mapOfPNs = new LinkedHashMap();
		for (ITrader bmu : listOfITraders)
		{
			mapOfPNs.put(bmu.getID(), bmu.getPN());
		}
		return mapOfPNs;
	}

	private double[] calculateNDF(LinkedHashMap<Integer, double[]> mapOfPNs)
	{

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("calculateNDF()");
		}

		double[][] arr_ij_pn = new double[CollectionUtils.numOfRowsWithNegValInCollection(mapOfPNs.values())][this.mainContext.ticksPerDay];
		int i = 0;

		for (double[] pn : mapOfPNs.values())
		{
			if (ArraysUtils.isContainNegative(pn))
			{
				arr_ij_pn[i] = pn;
				i++;
			}
		}

		return ArraysUtils.sumOfCols2DDoubleArray(arr_ij_pn);
	}

	private double[] calculateINDGEN(LinkedHashMap<Integer, double[]> mapOfPNs)
	{

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("calculateINDGEN()");
		}

		double[][] arr_ij_pn = new double[CollectionUtils.numOfRowsWithPosValInCollection(mapOfPNs.values())][this.mainContext.ticksPerDay];
		int i = 0;

		for (double[] pn : mapOfPNs.values())
		{
			if (!ArraysUtils.isContainNegative(pn))
			{
				arr_ij_pn[i] = pn;
				i++;
			}
		}

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(ArrayUtils.toString(arr_ij_pn));
		}

		return ArraysUtils.sumOfCols2DDoubleArray(arr_ij_pn);
	}

	private double[] calculateIMBAL(double[] ndf, double[] indGen)
	{
		return ArraysUtils.add(ndf, indGen);
	}

	private double[] calculateINDMAR(ArrayList<ITrader> listOfITraders, double[] imbalArray)
	{

		// sum of maxGenCap - INDGEN
		double[] indMargin = new double[imbalArray.length];
		double sumOfMaxGen = 0;
		for (ITrader bmu : listOfITraders)
		{
			sumOfMaxGen += bmu.getMaxGenCap();
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("bmuMaxGenCap: " + bmu.getMaxGenCap());
			}
		}
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("SumOfBmuMaxGenCap: " + sumOfMaxGen);
		}

		for (int i = 0; i < indMargin.length; i++)
		{
			indMargin[i] = sumOfMaxGen - imbalArray[i];
		}

		return indMargin;
	}

	private void broadcastIMBALandINDMARInfo(double[] imbalArray, double[] oldImbalArray, double[] indMarArray)
	{
		// pass INDMAR and IMBAL to all BMUs
		this.messageBoard.setIMBAL(imbalArray);
		this.messageBoard.setINDMAR(indMarArray);
		this.messageBoard.setPreviousDayIMBAL(oldImbalArray);
	}

	private LinkedHashMap<IBMTrader, ArrayList<BOD>> fetchBODs(List<ITrader> listOfITraders)
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("------ SO: fetchBOD");
		}
		// Received BOD are in the form of list
		LinkedHashMap<IBMTrader, ArrayList<BOD>> map_IBMTrader2bod = new LinkedHashMap<IBMTrader, ArrayList<BOD>>();

		for (int i = 0; i < listOfITraders.size(); i++)
		{
			IBMTrader bmu = (IBMTrader) listOfITraders.get(i);
			map_IBMTrader2bod.put(bmu, bmu.getListOfBOD());

		}
		// for (IBMTrader bmu : listOfITraders)
		// map_IBMTrader2bod.put(bmu, bmu.getListOfBOD());

		return map_IBMTrader2bod;
	}

	private LinkedHashMap<IBMTrader, ArrayList<BOD>> generateBOA(LinkedHashMap<IBMTrader, ArrayList<BOD>> mapOfIBMTrader2ListOfBODs,
			int sp, double[] prevDayIMBALArray)
	{

		ArrayList<BOD> listOfBODs = new ArrayList<BOD>();

		Collection<ArrayList<BOD>> valCol = mapOfIBMTrader2ListOfBODs.values();
		for (ArrayList<BOD> arrBOD : valCol)
		{
			listOfBODs.addAll(arrBOD);
		}

		ArrayList<BOD> listOfOffers = new ArrayList<BOD>();
		ArrayList<BOD> listOfBids = new ArrayList<BOD>();
		for (BOD bod : listOfBODs)
		{
			if (bod.getPairID() > 0)
			{
				listOfOffers.add(bod);
			}
			else
			{
				listOfBids.add(bod);
			}
		}

		Collections.sort(listOfOffers, SortComparatorUtils.SO_SUBMITTED_BO_TOPDOWN_ASCENDING_ORDER);

		Collections.sort(listOfBids, SortComparatorUtils.SO_SUBMITTED_BO_TOPDOWN_DESCENDING_ORDER);

		Iterator<BOD> iterOffers = listOfOffers.iterator();

		if (prevDayIMBALArray[sp] < 0)
		{
			double remainingIMBAL = prevDayIMBALArray[sp];

			while (remainingIMBAL < 0 && iterOffers.hasNext())
			{
				BOD bod = iterOffers.next();
				bod.isAccepted = true;
				remainingIMBAL = remainingIMBAL + bod.getLevel();
				if (remainingIMBAL > 0)
				{
					bod.setLevel(bod.getLevel() - remainingIMBAL);
					remainingIMBAL = 0;
				}
			}
		}

		Iterator<BOD> iterBids = listOfBids.iterator();

		if (prevDayIMBALArray[sp] > 0)
		{
			double remainingIMBAL = prevDayIMBALArray[sp];
			while (remainingIMBAL > 0 && iterBids.hasNext())
			{
				BOD bod = iterBids.next();
				bod.isAccepted = true;
				remainingIMBAL = remainingIMBAL + bod.getLevel();
				if (remainingIMBAL < 0)
				{
					bod.setLevel(bod.getLevel() - remainingIMBAL);
					remainingIMBAL = 0;
				}
			}
		}

		return mapOfIBMTrader2ListOfBODs;
	}

	private void sendBOAtoEachIBMTrader(LinkedHashMap<IBMTrader, ArrayList<BOD>> mapOfIBMTrader2ListOfBOAs)
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("SO: sendBOAtoEachBMU() called");
		}

		Set<IBMTrader> bmuSet = mapOfIBMTrader2ListOfBOAs.keySet();

		for (IBMTrader bmu : bmuSet)
		{
			bmu.recieveBOA(mapOfIBMTrader2ListOfBOAs.get(bmu));
		}

	}

	private void sendBOAtoSettlementCo(LinkedHashMap<IBMTrader, ArrayList<BOD>> mapOfIBMTrader2ListOfBOAs)
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("SO: sendBOAtoSettlementCo() called");
		}
		this.settlementCo.recieveBOAs(mapOfIBMTrader2ListOfBOAs); // @TODO: send
																	// a
																	// clone/copy
																	// not the
																	// original
																	// ref
	}

	/*
	 * private void sendBOAtoEachBMU_old(List<BMU> listOfBMUs, HashMap<Integer,
	 * ArrayList<BOD>> mapBMU_ID2ListOfBODs) { if ( *
	 * this.mainContext.logger.isDebugEnabled()) {
	 * this.mainContext.logger.debug("SO: sendBOAtoEachBMU() called"); } if ( *
	 * this.mainContext.logger.isDebugEnabled()) {
	 * this.mainContext.logger.debug("size of listOfBMUs: "+listOfBMUs.size());
if (	 * } this.mainContext.logger.isDebugEnabled()) {
	 * } this.mainContext.logger.debug("size of mapBMU_ID2ListOfBODs: "+
	 * mapBMU_ID2ListOfBODs.size());
}
	 * 
	 * for (BMU bmu : listOfBMUs){ this.mainContext.logger.debug(bmu.getID()); }
	 * 
	 * Set<Integer> keySet = mapBMU_ID2ListOfBODs.keySet();
	 * 
	 * //TestUtils.printListOfBODs(this.map_ListOf_BOD);
	 * 
	 * for (BMU bmu : listOfBMUs){ Object obj =
	 * mapBMU_ID2ListOfBODs.get(bmu.getID()); if (obj != null) {
	 * bmu.recieveBOA((ArrayList<BOD>)obj); } } }
	 */

	private void calculateFPN()
	{
	}

	private void sendFPNtoSettlementCo()
	{
	}

	private void sendBOAtoSettlementCo()
	{
	}

	private void recieveFPN()
	{
	}

	private ArrayList<ImbalData> calculate2hImbalance(double[] imbalArray, double totalImbal)
	{

		ArrayList<ImbalData> list_2hImbal = new ArrayList<ImbalData>();

		for (int i = 0, j = 0; i < imbalArray.length; i++, j++)
		{
			double vol = ArraysUtils.sum(Arrays.copyOfRange(imbalArray, i, i + 1));
			ImbalData imbalData = new ImbalData(vol / 1);
			if (Math.abs(vol) >= totalImbal)
			{
				imbalData.flag = true;
			}

			list_2hImbal.add(imbalData);
		}

		return list_2hImbal;

	}

	private ArrayList<ImbalData> calculate4hImbalance(ArrayList<ImbalData> list_2hImbal, double totalImbal)
	{

		ArrayList<ImbalData> list_4hImbal = new ArrayList<ImbalData>();
		Object[] arr_2hImbal = list_2hImbal.toArray();

		for (int i = 0, j = 0; i < arr_2hImbal.length; i += 4, j++)
		{

			double vol = ((ImbalData) arr_2hImbal[i]).getVolume() + ((ImbalData) arr_2hImbal[i + 1]).getVolume()
					+ ((ImbalData) arr_2hImbal[i + 2]).getVolume() + ((ImbalData) arr_2hImbal[i + 3]).getVolume();
			ImbalData imbalData = new ImbalData(vol / 4);

			if (((ImbalData) arr_2hImbal[i]).flag == false && ((ImbalData) arr_2hImbal[i + 1]).flag == false
					&& ((ImbalData) arr_2hImbal[i + 2]).flag == false && ((ImbalData) arr_2hImbal[i + 3]).flag == false
					&& Math.abs(vol) >= totalImbal)
			{
				imbalData.flag = true;
			}
			list_4hImbal.add(imbalData);
		}
		return list_4hImbal;
	}

	private ArrayList<ImbalData> calculate8hImbalance(ArrayList<ImbalData> list_2hImbal, ArrayList<ImbalData> list_4hImbal,
			double totalImbal)
	{

		ArrayList<ImbalData> list_8hImbal = new ArrayList<ImbalData>();
		Object[] arr_4hImbal = list_4hImbal.toArray();
		Object[] arr_2hImbal = list_2hImbal.toArray();

		ImbalData imbalData;
		for (int i = 0, j = 0; i < arr_4hImbal.length; i += 2, j++)
		{

			double vol = ((ImbalData) arr_4hImbal[i]).getVolume() + ((ImbalData) arr_4hImbal[i + 1]).getVolume();
			imbalData = new ImbalData(vol / 8);

			list_8hImbal.add(imbalData);
		}

		for (int i = 0; i < list_8hImbal.size(); i++)
		{

			if (((ImbalData) arr_4hImbal[2 * i]).flag == false && ((ImbalData) arr_4hImbal[2 * i + 1]).flag == false
					&& ((ImbalData) arr_2hImbal[4 * i]).flag == false && ((ImbalData) arr_2hImbal[4 * i + 1]).flag == false
					&& ((ImbalData) arr_2hImbal[4 * i + 2]).flag == false && ((ImbalData) arr_2hImbal[4 * i + 3]).flag == false
					&& ((ImbalData) arr_2hImbal[4 * i + 4]).flag == false && ((ImbalData) arr_2hImbal[4 * i + 5]).flag == false
					&& ((ImbalData) arr_2hImbal[4 * i + 6]).flag == false && ((ImbalData) arr_2hImbal[4 * i + 7]).flag == false
					&& Math.abs(list_8hImbal.get(i).getVolume()) >= totalImbal)
			{

				list_8hImbal.get(i).flag = true;
			}

		}

		return list_8hImbal;
	}

	public void updateOldIMBAL(double[] currentIMBALArray, double[] oldIMBALArray)
	{ // PN for previous day
		System.arraycopy(currentIMBALArray, 0, oldIMBALArray, 0, currentIMBALArray.length);
	}

	private LinkedHashMap<Integer, ArrayList<ImbalData>> calculatePXImbalance(double[] imbalArray)
	{

		double totalImbal = Math.abs(ArraysUtils.sum(imbalArray)) / ASTEMConsts.PX_IMBAL_FACTOR; // forTest

		ArrayList<ImbalData> list_2hImbal = this.calculate2hImbalance(imbalArray, totalImbal);
		ArrayList<ImbalData> list_4hImbal = this.calculate4hImbalance(list_2hImbal, totalImbal);
		ArrayList<ImbalData> list_8hImbal = this.calculate8hImbalance(list_2hImbal, list_4hImbal, totalImbal);

		LinkedHashMap<Integer, ArrayList<ImbalData>> map_imbalances = new LinkedHashMap<Integer, ArrayList<ImbalData>>();

		map_imbalances.put(ASTEMConsts.PX_PRODUCT_ID_2H, list_2hImbal);
		map_imbalances.put(ASTEMConsts.PX_PRODUCT_ID_4H, list_4hImbal);
		map_imbalances.put(ASTEMConsts.PX_PRODUCT_ID_8H, list_8hImbal);

		return map_imbalances;
	}

	private void broadcastPXImbalance(LinkedHashMap<Integer, ArrayList<ImbalData>> mapOfImbalType2ImbalData)
	{
		this.messageBoard.setPxProductList(mapOfImbalType2ImbalData);
	}

	@ScheduledMethod(start = Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP, interval = 1, shuffle = false, priority = Consts.SO_PRIORITY_SECOND)
	public void step()
	{

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("--SO: " + TestHelper.getEnvInfoInString(this.mainContext));
		}

		// TestUtils.printPNs(map_PN);

		int settlementPeriod = this.mainContext.getSettlementPeriod();

		if (this.mainContext.isMarketFirstDay() && settlementPeriod == 0)
		{ // things to do once at day 0 and sp=0
			// this.list_BMU = getBMUList();
			// this.list_IBMTrader = mainContext.getListOfRegisteredBMTraders();
			this.list_ITrader = this.mainContext.getListOfTraders();
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("list_ITrader.size: " + this.list_ITrader.size());
			}
		}

		// String[] labels={"NDF:", "INDGEN:", "IMBAL:", "INDMAR:"};

		switch (settlementPeriod)
		{

		case 14:
			this.map_dmuID2PN = this.fetchPNs(this.list_ITrader);
			// TestHelper.printMapOfPNs(map_dmuID2PN);
			this.arr_NDF = this.calculateNDF(this.map_dmuID2PN);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("NDF" + Arrays.toString(this.arr_NDF));
			}
			this.arr_INDGEN = this.calculateINDGEN(this.map_dmuID2PN); // indicated
																		// generation
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("indGEN" + Arrays.toString(this.arr_INDGEN));
			}

			this.arr_IMBAL = this.calculateIMBAL(this.arr_NDF, this.arr_INDGEN); // imbalance
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("IMBAL" + Arrays.toString(this.arr_IMBAL));
			}

			this.arr_INDMAR = this.calculateINDMAR(this.list_ITrader, this.arr_IMBAL); // idicator
																						// margine
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("IndMar" + Arrays.toString(this.arr_INDMAR));
			}

			// TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

			this.broadcastIMBALandINDMARInfo(this.arr_IMBAL, this.arr_oldIMBAL, this.arr_INDMAR);
			this.map_imbalType2ImbalData = this.calculatePXImbalance(this.arr_IMBAL);
			this.broadcastPXImbalance(this.map_imbalType2ImbalData);

			break;

		case 22:
			this.map_dmuID2PN = this.fetchPNs(this.list_ITrader);
			this.arr_NDF = this.calculateNDF(this.map_dmuID2PN);
			this.arr_INDGEN = this.calculateINDGEN(this.map_dmuID2PN);
			this.arr_IMBAL = this.calculateIMBAL(this.arr_NDF, this.arr_INDGEN);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("IMBAL" + Arrays.toString(this.arr_IMBAL));
			}

			this.arr_INDMAR = this.calculateINDMAR(this.list_ITrader, this.arr_IMBAL);
			// TestHelper.writeOutput("SO_sp", true, labels, arr_NDF,
			// arr_INDGEN, arr_IMBAL, arr_INDMAR);
			// TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

			this.broadcastIMBALandINDMARInfo(this.arr_IMBAL, this.arr_oldIMBAL, this.arr_INDMAR);
			this.map_imbalType2ImbalData = this.calculatePXImbalance(this.arr_IMBAL);
			this.broadcastPXImbalance(this.map_imbalType2ImbalData);

			break;

		case 30:
			this.map_dmuID2PN = this.fetchPNs(this.list_ITrader);
			this.arr_NDF = this.calculateNDF(this.map_dmuID2PN);
			this.arr_INDGEN = this.calculateINDGEN(this.map_dmuID2PN);
			this.arr_IMBAL = this.calculateIMBAL(this.arr_NDF, this.arr_INDGEN);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("IMBAL" + Arrays.toString(this.arr_IMBAL));
			}

			this.arr_INDMAR = this.calculateINDMAR(this.list_ITrader, this.arr_IMBAL);
			// TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

			this.broadcastIMBALandINDMARInfo(this.arr_IMBAL, this.arr_oldIMBAL, this.arr_INDMAR);
			this.map_imbalType2ImbalData = this.calculatePXImbalance(this.arr_IMBAL);
			this.broadcastPXImbalance(this.map_imbalType2ImbalData);

			break;

		case 38:
			this.map_dmuID2PN = this.fetchPNs(this.list_ITrader);
			this.arr_NDF = this.calculateNDF(this.map_dmuID2PN);
			this.arr_INDGEN = this.calculateINDGEN(this.map_dmuID2PN);
			this.arr_IMBAL = this.calculateIMBAL(this.arr_NDF, this.arr_INDGEN);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("IMBAL" + Arrays.toString(this.arr_IMBAL));
			}

			this.arr_INDMAR = this.calculateINDMAR(this.list_ITrader, this.arr_IMBAL);
			// TestHelper.writeOutput("SO_sp", true, arr_IMBAL);

			this.broadcastIMBALandINDMARInfo(this.arr_IMBAL, this.arr_oldIMBAL, this.arr_INDMAR);
			break;

		case 47:
			this.updateOldIMBAL(this.arr_IMBAL, this.arr_oldIMBAL);
			this.broadcastIMBALandINDMARInfo(this.arr_IMBAL, this.arr_oldIMBAL, this.arr_INDMAR);

			break;

		}// end of switch

		if (!this.mainContext.isMarketFirstDay())
		{

			this.map_IBMTrader2ListOfBODs = this.fetchBODs(this.list_ITrader);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(" ----printBOD before generatingBOAs ");
			}
			// TestHelper.printMapListOfBODs(map_IBMTrader2ListOfBODs);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("arr_oldIMBAL" + Arrays.toString(this.arr_oldIMBAL));
			}
			this.map_IBMTrader2ListOfBOAs = this.generateBOA(this.map_IBMTrader2ListOfBODs, settlementPeriod, this.arr_oldIMBAL);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(" ***printBOD AFTER generatingBOAs ");
			}
			// TestHelper.printMapListOfBODs(map_IBMTrader2ListOfBODs);
			this.sendBOAtoEachIBMTrader(this.map_IBMTrader2ListOfBODs);
			this.sendBOAtoSettlementCo(this.map_IBMTrader2ListOfBODs);

			// sendBOAtoEachBMU(list_BMU, map_dmuID2ListOf_BODs);
		}

		if (settlementPeriod == 47)
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("DEMAND: " + Arrays.toString(this.arr_NDF));
			}
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("GENERATION: " + Arrays.toString(this.arr_INDGEN));
			}
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("IMBALANCE: " + Arrays.toString(this.arr_IMBAL));
			}
			// TestHelper.printMapListOfBODs(map_IBMTrader2ListOfBOAs);
		}

	}

	public SystemOperator(CascadeContext context, SettlementCompany sc, MarketMessageBoard mb)
	{
		this.mainContext = context;
		this.settlementCo = sc;
		// this.messageBoard = new MessageBoard();
		this.messageBoard = mb;
		this.arr_oldIMBAL = new double[context.ticksPerDay];

	}

}
