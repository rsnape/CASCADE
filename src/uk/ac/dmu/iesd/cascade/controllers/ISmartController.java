/**
 * 
 */
package uk.ac.dmu.iesd.cascade.controllers;

import java.util.WeakHashMap;

/**
 * @author jsnape
 *
 */
public interface ISmartController {

	/**
	 * Method to tell the smart controller to gather the data that it needs and
	 * update its internal state.
	 * 
	 * Note - this may include learning algorithms if appropriate
	 */
	public void update(int time);

	/**
	 * Method to extract the current profiles for controlled appliance / device usage
	 * 
	 * @return a WeakHashMap containing whose keys describe the type of load profile and
	 * 			whose values are floating point arrays of the current profile for the day
	 */
	public WeakHashMap<String,double[]> getCurrentProfiles();



}
