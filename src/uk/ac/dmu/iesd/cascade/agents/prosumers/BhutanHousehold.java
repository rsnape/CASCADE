/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.Arrays;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import cern.jet.random.Empirical;

/**
 * @author jsnape
 * 
 */
public class BhutanHousehold extends ProsumerAgent
{
	private double[] propensity; // propensity to switch on rice cooker in each
									// 5 min slot
	private double riceCookerPower = 0.6; // cookerPower in kW
	private double riceCookerWarm = 0.04;
	private double riceCookEnergy = 0.3; // in kWh - based on 30 mins at full
											// power
	private double riceCookerEnergyConsumed = 0;
	private int cookStartSlot;
	private int timeToCook; // measured in slots
	private int bestTimeToCook;
	private static int COOKER_OFF = 0;
	private static int COOKER_DESIRED = 1;
	private static int COOKER_ON = 2;
	private static int COOKER_WARM = 3;
	private double voltage;
	private double riceCookerResistance;
	private double recency = 0.01;
	private double experimentation = 0.05;
	private double apparentResistance;

	private static int brownoutThreshold = 200;

	private int cookerState = BhutanHousehold.COOKER_OFF;
	private int prefCookerStart;

	public int getCookerOn()
	{
		return this.cookerState == BhutanHousehold.COOKER_ON ? 1 : 0;
	}

	private static double[] prefDist = new double[]
	{ 0.025, 0.025, 0.05, 0.1, 0.5, 0.1, 0.05, 0.025, 0.025 };

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#step()
	 */
	@Override
	public void step()
	{
		if (this.cookerState == BhutanHousehold.COOKER_ON || this.cookerState == BhutanHousehold.COOKER_DESIRED)
		{
			this.timeToCook += 1;
		}

		double thisStepDemand = this.arr_otherDemandProfile[this.mainContext.getTimeslotOfDay()];

		if (this.mainContext.getTimeslotOfDay() == this.cookStartSlot || this.cookerState == BhutanHousehold.COOKER_DESIRED)
		{
			boolean success = this.switchOnCooker();
			if (success)
			{
				this.cookerState = BhutanHousehold.COOKER_ON;
				// Calculate the apparent voltage. This assumes that the "other"
				// demand is a constant power
				// load and the rice cooker is a constant resistance load.
				this.apparentResistance = 1.0 / ((1.0 / (this.voltage * this.voltage / (thisStepDemand * 1000))) + (1.0 / this.riceCookerResistance));
			}
			else
			{
				this.cookerState = BhutanHousehold.COOKER_DESIRED;
			}
		}

		if (this.cookerState == BhutanHousehold.COOKER_ON)
		{

			if (this.riceCookerEnergyConsumed >= this.riceCookEnergy)
			{
				// Switch Cooker off and record how long it took.
				this.cookerState = BhutanHousehold.COOKER_OFF;
				this.apparentResistance = (230 * 230.0 / (thisStepDemand * 1000));
			}
			else
			{
				double riceCookerStepPower = (this.voltage * this.voltage / this.riceCookerResistance) / 1000;
				this.riceCookerEnergyConsumed += riceCookerStepPower * (24.0 / this.mainContext.ticksPerDay);
				thisStepDemand += riceCookerStepPower;
			}
		}

		this.setNetDemand(-thisStepDemand);

		if (this.mainContext.getTimeslotOfDay() == (this.mainContext.ticksPerDay - 1))
		{
			if (this.agentID < 3)
			{
				System.err.println(this.agentName + " propensity array : " + Arrays.toString(this.propensity));
			}

			// Once a day - update propensities based on yesterday's success and
			// pick cooking time today.
			double success = Math.exp(this.bestTimeToCook - this.timeToCook - 0.025 * Math.abs(this.cookStartSlot - this.prefCookerStart)); // exponent
																																			// is
																																			// always
																																			// negative
																																			// so
																																			// success
																																			// between
																																			// 0
																																			// and
																																			// 1
																																			// -
																																			// 1
																																			// being
																																			// ideal
			if (this.agentID < 3)
			{
				System.err.println(this.agentName + " success value " + success + "based on time to cook = " + this.timeToCook
						+ " vs. best " + this.bestTimeToCook);
			}
			this.updatePropensity(this.cookStartSlot, success);
			this.cookStartSlot = RandomHelper.createEmpiricalWalker(this.propensity, Empirical.NO_INTERPOLATION).nextInt();
			if (this.agentID < 3)
			{
				System.err.println(this.agentName + " new start time = " + this.cookStartSlot);
			}

			this.timeToCook = 0;
			this.riceCookerEnergyConsumed = 0;
		}
	}

	/**
	 * @param cookStartSlot2
	 * @param success
	 */
	private void updatePropensity(int attemptedStartTime, double success)
	{
		double[] newPropensity = ArrayUtils.multiply(this.propensity, 1 - this.recency);
		double[] updateFunc = this.updateFunction(attemptedStartTime, success);
		newPropensity = ArrayUtils.add(newPropensity, updateFunc);
		this.propensity = this.propensityToProbability(newPropensity);
	}

	/**
	 * @param newPropensity
	 * @return
	 */
	private double[] propensityToProbability(double[] newPropensity)
	{
		return ArrayUtils.multiply(newPropensity, 1.0 / ArrayUtils.sum(newPropensity));
	}

	/**
	 * implements suggested update function for the Modified Roth-Erev algorithm
	 * put forward by Nicolaisen et al, 2001
	 * 
	 * @param attemptedStartTime
	 * @param success
	 * @return
	 */
	private double[] updateFunction(int attemptedStartTime, double success)
	{
		double[] E = new double[this.mainContext.ticksPerDay];

		Arrays.fill(E, success * this.experimentation / (E.length - 1));
		E[attemptedStartTime] = success * (1 - this.experimentation);
		return E;
	}

	public void setVoltage(double v)
	{
		this.voltage = v;
	}

	public double getResistance()
	{
		return this.apparentResistance;
	}

	/**
	 * @return
	 */
	private boolean switchOnCooker()
	{
		if (this.agentID < 3)
		{
			System.err.println("Timeslot " + this.mainContext.getTimeslotOfDay() + ", " + this.agentName + " trying to switch on with V = "
					+ this.voltage);
		}
		if (this.voltage > BhutanHousehold.brownoutThreshold)
		{
			return true;
		}
		// return false;
		return true;
	}

	public BhutanHousehold(CascadeContext context)
	{
		this.mainContext = context;
		this.setAgentName("Bhutan_Household_" + this.agentID);

		double basePower = 0.1;
		this.cookerState = BhutanHousehold.COOKER_OFF;
		this.bestTimeToCook = context.ticksPerDay / 48; // Number of timeslots
														// in half an hour
		this.arr_otherDemandProfile = new double[context.ticksPerDay];
		Arrays.fill(this.arr_otherDemandProfile, basePower); // initalise a 100
																// W baseload
		this.voltage = 230;
		this.apparentResistance = (this.voltage * this.voltage / (basePower * 1000));
		this.propensity = new double[context.ticksPerDay];
		double baseProp = 1.0 / context.ticksPerDay;

		// allocate (arbitrarily) 90% of the propensity around a desired time
		// +/- 20 mins somewhere between 18:00 and 20:00, evenly distribute the
		// rest
		Arrays.fill(this.propensity, 0.1 / (context.ticksPerDay - 9));

		// initialise an initial preferred slot with propensity 0.5 - 0.1 to
		// either side, 0.05, then 0.025 for two each side
		int cookerDistStart = RandomHelper.nextIntFromTo((context.ticksPerDay / 24) * 18, (context.ticksPerDay / 24) * 20);
		for (int i = 0; i < BhutanHousehold.prefDist.length; i++)
		{
			this.propensity[cookerDistStart + i] = BhutanHousehold.prefDist[i];
		}

		this.prefCookerStart = ArrayUtils.indexOfMax(this.propensity);

		this.cookStartSlot = RandomHelper.createEmpiricalWalker(this.propensity, Empirical.NO_INTERPOLATION).nextInt();
		this.riceCookerResistance = this.voltage * this.voltage / (this.riceCookerPower * 1000);
		context.add(this);
	}

}
