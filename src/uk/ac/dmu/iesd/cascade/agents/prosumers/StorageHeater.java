/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

/**
 * @author Richard
 *
 */
public class StorageHeater
{
	int capacity;
	int maxReleasePower; // Maximum power released when heating
	int maxChargePower; // Maximum power drawn when charging
	double heatStored;
	double[] releaseProfile;
	int[] chargeProfile;
	private HouseholdProsumer owner;
	
	double getDemand(int timestep)
	{
		double demand = chargeProfile[timestep] * maxChargePower;
		heatStored += demand * 0.87; // From Becceril MSc thesis: expected heat losses during the charging up of the heater are in the range of 13% in normal operation conditions
		if (heatStored > capacity)
		{
			demand = (capacity - heatStored) / 0.87;
			heatStored = capacity;
		}
		
		return demand;
	}
	
	void demandHeat(double heatDemanded)
	{
		
	}
	
	public StorageHeater(HouseholdProsumer owner)
	{
		this.owner = owner;
		this.chargeProfile = new int[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0};
	}

}
