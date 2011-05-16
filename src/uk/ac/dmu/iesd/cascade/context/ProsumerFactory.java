/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import repast.simphony.context.Context;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;

/**
 * A class factory for creating instances of <tt>ProsumerAgent</tt> class 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/16 14:00:00 $
 */
public class ProsumerFactory implements IProsumerFactory {
	Parameters params;
	Context cascadeMainContext;
	
	
	public ProsumerFactory(Context context, Parameters par) {
		this.params = par;
		this.cascadeMainContext= context;
		
	}
	
	/*
	 * Creates a household prosumer with a basic consumption profile as supplied
	 * with or without added noise
	 * 
	 * @param baseProfile - an array of the basic consumption profile for this prosumer (kWh per tick)
	 * @param addNoise - boolean specifying whether or not to add noise to the profile
	 */
	
	public HouseholdProsumer createHouseholdProsumer(float[] baseProfileArray, boolean addNoise) {
		HouseholdProsumer pAgent;
		int ticksPerDay = (Integer) params.getValue("ticksPerDay");
		if (baseProfileArray.length % ticksPerDay != 0)
		{
			System.err.println("Household base demand array not a whole number of days");
			System.err.println("May cause unexpected behaviour");
		}
		
		if (addNoise) 
		{
			pAgent = new HouseholdProsumer((String) cascadeMainContext.getId(), createRandomHouseholdDemand(baseProfileArray), params);
		}
		else
		{
			pAgent = new HouseholdProsumer((String) cascadeMainContext.getId(), baseProfileArray, params);
		}
		//thisAgent.exercisesBehaviourChange = (RandomHelper.nextDouble() > 0.5);
		pAgent.hasSmartControl = (RandomHelper.nextDouble() > 0.9);
		pAgent.exercisesBehaviourChange = true;
		pAgent.hasSmartMeter = true;
		pAgent.costThreshold = 125;  //Threshold in price signal at which behaviour change is prompted (if agent is willing)
		pAgent.minSetPoint = 18;
		pAgent.maxSetPoint = 21;
		pAgent.currentInternalTemp = 19;
		
		pAgent.transmitPropensitySmartControl = (float) RandomHelper.nextDouble();
		
		return pAgent;
	}
	
	
	/*
	 * This method simply adds a random element to the base profile to create a household demand
	 * It should be over-ridden in the future to use something better - for instance melody's model
	 * or something which time-shifts demand somewhat, or select one of a number of typical profiles
	 * based on occupancy.
	 */
	private float[] createRandomHouseholdDemand(float[] baseProfile){
		float[] newProfile = new float[baseProfile.length];
		
		//add amplitude randomisation
		for (int i = 0; i < newProfile.length; i++)
		{
			newProfile[i] = baseProfile[i] * (float)(1 + 0.3*(RandomHelper.nextDouble() - 0.5));
		}
		
		//add time jitter
		float jitterFactor = (float) RandomHelper.nextDouble() - 0.5f;
		//System.out.println("Applying jitter" + jitterFactor);
		newProfile[0] = (jitterFactor * newProfile[0]) + ((1 - jitterFactor) * newProfile[newProfile.length - 1]);
		for (int i = 1; i < (newProfile.length - 1); i++)
		{
			newProfile[i] = (jitterFactor * newProfile[i]) + ((1 - jitterFactor) * newProfile[i+1]);
		}
		newProfile[newProfile.length - 1] = (jitterFactor * newProfile[newProfile.length - 1]) + ((1 - jitterFactor) * newProfile[0]);
		
		return newProfile;
	}
	
	
	

}
