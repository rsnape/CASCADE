/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import java.util.WeakHashMap;

/**
 * 
 * Provide a standardised interface for providing initial demand profiles to the model
 * 
 * @author jsnape
 *
 */
public interface ProfileGenerator {
	
	/*
	 * must return booleans allowing the user to determine which type(s) of profile this
	 * generator is capable of returning.
	 */
	boolean generatesTotalProfile();
	boolean generatesWetProfile();
	boolean generatesColdProfile();
	boolean generatesBrownProfile();
	boolean generatesLightingProfile();
	boolean generatesCookingProfile();
	boolean generatesSpaceHeatProfile();
	boolean generatesWaterHeatProfile();
	boolean generatesMiscProfile();

	/*
	 * These methods return the requested profiles (or null if the implementing class
	 * is not capable of returning them)
	 */
	double[] getTotalProfile();
	double[] getWetProfile();
	double[] getColdProfile();
	double[] getBrownProfile();
	double[] getLightingProfile();
	double[] getCookingProfile();
	double[] getSpaceHeatProfile();
	double[] getWaterHeatProfile();
	double[] getMiscProfile();
	
}
