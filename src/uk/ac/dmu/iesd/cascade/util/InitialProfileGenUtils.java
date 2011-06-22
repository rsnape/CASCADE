/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Arrays;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.Consts;

/**
 * @author jsnape
 *
 */
public abstract class InitialProfileGenUtils {

	/**
	 * Helper method to use boolean indicators of ownership which simply calls the
	 * method of same name with integer arguments translating true to 1 and false to 0
	 * 
	 * @param numDays
	 * @param fridges
	 * @param fridgeFreezers
	 * @param freezers
	 * @return
	 */
	public static float[] melodyStokesColdApplianceGen(int numDays, boolean fridges, boolean fridgeFreezers, boolean freezers)
	{
		return melodyStokesColdApplianceGen(numDays, fridges ? 1 : 0, freezers ? 1:0, fridgeFreezers ? 1:0);
	}

	
	/**
	 * Java implementation of the matlab code from Melody Stokes' model of
	 * cold appliance demand.  Note that this implementation does not account for leap years and 
	 * simply assumes that days per year or where in the year the simulation starts
	 * In effect, it assumes a 1st January start.
	 * 
	 * TODO: If this is to be used extensively, need to make it sensitive to start date etc.
	 */
	public static float[] melodyStokesColdApplianceGen(int numDays, int fridges, int fridgeFreezers, int freezers)
	{
		float[] d_fridge = new float[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		float[] d_freezer = new float[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		float[] d_fridge_freezer = new float[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		//This sub-model calculates the half-hourly group average domestic electricity demand
		//for cooling

		//this section calculates demand for fridges. The demand is the same basic annual tr} but scaled for each half-hour:
		float[] scale_fridge={0.113f, 0.106f, 0.083f, 0.100f, 0.092f, 0.090f, 0.095f, 0.085f, 0.085f, 0.084f, 0.072f, 0.084f, 0.081f, 0.079f, 0.083f, 0.092f, 0.098f, 0.084f, 0.093f, 0.101f, 0.101f, 0.092f, 0.105f, 0.104f, 0.107f, 0.115f, 0.114f, 0.121f, 0.118f, 0.120f, 0.110f, 0.119f, 0.122f, 0.118f, 0.119f, 0.119f, 0.129f, 0.124f, 0.122f, 0.116f, 0.107f, 0.108f, 0.100f, 0.104f, 0.107f, 0.110f, 0.110f, 0.100f};
		float[] phase_fridge={1.9f, 1.9f, 1.9f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.1f, 2.0f, 2.1f, 2.1f, 2.0f, 2.1f, 2.2f, 2.2f, 2.2f, 2.0f, 2.0f, 2.1f, 2.1f, 2.1f, 2.1f, 2.1f, 2.1f, 2.1f, 2.1f, 2.1f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.1f, 2.1f, 2.1f, 2.0f, 2.1f, 2.1f, 2.1f, 2.1f, 2.0f, 2.0f, 2.0f, 2.0f, 2.1f, 2.1f};
		float[] const_fridge ={0.441f, 0.447f, 0.424f, 0.433f, 0.428f, 0.423f, 0.415f, 0.406f, 0.405f, 0.409f, 0.397f, 0.399f, 0.403f, 0.403f, 0.421f, 0.443f, 0.451f, 0.449f, 0.452f, 0.453f, 0.451f, 0.456f, 0.459f, 0.460f, 0.470f, 0.490f, 0.503f, 0.482f, 0.485f, 0.481f, 0.483f, 0.488f, 0.495f, 0.506f, 0.529f, 0.545f, 0.549f, 0.549f, 0.546f, 0.532f, 0.519f, 0.510f, 0.510f, 0.502f, 0.503f, 0.493f, 0.472f, 0.458f};
		float[] stddev_fridge={0.064f, 0.065f, 0.063f, 0.063f, 0.062f, 0.060f, 0.060f, 0.063f, 0.058f, 0.061f, 0.058f, 0.058f, 0.057f, 0.056f, 0.063f, 0.062f, 0.060f, 0.062f, 0.064f, 0.059f, 0.063f, 0.068f, 0.064f, 0.071f, 0.071f, 0.079f, 0.076f, 0.079f, 0.077f, 0.077f, 0.076f, 0.077f, 0.070f, 0.079f, 0.081f, 0.086f, 0.078f, 0.076f, 0.077f, 0.076f, 0.071f, 0.070f, 0.066f, 0.067f, 0.066f, 0.068f, 0.069f, 0.065f};
		//this section calculates demand for freezers. It introduces a variable constant value as well as a variable std dev for the random element
		float[] scale_freezer={0.12f, 0.1f, 0.1f, 0.11f, 0.1f, 0.11f, 0.1f, 0.11f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.09f, 0.11f, 0.1f, 0.11f, 0.11f, 0.1f, 0.11f, 0.11f, 0.12f, 0.11f, 0.12f, 0.11f, 0.12f, 0.13f, 0.14f, 0.12f, 0.13f, 0.12f, 0.13f, 0.12f, 0.13f, 0.13f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.11f, 0.11f, 0.11f, 0.11f};
		float[] const_freezer={0.658f, 0.66f, 0.643f, 0.652f, 0.65f, 0.643f, 0.644f, 0.64f, 0.637f, 0.632f, 0.631f, 0.631f, 0.629f, 0.625f, 0.631f, 0.633f, 0.636f, 0.638f, 0.643f, 0.65f, 0.65f, 0.657f, 0.659f, 0.669f, 0.674f, 0.682f, 0.688f, 0.69f, 0.689f, 0.686f, 0.692f, 0.693f, 0.696f, 0.698f, 0.703f, 0.71f, 0.713f, 0.707f, 0.709f, 0.699f, 0.698f, 0.695f, 0.69f, 0.685f, 0.681f, 0.676f, 0.671f, 0.67f};
		float[] stddev_freezer={0.054f, 0.052f, 0.052f, 0.053f, 0.055f, 0.056f, 0.051f, 0.053f, 0.051f, 0.055f, 0.053f, 0.048f, 0.055f, 0.051f, 0.048f, 0.055f, 0.051f, 0.054f, 0.052f, 0.052f, 0.053f, 0.051f, 0.056f, 0.051f, 0.051f, 0.055f, 0.054f, 0.058f, 0.056f, 0.056f, 0.055f, 0.059f, 0.054f, 0.058f, 0.053f, 0.055f, 0.056f, 0.057f, 0.057f, 0.055f, 0.059f, 0.058f, 0.053f, 0.058f, 0.053f, 0.052f, 0.054f, 0.054f};
		//this section calculates demand for fridge-freezers.It follows a similar pattern to the freezer model
		//but phase is now also a variable with half-hour
		float[] scale_fridge_freezer={0.11f, 0.1f, 0.1f, 0.1f, 0.11f, 0.11f, 0.1f, 0.11f, 0.1f, 0.1f, 0.1f, 0.1f, 0.11f, 0.11f, 0.11f, 0.11f, 0.1f, 0.11f, 0.1f, 0.1f, 0.11f, 0.11f, 0.11f, 0.11f, 0.11f, 0.11f, 0.11f, 0.11f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.13f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.11f, 0.12f, 0.11f, 0.11f, 0.12f, 0.11f, 0.11f};
		float[] phase_fridge_freezer={2.25f, 2.25f, 2.25f, 2.39f, 2.44f, 2.42f, 2.42f, 2.45f, 2.43f, 2.43f, 2.43f, 2.49f, 2.42f, 2.45f, 2.37f, 2.35f, 2.34f, 2.39f, 2.38f, 2.35f, 2.41f, 2.37f, 2.38f, 2.34f, 2.35f, 2.28f, 2.29f, 2.28f, 2.28f, 2.28f, 2.25f, 2.28f, 2.26f, 2.27f, 2.3f, 2.25f, 2.26f, 2.24f, 2.29f, 2.3f, 2.27f, 2.36f, 2.32f, 2.31f, 2.42f, 2.4f, 2.4f, 2.36f};
		float[] const_fridge_freezer={0.615f, 0.618f, 0.589f, 0.603f, 0.601f, 0.597f, 0.594f, 0.589f, 0.584f, 0.582f, 0.584f, 0.581f, 0.58f, 0.584f, 0.594f, 0.604f, 0.611f, 0.61f, 0.612f, 0.615f, 0.616f, 0.617f, 0.621f, 0.631f, 0.642f, 0.657f, 0.668f, 0.659f, 0.659f, 0.657f, 0.652f, 0.66f, 0.665f, 0.67f, 0.677f, 0.686f, 0.694f, 0.688f, 0.684f, 0.673f, 0.672f, 0.663f, 0.657f, 0.654f, 0.654f, 0.651f, 0.639f, 0.625f};
		float[] stddev_fridge_freezer={0.05f, 0.049f, 0.052f, 0.049f, 0.046f, 0.047f, 0.049f, 0.048f, 0.046f, 0.049f, 0.043f, 0.046f, 0.047f, 0.048f, 0.048f, 0.048f, 0.047f, 0.047f, 0.045f, 0.047f, 0.047f, 0.051f, 0.048f, 0.053f, 0.051f, 0.052f, 0.056f, 0.056f, 0.054f, 0.056f, 0.056f, 0.055f, 0.057f, 0.055f, 0.057f, 0.055f, 0.054f, 0.056f, 0.054f, 0.053f, 0.049f, 0.051f, 0.049f, 0.053f, 0.049f, 0.052f, 0.048f, 0.048f};
		
		//Initialise a normal distribution for selection
		RandomHelper.createNormal(0, 1);
		
		for (int i=0; i < numDays; i++)
		{
			for (int HH=0; HH < Consts.MELODY_MODELS_TICKS_PER_DAY; HH++)
			{
				d_fridge[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(float) Math.max(0, scale_fridge[HH]*Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH])+const_fridge[HH]+(RandomHelper.getNormal().nextDouble()*stddev_fridge[HH]));
				d_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(float) Math.max(0,scale_freezer[HH]* Math.sin(2*Math.PI*(i / Consts.DAYS_PER_YEAR)-2.05)+const_freezer[HH]+(RandomHelper.getNormal().nextDouble()*stddev_freezer[HH]));
				d_fridge_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(float) Math.max(0,scale_fridge_freezer[HH]* Math.sin(2*Math.PI*(i / Consts.DAYS_PER_YEAR)-phase_fridge_freezer[HH])+const_fridge_freezer[HH]+(RandomHelper.getNormal().nextDouble()*stddev_fridge_freezer[HH]));

			}
		}

	return ArrayUtils.add(d_fridge, d_freezer, d_fridge_freezer);

}

}
