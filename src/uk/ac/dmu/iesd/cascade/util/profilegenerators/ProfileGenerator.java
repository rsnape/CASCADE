package uk.ac.dmu.iesd.cascade.util.profilegenerators;

/**
 * A standardised interface for providing initial demand profiles to the model
 * 
 * @author jsnape
 * 
 */
public interface ProfileGenerator
{

	/*
	 * must return booleans allowing the user to determine which type(s) of
	 * profile this generator is capable of returning.
	 */

	/**
	 * @return true if this generator can produce a profile over all appliances
	 */
	boolean generatesTotalProfile();

	/**
	 * @return true if this generator can produce a profile for wet appliances
	 *         (e.g. washing machine, dishwasher, tumble dryer domestically)
	 */
	boolean generatesWetProfile();

	/**
	 * @return true if this generator can produce a profile over cold appliances
	 *         (e.g. fridge, freezer etc)
	 */
	boolean generatesColdProfile();

	/**
	 * @return true if this generator can produce a profile over "brown"
	 *         appliances (e.g.
	 */
	boolean generatesBrownProfile();

	/**
	 * @return true if this generator can produce a profile of electricity used
	 *         for lighting
	 */
	boolean generatesLightingProfile();

	/**
	 * @return true if this generator can produce a profile of electricity used
	 *         for cooking
	 */
	boolean generatesCookingProfile();

	/**
	 * @return true if this generator can produce a profile of electricity used
	 *         for heating space
	 */
	boolean generatesSpaceHeatProfile();

	/**
	 * @return true if this generator can produce a profile of electricity used
	 *         for heating water
	 */
	boolean generatesWaterHeatProfile();

	/**
	 * @return true if this generator can produce a profile for electricity use
	 *         not covered by any other category
	 */
	boolean generatesMiscProfile();

	/*
	 * These methods return the requested profiles (or null if the implementing
	 * class is not capable of returning them)
	 */

	/**
	 * @return profile over all appliances
	 */
	double[] getTotalProfile();

	/**
	 * @return profile for wet appliances (e.g. washing machine, dishwasher,
	 *         tumble dryer domestically)
	 */
	double[] getWetProfile();

	/**
	 * @return profile over cold appliances (e.g. fridge, freezer etc)
	 */
	double[] getColdProfile();

	/**
	 * @return profile over "brown" appliances (e.g.
	 */
	double[] getBrownProfile();

	/**
	 * @return profile of electricity used for lighting
	 */
	double[] getLightingProfile();

	/**
	 * @return profile of electricity used for cooking
	 */
	double[] getCookingProfile();

	/**
	 * @return profile of electricity used for heating space
	 */
	double[] getSpaceHeatProfile();

	/**
	 * @return profile of electricity used for heating water
	 */
	double[] getWaterHeatProfile();

	/**
	 * @return profile for electricity use not covered by any other category
	 */
	double[] getMiscProfile();

}
