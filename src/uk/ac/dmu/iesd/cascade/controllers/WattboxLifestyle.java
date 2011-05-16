/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.Arrays;

/**
 * @author jsnape
 *
 */
public enum WattboxLifestyle {
	WORKING_WEEK (1),
	HEAVY_OCCUPANCY (2),
	IRREGULAR (3);

	//hold a probability vector for occupancy.
	//all values restricted 1 to 0
	private final double[] initialOccupancy;

	WattboxLifestyle(int occupancyPrior)
	{
		// TODO - if we want truly variable ticks per day, need to look at this
		int ticksPerDay = 48;
		initialOccupancy = new double[ticksPerDay];
		switch (occupancyPrior)
		{
		case 1:
			Arrays.fill(initialOccupancy, 0, 15, 1);
			Arrays.fill(initialOccupancy, 16, 33, 0);
			Arrays.fill(initialOccupancy, 34, 47, 1);
			break;
		case 2:
			Arrays.fill(initialOccupancy, 0, 47, 1);
			break;
		case 3:
			Arrays.fill(initialOccupancy, 0, 15, 0.5);
			break;
		}
	}
	
	public void evaluateOccupancy()
	{
		
	}
}
