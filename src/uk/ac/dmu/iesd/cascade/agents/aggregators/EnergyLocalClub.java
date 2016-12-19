/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;

/**
 * @author Richard
 *
 */
public class EnergyLocalClub extends SupplierCoAdvancedModel
{
	
	double[] usePerDay;
	double[] genPerDay;
	double[] importPerDay;
	double[] exportPerDay;
	
	double[] tariff;
	
	public double[] calculateDailyShare()
	{
		double[] retArr = null;
		return retArr;
	}
	
	public double[] calculateDailySavings()
	{
		double[] retArr = null;
		return retArr;
	}

	/**
	 * @param context
	 * @param mb
	 * @param cat
	 * @param type
	 * @param maxDem
	 * @param minDem
	 * @param baseDemand
	 */
	public EnergyLocalClub(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxDem, double minDem,
			double[] baseDemand)
	{
		super(context, mb, cat, type, maxDem, minDem, baseDemand);
	}

	/**
	 * @param context
	 */
	public EnergyLocalClub(CascadeContext context)
	{
		super(context);
	}

}
