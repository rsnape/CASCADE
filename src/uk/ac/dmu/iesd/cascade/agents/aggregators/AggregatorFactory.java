package uk.ac.dmu.iesd.cascade.agents.aggregators;

import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;

/**
 * This class is a factory for creating instances of <tt>AggregatorAgent</tt>
 * concrete subclasses (e.g. <code>SupplierCo</code>) Its public creator
 * method's signatures are defined by {@link IAggregatorFactory} interface.
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/19 14:00:00 $
 */

public class AggregatorFactory implements IAggregatorFactory
{

	private CascadeContext cascadeMainContext;
	private MarketMessageBoard messageBoard;

	public AggregatorFactory(CascadeContext context)
	{
		this.cascadeMainContext = context;
	}

	public AggregatorFactory(CascadeContext context, MarketMessageBoard mb)
	{
		this.cascadeMainContext = context;
		this.messageBoard = mb;
	}

	/*
	 * public SupplierCo createSupplierCo(double[] baseProfile, double maxDem,
	 * double minDem){ SupplierCo asCO = new SupplierCo(cascadeMainContext,
	 * messageBoard, baseProfile, BMU_CATEGORY.DEM_S, BMU_TYPE.DEM_LARGE,
	 * maxDem, minDem); return asCO; }
	 */

	@Override
	public SupplierCo createSupplierCo(double[] baseProfile)
	{
		// SupplierCo asCO = new SupplierCo(cascadeMainContext, baseProfile);

		double maxDem = ASTEMConsts.BMU_SMALLDEM_MAXDEM;
		double minDem = ASTEMConsts.BMU_SMALLDEM_MINDEM;
		SupplierCo asCO = new SupplierCo(this.cascadeMainContext, this.messageBoard, BMU_CATEGORY.DEM_S, BMU_TYPE.DEM_SMALL, maxDem,
				minDem, baseProfile);

		return asCO;
	}

	public SupplierCoAdvancedModel createSupplierCoAdvanced(double[] baseProfile)
	{
		// SupplierCo asCO = new SupplierCo(cascadeMainContext, baseProfile);

		double maxDem = ASTEMConsts.BMU_SMALLDEM_MAXDEM;
		double minDem = ASTEMConsts.BMU_SMALLDEM_MINDEM;
		SupplierCoAdvancedModel asCO = new SupplierCoAdvancedModel(this.cascadeMainContext, this.messageBoard, BMU_CATEGORY.DEM_S,
				BMU_TYPE.DEM_SMALL, maxDem, minDem, baseProfile);

		return asCO;
	}
	
	public EnergyLocalClub createEnergyLocalClub(double[] baseProfile)
	{
		// SupplierCo asCO = new SupplierCo(cascadeMainContext, baseProfile);

		double maxDem = ASTEMConsts.BMU_SMALLDEM_MAXDEM;
		double minDem = ASTEMConsts.BMU_SMALLDEM_MINDEM;
		EnergyLocalClub asCO = new EnergyLocalClub(this.cascadeMainContext, this.messageBoard, BMU_CATEGORY.DEM_S,
				BMU_TYPE.DEM_SMALL, maxDem, minDem, baseProfile);

		return asCO;
	}

	public WindFarmAggregator createWindFarmAggregator()
	{
		// SupplierCo asCO = new SupplierCo(cascadeMainContext, baseProfile);
		double maxGen = ASTEMConsts.BMU_WIND_MAXCAP; // in MW
		WindFarmAggregator wFA = new WindFarmAggregator(this.cascadeMainContext, this.messageBoard, maxGen);

		return wFA;
	}

	public BMPxTraderAggregator createGenericBMPxTraderAggregator(BMU_TYPE type, double[] baseLoadProfile)
	{
		BMPxTraderAggregator bmuAgent = null;

		double maxDem = 0;
		double minDem = 0;
		double maxGen = 0;

		switch (type)
		{
		case DEM_LARGE:
			maxDem = ASTEMConsts.BMU_LARGEDEM_MAXDEM;
			minDem = ASTEMConsts.BMU_LARGEDEM_MINDEM;
			bmuAgent = new GenericBMPxTraderAggregator(this.cascadeMainContext, this.messageBoard, BMU_CATEGORY.DEM_S, type, maxDem,
					minDem, baseLoadProfile);
			break;
		case DEM_SMALL:
			maxDem = ASTEMConsts.BMU_SMALLDEM_MAXDEM;
			minDem = ASTEMConsts.BMU_SMALLDEM_MINDEM;
			bmuAgent = new GenericBMPxTraderAggregator(this.cascadeMainContext, this.messageBoard, BMU_CATEGORY.DEM_S, type, maxDem,
					minDem, baseLoadProfile);
			break;
		case GEN_COAL:
			maxGen = ASTEMConsts.BMU_COAL_MAXCAP;
			bmuAgent = new GenericBMPxTraderAggregator(this.cascadeMainContext, this.messageBoard, BMU_CATEGORY.GEN_T, type, maxGen,
					baseLoadProfile);
			break;
		case GEN_CCGT:
			maxGen = ASTEMConsts.BMU_CCGT_MAXCAP;
			bmuAgent = new GenericBMPxTraderAggregator(this.cascadeMainContext, this.messageBoard, BMU_CATEGORY.GEN_T, type, maxGen,
					baseLoadProfile);
			break;
		case GEN_WIND:
			maxGen = ASTEMConsts.BMU_WIND_MAXCAP;
			bmuAgent = new GenericBMPxTraderAggregator(this.cascadeMainContext, this.messageBoard, BMU_CATEGORY.GEN_T, type, maxGen,
					baseLoadProfile);
			break;

		}

		return bmuAgent;
	}

	/**
	 * (02/06/12) DF
	 * 
	 * Creating a single non domestic aggregator
	 */
	public SingleNonDomesticAggregator createSingleNonDomesticAggregator(double[] baseProfile)
	{

		double maxDem = ASTEMConsts.BMU_LARGEDEM_MAXDEM;
		;
		double minDem = ASTEMConsts.BMU_LARGEDEM_MINDEM;
		SingleNonDomesticAggregator singleNonDomestic = new SingleNonDomesticAggregator(this.cascadeMainContext, this.messageBoard,
				BMU_CATEGORY.DEM_S, BMU_TYPE.DEM_SMALL, maxDem, minDem, baseProfile);

		return singleNonDomestic;
	}

}
