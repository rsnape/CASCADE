/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import repast.simphony.query.PropertyEquals;
import repast.simphony.query.Query;
import repast.simphony.util.collections.IndexedIterable;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

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
			
		//if (Consts.DEBUG) System.out.println("Population proportions");
	//	if (Consts.DEBUG) System.out.println("======================");
		//if (Consts.DEBUG) System.out.println();
		//if (Consts.DEBUG) System.out.println("There are " + totalPopulation + "agents");
		for ( int i = 1; i <= Consts.OCCUPANCY_PROBABILITY_ARRAY.length; i++)
		{
		Query<HouseholdProsumer> occ1Query = new PropertyEquals(thisContext, "numOccupants", i);
		//if (Consts.DEBUG) System.out.println(((IterableUtils.count(occ1Query.query()) * 100) / totalPopulation) + "% of agents with occupancy " + i);
		}
		String[] coldAppliances = {"hasFridgeFreezer", "hasRefrigerator", "hasUprightFreezer", "hasChestFreezer"};
		String[] wetAppliances = {"hasWashingMachine", "hasWasherDryer", "hasTumbleDryer", "hasDishWasher"};
		
		for (int i = 0; i < coldAppliances.length; i++)
		{
			Query<HouseholdProsumer> occ1Query = new PropertyEquals(thisContext, coldAppliances[i], true);
			//if (Consts.DEBUG) System.out.println(((IterableUtils.count(occ1Query.query()) * 100) / totalPopulation) + "% of agents with appliance " + coldAppliances[i]);
		}
		
		for (int i = 0; i < wetAppliances.length; i++)
		{

			Query<HouseholdProsumer> occ1Query = new PropertyEquals(thisContext, wetAppliances[i], true);
			//if (Consts.DEBUG) System.out.println(((IterableUtils.count(occ1Query.query()) * 100) / totalPopulation) + "% of agents with appliance " + wetAppliances[i]);						
		}
		
	}

}
