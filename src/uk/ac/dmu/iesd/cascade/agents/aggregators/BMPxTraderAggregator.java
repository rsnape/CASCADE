package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.util.ArrayList;
import java.util.Arrays;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.random.RandomRegistry;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IBMTrader;
import uk.ac.dmu.iesd.cascade.market.IPxTrader;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.market.astem.test.TestHelper;
import uk.ac.dmu.iesd.cascade.market.astem.util.ArraysUtils;
import uk.ac.dmu.iesd.cascade.market.data.BSOD;
import uk.ac.dmu.iesd.cascade.market.data.PxPD;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;

/**
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/05/16
 */

public abstract class BMPxTraderAggregator extends AggregatorAgent implements IBMTrader, IPxTrader
{

	protected MarketMessageBoard messageBoard;

	private ArrayList<BOD> list_BOD;
	private ArrayList<BOD> list_BOA;
	private ArrayList<BSOD> list_BSOD;

	private ArrayList<BSOD> list_BSOA;

	protected double[] arr_baselineProfile; // contains realistic load based on
											// real data from which PN are
											// calculated
	private double[] arr_Margin; // Margin for each settlement period - required
									// by the BM
									// difference between maxgen and oldPN (for
									// generators) or
									// mindem and oldPN (for Suppliers)

	private ArrayList<PxPD> list_PX_products; // make sure if needed

	int settlementPeriod;

	@Override
	public int getID()
	{
		return this.id;
	}

	@Override
	public BMU_CATEGORY getCategory()
	{
		return this.category;
	}

	@Override
	public String getCategoryAsString()
	{
		return super.getCategoryAsString();
	}

	@Override
	public BMU_TYPE getType()
	{
		return this.type;
	}

	@Override
	public double getMaxGenCap()
	{
		return this.maxGen;
	}

	@Override
	public double getMinDemCap()
	{
		return this.minDem;
	}

	@Override
	public double[] getPN()
	{
		return this.arr_PN;
	}

	@Override
	public double[] getPreviousDayPN()
	{
		return this.arr_oldPN;
	}

	public double[] updateMarginForBM(double[] oldPN)
	{
		double capacity = 0;
		if (this.category == BMU_CATEGORY.GEN_T)
		{
			capacity = this.maxGen;
		}
		else if (this.category == BMU_CATEGORY.DEM_S)
		{
			capacity = this.minDem;
		}

		for (int i = 0; i < this.arr_Margin.length; i++)
		{
			this.arr_Margin[i] = capacity - oldPN[i];
		}
		return this.arr_Margin;
	}

	private double[] updatePN(ArrayList<BSOD> listOfAcceptedBSOD, double[] arr_PN)
	{

		for (BSOD bsod : listOfAcceptedBSOD)
		{
			if (bsod.accepted)
			{
				for (int i = bsod.getStartSPIndex(); i < (bsod.getStartSPIndex() + bsod.getProductID()); i++)
				{
					arr_PN[i] = arr_PN[i] + bsod.getVolume();
				}
			}
		}
		// Note this return is in some ways redundant as this is working on the
		// array directly JRS
		return arr_PN;
	}

	private void generateMeteredVolumes()
	{
	}

	// ------

	private double[] calculateStochasticPN(double[] baselineProfile, double mFactor)
	{
		this.mainContext.logger.trace("calculateStochasticPN: " + mFactor);

		double[] randomPN = new double[this.ticksPerDay];
		double avg = ArraysUtils.avg(baselineProfile);
		double sd = avg * mFactor;

		for (int i = 0; i < randomPN.length; i++)
		{
			Normal normalDist = RandomHelper.createNormal(baselineProfile[i], sd);
			randomPN[i] = normalDist.nextDouble();
		}
		this.mainContext.logger.trace("randomPN: " + Arrays.toString(randomPN));

		return randomPN;
	}

	private double[] initializePN(double[] baselineProfile)
	{
		this.mainContext.logger.trace("initializePN (baselineProf): " + Arrays.toString(baselineProfile));
		this.mainContext.logger.trace(" initializePN " + this.id + " -- " + TestHelper.getEnvInfoInString(this.mainContext));

		double[] initializedArray = baselineProfile;

		/******
		 * The change here, from initialising the PN from the baseline to
		 * initialising the PN stochastically makes the difference between
		 * stable market and collapse JRS.
		 */
		switch (this.type)
		{
		case GEN_COAL:
			// initializedArray=
			// baselineProfile;//calculateStochasticPN(baselineProfile, 0.01);
			initializedArray = this.calculateStochasticPN(baselineProfile, 0.01);
			this.mainContext.logger.trace("initializedArray GENCOAL: " + Arrays.toString(baselineProfile));
			break;
		case GEN_CCGT:
			// initializedArray= baselineProfile;
			// //calculateStochasticPN(baselineProfile, 0.01);
			initializedArray = this.calculateStochasticPN(baselineProfile, 0.01);
			break;
		case GEN_WIND:
			// initializedArray = baselineProfile;
			// //calculateStochasticPN(baselineProfile, 0.2);
			initializedArray = this.calculateStochasticPN(baselineProfile, 0.2);
			break;
		case DEM_LARGE:
			// initializedArray = baselineProfile;
			// //calculateStochasticPN(baselineProfile, 0.02);
			initializedArray = this.calculateStochasticPN(baselineProfile, 0.02);
			break;
		case DEM_SMALL:
			// initializedArray =
			// baselineProfile;//calculateStochasticPN(baselineProfile, 0.02);
			initializedArray = this.calculateStochasticPN(baselineProfile, 0);
			break;
		}

		return initializedArray;

	}

	@Override
	public ArrayList<BSOD> getListOfBSOD()
	{
		return this.list_BSOD;
	}

	private ArrayList<BSOD> generateBSOforPX(ArrayList<PxPD> listOfPxProduct, double[] arrayPN)
	{

		ArrayList<BSOD> listOfBSOD = new ArrayList<BSOD>();

		for (PxPD pxPD : listOfPxProduct)
		{

			int spIndex = pxPD.getStartSPIndex();
			int productID = pxPD.getProductID();
			double vol = 0;
			double price = 0;

			double pn_avg = ArraysUtils.avg(Arrays.copyOfRange(arrayPN, spIndex, spIndex + productID));
			// ----------------------------------------------------------------------------------------------------------
			if (this.type == Consts.BMU_TYPE.GEN_COAL)
			{
				double margin = this.maxGen - pn_avg;
				if (pxPD.getVolume() < 0)
				{ // IMBAL < 0
					if (Math.abs(margin) >= Math.abs(pxPD.getVolume()))
					{
						vol = (pxPD.getVolume() * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(margin) > 0)
					{
						vol = (margin * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(20, 30);
				}
				else if (pxPD.getVolume() > 0)
				{ // IMBAL > 0
					if (Math.abs(margin) >= Math.abs(pxPD.getVolume()))
					{
						vol = -(pxPD.getVolume() * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(pn_avg) > 0)
					{
						vol = -(pn_avg * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(30, 50);
				}
			}

			else if (this.type == Consts.BMU_TYPE.GEN_CCGT)
			{
				double margin = this.maxGen - pn_avg;

				if (pxPD.getVolume() < 0)
				{ // IMBAL < 0
					if (Math.abs(margin) >= Math.abs(pxPD.getVolume()))
					{
						vol = (pxPD.getVolume() * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(margin) > 0)
					{
						vol = (margin * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(20, 30);
				}
				else if (pxPD.getVolume() > 0)
				{ // IMBAL > 0
					if (Math.abs(margin) >= Math.abs(pxPD.getVolume()))
					{
						vol = -(pxPD.getVolume() * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(pn_avg) > 0)
					{
						vol = -(pn_avg * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(30, 50);
				}
			}

			else if (this.type == Consts.BMU_TYPE.GEN_WIND)
			{
				double margin = this.maxGen - pn_avg;
				if (pxPD.getVolume() < 0)
				{ // IMBAL < 0
					if (Math.abs(margin) >= Math.abs(pxPD.getVolume()))
					{
						vol = (pxPD.getVolume() * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(margin) > 0)
					{
						vol = (margin * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(20, 30);
				}
				else if (pxPD.getVolume() > 0)
				{ // IMBAL > 0
					if (Math.abs(margin) >= Math.abs(pxPD.getVolume()))
					{
						vol = -(pxPD.getVolume() * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(pn_avg) > 0)
					{
						vol = -(pn_avg * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(30, 50);
				}
			}

			else if (this.type == Consts.BMU_TYPE.DEM_LARGE)
			{
				double margin = this.minDem - pn_avg;
				double marginMaxDem = this.maxDem - pn_avg;

				if (pxPD.getVolume() < 0)
				{ // IMBAL < 0
					if (Math.abs(margin) >= Math.abs(pxPD.getVolume()))
					{
						vol = (pxPD.getVolume() * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(margin) > 0)
					{
						vol = (margin * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(50, 70);
				}
				else if (pxPD.getVolume() > 0)
				{ // IMBAL > 0
					if (Math.abs(marginMaxDem) >= Math.abs(pxPD.getVolume()))
					{
						vol = -(pxPD.getVolume() * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(marginMaxDem) > 0)
					{
						vol = -(Math.abs(marginMaxDem) * ASTEMConsts.PX_IMBAL_MULTIFACTOR) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(30, 50);
				}

			}

			else if (this.type == Consts.BMU_TYPE.DEM_SMALL)
			{
				double margin = this.minDem - pn_avg;
				double marginMaxDem = this.maxDem - pn_avg;

				if (pxPD.getVolume() < 0)
				{ // IMBAL < 0
					if (Math.abs(margin) >= Math.abs(pxPD.getVolume()))
					{
						vol = (pxPD.getVolume() * 0.65) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(margin) > 0)
					{
						vol = (margin * 0.65) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(50, 70);
				}
				else if (pxPD.getVolume() > 0)
				{ // IMBAL > 0
					if (Math.abs(marginMaxDem) >= Math.abs(pxPD.getVolume()))
					{
						vol = -(pxPD.getVolume() * 0.65) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					else if (Math.abs(marginMaxDem) > 0)
					{
						vol = -(Math.abs(marginMaxDem) * 0.65) / RandomHelper.nextDoubleFromTo(2, 5);
					}
					price = RandomHelper.nextDoubleFromTo(30, 50);
				}

			}

			listOfBSOD.add(new BSOD(this.id, vol, price, productID, spIndex));
		}
		return listOfBSOD;
	}

	@Override
	public void recieveBOA(ArrayList<BOD> listOfBOD)
	{
		this.mainContext.logger.trace("BMU (" + this.getAgentName() + "): recieveBOA() called");
		// should they recieve or go to fetch?
		// if (!list_BOA.isEmpty())
		// list_BOA.clear();
		this.list_BOA = new ArrayList<BOD>();
		for (BOD bod : listOfBOD)
		{
			this.list_BOA.add(bod);
		}
		this.mainContext.logger.trace("BOA after they are recieved:");
		// TestUtils.printBODs(list_BOA);
	}

	/*
	 * public void recieveBSOD( ArrayList<BSOD> listOfBSOD){
	 * this.mainContext.logger.trace("BMU ("+
	 * this.getName()+"): recieveBSOD() called");
	 * 
	 * list_BSOD = new ArrayList<BSOD>(); for (BSOD bsod : listOfBSOD){
	 * list_BSOD.add(bsod); } }
	 */

	@Override
	public void recieveBSOA(ArrayList<BSOD> listOfBSOD)
	{
		this.mainContext.logger.trace("SupplierCo (" + this.getAgentName() + "): recieveBSOD() called");

		this.list_BSOD = new ArrayList<BSOD>();
		for (BSOD bsod : listOfBSOD)
		{
			this.list_BSOD.add(bsod);
		}
	}

	public void recieveBST()
	{
	}

	private double getMarginPC(int pairID)
	{
		double marginPC = 0;

		if (this.category == BMU_CATEGORY.GEN_T)
		{

			this.mainContext.logger.trace("getMarginPC:: is T type");
			switch (pairID)
			{
			case 1:
				marginPC = 0.05;
				break;
			case 2:
				marginPC = 0.1;
				break;
			case 3:
				marginPC = 0.25;
				break;
			case 4:
				marginPC = 0.5;
				break;
			case 5:
				marginPC = 1.0;
				break;

			case -1:
				marginPC = -0.05;
				break;
			case -2:
				marginPC = -0.1;
				break;
			case -3:
				marginPC = -0.25;
				break;
			case -4:
				marginPC = -0.5;
				break;
			case -5:
				marginPC = -1.0;
				break;
			}

		}
		else if (this.category == BMU_CATEGORY.DEM_S)
		{

			this.mainContext.logger.trace("getMarginPC:: is S type");

			switch (pairID)
			{
			case 1:
				marginPC = 0.2;
				break;

			case -1:
				marginPC = -0.2;
				break;
			}
		}

		this.mainContext.logger.trace("getMarginPC:: " + this.id + this.getCategoryAsString() + this.type + ", pairID=" + pairID
				+ ", marginPC=" + marginPC);

		return marginPC;
	}

	public double[] createAndIntializeArray(double from, double step)
	{
		double[] doubleArray = new double[ASTEMConsts.BMU_BO_NUM_OF_CHOICE];
		for (int i = 0; i < ASTEMConsts.BMU_BO_NUM_OF_CHOICE; i++)
		{
			doubleArray[i] = from + (step * i);
		}
		return doubleArray;
	}

	private ArrayList<BOD> generateBOD4WindGen(int sp, double[] marginArray)
	{
		ArrayList<BOD> listOfBOD = new ArrayList<BOD>();

		int pairID1 = 1;

		double level1 = marginArray[sp] * this.getMarginPC(pairID1);
		double[] arr_BO1 = this.createAndIntializeArray(50, 1);
		double[] arr_propensities1 = this.createAndIntializeArray(55, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID1, level1, arr_BO1, arr_propensities1));

		int pairID2 = 2;
		double level2 = marginArray[sp] * this.getMarginPC(pairID2);
		double[] arr_BO2 = this.createAndIntializeArray(60, 1);
		double[] arr_propensities2 = this.createAndIntializeArray(65, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID2, level2, arr_BO2, arr_propensities2));

		int pairID1m = -1;
		double level1m = marginArray[sp] * this.getMarginPC(pairID1m);
		double[] arr_BO1m = this.createAndIntializeArray(40, 1);
		double[] arr_propensities1m = this.createAndIntializeArray(45, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID1m, level1m, arr_BO1m, arr_propensities1m));

		int pairID2m = -2;
		double level2m = marginArray[sp] * this.getMarginPC(pairID2m);
		double[] arr_BO2m = this.createAndIntializeArray(30, 1);
		double[] arr_propensities2m = this.createAndIntializeArray(35, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID2m, level2m, arr_BO2m, arr_propensities2m));

		int pairID3m = -3;
		double level3m = marginArray[sp] * this.getMarginPC(pairID3m);
		double[] arr_BO3m = this.createAndIntializeArray(20, 1);
		double[] arr_propensities3m = this.createAndIntializeArray(25, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID3m, level3m, arr_BO3m, arr_propensities3m));

		int pairID4m = -4;
		double level4m = marginArray[sp] * this.getMarginPC(pairID4m);
		double[] arr_BO4m = this.createAndIntializeArray(10, 1);
		double[] arr_propensities4m = this.createAndIntializeArray(15, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID4m, level4m, arr_BO4m, arr_propensities4m));

		int pairID5m = -5;
		double level5m = marginArray[sp] * this.getMarginPC(pairID5m);
		double[] arr_BO5m = this.createAndIntializeArray(0, 1);
		double[] arr_propensities5m = this.createAndIntializeArray(5, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID5m, level5m, arr_BO5m, arr_propensities5m));

		return listOfBOD;
	}

	private ArrayList<BOD> generateBOD4Gen(int sp, double[] marginArray)
	{
		ArrayList<BOD> listOfBOD = new ArrayList<BOD>();

		int pairID1 = 1;

		double level1 = marginArray[sp] * this.getMarginPC(pairID1);
		double[] arr_BO1 = this.createAndIntializeArray(40, 1);
		double[] arr_propensities1 = this.createAndIntializeArray(45, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID1, level1, arr_BO1, arr_propensities1));

		int pairID2 = 2;
		double level2 = marginArray[sp] * this.getMarginPC(pairID2);
		double[] arr_BO2 = this.createAndIntializeArray(50, 1);
		double[] arr_propensities2 = this.createAndIntializeArray(55, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID2, level2, arr_BO2, arr_propensities2));

		int pairID3 = 3;
		double level3 = marginArray[sp] * this.getMarginPC(pairID3);
		double[] arr_BO3 = this.createAndIntializeArray(60, 1);
		double[] arr_propensities3 = this.createAndIntializeArray(65, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID3, level3, arr_BO3, arr_propensities3));

		int pairID4 = 4;
		double level4 = marginArray[sp] * this.getMarginPC(pairID4);
		double[] arr_BO4 = this.createAndIntializeArray(70, 1);
		double[] arr_propensities4 = this.createAndIntializeArray(75, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID4, level4, arr_BO4, arr_propensities4));

		int pairID5 = 5;
		double level5 = marginArray[sp] * this.getMarginPC(pairID5);
		double[] arr_BO5 = this.createAndIntializeArray(80, 1);
		double[] arr_propensities5 = this.createAndIntializeArray(85, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID5, level5, arr_BO5, arr_propensities5));

		int pairID1m = -1;
		double level1m = marginArray[sp] * this.getMarginPC(pairID1m);
		double[] arr_BO1m = this.createAndIntializeArray(30, 1);
		double[] arr_propensities1m = this.createAndIntializeArray(35, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID1m, level1m, arr_BO1m, arr_propensities1m));

		int pairID2m = -2;
		double level2m = marginArray[sp] * this.getMarginPC(pairID2m);
		double[] arr_BO2m = this.createAndIntializeArray(20, 1);
		double[] arr_propensities2m = this.createAndIntializeArray(25, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID2m, level2m, arr_BO2m, arr_propensities2m));

		int pairID3m = -3;
		double level3m = marginArray[sp] * this.getMarginPC(pairID3m);
		double[] arr_BO3m = this.createAndIntializeArray(10, 1);
		double[] arr_propensities3m = this.createAndIntializeArray(15, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID3m, level3m, arr_BO3m, arr_propensities3m));

		int pairID4m = -4;
		double level4m = marginArray[sp] * this.getMarginPC(pairID4m);
		double[] arr_BO4m = this.createAndIntializeArray(0, 1);
		double[] arr_propensities4m = this.createAndIntializeArray(5, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID4m, level4m, arr_BO4m, arr_propensities4m));

		int pairID5m = -5;
		double level5m = marginArray[sp] * this.getMarginPC(pairID5m);
		double[] arr_BO5m = this.createAndIntializeArray(-10, 1);
		double[] arr_propensities5m = this.createAndIntializeArray(-5, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID5m, level5m, arr_BO5m, arr_propensities5m));

		return listOfBOD;
	}

	private ArrayList<BOD> generateBOD4DemLarge(int sp, double[] marginArray)
	{

		ArrayList<BOD> listOfBOD = new ArrayList<BOD>();

		int pairID1 = 1;
		double level1 = marginArray[sp] * this.getMarginPC(pairID1);
		double[] arr_BO1 = this.createAndIntializeArray(80, 4); // prices
		double[] arr_propensities1 = this.createAndIntializeArray(100, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID1, level1, arr_BO1, arr_propensities1));

		int pairID1m = -1;
		double level1m = marginArray[sp] * this.getMarginPC(pairID1m);
		double[] arr_BO1m = this.createAndIntializeArray(0, 2);
		;
		double[] arr_propensities1m = this.createAndIntializeArray(10, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID1m, level1m, arr_BO1m, arr_propensities1m));

		return listOfBOD;
	}

	private ArrayList<BOD> generateBOD4DemSmall(int sp, double[] marginArray)
	{
		ArrayList<BOD> listOfBOD = new ArrayList<BOD>();

		int pairID1 = 1;
		double level1 = marginArray[sp] * this.getMarginPC(pairID1);
		double[] arr_BO1 = this.createAndIntializeArray(50, 4); // prices
		double[] arr_propensities1 = this.createAndIntializeArray(70, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID1, level1, arr_BO1, arr_propensities1));

		int pairID1m = -1;
		double level1m = marginArray[sp] * this.getMarginPC(pairID1m);
		double[] arr_BO1m = this.createAndIntializeArray(10, 2);
		double[] arr_propensities1m = this.createAndIntializeArray(20, 0);
		listOfBOD.add(new BOD(this.id, sp, pairID1m, level1m, arr_BO1m, arr_propensities1m));

		return listOfBOD;
	}

	private double getExperiment()
	{

		double experiment = 0;

		switch (this.type)
		{
		case GEN_COAL:
			experiment = ASTEMConsts.EXP_COAL;
			break;
		case GEN_CCGT:
			experiment = ASTEMConsts.EXP_CCGT;
			break;
		case GEN_WIND:
			experiment = ASTEMConsts.EXP_WIND;
			break;
		case DEM_LARGE:
			experiment = ASTEMConsts.EXP_LARGEDEM;
			break;
		case DEM_SMALL:
			experiment = ASTEMConsts.EXP_SMALLDEM;
			break;
		}

		return experiment;
	}

	private double getRegency()
	{

		double regency = 0;

		switch (this.type)
		{
		case GEN_COAL:
			regency = ASTEMConsts.REG_COAL;
			break;
		case GEN_CCGT:
			regency = ASTEMConsts.REG_CCGT;
			break;
		case GEN_WIND:
			regency = ASTEMConsts.REG_WIND;
			break;
		case DEM_LARGE:
			regency = ASTEMConsts.REG_LARGEDEM;
			break;
		case DEM_SMALL:
			regency = ASTEMConsts.REG_SMALLDEM;
			break;
		}

		return regency;
	}

	private double updateExperience(int currAction, int prevAction, double reward)
	{
		double updatedE = 0;
		if (currAction == prevAction)
		{
			updatedE = reward * (1 - this.getExperiment());
		}
		else
		{
			updatedE = (reward * this.getExperiment()) / (ASTEMConsts.BMU_BO_NUM_OF_CHOICE - 1);
		}

		this.mainContext.logger.trace("updatedE=" + updatedE);

		return updatedE;
	}

	private double[] learnAndUpdatePropensity(int acceptedIndex, double accBO, double[] propensityArray)
	{
		for (int i = 0; i < propensityArray.length; i++)
		{
			propensityArray[i] = (1 - this.getRegency()) * propensityArray[i] + this.updateExperience(i, acceptedIndex, Math.abs(accBO));
		}
		this.mainContext.logger.trace("propensityArray=" + Arrays.toString(propensityArray));

		return propensityArray;
	}

	private double[] updateProbabilities(double[] propensityArray, double[] probabilityArray)
	{

		double sumOfPropensities = ArraysUtils.sum(propensityArray);

		for (int i = 0; i < propensityArray.length; i++)
		{
			probabilityArray[i] = propensityArray[i] / sumOfPropensities;
		}
		return probabilityArray;
	}

	private ArrayList<BOD> updateBOD(ArrayList<BOD> listOfBOA, int sp, double[] marginArray)
	{

		for (BOD boa : listOfBOA)
		{
			boa.setLevel(marginArray[sp] * this.getMarginPC(boa.getPairID()));
			if (boa.isAccepted)
			{
				int indexOfSubmittedBO = ArraysUtils.indexOfGivenVal(boa.getBOArray(), boa.submittedBO);
				boa.setPropensityArray(this.learnAndUpdatePropensity(indexOfSubmittedBO, boa.submittedBO, boa.getPropensityArray()));
				boa.setProbabilityArray(this.updateProbabilities(boa.getPropensityArray(), boa.getProbabilityArray()));

				EmpiricalWalker randDist = RandomHelper.createEmpiricalWalker(boa.getProbabilityArray(), Empirical.NO_INTERPOLATION);
				// EmpiricalWalker randWalkDist= new
				// cern.jet.random.EmpiricalWalker(boa.getProbabilityArray(),
				// Empirical.NO_INTERPOLATION, mainContext.cRandomEng);
				// EmpiricalWalker randWalkDist= new
				// cern.jet.random.EmpiricalWalker(boa.getProbabilityArray(),
				// Empirical.NO_INTERPOLATION, (RandomEngine)
				// RandomHelper.getGenerator(DefaultRandomRegistry.DEFAULT_GENERATOR).clone());

				this.mainContext.logger.trace("Regis. Gen: "
						+ RandomHelper.getDefaultRegistry().getGenerator(RandomRegistry.DEFAULT_GENERATOR).toString());
				this.mainContext.logger.trace("RH. Gen: " + RandomHelper.getGenerator(RandomRegistry.DEFAULT_GENERATOR).clone());

				int ind = randDist.nextInt(); // check the value return by this.
				this.mainContext.logger.trace("EmpWalker output index: " + ind);
				double[] boArray = boa.getBOArray();
				boa.setSubmittedBO(boArray[ind]);
				boa.isAccepted = false;
			}
		}

		return listOfBOA;
	}

	private ArrayList<BOD> initializeBOD(int sp, double[] marginArray)
	{

		this.mainContext.logger.trace("BOA after they are recieved:");
		// TestUtils.printBODs(list_BOA);
		ArrayList<BOD> listOfBOD = null;
		switch (this.type)
		{
		case GEN_COAL:
			listOfBOD = this.generateBOD4Gen(sp, marginArray);
			this.mainContext.logger.trace("marginArray:Coal: " + Arrays.toString(marginArray));
			break;
		case GEN_CCGT:
			listOfBOD = this.generateBOD4Gen(sp, marginArray);
			this.mainContext.logger.trace("marginArray:CCGT: " + Arrays.toString(marginArray));
			break;
		case GEN_WIND:
			listOfBOD = this.generateBOD4WindGen(sp, marginArray);
			this.mainContext.logger.trace("marginArray:Wind: " + Arrays.toString(marginArray));
			break;
		case DEM_LARGE:
			listOfBOD = this.generateBOD4DemLarge(sp, marginArray);
			this.mainContext.logger.trace("marginArray:DEM_LARG: " + Arrays.toString(marginArray));
			break;
		case DEM_SMALL:
			listOfBOD = this.generateBOD4DemSmall(sp, marginArray);
			this.mainContext.logger.trace("marginArray:DEM_SMALL: " + Arrays.toString(marginArray));
			break;
		}

		return listOfBOD;
	}

	private ArrayList<PxPD> getPxProductFromSOMessageBoard()
	{

		return this.messageBoard.getPxProductList();
	}

	@Override
	public ArrayList<BOD> getListOfBOD()
	{
		return this.list_BOD;
	}

	public double[] updateOldPN(double[] currentPNArray, double[] oldPNArray)
	{ // PN for previous day
		System.arraycopy(currentPNArray, 0, oldPNArray, 0, currentPNArray.length);
		return oldPNArray;
	}

	@Override
	@ScheduledMethod(start = Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP, interval = 1, shuffle = true, priority = Consts.AGGREGATOR_INIT_MARKET_STEP_PRIORITY_FIRST)
	public void marketPreStep()
	{

		this.mainContext.logger.trace(" initializeMarketStep " + this.id + " -- " + TestHelper.getEnvInfoInString(this.mainContext));

		this.settlementPeriod = this.mainContext.getSettlementPeriod();

		switch (this.settlementPeriod)
		{

		case 13:
			// this.arr_PN = this.arr_day_B;
			this.arr_PN = this.initializePN(this.arr_baselineProfile);
			// this.arr_PN = initializePN_temp(arr_baselineProfile);
			this.mainContext.logger.trace("arr_PN: " + Arrays.toString(this.arr_PN));

			break;
		}
	}

	@Override
	@ScheduledMethod(start = Consts.AGGREGATOR_PROFILE_BUILDING_SP + Consts.AGGREGATOR_TRAINING_SP, interval = 1, shuffle = true, priority = Consts.AGGREGATOR_MARKET_STEP_PRIORITY_FIRST)
	public void marketStep()
	{

		this.mainContext.logger.trace("-marketStep (-------------------------");
		this.mainContext.logger.trace("--marketStep (BMPxTraderAgg): " + TestHelper.getEnvInfoInString(this.mainContext));

		// printBMUInfo();

		this.settlementPeriod = this.mainContext.getSettlementPeriod();

		switch (this.settlementPeriod)
		{

		case 19:
			this.mainContext.logger.trace("BMU: gotPxProd");
			// TestUtils.printListOfPxPD(list_PX_products);
			this.list_PX_products = this.getPxProductFromSOMessageBoard();
			this.list_BSOD = this.generateBSOforPX(this.list_PX_products, this.arr_PN);
			this.mainContext.logger.trace("Printing BSOD list");
			// TestHelper.printListOfBSOD(list_BSOD);

			break;
		case 21:
			this.mainContext.logger.trace("PN (BEFORE UPDATE): " + Arrays.toString(this.arr_PN));
			this.arr_PN = this.updatePN(this.list_BSOD, this.arr_PN);
			this.mainContext.logger.trace("PN (AFTER UPDATE): " + Arrays.toString(this.arr_PN));

			break;
		case 27:
			this.list_PX_products = this.getPxProductFromSOMessageBoard();
			this.list_BSOD = this.generateBSOforPX(this.list_PX_products, this.arr_PN);
			break;
		case 29:
			this.arr_PN = this.updatePN(this.list_BSOD, this.arr_PN);
			break;
		case 35:
			this.list_PX_products = this.getPxProductFromSOMessageBoard();
			this.list_BSOD = this.generateBSOforPX(this.list_PX_products, this.arr_PN);
			break;
		case 37:
			this.arr_PN = this.updatePN(this.list_BSOD, this.arr_PN);
			break;

		case 45:
			// this.arr_PN = updatePN(list_BSOD, arr_PN);
			// FPN = value return by updatePN()
			break;

		case 47: // end of day
			this.arr_oldPN = this.updateOldPN(this.arr_PN, this.arr_oldPN);
			this.mainContext.logger.trace("arr_oldPN: " + Arrays.toString(this.arr_oldPN));
			this.arr_Margin = this.updateMarginForBM(this.arr_oldPN);
			this.mainContext.logger.trace("arr_Margin: " + Arrays.toString(this.arr_Margin));

			break;
		}

		if (this.mainContext.isMarketFirstDay() && this.settlementPeriod == 47)
		{
			this.list_BOD = this.initializeBOD(0, this.arr_Margin);
			this.mainContext.logger.trace("BODs after initialisation:");
			// TestHelper.printListOfBODs(list_BOD);

		}
		else if (this.mainContext.isMarketSecondDay() && this.settlementPeriod != 0)
		{
			this.list_BOD = this.updateBOD(this.list_BOA, this.settlementPeriod, this.arr_Margin);

		}
		else if (!this.mainContext.isMarketFirstDay() && !this.mainContext.isMarketSecondDay())
		{
			this.list_BOD = this.updateBOD(this.list_BOA, this.settlementPeriod, this.arr_Margin);

		}
	}

	public BMPxTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxDem,
			double minDem, double[] baselineProfile)
	{
		super(context, cat, type, maxDem, minDem);

		this.messageBoard = mb;
		this.arr_Margin = new double[this.ticksPerDay];
		this.arr_baselineProfile = new double[this.ticksPerDay];
		System.arraycopy(baselineProfile, 0, this.arr_baselineProfile, 0, this.arr_baselineProfile.length);

	}

	public BMPxTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxGen,
			double[] baselineProfile)
	{
		super(context, cat, type, maxGen);

		this.messageBoard = mb;
		this.arr_Margin = new double[this.ticksPerDay];
		this.arr_baselineProfile = new double[this.ticksPerDay];
		System.arraycopy(baselineProfile, 0, this.arr_baselineProfile, 0, this.arr_baselineProfile.length);

	}

	public BMPxTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxGen)
	{
		super(context, cat, type, maxGen);

		this.messageBoard = mb;
		this.arr_Margin = new double[this.ticksPerDay];

	}

}
