/**
 * 
 */
package uk.ac.dmu.iesd.cascade.ui;

/**
 * @author jsnape
 *
 */

import repast.simphony.space.projection.Projection;
import repast.simphony.visualization.Box;
import repast.simphony.visualization.Layout;
import repast.simphony.visualization.VisualizationProperties;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent;
import uk.ac.dmu.iesd.cascade.agents.aggregators.GenericBMPxTraderAggregator;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;

//Imports below would be needed for later versions of Repast
/*import repast.simphony.visualization.Box;
 import repast.simphony.visualization.Layout;
 import repast.simphony.visualization.VisualizationProperties;
 import repast.simphony.space.projection.Projection;
 */

public class LargeNetwork implements Layout
{
	float x, y;

	public LargeNetwork()
	{
		this(800, 800);
	}

	public LargeNetwork(float x, float y)
	{
		this.x = x;
		this.y = y;
	}

	@Override
	public void update()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setProjection(Projection projection)
	{

	}

	/**
	 * Gets the layout properties for this layout.
	 * 
	 * @return the layout properties for this layout.
	 */
	@Override
	public VisualizationProperties getLayoutProperties()
	{
		return null;
	}

	/**
	 * Sets the layout properties for this layout.
	 * 
	 * @param props
	 *            the layout properties
	 */
	@Override
	public void setLayoutProperties(VisualizationProperties props)
	{
		// not used
	}

	@Override
	public float[] getLocation(Object obj)
	{
		float[] returnPosition = new float[2];

		if (obj instanceof HouseholdProsumer)
		{
			returnPosition[0] = (float) Math.random() * this.x;
			returnPosition[1] = (float) Math.random() * this.y / 2;
		}
		else if (obj instanceof GenericBMPxTraderAggregator)
		{
			returnPosition[0] = 3 * this.x / 4 + (float) Math.random() * this.x / 4;
			returnPosition[1] = 3 * this.y / 4 + (float) Math.random() * this.y / 4;
		}
		else if (obj instanceof ProsumerAgent)
		{
			returnPosition[0] = (float) Math.random() * this.x;
			returnPosition[1] = this.y / 2 + (float) Math.random() * this.y / 2;
		}
		else if (obj instanceof AggregatorAgent)
		{
			returnPosition[0] = (float) Math.random() * this.x;
			returnPosition[1] = this.y / 2;
		}
		else
		{
			returnPosition[0] = this.x / 2;
			returnPosition[1] = this.y / 2;
		}

		return returnPosition;
	}

	@Override
	public String getName()
	{
		return "LargeNetwork";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see repast.simphony.visualization.Layout#getBoundingBox()
	 * 
	 * Needed for later distributions of Repast
	 */

	@Override
	public Box getBoundingBox()
	{
		return new Box();
	}

}
