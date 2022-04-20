/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * @author jsnape
 * 
 */
public class BatteryProsumer extends ProsumerAgent
{
	private double capacity; // Holds capacity in kWh

	private double currentCharge; // percentage of capacity

	private double historicalMinCharge = 1;
	private double historicalMaxCharge = 0;

	private double regulatedMinCharge;

	private double[] chargeProfile; // holds charge / discharge info
	private double chargeRate;

	/**
	 * @return the capacity
	 */
	public double getCapacity()
	{
		return this.capacity;
	}

	/**
	 * @param capacity
	 *            the capacity to set
	 */
	public void setCapacity(double capacity)
	{
		this.capacity = capacity;
	}

	/**
	 * @return the currentCharge
	 */
	public double getCurrentCharge()
	{
		return this.currentCharge;
	}

	/**
	 * @param currentCharge
	 *            the currentCharge to set
	 */
	public void setCurrentCharge(double currentCharge)
	{
		this.currentCharge = currentCharge;
		if (currentCharge < this.historicalMinCharge)
		{
			this.historicalMinCharge = currentCharge;
		}
		if (currentCharge > this.historicalMaxCharge)
		{
			this.historicalMaxCharge = currentCharge;
		}
	}

	/**
	 * @param context
	 */
	public BatteryProsumer(CascadeContext context, double capacity)
	{
		super(context);
		this.setCapacity(capacity);
		this.setCurrentCharge(0.8);
	}

	/**
	 * @param context
	 */
	public BatteryProsumer(CascadeContext context)
	{
		super(context);
		this.setCurrentCharge(0.8);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport()
	{
		StringBuilder s = new StringBuilder();
		s.append("Battery prosumer - name " + this.getAgentName());
		s.append("\n");
		s.append("Capacity : " + this.getCapacity() + "\n");
		s.append("Historically has reached min charge of " + this.historicalMinCharge + " and max of " + this.historicalMaxCharge);
		s.append("Current charge : " + this.getCurrentCharge() + "\n");
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
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#currentGeneration()
	 */
	@Override
	public double currentGeneration() {
		// TODO Auto-generated method stub
		return 0;
	}

}
