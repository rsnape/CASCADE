/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.Arrays;

import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;

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
	
	double getDemand(int timestep)
	{
		if (chargeProfile[timestep] < 0)
		{
			System.err.println("Charging has gone negative at timestep " + timestep);
		}
		double demand = chargeProfile[timestep] * chargePower;
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
		heatStored -= heatDemanded;
	}
	
	public StorageHeater(HouseholdProsumer owner)
	{
		this.owner = owner;
		this.chargeProfile = new int[48];
		// Initialise the Southern region economy 7 hours (23:30 = 06:30)
		// see https://customerservices.npower.com/app/answers/detail/a_id/179/~/what-are-the-economy-7-peak-and-off-peak-periods%3F
		this.chargeProfile[47]=1;
		Arrays.fill(chargeProfile, 0, 13, 1);
		this.capacity = 30; // Assume that the bricks can store 30 kWh
		chargePower = RandomHelper.nextDouble() + 3; //Initialise chargePower between 3 and 4 kW
	}

}
