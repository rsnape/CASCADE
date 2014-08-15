/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import java.util.ArrayList;
import java.util.Date;
import java.util.WeakHashMap;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.PropertyEquals;
import repast.simphony.query.Query;
import uk.ac.dmu.iesd.cascade.agents.prosumers.Household;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.RHIAdopterHousehold;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.RHI_TYPE;
import uk.ac.dmu.iesd.cascade.util.DatedIntTimeSeries;
import uk.ac.dmu.iesd.cascade.util.DatedTimeSeries;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

/**
 * RHI Context encapsulates the conditions envisaged upon implementation of the 
 * UK Renewable Heat Incentive (RHI)
 * 
 * @author Richard
 *
 */
public class RHIContext extends AdoptionContext {
	
	WeakHashMap<RHI_TYPE,Integer> currentRHItariff = new WeakHashMap<RHI_TYPE,Integer>();
	DatedTimeSeries<WeakHashMap<RHI_TYPE,int[]>> assesmentDates = new DatedTimeSeries<WeakHashMap<RHI_TYPE,int[]>>();
	private int numHouseholds; 
	
	/**
	 * @param numHouseholds the numHouseholds to set
	 */
	public void setNumHouseholds(int numHouseholds) {
		this.numHouseholds = numHouseholds;
	}

	public void initialise()
	{
		WeakHashMap<RHI_TYPE, int[]> val = new WeakHashMap<RHI_TYPE, int[]>();
		val.put(RHI_TYPE.BIOMASS, new int[] {0, 0});
		val.put(RHI_TYPE.AIR_SOURCE_HP, new int[] {0, 0}); // check the values here - put in the comm spend thresholds - in  pound units
		val.put(RHI_TYPE.GND_SOURCE_HP, new int[] {0, 0});
		val.put(RHI_TYPE.SOLAR_THERMAL, new int[] {0, 0});
		assesmentDates.setBeforeAllTimesValue(val);
		
		
		val.put(RHI_TYPE.BIOMASS, new int[] {24, 48});
		val.put(RHI_TYPE.AIR_SOURCE_HP, new int[] {2400000, 4800000}); // check the values here - put in the comm spend thresholds - in pound units
		val.put(RHI_TYPE.GND_SOURCE_HP, new int[] {2400000, 4800000});
		val.put(RHI_TYPE.SOLAR_THERMAL, new int[] {1200000,2300000});
		assesmentDates.putValue(new Date("31 Jul 14"), val);
		val = new WeakHashMap<RHI_TYPE, int[]>();
		val.put(RHI_TYPE.BIOMASS, new int[] {4200000, 8400000});
		val.put(RHI_TYPE.AIR_SOURCE_HP, new int[] {4200000, 8400000}); // check the values here - put in the comm spend thresholds - in  pound units
		val.put(RHI_TYPE.GND_SOURCE_HP, new int[] {4200000, 8400000});
		val.put(RHI_TYPE.SOLAR_THERMAL, new int[] {2100000, 4100000});
		assesmentDates.putValue(new Date("31 Oct 14"), val);
		val = new WeakHashMap<RHI_TYPE, int[]>();
		val.put(RHI_TYPE.BIOMASS, new int[] {6000000, 12000000});
		val.put(RHI_TYPE.AIR_SOURCE_HP, new int[] {6000000, 12000000}); // check the values here - put in the comm spend thresholds - in  pound units
		val.put(RHI_TYPE.GND_SOURCE_HP, new int[] {6000000, 12000000});
		val.put(RHI_TYPE.SOLAR_THERMAL, new int[] {2900000, 5900000});
		assesmentDates.putValue(new Date("31 Jan 15"), val);
		val = new WeakHashMap<RHI_TYPE, int[]>();
		val.put(RHI_TYPE.BIOMASS, new int[] {8400000, 16800000});
		val.put(RHI_TYPE.AIR_SOURCE_HP, new int[] {8400000, 16800000}); // check the values here - put in the comm spend thresholds - in  pound units
		val.put(RHI_TYPE.GND_SOURCE_HP, new int[] {8400000, 16800000});
		val.put(RHI_TYPE.SOLAR_THERMAL, new int[] {3900000, 7800000});
		assesmentDates.putValue(new Date("30 Apr 15"), val);
		val = new WeakHashMap<RHI_TYPE, int[]>();
		val.put(RHI_TYPE.BIOMASS, new int[] {11900000, 23900000});
		val.put(RHI_TYPE.AIR_SOURCE_HP, new int[] {11900000, 23900000}); // check the values here - put in the comm spend thresholds - in  pound units
		val.put(RHI_TYPE.GND_SOURCE_HP, new int[] {11900000, 23900000});
		val.put(RHI_TYPE.SOLAR_THERMAL, new int[] {5000000, 10000000});
		assesmentDates.putValue(new Date("31 Jul 15"), val);
		val = new WeakHashMap<RHI_TYPE, int[]>();
		val.put(RHI_TYPE.BIOMASS, new int[] {15500000, 31100000});
		val.put(RHI_TYPE.AIR_SOURCE_HP, new int[] {15500000, 31100000}); // check the values here - put in the comm spend thresholds - in  pound units
		val.put(RHI_TYPE.GND_SOURCE_HP, new int[] {15500000, 31100000});
		val.put(RHI_TYPE.SOLAR_THERMAL, new int[] {6100000, 12200000});
		assesmentDates.putValue(new Date("31 Oct 15"), val);
		val = new WeakHashMap<RHI_TYPE, int[]>();
		val.put(RHI_TYPE.BIOMASS, new int[] {19100000, 38200000});
		val.put(RHI_TYPE.AIR_SOURCE_HP, new int[] {19100000, 38200000}); // check the values here - put in the comm spend thresholds - in  pound units
		val.put(RHI_TYPE.GND_SOURCE_HP, new int[] {19100000, 38200000});
		val.put(RHI_TYPE.SOLAR_THERMAL, new int[] {7200000, 14400000});
		assesmentDates.putValue(new Date("31 Jan 16"), val);
		
		/*
		 * tariffs in 10ths of pence per kWh
		 */
		int airSourceHP	= 73;
		int GroundOrWaterSourceHP =	188;
		int biomassBoilers = 122;
		int solarThermal = 192;
		
		currentRHItariff = new WeakHashMap<RHI_TYPE, Integer>();
		currentRHItariff.put(RHI_TYPE.AIR_SOURCE_HP, airSourceHP);
		currentRHItariff.put(RHI_TYPE.GND_SOURCE_HP, GroundOrWaterSourceHP);
		currentRHItariff.put(RHI_TYPE.BIOMASS, biomassBoilers);
		currentRHItariff.put(RHI_TYPE.SOLAR_THERMAL, solarThermal);
	}
	
	/*
	 * The degression check as programmed by DECC / Ofgem
	 * 
	 * this will be undertaken quarterly (modelled here as 92 days (max "gap" between quarters)
	 * 
	 * TODO: trigger this on actual review points. Then could pass in the thresholds i.e. pseudo code: Schedule.scheduleMethod(this.checkDegression, dateToTick(reviewDate), WeakHashMap<RHI_TYPE, int[]> thresholdMap) 
	 */
	@ScheduledMethod(start = 0, interval = 48*92, shuffle = true, priority = ScheduleParameters.FIRST_PRIORITY)
	public void checkDegression()
	{
		for (RHI_TYPE t : RHI_TYPE.values())
		{
			
			int committedSpend = this.getCommittedSpend(t);
			
			//Get trigger level in pounds
			int superTriggerLevel = assesmentDates.getValue(this.getDateTime()).get(t)[1];
			int triggerLevel = assesmentDates.getValue(this.getDateTime()).get(t)[0];
			
			//Manipulate proportionally to number of households in model and into
			//units of 10ths of pence
			superTriggerLevel *= (1000 * this.numHouseholds) / Consts.UK_HOUSEHOLD_COUNT;
			triggerLevel *= (1000 * this.numHouseholds) / Consts.UK_HOUSEHOLD_COUNT;
			
			this.logger.debug("Checking degression for " + t + "; comm spend = " + committedSpend + "; proportional trigger " + triggerLevel + "; superTrgg = " + superTriggerLevel);
			
			if (committedSpend > superTriggerLevel)
			{
				int c = currentRHItariff.get(t);
				this.logger.debug("Super trigger hit, reduce tariff from " + c + " to " + (0.8*c));
				currentRHItariff.put(t, (int) (c*0.8));
			}
			else if (committedSpend > triggerLevel)
			{
				//trigger hit
				int c = currentRHItariff.get(t);
				this.logger.debug("Trigger hit, reduce tariff from " + c + " to " + (0.9*c));
				currentRHItariff.put(t, (int) (c*0.9));
			}
		}
	}

	/**
	 * Understand this number as an annualised spend (c.f. DECC documents), so the committed spend is
	 * deemed annual load * tariff for the particular technology
	 * 
	 * @param t
	 * @return
	 */
	private int getCommittedSpend(RHI_TYPE t) {
		//Assumption here as DECC methodology not easily found
		int numAdopters = getNumberWith(t);
		System.err.println(t + " has " + numAdopters + " adopters");
		int deemedAnnualLoad = numAdopters * 20 * 365; //(20 kWh per day)		
		return deemedAnnualLoad * currentRHItariff.get(t);
	}

	/**
	 * @param t
	 * @return
	 */
	private int getNumberWith(RHI_TYPE t) {
		this.logger.trace("Get number called at " + System.nanoTime());
		Query<RHIAdopterHousehold> adoptionQuery = new PropertyEquals<RHIAdopterHousehold>(this,
				"RHIEligibleHeatingOwned", t);	
		Iterable<RHIAdopterHousehold> agentsWithTypeT = adoptionQuery.query();
		int ret = IterableUtils.count(agentsWithTypeT);
		this.logger.trace("Got count at " + System.nanoTime() + " = " + ret);
		return ret;
	}

	/**
	 * @param context
	 */
	public RHIContext(Context context) {
		super(context);
		initialise();
	}

	/**
	 * @param context
	 * @param string
	 */
	public RHIContext(Context context, String string) {
		super(context, string);
		initialise();
	}

	/**
	 * @param potentialRHICapacity
	 * @return
	 */
	public double getRHISystemPrice(double potentialRHICapacity, RHI_TYPE t) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param potentialRHICapacity
	 * @param type of RHI technology
	 * @return
	 */
	public double getRHITariff(double potentialRHICapacity, RHI_TYPE t) {
		return currentRHItariff.get(t);
	}
	
	public int getASHPCount()
	{
		return getNumberWith(RHI_TYPE.AIR_SOURCE_HP);
	}
	
	public int getGSHPCount()
	{
		return getNumberWith(RHI_TYPE.GND_SOURCE_HP);
	}
	
	public int getBiomassCount()
	{
		return getNumberWith(RHI_TYPE.BIOMASS);
	}

	/**
	 * @return next date that the tariff is scheduled to change
	 */
	public Date getRHIAvailableUntil() {
    	Date retVal = new Date();    	
    	Date now = this.getDateTime();
    	
    	this.logger.debug("Finding key following " + ukDateParser.format(now));
    	
    	retVal = this.assesmentDates.getFirstKeyFollowing(now);
    	
    	if (retVal == null)
    	{
    		//There is no end date specified - assume available "forever"
    		retVal = new Date();
    		retVal.setTime(Long.MAX_VALUE);
    	}
    	
		return retVal;
	}

}
