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
				d_fridge[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=fridges * ((float) Math.max(0, scale_fridge[HH]*Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH])+const_fridge[HH]+(RandomHelper.getNormal().nextDouble()*stddev_fridge[HH])));
				d_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=fridgeFreezers * ((float) Math.max(0,scale_freezer[HH]* Math.sin(2*Math.PI*(i / Consts.DAYS_PER_YEAR)-2.05)+const_freezer[HH]+(RandomHelper.getNormal().nextDouble()*stddev_freezer[HH])));
				d_fridge_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=freezers * ((float) Math.max(0,scale_fridge_freezer[HH]* Math.sin(2*Math.PI*(i / Consts.DAYS_PER_YEAR)-phase_fridge_freezer[HH])+const_fridge_freezer[HH]+(RandomHelper.getNormal().nextDouble()*stddev_fridge_freezer[HH])));

			}
		}

		return ArrayUtils.add(d_fridge, d_freezer, d_fridge_freezer);

	}


	/**
	 * @param daysPerYear
	 * @param hasWashingMachine
	 * @param hasWasherDryer
	 * @param hasDishWasher
	 * @param hasTumbleDryer
	 * @return
	 */
	public static float[] melodyStokesWetApplianceGen(int numDays,
			boolean washMachine, boolean washerDryer,
			boolean dishWasher, boolean tumbleDryer) {
		// TODO Auto-generated method stub
		return 	melodyStokesWetApplianceGen(numDays, washMachine ? 1 : 0, washerDryer ? 1:0, dishWasher ? 1:0, tumbleDryer ? 1:0);

	}


	/**
	 * @param numDays
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 * @return
	 */
	private static float[] melodyStokesWetApplianceGen(int numDays, int washMach,
			int washDry, int dishWash, int tumbleDry) {
		// nasty implementation that assumes this starts 1st Jan and that's a Sunday
		// TODO: refine days of week.  Possibly add start date to context and maintain day of week etc in there too
		//this sub-model is for half-hourly electricty demand for washing appliances
		//it covers washers, dryers, washer-dryers combined and dishwashers
		float[] d_washer_UR = new float[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		float[] d_dryer_UR =new float[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		float[] d_dish_UR =new float[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		//washer parameters for Mondays/Saturdays/Sundays/Weekdays for UR(unrestricted) and E7 tariffs:
		float[] scale_washer_mon_UR={0.004f, 0f, 0.006f, 0.003f, 0.046f, 0.039f, 0.002f, 0.001f, 0.026f, 0.008f, 0.005f, 0.001f, 0.006f, 0.013f, 0.006f, 0.046f, 0.083f, 0.078f, 0.03f, 0.065f, 0.12f, 0.1f, 0.075f, 0.04f, 0.086f, 0.038f, 0.07f, 0.009f, 0.033f, 0.043f, 0.043f, 0.05f, 0.05f, 0.037f, 0.028f, 0.012f, 0.031f, 0.051f, 0.073f, 0.028f, 0.039f, 0.032f, 0.015f, 0.034f, 0.007f, 0.007f, 0.01f, 0.007f};
		float[] phase_washer_mon_UR={2.8f, 0f, 3.6f, 4.6f, 3f, 3.3f, 2.5f, 3.8f, 5.5f, 5.5f, 0.1f, 5f, 0.7f, 2.2f, 0.5f, 0.9f, 0.5f, 0f, 0f, 0.5f, 0.3f, 5.8f, 5.4f, 6.2f, 6.1f, 0.4f, 0.1f, 5.6f, 5.2f, 5.5f, 0.9f, 6.2f, 5.3f, 5f, 5.9f, 4f, 4.4f, 5.5f, 5.7f, 5.2f, 5.2f, 5.6f, 5.6f, 1.1f, 1.8f, 3.9f, 5.6f, 6.2f};
		float[] const_washer_mon_UR={0.009f, 0.007f, 0.009f, 0.005f, 0.041f, 0.036f, 0.005f, 0.004f, 0.016f, 0.005f, 0.005f, 0.008f, 0.024f, 0.02f, 0.102f, 0.122f, 0.263f, 0.394f, 0.417f, 0.368f, 0.385f, 0.37f, 0.347f, 0.292f, 0.281f, 0.246f, 0.209f, 0.165f, 0.164f, 0.176f, 0.158f, 0.155f, 0.164f, 0.148f, 0.139f, 0.128f, 0.131f, 0.12f, 0.157f, 0.119f, 0.091f, 0.086f, 0.083f, 0.059f, 0.038f, 0.027f, 0.03f, 0.013f};
		float[] stddev_washer_mon_UR={0.02f, 0.019f, 0.021f, 0.014f, 0.042f, 0.05f, 0.011f, 0.006f, 0.046f, 0.015f, 0.023f, 0.019f, 0.044f, 0.032f, 0.074f, 0.096f, 0.148f, 0.19f, 0.181f, 0.16f, 0.146f, 0.171f, 0.144f, 0.137f, 0.142f, 0.145f, 0.113f, 0.111f, 0.108f, 0.138f, 0.1f, 0.105f, 0.11f, 0.082f, 0.088f, 0.082f, 0.106f, 0.095f, 0.095f, 0.088f, 0.09f, 0.084f, 0.083f, 0.065f, 0.044f, 0.045f, 0.06f, 0.026f};

		float[] scale_washer_sat_UR={0.05f, 0.012f, 0.007f, 0.008f, 0.013f, 0.007f, 0f, 0f, 0f, 0f, 0f, 0.003f, 0.009f, 0.004f, 0.01f, 0.027f, 0.029f, 0.02f, 0.05f, 0.051f, 0.063f, 0.057f, 0.044f, 0.071f, 0.057f, 0.014f, 0.045f, 0.085f, 0.028f, 0.019f, 0.029f, 0.007f, 0.013f, 0.008f, 0.02f, 0.055f, 0.054f, 0.033f, 0.006f, 0.004f, 0.015f, 0.025f, 0.015f, 0.013f, 0f, 0.011f, 0f, 0.004f};
		float[] 	phase_washer_sat_UR={5f, 4.6f, 5.2f, 4.6f, 2.4f, 2.9f, 0f, 0f, 0f, 0f, 0f, 4.2f, 3.8f, 3.4f, 2.2f, 2.6f, 2.7f, 5.3f, 0.2f, 5.2f, 6f, 5.3f, 4.8f, 5.6f, 0f, 4.8f, 6.1f, 0.2f, 0f, 5f, 3.2f, 1.8f, 0.9f, 1.7f, 4.9f, 0.5f, 0.5f, 0.2f, 6.1f, 0.6f, 5.5f, 6f, 5.4f, 4.3f, 0f, 6f, 0f, 4.1f};
		float[] const_washer_sat_UR={0.024f, 0.017f, 0.012f, 0.008f, 0.014f, 0.014f, 0.005f, 0.003f, 0.006f, 0.005f, 0.007f, 0.012f, 0.023f, 0.028f, 0.03f, 0.109f, 0.159f, 0.203f, 0.31f, 0.372f, 0.43f, 0.444f, 0.391f, 0.38f, 0.359f, 0.326f, 0.286f, 0.284f, 0.279f, 0.251f, 0.21f, 0.212f, 0.186f, 0.146f, 0.124f, 0.12f, 0.12f, 0.117f, 0.111f, 0.087f, 0.072f, 0.071f, 0.066f, 0.047f, 0.05f, 0.037f, 0.043f, 0.024f};
		float[] stddev_washer_sat_UR={0.041f, 0.041f, 0.032f, 0.031f, 0.035f, 0.033f, 0.022f, 0.015f, 0.028f, 0.023f, 0.034f, 0.032f, 0.054f, 0.041f, 0.041f, 0.071f, 0.089f, 0.119f, 0.155f, 0.142f, 0.166f, 0.18f, 0.163f, 0.151f, 0.135f, 0.146f, 0.113f, 0.134f, 0.128f, 0.104f, 0.135f, 0.109f, 0.12f, 0.098f, 0.091f, 0.081f, 0.081f, 0.088f, 0.08f, 0.066f, 0.071f, 0.068f, 0.06f, 0.053f, 0.05f, 0.044f, 0.057f, 0.04f};

		float[] scale_washer_sun_UR={0.016f, 0.011f, 0.012f, 0.009f, 0.036f, 0.032f, 0.004f, 0.012f, 0.033f, 0.01f, 0.016f, 0.008f, 0.016f, 0.009f, 0.007f, 0f, 0.022f, 0.011f, 0.05f, 0.073f, 0.088f, 0.08f, 0.057f, 0.057f, 0.062f, 0.069f, 0.058f, 0.065f, 0.061f, 0.058f, 0.073f, 0.052f, 0.082f, 0.076f, 0.08f, 0.048f, 0.019f, 0.03f, 0.04f, 0.007f, 0.032f, 0.035f, 0.006f, 0.022f, 0.005f, 0.01f, 0.014f, 0.01f};
		float[] phase_washer_sun_UR={4.8f, 5.6f, 5.3f, 3.7f, 2.8f, 3.3f, 3.7f, 4.9f, 5f, 5f, 4.6f, 4.6f, 4.5f, 4.6f, 3.8f, 0f, 2.6f, 1.3f, 1.2f, 0.6f, 0.1f, 5.2f, 5f, 5.7f, 5.7f, 5.4f, 5f, 5.2f, 5.2f, 4.5f, 4.7f, 4.4f, 4.8f, 5f, 5f, 4.6f, 4.3f, 5f, 5.1f, 5.6f, 5.9f, 5.7f, 0.4f, 0.4f, 1.6f, 0f, 0f, 4.8f};
		float[] const_washer_sun_UR={0.024f, 0.017f, 0.018f, 0.022f, 0.04f, 0.032f, 0.01f, 0.012f, 0.019f, 0.006f, 0.01f, 0.005f, 0.013f, 0.021f, 0.021f, 0.032f, 0.055f, 0.096f, 0.184f, 0.251f, 0.348f, 0.406f, 0.358f, 0.323f, 0.316f, 0.267f, 0.268f, 0.193f, 0.22f, 0.198f, 0.201f, 0.187f, 0.181f, 0.189f, 0.154f, 0.146f, 0.119f, 0.12f, 0.122f, 0.122f, 0.09f, 0.079f, 0.085f, 0.074f, 0.055f, 0.035f, 0.031f, 0.025f};
		float[] stddev_washer_sun_UR={0.042f, 0.041f, 0.038f, 0.041f, 0.055f, 0.046f, 0.027f, 0.033f, 0.047f, 0.023f, 0.038f, 0.024f, 0.047f, 0.041f, 0.042f, 0.042f, 0.055f, 0.096f, 0.184f, 0.251f, 0.348f, 0.406f, 0.358f, 0.323f, 0.146f, 0.137f, 0.131f, 0.104f, 0.12f, 0.122f, 0.115f, 0.116f, 0.112f, 0.123f, 0.101f, 0.097f, 0.083f, 0.081f, 0.101f, 0.095f, 0.074f, 0.084f, 0.087f, 0.069f, 0.049f, 0.041f, 0.045f, 0.048f};

		float[] scale_washer_wkdays_UR={0.002f, 0.001f, 0f, 0f, 0.022f, 0.01f, 0f, 0.003f, 0.026f, 0f, 0f, 0.005f, 0.002f, 0.004f, 0.029f, 0.013f, 0.046f, 0.029f, 0.006f, 0.023f, 0.009f, 0.029f, 0.019f, 0.014f, 0.014f, 0.009f, 0.007f, 0.022f, 0.019f, 0.009f, 0.01f, 0.015f, 0.021f, 0.01f, 0.017f, 0.018f, 0.018f, 0.025f, 0.022f, 0.01f, 0.034f, 0.023f, 0.025f, 0.018f, 0.003f, 0.002f, 0.003f, 0.002f};
		float[] phase_washer_wkdays_UR={1.4f, 1.4f, 0f, 0f, 2.4f, 2.6f, 0f, 5f, 5.4f, 0f, 0f, 2.9f, 4.2f, 2.8f, 0.5f, 0.9f, 1f, 6.2f, 2f, 5.9f, 0f, 0f, 5.9f, 0.5f, 4.6f, 4.4f, 5.2f, 0f, 5.5f, 4.9f, 5.8f, 5.4f, 6f, 5.5f, 6f, 6.2f, 6f, 4.9f, 5.3f, 6f, 6f, 0.5f, 0.7f, 0.4f, 2.5f, 1.9f, 0.6f, 0.4f};
		float[] const_washer_wkdays_UR={0.011f, 0.005f, 0.002f, 0.001f, 0.025f, 0.016f, 0.004f, 0.003f, 0.016f, 0.004f, 0.005f, 0.02f, 0.039f, 0.032f, 0.072f, 0.081f, 0.186f, 0.262f, 0.257f, 0.279f, 0.26f, 0.239f, 0.216f, 0.18f, 0.171f, 0.164f, 0.127f, 0.116f, 0.131f, 0.116f, 0.103f, 0.108f, 0.112f, 0.127f, 0.125f, 0.123f, 0.113f, 0.114f, 0.119f, 0.098f, 0.1f, 0.094f, 0.089f, 0.08f, 0.05f, 0.036f, 0.031f, 0.016f};
		float[] stddev_washer_wkdays_UR={0.027f, 0.018f, 0.01f, 0.006f, 0.044f, 0.038f, 0.014f, 0.007f, 0.043f, 0.014f, 0.02f, 0.039f, 0.054f, 0.041f, 0.059f, 0.071f, 0.13f, 0.148f, 0.12f, 0.133f, 0.134f, 0.134f, 0.127f, 0.117f, 0.12f, 0.126f, 0.096f, 0.099f, 0.109f, 0.089f, 0.078f, 0.086f, 0.087f, 0.099f, 0.1f, 0.094f, 0.092f, 0.096f, 0.091f, 0.082f, 0.089f, 0.082f, 0.078f, 0.086f, 0.055f, 0.049f, 0.051f, 0.031f};
		// dryer parameters
		float[] scale_dryer_mon_UR={0.017f, 0.014f, 0.008f, 0f, 0f, 0f, 0f, 0.004f, 0f, 0f, 0.01f, 0.022f, 0.035f, 0.026f, 0.034f, 0.019f, 0.005f, 0.027f, 0.04f, 0.032f, 0.051f, 0.048f, 0.107f, 0.116f, 0.058f, 0.124f, 0.125f, 0.058f, 0.117f, 0.103f, 0.086f, 0.088f, 0.11f, 0.124f, 0.118f, 0.156f, 0.162f, 0.1f, 0.024f, 0.034f, 0.042f, 0.024f, 0.045f, 0.009f, 0.011f, 0.036f, 0.009f, 0.023f};
		float[] phase_dryer_mon_UR={3.7f, 3.5f, 3.8f, 0f, 0f, 0f, 0f, 4f, 0f, 0f, 4.8f, 5.6f, 6.2f, 5.9f, 5.4f, 5.5f, 3.5f, 3.9f, 5.3f, 5.3f, 4.3f, 4.6f, 5.4f, 5.5f, 4.9f, 5.2f, 5.2f, 5.2f, 4.9f, 4.6f, 4.5f, 4.9f, 5.2f, 5.2f, 4.8f, 4.7f, 4.8f, 4.8f, 4.8f, 4.2f, 4.7f, 4.7f, 5.5f, 5.3f, 0.2f, 0.2f, 3.4f, 3.1f};
		float[] const_dryer_mon_UR={0.028f, 0.012f, 0.006f, 0.001f, 0.001f, 0.001f, 0.001f, 0.003f, 0.005f, 0.005f, 0.015f, 0.031f, 0.046f, 0.043f, 0.05f, 0.073f, 0.092f, 0.115f, 0.114f, 0.108f, 0.119f, 0.17f, 0.205f, 0.22f, 0.208f, 0.215f, 0.229f, 0.202f, 0.181f, 0.163f, 0.162f, 0.172f, 0.174f, 0.204f, 0.232f, 0.225f, 0.206f, 0.184f, 0.168f, 0.131f, 0.094f, 0.09f, 0.095f, 0.1f, 0.084f, 0.072f, 0.072f, 0.062f};
		float[] stddev_dryer_mon_UR={0.028f, 0.038f, 0.023f, 0.004f, 0.002f, 0.002f, 0.002f, 0.014f, 0.024f, 0.019f, 0.043f, 0.06f, 0.066f, 0.063f, 0.061f, 0.075f, 0.1f, 0.113f, 0.12f, 0.104f, 0.123f, 0.109f, 0.137f, 0.119f, 0.133f, 0.128f, 0.13f, 0.146f, 0.131f, 0.123f, 0.148f, 0.147f, 0.141f, 0.177f, 0.172f, 0.144f, 0.15f, 0.143f, 0.14f, 0.107f, 0.101f, 0.091f, 0.108f, 0.114f, 0.108f, 0.096f, 0.098f, 0.078f};

		float[] scale_dryer_sat_UR={0.018f, 0.005f, 0.003f, 0.009f, 0.014f, 0.011f, 0.005f, 0f, 0f, 0f, 0.009f, 0.005f, 0.023f, 0.016f, 0.016f, 0.021f, 0.017f, 0.023f, 0.04f, 0.029f, 0.043f, 0.037f, 0.116f, 0.148f, 0.078f, 0.078f, 0.103f, 0.094f, 0.112f, 0.101f, 0.101f, 0.106f, 0.074f, 0.087f, 0.065f, 0.108f, 0.087f, 0.107f, 0.084f, 0.052f, 0.028f, 0.067f, 0.032f, 0.029f, 0.02f, 0.004f, 0.012f, 0.005f};
		float[] phase_dryer_sat_UR={3.9f, 4.5f, 4.4f, 4.7f, 5.4f, 5.4f, 5.4f, 0f, 0f, 0f, 5.3f, 4.7f, 5.5f, 5.9f, 0f, 0.5f, 4.4f, 5.9f, 5.6f, 4.6f, 5.6f, 5.2f, 5.2f, 5.3f, 4.8f, 4.8f, 4.4f, 4.9f, 5f, 4.7f, 4.9f, 4.9f, 4.7f, 4.7f, 5.2f, 5.1f, 4.9f, 4.9f, 5.2f, 5.2f, 6.2f, 6.2f, 5.9f, 3.9f, 4.5f, 3.5f, 3.2f, 4f};
		float[] const_dryer_sat_UR={0.018f, 0.009f, 0.015f, 0.011f, 0.009f, 0.007f, 0.004f, 0.001f, 0.003f, 0.002f, 0.007f, 0.01f, 0.024f, 0.024f, 0.023f, 0.037f, 0.075f, 0.081f, 0.102f, 0.132f, 0.159f, 0.171f, 0.212f, 0.206f, 0.185f, 0.194f, 0.198f, 0.219f, 0.175f, 0.197f, 0.208f, 0.197f, 0.163f, 0.174f, 0.185f, 0.205f, 0.178f, 0.168f, 0.167f, 0.119f, 0.093f, 0.103f, 0.096f, 0.08f, 0.058f, 0.039f, 0.043f, 0.038f};
		float[] stddev_dryer_sat_UR={0.044f, 0.029f, 0.045f, 0.04f, 0.035f, 0.032f, 0.019f, 0.002f, 0.019f, 0.009f, 0.032f, 0.032f, 0.065f, 0.051f, 0.048f, 0.061f, 0.085f, 0.084f, 0.12f, 0.114f, 0.129f, 0.128f, 0.156f, 0.163f, 0.145f, 0.16f, 0.159f, 0.166f, 0.144f, 0.154f, 0.165f, 0.154f, 0.126f, 0.119f, 0.142f, 0.152f, 0.129f, 0.162f, 0.165f, 0.14f, 0.107f, 0.105f, 0.114f, 0.104f, 0.097f, 0.064f, 0.072f, 0.068f};

		float[] scale_dryer_sun_UR={0.013f, 0.006f, 0.003f, 0.006f, 0.005f, 0.003f, 0f, 0f, 0f, 0f, 0f, 0.017f, 0.005f, 0.007f, 0.032f, 0.057f, 0.043f, 0.045f, 0.057f, 0.064f, 0.034f, 0.09f, 0.072f, 0.115f, 0.17f, 0.134f, 0.08f, 0.099f, 0.092f, 0.1f, 0.091f, 0.155f, 0.161f, 0.158f, 0.199f, 0.202f, 0.168f, 0.121f, 0.154f, 0.098f, 0.118f, 0.129f, 0.063f, 0.084f, 0.044f, 0.014f, 0.024f, 0.017f};
		float[] phase_dryer_sun_UR={5.8f, 4.7f, 5.3f, 6.1f, 5.5f, 5.4f, 0f, 0f, 0f, 0f, 0f, 3.3f, 4f, 2.4f, 4.9f, 5.1f, 5.2f, 5.5f, 5.3f, 5.2f, 5.2f, 5.1f, 5.1f, 4.8f, 4.7f, 4.7f, 5f, 4.6f, 4.9f, 4.8f, 4.4f, 4.7f, 4.6f, 4.8f, 5f, 4.8f, 4.7f, 4.7f, 4.7f, 4.5f, 4.7f, 4.8f, 4.3f, 4.1f, 3.6f, 3f, 3.3f, 3.8f};
		float[] const_dryer_sun_UR={0.021f, 0.018f, 0.012f, 0.011f, 0.009f, 0.004f, 0.001f, 0.003f, 0.003f, 0.002f, 0.006f, 0.012f, 0.019f, 0.022f, 0.046f, 0.068f, 0.069f, 0.075f, 0.083f, 0.121f, 0.16f, 0.204f, 0.208f, 0.232f, 0.253f, 0.214f, 0.226f, 0.219f, 0.195f, 0.173f, 0.169f, 0.195f, 0.207f, 0.261f, 0.257f, 0.25f, 0.223f, 0.184f, 0.194f, 0.196f, 0.164f, 0.14f, 0.137f, 0.149f, 0.131f, 0.088f, 0.09f, 0.046f};
		float[] stddev_dryer_sun_UR={0.045f, 0.047f, 0.031f, 0.036f, 0.038f, 0.014f, 0.002f, 0.013f, 0.015f, 0.013f, 0.025f, 0.034f, 0.046f, 0.048f, 0.077f, 0.083f, 0.08f, 0.086f, 0.093f, 0.105f, 0.121f, 0.129f, 0.131f, 0.131f, 0.136f, 0.152f, 0.153f, 0.145f, 0.147f, 0.13f, 0.149f, 0.155f, 0.178f, 0.185f, 0.185f, 0.156f, 0.146f, 0.127f, 0.132f, 0.128f, 0.125f, 0.111f, 0.116f, 0.109f, 0.102f, 0.09f, 0.108f, 0.067f};

		float[] scale_dryer_wkdays_UR={0.01f, 0.005f, 0.005f, 0.004f, 0f, 0.001f, 0.001f, 0f, 0f, 0f, 0f, 0.033f, 0.035f, 0.027f, 0.025f, 0.028f, 0.03f, 0.033f, 0.05f, 0.06f, 0.075f, 0.077f, 0.069f, 0.08f, 0.088f, 0.104f, 0.095f, 0.08f, 0.09f, 0.082f, 0.068f, 0.099f, 0.094f, 0.109f, 0.093f, 0.086f, 0.081f, 0.046f, 0.019f, 0.024f, 0.038f, 0.027f, 0.01f, 0.004f, 0.01f, 0.013f, 0.02f, 0.018f};
		float[] phase_dryer_wkdays_UR={2.8f, 3.5f, 3.4f, 4.3f, 0f, 3.7f, 3.7f, 0f, 0f, 0f, 3.7f, 5.6f, 5.6f, 5.5f, 5.7f, 6.1f, 5.9f, 4.7f, 5.1f, 4.9f, 5f, 5f, 5f, 5.2f, 5.2f, 5.3f, 5.1f, 5.1f, 5f, 4.8f, 5.1f, 5.2f, 4.9f, 5.2f, 5.3f, 5.4f, 5.1f, 5.3f, 5.3f, 0.1f, 0.1f, 0.1f, 0.6f, 5.2f, 3.1f, 2.2f, 2.5f, 2.6f};
		float[] const_dryer_wkdays_UR={0.028f, 0.017f, 0.01f, 0.006f, 0.007f, 0.002f, 0.002f, 0.003f, 0.002f, 0.004f, 0.015f, 0.029f, 0.042f, 0.052f, 0.06f, 0.073f, 0.087f, 0.085f, 0.082f, 0.092f, 0.121f, 0.137f, 0.143f, 0.133f, 0.145f, 0.154f, 0.139f, 0.126f, 0.129f, 0.126f, 0.116f, 0.12f, 0.136f, 0.158f, 0.163f, 0.156f, 0.135f, 0.129f, 0.107f, 0.103f, 0.092f, 0.085f, 0.079f, 0.082f, 0.079f, 0.061f, 0.055f, 0.038f};
		float[] stddev_dryer_wkdays_UR={0.062f, 0.049f, 0.033f, 0.028f, 0.043f, 0.012f, 0.016f, 0.018f, 0.013f, 0.019f, 0.05f, 0.065f, 0.076f, 0.081f, 0.08f, 0.085f, 0.102f, 0.096f, 0.101f, 0.117f, 0.118f, 0.121f, 0.125f, 0.118f, 0.138f, 0.131f, 0.134f, 0.129f, 0.141f, 0.139f, 0.121f, 0.117f, 0.124f, 0.128f, 0.131f, 0.121f, 0.119f, 0.127f, 0.117f, 0.111f, 0.101f, 0.094f, 0.096f, 0.096f, 0.09f, 0.08f, 0.078f, 0.065f};
		//dishwasher parameters
		float[] scale_dish_UR={0.004f, 0.015f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.001f, 0.012f, 0.016f, 0f, 0f, 0.005f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.022f, 0.022f, 0.017f, 0.014f, 0.006f, 0.016f, 0.015f, 0.017f, 0.025f, 0.009f, 0.037f, 0.024f, 0.024f, 0.001f, 0.015f, 0.012f, 0.004f, 0.005f};
		float[] phase_dish_UR={2.6f, 2.4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 1f, 0.5f, 0f, 0f, 3.4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 5.2f, 5.1f, 5.3f, 4.7f, 5.2f, 5f, 5f, 5.5f, 5f, 5.8f, 5.7f, 0f, 0.1f, 0f, 0f, 5.7f, 4.4f, 5.5f};
		float[] const_dish_UR={0.058f, 0.053f, 0.017f, 0.009f, 0.008f, 0.006f, 0.006f, 0.003f, 0.001f, 0.001f, 0.001f, 0.001f, 0.001f, 0.002f, 0.009f, 0.025f, 0.072f, 0.104f, 0.114f, 0.117f, 0.137f, 0.128f, 0.094f, 0.068f, 0.06f, 0.051f, 0.061f, 0.079f, 0.083f, 0.085f, 0.078f, 0.068f, 0.06f, 0.056f, 0.053f, 0.061f, 0.067f, 0.094f, 0.143f, 0.196f, 0.212f, 0.195f, 0.182f, 0.187f, 0.172f, 0.13f, 0.093f, 0.068f};
		float[] stddev_dish_UR={0.071f, 0.053f, 0.036f, 0.032f, 0.03f, 0.023f, 0.025f, 0.016f, 0.007f, 0.006f, 0.011f, 0.013f, 0.007f, 0.015f, 0.034f, 0.053f, 0.094f, 0.114f, 0.112f, 0.106f, 0.113f, 0.11f, 0.102f, 0.088f, 0.087f, 0.073f, 0.082f, 0.098f, 0.102f, 0.112f, 0.101f, 0.094f, 0.088f, 0.081f, 0.086f, 0.095f, 0.091f, 0.095f, 0.133f, 0.146f, 0.14f, 0.134f, 0.141f, 0.132f, 0.125f, 0.111f, 0.091f, 0.08f};

		float[] scale_dish_E7={0.063f, 0.062f, 0.062f, 0.073f, 0.084f, 0.072f, 0.08f, 0.057f, 0.007f, 0.042f, 0.083f, 0.006f, 0f, 0.002f, 0.005f, 0.002f, 0.007f, 0.022f, 0.008f, 0.019f, 0.026f, 0.037f, 0.034f, 0.011f, 0.068f, 0.021f, 0.015f, 0.008f, 0.015f, 0.022f, 0.024f, 0.018f, 0.018f, 0.011f, 0.012f, 0.01f, 0.005f, 0.017f, 0.043f, 0.026f, 0.026f, 0.009f, 0.007f, 0.006f, 0.004f, 0.01f, 0.015f, 0.029f};
		float[] phase_dish_E7={3.8f, 4.2f, 3.8f, 4.4f, 6.1f, 0.3f, 0.9f, 1.4f, 0.2f, 6.1f, 6f, 6f, 0f, 6f, 0.9f, 0f, 5.2f, 4.9f, 4.4f, 4.9f, 4.4f, 4.7f, 4.5f, 4.8f, 4.7f, 5f, 4.8f, 5f, 4.7f, 4.3f, 4.4f, 4.7f, 4.8f, 4.7f, 4.6f, 4.8f, 4.7f, 4.7f, 4.6f, 4.5f, 4.3f, 3.6f, 3.8f, 3.4f, 4.1f, 3.9f, 3.2f, 3.7f};
		float[] const_dish_E7={0.085f, 0.092f, 0.12f, 0.157f, 0.166f, 0.095f, 0.081f, 0.061f, 0.073f, 0.097f, 0.102f, 0.015f, 0.012f, 0.018f, 0.048f, 0.067f, 0.087f, 0.075f, 0.087f, 0.106f, 0.112f, 0.11f, 0.091f, 0.062f, 0.068f, 0.064f, 0.061f, 0.076f, 0.1f, 0.115f, 0.087f, 0.062f, 0.048f, 0.043f, 0.04f, 0.037f, 0.046f, 0.058f, 0.1f, 0.117f, 0.125f, 0.098f, 0.07f, 0.048f, 0.047f, 0.065f, 0.12f, 0.12f};
		float[] stddev_dish_E7={0.128f, 0.125f, 0.119f, 0.15f, 0.146f, 0.108f, 0.118f, 0.094f, 0.097f, 0.109f, 0.11f, 0.05f, 0.051f, 0.061f, 0.109f, 0.123f, 0.134f, 0.121f, 0.141f, 0.151f, 0.151f, 0.145f, 0.141f, 0.107f, 0.125f, 0.124f, 0.112f, 0.126f, 0.15f, 0.17f, 0.144f, 0.127f, 0.114f, 0.097f, 0.094f, 0.089f, 0.1f, 0.112f, 0.149f, 0.155f, 0.152f, 0.138f, 0.124f, 0.106f, 0.102f, 0.12f, 0.146f, 0.151f};

		for (int i = 0; i < numDays; i++)
		{

			//washing demand for Mondays:
			if (i%Consts.DAYS_PER_WEEK == 1)
			{
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]= (washMach + washDry) * (float) Math.max(0, scale_washer_mon_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_mon_UR[HH])+const_washer_mon_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_washer_mon_UR[HH]) ); 
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(tumbleDry + washDry) * (float) Math.max(0, scale_dryer_mon_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_mon_UR[HH])+const_dryer_mon_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_mon_UR[HH])  );
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=dishWash * (float) Math.max(0, scale_dish_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[HH])+const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[HH])  );
				}
			}
			//washing demand for Sundays:
			else if (i%Consts.DAYS_PER_WEEK == 0)
			{
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(washMach + washDry) * (float) Math.max(0, scale_washer_sun_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_sun_UR[HH])+const_washer_sun_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_washer_sun_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(tumbleDry + washDry) * (float) Math.max(0, scale_dryer_sun_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_sun_UR[HH])+const_dryer_sun_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_sun_UR[HH]) );   
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=dishWash * (float) Math.max(0, scale_dish_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[HH])+const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[HH]) ) ;
				}
			}
			//washing demand for Saturdays:
			else if (i%Consts.DAYS_PER_WEEK == 6)
			{
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(washMach + washDry) * (float) Math.max(0, scale_washer_sat_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_sat_UR[HH])+const_washer_sat_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_washer_sat_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(tumbleDry + washDry) * (float) Math.max(0, scale_dryer_sat_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_sat_UR[HH])+const_dryer_sat_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_sat_UR[HH]) );   
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=dishWash * (float) Math.max(0, scale_dish_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[HH])+const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[HH])  );
				}
			}
			else
			{
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(washMach + washDry) * (float) Math.max(0, scale_washer_wkdays_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_wkdays_UR[HH])+const_washer_wkdays_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_washer_wkdays_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(tumbleDry + washDry) * (float) Math.max(0, scale_dryer_wkdays_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_wkdays_UR[HH])+const_dryer_wkdays_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_wkdays_UR[HH])) ;   
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=dishWash * (float) Math.max(0, scale_dish_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[HH])+const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[HH])) ;               
				}
			}
		}

	return ArrayUtils.add(d_washer_UR, d_dryer_UR, d_dish_UR);

}

}
