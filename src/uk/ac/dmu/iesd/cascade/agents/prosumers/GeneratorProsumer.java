package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.Arrays;

import repast.simphony.essentials.RepastEssentials;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * 
 * @version $Revision: 1.1 $ $Date: 2011/05/19 12:00:00 $
 * 
 *          Version history (for intermediate steps see Git repository history
 * 
 *          1.0 - Initial split of categories of prosumer from the abstract
 *          class representing all prosumers 1.1 - Factored out (eliminated)
 *          those methodes already defined in the superclass (Babak)
 * 
 * 
 */
public abstract class GeneratorProsumer extends ProsumerAgent
{

	/*
	 * the rated power of the various technologies / appliances we are
	 * interested in
	 * 
	 * Do not initialise these initially. They should be initialised when an
	 * instantiated agent is given the boolean attribute which means that they
	 * have access to one of these technologies.
	 */
	double ratedPowerCHP;
	double ratedPowerWind;
	double ratedPowerHydro;
	double ratedPowerThermalGeneration;

	/*
	 * TODO - need some operating characteristic parameters here - e.g. time to
	 * start ramp up generation etc. etc.
	 */

	double percentageMoveableDemand; // This can be set constant, or calculated
										// from available appliances
	int maxTimeShift; // The "furthest" a demand may be moved in time. This can
						// be constant or calculated dynamically.

	/*
	 * Accessor functions (NetBeans style) TODO: May make some of these private
	 * to respect agent conventions of autonomy / realistic simulation of humans
	 */

	public double getUnadaptedDemand()
	{
		// Cope with tick count being null between project initialisation and
		// start.
		int index = Math.max(((int) RepastEssentials.GetTickCount() % this.arr_otherDemandProfile.length), 0);
		return (this.arr_otherDemandProfile[index]) - this.currentGeneration();
	}

	/**
	 * This method must be implemented by all classes which extend this class.
	 * 
	 * @return a floating point number of the real time generation of this
	 *         prosumer
	 */
	public double currentGeneration()
	{
		return PVGeneration() + thermalGeneration()+hydroGeneration()+CHPGeneration();
		
	}

	/**
	 * @return
	 */
	protected double PVGeneration()
	{
		if (this.hasPV)
		{
			return this.ratedPowerPV * this.mainContext.getInsolation(this.mainContext.getTickCount());
		}
		return 0;
	}

	/**
	 * @return
	 */
	private double thermalGeneration()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private double hydroGeneration()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private double CHPGeneration()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param time
	 * @return double giving sum of baseDemand for the day.
	 */
	private double calculateFixedDayTotalDemand(int time)
	{
		int baseProfileIndex = time % this.arr_otherDemandProfile.length;
		return ArrayUtils.sum(Arrays.copyOfRange(this.arr_otherDemandProfile, baseProfileIndex, baseProfileIndex
				+ this.mainContext.ticksPerDay - 1));
	}

	/**
	 * constructor
	 */
	public GeneratorProsumer(CascadeContext context)
	{
		super(context);
	}

}
