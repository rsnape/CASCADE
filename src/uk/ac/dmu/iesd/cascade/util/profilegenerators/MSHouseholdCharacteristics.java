/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util.profilegenerators;

/**
 * Effectively a struct holding information about the appliance ownership of a household,
 * as well as some of its physical characteristics and 
 * some income / social status related vairables.
 * 
 * Mainly based on those variables defined in HH_specific.m, although some added to make
 * the class more general purpose.  Those not in the original are commented (added)
 * 
 * @author jsnape
 *
 */
public class MSHouseholdCharacteristics {

	public boolean heat_marker; // equivalent to hasSpaceHeat public boolean
	public int water_marker; // equivalent to { 0 = no Water Heat, 
							 //	 1 = all year water heat, 
							 //	 2 = summer only water heat}
	
	//Cooking appliance ownership markers.  Assumed boolean i.e. either own 1 or zero
	public boolean hob_marker;
	public boolean oven_marker;
	public boolean microwave_marker;
	public boolean kettle_marker;

	//Cooling appliance ownership markers and quantity
	public boolean fridge_marker;
	public int fridge_num; // Number of fridges (added)
	public boolean ff_marker;
	public int ff_num; // Number of fridge freezers
	public boolean freezer_marker;
	public int freezer_num; // Number of freezers
	
	//Washing appliance ownership markers and quantity
	public boolean wash_marker;
	public int washer_num; // number of washing machines (added)
	public boolean dryer_marker;
	public int dryer_num; // number of tumble dryers (added)
	public boolean dish_marker;
	public int dish_num; // number of dishwashers (added)
	public boolean wash_dry_marker;
	public int wash_dry_num; // number of washer dryers (added)

	//household socio-economic, physical and occupancy characteristics
	public int social_num; // ACORN class - A,B,C1,C2,D and E represented as 1,2,3,4,5,6
	public int floor_area; // floor area of property in m^2
	public int num_occ;  // number of occupants
	public int income_num; // an index indicating income bracket - again - discern how this is populated
	
	/**
	 * null constructor - initialise this to having no appliances, no space heat, no water heat etc.
	 * assign default or "don't know" where available for socio-ec status, floor area and occupancy 
	 */
	public MSHouseholdCharacteristics() {
		this.heat_marker = false; 
		this.water_marker= 0;		
		
		this.hob_marker = false;
		this.oven_marker = false;
		this.microwave_marker = false;
		this.kettle_marker = false;

		this.fridge_marker = false;
		this.fridge_num= 0; // Number of fridges (added)
		this.ff_marker = false;
		this.ff_num= 0; // Number of fridge freezers
		this.freezer_marker = false;
		this.freezer_num= 0; // Number of freezers

		this.wash_marker = false;
		this.washer_num= 0; // number of washing machines (added)
		this.dryer_marker = false;
		this.dryer_num= 0; // number of tumble dryers (added)
		this.dish_marker = false;
		this.dish_num= 0; // number of dishwashers (added)
		this.wash_dry_marker = false;
		this.wash_dry_num= 0; // number of washer dryers (added)

		//household socio-economic, physical and occupancy characteristics
		this.social_num = 3; //ACORN class - 3 used as default (C1)
		this.floor_area = 120; // floor area of property in m^2
		this.num_occ = 2;  // mean occupancy = 2.4 so rounded to 2
		this.income_num = 0; // an index indicating income bracket - again - discern how this is populated
	}
}
