/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.util.ArrayList;

import uk.ac.dmu.iesd.cascade.agents.prosumers.BhutanHousehold;
import uk.ac.dmu.iesd.cascade.agents.prosumers.BhutanHydro;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

/**
 * @author jsnape
 * 
 */
public class BhutanVillage extends AggregatorAgent
{
	private double nominalVoltage = 230;
	private double actualVoltage;
	private BhutanHydro myGen = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#paramStringReport
	 * ()
	 */
	@Override
	protected String paramStringReport()
	{
		// TODO Auto-generated method stub
		return "BhutanVillage class representing a village with a small Hydro generator feeding some houses with rice cookers";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#bizPreStep()
	 */
	@Override
	public void bizPreStep()
	{
		if (this.myGen == null)
		{
			this.myGen = (BhutanHydro) this.mainContext.getRandomObjects(BhutanHydro.class, 1).iterator().next(); // Very
																													// Hacky
																													// -
																													// shouldn't
																													// know
																													// this.
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#bizStep()
	 */
	@Override
	public void bizStep()
	{
		double thisStepDemand = 0;
		this.actualVoltage = this.nominalVoltage;

		for (Object o : this.mainContext.getEconomicNetwork().getAdjacent(this))
		{
			if (o instanceof ProsumerAgent)
			{
				thisStepDemand += ((ProsumerAgent) o).getNetDemand();
			}
		}

		if (thisStepDemand < 0)
		{
			// over demand - work out brownout conditions
			double brownoutVoltage = this.nominalVoltage;

			double fullRes = -1;

			ArrayList hhs = IterableUtils.Iterable2ArrayList(this.mainContext.getAgentLayer(BhutanHousehold.class));

			for (Object o : hhs) // don't like this implementation - means we
									// know too much about the prosumer!!
			{
				BhutanHousehold hh = (BhutanHousehold) o;
				if (fullRes == -1)
				{
					fullRes = hh.getResistance();
				}
				else
				{
					fullRes = 1.0 / ((1.0 / fullRes) + (1.0 / hh.getResistance()));
				}

			}

			brownoutVoltage = Math.sqrt(this.myGen.getCapacity() * 1000 * fullRes);
			this.myGen.setVoltage(brownoutVoltage);

			for (Object o : hhs) // don't like this implementation - means we
									// know too much about the prosumer!!
			{
				BhutanHousehold hh = (BhutanHousehold) o;
				hh.setVoltage(brownoutVoltage);
			}

			this.actualVoltage = brownoutVoltage;

			thisStepDemand = 0; // the voltage lowering will lower all demands -
								// equalising supply and demand
		}

		this.setNetDemand(thisStepDemand);

	}

	public double getVillageVoltage()
	{
		return this.actualVoltage;
	}

	public BhutanVillage(CascadeContext context)
	{
		this.mainContext = context;
		this.actualVoltage = this.nominalVoltage;
		context.add(this);
	}

}
