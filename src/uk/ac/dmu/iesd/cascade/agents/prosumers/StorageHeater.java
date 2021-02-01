/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.Arrays;

import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.base.Consts;

/**
 * @author Richard
 *
 */
public class StorageHeater
{
	int capacity;
	int maxReleasePower; // Maximum power released when heating
	double chargePower; // Maximum power drawn when charging
	double heatStored;
	double[] releaseProfile;
	int[] chargeProfile;
	private HouseholdProsumer owner;
	double[] stateOfCharge;
	
	double getDemand(int timestep)
	{
		if (this.owner.mainContext.logger.isTraceEnabled()) {
			this.owner.mainContext.logger.trace("Calculating storage heater demand. Current charge " + this.heatStored);
		}
		double chargePerTimestep = (this.chargePower * Consts.HOURS_PER_DAY)/this.owner.mainContext.ticksPerDay;
		if (chargeProfile[timestep] < 0)
		{
			System.err.println("Charging has gone negative at timestep " + timestep);
		}
		double demand = chargeProfile[timestep] * chargePerTimestep;
		heatStored += demand * 0.87; // From Becceril MSc thesis: expected heat losses during the charging up of the heater are in the range of 13% in normal operation conditions
		
		// If the thing is already full, we won't be charging, so remove that...
		if (heatStored > capacity)
		{
			demand += (capacity - heatStored) / 0.87;
			heatStored = capacity;
		}
		if (this.owner.mainContext.logger.isTraceEnabled()) {
			this.owner.mainContext.logger.trace("Returning demand "+demand+". Current charge " + this.heatStored);
		}
		
		return demand;
	}
	
	double demandHeat(double heatDemanded)
	{
		double retVal = heatDemanded;
		if (heatStored > heatDemanded)	
		{
			heatStored -= heatDemanded;
		}
		else
		{
			retVal = heatStored;
			heatStored = 0;
		}
		this.stateOfCharge[this.owner.mainContext.getTimeslotOfDay()] = heatStored;
		return retVal;

	}
	
	/**
	 * @return
	 */
	public double[] getHistoricalChargeState()
	{
		return this.stateOfCharge;
	}
	
	public StorageHeater(HouseholdProsumer owner)
	{
		this.owner = owner;
		this.chargeProfile = new int[48];
		// Initialise the Southern region economy 7 hours (23:30 = 06:30)
		// see https://customerservices.npower.com/app/answers/detail/a_id/179/~/what-are-the-economy-7-peak-and-off-peak-periods%3F
		this.chargeProfile[47]=1;
		Arrays.fill(chargeProfile, 0, 13, 1);
		
		//chargePower = RandomHelper.nextDouble() + 3; //Initialise chargePower between 3 and 4 kW
		this.chargePower = 2; //Empirical evidence from swell shows max charg power 1 kWh in a hlaf hour i.e. 2 kW
		this.capacity = (int) (chargePower*7); // 7 is a rule of thumb - see e.g http://www.storageheaters.com/storage-heater-kw.htm
		                               // Tallies with charging all night on an economy seven
		
		this.heatStored = this.capacity; // Start full - pretty arbitrary, but might as well.
		this.stateOfCharge = new double[this.owner.mainContext.ticksPerDay];
		Arrays.fill(this.stateOfCharge,this.capacity);
		
	}



}
