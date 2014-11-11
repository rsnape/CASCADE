package uk.ac.dmu.iesd.cascade.util;

import java.util.Arrays;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * A suite of profile generating methods for various appliance types.
 * 
 * Based almost wholly on the work of Melody Stokes, see her thesis (2004-ish?)
 * 
 * NOTE: Values for this are given in kW. Depending on your end use, these may
 * require conversion to kWh.
 * 
 * @author jsnape
 */
public abstract class InitialProfileGenUtils
{

	/**
	 * Helper method to use boolean indicators of ownership which simply calls
	 * the method of same name with integer arguments translating true to 1 and
	 * false to 0
	 * 
	 * @param numDays
	 * @param fridges
	 * @param fridgeFreezers
	 * @param freezers
	 * @return
	 */
	public static WeakHashMap<String, double[]> melodyStokesColdApplianceGen(int numDays, boolean fridges, boolean fridgeFreezers,
			boolean freezers)
	{
		return InitialProfileGenUtils.melodyStokesColdApplianceGen(numDays, fridges ? 1 : 0, fridgeFreezers ? 1 : 0, freezers ? 1 : 0);
	}

	/**
	 * Java implementation of the matlab code from Melody Stokes' model of cold
	 * appliance demand. Note that this implementation does not account for leap
	 * years and simply assumes that days per year or where in the year the
	 * simulation starts In effect, it assumes a 1st January start.
	 * 
	 * TODO: If this is to be used extensively, need to make it sensitive to
	 * start date etc.
	 * 
	 * NOTE: Values for this are given in kW. Depending on your end use, these
	 * may require conversion to kWh.
	 */
	@Deprecated
	public static double[] melodyStokesColdApplianceGen_Old(int numDays, int fridges, int fridgeFreezers, int freezers)
	{
		double[] d_fridge = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_freezer = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_fridge_freezer = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		// This sub-model calculates the half-hourly group average domestic
		// electricity demand
		// for cooling

		// this section calculates demand for fridges. The demand is the same
		// basic annual tr} but scaled for each half-hour:
		double[] scale_fridge =
		{ 0.113d, 0.106d, 0.083d, 0.100d, 0.092d, 0.090d, 0.095d, 0.085d, 0.085d, 0.084d, 0.072d, 0.084d, 0.081d, 0.079d, 0.083d, 0.092d,
				0.098d, 0.084d, 0.093d, 0.101d, 0.101d, 0.092d, 0.105d, 0.104d, 0.107d, 0.115d, 0.114d, 0.121d, 0.118d, 0.120d, 0.110d,
				0.119d, 0.122d, 0.118d, 0.119d, 0.119d, 0.129d, 0.124d, 0.122d, 0.116d, 0.107d, 0.108d, 0.100d, 0.104d, 0.107d, 0.110d,
				0.110d, 0.100d };
		double[] phase_fridge =
		{ 1.9d, 1.9d, 1.9d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.0d, 2.1d, 2.1d, 2.0d, 2.1d, 2.2d, 2.2d, 2.2d, 2.0d, 2.0d, 2.1d, 2.1d,
				2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.1d, 2.1d, 2.0d, 2.1d, 2.1d, 2.1d,
				2.1d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.1d };
		double[] const_fridge =
		{ 0.441d, 0.447d, 0.424d, 0.433d, 0.428d, 0.423d, 0.415d, 0.406d, 0.405d, 0.409d, 0.397d, 0.399d, 0.403d, 0.403d, 0.421d, 0.443d,
				0.451d, 0.449d, 0.452d, 0.453d, 0.451d, 0.456d, 0.459d, 0.460d, 0.470d, 0.490d, 0.503d, 0.482d, 0.485d, 0.481d, 0.483d,
				0.488d, 0.495d, 0.506d, 0.529d, 0.545d, 0.549d, 0.549d, 0.546d, 0.532d, 0.519d, 0.510d, 0.510d, 0.502d, 0.503d, 0.493d,
				0.472d, 0.458d };
		double[] stddev_fridge =
		{ 0.064d, 0.065d, 0.063d, 0.063d, 0.062d, 0.060d, 0.060d, 0.063d, 0.058d, 0.061d, 0.058d, 0.058d, 0.057d, 0.056d, 0.063d, 0.062d,
				0.060d, 0.062d, 0.064d, 0.059d, 0.063d, 0.068d, 0.064d, 0.071d, 0.071d, 0.079d, 0.076d, 0.079d, 0.077d, 0.077d, 0.076d,
				0.077d, 0.070d, 0.079d, 0.081d, 0.086d, 0.078d, 0.076d, 0.077d, 0.076d, 0.071d, 0.070d, 0.066d, 0.067d, 0.066d, 0.068d,
				0.069d, 0.065d };
		// this section calculates demand for freezers. It introduces a variable
		// constant value as well as a variable std dev for the random element
		double[] scale_freezer =
		{ 0.12d, 0.1d, 0.1d, 0.11d, 0.1d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.09d, 0.11d, 0.1d,
				0.11d, 0.11d, 0.1d, 0.11d, 0.11d, 0.12d, 0.11d, 0.12d, 0.11d, 0.12d, 0.13d, 0.14d, 0.12d, 0.13d, 0.12d, 0.13d, 0.12d,
				0.13d, 0.13d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.11d, 0.11d, 0.11d, 0.11d };
		double[] const_freezer =
		{ 0.658d, 0.66d, 0.643d, 0.652d, 0.65d, 0.643d, 0.644d, 0.64d, 0.637d, 0.632d, 0.631d, 0.631d, 0.629d, 0.625d, 0.631d, 0.633d,
				0.636d, 0.638d, 0.643d, 0.65d, 0.65d, 0.657d, 0.659d, 0.669d, 0.674d, 0.682d, 0.688d, 0.69d, 0.689d, 0.686d, 0.692d,
				0.693d, 0.696d, 0.698d, 0.703d, 0.71d, 0.713d, 0.707d, 0.709d, 0.699d, 0.698d, 0.695d, 0.69d, 0.685d, 0.681d, 0.676d,
				0.671d, 0.67d };
		double[] stddev_freezer =
		{ 0.054d, 0.052d, 0.052d, 0.053d, 0.055d, 0.056d, 0.051d, 0.053d, 0.051d, 0.055d, 0.053d, 0.048d, 0.055d, 0.051d, 0.048d, 0.055d,
				0.051d, 0.054d, 0.052d, 0.052d, 0.053d, 0.051d, 0.056d, 0.051d, 0.051d, 0.055d, 0.054d, 0.058d, 0.056d, 0.056d, 0.055d,
				0.059d, 0.054d, 0.058d, 0.053d, 0.055d, 0.056d, 0.057d, 0.057d, 0.055d, 0.059d, 0.058d, 0.053d, 0.058d, 0.053d, 0.052d,
				0.054d, 0.054d };
		// this section calculates demand for fridge-freezers.It follows a
		// similar pattern to the freezer model
		// but phase is now also a variable with half-hour
		double[] scale_fridge_freezer =
		{ 0.11d, 0.1d, 0.1d, 0.1d, 0.11d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.1d, 0.1d, 0.11d, 0.11d, 0.11d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d,
				0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.13d, 0.12d, 0.12d, 0.12d,
				0.12d, 0.12d, 0.12d, 0.12d, 0.11d, 0.12d, 0.11d, 0.11d, 0.12d, 0.11d, 0.11d };
		double[] phase_fridge_freezer =
		{ 2.25d, 2.25d, 2.25d, 2.39d, 2.44d, 2.42d, 2.42d, 2.45d, 2.43d, 2.43d, 2.43d, 2.49d, 2.42d, 2.45d, 2.37d, 2.35d, 2.34d, 2.39d,
				2.38d, 2.35d, 2.41d, 2.37d, 2.38d, 2.34d, 2.35d, 2.28d, 2.29d, 2.28d, 2.28d, 2.28d, 2.25d, 2.28d, 2.26d, 2.27d, 2.3d,
				2.25d, 2.26d, 2.24d, 2.29d, 2.3d, 2.27d, 2.36d, 2.32d, 2.31d, 2.42d, 2.4d, 2.4d, 2.36d };
		double[] const_fridge_freezer =
		{ 0.615d, 0.618d, 0.589d, 0.603d, 0.601d, 0.597d, 0.594d, 0.589d, 0.584d, 0.582d, 0.584d, 0.581d, 0.58d, 0.584d, 0.594d, 0.604d,
				0.611d, 0.61d, 0.612d, 0.615d, 0.616d, 0.617d, 0.621d, 0.631d, 0.642d, 0.657d, 0.668d, 0.659d, 0.659d, 0.657d, 0.652d,
				0.66d, 0.665d, 0.67d, 0.677d, 0.686d, 0.694d, 0.688d, 0.684d, 0.673d, 0.672d, 0.663d, 0.657d, 0.654d, 0.654d, 0.651d,
				0.639d, 0.625d };
		double[] stddev_fridge_freezer =
		{ 0.05d, 0.049d, 0.052d, 0.049d, 0.046d, 0.047d, 0.049d, 0.048d, 0.046d, 0.049d, 0.043d, 0.046d, 0.047d, 0.048d, 0.048d, 0.048d,
				0.047d, 0.047d, 0.045d, 0.047d, 0.047d, 0.051d, 0.048d, 0.053d, 0.051d, 0.052d, 0.056d, 0.056d, 0.054d, 0.056d, 0.056d,
				0.055d, 0.057d, 0.055d, 0.057d, 0.055d, 0.054d, 0.056d, 0.054d, 0.053d, 0.049d, 0.051d, 0.049d, 0.053d, 0.049d, 0.052d,
				0.048d, 0.048d };

		// Initialise a normal distribution for selection
		RandomHelper.createNormal(0, 1);

		for (int i = 0; i < numDays; i++)
		{
			for (int HH = 0; HH < Consts.MELODY_MODELS_TICKS_PER_DAY; HH++)
			{
				Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH]): "
						+ Math.sin(2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - phase_fridge[HH]));

				d_fridge[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = fridges
						* (Math.max(0, scale_fridge[HH] * Math.sin(2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - phase_fridge[HH])
								+ const_fridge[HH] + (RandomHelper.getNormal().nextDouble() * stddev_fridge[HH])));
				d_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = fridgeFreezers
						* (Math.max(0, scale_freezer[HH] * Math.sin(2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - 2.05) + const_freezer[HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_freezer[HH])));
				d_fridge_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = freezers
						* (Math.max(0, scale_fridge_freezer[HH]
								* Math.sin(2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - phase_fridge_freezer[HH]) + const_fridge_freezer[HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_fridge_freezer[HH])));

			}
		}

		return ArrayUtils.add(d_fridge, d_freezer, d_fridge_freezer);

	}

	/**
	 * Java implementation of the matlab code from Melody Stokes' model of cold
	 * appliance demand. Note that this implementation does not account for leap
	 * years and simply assumes that days per year or where in the year the
	 * simulation starts In effect, it assumes a 1st January start.
	 * 
	 * TODO: If this is to be used extensively, need to make it sensitive to
	 * start date etc.
	 * 
	 * NOTE: Values for this are given in kW. Depending on your end use, these
	 * may require conversion to kWh.
	 */
	public static WeakHashMap<String, double[]> melodyStokesColdApplianceGen(int numDays, int fridges, int fridgeFreezers, int freezers)
	{
		double[] d_fridge = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_freezer = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_fridge_freezer = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		// This sub-model calculates the half-hourly group average domestic
		// electricity demand
		// for cooling

		// this section calculates demand for fridges. The demand is the same
		// basic annual tr} but scaled for each half-hour:
		double[] scale_fridge =
		{ 0.113d, 0.106d, 0.083d, 0.100d, 0.092d, 0.090d, 0.095d, 0.085d, 0.085d, 0.084d, 0.072d, 0.084d, 0.081d, 0.079d, 0.083d, 0.092d,
				0.098d, 0.084d, 0.093d, 0.101d, 0.101d, 0.092d, 0.105d, 0.104d, 0.107d, 0.115d, 0.114d, 0.121d, 0.118d, 0.120d, 0.110d,
				0.119d, 0.122d, 0.118d, 0.119d, 0.119d, 0.129d, 0.124d, 0.122d, 0.116d, 0.107d, 0.108d, 0.100d, 0.104d, 0.107d, 0.110d,
				0.110d, 0.100d };
		double[] phase_fridge =
		{ 1.9d, 1.9d, 1.9d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.0d, 2.1d, 2.1d, 2.0d, 2.1d, 2.2d, 2.2d, 2.2d, 2.0d, 2.0d, 2.1d, 2.1d,
				2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.1d, 2.1d, 2.0d, 2.1d, 2.1d, 2.1d,
				2.1d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.1d };
		double[] const_fridge =
		{ 0.441d, 0.447d, 0.424d, 0.433d, 0.428d, 0.423d, 0.415d, 0.406d, 0.405d, 0.409d, 0.397d, 0.399d, 0.403d, 0.403d, 0.421d, 0.443d,
				0.451d, 0.449d, 0.452d, 0.453d, 0.451d, 0.456d, 0.459d, 0.460d, 0.470d, 0.490d, 0.503d, 0.482d, 0.485d, 0.481d, 0.483d,
				0.488d, 0.495d, 0.506d, 0.529d, 0.545d, 0.549d, 0.549d, 0.546d, 0.532d, 0.519d, 0.510d, 0.510d, 0.502d, 0.503d, 0.493d,
				0.472d, 0.458d };
		double[] stddev_fridge =
		{ 0.064d, 0.065d, 0.063d, 0.063d, 0.062d, 0.060d, 0.060d, 0.063d, 0.058d, 0.061d, 0.058d, 0.058d, 0.057d, 0.056d, 0.063d, 0.062d,
				0.060d, 0.062d, 0.064d, 0.059d, 0.063d, 0.068d, 0.064d, 0.071d, 0.071d, 0.079d, 0.076d, 0.079d, 0.077d, 0.077d, 0.076d,
				0.077d, 0.070d, 0.079d, 0.081d, 0.086d, 0.078d, 0.076d, 0.077d, 0.076d, 0.071d, 0.070d, 0.066d, 0.067d, 0.066d, 0.068d,
				0.069d, 0.065d };
		// this section calculates demand for freezers. It introduces a variable
		// constant value as well as a variable std dev for the random element
		double[] scale_freezer =
		{ 0.12d, 0.1d, 0.1d, 0.11d, 0.1d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.09d, 0.11d, 0.1d,
				0.11d, 0.11d, 0.1d, 0.11d, 0.11d, 0.12d, 0.11d, 0.12d, 0.11d, 0.12d, 0.13d, 0.14d, 0.12d, 0.13d, 0.12d, 0.13d, 0.12d,
				0.13d, 0.13d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.11d, 0.11d, 0.11d, 0.11d };
		double[] const_freezer =
		{ 0.658d, 0.66d, 0.643d, 0.652d, 0.65d, 0.643d, 0.644d, 0.64d, 0.637d, 0.632d, 0.631d, 0.631d, 0.629d, 0.625d, 0.631d, 0.633d,
				0.636d, 0.638d, 0.643d, 0.65d, 0.65d, 0.657d, 0.659d, 0.669d, 0.674d, 0.682d, 0.688d, 0.69d, 0.689d, 0.686d, 0.692d,
				0.693d, 0.696d, 0.698d, 0.703d, 0.71d, 0.713d, 0.707d, 0.709d, 0.699d, 0.698d, 0.695d, 0.69d, 0.685d, 0.681d, 0.676d,
				0.671d, 0.67d };
		double[] stddev_freezer =
		{ 0.054d, 0.052d, 0.052d, 0.053d, 0.055d, 0.056d, 0.051d, 0.053d, 0.051d, 0.055d, 0.053d, 0.048d, 0.055d, 0.051d, 0.048d, 0.055d,
				0.051d, 0.054d, 0.052d, 0.052d, 0.053d, 0.051d, 0.056d, 0.051d, 0.051d, 0.055d, 0.054d, 0.058d, 0.056d, 0.056d, 0.055d,
				0.059d, 0.054d, 0.058d, 0.053d, 0.055d, 0.056d, 0.057d, 0.057d, 0.055d, 0.059d, 0.058d, 0.053d, 0.058d, 0.053d, 0.052d,
				0.054d, 0.054d };
		// this section calculates demand for fridge-freezers.It follows a
		// similar pattern to the freezer model
		// but phase is now also a variable with half-hour
		double[] scale_fridge_freezer =
		{ 0.11d, 0.1d, 0.1d, 0.1d, 0.11d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.1d, 0.1d, 0.11d, 0.11d, 0.11d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d,
				0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.13d, 0.12d, 0.12d, 0.12d,
				0.12d, 0.12d, 0.12d, 0.12d, 0.11d, 0.12d, 0.11d, 0.11d, 0.12d, 0.11d, 0.11d };
		double[] phase_fridge_freezer =
		{ 2.25d, 2.25d, 2.25d, 2.39d, 2.44d, 2.42d, 2.42d, 2.45d, 2.43d, 2.43d, 2.43d, 2.49d, 2.42d, 2.45d, 2.37d, 2.35d, 2.34d, 2.39d,
				2.38d, 2.35d, 2.41d, 2.37d, 2.38d, 2.34d, 2.35d, 2.28d, 2.29d, 2.28d, 2.28d, 2.28d, 2.25d, 2.28d, 2.26d, 2.27d, 2.3d,
				2.25d, 2.26d, 2.24d, 2.29d, 2.3d, 2.27d, 2.36d, 2.32d, 2.31d, 2.42d, 2.4d, 2.4d, 2.36d };
		double[] const_fridge_freezer =
		{ 0.615d, 0.618d, 0.589d, 0.603d, 0.601d, 0.597d, 0.594d, 0.589d, 0.584d, 0.582d, 0.584d, 0.581d, 0.58d, 0.584d, 0.594d, 0.604d,
				0.611d, 0.61d, 0.612d, 0.615d, 0.616d, 0.617d, 0.621d, 0.631d, 0.642d, 0.657d, 0.668d, 0.659d, 0.659d, 0.657d, 0.652d,
				0.66d, 0.665d, 0.67d, 0.677d, 0.686d, 0.694d, 0.688d, 0.684d, 0.673d, 0.672d, 0.663d, 0.657d, 0.654d, 0.654d, 0.651d,
				0.639d, 0.625d };
		double[] stddev_fridge_freezer =
		{ 0.05d, 0.049d, 0.052d, 0.049d, 0.046d, 0.047d, 0.049d, 0.048d, 0.046d, 0.049d, 0.043d, 0.046d, 0.047d, 0.048d, 0.048d, 0.048d,
				0.047d, 0.047d, 0.045d, 0.047d, 0.047d, 0.051d, 0.048d, 0.053d, 0.051d, 0.052d, 0.056d, 0.056d, 0.054d, 0.056d, 0.056d,
				0.055d, 0.057d, 0.055d, 0.057d, 0.055d, 0.054d, 0.056d, 0.054d, 0.053d, 0.049d, 0.051d, 0.049d, 0.053d, 0.049d, 0.052d,
				0.048d, 0.048d };

		// Initialise a normal distribution for selection
		RandomHelper.createNormal(0, 1);

		WeakHashMap<String, double[]> coldProfiles = new WeakHashMap<String, double[]>();

		Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("fridges: " + fridges);
		Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("freezers: " + freezers);
		Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("fridgeFreezers: " + fridgeFreezers);

		for (int i = 0; i < numDays; i++)
		{
			for (int HH = 0; HH < Consts.MELODY_MODELS_TICKS_PER_DAY; HH++)
			{
				Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH]): "
						+ Math.sin(2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - phase_fridge[HH]));
				Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("randomNB: " + RandomHelper.getNormal().nextDouble());

				d_fridge[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = fridges
						* (Math.max(0, scale_fridge[HH] * Math.sin(2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - phase_fridge[HH])
								+ const_fridge[HH] + (RandomHelper.getNormal().nextDouble() * stddev_fridge[HH])));
				d_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = freezers
						* (Math.max(0, scale_freezer[HH] * Math.sin(2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - 2.05) + const_freezer[HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_freezer[HH])));
				d_fridge_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = fridgeFreezers
						* (Math.max(0, scale_fridge_freezer[HH]
								* Math.sin(2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - phase_fridge_freezer[HH]) + const_fridge_freezer[HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_fridge_freezer[HH])));

			}
		}

		d_fridge = ArrayUtils.multiply(d_fridge, Consts.COLD_APP_SCALE_FACTOR_FRIDGE);
		d_freezer = ArrayUtils.multiply(d_freezer, Consts.COLD_APP_SCALE_FACTOR_FREEZER);
		d_fridge_freezer = ArrayUtils.multiply(d_fridge_freezer, Consts.COLD_APP_SCALE_FACTOR_FRIDGEFREEZER);

		// Convert kW to kWh
		d_fridge = InitialProfileGenUtils.convertToKWh(d_fridge, true);
		d_freezer = InitialProfileGenUtils.convertToKWh(d_freezer, true);
		d_fridge_freezer = InitialProfileGenUtils.convertToKWh(d_fridge_freezer, true);

		coldProfiles.put(Consts.COLD_APP_FRIDGE, d_fridge);
		coldProfiles.put(Consts.COLD_APP_FREEZER, d_freezer);
		coldProfiles.put(Consts.COLD_APP_FRIDGEFREEZER, d_fridge_freezer);

		coldProfiles.put(Consts.COLD_APP_FRIDGE_ORIGINAL, Arrays.copyOf(d_fridge, d_fridge.length)); // initial/original
																										// values
		coldProfiles.put(Consts.COLD_APP_FREEZER_ORIGINAL, Arrays.copyOf(d_freezer, d_freezer.length));
		coldProfiles.put(Consts.COLD_APP_FRIDGEFREEZER_ORIGINAL, Arrays.copyOf(d_fridge_freezer, d_fridge_freezer.length));

		Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("d_fridge_freezer: " + Arrays.toString(d_fridge_freezer));
		Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("d_fridge_freezer(sum): " + ArrayUtils.sum(d_fridge_freezer));

		return coldProfiles;

		// return ArrayUtils.add(d_fridge, d_freezer, d_fridge_freezer);
	}

	/**
	 * @param daysPerYear
	 * @param hasWashingMachine
	 * @param hasWasherDryer
	 * @param hasDishWasher
	 * @param hasTumbleDryer
	 * @return
	 * 
	 *         NOTE: Values for this are given in kW. Depending on your end use,
	 *         these may require conversion to kWh.
	 */
	public static WeakHashMap<String, double[]> melodyStokesWetApplianceGen(CascadeContext context, int numDays, boolean washMachine,
			boolean washerDryer, boolean dishWasher, boolean tumbleDryer)
	{
		// TODO Auto-generated method stub
		return InitialProfileGenUtils
				.melodyStokesWetApplianceGenWithWeekends(context, numDays, washMachine ? 1 : 0, washerDryer ? 1 : 0, dishWasher ? 1 : 0, tumbleDryer ? 1
						: 0);
	}

	/**
	 * @param daysPerYear
	 * @param hasWashingMachine
	 * @param hasWasherDryer
	 * @param hasDishWasher
	 * @param hasTumbleDryer
	 * @return
	 * 
	 *         NOTE: Values for this are given in kW. Depending on your end use,
	 *         these may require conversion to kWh.
	 */

	@Deprecated
	public static WeakHashMap<String, double[]> melodyStokesWetApplianceGen_DiscreteValues(CascadeContext context, int numDays,
			boolean washMachine, boolean washerDryer, boolean dishWasher, boolean tumbleDryer)
	{
		// TODO Auto-generated method stub
		return InitialProfileGenUtils
				.melodyStokesWetApplianceGen_DiscreteValues(context, numDays, washMachine ? 1 : 0, washerDryer ? 1 : 0, dishWasher ? 1 : 0, tumbleDryer ? 1
						: 0);
	}

	/**
	 * @param numDays
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 * @return
	 * 
	 *         NOTE: Values for this are given in kW. Depending on your end use,
	 *         these may require conversion to kWh.
	 */
	private static WeakHashMap<String, double[]> melodyStokesWetApplianceGenWithWeekends(CascadeContext context, int numDays, int washMach,
			int washDry, int dishWash, int tumbleDry)
	{
		// nasty implementation that assumes this starts 1st Jan and that's a
		// Sunday
		// TODO: refine days of week. Possibly add start date to context and
		// maintain day of week etc in there too
		// this sub-model is for half-hourly electricty demand for washing
		// appliances
		// it covers washers, dryers, washer-dryers combined and dishwashers
		double[] d_washer_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dryer_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dish_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		// washer parameters for Mondays/Saturdays/Sundays/Weekdays for
		// UR(unrestricted) and E7 tariffs:
		double[] scale_washer_mon_UR =
		{ 0.004d, 0d, 0.006d, 0.003d, 0.046d, 0.039d, 0.002d, 0.001d, 0.026d, 0.008d, 0.005d, 0.001d, 0.006d, 0.013d, 0.006d, 0.046d,
				0.083d, 0.078d, 0.03d, 0.065d, 0.12d, 0.1d, 0.075d, 0.04d, 0.086d, 0.038d, 0.07d, 0.009d, 0.033d, 0.043d, 0.043d, 0.05d,
				0.05d, 0.037d, 0.028d, 0.012d, 0.031d, 0.051d, 0.073d, 0.028d, 0.039d, 0.032d, 0.015d, 0.034d, 0.007d, 0.007d, 0.01d,
				0.007d };
		double[] phase_washer_mon_UR =
		{ 2.8d, 0d, 3.6d, 4.6d, 3d, 3.3d, 2.5d, 3.8d, 5.5d, 5.5d, 0.1d, 5d, 0.7d, 2.2d, 0.5d, 0.9d, 0.5d, 0d, 0d, 0.5d, 0.3d, 5.8d, 5.4d,
				6.2d, 6.1d, 0.4d, 0.1d, 5.6d, 5.2d, 5.5d, 0.9d, 6.2d, 5.3d, 5d, 5.9d, 4d, 4.4d, 5.5d, 5.7d, 5.2d, 5.2d, 5.6d, 5.6d, 1.1d,
				1.8d, 3.9d, 5.6d, 6.2d };
		double[] const_washer_mon_UR =
		{ 0.009d, 0.007d, 0.009d, 0.005d, 0.041d, 0.036d, 0.005d, 0.004d, 0.016d, 0.005d, 0.005d, 0.008d, 0.024d, 0.02d, 0.102d, 0.122d,
				0.263d, 0.394d, 0.417d, 0.368d, 0.385d, 0.37d, 0.347d, 0.292d, 0.281d, 0.246d, 0.209d, 0.165d, 0.164d, 0.176d, 0.158d,
				0.155d, 0.164d, 0.148d, 0.139d, 0.128d, 0.131d, 0.12d, 0.157d, 0.119d, 0.091d, 0.086d, 0.083d, 0.059d, 0.038d, 0.027d,
				0.03d, 0.013d };
		double[] stddev_washer_mon_UR =
		{ 0.02d, 0.019d, 0.021d, 0.014d, 0.042d, 0.05d, 0.011d, 0.006d, 0.046d, 0.015d, 0.023d, 0.019d, 0.044d, 0.032d, 0.074d, 0.096d,
				0.148d, 0.19d, 0.181d, 0.16d, 0.146d, 0.171d, 0.144d, 0.137d, 0.142d, 0.145d, 0.113d, 0.111d, 0.108d, 0.138d, 0.1d, 0.105d,
				0.11d, 0.082d, 0.088d, 0.082d, 0.106d, 0.095d, 0.095d, 0.088d, 0.09d, 0.084d, 0.083d, 0.065d, 0.044d, 0.045d, 0.06d, 0.026d };

		double[] scale_washer_sat_UR =
		{ 0.05d, 0.012d, 0.007d, 0.008d, 0.013d, 0.007d, 0d, 0d, 0d, 0d, 0d, 0.003d, 0.009d, 0.004d, 0.01d, 0.027d, 0.029d, 0.02d, 0.05d,
				0.051d, 0.063d, 0.057d, 0.044d, 0.071d, 0.057d, 0.014d, 0.045d, 0.085d, 0.028d, 0.019d, 0.029d, 0.007d, 0.013d, 0.008d,
				0.02d, 0.055d, 0.054d, 0.033d, 0.006d, 0.004d, 0.015d, 0.025d, 0.015d, 0.013d, 0d, 0.011d, 0d, 0.004d };
		double[] phase_washer_sat_UR =
		{ 5d, 4.6d, 5.2d, 4.6d, 2.4d, 2.9d, 0d, 0d, 0d, 0d, 0d, 4.2d, 3.8d, 3.4d, 2.2d, 2.6d, 2.7d, 5.3d, 0.2d, 5.2d, 6d, 5.3d, 4.8d, 5.6d,
				0d, 4.8d, 6.1d, 0.2d, 0d, 5d, 3.2d, 1.8d, 0.9d, 1.7d, 4.9d, 0.5d, 0.5d, 0.2d, 6.1d, 0.6d, 5.5d, 6d, 5.4d, 4.3d, 0d, 6d, 0d,
				4.1d };
		double[] const_washer_sat_UR =
		{ 0.024d, 0.017d, 0.012d, 0.008d, 0.014d, 0.014d, 0.005d, 0.003d, 0.006d, 0.005d, 0.007d, 0.012d, 0.023d, 0.028d, 0.03d, 0.109d,
				0.159d, 0.203d, 0.31d, 0.372d, 0.43d, 0.444d, 0.391d, 0.38d, 0.359d, 0.326d, 0.286d, 0.284d, 0.279d, 0.251d, 0.21d, 0.212d,
				0.186d, 0.146d, 0.124d, 0.12d, 0.12d, 0.117d, 0.111d, 0.087d, 0.072d, 0.071d, 0.066d, 0.047d, 0.05d, 0.037d, 0.043d, 0.024d };
		double[] stddev_washer_sat_UR =
		{ 0.041d, 0.041d, 0.032d, 0.031d, 0.035d, 0.033d, 0.022d, 0.015d, 0.028d, 0.023d, 0.034d, 0.032d, 0.054d, 0.041d, 0.041d, 0.071d,
				0.089d, 0.119d, 0.155d, 0.142d, 0.166d, 0.18d, 0.163d, 0.151d, 0.135d, 0.146d, 0.113d, 0.134d, 0.128d, 0.104d, 0.135d,
				0.109d, 0.12d, 0.098d, 0.091d, 0.081d, 0.081d, 0.088d, 0.08d, 0.066d, 0.071d, 0.068d, 0.06d, 0.053d, 0.05d, 0.044d, 0.057d,
				0.04d };

		double[] scale_washer_sun_UR =
		{ 0.016d, 0.011d, 0.012d, 0.009d, 0.036d, 0.032d, 0.004d, 0.012d, 0.033d, 0.01d, 0.016d, 0.008d, 0.016d, 0.009d, 0.007d, 0d,
				0.022d, 0.011d, 0.05d, 0.073d, 0.088d, 0.08d, 0.057d, 0.057d, 0.062d, 0.069d, 0.058d, 0.065d, 0.061d, 0.058d, 0.073d,
				0.052d, 0.082d, 0.076d, 0.08d, 0.048d, 0.019d, 0.03d, 0.04d, 0.007d, 0.032d, 0.035d, 0.006d, 0.022d, 0.005d, 0.01d, 0.014d,
				0.01d };
		double[] phase_washer_sun_UR =
		{ 4.8d, 5.6d, 5.3d, 3.7d, 2.8d, 3.3d, 3.7d, 4.9d, 5d, 5d, 4.6d, 4.6d, 4.5d, 4.6d, 3.8d, 0d, 2.6d, 1.3d, 1.2d, 0.6d, 0.1d, 5.2d, 5d,
				5.7d, 5.7d, 5.4d, 5d, 5.2d, 5.2d, 4.5d, 4.7d, 4.4d, 4.8d, 5d, 5d, 4.6d, 4.3d, 5d, 5.1d, 5.6d, 5.9d, 5.7d, 0.4d, 0.4d, 1.6d,
				0d, 0d, 4.8d };
		double[] const_washer_sun_UR =
		{ 0.024d, 0.017d, 0.018d, 0.022d, 0.04d, 0.032d, 0.01d, 0.012d, 0.019d, 0.006d, 0.01d, 0.005d, 0.013d, 0.021d, 0.021d, 0.032d,
				0.055d, 0.096d, 0.184d, 0.251d, 0.348d, 0.406d, 0.358d, 0.323d, 0.316d, 0.267d, 0.268d, 0.193d, 0.22d, 0.198d, 0.201d,
				0.187d, 0.181d, 0.189d, 0.154d, 0.146d, 0.119d, 0.12d, 0.122d, 0.122d, 0.09d, 0.079d, 0.085d, 0.074d, 0.055d, 0.035d,
				0.031d, 0.025d };
		double[] stddev_washer_sun_UR =
		{ 0.042d, 0.041d, 0.038d, 0.041d, 0.055d, 0.046d, 0.027d, 0.033d, 0.047d, 0.023d, 0.038d, 0.024d, 0.047d, 0.041d, 0.042d, 0.042d,
				0.055d, 0.096d, 0.184d, 0.251d, 0.348d, 0.406d, 0.358d, 0.323d, 0.146d, 0.137d, 0.131d, 0.104d, 0.12d, 0.122d, 0.115d,
				0.116d, 0.112d, 0.123d, 0.101d, 0.097d, 0.083d, 0.081d, 0.101d, 0.095d, 0.074d, 0.084d, 0.087d, 0.069d, 0.049d, 0.041d,
				0.045d, 0.048d };

		double[] scale_washer_wkdays_UR =
		{ 0.002d, 0.001d, 0d, 0d, 0.022d, 0.01d, 0d, 0.003d, 0.026d, 0d, 0d, 0.005d, 0.002d, 0.004d, 0.029d, 0.013d, 0.046d, 0.029d,
				0.006d, 0.023d, 0.009d, 0.029d, 0.019d, 0.014d, 0.014d, 0.009d, 0.007d, 0.022d, 0.019d, 0.009d, 0.01d, 0.015d, 0.021d,
				0.01d, 0.017d, 0.018d, 0.018d, 0.025d, 0.022d, 0.01d, 0.034d, 0.023d, 0.025d, 0.018d, 0.003d, 0.002d, 0.003d, 0.002d };
		double[] phase_washer_wkdays_UR =
		{ 1.4d, 1.4d, 0d, 0d, 2.4d, 2.6d, 0d, 5d, 5.4d, 0d, 0d, 2.9d, 4.2d, 2.8d, 0.5d, 0.9d, 1d, 6.2d, 2d, 5.9d, 0d, 0d, 5.9d, 0.5d, 4.6d,
				4.4d, 5.2d, 0d, 5.5d, 4.9d, 5.8d, 5.4d, 6d, 5.5d, 6d, 6.2d, 6d, 4.9d, 5.3d, 6d, 6d, 0.5d, 0.7d, 0.4d, 2.5d, 1.9d, 0.6d,
				0.4d };
		double[] const_washer_wkdays_UR =
		{ 0.011d, 0.005d, 0.002d, 0.001d, 0.025d, 0.016d, 0.004d, 0.003d, 0.016d, 0.004d, 0.005d, 0.02d, 0.039d, 0.032d, 0.072d, 0.081d,
				0.186d, 0.262d, 0.257d, 0.279d, 0.26d, 0.239d, 0.216d, 0.18d, 0.171d, 0.164d, 0.127d, 0.116d, 0.131d, 0.116d, 0.103d,
				0.108d, 0.112d, 0.127d, 0.125d, 0.123d, 0.113d, 0.114d, 0.119d, 0.098d, 0.1d, 0.094d, 0.089d, 0.08d, 0.05d, 0.036d, 0.031d,
				0.016d };
		double[] stddev_washer_wkdays_UR =
		{ 0.027d, 0.018d, 0.01d, 0.006d, 0.044d, 0.038d, 0.014d, 0.007d, 0.043d, 0.014d, 0.02d, 0.039d, 0.054d, 0.041d, 0.059d, 0.071d,
				0.13d, 0.148d, 0.12d, 0.133d, 0.134d, 0.134d, 0.127d, 0.117d, 0.12d, 0.126d, 0.096d, 0.099d, 0.109d, 0.089d, 0.078d,
				0.086d, 0.087d, 0.099d, 0.1d, 0.094d, 0.092d, 0.096d, 0.091d, 0.082d, 0.089d, 0.082d, 0.078d, 0.086d, 0.055d, 0.049d,
				0.051d, 0.031d };
		// dryer parameters
		double[] scale_dryer_mon_UR =
		{ 0.017d, 0.014d, 0.008d, 0d, 0d, 0d, 0d, 0.004d, 0d, 0d, 0.01d, 0.022d, 0.035d, 0.026d, 0.034d, 0.019d, 0.005d, 0.027d, 0.04d,
				0.032d, 0.051d, 0.048d, 0.107d, 0.116d, 0.058d, 0.124d, 0.125d, 0.058d, 0.117d, 0.103d, 0.086d, 0.088d, 0.11d, 0.124d,
				0.118d, 0.156d, 0.162d, 0.1d, 0.024d, 0.034d, 0.042d, 0.024d, 0.045d, 0.009d, 0.011d, 0.036d, 0.009d, 0.023d };
		double[] phase_dryer_mon_UR =
		{ 3.7d, 3.5d, 3.8d, 0d, 0d, 0d, 0d, 4d, 0d, 0d, 4.8d, 5.6d, 6.2d, 5.9d, 5.4d, 5.5d, 3.5d, 3.9d, 5.3d, 5.3d, 4.3d, 4.6d, 5.4d, 5.5d,
				4.9d, 5.2d, 5.2d, 5.2d, 4.9d, 4.6d, 4.5d, 4.9d, 5.2d, 5.2d, 4.8d, 4.7d, 4.8d, 4.8d, 4.8d, 4.2d, 4.7d, 4.7d, 5.5d, 5.3d,
				0.2d, 0.2d, 3.4d, 3.1d };
		double[] const_dryer_mon_UR =
		{ 0.028d, 0.012d, 0.006d, 0.001d, 0.001d, 0.001d, 0.001d, 0.003d, 0.005d, 0.005d, 0.015d, 0.031d, 0.046d, 0.043d, 0.05d, 0.073d,
				0.092d, 0.115d, 0.114d, 0.108d, 0.119d, 0.17d, 0.205d, 0.22d, 0.208d, 0.215d, 0.229d, 0.202d, 0.181d, 0.163d, 0.162d,
				0.172d, 0.174d, 0.204d, 0.232d, 0.225d, 0.206d, 0.184d, 0.168d, 0.131d, 0.094d, 0.09d, 0.095d, 0.1d, 0.084d, 0.072d,
				0.072d, 0.062d };
		double[] stddev_dryer_mon_UR =
		{ 0.028d, 0.038d, 0.023d, 0.004d, 0.002d, 0.002d, 0.002d, 0.014d, 0.024d, 0.019d, 0.043d, 0.06d, 0.066d, 0.063d, 0.061d, 0.075d,
				0.1d, 0.113d, 0.12d, 0.104d, 0.123d, 0.109d, 0.137d, 0.119d, 0.133d, 0.128d, 0.13d, 0.146d, 0.131d, 0.123d, 0.148d, 0.147d,
				0.141d, 0.177d, 0.172d, 0.144d, 0.15d, 0.143d, 0.14d, 0.107d, 0.101d, 0.091d, 0.108d, 0.114d, 0.108d, 0.096d, 0.098d,
				0.078d };

		double[] scale_dryer_sat_UR =
		{ 0.018d, 0.005d, 0.003d, 0.009d, 0.014d, 0.011d, 0.005d, 0d, 0d, 0d, 0.009d, 0.005d, 0.023d, 0.016d, 0.016d, 0.021d, 0.017d,
				0.023d, 0.04d, 0.029d, 0.043d, 0.037d, 0.116d, 0.148d, 0.078d, 0.078d, 0.103d, 0.094d, 0.112d, 0.101d, 0.101d, 0.106d,
				0.074d, 0.087d, 0.065d, 0.108d, 0.087d, 0.107d, 0.084d, 0.052d, 0.028d, 0.067d, 0.032d, 0.029d, 0.02d, 0.004d, 0.012d,
				0.005d };
		double[] phase_dryer_sat_UR =
		{ 3.9d, 4.5d, 4.4d, 4.7d, 5.4d, 5.4d, 5.4d, 0d, 0d, 0d, 5.3d, 4.7d, 5.5d, 5.9d, 0d, 0.5d, 4.4d, 5.9d, 5.6d, 4.6d, 5.6d, 5.2d, 5.2d,
				5.3d, 4.8d, 4.8d, 4.4d, 4.9d, 5d, 4.7d, 4.9d, 4.9d, 4.7d, 4.7d, 5.2d, 5.1d, 4.9d, 4.9d, 5.2d, 5.2d, 6.2d, 6.2d, 5.9d, 3.9d,
				4.5d, 3.5d, 3.2d, 4d };
		double[] const_dryer_sat_UR =
		{ 0.018d, 0.009d, 0.015d, 0.011d, 0.009d, 0.007d, 0.004d, 0.001d, 0.003d, 0.002d, 0.007d, 0.01d, 0.024d, 0.024d, 0.023d, 0.037d,
				0.075d, 0.081d, 0.102d, 0.132d, 0.159d, 0.171d, 0.212d, 0.206d, 0.185d, 0.194d, 0.198d, 0.219d, 0.175d, 0.197d, 0.208d,
				0.197d, 0.163d, 0.174d, 0.185d, 0.205d, 0.178d, 0.168d, 0.167d, 0.119d, 0.093d, 0.103d, 0.096d, 0.08d, 0.058d, 0.039d,
				0.043d, 0.038d };
		double[] stddev_dryer_sat_UR =
		{ 0.044d, 0.029d, 0.045d, 0.04d, 0.035d, 0.032d, 0.019d, 0.002d, 0.019d, 0.009d, 0.032d, 0.032d, 0.065d, 0.051d, 0.048d, 0.061d,
				0.085d, 0.084d, 0.12d, 0.114d, 0.129d, 0.128d, 0.156d, 0.163d, 0.145d, 0.16d, 0.159d, 0.166d, 0.144d, 0.154d, 0.165d,
				0.154d, 0.126d, 0.119d, 0.142d, 0.152d, 0.129d, 0.162d, 0.165d, 0.14d, 0.107d, 0.105d, 0.114d, 0.104d, 0.097d, 0.064d,
				0.072d, 0.068d };

		double[] scale_dryer_sun_UR =
		{ 0.013d, 0.006d, 0.003d, 0.006d, 0.005d, 0.003d, 0d, 0d, 0d, 0d, 0d, 0.017d, 0.005d, 0.007d, 0.032d, 0.057d, 0.043d, 0.045d,
				0.057d, 0.064d, 0.034d, 0.09d, 0.072d, 0.115d, 0.17d, 0.134d, 0.08d, 0.099d, 0.092d, 0.1d, 0.091d, 0.155d, 0.161d, 0.158d,
				0.199d, 0.202d, 0.168d, 0.121d, 0.154d, 0.098d, 0.118d, 0.129d, 0.063d, 0.084d, 0.044d, 0.014d, 0.024d, 0.017d };
		double[] phase_dryer_sun_UR =
		{ 5.8d, 4.7d, 5.3d, 6.1d, 5.5d, 5.4d, 0d, 0d, 0d, 0d, 0d, 3.3d, 4d, 2.4d, 4.9d, 5.1d, 5.2d, 5.5d, 5.3d, 5.2d, 5.2d, 5.1d, 5.1d,
				4.8d, 4.7d, 4.7d, 5d, 4.6d, 4.9d, 4.8d, 4.4d, 4.7d, 4.6d, 4.8d, 5d, 4.8d, 4.7d, 4.7d, 4.7d, 4.5d, 4.7d, 4.8d, 4.3d, 4.1d,
				3.6d, 3d, 3.3d, 3.8d };
		double[] const_dryer_sun_UR =
		{ 0.021d, 0.018d, 0.012d, 0.011d, 0.009d, 0.004d, 0.001d, 0.003d, 0.003d, 0.002d, 0.006d, 0.012d, 0.019d, 0.022d, 0.046d, 0.068d,
				0.069d, 0.075d, 0.083d, 0.121d, 0.16d, 0.204d, 0.208d, 0.232d, 0.253d, 0.214d, 0.226d, 0.219d, 0.195d, 0.173d, 0.169d,
				0.195d, 0.207d, 0.261d, 0.257d, 0.25d, 0.223d, 0.184d, 0.194d, 0.196d, 0.164d, 0.14d, 0.137d, 0.149d, 0.131d, 0.088d,
				0.09d, 0.046d };
		double[] stddev_dryer_sun_UR =
		{ 0.045d, 0.047d, 0.031d, 0.036d, 0.038d, 0.014d, 0.002d, 0.013d, 0.015d, 0.013d, 0.025d, 0.034d, 0.046d, 0.048d, 0.077d, 0.083d,
				0.08d, 0.086d, 0.093d, 0.105d, 0.121d, 0.129d, 0.131d, 0.131d, 0.136d, 0.152d, 0.153d, 0.145d, 0.147d, 0.13d, 0.149d,
				0.155d, 0.178d, 0.185d, 0.185d, 0.156d, 0.146d, 0.127d, 0.132d, 0.128d, 0.125d, 0.111d, 0.116d, 0.109d, 0.102d, 0.09d,
				0.108d, 0.067d };

		double[] scale_dryer_wkdays_UR =
		{ 0.01d, 0.005d, 0.005d, 0.004d, 0d, 0.001d, 0.001d, 0d, 0d, 0d, 0d, 0.033d, 0.035d, 0.027d, 0.025d, 0.028d, 0.03d, 0.033d, 0.05d,
				0.06d, 0.075d, 0.077d, 0.069d, 0.08d, 0.088d, 0.104d, 0.095d, 0.08d, 0.09d, 0.082d, 0.068d, 0.099d, 0.094d, 0.109d, 0.093d,
				0.086d, 0.081d, 0.046d, 0.019d, 0.024d, 0.038d, 0.027d, 0.01d, 0.004d, 0.01d, 0.013d, 0.02d, 0.018d };
		double[] phase_dryer_wkdays_UR =
		{ 2.8d, 3.5d, 3.4d, 4.3d, 0d, 3.7d, 3.7d, 0d, 0d, 0d, 3.7d, 5.6d, 5.6d, 5.5d, 5.7d, 6.1d, 5.9d, 4.7d, 5.1d, 4.9d, 5d, 5d, 5d, 5.2d,
				5.2d, 5.3d, 5.1d, 5.1d, 5d, 4.8d, 5.1d, 5.2d, 4.9d, 5.2d, 5.3d, 5.4d, 5.1d, 5.3d, 5.3d, 0.1d, 0.1d, 0.1d, 0.6d, 5.2d, 3.1d,
				2.2d, 2.5d, 2.6d };
		double[] const_dryer_wkdays_UR =
		{ 0.028d, 0.017d, 0.01d, 0.006d, 0.007d, 0.002d, 0.002d, 0.003d, 0.002d, 0.004d, 0.015d, 0.029d, 0.042d, 0.052d, 0.06d, 0.073d,
				0.087d, 0.085d, 0.082d, 0.092d, 0.121d, 0.137d, 0.143d, 0.133d, 0.145d, 0.154d, 0.139d, 0.126d, 0.129d, 0.126d, 0.116d,
				0.12d, 0.136d, 0.158d, 0.163d, 0.156d, 0.135d, 0.129d, 0.107d, 0.103d, 0.092d, 0.085d, 0.079d, 0.082d, 0.079d, 0.061d,
				0.055d, 0.038d };
		double[] stddev_dryer_wkdays_UR =
		{ 0.062d, 0.049d, 0.033d, 0.028d, 0.043d, 0.012d, 0.016d, 0.018d, 0.013d, 0.019d, 0.05d, 0.065d, 0.076d, 0.081d, 0.08d, 0.085d,
				0.102d, 0.096d, 0.101d, 0.117d, 0.118d, 0.121d, 0.125d, 0.118d, 0.138d, 0.131d, 0.134d, 0.129d, 0.141d, 0.139d, 0.121d,
				0.117d, 0.124d, 0.128d, 0.131d, 0.121d, 0.119d, 0.127d, 0.117d, 0.111d, 0.101d, 0.094d, 0.096d, 0.096d, 0.09d, 0.08d,
				0.078d, 0.065d };
		// dishwasher parameters
		double[] scale_dish_UR =
		{ 0.004d, 0.015d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.001d, 0.012d, 0.016d, 0d, 0d, 0.005d, 0d, 0d, 0d, 0d, 0d, 0d,
				0d, 0d, 0d, 0d, 0.022d, 0.022d, 0.017d, 0.014d, 0.006d, 0.016d, 0.015d, 0.017d, 0.025d, 0.009d, 0.037d, 0.024d, 0.024d,
				0.001d, 0.015d, 0.012d, 0.004d, 0.005d };
		double[] phase_dish_UR =
		{ 2.6d, 2.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 2d, 1d, 0.5d, 0d, 0d, 3.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d,
				5.2d, 5.1d, 5.3d, 4.7d, 5.2d, 5d, 5d, 5.5d, 5d, 5.8d, 5.7d, 0d, 0.1d, 0d, 0d, 5.7d, 4.4d, 5.5d };
		double[] const_dish_UR =
		{ 0.058d, 0.053d, 0.017d, 0.009d, 0.008d, 0.006d, 0.006d, 0.003d, 0.001d, 0.001d, 0.001d, 0.001d, 0.001d, 0.002d, 0.009d, 0.025d,
				0.072d, 0.104d, 0.114d, 0.117d, 0.137d, 0.128d, 0.094d, 0.068d, 0.06d, 0.051d, 0.061d, 0.079d, 0.083d, 0.085d, 0.078d,
				0.068d, 0.06d, 0.056d, 0.053d, 0.061d, 0.067d, 0.094d, 0.143d, 0.196d, 0.212d, 0.195d, 0.182d, 0.187d, 0.172d, 0.13d,
				0.093d, 0.068d };
		double[] stddev_dish_UR =
		{ 0.071d, 0.053d, 0.036d, 0.032d, 0.03d, 0.023d, 0.025d, 0.016d, 0.007d, 0.006d, 0.011d, 0.013d, 0.007d, 0.015d, 0.034d, 0.053d,
				0.094d, 0.114d, 0.112d, 0.106d, 0.113d, 0.11d, 0.102d, 0.088d, 0.087d, 0.073d, 0.082d, 0.098d, 0.102d, 0.112d, 0.101d,
				0.094d, 0.088d, 0.081d, 0.086d, 0.095d, 0.091d, 0.095d, 0.133d, 0.146d, 0.14d, 0.134d, 0.141d, 0.132d, 0.125d, 0.111d,
				0.091d, 0.08d };

		// Initialise a normal distribution for selection
		RandomHelper.createNormal(0, 1);

		for (int i = 0; i < numDays; i++)
		{
			// washing demand for Mondays:
			if (i % Consts.DAYS_PER_WEEK == 1)
			{
				Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("Wet: Monday");
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = (washMach + washDry)
							* Math.max(0, scale_washer_mon_UR[HH]
									* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_washer_mon_UR[HH])
									+ const_washer_mon_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_washer_mon_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = (tumbleDry + washDry)
							* Math.max(0, scale_dryer_mon_UR[HH]
									* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dryer_mon_UR[HH])
									+ const_dryer_mon_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_dryer_mon_UR[HH]));
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = dishWash
							* Math.max(0, scale_dish_UR[HH] * Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dish_UR[HH])
									+ const_dish_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_dish_UR[HH]));
				}
			}
			// washing demand for Sundays:
			else if (i % Consts.DAYS_PER_WEEK == 0)
			{
				Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("Wet: SUNDAY");
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = (washMach + washDry)
							* Math.max(0, scale_washer_sun_UR[HH]
									* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_washer_sun_UR[HH])
									+ const_washer_sun_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_washer_sun_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = (tumbleDry + washDry)
							* Math.max(0, scale_dryer_sun_UR[HH]
									* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dryer_sun_UR[HH])
									+ const_dryer_sun_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_dryer_sun_UR[HH]));
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = dishWash
							* Math.max(0, scale_dish_UR[HH] * Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dish_UR[HH])
									+ const_dish_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_dish_UR[HH]));
				}
			}
			// washing demand for Saturdays:
			else if (i % Consts.DAYS_PER_WEEK == 6)
			{
				Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("Wet: SAT");
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = (washMach + washDry)
							* Math.max(0, scale_washer_sat_UR[HH]
									* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_washer_sat_UR[HH])
									+ const_washer_sat_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_washer_sat_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = (tumbleDry + washDry)
							* Math.max(0, scale_dryer_sat_UR[HH]
									* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dryer_sat_UR[HH])
									+ const_dryer_sat_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_dryer_sat_UR[HH]));
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = dishWash
							* Math.max(0, scale_dish_UR[HH] * Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dish_UR[HH])
									+ const_dish_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_dish_UR[HH]));
				}
			}
			else
			{
				for (int HH = 0; HH < 48; HH++)
				{
					Logger.getLogger(Consts.CASCADE_LOGGER_NAME).trace("Wet: Wkdays");
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = (washMach + washDry)
							* Math.max(0, scale_washer_wkdays_UR[HH]
									* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_washer_wkdays_UR[HH])
									+ const_washer_wkdays_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_washer_wkdays_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = (tumbleDry + washDry)
							* Math.max(0, scale_dryer_wkdays_UR[HH]
									* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dryer_wkdays_UR[HH])
									+ const_dryer_wkdays_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_dryer_wkdays_UR[HH]));
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = dishWash
							* Math.max(0, scale_dish_UR[HH] * Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dish_UR[HH])
									+ const_dish_UR[HH] + (RandomHelper.getNormal().nextDouble() * stddev_dish_UR[HH]));
				}
			}
		}

		d_washer_UR = ArrayUtils.multiply(d_washer_UR, Consts.WET_APP_SCALE_FACTOR_WASHER);
		d_dryer_UR = ArrayUtils.multiply(d_dryer_UR, Consts.WET_APP_SCALE_FACTOR_DRYER);
		d_dish_UR = ArrayUtils.multiply(d_dish_UR, Consts.WET_APP_SCALE_FACTOR_DISH);

		return InitialProfileGenUtils
				.one_min_wash_generate(d_washer_UR, d_dish_UR, d_dryer_UR, numDays, 2010, washMach, tumbleDry, washDry, dishWash);

		/*
		 * WeakHashMap<String,double[]> wetProfiles = new
		 * WeakHashMap<String,double[]>();
		 * 
		 * //Convert kW to kWh d_washer_UR = ArrayUtils.multiply(d_washer_UR,
		 * (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());
		 * d_dryer_UR = ArrayUtils.multiply(d_dryer_UR, (double)
		 * Consts.HOURS_PER_DAY / context.getNbOfTickPerDay()); d_dish_UR =
		 * ArrayUtils.multiply(d_dish_UR, (double) Consts.HOURS_PER_DAY /
		 * context.getNbOfTickPerDay());
		 * 
		 * wetProfiles.put(Consts.WET_APP_WASHER, d_washer_UR);
		 * wetProfiles.put(Consts.WET_APP_DRYER, d_dryer_UR);
		 * wetProfiles.put(Consts.WET_APP_DISHWASHER, d_dish_UR);
		 * 
		 * wetProfiles.put(Consts.WET_APP_WASHER_ORIGINAL,
		 * Arrays.copyOf(d_washer_UR,d_washer_UR.length));
		 * wetProfiles.put(Consts.WET_APP_DRYER_ORIGINAL,
		 * Arrays.copyOf(d_dryer_UR,d_dryer_UR.length));
		 * wetProfiles.put(Consts.WET_APP_DISHWASHER_ORIGINAL,
		 * Arrays.copyOf(d_dish_UR,d_dish_UR.length));
		 * 
		 * //return ArrayUtils.add(d_washer_UR, d_dryer_UR, d_dish_UR); return
		 * wetProfiles;
		 */

	}

	/**
	 * This version modifies the Melody's initial 'continious' function to
	 * 'discrete' loads (Babak Mahdavi)
	 * 
	 * @param numDays
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 * @return
	 * 
	 *         NOTE: Values for this are given in kW. Depending on your end use,
	 *         these may require conversion to kWh.
	 */
	@Deprecated
	private static WeakHashMap<String, double[]> melodyStokesWetApplianceGen_DiscreteValues(CascadeContext context, int numDays,
			int washMach, int washDry, int dishWash, int tumbleDry)
	{
		// Each day of week is treated the same way!
		// this sub-model is for half-hourly electricty demand for washing
		// appliances
		// it covers washers, dryers, washer-dryers combined and dishwashers

		// final double[] wet_pdf =
		// {18.91,16.45,13.49,12.52,16.80,14.41,11.13,9.99,13.90,10.18,13.30,15.53,18.79,17.65,21.79,25.72,36.83,43.13,43.94,46.43,49.61,52.02,49.30,45.71,42.85,42.42,39.08,39.67,41.19,40.16,37.68,37.56,37.67,38.10,38.19,37.10,36.46,37.32,39.44,37.77,37.05,35.09,35.13,34.19,29.75,26.68,26.01,21.30};
		// EmpiricalWalker wetApplProbDistGenerator =
		// RandomHelper.createEmpiricalWalker(wet_pdf,
		// Empirical.NO_INTERPOLATION);

		// ChartUtils.testProbabilityDistAndShowHistogram(wetApplProbDistGenerator,
		// 10000, 48); //test to make sure the prob dist generate desired
		// outcomes

		double[] d_washer_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dryer_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dish_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		double[] scale_washer_wkdays_UR =
		{ 0.002d, 0.001d, 0d, 0d, 0.022d, 0.01d, 0d, 0.003d, 0.026d, 0d, 0d, 0.005d, 0.002d, 0.004d, 0.029d, 0.013d, 0.046d, 0.029d,
				0.006d, 0.023d, 0.009d, 0.029d, 0.019d, 0.014d, 0.014d, 0.009d, 0.007d, 0.022d, 0.019d, 0.009d, 0.01d, 0.015d, 0.021d,
				0.01d, 0.017d, 0.018d, 0.018d, 0.025d, 0.022d, 0.01d, 0.034d, 0.023d, 0.025d, 0.018d, 0.003d, 0.002d, 0.003d, 0.002d };
		double[] phase_washer_wkdays_UR =
		{ 1.4d, 1.4d, 0d, 0d, 2.4d, 2.6d, 0d, 5d, 5.4d, 0d, 0d, 2.9d, 4.2d, 2.8d, 0.5d, 0.9d, 1d, 6.2d, 2d, 5.9d, 0d, 0d, 5.9d, 0.5d, 4.6d,
				4.4d, 5.2d, 0d, 5.5d, 4.9d, 5.8d, 5.4d, 6d, 5.5d, 6d, 6.2d, 6d, 4.9d, 5.3d, 6d, 6d, 0.5d, 0.7d, 0.4d, 2.5d, 1.9d, 0.6d,
				0.4d };
		double[] const_washer_wkdays_UR =
		{ 0.011d, 0.005d, 0.002d, 0.001d, 0.025d, 0.016d, 0.004d, 0.003d, 0.016d, 0.004d, 0.005d, 0.02d, 0.039d, 0.032d, 0.072d, 0.081d,
				0.186d, 0.262d, 0.257d, 0.279d, 0.26d, 0.239d, 0.216d, 0.18d, 0.171d, 0.164d, 0.127d, 0.116d, 0.131d, 0.116d, 0.103d,
				0.108d, 0.112d, 0.127d, 0.125d, 0.123d, 0.113d, 0.114d, 0.119d, 0.098d, 0.1d, 0.094d, 0.089d, 0.08d, 0.05d, 0.036d, 0.031d,
				0.016d };
		double[] stddev_washer_wkdays_UR =
		{ 0.027d, 0.018d, 0.01d, 0.006d, 0.044d, 0.038d, 0.014d, 0.007d, 0.043d, 0.014d, 0.02d, 0.039d, 0.054d, 0.041d, 0.059d, 0.071d,
				0.13d, 0.148d, 0.12d, 0.133d, 0.134d, 0.134d, 0.127d, 0.117d, 0.12d, 0.126d, 0.096d, 0.099d, 0.109d, 0.089d, 0.078d,
				0.086d, 0.087d, 0.099d, 0.1d, 0.094d, 0.092d, 0.096d, 0.091d, 0.082d, 0.089d, 0.082d, 0.078d, 0.086d, 0.055d, 0.049d,
				0.051d, 0.031d };

		double[] scale_dryer_wkdays_UR =
		{ 0.01d, 0.005d, 0.005d, 0.004d, 0d, 0.001d, 0.001d, 0d, 0d, 0d, 0d, 0.033d, 0.035d, 0.027d, 0.025d, 0.028d, 0.03d, 0.033d, 0.05d,
				0.06d, 0.075d, 0.077d, 0.069d, 0.08d, 0.088d, 0.104d, 0.095d, 0.08d, 0.09d, 0.082d, 0.068d, 0.099d, 0.094d, 0.109d, 0.093d,
				0.086d, 0.081d, 0.046d, 0.019d, 0.024d, 0.038d, 0.027d, 0.01d, 0.004d, 0.01d, 0.013d, 0.02d, 0.018d };
		double[] phase_dryer_wkdays_UR =
		{ 2.8d, 3.5d, 3.4d, 4.3d, 0d, 3.7d, 3.7d, 0d, 0d, 0d, 3.7d, 5.6d, 5.6d, 5.5d, 5.7d, 6.1d, 5.9d, 4.7d, 5.1d, 4.9d, 5d, 5d, 5d, 5.2d,
				5.2d, 5.3d, 5.1d, 5.1d, 5d, 4.8d, 5.1d, 5.2d, 4.9d, 5.2d, 5.3d, 5.4d, 5.1d, 5.3d, 5.3d, 0.1d, 0.1d, 0.1d, 0.6d, 5.2d, 3.1d,
				2.2d, 2.5d, 2.6d };
		double[] const_dryer_wkdays_UR =
		{ 0.028d, 0.017d, 0.01d, 0.006d, 0.007d, 0.002d, 0.002d, 0.003d, 0.002d, 0.004d, 0.015d, 0.029d, 0.042d, 0.052d, 0.06d, 0.073d,
				0.087d, 0.085d, 0.082d, 0.092d, 0.121d, 0.137d, 0.143d, 0.133d, 0.145d, 0.154d, 0.139d, 0.126d, 0.129d, 0.126d, 0.116d,
				0.12d, 0.136d, 0.158d, 0.163d, 0.156d, 0.135d, 0.129d, 0.107d, 0.103d, 0.092d, 0.085d, 0.079d, 0.082d, 0.079d, 0.061d,
				0.055d, 0.038d };
		double[] stddev_dryer_wkdays_UR =
		{ 0.062d, 0.049d, 0.033d, 0.028d, 0.043d, 0.012d, 0.016d, 0.018d, 0.013d, 0.019d, 0.05d, 0.065d, 0.076d, 0.081d, 0.08d, 0.085d,
				0.102d, 0.096d, 0.101d, 0.117d, 0.118d, 0.121d, 0.125d, 0.118d, 0.138d, 0.131d, 0.134d, 0.129d, 0.141d, 0.139d, 0.121d,
				0.117d, 0.124d, 0.128d, 0.131d, 0.121d, 0.119d, 0.127d, 0.117d, 0.111d, 0.101d, 0.094d, 0.096d, 0.096d, 0.09d, 0.08d,
				0.078d, 0.065d };
		// dishwasher parameters
		double[] scale_dish_UR =
		{ 0.004d, 0.015d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.001d, 0.012d, 0.016d, 0d, 0d, 0.005d, 0d, 0d, 0d, 0d, 0d, 0d,
				0d, 0d, 0d, 0d, 0.022d, 0.022d, 0.017d, 0.014d, 0.006d, 0.016d, 0.015d, 0.017d, 0.025d, 0.009d, 0.037d, 0.024d, 0.024d,
				0.001d, 0.015d, 0.012d, 0.004d, 0.005d };
		double[] phase_dish_UR =
		{ 2.6d, 2.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 2d, 1d, 0.5d, 0d, 0d, 3.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d,
				5.2d, 5.1d, 5.3d, 4.7d, 5.2d, 5d, 5d, 5.5d, 5d, 5.8d, 5.7d, 0d, 0.1d, 0d, 0d, 5.7d, 4.4d, 5.5d };
		double[] const_dish_UR =
		{ 0.058d, 0.053d, 0.017d, 0.009d, 0.008d, 0.006d, 0.006d, 0.003d, 0.001d, 0.001d, 0.001d, 0.001d, 0.001d, 0.002d, 0.009d, 0.025d,
				0.072d, 0.104d, 0.114d, 0.117d, 0.137d, 0.128d, 0.094d, 0.068d, 0.06d, 0.051d, 0.061d, 0.079d, 0.083d, 0.085d, 0.078d,
				0.068d, 0.06d, 0.056d, 0.053d, 0.061d, 0.067d, 0.094d, 0.143d, 0.196d, 0.212d, 0.195d, 0.182d, 0.187d, 0.172d, 0.13d,
				0.093d, 0.068d };
		double[] stddev_dish_UR =
		{ 0.071d, 0.053d, 0.036d, 0.032d, 0.03d, 0.023d, 0.025d, 0.016d, 0.007d, 0.006d, 0.011d, 0.013d, 0.007d, 0.015d, 0.034d, 0.053d,
				0.094d, 0.114d, 0.112d, 0.106d, 0.113d, 0.11d, 0.102d, 0.088d, 0.087d, 0.073d, 0.082d, 0.098d, 0.102d, 0.112d, 0.101d,
				0.094d, 0.088d, 0.081d, 0.086d, 0.095d, 0.091d, 0.095d, 0.133d, 0.146d, 0.14d, 0.134d, 0.141d, 0.132d, 0.125d, 0.111d,
				0.091d, 0.08d };

		int timeslot;

		WeakHashMap<String, double[]> wetProfiles = new WeakHashMap<String, double[]>();

		for (int i = 0; i < numDays; i++)
		{
			timeslot = context.wetApplProbDistGenerator.nextInt();
			d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + timeslot] = (washMach + washDry)
					* Math.max(0, scale_washer_wkdays_UR[timeslot]
							* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_washer_wkdays_UR[timeslot])
							+ const_washer_wkdays_UR[timeslot]
							+ (RandomHelper.getNormal().nextDouble() * stddev_washer_wkdays_UR[timeslot]));
			d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + timeslot] = (tumbleDry + washDry)
					* Math.max(0, scale_dryer_wkdays_UR[timeslot]
							* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dryer_wkdays_UR[timeslot])
							+ const_dryer_wkdays_UR[timeslot] + (RandomHelper.getNormal().nextDouble() * stddev_dryer_wkdays_UR[timeslot]));
			timeslot = context.wetApplProbDistGenerator.nextInt();
			d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY + timeslot] = dishWash
					* Math.max(0, scale_dish_UR[timeslot] * Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR)) - phase_dish_UR[timeslot])
							+ const_dish_UR[timeslot] + (RandomHelper.getNormal().nextDouble() * stddev_dish_UR[timeslot]));
		}

		// Convert kW to kWh
		d_washer_UR = ArrayUtils.multiply(d_washer_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());
		d_dryer_UR = ArrayUtils.multiply(d_dryer_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());
		d_dish_UR = ArrayUtils.multiply(d_dish_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());

		wetProfiles.put(Consts.WET_APP_WASHER, d_washer_UR);
		wetProfiles.put(Consts.WET_APP_DRYER, d_dryer_UR);
		wetProfiles.put(Consts.WET_APP_DISHWASHER, d_dish_UR);

		wetProfiles.put(Consts.WET_APP_WASHER_ORIGINAL, Arrays.copyOf(d_washer_UR, d_washer_UR.length));
		wetProfiles.put(Consts.WET_APP_DRYER_ORIGINAL, Arrays.copyOf(d_dryer_UR, d_dryer_UR.length));
		wetProfiles.put(Consts.WET_APP_DISHWASHER_ORIGINAL, Arrays.copyOf(d_dish_UR, d_dish_UR.length));

		return wetProfiles;

	}

	/**
	 * Translation from Matlab - Melody Stokes' cooking demand model (domestic
	 * section only)
	 * 
	 * This sub-model calculates the half-hourly group average domestic
	 * electricity demand for cooking TODO: This currently always starts on a
	 * Sunday and doesn't deal with day of the year very well - can be improved.
	 * 
	 * @param numDays
	 * @return
	 * 
	 *         NOTE: Values for this are given in kW. Depending on your end use,
	 *         these may require conversion to kWh.
	 */

	public static double[] melodyStokesDomesticCookingLoadGen(int numDays)
	{
		double[] d_cook = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		double[] cooker_scale_wkdays = new double[]
		{ 0, 0.003, 0, 0, 0.001, 0, 0, 0, 0.001, 0.001, 0.001, 0.004, 0.009, 0.007, 0.005, 0, 0, 0, 0.007, 0.006, 0.008, 0.005, 0.011,
				0.026, 0.039, 0.028, 0.031, 0.046, 0.042, 0.039, 0.035, 0.039, 0.041, 0.031, 0.048, 0.022, 0.034, 0.003, 0.02, 0.019,
				0.007, 0.004, 0.004, 0.005, 0.006, 0, 0, 0 };
		double[] cooker_phase_wkdays = new double[]
		{ 0, 4.6, 0, 0, 4.6, 0, 0, 0, 4.6, 4.2, 4.2, 5.7, 5.2, 5.9, 6, 0, 0, 0, 3.3, 3.4, 4.7, 4.3, 4.8, 4.1, 4.2, 4.8, 4.5, 4, 3.9, 3.9,
				4, 4, 4.2, 4.1, 5.8, 5.4, 6.1, 1.3, 3, 2.9, 2.1, 2.9, 1, 0, 6.2, 0, 0, 0 };
		double[] cooker_const_wkdays = new double[]
		{ 0.004, 0.004, 0.003, 0.002, 0.002, 0.002, 0.002, 0.002, 0.002, 0.002, 0.002, 0.005, 0.008, 0.009, 0.016, 0.046, 0.035, 0.039,
				0.04, 0.04, 0.052, 0.06, 0.078, 0.164, 0.226, 0.232, 0.158, 0.095, 0.069, 0.064, 0.069, 0.093, 0.148, 0.228, 0.34, 0.261,
				0.229, 0.182, 0.138, 0.083, 0.056, 0.034, 0.024, 0.021, 0.014, 0.009, 0.006, 0.007 };
		double[] cooker_stddev_wkdays = new double[]
		{ 0.008, 0.011, 0.015, 0.005, 0.005, 0.004, 0.005, 0.005, 0.005, 0.004, 0.005, 0.007, 0.008, 0.012, 0.016, 0.028, 0.032, 0.037,
				0.038, 0.039, 0.054, 0.057, 0.069, 0.099, 0.099, 0.113, 0.09, 0.076, 0.062, 0.062, 0.064, 0.074, 0.081, 0.106, 0.125,
				0.121, 0.118, 0.098, 0.088, 0.066, 0.053, 0.038, 0.03, 0.028, 0.02, 0.014, 0.013, 0.018 };

		double[] cooker_scale_sat = new double[]
		{ 0.004, 0.001, 0.003, 0, 0.007, 0, 0, 0.004, 0.002, 0.002, 0.003, 0, 0.003, 0.002, 0.005, 0.012, 0.013, 0.022, 0.01, 0.02, 0.024,
				0.006, 0.006, 0.017, 0.023, 0.078, 0.046, 0.024, 0.054, 0.032, 0.028, 0.032, 0.045, 0.069, 0.065, 0.024, 0.057, 0.068,
				0.019, 0.027, 0.023, 0.005, 0.006, 0.005, 0.007, 0.007, 0.004, 0 };
		double[] cooker_phase_sat = new double[]
		{ 4.1, 4, 5.7, 0, 0.5, 0, 0, 5.8, 5.8, 5.8, 5.8, 0, 5.9, 5.9, 5.9, 5.8, 5.2, 3.5, 3.5, 3.9, 3.5, 0.1, 1, 3.8, 4.7, 5, 5.2, 4.5,
				4.7, 4.9, 4.1, 4.5, 4.3, 4.8, 4.9, 4.4, 5, 5.7, 0, 0.6, 5.6, 5.3, 5.1, 5.4, 5.8, 4.9, 4.6, 0 };
		double[] cooker_const_sat = new double[]
		{ 0.009, 0.005, 0.004, 0.003, 0.004, 0.002, 0.006, 0.004, 0.002, 0.003, 0.004, 0.003, 0.003, 0.004, 0.012, 0.041, 0.056, 0.059,
				0.06, 0.053, 0.068, 0.079, 0.104, 0.2, 0.266, 0.316, 0.2, 0.103, 0.066, 0.046, 0.049, 0.065, 0.147, 0.204, 0.277, 0.216,
				0.209, 0.215, 0.2, 0.107, 0.056, 0.027, 0.016, 0.013, 0.023, 0.014, 0.01, 0.007 };
		double[] cooker_stddev_sat = new double[]
		{ 0.013, 0.011, 0.008, 0.006, 0.015, 0.004, 0.016, 0.015, 0.005, 0.01, 0.008, 0.007, 0.009, 0.01, 0.014, 0.033, 0.032, 0.036, 0.05,
				0.039, 0.062, 0.065, 0.073, 0.099, 0.097, 0.124, 0.083, 0.065, 0.053, 0.039, 0.048, 0.062, 0.081, 0.099, 0.117, 0.084,
				0.108, 0.092, 0.107, 0.074, 0.055, 0.033, 0.022, 0.015, 0.028, 0.021, 0.018, 0.012 };

		double[] cooker_scale_sun = new double[]
		{ 0.005, 0.005, 0, 0, 0, 0, 0.005, 0, 0, 0, 0, 0, 0.002, 0, 0.007, 0.032, 0.004, 0.012, 0.005, 0.06, 0.133, 0.122, 0.154, 0.178,
				0.148, 0.168, 0.081, 0.057, 0.033, 0.021, 0.04, 0.008, 0.017, 0.044, 0.068, 0.037, 0.004, 0.025, 0.02, 0.005, 0.016, 0.011,
				0.007, 0, 0.015, 0.005, 0.009, 0.007 };
		double[] cooker_phase_sun = new double[]
		{ 0.8, 4.6, 0, 0, 0, 0, 1.6, 0, 0, 0, 0, 0, 2.5, 0, 0.7, 6.2, 0.5, 2.8, 3.2, 3.2, 3.6, 3.8, 3.7, 4, 4.5, 4.8, 4.9, 5.2, 4.8, 5.4,
				3.4, 4.9, 5.1, 4.9, 4.4, 4.8, 6.2, 0.6, 0.9, 2.1, 2.7, 1.7, 0.5, 0, 5.4, 4.7, 4.5, 4.2 };
		double[] cooker_const_sun = new double[]
		{ 0.004, 0.005, 0.003, 0.004, 0.002, 0.001, 0.005, 0.002, 0.001, 0.001, 0.003, 0.002, 0.003, 0.003, 0.011, 0.046, 0.071, 0.066,
				0.1, 0.152, 0.241, 0.232, 0.299, 0.504, 0.585, 0.594, 0.386, 0.256, 0.157, 0.128, 0.138, 0.171, 0.213, 0.261, 0.266, 0.278,
				0.245, 0.189, 0.144, 0.094, 0.049, 0.043, 0.022, 0.019, 0.016, 0.005, 0.009, 0.007 };
		double[] cooker_stddev_sun = new double[]
		{ 0.016, 0.014, 0.008, 0.009, 0.004, 0.003, 0.018, 0.004, 0.002, 0.002, 0.006, 0.004, 0.005, 0.008, 0.015, 0.033, 0.061, 0.055,
				0.071, 0.105, 0.09, 0.081, 0.095, 0.123, 0.144, 0.126, 0.145, 0.105, 0.08, 0.075, 0.078, 0.083, 0.082, 0.091, 0.128, 0.113,
				0.121, 0.102, 0.089, 0.075, 0.051, 0.047, 0.027, 0.026, 0.019, 0.011, 0.017, 0.015 };

		for (int i = 0; i < numDays; i++)
		{
			if (i % Consts.DAYS_PER_WEEK == 0)
			{
				// for ( Sundays:
				for (int HH = 0; HH < 48; HH++)
				{
					d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = cooker_scale_sun[HH]
							* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - cooker_phase_sun[HH])) + cooker_const_sun[HH]
							+ (RandomHelper.getNormal().nextDouble() * cooker_stddev_sun[HH]);
					if (d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] < 0)
					{
						d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = 0;
					}

				}
			}
			else if (i % Consts.DAYS_PER_WEEK == 6)
			{
				// for ( Saturdays:
				for (int HH = 0; HH < 48; HH++)
				{
					d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = cooker_scale_sat[HH]
							* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - cooker_phase_sat[HH])) + cooker_const_sat[HH]
							+ (RandomHelper.getNormal().nextDouble() * cooker_stddev_sat[HH]);
					if (d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] < 0)
					{
						d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = 0;
					}
				}

			}
			else
			{
				// for ( weekdays:

				for (int HH = 0; HH < 48; HH++)
				{
					d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = cooker_scale_wkdays[HH]
							* Math.sin((2 * Math.PI * (i / Consts.DAYS_PER_YEAR) - cooker_phase_wkdays[HH])) + cooker_const_wkdays[HH]
							+ (RandomHelper.getNormal().nextDouble() * cooker_stddev_wkdays[HH]);
					if (d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] < 0)
					{
						d_cook[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = 0;
					}
				}

			}
		}

		return d_cook;
	}

	/**
	 * Translation from Matlab - Melody Stokes' lighting demand model (domestic
	 * section only)
	 * 
	 * TODO: This currently always starts on a Sunday and doesn't deal with day
	 * of the year very well - can be improved.
	 * 
	 * @param numDays
	 * @return
	 * 
	 *         NOTE: Values for this are given in kW. Depending on your end use,
	 *         these may require conversion to kWh.
	 * 
	 *         NOTE TOO: Ignores the BST / GMT issue - this should be addressed
	 *         if used in anger.
	 */
	@SuppressWarnings("unused")
	private static double[] melodyStokesDomesticLightingLoadGen(int numDays)
	{
		// Some sample values for the change from BST to GMT and back again
		// To make this generic, we need to change depending on the year
		// properly.
		int BST_GMT = 84;
		int GMT_BST = 301;

		double[] d_lights = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_lights_sine = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_lights_sine1 = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_lights_sine2 = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		// This module calculates the half-hourly average demands for lighting//
		// Lighting demand parameters for weekdays://
		double[] min_wkdays = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.051, 0.08, 0.095, 0.098, 0.077, 0.064, 0.06, 0.057, 0.057, 0.06, 0.057, 0.054, 0.056,
				0.059, 0.055, 0.054, 0.054, 0.058, 0.064, 0.073, 0.083, 0.087, 0.087, 0.094, 0.115, 0.127, 0.125, 0.149, 0, 0, 0, 0, 0, 0,
				0 };
		double[] max_wkdays = new double[]
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.091, 0.169, 0.318, 0.452, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.805,
				0.826, 0.847, 0.848, 0.839, 0.813, 0.789, 0.782, 0.759, 0.743, 0.706, 0.623, 1, 1 };
		double[] sinescale1_wkdays = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.006, 0.005, 0.009, 0.015, 0.114, 0.035, 0.026, 0.063, 0.048, 0.021, 0.022, 0.031, 0.018, 0.027,
				0.017, 0.009, 0.019, 0.01, 0.013, 0.018, 0.029, 0.054, 0.188, 0.351, 0.607, 0.564, 0.56, 0.476, 0.439, 0.37, 0.256, 0.37,
				0.473, 0.39, 0.375, 0.172, 0.083, 0.049, 0.035 };
		double[] sinephase1_wkdays = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.9, 0.5, 4, 4.2, 4.7, 4.4, 5.8, 4.9, 4.6, 5.6, 5.5, 4.6, 5, 4.7, 4.7, 4.7, 5.6, 6.1, 0, 5.1, 5.8,
				5.1, 4.9, 4.9, 4.9, 4.9, 4.8, 4.7, 4.7, 4.7, 4.7, 4.7, 4.7, 4.7, 4.8, 4.8, 5.2, 5.3, 5.3 };
		double[] sinescale2_wkdays = new double[]
		{ 0.011, 0.004, 0.003, 0.006, 0.008, 0.009, 0.006, 0.005, 0.005, 0.01, 0.013, 0.007, 0.045, 0.03, 0.295, 0.498, 0.453, 0.352,
				0.255, 0.206, 0.182, 0.164, 0.153, 0.159, 0.139, 0.145, 0.146, 0.145, 0.147, 0.153, 0.184, 0.269, 0.347, 0.107, 0.157,
				0.333, 0.554, 0.728, 0.705, 0.46, 0.322, 0.188, 0.322, 0.012, 0, 0, 0, 0 };
		double[] sinephase2_wkdays = new double[]
		{ 5.8, 5.9, 4.3, 3.7, 3.9, 4, 3.8, 3.9, 3.9, 3.6, 3.5, 4.1, 4.7, 4.3, 4.2, 4.1, 4.2, 4.2, 4.1, 4.1, 4.2, 4.1, 4.2, 4.2, 4.2, 4.2,
				4.2, 4.1, 4.2, 4, 4.1, 4.1, 4, 4.4, 4.3, 4.5, 4.5, 4.5, 4.5, 4.5, 4.5, 4.5, 4.7, 3.5, 0, 0, 0, 0 };
		double[] const_wkdays = new double[]
		{ 0.263, 0.186, 0.14, 0.11, 0.092, 0.079, 0.069, 0.065, 0.062, 0.057, 0.058, 0.069, 0.111, 0.149, 0.092, 0.008, -0.028, -0.086,
				-0.039, -0.027, -0.028, -0.022, -0.025, -0.03, -0.009, -0.023, -0.013, -0.007, -0.016, -0.018, -0.042, -0.141, -0.224,
				-0.016, 0.129, 0.192, 0.247, 0.326, 0.459, 0.581, 0.712, 0.823, 0.98, 0.801, 0.697, 0.59, 0.472, 0.366 };
		double[] stddev_min_wkdays = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.013, 0.025, 0.045, 0.139, 0.024, 0.024, 0.027, 0.026, 0.027, 0.029, 0.029, 0.024, 0.03,
				0.032, 0.024, 0.031, 0.025, 0.026, 0.031, 0.04, 0.046, 0.04, 0.038, 0.042, 0.054, 0.069, 0.054, 0.089, 0, 0, 0, 0, 0, 0, 0 };
		double[] stddev_max_wkdays = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.017, 0.035, 0.063, 0.133, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.06,
				0.064, 0.063, 0.06, 0.058, 0.05, 0.053, 0.056, 0.063, 0.044, 0.043, 0.045, 0, 0 };
		double[] stddev_sine_wkdays =
		{ 0.049, 0.04, 0.035, 0.026, 0.022, 0.018, 0.016, 0.014, 0.014, 0.013, 0.014, 0.014, 0.013, 0.026, 0.058, 0.072, 0.065, 0.061,
				0.062, 0.056, 0.053, 0.049, 0.041, 0.04, 0.042, 0.042, 0.041, 0.047, 0.047, 0.053, 0.064, 0.078, 0.082, 0.069, 0.085,
				0.103, 0.092, 0.079, 0.086, 0.079, 0.055, 0.068, 0.055, 0.057, 0.042, 0.042, 0.047, 0.048 };
		// Lighting demand parameters for Saturdays
		double[] min_sat = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.049, 0.054, 0.065, 0.075, 0.077, 0.087, 0.075, 0.069, 0.065, 0.069, 0.062, 0.059, 0.064,
				0.063, 0.052, 0.05, 0.049, 0.045, 0.049, 0.061, 0.069, 0.072, 0.072, 0.086, 0.097, 0.126, 0.141, 0.174, 0, 0, 0, 0, 0, 0, 0 };
		double[] max_sat = new double[]
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.081, 0.102, 0.153, 0.211, 0.275, 0.275, 0.275, 0.275, 0.275, 0.275, 0.275, 0.275, 0.275,
				0.275, 0.275, 0.275, 0.275, 0.275, 0.275, 0.275, 0.275, 0.275, 0.76, 0.789, 0.819, 0.816, 0.797, 0.769, 0.741, 0.719,
				0.703, 0.681, 0.642, 0.605, 0.552, 0.507 };
		double[] sinescale1_sat = new double[]
		{ 0.042, 0.034, 0.026, 0.021, 0.018, 0.014, 0.008, 0.006, 0.005, 0.004, 0.006, 0.007, 0.014, 0.039, 0.097, 0.066, 0.019, 0.041,
				0.037, 0.038, 0.045, 0.057, 0.058, 0.06, 0.056, 0.054, 0.045, 0.044, 0.054, 0.051, 0.092, 0.165, 0.308, 0.605, 0.609, 0.48,
				0.47, 0.63, 0.59, 0.54, 0.55, 0.353, 0.308, 0.261, 0.206, 0.2, 0.189, 0.091 };
		double[] sinephase1_sat = new double[]
		{ 5.7, 5.8, 5.6, 5.4, 5.5, 5.5, 5.9, 5.7, 5.6, 5.5, 5.6, 5.6, 4, 4.3, 4.5, 5.3, 5.6, 4.6, 5.7, 5, 4.6, 4.9, 4.9, 5.2, 5.3, 5.3,
				5.1, 5.1, 5.5, 5.5, 5.4, 5.1, 5, 4.9, 5, 5.1, 5.3, 5.3, 5.6, 5.6, 5.6, 5.6, 5.6, 5.6, 5.6, 5.6, 5.7, 5.6 };
		double[] sinescale2_sat = new double[]
		{ 0.013, 0.011, 0.007, 0.006, 0.009, 0.006, 0.006, 0.003, 0.004, 0.004, 0.009, 0.008, 0.045, 0.078, 287, 0.29, 0.299, 0.254, 0.206,
				0.203, 0.212, 0.09, 0.087, 0.084, 0.087, 0.09, 0.093, 0.093, 0.099, 0.122, 0.126, 0.132, 0.317, 0.107, 0.209, 0.439, 0.66,
				0.89, 1.31, 0.91, 0.76, 0.44, 0.35, 0.247, 0.142, 0.118, 0.107, 0.048 };
		double[] sinephase2_sat = new double[]
		{ 3.9, 3.4, 3.4, 3.4, 3.1, 3.2, 3.5, 3.2, 3.2, 3.4, 3.4, 3.6, 4.7, 4.8, 4.2, 4.1, 4.1, 4.3, 4.1, 4.2, 4.3, 4.2, 4.2, 4.2, 4.2, 4.1,
				4.2, 4.2, 4.2, 4, 4, 4.2, 4.1, 4.4, 4.1, 4.1, 4.1, 4.1, 4.1, 4.1, 4, 4, 4, 4, 4, 3.8, 3.5, 3.3 };
		double[] const_sat = new double[]
		{ 0.312, 0.224, 0.163, 0.128, 0.106, 0.092, 0.081, 0.072, 0.066, 0.063, 0.061, 0.063, 0.097, 0.111, 0.031, 0.004, -0.019, 0.003,
				0.02, 0.01, -0.002, 0.054, 0.053, 0.049, 0.044, 0.044, 0.049, 0.058, 0.051, 0.045, 0.042, 0.032, -0.115, -0.015, 0.104,
				0.156, 0.204, 0.34, 0.348, 0.64, 0.75, 0.701, 0.703, 0.686, 0.669, 0.63, 0.565, 0.442 };
		double[] stddev_min_sat = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.014, 0.016, 0.02, 0.021, 0.024, 0.025, 0.022, 0.023, 0.022, 0.027, 0.028, 0.023, 0.026,
				0.028, 0.019, 0.019, 0.021, 0.019, 0.027, 0.043, 0.036, 0.037, 0.051, 0.044, 0.06, 0.054, 0.065, 0.094, 0, 0, 0, 0, 0, 0, 0 };
		double[] stddev_max_sat = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.019, 0.025, 0.025, 0.037, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032,
				0.032, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032, 0.036, 0.042, 0.05, 0.055, 0.054, 0.05, 0.054, 0.047, 0.049,
				0.049, 0.051, 0.053, 0.056, 0.037 };
		double[] stddev_sine_sat = new double[]
		{ 0.032, 0.03, 0.028, 0.026, 0.024, 0.02, 0.017, 0.016, 0.015, 0.015, 0.014, 0.014, 0.015, 0.012, 0.006, 0.032, 0.044, 0.039,
				0.045, 0.059, 0.049, 0.048, 0.049, 0.039, 0.038, 0.043, 0.056, 0.048, 0.051, 0.052, 0.055, 0.071, 0.072, 0.064, 0.036,
				0.042, 0.043, 0.055, 0.023, 0.042, 0.071, 0.07, 0.068, 0.059, 0.051, 0.038, 0.042, 0.05 };
		// Lighting demand parameters for Sundays
		double[] min_sun = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.061, 0.066, 0.069, 0.072, 0.075, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.06, 0.071,
				0.078, 0.081, 0.085, 0.094, 0.109, 0.14, 0.181, 0.186, 0.195, 0, 0, 0, 0, 0, 0 };
		double[] max_sun = new double[]
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.799, 0.822, 0.819,
				0.822, 0.798, 0.762, 0.747, 0.737, 0.708, 0.66, 1, 1, 1 };
		double[] sinescale1_sun = new double[]
		{ 0.056, 0.046, 0.026, 0.012, 0.008, 0.004, 0.008, 0.004, 0.004, 0.003, 0.001, 0.001, 0.003, 0, 0.017, 0.022, 0.043, 0.056, 0.048,
				0.051, 0.062, 0.083, 0.078, 0.08, 0.078, 0.075, 0.078, 0.074, 0.078, 0.082, 0.088, 0.16, 0.301, 0.477, 0.409, 0.509, 0.421,
				0.657, 0.829, 0.831, 0.525, 0.525, 0.412, 0.06, 0.06, 0.025, 0.027, 0.028 };
		double[] sinephase1_sun = new double[]
		{ 5.7, 5.7, 5.7, 5.7, 5.6, 5.8, 6, 6, 6, 6, 6, 6, 5.9, 0, 5, 5.2, 4.8, 4.9, 4.7, 4.6, 4.5, 4.8, 4.7, 4.8, 4.8, 4.7, 4.8, 4.8, 4.8,
				4.9, 4.9, 5, 5, 4.8, 5, 5, 5.2, 5.2, 5.1, 5.1, 5.6, 5.7, 5.7, 4.9, 4.9, 5.1, 5.5, 5.5 };
		double[] sinescale2_sun = new double[]
		{ 0.021, 0.008, 0.01, 0.01, 0.014, 0.015, 0.013, 0.01, 0.01, 0.01, 0.01, 0.01, 0.015, 0.028, 0.027, 0.046, 0.094, 0.096, 0.102,
				0.107, 0.08, 0.004, 0.004, 0, 0, 0.003, 0.004, 0.016, 0.018, 0.031, 0.043, 0.072, 0.203, 0.12, 0.224, 0.438, 0.699, 0.927,
				0.999, 1.001, 0.722, 0.74, 0.591, 0.3, 0.231, 0.02, 0, 0 };
		double[] sinephase2_sun = new double[]
		{ 3.9, 3.9, 3.9, 3.8, 3.8, 3.9, 3.9, 3.9, 3.9, 3.9, 3.9, 3.9, 4, 4.3, 4.3, 4.2, 4.2, 4, 4, 4, 4.5, 3.3, 3.3, 0, 0, 3.2, 3.2, 3.7,
				3.7, 3.6, 3.6, 3.6, 3.8, 4.4, 3.9, 4.1, 4.1, 4.1, 4.1, 4.1, 4, 4.1, 4.1, 4.8, 4.8, 4.8, 0, 0 };
		double[] const_sun = new double[]
		{ 0.352, 0.267, 0.202, 0.152, 0.12, 0.097, 0.082, 0.075, 0.069, 0.064, 0.061, 0.059, 0.062, 0.071, 0.079, 0.089, 0.073, 0.082,
				0.093, 0.085, 0.094, 0.131, 0.129, 0.126, 0.126, 0.121, 0.129, 0.126, 0.129, 0.131, 0.142, 0.138, 0.05, 0.133, 0.189,
				0.148, 0.153, 0.248, 0.42, 0.588, 0.772, 0.969, 0.972, 0.774, 0.777, 0.545, 0.453, 0.351 };
		double[] stddev_min_sun = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.023, 0.023, 0.026, 0.024, 0.035, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.023, 0.025,
				0.041, 0.036, 0.039, 0.043, 0.055, 0.083, 0.115, 0.157, 0.076, 0, 0, 0, 0, 0, 0 };
		double[] stddev_max_sun = new double[]
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.043, 0.053, 0.049,
				0.054, 0.061, 0.06, 0.072, 0.069, 0.061, 0.045, 0, 0, 0 };
		double[] stddev_sine_sun = new double[]
		{ 0.047, 0.039, 0.037, 0.027, 0.023, 0.022, 0.018, 0.017, 0.016, 0.015, 0.014, 0.015, 0.015, 0.018, 0.022, 0.023, 0.031, 0.035,
				0.043, 0.049, 0.051, 0.039, 0.053, 0.041, 0.048, 0.046, 0.05, 0.046, 0.048, 0.045, 0.058, 0.079, 0.084, 0.075, 0.088,
				0.088, 0.057, 0.05, 0.094, 0.072, 0.089, 0.068, 0.069, 0.053, 0.035, 0.043, 0.051, 0.044 };

		// this section only works for test days in a single year at present
		// checks the weekday type, sets parameter BST (phase shift for autumn
		// clock change to 0
		// checks to see if (date if (before or after clock change in test year;
		// if (it is BST=34
		// for each half-hour, calculates the two annual sine trends and totals
		// them
		// if (the total is under the min level, sets demand to min; if (over
		// max level, sets demand to max
		// adds on a normally distributed random number - mean 0, std dev 1 &
		// scales by appropriate std dev for model
		// sets negative demands to zero

		// domestic consumers - different lighting demands, depending on day
		// type

		for (int i = 0; i < numDays; i++)
		{
			if (i % Consts.DAYS_PER_WEEK != 0)
			{
				// Not Sunday
				if (i % Consts.DAYS_PER_WEEK != 6)
				{
					// Not Saturday either, so this is a weekday
					int BST = 0;
					if (i > BST_GMT)
					{

						BST = 34;
					}
					if (i < GMT_BST)
					{
						BST = 34;
					}

					for (int HH = 0; HH < 48; HH++)
					{
						int n = 0;
						d_lights_sine1[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = sinescale1_sun[HH]
								* Math.sin((2 * Math.PI * ((i + BST) / Consts.DAYS_PER_YEAR) - sinephase1_sun[HH]));
						d_lights_sine2[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = sinescale2_sun[HH]
								* Math.sin((2 * Math.PI * ((i - BST) / Consts.DAYS_PER_YEAR) - sinephase2_sun[HH])) + const_sun[HH];
						d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = d_lights_sine1[i * Consts.MELODY_MODELS_TICKS_PER_DAY
								+ HH]
								+ d_lights_sine2[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH];
						if (d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] <= min_sun[HH])
						{
							d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = min_sun[HH]
									+ (RandomHelper.getNormal().nextDouble() * stddev_min_sun[HH]);
							n = 1;
						}
						if (d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] >= max_sun[HH])
						{
							d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = max_sun[HH]
									+ (RandomHelper.getNormal().nextDouble() * stddev_max_sun[HH]);
							n = 1;
						}
						if (n == 0)
						{
							d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY
									+ HH]
									+ (RandomHelper.getNormal().nextDouble() * stddev_sine_sun[HH]);
						}

						if ((d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] < 0))
						{

							d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = 0;

						}
					}
				}

				int BST = 0;
				if (i > BST_GMT)
				{
					BST = 34;
				}
				if (i < GMT_BST)
				{
					BST = 34;
				}
				for (int HH = 0; HH < 48; HH++)
				{
					int n = 0;
					d_lights_sine1[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = sinescale1_wkdays[HH]
							* Math.sin((2 * Math.PI * ((i + BST) / Consts.DAYS_PER_YEAR) - sinephase1_wkdays[HH]));
					d_lights_sine2[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = sinescale2_wkdays[HH]
							* Math.sin((2 * Math.PI * ((i - BST) / Consts.DAYS_PER_YEAR) - sinephase2_wkdays[HH])) + const_wkdays[HH];
					d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = d_lights_sine1[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH]
							+ d_lights_sine2[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH];
					if (d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] <= min_wkdays[HH])
					{
						d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = min_wkdays[HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_min_wkdays[HH]);
						n = 1;
					}
					if (d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] >= max_wkdays[HH])
					{
						d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = max_wkdays[HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_max_wkdays[HH]);
						n = 1;
					}
					if (n == 0)
					{
						d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_sine_wkdays[HH]);
					}
					if (d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] < 0)
					{
						d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = 0;
					}
				}
			}

			if (i % Consts.DAYS_PER_WEEK == 0)
			{
				// Sunday

				int BST = 0;
				if (i > BST_GMT)
				{
					BST = 34;
				}
				if (i < GMT_BST)
				{
					BST = 34;
				}
				for (int HH = 0; HH < 48; HH++)
				{
					int n = 0;
					d_lights_sine1[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = sinescale1_sat[HH]
							* Math.sin((2 * Math.PI * ((i + BST) / Consts.DAYS_PER_YEAR) - sinephase1_sat[HH]));
					d_lights_sine2[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = sinescale2_sat[HH]
							* Math.sin((2 * Math.PI * ((i - BST) / Consts.DAYS_PER_YEAR) - sinephase2_sat[HH])) + const_sat[HH];
					d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = d_lights_sine1[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH]
							+ d_lights_sine2[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH];
					if (d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] <= min_sat[HH])
					{
						d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = min_sat[HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_min_sat[HH]);
						n = 1;
					}

					if (d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] >= max_sat[HH])
					{
						d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = max_sat[HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_max_sat[HH]);
						n = 1;
					}
					if (n == 0)
					{
						d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = d_lights_sine[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH]
								+ (RandomHelper.getNormal().nextDouble() * stddev_sine_sat[HH]);
					}
					if (d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] < 0)
					{
						d_lights[i * Consts.MELODY_MODELS_TICKS_PER_DAY + HH] = 0;
					}
				}
			}
		}
		return d_lights;
	}

	// this submodule calculates the one minute demands for washing from the
	// specif (ic half-hourly demand)

	public static double[] trend_year_wash = new double[]
	{ 1970, 1990, 1998, 2000, 2005, 2010, 2015, 2020 };
	public static double[] forty_trend = new double[]
	{ 0.30, 0.58, 0.64, 0.66, 0.68, 0.68, 0.68, 0.68 };
	public static double[] sixty_trend = new double[]
	{ 0.45, 0.36, 0.34, 0.32, 0.30, 0.30, 0.30, 0.30 };
	public static double[] ninety_trend = new double[]
	{ 0.25, 0.06, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03 };
	public static double[] forty_wash = new double[]
	{ 0.05, 0.05, 0.3, 0.3, 2, 2, 2.05, 2.1, 2.1, 2, 2.05, 2, 2.1, 2.05, 0.3, 0.25, 0.1, 0.05, 0.45, 0.3, 0.25, 0.75, 0.05, 0.05, 0.45,
			0.3, 0.25, 0.75, 0.05, 0.05, 0.45, 0.3, 0.25, 0.75, 0.05, 0.05, 0.05, 0.3, 0.3, 0.1, 0.75, 0.4, 0.4, 0.4, 0.1, 0.1, 0.1, 0.1 };

	public static double[] forty_wash_reactive = new double[]
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.216, 0.144, 0.12, 0.36, 0, 0, 0.216, 0.144, 0.12, 0.36, 0, 0, 0.216, 0.144,
			0.12, 0.36, 0, 0, 0, 0, 0, 0, 0.36, 0.192, 0.192, 0.192, 0, 0, 0, 0 };
	// based on forty_wash*0.48 (tan(cos-1(PF of, 0.9 - while spin motor runs)
	public static double[] sixty_wash = new double[]
	{ 0.05, 0.05, 0.3, 0.3, 2, 2, 2.05, 2.1, 2, 2.05, 2.05, 2, 2.1, 2.05, 2, 2.05, 2, 2.1, 2, 2.1, 0.1, 0.05, 2, 2.1, 0.3, 0.25, 0.1, 0.05,
			0.45, 0.3, 0.25, 0.75, 0.05, 0.05, 0.45, 0.3, 0.25, 0.75, 0.05, 0.05, 0.45, 0.3, 0.25, 0.75, 0.05, 0.05, 0.05, 0.3, 0.3, 0.1,
			0.75, 0.4, 0.4, 0.4, 0.1, 0.1, 0.1, 0.1 };
	public static double[] sixty_wash_reactive = new double[]
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.144, 0.12, 0, 0, 0.216, 0.144, 0.12, 0.36, 0, 0, 0.216,
			0.144, 0.12, 0.36, 0, 0, 0.216, 0.144, 0.12, 0.36, 0, 0, 0, 0, 0, 0, 0.36, 0.192, 0.192, 0.192, 0, 0, 0, 0, 0 };
	public static double[] ninety_wash = new double[]
	{ 0.05, 0.05, 0.3, 0.3, 2, 2, 2.05, 2.1, 2, 2.05, 2.05, 2, 2.1, 2.05, 2, 2.05, 2, 2.1, 2, 2.1, 2, 2.05, 2.1, 2.1, 2, 2.05, 2.1, 2, 2.1,
			2.05, 2.1, 2, 2.1, 2.05, 2.1, 2, 2, 2.1, 2.05, 2.1, 0.1, 0.05, 2, 2.1, 0.1, 0.05, 2, 2.1, 0.1, 0.05, 2, 2.1, 0.3, 0.25, 0.1,
			0.05, 0.45, 0.3, 0.25, 0.75, 0.05, 0.05, 0.45, 0.3, 0.25, 0.75, 0.05, 0.05, 0.45, 0.3, 0.25, 0.75, 0.05, 0.05, 0.05, 0.3, 0.3,
			0.1, 0.75, 0.4, 0.4, 0.4, 0.1, 0.1, 0.1, 0.1 };
	public static double[] ninety_wash_reactive = new double[]
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.216, 0.144, 0.12, 0.36, 0, 0, 0.216, 0.144, 0.12, 0.36, 0, 0, 0.216, 0.144, 0.12, 0.36,
			0, 0, 0, 0, 0, 0, 0.36, 0.192, 0.192, 0.192, 0, 0, 0, 0 };

	public static double[] dry_event = new double[]
	{ 1.901, 2.340, 2.441, 2.326, 2.016, 2.340, 2.441, 2.441, 2.300, 1.941, 2.441, 2.441, 2.415, 2.225, 2.041, 2.441, 2.415, 2.340, 2.326,
			2.041, 2.415, 2.340, 2.441, 2.326, 2.016, 2.340, 2.441, 2.441, 2.300, 1.941, 2.441, 2.441, 2.415, 2.225, 2.041, 2.441, 2.415,
			2.340, 2.326, 2.041, 2.415, 2.340, 2.441, 2.326, 2.016, 2.340, 2.441, 2.441, 2.300, 1.941, 2.441, 2.441, 2.415, 2.225, 2.041,
			2.441, 2.415, 2.340, 2.326, 2.041, 2.415, 2.340, 2.441, 2.326, 2.016, 2.340, 2.441, 2.441, 2.300, 1.941, 2.441, 2.441, 2.415,
			2.225, 2.041, 2.441, 2.415, 2.340, 2.326, 2.041, 2.415, 2.340, 2.441, 2.326, 2.016, 2.340, 2.441, 2.441, 2.300, 1.941, 2.441,
			2.441, 2.415, 2.225, 2.041, 2.441, 2.415, 2.340, 2.326, 2.041, 2.415, 2.340, 2.441, 2.326, 2.016, 2.340, 2.441, 2.441, 2.300,
			1.941, 2.441, 2.441, 2.415, 2.225, 2.041, 2.441, 2.415, 2.340, 2.326, 2.041 };
	public static double[] dry_event_reactive = new double[]
	{ 0.048, 0.144, 0.192, 0.156, 0.084, 0.144, 0.192, 0.192, 0.144, 0.048, 0.192, 0.192, 0.180, 0.108, 0.096, 0.192, 0.180, 0.144, 0.156,
			0.096, 0.180, 0.144, 0.192, 0.156, 0.084, 0.144, 0.192, 0.192, 0.144, 0.048, 0.192, 0.192, 0.180, 0.108, 0.096, 0.192, 0.180,
			0.144, 0.156, 0.096, 0.180, 0.144, 0.192, 0.156, 0.084, 0.144, 0.192, 0.192, 0.144, 0.048, 0.192, 0.192, 0.180, 0.108, 0.096,
			0.192, 0.180, 0.144, 0.156, 0.096, 0.180, 0.144, 0.192, 0.156, 0.084, 0.144, 0.192, 0.192, 0.144, 0.048, 0.192, 0.192, 0.180,
			0.108, 0.096, 0.192, 0.180, 0.144, 0.156, 0.096, 0.180, 0.144, 0.192, 0.156, 0.084, 0.144, 0.192, 0.192, 0.144, 0.048, 0.192,
			0.192, 0.180, 0.108, 0.096, 0.192, 0.180, 0.144, 0.156, 0.096, 0.180, 0.144, 0.192, 0.156, 0.084, 0.144, 0.192, 0.192, 0.144,
			0.048, 0.192, 0.192, 0.180, 0.108, 0.096, 0.192, 0.180, 0.144, 0.156, 0.096 };
	// dishwashers
	public static double[] dish_chance = new double[]
	{ 1.337, 0.920, 0.998, 0.677 };
	// half-hourly average demand for 4 dishwasher cycles
	public static double[] trend_year_dish = new double[]
	{ 1990, 1998, 2000, 2005, 2010, 2015, 2020 };
	public static double[] dish_temp_trend = new double[]
	{ 0.68, 0.64, 0.63, 0.59, 0.55, 0.51, 0.50 };
	// ratio of dish-washes that are at 65deg, rest at 55 deg

	// 76x4 array
	public static double[][] dish_event = new double[][]
	{
	{ 0, 0, 0, 0 },
	{ 0, 0, 0, 0 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.05, 0.05 },
	{ 0.1, 0.1, 0, 0 },
	{ 0, 0, 0.2, 0.2 },
	{ 0, 0, 0.2, 0.2 },
	{ 0.2, 0.2, 2.1, 2.1 },
	{ 2.7, 2.7, 2.1, 2.1 },
	{ 2.7, 2.7, 2.1, 2.1 },
	{ 2.7, 2.7, 2.1, 2.1 },
	{ 2.7, 2.7, 2.1, 2.1 },
	{ 2.7, 2.7, 2.1, 2.1 },
	{ 2.7, 2.7, 2.1, 2.1 },
	{ 2.7, 2.7, 2.1, 2.1 },
	{ 2.7, 2.7, 2.1, 0.2 },
	{ 2.7, 2.7, 2.1, 0.2 },
	{ 2.7, 0.2, 2.1, 0.2 },
	{ 2.7, 0.2, 2.1, 0.2 },
	{ 2.7, 0.2, 2.1, 0.2 },
	{ 2.7, 0.2, 0.2, 0.2 },
	{ 2.7, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.05 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.2, 0.1, 0.2, 0.2 },
	{ 0.2, 0, 0.05, 0 },
	{ 0.2, 0, 0.2, 0.2 },
	{ 0.2, 0, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 0.2 },
	{ 0.1, 0.2, 0.2, 0.05 },
	{ 0, 0.1, 0, 0 },
	{ 0, 0.1, 0.2, 0.2 },
	{ 0, 0, 0.2, 0.2 },
	{ 0.2, 0.2, 0.2, 2.1 },
	{ 0.2, 0.2, 0.05, 2.1 },
	{ 0.1, 0.1, 0, 2.1 },
	{ 0.1, 0.1, 0.2, 2.1 },
	{ 0, 0, 0.2, 2.15 },
	{ 0.2, 0.2, 2.1, 2.15 },
	{ 0.2, 2.7, 2.1, 2.15 },
	{ 0.1, 2.7, 2.1, 0.2 },
	{ 0.1, 2.7, 2.1, 0.2 },
	{ 0, 2.7, 2.15, 0.2 },
	{ 0.2, 2.7, 2.15, 0.2 },
	{ 2.7, 2.7, 2.15, 0.2 },
	{ 2.7, 2.7, 2.15, 0.2 },
	{ 2.7, 2.7, 2.15, 0.2 },
	{ 2.7, 2.7, 2.15, 0.05 },
	{ 2.7, 2.7, 2.15, 0 },
	{ 2.7, 0.2, 0.2, 0 },
	{ 2.7, 0.2, 0.2, 2.1 },
	{ 2.7, 0.2, 0.2, 2.1 },
	{ 2.7, 0.2, 0.2, 0 },
	{ 2.7, 0.2, 0.2, 0 },
	{ 2.7, 0.1, 0.2, 2.1 },
	{ 2.7, 0.1, 0.2, 2.1 },
	{ 2.7, 0, 0.05, 0.05 },
	{ 2.7, 0, 0, 0 },
	{ 0.2, 0, 0, 0 },
	{ 0.2, 0, 2.1, 0 },
	{ 0.2, 0, 2.1, 0 },
	{ 0.2, 0, 0, 0 },
	{ 0.2, 0, 0, 0 },
	{ 0.1, 0, 2.1, 0 },
	{ 0.1, 0, 2.1, 0 },
	{ 0, 0, 0.05, 0 },
	{ 0, 0, 0, 0 },
	{ 0, 0, 0, 0 },
	{ 0, 0, 0, 0 } };

	// private static double[] one_min_wash_generate(double[] D_HHspecific_wash,
	// double[] D_HHspecific_dish, double[] D_HHspecific_dryer, int Nspan, int
	// y, int washMachs, int dryers, int washer_dryers, int dishwashers)//y =
	// year of simulation
	// {
	private static WeakHashMap<String, double[]> one_min_wash_generate(double[] D_HHspecific_wash, double[] D_HHspecific_dish,
			double[] D_HHspecific_dryer, int Nspan, int y, int washMachs, int dryers, int washer_dryers, int dishwashers)// y
																															// =
																															// year
																															// of
																															// simulation
	{
		double[] D_min_wash = new double[D_HHspecific_wash.length * 30];
		double[] D_min_wash_reactive = new double[D_HHspecific_wash.length * 30];
		double[] D_min_dish = new double[D_HHspecific_dish.length * 30];
		double[] D_min_dish_reactive = new double[D_HHspecific_dish.length * 30];
		double[] D_min_dry = new double[D_HHspecific_dryer.length * 30];
		double[] D_min_dry_reactive = new double[D_HHspecific_dryer.length * 30];

		boolean wash_marker = (washMachs > 0);
		boolean wash_dry_marker = (washer_dryers > 0);
		boolean dryer_marker = (dryers > 0);
		boolean dish_marker = (dishwashers > 0);

		/*
		 * D_min_washing (1:Nspan, 1:1440)=0; D_min_wash(1:Nspan,1:1440) = 0;
		 * D_min_dry(1:Nspan,1:1440) = 0; D_min_dish(1:Nspan,1:1440) = 0;
		 * D_min_wash_reactive(1:Nspan,1:1440)=0;
		 * D_min_dry_reactive(1:Nspan,1:1440) =0;
		 */

		// washing machines
		// first, the relative number of washes at 40, 60 and 90 deg is
		// calculated:
		int a = 0;
		for (int i = 0; i < 8; i++)
		{
			if (y > InitialProfileGenUtils.trend_year_wash[i])
			{
				a = a + 1;
			}
		}

		double b = (y - InitialProfileGenUtils.trend_year_wash[a - 1])
				/ (InitialProfileGenUtils.trend_year_wash[a] - InitialProfileGenUtils.trend_year_wash[a - 1]);
		double forty_num = InitialProfileGenUtils.forty_trend[a - 1] + b
				* (InitialProfileGenUtils.forty_trend[a] - InitialProfileGenUtils.forty_trend[a - 1]);
		double sixty_num = InitialProfileGenUtils.sixty_trend[a - 1] + b
				* (InitialProfileGenUtils.sixty_trend[a] - InitialProfileGenUtils.sixty_trend[a - 1]);
		double ninety_num = InitialProfileGenUtils.ninety_trend[a - 1] + b
				* (InitialProfileGenUtils.ninety_trend[a] - InitialProfileGenUtils.ninety_trend[a - 1]);

		int wash = 0;
		// determines whether wash events occur in washing machines &
		// washer-dryers
		if (wash_marker)
		{
			wash = 1;
		}

		if (wash_dry_marker)
		{
			wash = 1;
		}

		if (wash == 1)
		{
			int wash_end = 0;
			int wash_start = 0;
			double wash_demand = 0;
			for (int i = 0; i < Nspan; i++)
			{

				if (i > 0)
				{
					wash_end = wash_end - 1440;
				}

				for (int p = 0; p < 48; p++)
				{
					double R = RandomHelper.nextDouble();

					if (R > (forty_num))
					{
						if (R > (forty_num + sixty_num))
						{
							a = 1;
							// 90 deg wash
							wash_demand = 1.58;
						}
						if (R <= (forty_num + sixty_num))
						{
							a = 2;
							// 60 deg wash
							wash_demand = 1.099;
						}
					}
					if (R <= (forty_num))
					{
						a = 3;
						// 40 deg wash
						wash_demand = 0.852;
					}
					double wash_chance = 0.61 * D_HHspecific_wash[48 * i + p] / wash_demand;

					// factor of 0.61 added in to give 4.3 cycles/week on
					// average (Mansouri)
					R = RandomHelper.nextDouble();
					if (R < wash_chance)
					{
						wash_start = (int) (RandomHelper.nextDouble() * 30);
						if (wash_start > 30)
						{
							wash_start = 30;
						}
						if (wash_start == 0)
						{
							wash_start = 1;
						}
						// wash_start = ((p-1)*30) + wash_start;
						wash_start = (p * 30) + wash_start; // because we start
															// p at 0, not 1?

						int wash_flag = 0;
						if (wash_start <= wash_end)
						{
							wash_flag = 0;
						}
						if (wash_start > wash_end)
						{
							wash_flag = 1;
						}

						if (wash_flag > 0)
						{
							if (a == 1)
							{
								wash_end = wash_start + 86;
								for (int q = 0; q < 86; q++)
								{
									if (wash_start + q < 1440)
									{
										D_min_wash[i * 1440 + wash_start + q] = InitialProfileGenUtils.ninety_wash[q];
										D_min_wash_reactive[i * 1440 + wash_start + q] = InitialProfileGenUtils.ninety_wash_reactive[q];
									}
									if (wash_start + q >= 1440)
									{
										if ((i + 1) < Nspan)
										{
											D_min_wash[(i + 1) * 1440 + ((wash_start + q) - 1440)] = InitialProfileGenUtils.ninety_wash[q];
											D_min_wash_reactive[(i + 1) * 1440 + ((wash_start + q) - 1440)] = InitialProfileGenUtils.ninety_wash_reactive[q];
										}
									}
								}
							}
							if (a == 2)
							{
								wash_end = wash_start + 58;
								for (int q = 0; q < 58; q++)
								{
									if (wash_start + q < 1440)
									{
										D_min_wash[i * 1440 + wash_start + q] = InitialProfileGenUtils.sixty_wash[q];
										D_min_wash_reactive[i * 1440 + wash_start + q] = InitialProfileGenUtils.sixty_wash_reactive[q];
									}
									if (wash_start + q >= 1440)
									{
										if ((i + 1) < Nspan)
										{
											D_min_wash[(i + 1) * 1440 + ((wash_start + q) - 1440)] = InitialProfileGenUtils.sixty_wash[q];
											D_min_wash_reactive[(i + 1) * 1440 + ((wash_start + q) - 1440)] = InitialProfileGenUtils.sixty_wash_reactive[q];
										}
									}
								}
							}
							if (a == 3)
							{
								wash_end = wash_start + 48;
								for (int q = 0; q < 48; q++)
								{
									if (wash_start + q < 1440)
									{
										D_min_wash[i * 1440 + wash_start + q] = InitialProfileGenUtils.forty_wash[q];
										D_min_wash_reactive[i * 1440 + wash_start + q] = InitialProfileGenUtils.forty_wash_reactive[q];
									}
									if (wash_start + q >= 1440)
									{
										if ((i + 1) < Nspan)
										{
											D_min_wash[(i + 1) * 1440 + ((wash_start + q) - 1440)] = InitialProfileGenUtils.forty_wash[q];
											D_min_wash_reactive[(i + 1) * 1440 + ((wash_start + q) - 1440)] = InitialProfileGenUtils.forty_wash_reactive[q];
										}
									}
								}
							}
						}
					}
				}
			}
		}

		// tumble-dryers
		int dry = 0;
		if (dryer_marker)
		{
			dry = 1;
		}
		if (wash_dry_marker)
		{
			dry = 1;
		}

		if (dry == 1)
		{

			int dry_end = 0;
			int dry_start = 0;
			for (int i = 0; i < Nspan; i++)
			{
				if (i == 1)
				{
					dry_end = 0;
				}
				if (i > 1)
				{
					dry_end = dry_end - 1440;
				}

				for (int p = 0; p < 48; p++)
				{
					double dry_chance = D_HHspecific_dryer[i * 48 + p] / 2.29;

					double R = RandomHelper.nextDouble();
					if (R < dry_chance)
					{
						double R1 = RandomHelper.nextDouble();
						if (R1 > 0.75)
						{
							// 90min cycle
							a = 1;
						}
						if (R1 <= 0.75)
						{
							// 120 min cycle
							a = 2;
						}
						dry_start = (int) (RandomHelper.nextDouble() * 30);
						if (dry_start > 30)
						{
							dry_start = 30;
						}
						// dry_start = ((p-1)*30)+dry_start;
						dry_start = (p * 30) + dry_start;
						int dry_flag = 0;
						if (dry_start <= dry_end)
						{
							dry_flag = 0;
						}
						if (dry_start > dry_end)
						{
							dry_flag = 1;
						}
						if (dry_flag > 0)
						{
							if (a == 1)
							{
								dry_end = dry_start + 90;
								for (int q = 0; q < 90; q++)
								{
									if (dry_start + q < 1440)
									{
										D_min_dry[i * 1440 + dry_start + q] = InitialProfileGenUtils.dry_event[q];
										D_min_dry_reactive[i * 1440 + dry_start + q] = InitialProfileGenUtils.dry_event_reactive[q];
									}
									if (dry_start + q >= 1440)
									{
										if ((i + 1) < Nspan)
										{
											D_min_dry[(i + 1) * 1440 + ((dry_start + q) - 1440)] = InitialProfileGenUtils.dry_event[q];
											D_min_dry_reactive[(i + 1) * 1440 + ((dry_start + q) - 1440)] = InitialProfileGenUtils.dry_event_reactive[q];
										}
									}
								}
							}
							if (a == 2)
							{
								dry_end = dry_start + 120;
								for (int q = 0; q < 120; q++)
								{
									if (dry_start + q < 1440)
									{
										D_min_dry[i * 1440 + dry_start + q] = InitialProfileGenUtils.dry_event[q];
										D_min_dry_reactive[i * 1440 + dry_start + q] = InitialProfileGenUtils.dry_event_reactive[q];
									}
									if (dry_start + q >= 1440)
									{
										if ((i + 1) < Nspan)
										{
											D_min_dry[(i + 1) * 1440 + ((dry_start + q) - 1440)] = InitialProfileGenUtils.dry_event[q];
											D_min_dry_reactive[(i + 1) * 1440 + ((dry_start + q) - 1440)] = InitialProfileGenUtils.dry_event_reactive[q];
										}
									}
								}
							}
						}
					}
				}
			}
		}

		// dishwashers

		a = 0;
		for (int i = 0; i < 7; i++)
		{
			if (y > InitialProfileGenUtils.trend_year_dish[i])
			{
				a = a + 1;
			}
		}
		b = (y - InitialProfileGenUtils.trend_year_dish[a - 1])
				/ (InitialProfileGenUtils.trend_year_dish[a] - InitialProfileGenUtils.trend_year_dish[a - 1]);
		double dish_temp = InitialProfileGenUtils.dish_temp_trend[a - 1] + b
				* (InitialProfileGenUtils.dish_temp_trend[a] - InitialProfileGenUtils.dish_temp_trend[a - 1]);
		if (y < 1990)
		{
			dish_temp = 0.68;
		}

		if (dish_marker)
		{

			int dish_end = 0;
			int dish_start = 0;
			for (int i = 0; i < Nspan; i++)
			{
				if (i == 1)
				{
					dish_end = 0;
				}
				if (i > 1)
				{
					dish_end = dish_end - 1440;
				}
				for (int p = 0; p < 48; p++)
				{
					double R = RandomHelper.nextDouble();
					double R1 = RandomHelper.nextDouble();
					int n = 0;
					if (R <= 0.4)
					{
						// programme A
						if (R1 <= dish_temp)
						{
							// 65 deg wash
							n = 1;
						}
						if (R1 > dish_temp)
						{
							// 55 deg wash
							n = 2;
						}
					}
					if (R > 0.4)
					{
						// programme B
						if (R1 <= dish_temp)
						{
							n = 3;
						}
						if (R1 > dish_temp)
						{
							n = 4;
						}
					}
					R = RandomHelper.nextDouble();
					double dish_event_chance = 0.33 * D_HHspecific_dish[i * 48 + p] / InitialProfileGenUtils.dish_chance[n - 1];

					// 0.33 factor used to make average 0.76 events/day
					// (Mansouri)
					if (R < dish_event_chance)
					{
						dish_start = (int) (RandomHelper.nextDouble() * 30);
						if (dish_start == 0)
						{
							dish_start = 1;
						}
						if (dish_start > 30)
						{
							dish_start = 30;
						}
						// dish_start = ((p-1)*30)+dish_start;
						dish_start = (p * 30) + dish_start;
						int dish_flag = 0;
						if (dish_start <= dish_end)
						{
							dish_flag = 0;
						}
						if (dish_start > dish_end)
						{
							dish_flag = 1;
						}
						if (dish_flag > 0)
						{
							dish_end = dish_start + 76;
							for (int q = 0; q < 76; q++)
							{
								if (dish_start + q < 1440)
								{
									D_min_dish[i * 1440 + dish_start + q] = InitialProfileGenUtils.dish_event[q][n - 1];
								}
								if (dish_start + q >= 1440)
								{
									if ((i + 1) < Nspan)
									{
										D_min_dish[(i + 1) * 1440 + ((dish_start + q) - 1440)] = InitialProfileGenUtils.dish_event[q][n - 1];
									}
								}
							}
						}
					}
				}
			}
		}

		/*
		 * double[] D_min_washing = ArrayUtils.add(D_min_wash, D_min_dry,
		 * D_min_dish); double[] D_min_washing_reactive =
		 * ArrayUtils.add(D_min_wash_reactive, D_min_dry_reactive); // no
		 * reactive load for dishwasher (no spin cycle!) return D_min_washing;
		 */

		// Convert kW to kWh, as well as aggregate from one minute to half an
		// hour
		double[] d_washer = InitialProfileGenUtils.aggregate_one_min_kWh_to_half_hour_kWh(InitialProfileGenUtils
				.convertToKWh(D_min_wash, false));
		double[] d_dryer = InitialProfileGenUtils.aggregate_one_min_kWh_to_half_hour_kWh(InitialProfileGenUtils
				.convertToKWh(D_min_dry, false));
		double[] d_dish = InitialProfileGenUtils.aggregate_one_min_kWh_to_half_hour_kWh(InitialProfileGenUtils
				.convertToKWh(D_min_dish, false));

		/*
		 * for(int f=0; f<48; f++) { System.out.print(d_dryer[f] + " "); }
		 * Logger.getLogger(Consts.CASCADE_LOGGER_NAME).debug("\n");
		 */

		double temp1 = ArrayUtils.sum(d_washer) / Nspan;
		double temp2 = ArrayUtils.sum(d_dryer) / Nspan;
		double temp3 = ArrayUtils.sum(d_dish) / Nspan;

		/*
		 * System.out.print("[" + temp1 + "," + temp2 + "," + temp3 + "];");
		 * Logger
		 * .getLogger(Consts.CASCADE_LOGGER_NAME).debug(Arrays.toString(d_washer
		 * ));
		 * Logger.getLogger(Consts.CASCADE_LOGGER_NAME).debug(Arrays.toString
		 * (d_dryer));
		 * Logger.getLogger(Consts.CASCADE_LOGGER_NAME).debug(Arrays.
		 * toString(d_dish));
		 * 
		 * Logger.getLogger(Consts.CASCADE_LOGGER_NAME).debug(ArrayUtils.sum(
		 * D_min_wash));
		 */

		WeakHashMap<String, double[]> wetProfiles = new WeakHashMap<String, double[]>();

		wetProfiles.put(Consts.WET_APP_WASHER, d_washer);
		wetProfiles.put(Consts.WET_APP_DRYER, d_dryer);
		wetProfiles.put(Consts.WET_APP_DISHWASHER, d_dish);

		wetProfiles.put(Consts.WET_APP_WASHER_ORIGINAL, Arrays.copyOf(d_washer, d_washer.length));
		wetProfiles.put(Consts.WET_APP_DRYER_ORIGINAL, Arrays.copyOf(d_dryer, d_dryer.length));
		wetProfiles.put(Consts.WET_APP_DISHWASHER_ORIGINAL, Arrays.copyOf(d_dish, d_dish.length));

		return wetProfiles;
	}

	/**
	 * @param d_fridge
	 * @param b
	 * @return
	 */
	private static double[] convertToKWh(double[] array, boolean halfHourAverages)
	{

		double conversionFactor = 0;
		if (halfHourAverages)
		{
			conversionFactor = 0.5;
		}
		else
		{
			// one minute profile
			conversionFactor = 1d / 60;
		}

		return ArrayUtils.multiply(array, conversionFactor);
	}

	private static double[] aggregate_one_min_kWh_to_half_hour_kWh(double[] one_min_prof)
	{
		double[] returnArr = new double[one_min_prof.length / 30];
		for (int i = 0; i < one_min_prof.length; i += 30)
		{
			double sum = 0;
			for (int j = 0; j < 30; j++)
			{
				sum += one_min_prof[i + j];
			}
			returnArr[i / 30] = sum;
		}
		return returnArr;
	}

}
