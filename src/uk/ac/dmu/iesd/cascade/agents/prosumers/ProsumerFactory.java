package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.WeakHashMap;
import cern.jet.random.Normal;
import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.astem.util.ArraysUtils;


/**
 * This class is a factory for creating instances of <tt>ProsumerAgent</tt> concrete subclasses
 * Its public creator method's signatures are defined by {@link IProsumerFactory} interface.
 *
 * @author Babak Mahdavi
 * @author Richard Snape
 * @version $Revision: 1.0 $ $Date: 2011/05/16 14:00:00 $
 * @version $Revision: 1.1 $ $Date: 2012/06/13 
 * 
 * 1.1 Added new implementation of randomizeDemandProfile method to replace the old one (Babak)
 * 
 */
public class ProsumerFactory implements IProsumerFactory {
	CascadeContext cascadeMainContext;


	public ProsumerFactory(CascadeContext context) {
		this.cascadeMainContext= context;
	}

	/**
	 * Creates a household prosumer with a basic consumption profile as supplied
	 * with or without added noise
	 *
	 * @param otherElasticDemandProfile - an array of the basic consumption profile for this prosumer (kWh per tick)
	 * @param randomize - boolean specifying whether or not to randomize the profile
	 * @param numOfOccupants - number of occupancy per household, if Consts.RANDOM is passed, it will done randomly  
	 */

	public HouseholdProsumer createHouseholdProsumer(double[] otherDemandProfileArray,  int numOfOccupants, boolean randomize, boolean hasGas) {
		
		HouseholdProsumer hhProsAgent;
		
		int ticksPerDay = cascadeMainContext.getNbOfTickPerDay();
		
		if (otherDemandProfileArray.length % ticksPerDay != 0)	{
			System.err.println("ProsumerFactory: Household base demand array not a whole number of days");
			System.err.println("ProsumerFactory: May cause unexpected behaviour");
		}

		if (randomize)		
			hhProsAgent = new HouseholdProsumer(cascadeMainContext, randomizeDemandProfile(otherDemandProfileArray, 0.01));
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
	
	public HouseholdProsumer createHouseholdProsumer(WeakHashMap <Integer, double[]> map_nbOfOcc2OtherDemand, int occupancyModeOrNb, boolean randomize, boolean hasGas) {
		
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
		
		return createHouseholdProsumer(arr_otherDemand,  numOfOccupant, randomize, hasGas);
		
	}
	
	private double[] randomizeDemandProfile(double[] demandProfileArray, double mFactor){
		double[] newProfileArray = new double[demandProfileArray.length];
		
		double avg = ArraysUtils.avg(demandProfileArray);
		double sd = avg * mFactor;
		double randomVal;
		for (int i=0; i<newProfileArray.length; i++) {
			Normal normalDist = RandomHelper.createNormal(demandProfileArray[i], sd);
			randomVal = Math.abs(normalDist.nextDouble());
			newProfileArray[i] = randomVal;
		}

		return newProfileArray;		
	}

}

