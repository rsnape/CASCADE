/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * @author jsnape
 * 
 */
public class BhutanHydro extends ProsumerAgent
{

	private double capacity = 30; // capacity in kW
	private double nominalVoltage = 230;
	private double actualVoltage;
	private double apparentLoadResistanceAtCapacity;

	public double getResistanceAtFullLoad()
	{
		return this.apparentLoadResistanceAtCapacity;
	}

	public void setVoltage(double v)
	{
		this.actualVoltage = v;
	}

	public double getCapacity()
	{
		return this.capacity;
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
		this.setNetDemand(this.capacity);

	}

	public BhutanHydro(CascadeContext context)
	{
		this.mainContext = context;
		this.apparentLoadResistanceAtCapacity = (this.nominalVoltage * this.nominalVoltage) / (this.capacity * 1000);
		context.add(this);
	}

}
