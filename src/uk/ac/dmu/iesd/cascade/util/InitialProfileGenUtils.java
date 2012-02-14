/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Arrays;
import java.util.HashMap;

import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * A suite of profile generating methods for various appliance types.
 * 
 * Based on the work of Melody Stokes, in her 2004 thesis
 * 
 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
 * 
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
	public static double[] melodyStokesColdApplianceGen_Old(int numDays, boolean fridges, boolean fridgeFreezers, boolean freezers)
	{
		return melodyStokesColdApplianceGen_Old(numDays, fridges ? 1 : 0, freezers ? 1:0, fridgeFreezers ? 1:0);
	}
	
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
	public static HashMap melodyStokesColdApplianceGen(int numDays, boolean fridges, boolean fridgeFreezers, boolean freezers)
	{
		//System.out.println("Fridge; FridgeFreezer; Freezer"+ fridges  +" "+ fridgeFreezers + " "+ freezers); 

		//return melodyStokesColdApplianceGen(numDays, fridges ? 1 : 0, freezers ? 1:0, fridgeFreezers ? 1:0);
		
		return melodyStokesColdApplianceGen(numDays, fridges ? 1 : 0, fridgeFreezers ? 1:0, freezers ? 1:0);

	}


	/**
	 * Java implementation of the matlab code from Melody Stokes' model of
	 * cold appliance demand.  Note that this implementation does not account for leap years and 
	 * simply assumes that days per year or where in the year the simulation starts
	 * In effect, it assumes a 1st January start.
	 * 
	 * TODO: If this is to be used extensively, need to make it sensitive to start date etc.
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	public static double[] melodyStokesColdApplianceGen_Old(int numDays, int fridges, int fridgeFreezers, int freezers)
	{
		double[] d_fridge = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_freezer = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_fridge_freezer = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		//This sub-model calculates the half-hourly group average domestic electricity demand
		//for cooling

		//this section calculates demand for fridges. The demand is the same basic annual tr} but scaled for each half-hour:
		double[] scale_fridge={0.113d, 0.106d, 0.083d, 0.100d, 0.092d, 0.090d, 0.095d, 0.085d, 0.085d, 0.084d, 0.072d, 0.084d, 0.081d, 0.079d, 0.083d, 0.092d, 0.098d, 0.084d, 0.093d, 0.101d, 0.101d, 0.092d, 0.105d, 0.104d, 0.107d, 0.115d, 0.114d, 0.121d, 0.118d, 0.120d, 0.110d, 0.119d, 0.122d, 0.118d, 0.119d, 0.119d, 0.129d, 0.124d, 0.122d, 0.116d, 0.107d, 0.108d, 0.100d, 0.104d, 0.107d, 0.110d, 0.110d, 0.100d};
		double[] phase_fridge={1.9d, 1.9d, 1.9d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.0d, 2.1d, 2.1d, 2.0d, 2.1d, 2.2d, 2.2d, 2.2d, 2.0d, 2.0d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.1d, 2.1d, 2.0d, 2.1d, 2.1d, 2.1d, 2.1d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.1d};
		double[] const_fridge ={0.441d, 0.447d, 0.424d, 0.433d, 0.428d, 0.423d, 0.415d, 0.406d, 0.405d, 0.409d, 0.397d, 0.399d, 0.403d, 0.403d, 0.421d, 0.443d, 0.451d, 0.449d, 0.452d, 0.453d, 0.451d, 0.456d, 0.459d, 0.460d, 0.470d, 0.490d, 0.503d, 0.482d, 0.485d, 0.481d, 0.483d, 0.488d, 0.495d, 0.506d, 0.529d, 0.545d, 0.549d, 0.549d, 0.546d, 0.532d, 0.519d, 0.510d, 0.510d, 0.502d, 0.503d, 0.493d, 0.472d, 0.458d};
		double[] stddev_fridge={0.064d, 0.065d, 0.063d, 0.063d, 0.062d, 0.060d, 0.060d, 0.063d, 0.058d, 0.061d, 0.058d, 0.058d, 0.057d, 0.056d, 0.063d, 0.062d, 0.060d, 0.062d, 0.064d, 0.059d, 0.063d, 0.068d, 0.064d, 0.071d, 0.071d, 0.079d, 0.076d, 0.079d, 0.077d, 0.077d, 0.076d, 0.077d, 0.070d, 0.079d, 0.081d, 0.086d, 0.078d, 0.076d, 0.077d, 0.076d, 0.071d, 0.070d, 0.066d, 0.067d, 0.066d, 0.068d, 0.069d, 0.065d};
		//this section calculates demand for freezers. It introduces a variable constant value as well as a variable std dev for the random element
		double[] scale_freezer={0.12d, 0.1d, 0.1d, 0.11d, 0.1d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.09d, 0.11d, 0.1d, 0.11d, 0.11d, 0.1d, 0.11d, 0.11d, 0.12d, 0.11d, 0.12d, 0.11d, 0.12d, 0.13d, 0.14d, 0.12d, 0.13d, 0.12d, 0.13d, 0.12d, 0.13d, 0.13d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.11d, 0.11d, 0.11d, 0.11d};
		double[] const_freezer={0.658d, 0.66d, 0.643d, 0.652d, 0.65d, 0.643d, 0.644d, 0.64d, 0.637d, 0.632d, 0.631d, 0.631d, 0.629d, 0.625d, 0.631d, 0.633d, 0.636d, 0.638d, 0.643d, 0.65d, 0.65d, 0.657d, 0.659d, 0.669d, 0.674d, 0.682d, 0.688d, 0.69d, 0.689d, 0.686d, 0.692d, 0.693d, 0.696d, 0.698d, 0.703d, 0.71d, 0.713d, 0.707d, 0.709d, 0.699d, 0.698d, 0.695d, 0.69d, 0.685d, 0.681d, 0.676d, 0.671d, 0.67d};
		double[] stddev_freezer={0.054d, 0.052d, 0.052d, 0.053d, 0.055d, 0.056d, 0.051d, 0.053d, 0.051d, 0.055d, 0.053d, 0.048d, 0.055d, 0.051d, 0.048d, 0.055d, 0.051d, 0.054d, 0.052d, 0.052d, 0.053d, 0.051d, 0.056d, 0.051d, 0.051d, 0.055d, 0.054d, 0.058d, 0.056d, 0.056d, 0.055d, 0.059d, 0.054d, 0.058d, 0.053d, 0.055d, 0.056d, 0.057d, 0.057d, 0.055d, 0.059d, 0.058d, 0.053d, 0.058d, 0.053d, 0.052d, 0.054d, 0.054d};
		//this section calculates demand for fridge-freezers.It follows a similar pattern to the freezer model
		//but phase is now also a variable with half-hour
		double[] scale_fridge_freezer={0.11d, 0.1d, 0.1d, 0.1d, 0.11d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.1d, 0.1d, 0.11d, 0.11d, 0.11d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.13d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.11d, 0.12d, 0.11d, 0.11d, 0.12d, 0.11d, 0.11d};
		double[] phase_fridge_freezer={2.25d, 2.25d, 2.25d, 2.39d, 2.44d, 2.42d, 2.42d, 2.45d, 2.43d, 2.43d, 2.43d, 2.49d, 2.42d, 2.45d, 2.37d, 2.35d, 2.34d, 2.39d, 2.38d, 2.35d, 2.41d, 2.37d, 2.38d, 2.34d, 2.35d, 2.28d, 2.29d, 2.28d, 2.28d, 2.28d, 2.25d, 2.28d, 2.26d, 2.27d, 2.3d, 2.25d, 2.26d, 2.24d, 2.29d, 2.3d, 2.27d, 2.36d, 2.32d, 2.31d, 2.42d, 2.4d, 2.4d, 2.36d};
		double[] const_fridge_freezer={0.615d, 0.618d, 0.589d, 0.603d, 0.601d, 0.597d, 0.594d, 0.589d, 0.584d, 0.582d, 0.584d, 0.581d, 0.58d, 0.584d, 0.594d, 0.604d, 0.611d, 0.61d, 0.612d, 0.615d, 0.616d, 0.617d, 0.621d, 0.631d, 0.642d, 0.657d, 0.668d, 0.659d, 0.659d, 0.657d, 0.652d, 0.66d, 0.665d, 0.67d, 0.677d, 0.686d, 0.694d, 0.688d, 0.684d, 0.673d, 0.672d, 0.663d, 0.657d, 0.654d, 0.654d, 0.651d, 0.639d, 0.625d};
		double[] stddev_fridge_freezer={0.05d, 0.049d, 0.052d, 0.049d, 0.046d, 0.047d, 0.049d, 0.048d, 0.046d, 0.049d, 0.043d, 0.046d, 0.047d, 0.048d, 0.048d, 0.048d, 0.047d, 0.047d, 0.045d, 0.047d, 0.047d, 0.051d, 0.048d, 0.053d, 0.051d, 0.052d, 0.056d, 0.056d, 0.054d, 0.056d, 0.056d, 0.055d, 0.057d, 0.055d, 0.057d, 0.055d, 0.054d, 0.056d, 0.054d, 0.053d, 0.049d, 0.051d, 0.049d, 0.053d, 0.049d, 0.052d, 0.048d, 0.048d};

		//Initialise a normal distribution for selection
		RandomHelper.createNormal(0, 1);

		for (int i=0; i < numDays; i++)
		{
			for (int HH=0; HH < Consts.MELODY_MODELS_TICKS_PER_DAY; HH++)
			{
				//System.out.println("Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH]): "+ Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH]));

				d_fridge[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=fridges * ( Math.max(0, scale_fridge[HH]*Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH])+const_fridge[HH]+(RandomHelper.getNormal().nextDouble()*stddev_fridge[HH])));
				d_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=fridgeFreezers * ( Math.max(0,scale_freezer[HH]* Math.sin(2*Math.PI*(i / Consts.DAYS_PER_YEAR)-2.05)+const_freezer[HH]+(RandomHelper.getNormal().nextDouble()*stddev_freezer[HH])));
				d_fridge_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=freezers * ( Math.max(0,scale_fridge_freezer[HH]* Math.sin(2*Math.PI*(i / Consts.DAYS_PER_YEAR)-phase_fridge_freezer[HH])+const_fridge_freezer[HH]+(RandomHelper.getNormal().nextDouble()*stddev_fridge_freezer[HH])));

			}
		}

		return ArrayUtils.add(d_fridge, d_freezer, d_fridge_freezer);

	}
	
	/**
	 * Java implementation of the matlab code from Melody Stokes' model of
	 * cold appliance demand.  Note that this implementation does not account for leap years and 
	 * simply assumes that days per year or where in the year the simulation starts
	 * In effect, it assumes a 1st January start.
	 * 
	 * TODO: If this is to be used extensively, need to make it sensitive to start date etc.
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	public static HashMap melodyStokesColdApplianceGen(int numDays, int fridges, int fridgeFreezers, int freezers)
	{
		double[] d_fridge = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_freezer = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_fridge_freezer = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		//This sub-model calculates the half-hourly group average domestic electricity demand
		//for cooling

		//this section calculates demand for fridges. The demand is the same basic annual tr} but scaled for each half-hour:
		double[] scale_fridge={0.113d, 0.106d, 0.083d, 0.100d, 0.092d, 0.090d, 0.095d, 0.085d, 0.085d, 0.084d, 0.072d, 0.084d, 0.081d, 0.079d, 0.083d, 0.092d, 0.098d, 0.084d, 0.093d, 0.101d, 0.101d, 0.092d, 0.105d, 0.104d, 0.107d, 0.115d, 0.114d, 0.121d, 0.118d, 0.120d, 0.110d, 0.119d, 0.122d, 0.118d, 0.119d, 0.119d, 0.129d, 0.124d, 0.122d, 0.116d, 0.107d, 0.108d, 0.100d, 0.104d, 0.107d, 0.110d, 0.110d, 0.100d};
		double[] phase_fridge={1.9d, 1.9d, 1.9d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.0d, 2.1d, 2.1d, 2.0d, 2.1d, 2.2d, 2.2d, 2.2d, 2.0d, 2.0d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.1d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.1d, 2.1d, 2.0d, 2.1d, 2.1d, 2.1d, 2.1d, 2.0d, 2.0d, 2.0d, 2.0d, 2.1d, 2.1d};
		double[] const_fridge ={0.441d, 0.447d, 0.424d, 0.433d, 0.428d, 0.423d, 0.415d, 0.406d, 0.405d, 0.409d, 0.397d, 0.399d, 0.403d, 0.403d, 0.421d, 0.443d, 0.451d, 0.449d, 0.452d, 0.453d, 0.451d, 0.456d, 0.459d, 0.460d, 0.470d, 0.490d, 0.503d, 0.482d, 0.485d, 0.481d, 0.483d, 0.488d, 0.495d, 0.506d, 0.529d, 0.545d, 0.549d, 0.549d, 0.546d, 0.532d, 0.519d, 0.510d, 0.510d, 0.502d, 0.503d, 0.493d, 0.472d, 0.458d};
		double[] stddev_fridge={0.064d, 0.065d, 0.063d, 0.063d, 0.062d, 0.060d, 0.060d, 0.063d, 0.058d, 0.061d, 0.058d, 0.058d, 0.057d, 0.056d, 0.063d, 0.062d, 0.060d, 0.062d, 0.064d, 0.059d, 0.063d, 0.068d, 0.064d, 0.071d, 0.071d, 0.079d, 0.076d, 0.079d, 0.077d, 0.077d, 0.076d, 0.077d, 0.070d, 0.079d, 0.081d, 0.086d, 0.078d, 0.076d, 0.077d, 0.076d, 0.071d, 0.070d, 0.066d, 0.067d, 0.066d, 0.068d, 0.069d, 0.065d};
		//this section calculates demand for freezers. It introduces a variable constant value as well as a variable std dev for the random element
		double[] scale_freezer={0.12d, 0.1d, 0.1d, 0.11d, 0.1d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.1d, 0.09d, 0.11d, 0.1d, 0.11d, 0.11d, 0.1d, 0.11d, 0.11d, 0.12d, 0.11d, 0.12d, 0.11d, 0.12d, 0.13d, 0.14d, 0.12d, 0.13d, 0.12d, 0.13d, 0.12d, 0.13d, 0.13d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.11d, 0.11d, 0.11d, 0.11d};
		double[] const_freezer={0.658d, 0.66d, 0.643d, 0.652d, 0.65d, 0.643d, 0.644d, 0.64d, 0.637d, 0.632d, 0.631d, 0.631d, 0.629d, 0.625d, 0.631d, 0.633d, 0.636d, 0.638d, 0.643d, 0.65d, 0.65d, 0.657d, 0.659d, 0.669d, 0.674d, 0.682d, 0.688d, 0.69d, 0.689d, 0.686d, 0.692d, 0.693d, 0.696d, 0.698d, 0.703d, 0.71d, 0.713d, 0.707d, 0.709d, 0.699d, 0.698d, 0.695d, 0.69d, 0.685d, 0.681d, 0.676d, 0.671d, 0.67d};
		double[] stddev_freezer={0.054d, 0.052d, 0.052d, 0.053d, 0.055d, 0.056d, 0.051d, 0.053d, 0.051d, 0.055d, 0.053d, 0.048d, 0.055d, 0.051d, 0.048d, 0.055d, 0.051d, 0.054d, 0.052d, 0.052d, 0.053d, 0.051d, 0.056d, 0.051d, 0.051d, 0.055d, 0.054d, 0.058d, 0.056d, 0.056d, 0.055d, 0.059d, 0.054d, 0.058d, 0.053d, 0.055d, 0.056d, 0.057d, 0.057d, 0.055d, 0.059d, 0.058d, 0.053d, 0.058d, 0.053d, 0.052d, 0.054d, 0.054d};
		//this section calculates demand for fridge-freezers.It follows a similar pattern to the freezer model
		//but phase is now also a variable with half-hour
		double[] scale_fridge_freezer={0.11d, 0.1d, 0.1d, 0.1d, 0.11d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.1d, 0.1d, 0.11d, 0.11d, 0.11d, 0.11d, 0.1d, 0.11d, 0.1d, 0.1d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.11d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.13d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.12d, 0.11d, 0.12d, 0.11d, 0.11d, 0.12d, 0.11d, 0.11d};
		double[] phase_fridge_freezer={2.25d, 2.25d, 2.25d, 2.39d, 2.44d, 2.42d, 2.42d, 2.45d, 2.43d, 2.43d, 2.43d, 2.49d, 2.42d, 2.45d, 2.37d, 2.35d, 2.34d, 2.39d, 2.38d, 2.35d, 2.41d, 2.37d, 2.38d, 2.34d, 2.35d, 2.28d, 2.29d, 2.28d, 2.28d, 2.28d, 2.25d, 2.28d, 2.26d, 2.27d, 2.3d, 2.25d, 2.26d, 2.24d, 2.29d, 2.3d, 2.27d, 2.36d, 2.32d, 2.31d, 2.42d, 2.4d, 2.4d, 2.36d};
		double[] const_fridge_freezer={0.615d, 0.618d, 0.589d, 0.603d, 0.601d, 0.597d, 0.594d, 0.589d, 0.584d, 0.582d, 0.584d, 0.581d, 0.58d, 0.584d, 0.594d, 0.604d, 0.611d, 0.61d, 0.612d, 0.615d, 0.616d, 0.617d, 0.621d, 0.631d, 0.642d, 0.657d, 0.668d, 0.659d, 0.659d, 0.657d, 0.652d, 0.66d, 0.665d, 0.67d, 0.677d, 0.686d, 0.694d, 0.688d, 0.684d, 0.673d, 0.672d, 0.663d, 0.657d, 0.654d, 0.654d, 0.651d, 0.639d, 0.625d};
		double[] stddev_fridge_freezer={0.05d, 0.049d, 0.052d, 0.049d, 0.046d, 0.047d, 0.049d, 0.048d, 0.046d, 0.049d, 0.043d, 0.046d, 0.047d, 0.048d, 0.048d, 0.048d, 0.047d, 0.047d, 0.045d, 0.047d, 0.047d, 0.051d, 0.048d, 0.053d, 0.051d, 0.052d, 0.056d, 0.056d, 0.054d, 0.056d, 0.056d, 0.055d, 0.057d, 0.055d, 0.057d, 0.055d, 0.054d, 0.056d, 0.054d, 0.053d, 0.049d, 0.051d, 0.049d, 0.053d, 0.049d, 0.052d, 0.048d, 0.048d};

		//Initialise a normal distribution for selection
		RandomHelper.createNormal(0, 1);
		
		HashMap coldProfiles = new HashMap();

		for (int i=0; i < numDays; i++)
		{
			for (int HH=0; HH < Consts.MELODY_MODELS_TICKS_PER_DAY; HH++)
			{
				//System.out.println("Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH]): "+ Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH]));

				d_fridge[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=fridges * ( Math.max(0, scale_fridge[HH]*Math.sin(2*Math.PI*(i/Consts.DAYS_PER_YEAR)-phase_fridge[HH])+const_fridge[HH]+(RandomHelper.getNormal().nextDouble()*stddev_fridge[HH])));
				d_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=freezers * ( Math.max(0,scale_freezer[HH]* Math.sin(2*Math.PI*(i / Consts.DAYS_PER_YEAR)-2.05)+const_freezer[HH]+(RandomHelper.getNormal().nextDouble()*stddev_freezer[HH])));
				d_fridge_freezer[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]= fridgeFreezers  * ( Math.max(0,scale_fridge_freezer[HH]* Math.sin(2*Math.PI*(i / Consts.DAYS_PER_YEAR)-phase_fridge_freezer[HH])+const_fridge_freezer[HH]+(RandomHelper.getNormal().nextDouble()*stddev_fridge_freezer[HH])));

			}
		}
		
		//Convert kW to kWh
		//TODO: Hard - coded constant!!! Shouldn't do this - fix.
		d_fridge = ArrayUtils.multiply(d_fridge, 0.5);
		d_freezer = ArrayUtils.multiply(d_freezer, 0.5);
		d_fridge_freezer = ArrayUtils.multiply(d_fridge_freezer, 0.5);
		
		coldProfiles.put(Consts.COLD_APP_FRIDGE, d_fridge);
		coldProfiles.put(Consts.COLD_APP_FREEZER, d_freezer);
		coldProfiles.put(Consts.COLD_APP_FRIDGEFREEZER, d_fridge_freezer);
		
		return coldProfiles;
		
		//return ArrayUtils.add(d_fridge, d_freezer, d_fridge_freezer);
	}



	/**
	 * @param daysPerYear
	 * @param hasWashingMachine
	 * @param hasWasherDryer
	 * @param hasDishWasher
	 * @param hasTumbleDryer
	 * @return
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	public static double[] melodyStokesWetApplianceGen_old(int numDays,
			boolean washMachine, boolean washerDryer,
			boolean dishWasher, boolean tumbleDryer) {
		// TODO Auto-generated method stub
		return 	melodyStokesWetApplianceGen_old2(numDays, washMachine ? 1 : 0, washerDryer ? 1:0, dishWasher ? 1:0, tumbleDryer ? 1:0);
	}
	
	/**
	 * @param daysPerYear
	 * @param hasWashingMachine
	 * @param hasWasherDryer
	 * @param hasDishWasher
	 * @param hasTumbleDryer
	 * @return
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	public static HashMap melodyStokesWetApplianceGen(CascadeContext context,int numDays,
			boolean washMachine, boolean washerDryer,
			boolean dishWasher, boolean tumbleDryer) {
		// TODO Auto-generated method stub
		return 	melodyStokesWetApplianceGen(context, numDays, washMachine ? 1 : 0, washerDryer ? 1:0, dishWasher ? 1:0, tumbleDryer ? 1:0);
	}

	/**
	 * @param numDays
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 * @return
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	private static double[] melodyStokesWetApplianceGen_old(int numDays, int washMach,
			int washDry, int dishWash, int tumbleDry) {
		// nasty implementation that assumes this starts 1st Jan and that's a Sunday
		// TODO: refine days of week.  Possibly add start date to context and maintain day of week etc in there too
		//this sub-model is for half-hourly electricty demand for washing appliances
		//it covers washers, dryers, washer-dryers combined and dishwashers
		double[] d_washer_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dryer_UR =new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dish_UR =new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		//washer parameters for Mondays/Saturdays/Sundays/Weekdays for UR(unrestricted) and E7 tariffs:
		double[] scale_washer_mon_UR={0.004d, 0d, 0.006d, 0.003d, 0.046d, 0.039d, 0.002d, 0.001d, 0.026d, 0.008d, 0.005d, 0.001d, 0.006d, 0.013d, 0.006d, 0.046d, 0.083d, 0.078d, 0.03d, 0.065d, 0.12d, 0.1d, 0.075d, 0.04d, 0.086d, 0.038d, 0.07d, 0.009d, 0.033d, 0.043d, 0.043d, 0.05d, 0.05d, 0.037d, 0.028d, 0.012d, 0.031d, 0.051d, 0.073d, 0.028d, 0.039d, 0.032d, 0.015d, 0.034d, 0.007d, 0.007d, 0.01d, 0.007d};
		double[] phase_washer_mon_UR={2.8d, 0d, 3.6d, 4.6d, 3d, 3.3d, 2.5d, 3.8d, 5.5d, 5.5d, 0.1d, 5d, 0.7d, 2.2d, 0.5d, 0.9d, 0.5d, 0d, 0d, 0.5d, 0.3d, 5.8d, 5.4d, 6.2d, 6.1d, 0.4d, 0.1d, 5.6d, 5.2d, 5.5d, 0.9d, 6.2d, 5.3d, 5d, 5.9d, 4d, 4.4d, 5.5d, 5.7d, 5.2d, 5.2d, 5.6d, 5.6d, 1.1d, 1.8d, 3.9d, 5.6d, 6.2d};
		double[] const_washer_mon_UR={0.009d, 0.007d, 0.009d, 0.005d, 0.041d, 0.036d, 0.005d, 0.004d, 0.016d, 0.005d, 0.005d, 0.008d, 0.024d, 0.02d, 0.102d, 0.122d, 0.263d, 0.394d, 0.417d, 0.368d, 0.385d, 0.37d, 0.347d, 0.292d, 0.281d, 0.246d, 0.209d, 0.165d, 0.164d, 0.176d, 0.158d, 0.155d, 0.164d, 0.148d, 0.139d, 0.128d, 0.131d, 0.12d, 0.157d, 0.119d, 0.091d, 0.086d, 0.083d, 0.059d, 0.038d, 0.027d, 0.03d, 0.013d};
		double[] stddev_washer_mon_UR={0.02d, 0.019d, 0.021d, 0.014d, 0.042d, 0.05d, 0.011d, 0.006d, 0.046d, 0.015d, 0.023d, 0.019d, 0.044d, 0.032d, 0.074d, 0.096d, 0.148d, 0.19d, 0.181d, 0.16d, 0.146d, 0.171d, 0.144d, 0.137d, 0.142d, 0.145d, 0.113d, 0.111d, 0.108d, 0.138d, 0.1d, 0.105d, 0.11d, 0.082d, 0.088d, 0.082d, 0.106d, 0.095d, 0.095d, 0.088d, 0.09d, 0.084d, 0.083d, 0.065d, 0.044d, 0.045d, 0.06d, 0.026d};

		double[] scale_washer_sat_UR={0.05d, 0.012d, 0.007d, 0.008d, 0.013d, 0.007d, 0d, 0d, 0d, 0d, 0d, 0.003d, 0.009d, 0.004d, 0.01d, 0.027d, 0.029d, 0.02d, 0.05d, 0.051d, 0.063d, 0.057d, 0.044d, 0.071d, 0.057d, 0.014d, 0.045d, 0.085d, 0.028d, 0.019d, 0.029d, 0.007d, 0.013d, 0.008d, 0.02d, 0.055d, 0.054d, 0.033d, 0.006d, 0.004d, 0.015d, 0.025d, 0.015d, 0.013d, 0d, 0.011d, 0d, 0.004d};
		double[] phase_washer_sat_UR={5d, 4.6d, 5.2d, 4.6d, 2.4d, 2.9d, 0d, 0d, 0d, 0d, 0d, 4.2d, 3.8d, 3.4d, 2.2d, 2.6d, 2.7d, 5.3d, 0.2d, 5.2d, 6d, 5.3d, 4.8d, 5.6d, 0d, 4.8d, 6.1d, 0.2d, 0d, 5d, 3.2d, 1.8d, 0.9d, 1.7d, 4.9d, 0.5d, 0.5d, 0.2d, 6.1d, 0.6d, 5.5d, 6d, 5.4d, 4.3d, 0d, 6d, 0d, 4.1d};
		double[] const_washer_sat_UR={0.024d, 0.017d, 0.012d, 0.008d, 0.014d, 0.014d, 0.005d, 0.003d, 0.006d, 0.005d, 0.007d, 0.012d, 0.023d, 0.028d, 0.03d, 0.109d, 0.159d, 0.203d, 0.31d, 0.372d, 0.43d, 0.444d, 0.391d, 0.38d, 0.359d, 0.326d, 0.286d, 0.284d, 0.279d, 0.251d, 0.21d, 0.212d, 0.186d, 0.146d, 0.124d, 0.12d, 0.12d, 0.117d, 0.111d, 0.087d, 0.072d, 0.071d, 0.066d, 0.047d, 0.05d, 0.037d, 0.043d, 0.024d};
		double[] stddev_washer_sat_UR={0.041d, 0.041d, 0.032d, 0.031d, 0.035d, 0.033d, 0.022d, 0.015d, 0.028d, 0.023d, 0.034d, 0.032d, 0.054d, 0.041d, 0.041d, 0.071d, 0.089d, 0.119d, 0.155d, 0.142d, 0.166d, 0.18d, 0.163d, 0.151d, 0.135d, 0.146d, 0.113d, 0.134d, 0.128d, 0.104d, 0.135d, 0.109d, 0.12d, 0.098d, 0.091d, 0.081d, 0.081d, 0.088d, 0.08d, 0.066d, 0.071d, 0.068d, 0.06d, 0.053d, 0.05d, 0.044d, 0.057d, 0.04d};

		double[] scale_washer_sun_UR={0.016d, 0.011d, 0.012d, 0.009d, 0.036d, 0.032d, 0.004d, 0.012d, 0.033d, 0.01d, 0.016d, 0.008d, 0.016d, 0.009d, 0.007d, 0d, 0.022d, 0.011d, 0.05d, 0.073d, 0.088d, 0.08d, 0.057d, 0.057d, 0.062d, 0.069d, 0.058d, 0.065d, 0.061d, 0.058d, 0.073d, 0.052d, 0.082d, 0.076d, 0.08d, 0.048d, 0.019d, 0.03d, 0.04d, 0.007d, 0.032d, 0.035d, 0.006d, 0.022d, 0.005d, 0.01d, 0.014d, 0.01d};
		double[] phase_washer_sun_UR={4.8d, 5.6d, 5.3d, 3.7d, 2.8d, 3.3d, 3.7d, 4.9d, 5d, 5d, 4.6d, 4.6d, 4.5d, 4.6d, 3.8d, 0d, 2.6d, 1.3d, 1.2d, 0.6d, 0.1d, 5.2d, 5d, 5.7d, 5.7d, 5.4d, 5d, 5.2d, 5.2d, 4.5d, 4.7d, 4.4d, 4.8d, 5d, 5d, 4.6d, 4.3d, 5d, 5.1d, 5.6d, 5.9d, 5.7d, 0.4d, 0.4d, 1.6d, 0d, 0d, 4.8d};
		double[] const_washer_sun_UR={0.024d, 0.017d, 0.018d, 0.022d, 0.04d, 0.032d, 0.01d, 0.012d, 0.019d, 0.006d, 0.01d, 0.005d, 0.013d, 0.021d, 0.021d, 0.032d, 0.055d, 0.096d, 0.184d, 0.251d, 0.348d, 0.406d, 0.358d, 0.323d, 0.316d, 0.267d, 0.268d, 0.193d, 0.22d, 0.198d, 0.201d, 0.187d, 0.181d, 0.189d, 0.154d, 0.146d, 0.119d, 0.12d, 0.122d, 0.122d, 0.09d, 0.079d, 0.085d, 0.074d, 0.055d, 0.035d, 0.031d, 0.025d};
		double[] stddev_washer_sun_UR={0.042d, 0.041d, 0.038d, 0.041d, 0.055d, 0.046d, 0.027d, 0.033d, 0.047d, 0.023d, 0.038d, 0.024d, 0.047d, 0.041d, 0.042d, 0.042d, 0.055d, 0.096d, 0.184d, 0.251d, 0.348d, 0.406d, 0.358d, 0.323d, 0.146d, 0.137d, 0.131d, 0.104d, 0.12d, 0.122d, 0.115d, 0.116d, 0.112d, 0.123d, 0.101d, 0.097d, 0.083d, 0.081d, 0.101d, 0.095d, 0.074d, 0.084d, 0.087d, 0.069d, 0.049d, 0.041d, 0.045d, 0.048d};

		double[] scale_washer_wkdays_UR={0.002d, 0.001d, 0d, 0d, 0.022d, 0.01d, 0d, 0.003d, 0.026d, 0d, 0d, 0.005d, 0.002d, 0.004d, 0.029d, 0.013d, 0.046d, 0.029d, 0.006d, 0.023d, 0.009d, 0.029d, 0.019d, 0.014d, 0.014d, 0.009d, 0.007d, 0.022d, 0.019d, 0.009d, 0.01d, 0.015d, 0.021d, 0.01d, 0.017d, 0.018d, 0.018d, 0.025d, 0.022d, 0.01d, 0.034d, 0.023d, 0.025d, 0.018d, 0.003d, 0.002d, 0.003d, 0.002d};
		double[] phase_washer_wkdays_UR={1.4d, 1.4d, 0d, 0d, 2.4d, 2.6d, 0d, 5d, 5.4d, 0d, 0d, 2.9d, 4.2d, 2.8d, 0.5d, 0.9d, 1d, 6.2d, 2d, 5.9d, 0d, 0d, 5.9d, 0.5d, 4.6d, 4.4d, 5.2d, 0d, 5.5d, 4.9d, 5.8d, 5.4d, 6d, 5.5d, 6d, 6.2d, 6d, 4.9d, 5.3d, 6d, 6d, 0.5d, 0.7d, 0.4d, 2.5d, 1.9d, 0.6d, 0.4d};
		double[] const_washer_wkdays_UR={0.011d, 0.005d, 0.002d, 0.001d, 0.025d, 0.016d, 0.004d, 0.003d, 0.016d, 0.004d, 0.005d, 0.02d, 0.039d, 0.032d, 0.072d, 0.081d, 0.186d, 0.262d, 0.257d, 0.279d, 0.26d, 0.239d, 0.216d, 0.18d, 0.171d, 0.164d, 0.127d, 0.116d, 0.131d, 0.116d, 0.103d, 0.108d, 0.112d, 0.127d, 0.125d, 0.123d, 0.113d, 0.114d, 0.119d, 0.098d, 0.1d, 0.094d, 0.089d, 0.08d, 0.05d, 0.036d, 0.031d, 0.016d};
		double[] stddev_washer_wkdays_UR={0.027d, 0.018d, 0.01d, 0.006d, 0.044d, 0.038d, 0.014d, 0.007d, 0.043d, 0.014d, 0.02d, 0.039d, 0.054d, 0.041d, 0.059d, 0.071d, 0.13d, 0.148d, 0.12d, 0.133d, 0.134d, 0.134d, 0.127d, 0.117d, 0.12d, 0.126d, 0.096d, 0.099d, 0.109d, 0.089d, 0.078d, 0.086d, 0.087d, 0.099d, 0.1d, 0.094d, 0.092d, 0.096d, 0.091d, 0.082d, 0.089d, 0.082d, 0.078d, 0.086d, 0.055d, 0.049d, 0.051d, 0.031d};
		// dryer parameters
		double[] scale_dryer_mon_UR={0.017d, 0.014d, 0.008d, 0d, 0d, 0d, 0d, 0.004d, 0d, 0d, 0.01d, 0.022d, 0.035d, 0.026d, 0.034d, 0.019d, 0.005d, 0.027d, 0.04d, 0.032d, 0.051d, 0.048d, 0.107d, 0.116d, 0.058d, 0.124d, 0.125d, 0.058d, 0.117d, 0.103d, 0.086d, 0.088d, 0.11d, 0.124d, 0.118d, 0.156d, 0.162d, 0.1d, 0.024d, 0.034d, 0.042d, 0.024d, 0.045d, 0.009d, 0.011d, 0.036d, 0.009d, 0.023d};
		double[] phase_dryer_mon_UR={3.7d, 3.5d, 3.8d, 0d, 0d, 0d, 0d, 4d, 0d, 0d, 4.8d, 5.6d, 6.2d, 5.9d, 5.4d, 5.5d, 3.5d, 3.9d, 5.3d, 5.3d, 4.3d, 4.6d, 5.4d, 5.5d, 4.9d, 5.2d, 5.2d, 5.2d, 4.9d, 4.6d, 4.5d, 4.9d, 5.2d, 5.2d, 4.8d, 4.7d, 4.8d, 4.8d, 4.8d, 4.2d, 4.7d, 4.7d, 5.5d, 5.3d, 0.2d, 0.2d, 3.4d, 3.1d};
		double[] const_dryer_mon_UR={0.028d, 0.012d, 0.006d, 0.001d, 0.001d, 0.001d, 0.001d, 0.003d, 0.005d, 0.005d, 0.015d, 0.031d, 0.046d, 0.043d, 0.05d, 0.073d, 0.092d, 0.115d, 0.114d, 0.108d, 0.119d, 0.17d, 0.205d, 0.22d, 0.208d, 0.215d, 0.229d, 0.202d, 0.181d, 0.163d, 0.162d, 0.172d, 0.174d, 0.204d, 0.232d, 0.225d, 0.206d, 0.184d, 0.168d, 0.131d, 0.094d, 0.09d, 0.095d, 0.1d, 0.084d, 0.072d, 0.072d, 0.062d};
		double[] stddev_dryer_mon_UR={0.028d, 0.038d, 0.023d, 0.004d, 0.002d, 0.002d, 0.002d, 0.014d, 0.024d, 0.019d, 0.043d, 0.06d, 0.066d, 0.063d, 0.061d, 0.075d, 0.1d, 0.113d, 0.12d, 0.104d, 0.123d, 0.109d, 0.137d, 0.119d, 0.133d, 0.128d, 0.13d, 0.146d, 0.131d, 0.123d, 0.148d, 0.147d, 0.141d, 0.177d, 0.172d, 0.144d, 0.15d, 0.143d, 0.14d, 0.107d, 0.101d, 0.091d, 0.108d, 0.114d, 0.108d, 0.096d, 0.098d, 0.078d};

		double[] scale_dryer_sat_UR={0.018d, 0.005d, 0.003d, 0.009d, 0.014d, 0.011d, 0.005d, 0d, 0d, 0d, 0.009d, 0.005d, 0.023d, 0.016d, 0.016d, 0.021d, 0.017d, 0.023d, 0.04d, 0.029d, 0.043d, 0.037d, 0.116d, 0.148d, 0.078d, 0.078d, 0.103d, 0.094d, 0.112d, 0.101d, 0.101d, 0.106d, 0.074d, 0.087d, 0.065d, 0.108d, 0.087d, 0.107d, 0.084d, 0.052d, 0.028d, 0.067d, 0.032d, 0.029d, 0.02d, 0.004d, 0.012d, 0.005d};
		double[] phase_dryer_sat_UR={3.9d, 4.5d, 4.4d, 4.7d, 5.4d, 5.4d, 5.4d, 0d, 0d, 0d, 5.3d, 4.7d, 5.5d, 5.9d, 0d, 0.5d, 4.4d, 5.9d, 5.6d, 4.6d, 5.6d, 5.2d, 5.2d, 5.3d, 4.8d, 4.8d, 4.4d, 4.9d, 5d, 4.7d, 4.9d, 4.9d, 4.7d, 4.7d, 5.2d, 5.1d, 4.9d, 4.9d, 5.2d, 5.2d, 6.2d, 6.2d, 5.9d, 3.9d, 4.5d, 3.5d, 3.2d, 4d};
		double[] const_dryer_sat_UR={0.018d, 0.009d, 0.015d, 0.011d, 0.009d, 0.007d, 0.004d, 0.001d, 0.003d, 0.002d, 0.007d, 0.01d, 0.024d, 0.024d, 0.023d, 0.037d, 0.075d, 0.081d, 0.102d, 0.132d, 0.159d, 0.171d, 0.212d, 0.206d, 0.185d, 0.194d, 0.198d, 0.219d, 0.175d, 0.197d, 0.208d, 0.197d, 0.163d, 0.174d, 0.185d, 0.205d, 0.178d, 0.168d, 0.167d, 0.119d, 0.093d, 0.103d, 0.096d, 0.08d, 0.058d, 0.039d, 0.043d, 0.038d};
		double[] stddev_dryer_sat_UR={0.044d, 0.029d, 0.045d, 0.04d, 0.035d, 0.032d, 0.019d, 0.002d, 0.019d, 0.009d, 0.032d, 0.032d, 0.065d, 0.051d, 0.048d, 0.061d, 0.085d, 0.084d, 0.12d, 0.114d, 0.129d, 0.128d, 0.156d, 0.163d, 0.145d, 0.16d, 0.159d, 0.166d, 0.144d, 0.154d, 0.165d, 0.154d, 0.126d, 0.119d, 0.142d, 0.152d, 0.129d, 0.162d, 0.165d, 0.14d, 0.107d, 0.105d, 0.114d, 0.104d, 0.097d, 0.064d, 0.072d, 0.068d};

		double[] scale_dryer_sun_UR={0.013d, 0.006d, 0.003d, 0.006d, 0.005d, 0.003d, 0d, 0d, 0d, 0d, 0d, 0.017d, 0.005d, 0.007d, 0.032d, 0.057d, 0.043d, 0.045d, 0.057d, 0.064d, 0.034d, 0.09d, 0.072d, 0.115d, 0.17d, 0.134d, 0.08d, 0.099d, 0.092d, 0.1d, 0.091d, 0.155d, 0.161d, 0.158d, 0.199d, 0.202d, 0.168d, 0.121d, 0.154d, 0.098d, 0.118d, 0.129d, 0.063d, 0.084d, 0.044d, 0.014d, 0.024d, 0.017d};
		double[] phase_dryer_sun_UR={5.8d, 4.7d, 5.3d, 6.1d, 5.5d, 5.4d, 0d, 0d, 0d, 0d, 0d, 3.3d, 4d, 2.4d, 4.9d, 5.1d, 5.2d, 5.5d, 5.3d, 5.2d, 5.2d, 5.1d, 5.1d, 4.8d, 4.7d, 4.7d, 5d, 4.6d, 4.9d, 4.8d, 4.4d, 4.7d, 4.6d, 4.8d, 5d, 4.8d, 4.7d, 4.7d, 4.7d, 4.5d, 4.7d, 4.8d, 4.3d, 4.1d, 3.6d, 3d, 3.3d, 3.8d};
		double[] const_dryer_sun_UR={0.021d, 0.018d, 0.012d, 0.011d, 0.009d, 0.004d, 0.001d, 0.003d, 0.003d, 0.002d, 0.006d, 0.012d, 0.019d, 0.022d, 0.046d, 0.068d, 0.069d, 0.075d, 0.083d, 0.121d, 0.16d, 0.204d, 0.208d, 0.232d, 0.253d, 0.214d, 0.226d, 0.219d, 0.195d, 0.173d, 0.169d, 0.195d, 0.207d, 0.261d, 0.257d, 0.25d, 0.223d, 0.184d, 0.194d, 0.196d, 0.164d, 0.14d, 0.137d, 0.149d, 0.131d, 0.088d, 0.09d, 0.046d};
		double[] stddev_dryer_sun_UR={0.045d, 0.047d, 0.031d, 0.036d, 0.038d, 0.014d, 0.002d, 0.013d, 0.015d, 0.013d, 0.025d, 0.034d, 0.046d, 0.048d, 0.077d, 0.083d, 0.08d, 0.086d, 0.093d, 0.105d, 0.121d, 0.129d, 0.131d, 0.131d, 0.136d, 0.152d, 0.153d, 0.145d, 0.147d, 0.13d, 0.149d, 0.155d, 0.178d, 0.185d, 0.185d, 0.156d, 0.146d, 0.127d, 0.132d, 0.128d, 0.125d, 0.111d, 0.116d, 0.109d, 0.102d, 0.09d, 0.108d, 0.067d};

		double[] scale_dryer_wkdays_UR={0.01d, 0.005d, 0.005d, 0.004d, 0d, 0.001d, 0.001d, 0d, 0d, 0d, 0d, 0.033d, 0.035d, 0.027d, 0.025d, 0.028d, 0.03d, 0.033d, 0.05d, 0.06d, 0.075d, 0.077d, 0.069d, 0.08d, 0.088d, 0.104d, 0.095d, 0.08d, 0.09d, 0.082d, 0.068d, 0.099d, 0.094d, 0.109d, 0.093d, 0.086d, 0.081d, 0.046d, 0.019d, 0.024d, 0.038d, 0.027d, 0.01d, 0.004d, 0.01d, 0.013d, 0.02d, 0.018d};
		double[] phase_dryer_wkdays_UR={2.8d, 3.5d, 3.4d, 4.3d, 0d, 3.7d, 3.7d, 0d, 0d, 0d, 3.7d, 5.6d, 5.6d, 5.5d, 5.7d, 6.1d, 5.9d, 4.7d, 5.1d, 4.9d, 5d, 5d, 5d, 5.2d, 5.2d, 5.3d, 5.1d, 5.1d, 5d, 4.8d, 5.1d, 5.2d, 4.9d, 5.2d, 5.3d, 5.4d, 5.1d, 5.3d, 5.3d, 0.1d, 0.1d, 0.1d, 0.6d, 5.2d, 3.1d, 2.2d, 2.5d, 2.6d};
		double[] const_dryer_wkdays_UR={0.028d, 0.017d, 0.01d, 0.006d, 0.007d, 0.002d, 0.002d, 0.003d, 0.002d, 0.004d, 0.015d, 0.029d, 0.042d, 0.052d, 0.06d, 0.073d, 0.087d, 0.085d, 0.082d, 0.092d, 0.121d, 0.137d, 0.143d, 0.133d, 0.145d, 0.154d, 0.139d, 0.126d, 0.129d, 0.126d, 0.116d, 0.12d, 0.136d, 0.158d, 0.163d, 0.156d, 0.135d, 0.129d, 0.107d, 0.103d, 0.092d, 0.085d, 0.079d, 0.082d, 0.079d, 0.061d, 0.055d, 0.038d};
		double[] stddev_dryer_wkdays_UR={0.062d, 0.049d, 0.033d, 0.028d, 0.043d, 0.012d, 0.016d, 0.018d, 0.013d, 0.019d, 0.05d, 0.065d, 0.076d, 0.081d, 0.08d, 0.085d, 0.102d, 0.096d, 0.101d, 0.117d, 0.118d, 0.121d, 0.125d, 0.118d, 0.138d, 0.131d, 0.134d, 0.129d, 0.141d, 0.139d, 0.121d, 0.117d, 0.124d, 0.128d, 0.131d, 0.121d, 0.119d, 0.127d, 0.117d, 0.111d, 0.101d, 0.094d, 0.096d, 0.096d, 0.09d, 0.08d, 0.078d, 0.065d};
		//dishwasher parameters
		double[] scale_dish_UR={0.004d, 0.015d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.001d, 0.012d, 0.016d, 0d, 0d, 0.005d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.022d, 0.022d, 0.017d, 0.014d, 0.006d, 0.016d, 0.015d, 0.017d, 0.025d, 0.009d, 0.037d, 0.024d, 0.024d, 0.001d, 0.015d, 0.012d, 0.004d, 0.005d};
		double[] phase_dish_UR={2.6d, 2.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 2d, 1d, 0.5d, 0d, 0d, 3.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 5.2d, 5.1d, 5.3d, 4.7d, 5.2d, 5d, 5d, 5.5d, 5d, 5.8d, 5.7d, 0d, 0.1d, 0d, 0d, 5.7d, 4.4d, 5.5d};
		double[] const_dish_UR={0.058d, 0.053d, 0.017d, 0.009d, 0.008d, 0.006d, 0.006d, 0.003d, 0.001d, 0.001d, 0.001d, 0.001d, 0.001d, 0.002d, 0.009d, 0.025d, 0.072d, 0.104d, 0.114d, 0.117d, 0.137d, 0.128d, 0.094d, 0.068d, 0.06d, 0.051d, 0.061d, 0.079d, 0.083d, 0.085d, 0.078d, 0.068d, 0.06d, 0.056d, 0.053d, 0.061d, 0.067d, 0.094d, 0.143d, 0.196d, 0.212d, 0.195d, 0.182d, 0.187d, 0.172d, 0.13d, 0.093d, 0.068d};
		double[] stddev_dish_UR={0.071d, 0.053d, 0.036d, 0.032d, 0.03d, 0.023d, 0.025d, 0.016d, 0.007d, 0.006d, 0.011d, 0.013d, 0.007d, 0.015d, 0.034d, 0.053d, 0.094d, 0.114d, 0.112d, 0.106d, 0.113d, 0.11d, 0.102d, 0.088d, 0.087d, 0.073d, 0.082d, 0.098d, 0.102d, 0.112d, 0.101d, 0.094d, 0.088d, 0.081d, 0.086d, 0.095d, 0.091d, 0.095d, 0.133d, 0.146d, 0.14d, 0.134d, 0.141d, 0.132d, 0.125d, 0.111d, 0.091d, 0.08d};

		double[] scale_dish_E7={0.063d, 0.062d, 0.062d, 0.073d, 0.084d, 0.072d, 0.08d, 0.057d, 0.007d, 0.042d, 0.083d, 0.006d, 0d, 0.002d, 0.005d, 0.002d, 0.007d, 0.022d, 0.008d, 0.019d, 0.026d, 0.037d, 0.034d, 0.011d, 0.068d, 0.021d, 0.015d, 0.008d, 0.015d, 0.022d, 0.024d, 0.018d, 0.018d, 0.011d, 0.012d, 0.01d, 0.005d, 0.017d, 0.043d, 0.026d, 0.026d, 0.009d, 0.007d, 0.006d, 0.004d, 0.01d, 0.015d, 0.029d};
		double[] phase_dish_E7={3.8d, 4.2d, 3.8d, 4.4d, 6.1d, 0.3d, 0.9d, 1.4d, 0.2d, 6.1d, 6d, 6d, 0d, 6d, 0.9d, 0d, 5.2d, 4.9d, 4.4d, 4.9d, 4.4d, 4.7d, 4.5d, 4.8d, 4.7d, 5d, 4.8d, 5d, 4.7d, 4.3d, 4.4d, 4.7d, 4.8d, 4.7d, 4.6d, 4.8d, 4.7d, 4.7d, 4.6d, 4.5d, 4.3d, 3.6d, 3.8d, 3.4d, 4.1d, 3.9d, 3.2d, 3.7d};
		double[] const_dish_E7={0.085d, 0.092d, 0.12d, 0.157d, 0.166d, 0.095d, 0.081d, 0.061d, 0.073d, 0.097d, 0.102d, 0.015d, 0.012d, 0.018d, 0.048d, 0.067d, 0.087d, 0.075d, 0.087d, 0.106d, 0.112d, 0.11d, 0.091d, 0.062d, 0.068d, 0.064d, 0.061d, 0.076d, 0.1d, 0.115d, 0.087d, 0.062d, 0.048d, 0.043d, 0.04d, 0.037d, 0.046d, 0.058d, 0.1d, 0.117d, 0.125d, 0.098d, 0.07d, 0.048d, 0.047d, 0.065d, 0.12d, 0.12d};
		double[] stddev_dish_E7={0.128d, 0.125d, 0.119d, 0.15d, 0.146d, 0.108d, 0.118d, 0.094d, 0.097d, 0.109d, 0.11d, 0.05d, 0.051d, 0.061d, 0.109d, 0.123d, 0.134d, 0.121d, 0.141d, 0.151d, 0.151d, 0.145d, 0.141d, 0.107d, 0.125d, 0.124d, 0.112d, 0.126d, 0.15d, 0.17d, 0.144d, 0.127d, 0.114d, 0.097d, 0.094d, 0.089d, 0.1d, 0.112d, 0.149d, 0.155d, 0.152d, 0.138d, 0.124d, 0.106d, 0.102d, 0.12d, 0.146d, 0.151d};

		for (int i = 0; i < numDays; i++)
		{
			//washing demand for Mondays:
			if (i%Consts.DAYS_PER_WEEK == 1)
			{
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]= (washMach + washDry) * Math.max(0, scale_washer_mon_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_mon_UR[HH])+const_washer_mon_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_washer_mon_UR[HH]) ); 
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(tumbleDry + washDry) *  Math.max(0, scale_dryer_mon_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_mon_UR[HH])+const_dryer_mon_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_mon_UR[HH])  );
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=dishWash * Math.max(0, scale_dish_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[HH])+const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[HH])  );
				}
			}
			//washing demand for Sundays:
			else if (i%Consts.DAYS_PER_WEEK == 0)
			{
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(washMach + washDry) *  Math.max(0, scale_washer_sun_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_sun_UR[HH])+const_washer_sun_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_washer_sun_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(tumbleDry + washDry) * Math.max(0, scale_dryer_sun_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_sun_UR[HH])+const_dryer_sun_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_sun_UR[HH]) );   
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=dishWash * Math.max(0, scale_dish_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[HH])+const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[HH]) ) ;
				}
			}
			//washing demand for Saturdays:
			else if (i%Consts.DAYS_PER_WEEK == 6)
			{
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(washMach + washDry) * Math.max(0, scale_washer_sat_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_sat_UR[HH])+const_washer_sat_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_washer_sat_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(tumbleDry + washDry) * Math.max(0, scale_dryer_sat_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_sat_UR[HH])+const_dryer_sat_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_sat_UR[HH]) );   
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=dishWash * Math.max(0, scale_dish_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[HH])+const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[HH])  );
				}
			}
			else
			{
				for (int HH = 0; HH < 48; HH++)
				{
					d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(washMach + washDry) *  Math.max(0, scale_washer_wkdays_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_wkdays_UR[HH])+const_washer_wkdays_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_washer_wkdays_UR[HH]));
					d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=(tumbleDry + washDry) * Math.max(0, scale_dryer_wkdays_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_wkdays_UR[HH])+const_dryer_wkdays_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_wkdays_UR[HH])) ;   
					d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +HH]=dishWash * Math.max(0, scale_dish_UR[HH]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[HH])+const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[HH])) ;               
				}
			}
		}

	return ArrayUtils.add(d_washer_UR, d_dryer_UR, d_dish_UR);

}
	
	
	/**
	 * @param numDays
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 * @return
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	private static double[] melodyStokesWetApplianceGen_old2(int numDays, int washMach,
			int washDry, int dishWash, int tumbleDry) {
		// Each day of week is treated the same way! 
		//this sub-model is for half-hourly electricty demand for washing appliances
		//it covers washers, dryers, washer-dryers combined and dishwashers

		final double[] wet_pdf = {18.91,16.45,13.49,12.52,16.80,14.41,11.13,9.99,13.90,10.18,13.30,15.53,18.79,17.65,21.79,25.72,36.83,43.13,43.94,46.43,49.61,52.02,49.30,45.71,42.85,42.42,39.08,39.67,41.19,40.16,37.68,37.56,37.67,38.10,38.19,37.10,36.46,37.32,39.44,37.77,37.05,35.09,35.13,34.19,29.75,26.68,26.01,21.30};
		EmpiricalWalker wetApplProbDistGenerator = RandomHelper.createEmpiricalWalker(wet_pdf, Empirical.NO_INTERPOLATION); //temporarily used for test

		//ChartUtils.testProbabilityDistAndShowHistogram(wetApplProbDistGenerator, 10000, 48);

		double[] d_washer_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dryer_UR =new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dish_UR =new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		double[] scale_washer_wkdays_UR={0.002d, 0.001d, 0d, 0d, 0.022d, 0.01d, 0d, 0.003d, 0.026d, 0d, 0d, 0.005d, 0.002d, 0.004d, 0.029d, 0.013d, 0.046d, 0.029d, 0.006d, 0.023d, 0.009d, 0.029d, 0.019d, 0.014d, 0.014d, 0.009d, 0.007d, 0.022d, 0.019d, 0.009d, 0.01d, 0.015d, 0.021d, 0.01d, 0.017d, 0.018d, 0.018d, 0.025d, 0.022d, 0.01d, 0.034d, 0.023d, 0.025d, 0.018d, 0.003d, 0.002d, 0.003d, 0.002d};
		double[] phase_washer_wkdays_UR={1.4d, 1.4d, 0d, 0d, 2.4d, 2.6d, 0d, 5d, 5.4d, 0d, 0d, 2.9d, 4.2d, 2.8d, 0.5d, 0.9d, 1d, 6.2d, 2d, 5.9d, 0d, 0d, 5.9d, 0.5d, 4.6d, 4.4d, 5.2d, 0d, 5.5d, 4.9d, 5.8d, 5.4d, 6d, 5.5d, 6d, 6.2d, 6d, 4.9d, 5.3d, 6d, 6d, 0.5d, 0.7d, 0.4d, 2.5d, 1.9d, 0.6d, 0.4d};
		double[] const_washer_wkdays_UR={0.011d, 0.005d, 0.002d, 0.001d, 0.025d, 0.016d, 0.004d, 0.003d, 0.016d, 0.004d, 0.005d, 0.02d, 0.039d, 0.032d, 0.072d, 0.081d, 0.186d, 0.262d, 0.257d, 0.279d, 0.26d, 0.239d, 0.216d, 0.18d, 0.171d, 0.164d, 0.127d, 0.116d, 0.131d, 0.116d, 0.103d, 0.108d, 0.112d, 0.127d, 0.125d, 0.123d, 0.113d, 0.114d, 0.119d, 0.098d, 0.1d, 0.094d, 0.089d, 0.08d, 0.05d, 0.036d, 0.031d, 0.016d};
		double[] stddev_washer_wkdays_UR={0.027d, 0.018d, 0.01d, 0.006d, 0.044d, 0.038d, 0.014d, 0.007d, 0.043d, 0.014d, 0.02d, 0.039d, 0.054d, 0.041d, 0.059d, 0.071d, 0.13d, 0.148d, 0.12d, 0.133d, 0.134d, 0.134d, 0.127d, 0.117d, 0.12d, 0.126d, 0.096d, 0.099d, 0.109d, 0.089d, 0.078d, 0.086d, 0.087d, 0.099d, 0.1d, 0.094d, 0.092d, 0.096d, 0.091d, 0.082d, 0.089d, 0.082d, 0.078d, 0.086d, 0.055d, 0.049d, 0.051d, 0.031d};

		double[] scale_dryer_wkdays_UR={0.01d, 0.005d, 0.005d, 0.004d, 0d, 0.001d, 0.001d, 0d, 0d, 0d, 0d, 0.033d, 0.035d, 0.027d, 0.025d, 0.028d, 0.03d, 0.033d, 0.05d, 0.06d, 0.075d, 0.077d, 0.069d, 0.08d, 0.088d, 0.104d, 0.095d, 0.08d, 0.09d, 0.082d, 0.068d, 0.099d, 0.094d, 0.109d, 0.093d, 0.086d, 0.081d, 0.046d, 0.019d, 0.024d, 0.038d, 0.027d, 0.01d, 0.004d, 0.01d, 0.013d, 0.02d, 0.018d};
		double[] phase_dryer_wkdays_UR={2.8d, 3.5d, 3.4d, 4.3d, 0d, 3.7d, 3.7d, 0d, 0d, 0d, 3.7d, 5.6d, 5.6d, 5.5d, 5.7d, 6.1d, 5.9d, 4.7d, 5.1d, 4.9d, 5d, 5d, 5d, 5.2d, 5.2d, 5.3d, 5.1d, 5.1d, 5d, 4.8d, 5.1d, 5.2d, 4.9d, 5.2d, 5.3d, 5.4d, 5.1d, 5.3d, 5.3d, 0.1d, 0.1d, 0.1d, 0.6d, 5.2d, 3.1d, 2.2d, 2.5d, 2.6d};
		double[] const_dryer_wkdays_UR={0.028d, 0.017d, 0.01d, 0.006d, 0.007d, 0.002d, 0.002d, 0.003d, 0.002d, 0.004d, 0.015d, 0.029d, 0.042d, 0.052d, 0.06d, 0.073d, 0.087d, 0.085d, 0.082d, 0.092d, 0.121d, 0.137d, 0.143d, 0.133d, 0.145d, 0.154d, 0.139d, 0.126d, 0.129d, 0.126d, 0.116d, 0.12d, 0.136d, 0.158d, 0.163d, 0.156d, 0.135d, 0.129d, 0.107d, 0.103d, 0.092d, 0.085d, 0.079d, 0.082d, 0.079d, 0.061d, 0.055d, 0.038d};
		double[] stddev_dryer_wkdays_UR={0.062d, 0.049d, 0.033d, 0.028d, 0.043d, 0.012d, 0.016d, 0.018d, 0.013d, 0.019d, 0.05d, 0.065d, 0.076d, 0.081d, 0.08d, 0.085d, 0.102d, 0.096d, 0.101d, 0.117d, 0.118d, 0.121d, 0.125d, 0.118d, 0.138d, 0.131d, 0.134d, 0.129d, 0.141d, 0.139d, 0.121d, 0.117d, 0.124d, 0.128d, 0.131d, 0.121d, 0.119d, 0.127d, 0.117d, 0.111d, 0.101d, 0.094d, 0.096d, 0.096d, 0.09d, 0.08d, 0.078d, 0.065d};
		//dishwasher parameters
		double[] scale_dish_UR={0.004d, 0.015d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.001d, 0.012d, 0.016d, 0d, 0d, 0.005d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.022d, 0.022d, 0.017d, 0.014d, 0.006d, 0.016d, 0.015d, 0.017d, 0.025d, 0.009d, 0.037d, 0.024d, 0.024d, 0.001d, 0.015d, 0.012d, 0.004d, 0.005d};
		double[] phase_dish_UR={2.6d, 2.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 2d, 1d, 0.5d, 0d, 0d, 3.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 5.2d, 5.1d, 5.3d, 4.7d, 5.2d, 5d, 5d, 5.5d, 5d, 5.8d, 5.7d, 0d, 0.1d, 0d, 0d, 5.7d, 4.4d, 5.5d};
		double[] const_dish_UR={0.058d, 0.053d, 0.017d, 0.009d, 0.008d, 0.006d, 0.006d, 0.003d, 0.001d, 0.001d, 0.001d, 0.001d, 0.001d, 0.002d, 0.009d, 0.025d, 0.072d, 0.104d, 0.114d, 0.117d, 0.137d, 0.128d, 0.094d, 0.068d, 0.06d, 0.051d, 0.061d, 0.079d, 0.083d, 0.085d, 0.078d, 0.068d, 0.06d, 0.056d, 0.053d, 0.061d, 0.067d, 0.094d, 0.143d, 0.196d, 0.212d, 0.195d, 0.182d, 0.187d, 0.172d, 0.13d, 0.093d, 0.068d};
		double[] stddev_dish_UR={0.071d, 0.053d, 0.036d, 0.032d, 0.03d, 0.023d, 0.025d, 0.016d, 0.007d, 0.006d, 0.011d, 0.013d, 0.007d, 0.015d, 0.034d, 0.053d, 0.094d, 0.114d, 0.112d, 0.106d, 0.113d, 0.11d, 0.102d, 0.088d, 0.087d, 0.073d, 0.082d, 0.098d, 0.102d, 0.112d, 0.101d, 0.094d, 0.088d, 0.081d, 0.086d, 0.095d, 0.091d, 0.095d, 0.133d, 0.146d, 0.14d, 0.134d, 0.141d, 0.132d, 0.125d, 0.111d, 0.091d, 0.08d};

		int timeslot;

		for (int i = 0; i < numDays; i++)	{
			timeslot = wetApplProbDistGenerator.nextInt();
			d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=(washMach + washDry) *  Math.max(0, scale_washer_wkdays_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_wkdays_UR[timeslot])+const_washer_wkdays_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_washer_wkdays_UR[timeslot]));
			d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=(tumbleDry + washDry) * Math.max(0, scale_dryer_wkdays_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_wkdays_UR[timeslot])+const_dryer_wkdays_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_wkdays_UR[timeslot])) ;
			timeslot = wetApplProbDistGenerator.nextInt();
			d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=dishWash * Math.max(0, scale_dish_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[timeslot])+const_dish_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[timeslot])) ;               
		}

		return ArrayUtils.add(d_washer_UR, d_dryer_UR, d_dish_UR);

	}
	
	
	/**
	 * @param numDays
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 * @return
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	private static HashMap melodyStokesWetApplianceGen(CascadeContext context,int numDays, int washMach,
			int washDry, int dishWash, int tumbleDry) {
		// Each day of week is treated the same way! 
		//this sub-model is for half-hourly electricty demand for washing appliances
		//it covers washers, dryers, washer-dryers combined and dishwashers

		//final double[] wet_pdf = {18.91,16.45,13.49,12.52,16.80,14.41,11.13,9.99,13.90,10.18,13.30,15.53,18.79,17.65,21.79,25.72,36.83,43.13,43.94,46.43,49.61,52.02,49.30,45.71,42.85,42.42,39.08,39.67,41.19,40.16,37.68,37.56,37.67,38.10,38.19,37.10,36.46,37.32,39.44,37.77,37.05,35.09,35.13,34.19,29.75,26.68,26.01,21.30};
		//EmpiricalWalker wetApplProbDistGenerator = RandomHelper.createEmpiricalWalker(wet_pdf, Empirical.NO_INTERPOLATION); 

		//ChartUtils.testProbabilityDistAndShowHistogram(wetApplProbDistGenerator, 10000, 48);  //test to make sure the prob dist generate desired outcomes

		double[] d_washer_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dryer_UR =new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[] d_dish_UR =new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		double[] scale_washer_wkdays_UR={0.002d, 0.001d, 0d, 0d, 0.022d, 0.01d, 0d, 0.003d, 0.026d, 0d, 0d, 0.005d, 0.002d, 0.004d, 0.029d, 0.013d, 0.046d, 0.029d, 0.006d, 0.023d, 0.009d, 0.029d, 0.019d, 0.014d, 0.014d, 0.009d, 0.007d, 0.022d, 0.019d, 0.009d, 0.01d, 0.015d, 0.021d, 0.01d, 0.017d, 0.018d, 0.018d, 0.025d, 0.022d, 0.01d, 0.034d, 0.023d, 0.025d, 0.018d, 0.003d, 0.002d, 0.003d, 0.002d};
		double[] phase_washer_wkdays_UR={1.4d, 1.4d, 0d, 0d, 2.4d, 2.6d, 0d, 5d, 5.4d, 0d, 0d, 2.9d, 4.2d, 2.8d, 0.5d, 0.9d, 1d, 6.2d, 2d, 5.9d, 0d, 0d, 5.9d, 0.5d, 4.6d, 4.4d, 5.2d, 0d, 5.5d, 4.9d, 5.8d, 5.4d, 6d, 5.5d, 6d, 6.2d, 6d, 4.9d, 5.3d, 6d, 6d, 0.5d, 0.7d, 0.4d, 2.5d, 1.9d, 0.6d, 0.4d};
		double[] const_washer_wkdays_UR={0.011d, 0.005d, 0.002d, 0.001d, 0.025d, 0.016d, 0.004d, 0.003d, 0.016d, 0.004d, 0.005d, 0.02d, 0.039d, 0.032d, 0.072d, 0.081d, 0.186d, 0.262d, 0.257d, 0.279d, 0.26d, 0.239d, 0.216d, 0.18d, 0.171d, 0.164d, 0.127d, 0.116d, 0.131d, 0.116d, 0.103d, 0.108d, 0.112d, 0.127d, 0.125d, 0.123d, 0.113d, 0.114d, 0.119d, 0.098d, 0.1d, 0.094d, 0.089d, 0.08d, 0.05d, 0.036d, 0.031d, 0.016d};
		double[] stddev_washer_wkdays_UR={0.027d, 0.018d, 0.01d, 0.006d, 0.044d, 0.038d, 0.014d, 0.007d, 0.043d, 0.014d, 0.02d, 0.039d, 0.054d, 0.041d, 0.059d, 0.071d, 0.13d, 0.148d, 0.12d, 0.133d, 0.134d, 0.134d, 0.127d, 0.117d, 0.12d, 0.126d, 0.096d, 0.099d, 0.109d, 0.089d, 0.078d, 0.086d, 0.087d, 0.099d, 0.1d, 0.094d, 0.092d, 0.096d, 0.091d, 0.082d, 0.089d, 0.082d, 0.078d, 0.086d, 0.055d, 0.049d, 0.051d, 0.031d};

		double[] scale_dryer_wkdays_UR={0.01d, 0.005d, 0.005d, 0.004d, 0d, 0.001d, 0.001d, 0d, 0d, 0d, 0d, 0.033d, 0.035d, 0.027d, 0.025d, 0.028d, 0.03d, 0.033d, 0.05d, 0.06d, 0.075d, 0.077d, 0.069d, 0.08d, 0.088d, 0.104d, 0.095d, 0.08d, 0.09d, 0.082d, 0.068d, 0.099d, 0.094d, 0.109d, 0.093d, 0.086d, 0.081d, 0.046d, 0.019d, 0.024d, 0.038d, 0.027d, 0.01d, 0.004d, 0.01d, 0.013d, 0.02d, 0.018d};
		double[] phase_dryer_wkdays_UR={2.8d, 3.5d, 3.4d, 4.3d, 0d, 3.7d, 3.7d, 0d, 0d, 0d, 3.7d, 5.6d, 5.6d, 5.5d, 5.7d, 6.1d, 5.9d, 4.7d, 5.1d, 4.9d, 5d, 5d, 5d, 5.2d, 5.2d, 5.3d, 5.1d, 5.1d, 5d, 4.8d, 5.1d, 5.2d, 4.9d, 5.2d, 5.3d, 5.4d, 5.1d, 5.3d, 5.3d, 0.1d, 0.1d, 0.1d, 0.6d, 5.2d, 3.1d, 2.2d, 2.5d, 2.6d};
		double[] const_dryer_wkdays_UR={0.028d, 0.017d, 0.01d, 0.006d, 0.007d, 0.002d, 0.002d, 0.003d, 0.002d, 0.004d, 0.015d, 0.029d, 0.042d, 0.052d, 0.06d, 0.073d, 0.087d, 0.085d, 0.082d, 0.092d, 0.121d, 0.137d, 0.143d, 0.133d, 0.145d, 0.154d, 0.139d, 0.126d, 0.129d, 0.126d, 0.116d, 0.12d, 0.136d, 0.158d, 0.163d, 0.156d, 0.135d, 0.129d, 0.107d, 0.103d, 0.092d, 0.085d, 0.079d, 0.082d, 0.079d, 0.061d, 0.055d, 0.038d};
		double[] stddev_dryer_wkdays_UR={0.062d, 0.049d, 0.033d, 0.028d, 0.043d, 0.012d, 0.016d, 0.018d, 0.013d, 0.019d, 0.05d, 0.065d, 0.076d, 0.081d, 0.08d, 0.085d, 0.102d, 0.096d, 0.101d, 0.117d, 0.118d, 0.121d, 0.125d, 0.118d, 0.138d, 0.131d, 0.134d, 0.129d, 0.141d, 0.139d, 0.121d, 0.117d, 0.124d, 0.128d, 0.131d, 0.121d, 0.119d, 0.127d, 0.117d, 0.111d, 0.101d, 0.094d, 0.096d, 0.096d, 0.09d, 0.08d, 0.078d, 0.065d};
		//dishwasher parameters
		double[] scale_dish_UR={0.004d, 0.015d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.001d, 0.012d, 0.016d, 0d, 0d, 0.005d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.022d, 0.022d, 0.017d, 0.014d, 0.006d, 0.016d, 0.015d, 0.017d, 0.025d, 0.009d, 0.037d, 0.024d, 0.024d, 0.001d, 0.015d, 0.012d, 0.004d, 0.005d};
		double[] phase_dish_UR={2.6d, 2.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 2d, 1d, 0.5d, 0d, 0d, 3.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 5.2d, 5.1d, 5.3d, 4.7d, 5.2d, 5d, 5d, 5.5d, 5d, 5.8d, 5.7d, 0d, 0.1d, 0d, 0d, 5.7d, 4.4d, 5.5d};
		double[] const_dish_UR={0.058d, 0.053d, 0.017d, 0.009d, 0.008d, 0.006d, 0.006d, 0.003d, 0.001d, 0.001d, 0.001d, 0.001d, 0.001d, 0.002d, 0.009d, 0.025d, 0.072d, 0.104d, 0.114d, 0.117d, 0.137d, 0.128d, 0.094d, 0.068d, 0.06d, 0.051d, 0.061d, 0.079d, 0.083d, 0.085d, 0.078d, 0.068d, 0.06d, 0.056d, 0.053d, 0.061d, 0.067d, 0.094d, 0.143d, 0.196d, 0.212d, 0.195d, 0.182d, 0.187d, 0.172d, 0.13d, 0.093d, 0.068d};
		double[] stddev_dish_UR={0.071d, 0.053d, 0.036d, 0.032d, 0.03d, 0.023d, 0.025d, 0.016d, 0.007d, 0.006d, 0.011d, 0.013d, 0.007d, 0.015d, 0.034d, 0.053d, 0.094d, 0.114d, 0.112d, 0.106d, 0.113d, 0.11d, 0.102d, 0.088d, 0.087d, 0.073d, 0.082d, 0.098d, 0.102d, 0.112d, 0.101d, 0.094d, 0.088d, 0.081d, 0.086d, 0.095d, 0.091d, 0.095d, 0.133d, 0.146d, 0.14d, 0.134d, 0.141d, 0.132d, 0.125d, 0.111d, 0.091d, 0.08d};

		int timeslot;
		
		HashMap wetProfiles = new HashMap();

		for (int i = 0; i < numDays; i++)	{
			timeslot = context.wetApplProbDistGenerator.nextInt();
			d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=(washMach + washDry) *  Math.max(0, scale_washer_wkdays_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_wkdays_UR[timeslot])+const_washer_wkdays_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_washer_wkdays_UR[timeslot]));
			d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=(tumbleDry + washDry) * Math.max(0, scale_dryer_wkdays_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_wkdays_UR[timeslot])+const_dryer_wkdays_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_wkdays_UR[timeslot])) ;
			timeslot = context.wetApplProbDistGenerator.nextInt();
			d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=dishWash * Math.max(0, scale_dish_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[timeslot])+const_dish_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[timeslot])) ;               
		}
		
		
		//Convert kW to kWh
		d_washer_UR = ArrayUtils.multiply(d_washer_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());
		d_dryer_UR = ArrayUtils.multiply(d_dryer_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());
		d_dish_UR = ArrayUtils.multiply(d_dish_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());
		
		wetProfiles.put(Consts.WET_APP_WASHER, d_washer_UR);
		wetProfiles.put(Consts.WET_APP_DRYER, d_dryer_UR);
		wetProfiles.put(Consts.WET_APP_DISHWASHER, d_dish_UR);
		
		return wetProfiles;

	}

}
