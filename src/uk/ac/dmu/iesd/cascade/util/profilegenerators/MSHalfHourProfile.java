/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util.profilegenerators;

/**
 * @author jsnape
 *
 */
public class MSHalfHourProfile{
	
	boolean convertedToKWH = false;

	//Group arrays (before scaling etc.)
	double[] d_lights;
	double[] d_water_E7;
	double[] d_water_UR;
	double[] d_heat;
	double[] d_fridge;
	double[] d_freezer;
	double[] d_fridge_freezer;
	double[] d_washer_E7; 
	double[] d_washer_UR;
	double[] d_dryer_E7; 
	double[] d_dryer_UR;
	double[] d_dish_E7;
	double[] d_dish_UR;
	double[] d_cook;
	double[] d_misc;
	
	//Final grouped arrays (after peak scaling etc)
	double[] D_lights;
	double[] D_water_E7;
	double[] D_water_UR;
	double[] D_heat;
	double[] D_fridge;
	double[] D_freezer;
	double[] D_fridge_freezer;
	double[] D_washer_E7; 
	double[] D_washer_UR;
	double[] D_dryer_E7; 
	double[] D_dryer_UR;
	double[] D_dish_E7;
	double[] D_dish_UR;
	double[] D_hob;
	double[] D_oven;
	double[] D_kettle;
	double[] D_microwave;
	double[] D_misc;
	
	// Final arrays after the "specific" module
	double[] D_HHspecific_hob;
	double[] D_HHspecific_heat;
	double[] D_HHspecific_water;
	double[] D_HHspecific_oven;
	double[] D_HHspecific_microwave;
	double[] D_HHspecific_kettle;
	double[] D_HHspecific_cook;
	double[] D_HHspecific_fridge;
	double[] D_HHspecific_ff;
	double[] D_HHspecific_cold;
	double[] D_HHspecific_freezer;
	double[] D_HHspecific_wash;
	double[] D_HHspecific_dry;
	double[] D_HHspecific_dish;
	double[] D_HHspecific_dryer;
	double[] D_HHspecific_washing;
	double[] D_HHspecific_lights;
	double[] D_HHspecific_misc;
	double[] D_HHspecific_total;
	double[] D_HHreactive_total;
	double[] PF_HH;	
}
