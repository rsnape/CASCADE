/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import javax.accessibility.AccessibleContext;

import cern.jet.random.Empirical;

import repast.simphony.context.Context;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.Consts.*;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * This class is a factory for creating instances of <tt>ProsumerAgent</tt> concrete subclasses
 * Its public creator method's signatures are defined by {@link IProsumerFactory} interface.
 *   
 * @author Babak Mahdavi
 * @author Richard Snape
 * @version $Revision: 1.0 $ $Date: 2011/05/16 14:00:00 $ 
 */
public class ProsumerFactory implements IProsumerFactory {
	//Parameters params;
	CascadeContext cascadeMainContext;


	public ProsumerFactory(CascadeContext context) {
		//this.params = par;
		this.cascadeMainContext= context;

	}

	/**
	 * Creates a household prosumer with a basic consumption profile as supplied
	 * with or without added noise
	 * 
	 * NOTE: The addNoise option is unsafe as implemented - under a certain combination of random factors, it can give negative
	 * demands!! DO NOT USE
	 * 
	 * @param baseProfile - an array of the basic consumption profile for this prosumer (kWh per tick)
	 * @param addNoise - boolean specifying whether or not to add noise to the profile
	 */

	public HouseholdProsumer createHouseholdProsumer(float[] baseProfileArray, boolean addNoise) {
		HouseholdProsumer pAgent;
		int ticksPerDay = cascadeMainContext.getTickPerDay();
		if (baseProfileArray.length % ticksPerDay != 0)
		{
			System.err.println("ProsumerFactory: Household base demand array not a whole number of days");
			System.err.println("ProsumerFactory: May cause unexpected behaviour");
		}

		if (addNoise) 
		{
			pAgent = new HouseholdProsumer(cascadeMainContext, createRandomHouseholdDemand(baseProfileArray));
		}
		else
		{
			pAgent = new HouseholdProsumer(cascadeMainContext, baseProfileArray);
		}

		pAgent.exercisesBehaviourChange = (RandomHelper.nextDouble() > (1 - Consts.HOUSEHOLDS_WILLING_TO_CHANGE_BEHAVIOUR));
		//pAgent.hasSmartControl = (RandomHelper.nextDouble() > (1 - Consts.HOUSEHOLDS_WITH_SMART_CONTROL));
		pAgent.exercisesBehaviourChange = true;
		pAgent.hasSmartMeter = true;
		pAgent.costThreshold = Consts.HOUSEHOLD_COST_THRESHOLD;
		pAgent.minSetPoint = Consts.HOUSEHOLD_MIN_SETPOINT;
		pAgent.maxSetPoint = Consts.HOUSEHOLD_MAX_SETPOINT;

		pAgent.transmitPropensitySmartControl = (float) RandomHelper.nextDouble();

		return pAgent;
	}


	/**
	 * Adds a random noise to the base profile to create a household demand.
	 * For amplitude multiplies each point on the base profile by a random float uniformly distributed between 0.7 and 1.3 (arbitrary)
	 * then selects a uniformly distributed time based <code> jitterFactor </code> between -0.5 and + 0.5 and shifts the demand in time by <code> jitterFactor </code> timesteps
	 * 
	 * NOTE: This is unsafe as implemented - under a certain combination of random factors, it can give negative
	 * demands!! DO NOT USE
	 * 
	 * TODO: It should be over-ridden in the future to use something better - for instance melody's model
	 * or something which time-shifts demand somewhat, or select one of a number of typical profiles
	 * based on occupancy.
	 */
	@Deprecated
	private float[] createRandomHouseholdDemand(float[] baseProfileArray){
		float[] newProfile = new float[baseProfileArray.length];

		//add amplitude randomisation
		for (int i = 0; i < newProfile.length; i++)
		{
			newProfile[i] = baseProfileArray[i] * (float)(1 + 0.3*(RandomHelper.nextDouble() - 0.5));
		}

		//add time jitter
		float jitterFactor = (float) RandomHelper.nextDouble() - 0.5f;

		if (Consts.DEBUG)
		{
			System.out.println("ProsumerFactory: Applying jitter" + jitterFactor);
		}

		newProfile[0] = (jitterFactor * newProfile[0]) + ((1 - jitterFactor) * newProfile[newProfile.length - 1]);
		for (int i = 1; i < (newProfile.length - 1); i++)
		{
			newProfile[i] = (jitterFactor * newProfile[i]) + ((1 - jitterFactor) * newProfile[i+1]);
		}
		newProfile[newProfile.length - 1] = (jitterFactor * newProfile[newProfile.length - 1]) + ((1 - jitterFactor) * newProfile[0]);

		return newProfile;
	}


	/*
	 * Creates a prosumer to represent a pure generator.  Therefore zero
	 * base demand, set the generator type and capacity.
	 * 
	 * TODO: should the base demand be zero or null???
	 * 
	 * @param Capacity - the generation capacity of this agent (kWh)
	 * @param type - the type of this generator (from an enumerator of all types)
	 */

	public ProsumerAgent createPureGenerator(float capacity, Consts.GENERATOR_TYPE genType) {
		GeneratorProsumer thisAgent = null;

		// Create a prosumer with zero base demand
		// TODO: Should this be null?  or zero as implemented?
		float[] nilDemand = new float[1];
		nilDemand[0] = 0;
		switch (genType){
		case WIND:
			thisAgent = new WindGeneratorProsumer(cascadeMainContext, nilDemand, capacity);
			break;			
		}

		return thisAgent;
	} 

	public ArrayList<ProsumerAgent> createDEFRAHouseholds(int number, String categoryFile, String profileFile)
	{
		ArrayList<ProsumerAgent> returnList = new ArrayList();

		CSVReader defraCategories = null;
		CSVReader defraProfiles = null;

		try
		{
			defraCategories = new CSVReader(categoryFile);
		}
		catch(FileNotFoundException e)
		{
			System.err.println("ProsumerFactory: File containing DEFRA types not found at "+categoryFile);
			System.err.println("ProsumerFactory: Doesn't look like this will work, terminating");
			System.err.println(e.getMessage());
			System.exit(Consts.BAD_FILE_ERR_CODE);
		}

		try
		{
			defraProfiles = new CSVReader(profileFile);
		}
		catch(FileNotFoundException e)
		{
			System.err.println("ProsumerFactory: File containing average profiles for DEFRA types not found at "+profileFile);
			System.err.println("ProsumerFactory: Doesn't look like this will work, terminating");
			System.err.println(e.getMessage());
			System.exit(Consts.BAD_FILE_ERR_CODE);
		}

		defraCategories.parseByColumn();
		defraProfiles.parseByColumn();

		//Need to think about defining the column names as consts, or otherwise working out
		//How we import files, whether column names are pre-ordained, or arbitrary etc.

		Empirical myDist = RandomHelper.createEmpirical(ArrayUtils.convertStringArrayToDoubleArray(defraCategories.getColumn("Population_fraction")), Empirical.NO_INTERPOLATION);

		for (int j = 0; j < number; j++)
		{

			int custSegment = 0;
			double choiceVar = RandomHelper.nextDouble();
			int i = 0;
			while (custSegment < 1)
			{
				if (choiceVar < myDist.cdf(i))
				{
					custSegment = i;
				}
				i++;
			}

			if (Consts.DEBUG)
			{
				System.out.println("DEFRA Customer segment is"  + custSegment);
			}

			HouseholdProsumer prAgent = this.createHouseholdProsumer(ArrayUtils.convertStringArrayToFloatArray(defraProfiles.getColumn("demand" + (custSegment - 1))), true);

			prAgent.defraCategory = Integer.parseInt(defraCategories.getColumn("DEFRA_category")[custSegment - 1]);
			prAgent.microgenPropensity = Float.parseFloat(defraCategories.getColumn("Microgen_propensity")[custSegment - 1]);
			prAgent.insulationPropensity = Float.parseFloat(defraCategories.getColumn("Insulation_propensity")[custSegment - 1]);
			prAgent.HEMSPropensity = Float.parseFloat(defraCategories.getColumn("HEMS_propensity")[custSegment - 1]);
			prAgent.EVPropensity = Float.parseFloat(defraCategories.getColumn("EV_propensity")[custSegment - 1]);
			prAgent.habit = Float.parseFloat(defraCategories.getColumn("Habit_factor")[custSegment - 1]);

			prAgent.hasSmartControl = true; //(RandomHelper.nextDouble() < prAgent.HEMSPropensity);
			prAgent.hasElectricVehicle =  (RandomHelper.nextDouble() < prAgent.EVPropensity);

			returnList.add(prAgent);
		}

		return returnList;
	}

}
