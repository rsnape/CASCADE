/**
 *
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;


import java.util.WeakHashMap;

import org.apache.commons.lang.ArrayUtils;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;


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
		this.cascadeMainContext= context;
	}

	/**
	 * Creates a household prosumer with a basic consumption profile as supplied
	 * with or without added noise
	 *
	 * NOTE: The addNoise option is unsafe as implemented - under a certain combination of random factors, it can give negative
	 * demands!! DO NOT USE
	 *
	 * @param otherElasticDemandProfile - an array of the basic consumption profile for this prosumer (kWh per tick)
	 * @param addNoise - boolean specifying whether or not to add noise to the profile
	 * @param numOfOccupants - number of occupancy per household, if Consts.RANDOM is passed, it will done randomly  
	 */

	public HouseholdProsumer createHouseholdProsumer(double[] otherDemandProfileArray,  int numOfOccupants, boolean addNoise, boolean hasGas) {
		
		HouseholdProsumer hhProsAgent;
		
		int ticksPerDay = cascadeMainContext.getNbOfTickPerDay();
		
		if (otherDemandProfileArray.length % ticksPerDay != 0)	{
			System.err.println("ProsumerFactory: Household base demand array not a whole number of days");
			System.err.println("ProsumerFactory: May cause unexpected behaviour");
		}

		if (addNoise)		
			hhProsAgent = new HouseholdProsumer(cascadeMainContext, randomizeDemandProfile(otherDemandProfileArray));
		else 		
			hhProsAgent = new HouseholdProsumer(cascadeMainContext, otherDemandProfileArray);

		hhProsAgent.setNumOccupants(numOfOccupants);
		
		hhProsAgent.setHasGas(hasGas);
		
		/*if (hasGas) {
			hhProsAgent.setHasElectricalSpaceHeat(false);
		}
		else {
			hhProsAgent.setHasElectricalSpaceHeat(true);
			hhProsAgent.initializeElecSpaceHeatPar();
		} */
			
		hhProsAgent.costThreshold = Consts.HOUSEHOLD_COST_THRESHOLD;
		hhProsAgent.setPredictedCostSignal(Consts.ZERO_COST_SIGNAL);

		hhProsAgent.transmitPropensitySmartControl = (double) RandomHelper.nextDouble();

		hhProsAgent.initializeRandomlyDailyElasticityArray(0, 0.1);
		
		//hhProsAgent.initializeSimilarlyDailyElasticityArray(0.1d);
		hhProsAgent.setRandomlyPercentageMoveableDemand(0, Consts.MAX_DOMESTIC_MOVEABLE_LOAD_FRACTION);
		
		hhProsAgent.exercisesBehaviourChange = true;
		//pAgent.exercisesBehaviourChange = (RandomHelper.nextDouble() > (1 - Consts.HOUSEHOLDS_WILLING_TO_CHANGE_BEHAVIOUR));
		
		//TODO: We just set smart meter true here - need more sophisticated way to set for different scenarios
		hhProsAgent.hasSmartMeter = true;

		//pAgent.hasSmartControl = (RandomHelper.nextDouble() > (1 - Consts.HOUSEHOLDS_WITH_SMART_CONTROL));
		hhProsAgent.hasSmartControl = true;
		
		//TODO: we need to set up wattbox after appliances added.  This is all a bit
		//non-object oriented.  Could do with a proper design methodology here.
		if (hhProsAgent.hasSmartControl)
			hhProsAgent.setWattboxController();

		return hhProsAgent;
	}
	
	public HouseholdProsumer createHouseholdProsumer(WeakHashMap <Integer, double[]> map_nbOfOcc2OtherDemand, int occupancyModeOrNb, boolean addNoise, boolean hasGas) {
		
		int numOfOccupant = occupancyModeOrNb;
		if (occupancyModeOrNb == Consts.RANDOM) {
			numOfOccupant = cascadeMainContext.occupancyGenerator.nextInt() + 1;
			if (numOfOccupant > map_nbOfOcc2OtherDemand.size())
				numOfOccupant = map_nbOfOcc2OtherDemand.size();
		}
		double[] arr_otherDemand=null;
		if (cascadeMainContext.signalMode == Consts.SIGNAL_MODE_SMART) {
		    /*
		     * If the signal is smart, we use one single profile, currently the one with 2 occupants.
		     * If required, instead we can get find the average of all (different occupancy) and use it instead.
		     * Furthermore, if required, the size of this array should be reduced to one single day (currently left at one year) 
		     */
			arr_otherDemand = map_nbOfOcc2OtherDemand.get(2);
		}
		else arr_otherDemand = map_nbOfOcc2OtherDemand.get(numOfOccupant);
		
		return createHouseholdProsumer(arr_otherDemand,  numOfOccupant, addNoise, hasGas);
		
	}
	

	/**
	 * Adds a random noise to a profile.
	 * For amplitude multiplies each point on the base profile by a random double uniformly distributed between 0.7 and 1.3 (arbitrary)
	 * then selects a uniformly distributed time based <code> jitterFactor </code> between -0.5 and + 0.5 and shifts the demand in time by <code> jitterFactor </code> timesteps
	 *
	 * NOTE: This is unsafe as implemented - under a certain combination of random factors, it can give negative
	 * demands!! DO NOT USE
	 *
	 * TODO: It should be over-ridden in the future to use something better - for instance melody's model
	 * or something which time-shifts demand somewhat, or select one of a number of typical profiles
	 * based on occupancy.
	 */
	private double[] randomizeDemandProfile(double[] demandProfileArray){
		double[] newProfile = new double[demandProfileArray.length];

		//add amplitude randomisation
		for (int i = 0; i < newProfile.length; i++)
		{
			newProfile[i] = demandProfileArray[i] * (double)(1 + 0.3*(RandomHelper.nextDouble() - 0.5));
		}

		//add time jitter
		double jitterFactor = RandomHelper.nextDouble() - 0.5d;

		//if (Consts.DEBUG) System.out.println("ProsumerFactory: Applying jitter" + jitterFactor);

		newProfile[0] = (jitterFactor * newProfile[0]) + ((1 - jitterFactor) * newProfile[newProfile.length - 1]);
		for (int i = 1; i < (newProfile.length - 1); i++)
		{
			newProfile[i] = (jitterFactor * newProfile[i]) + ((1 - jitterFactor) * newProfile[i+1]);
		}
		newProfile[newProfile.length - 1] = (jitterFactor * newProfile[newProfile.length - 1]) + ((1 - jitterFactor) * newProfile[0]);

		return newProfile;
	}


	/*
	 * Creates a prosumer to represent a pure generator. Therefore zero
	 * base demand, set the generator type and capacity.
	 *
	 * TODO: should the base demand be zero or null???
	 *
	 * @param Capacity - the generation capacity of this agent (kWh)
	 * @param type - the type of this generator (from an enumerator of all types)
	 */

	public ProsumerAgent createPureGenerator(double capacity, Consts.GENERATOR_TYPE genType) {
		GeneratorProsumer thisAgent = null;

		// Create a prosumer with zero base demand
		// TODO: Should this be null? or zero as implemented?
		double[] nilDemand = new double[1];
		nilDemand[0] = 0;
		switch (genType){
		case WIND:
			thisAgent = new WindGeneratorProsumer(cascadeMainContext, nilDemand, capacity);
			break;
		}

		return thisAgent;
	}
	
	/*

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
		
			if (Consts.DEBUG) System.out.println("DEFRA Customer segment is" + custSegment);
		
			HouseholdProsumer prAgent = this.createHouseholdProsumer(ArrayUtils.convertStringArrayToDoubleArray(defraProfiles.getColumn("demand" + (custSegment - 1))), true);

			prAgent.defraCategory = Integer.parseInt(defraCategories.getColumn("DEFRA_category")[custSegment - 1]);
			prAgent.microgenPropensity = Double.parseDouble(defraCategories.getColumn("Microgen_propensity")[custSegment - 1]);
			prAgent.insulationPropensity = Double.parseDouble(defraCategories.getColumn("Insulation_propensity")[custSegment - 1]);
			prAgent.HEMSPropensity = Double.parseDouble(defraCategories.getColumn("HEMS_propensity")[custSegment - 1]);
			prAgent.EVPropensity = Double.parseDouble(defraCategories.getColumn("EV_propensity")[custSegment - 1]);
			prAgent.habit = Double.parseDouble(defraCategories.getColumn("Habit_factor")[custSegment - 1]);

			prAgent.hasSmartControl = true; //(RandomHelper.nextDouble() < prAgent.HEMSPropensity);
			prAgent.hasElectricVehicle = (RandomHelper.nextDouble() < prAgent.EVPropensity);

			returnList.add(prAgent);
		}

		return returnList;
	}  */
	
}

