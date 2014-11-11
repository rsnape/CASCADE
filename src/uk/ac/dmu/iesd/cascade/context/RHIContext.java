/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import java.util.Date;
import java.util.WeakHashMap;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.PropertyEquals;
import repast.simphony.query.Query;
import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.agents.prosumers.RHIAdopterHousehold;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.HEATING_TYPE;
import uk.ac.dmu.iesd.cascade.util.DatedTimeSeries;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

/**
 * RHI Context encapsulates the conditions envisaged upon implementation of the
 * UK Renewable Heat Incentive (RHI)
 * 
 * @author Richard
 * 
 */
public class RHIContext extends AdoptionContext
{

	/*
	 * Failure rate of heating systems is a crucial input factor. Here we make
	 * it consistent across all types and set it as probabilistic, once in 5
	 * years.
	 */
	public final double dailyChanceOfFailure = 1.0 / (5.0 * 365);
	WeakHashMap<HEATING_TYPE, Integer> currentRHItariff = new WeakHashMap<HEATING_TYPE, Integer>();
	DatedTimeSeries<WeakHashMap<HEATING_TYPE, int[]>> assesmentDates = new DatedTimeSeries<WeakHashMap<HEATING_TYPE, int[]>>();
	private int numHouseholds;
	public static WeakHashMap<HEATING_TYPE, Double> heatingCostPerkWh = new WeakHashMap<HEATING_TYPE, Double>();

	/**
	 * @param numHouseholds
	 *            the numHouseholds to set
	 */
	public void setNumHouseholds(int numHouseholds)
	{
		this.numHouseholds = numHouseholds;
	}

	public void initialise()
	{
		WeakHashMap<HEATING_TYPE, int[]> val = new WeakHashMap<HEATING_TYPE, int[]>();
		val.put(HEATING_TYPE.BIOMASS, new int[]
		{ Integer.MAX_VALUE, Integer.MAX_VALUE });
		val.put(HEATING_TYPE.AIR_SOURCE_HP, new int[]
		{ Integer.MAX_VALUE, Integer.MAX_VALUE }); // check the values here -
													// put in the comm
													// spend thresholds - in
													// pound units
		val.put(HEATING_TYPE.GND_SOURCE_HP, new int[]
		{ Integer.MAX_VALUE, Integer.MAX_VALUE });
		val.put(HEATING_TYPE.SOLAR_THERMAL, new int[]
		{ Integer.MAX_VALUE, Integer.MAX_VALUE });
		this.assesmentDates.setBeforeAllTimesValue(val);

		val = new WeakHashMap<HEATING_TYPE, int[]>();
		val.put(HEATING_TYPE.BIOMASS, new int[]
		{ 2400000, 4800000 });
		val.put(HEATING_TYPE.AIR_SOURCE_HP, new int[]
		{ 2400000, 4800000 }); // check
								// the
								// values
								// here
								// -
								// put
								// in
								// the
								// comm
								// spend
								// thresholds
								// -
								// in
								// pound
								// units
		val.put(HEATING_TYPE.GND_SOURCE_HP, new int[]
		{ 2400000, 4800000 });
		val.put(HEATING_TYPE.SOLAR_THERMAL, new int[]
		{ 1200000, 2300000 });
		this.assesmentDates.putValue(new Date("31 Jul 14"), val);
		val = new WeakHashMap<HEATING_TYPE, int[]>();
		val.put(HEATING_TYPE.BIOMASS, new int[]
		{ 4200000, 8400000 });
		val.put(HEATING_TYPE.AIR_SOURCE_HP, new int[]
		{ 4200000, 8400000 }); // check
								// the
								// values
								// here
								// -
								// put
								// in
								// the
								// comm
								// spend
								// thresholds
								// -
								// in
								// pound
								// units
		val.put(HEATING_TYPE.GND_SOURCE_HP, new int[]
		{ 4200000, 8400000 });
		val.put(HEATING_TYPE.SOLAR_THERMAL, new int[]
		{ 2100000, 4100000 });
		this.assesmentDates.putValue(new Date("31 Oct 14"), val);
		val = new WeakHashMap<HEATING_TYPE, int[]>();
		val.put(HEATING_TYPE.BIOMASS, new int[]
		{ 6000000, 12000000 });
		val.put(HEATING_TYPE.AIR_SOURCE_HP, new int[]
		{ 6000000, 12000000 }); // check
								// the
								// values
								// here
								// -
								// put
								// in
								// the
								// comm
								// spend
								// thresholds
								// -
								// in
								// pound
								// units
		val.put(HEATING_TYPE.GND_SOURCE_HP, new int[]
		{ 6000000, 12000000 });
		val.put(HEATING_TYPE.SOLAR_THERMAL, new int[]
		{ 2900000, 5900000 });
		this.assesmentDates.putValue(new Date("31 Jan 15"), val);
		val = new WeakHashMap<HEATING_TYPE, int[]>();
		val.put(HEATING_TYPE.BIOMASS, new int[]
		{ 8400000, 16800000 });
		val.put(HEATING_TYPE.AIR_SOURCE_HP, new int[]
		{ 8400000, 16800000 }); // check
								// the
								// values
								// here
								// -
								// put
								// in
								// the
								// comm
								// spend
								// thresholds
								// -
								// in
								// pound
								// units
		val.put(HEATING_TYPE.GND_SOURCE_HP, new int[]
		{ 8400000, 16800000 });
		val.put(HEATING_TYPE.SOLAR_THERMAL, new int[]
		{ 3900000, 7800000 });
		this.assesmentDates.putValue(new Date("30 Apr 15"), val);
		val = new WeakHashMap<HEATING_TYPE, int[]>();
		val.put(HEATING_TYPE.BIOMASS, new int[]
		{ 11900000, 23900000 });
		val.put(HEATING_TYPE.AIR_SOURCE_HP, new int[]
		{ 11900000, 23900000 }); // check
									// the
									// values
									// here
									// -
									// put
									// in
									// the
									// comm
									// spend
									// thresholds
									// -
									// in
									// pound
									// units
		val.put(HEATING_TYPE.GND_SOURCE_HP, new int[]
		{ 11900000, 23900000 });
		val.put(HEATING_TYPE.SOLAR_THERMAL, new int[]
		{ 5000000, 10000000 });
		this.assesmentDates.putValue(new Date("31 Jul 15"), val);
		val = new WeakHashMap<HEATING_TYPE, int[]>();
		val.put(HEATING_TYPE.BIOMASS, new int[]
		{ 15500000, 31100000 });
		val.put(HEATING_TYPE.AIR_SOURCE_HP, new int[]
		{ 15500000, 31100000 }); // check
									// the
									// values
									// here
									// -
									// put
									// in
									// the
									// comm
									// spend
									// thresholds
									// -
									// in
									// pound
									// units
		val.put(HEATING_TYPE.GND_SOURCE_HP, new int[]
		{ 15500000, 31100000 });
		val.put(HEATING_TYPE.SOLAR_THERMAL, new int[]
		{ 6100000, 12200000 });
		this.assesmentDates.putValue(new Date("31 Oct 15"), val);
		val = new WeakHashMap<HEATING_TYPE, int[]>();
		val.put(HEATING_TYPE.BIOMASS, new int[]
		{ 19100000, 38200000 });
		val.put(HEATING_TYPE.AIR_SOURCE_HP, new int[]
		{ 19100000, 38200000 }); // check
									// the
									// values
									// here
									// -
									// put
									// in
									// the
									// comm
									// spend
									// thresholds
									// -
									// in
									// pound
									// units
		val.put(HEATING_TYPE.GND_SOURCE_HP, new int[]
		{ 19100000, 38200000 });
		val.put(HEATING_TYPE.SOLAR_THERMAL, new int[]
		{ 7200000, 14400000 });
		this.assesmentDates.putValue(new Date("31 Jan 16"), val);

		/*
		 * tariffs in 10ths of pence per kWh
		 */
		int airSourceHP = 73;
		int GroundOrWaterSourceHP = 188;
		int biomassBoilers = 122;
		int solarThermal = 192;

		this.currentRHItariff = new WeakHashMap<HEATING_TYPE, Integer>();
		this.currentRHItariff.put(HEATING_TYPE.AIR_SOURCE_HP, airSourceHP);
		this.currentRHItariff.put(HEATING_TYPE.GND_SOURCE_HP, GroundOrWaterSourceHP);
		this.currentRHItariff.put(HEATING_TYPE.BIOMASS, biomassBoilers);
		this.currentRHItariff.put(HEATING_TYPE.SOLAR_THERMAL, solarThermal);

		// costs from
		// http://www.biomassenergycentre.org.uk/portal/page?_pageid=75,59188&_dad=portal
		// may be slightly biased and might want to update
		// see also http://www.nottenergy.com/energy_cost_comparison col 3
		//
		RHIContext.heatingCostPerkWh.put(HEATING_TYPE.GRID_GAS, 4.3);
		RHIContext.heatingCostPerkWh.put(HEATING_TYPE.CALOR_GAS, 6.6); // Note
																		// various
		// types of off grid
		// gas - this is
		// representative
		// (LPG)
		RHIContext.heatingCostPerkWh.put(HEATING_TYPE.ELECTRIC_STORAGE, 15.0);
		RHIContext.heatingCostPerkWh.put(HEATING_TYPE.OIL, 6.6); // average
																	// between
		// kerosene and gas oil
		RHIContext.heatingCostPerkWh.put(HEATING_TYPE.BIOMASS, 4.4);
		RHIContext.heatingCostPerkWh.put(HEATING_TYPE.AIR_SOURCE_HP, 5.4); // Cost
																			// of
		// electricity /
		// (i.e.
		// implicit CoP
		// = 2.7)
		RHIContext.heatingCostPerkWh.put(HEATING_TYPE.GND_SOURCE_HP, 4.2); // Cost
																			// of
		// electricity /
		// (i.e.
		// implicit CoP
		// = 3.6)
		RHIContext.heatingCostPerkWh.put(HEATING_TYPE.SOLAR_THERMAL, 0.0); // Water
																			// only
		
		this.hassleFactors = new WeakHashMap<Consts.HEATING_TYPE, Double>();
		this.hassleFactors.put(HEATING_TYPE.AIR_SOURCE_HP, 0.7);
		this.hassleFactors.put(HEATING_TYPE.BIOMASS, 0.5);
		this.hassleFactors.put(HEATING_TYPE.CALOR_GAS, 0.1);		
		this.hassleFactors.put(HEATING_TYPE.ELECTRIC_STORAGE, 0.3);
		this.hassleFactors.put(HEATING_TYPE.GND_SOURCE_HP, 0.9);
		this.hassleFactors.put(HEATING_TYPE.GRID_GAS, 0.1);
		this.hassleFactors.put(HEATING_TYPE.OIL, 0.2);		
		this.hassleFactors.put(HEATING_TYPE.SOLAR_THERMAL, 0.4);


	}

	/*
	 * The degression check as programmed by DECC / Ofgem
	 * 
	 * this will be undertaken quarterly (modelled here as 92 days (max "gap"
	 * between quarters)
	 * 
	 * TODO: trigger this on actual review points. Then could pass in the
	 * thresholds i.e. pseudo code:
	 * Schedule.scheduleMethod(this.checkDegression, dateToTick(reviewDate),
	 * WeakHashMap<heatingType, int[]> thresholdMap)
	 */
	@ScheduledMethod(start = 49, interval = 48 * 92, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void checkDegression()
	{
		for (HEATING_TYPE t : HEATING_TYPE.values())
		{

			// Committed spend here is in 10ths of pence (as that is what the
			// tariffs are
			// stored as
			int committedSpend = this.getCommittedSpend(t);

			if (committedSpend > 0)
			{
				// Get trigger level in pounds
				long superTriggerLevel = (this.assesmentDates.getValue(this.getDateTime())).get(t)[1];
				long triggerLevel = (this.assesmentDates.getValue(this.getDateTime())).get(t)[0];

				// Manipulate proportionally to number of households in model
				// and into units of 10ths of pence
				superTriggerLevel *= (1000 * this.numHouseholds);
				triggerLevel *= (1000 * this.numHouseholds);

				superTriggerLevel /= Consts.UK_HOUSEHOLD_COUNT;
				triggerLevel /= Consts.UK_HOUSEHOLD_COUNT;

				this.logger.debug("Checking degression for " + t + "; comm spend = " + committedSpend + "; proportional trigger "
						+ triggerLevel + "; superTrgg = " + superTriggerLevel);

				if (committedSpend > superTriggerLevel)
				{
					int c = this.currentRHItariff.get(t);
					this.logger.info("Super trigger hit for " + t + ", reduce tariff from " + c + " to " + (0.8 * c));
					this.currentRHItariff.put(t, (int) (c * 0.8));
				}
				else if (committedSpend > triggerLevel)
				{
					// trigger hit
					int c = this.currentRHItariff.get(t);
					this.logger.info("Trigger hit for " + t + ", reduce tariff from " + c + " to " + (0.9 * c));
					this.currentRHItariff.put(t, (int) (c * 0.9));
				}
			}
		}
	}

	/**
	 * Understand this number as an annualised spend (c.f. DECC documents), so
	 * the committed spend is deemed annual load * tariff for the particular
	 * technology
	 * 
	 * @param t
	 * @return
	 */
	private int getCommittedSpend(HEATING_TYPE t)
	{
		Integer techTariff = this.currentRHItariff.get(t);
		if (techTariff != null)
		{
			// TODO: Assumption here on deemed load per household as DECC
			// methodology not yet found
			int numAdopters = this.getNumberWith(t);
			System.err.println(t + " has " + numAdopters + " adopters");
			int deemedAnnualLoad = numAdopters * 20 * 365; // (20 kWh per day -
															// assumption)
			return deemedAnnualLoad * techTariff;
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @param t
	 * @return
	 */
	private int getNumberWith(HEATING_TYPE t)
	{
		this.logger.trace("Get number called at " + System.nanoTime());
		Query<RHIAdopterHousehold> adoptionQuery = new PropertyEquals<RHIAdopterHousehold>(this, "RHIEligibleHeatingOwned", t);
		Iterable<RHIAdopterHousehold> agentsWithTypeT = adoptionQuery.query();
		int ret = IterableUtils.count(agentsWithTypeT);
		this.logger.trace("Got count at " + System.nanoTime() + " = " + ret);
		return ret;
	}

	/**
	 * @param context
	 */
	public RHIContext(Context context)
	{
		super(context);
		this.initialise();
	}

	/**
	 * @param context
	 * @param string
	 */
	public RHIContext(Context context, String string)
	{
		super(context, string);
		this.initialise();
	}

	/**
	 * @param potentialRHICapacity
	 * @return
	 */
	public double getRHISystemPrice(double potentialRHICapacity, HEATING_TYPE t)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param potentialRHICapacity
	 * @param type
	 *            of RHI technology
	 * @return
	 */
	public double getRHITariff(double potentialRHICapacity, HEATING_TYPE t)
	{
		if (t == null)
		{
			return 0;
		}
		return this.currentRHItariff.get(t);
	}

	public int getASHPCount()
	{
		return this.getNumberWith(HEATING_TYPE.AIR_SOURCE_HP);
	}

	public int getGSHPCount()
	{
		return this.getNumberWith(HEATING_TYPE.GND_SOURCE_HP);
	}

	public int getBiomassCount()
	{
		return this.getNumberWith(HEATING_TYPE.BIOMASS);
	}

	/**
	 * @return next date that the tariff is scheduled to change
	 */
	public Date getRHIAvailableUntil()
	{
		Date retVal = new Date();
		Date now = this.getDateTime();

		this.logger.debug("Finding key following " + this.ukDateParser.format(now));

		retVal = this.assesmentDates.getFirstKeyFollowing(now);

		if (retVal == null)
		{
			// There is no end date specified - assume available "forever"
			retVal = new Date();
			retVal.setTime(Long.MAX_VALUE);
		}

		return retVal;
	}

	/**
	 * @param rhiEligibleHeatingTechnology
	 * @return
	 */
	public double getQuote(HEATING_TYPE rhiTech)
	{
		double retVal;
		if (rhiTech == null)
		{
			return Double.MAX_VALUE;
		}

		switch (rhiTech)
		{
		case AIR_SOURCE_HP:
			retVal = 7000 + RandomHelper.nextDouble() * 4000;
			break;
		case GRID_GAS:
			retVal = 2000 + RandomHelper.nextDouble() * 1500;
			break;
		case GND_SOURCE_HP:
			retVal = 11000 + RandomHelper.nextDouble() * 4000;
			break;
		case BIOMASS:
			retVal = 4000 + RandomHelper.nextDouble() * 1500;
			break;
		case SOLAR_THERMAL:
			retVal = 3000 + RandomHelper.nextDouble() * 750;
			break;
		default:
			retVal = Double.MAX_VALUE;
			break;
		}

		return retVal;
	}

	public WeakHashMap<HEATING_TYPE, Double> hassleFactors;
	
	/**
	 * @param rhiEligibleHeatingTechnology
	 * @return
	 */
	public double getHassleFactor(HEATING_TYPE heatingTechnology)
	{
		// TODO Auto-generated method stub
		return hassleFactors.get(heatingTechnology);
	}

}
