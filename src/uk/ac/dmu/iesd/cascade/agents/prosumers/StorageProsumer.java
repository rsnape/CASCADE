package uk.ac.dmu.iesd.cascade.agents.prosumers;

import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * TODO: Determine the role of this class; prosumers supposed to range from pure
 * generators to pure consumers, but not storage (is this a tool)? May need to
 * be eliminated.
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * 
 * @version $Revision: 1.1 $ $Date: 2011/05/19 12:00:00 $
 * 
 *          Version history (for intermediate steps see Git repository history
 * 
 *          1.0 - Initial split of categories of prosumer from the abstract
 *          class representing all prosumers 1.1 - Factored out (eliminated) the
 *          methods defined in the superclass
 * 
 * 
 */
public class StorageProsumer extends ProsumerAgent
{

	/*
	 * NOTE TODO: - It is possible that we should have some kind of hierarchical
	 * inheritance and this should also be abstract, sub-classed to specific
	 * types of non-dom consumer. For now, it is a placeholder.
	 */

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */

	boolean hasElectricalStorage = false; // Do we need to break out various
											// storage technologies?
	boolean hasHotWaterStorage = false;
	boolean hasSpaceHeatStorage = false;

	/*
	 * the rated power of the various technologies / appliances we are
	 * interested in
	 * 
	 * Do not initialise these initially. They should be initialised when an
	 * instantiated agent is given the boolean attribute which means that they
	 * have access to one of these technologies.
	 */
	double ratedCapacityElectricalStorage; // Note kWh rather than kW
	double ratedCapacityHotWaterStorage;
	double ratedCapacitySpaceHeatStorage; // Note - related to thermal mass

	/*
	 * Specifically, a household may have a certain percentage of demand that it
	 * believes is moveable and / or a certain maximum time shift of demand
	 */
	double percentageMoveableDemand; // This can be set constant, or calculated
										// from available appliances
	int maxTimeShift; // The "furthest" a demand may be moved in time. This can
						// be constant or calculated dynamically.

	/*
	 * temperature control parameters
	 */
	double minSetPoint; // The minimum temperature for this Household's building
						// in centigrade (where relevant)
	double maxSetPoint; // The maximum temperature for this Household's building
						// in centigrade (where relevant)
	double currentInternalTemp;

	/*
	 * This may or may not be used, but is a threshold cost above which actions
	 * take place for the household
	 */
	double costThreshold;

	/*
	 * Accessor functions (NetBeans style) TODO: May make some of these private
	 * to respect agent conventions of autonomy / realistic simulation of humans
	 */

	/**
	 * Returns a string representing the state of this agent. This method is
	 * intended to be used for debugging purposes, and the content and format of
	 * the returned string should include the states (variables/parameters)
	 * which are important for debugging purpose. The returned string may be
	 * empty but may not be <code>null</code>.
	 * 
	 * @return a string representation of this agent's state parameters
	 */
	@Override
	protected String paramStringReport()
	{
		String str = "";
		return str;

	}

	// @ScheduledMethod(start = 0, interval = 1, shuffle = true)
	@Override
	public void step()
	{
	}

	/**
	 * Constructor
	 */
	public StorageProsumer(CascadeContext context, double[] baseDemand)
	{
		super(context);
		// this.percentageMoveableDemand = RandomHelper.nextDoubleFromTo(0,
		// 0.5);
		// setElasticityFactor(percentageMoveableDemand);
		if (baseDemand.length % this.mainContext.ticksPerDay != 0)
		{
			System.err.print("Error/Warning message from " + this.getClass() + ": BaseDemand array not a whole number of days.");
			System.err.println("StorageProsumer:  Will be truncated and may cause unexpected behaviour");
		}
		this.arr_otherDemandProfile = new double[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.arr_otherDemandProfile, 0, baseDemand.length);
		// Initialise the smart optimised profile to be the same as base demand
		// smart controller will alter this
		this.smartOptimisedProfile = new double[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0, this.smartOptimisedProfile.length);
	}

}
