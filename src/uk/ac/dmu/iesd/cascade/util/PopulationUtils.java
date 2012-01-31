/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import repast.simphony.query.PropertyEquals;
import repast.simphony.query.Query;
import repast.simphony.util.collections.IndexedIterable;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.context.HouseholdProsumer;

/**
 * Provides utilities to perform operations across a population in a simulation
 * 
 * @author jsnape
 *
 */
public class PopulationUtils {
	
	
	/**
	 * 
	 */
	public static void testAndPrintHouseholdApplianceProportions(CascadeContext thisContext) {
		// TODO Auto-generated method stub
		IndexedIterable<HouseholdProsumer> householdProsumers = thisContext.getObjects(HouseholdProsumer.class);
		int totalPopulation = IterableUtils.count(householdProsumers);
			
		System.out.println("Population proportions");
		System.out.println("======================");
		System.out.println();
		System.out.println("There are " + totalPopulation + "agents");
		for ( int i = 1; i <= Consts.OCCUPANCY_PROBABILITY_ARRAY.length; i++)
		{
		Query<HouseholdProsumer> occ1Query = new PropertyEquals(thisContext, "numOccupants", i);
		System.out.println(((IterableUtils.count(occ1Query.query()) * 100) / totalPopulation) + "% of agents with occupancy " + i);
		}
		String[] coldAppliances = {"hasFridgeFreezer", "hasRefrigerator", "hasUprightFreezer", "hasChestFreezer"};
		String[] wetAppliances = {"hasWashingMachine", "hasWasherDryer", "hasTumbleDryer", "hasDishWasher"};
		
		for (int i = 0; i < coldAppliances.length; i++)
		{
			Query<HouseholdProsumer> occ1Query = new PropertyEquals(thisContext, coldAppliances[i], true);
			System.out.println(((IterableUtils.count(occ1Query.query()) * 100) / totalPopulation) + "% of agents with appliance " + coldAppliances[i]);
		}
		
		for (int i = 0; i < wetAppliances.length; i++)
		{

			Query<HouseholdProsumer> occ1Query = new PropertyEquals(thisContext, wetAppliances[i], true);
			System.out.println(((IterableUtils.count(occ1Query.query()) * 100) / totalPopulation) + "% of agents with appliance " + wetAppliances[i]);						
		}
		
	}

}
