/**
 * 
 */
package uk.ac.dmu.iesd.cascade.ui;

/**
 * @author jsnape
 *
 */

import uk.ac.dmu.iesd.cascade.*;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.context.*;
import repast.simphony.visualization.Layout;
import repast.simphony.visualization.VisualizationProperties;
import repast.simphony.space.projection.Projection;

public class LargeNetwork implements Layout {
	float x, y;

	public LargeNetwork() {
		this(800, 800);
	}

	public LargeNetwork(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void update() {
		// TODO Auto-generated method stub

	}

	public void setProjection(Projection projection) {

	}

	/**
	 * Gets the layout properties for this layout.
	 *
	 * @return the layout properties for this layout.
	 */
	public VisualizationProperties getLayoutProperties() {
		return null;
	}

	/**
	 * Sets the layout properties for this layout.
	 *
	 * @param props the layout properties
	 */
	public void setLayoutProperties(VisualizationProperties props) {
		// not used
	}

	public float[] getLocation(Object obj) {
		float[] returnPosition = new float[2];
		
		if (obj instanceof HouseholdProsumer)
		{
			returnPosition[0] = (float) Math.random() * x;
			returnPosition[1] = (float) Math.random() * y / 2;
		}
		else if (obj instanceof ProsumerAgent)
		{
			returnPosition[0] = (float) Math.random() * x;
			returnPosition[1] = y / 2 + (float) Math.random() * y / 2;
		}
		else if (obj instanceof AggregatorAgent)
		{
			returnPosition[0] = (float) Math.random() * x;
			returnPosition[1] = y / 2;
		}
		else
		{
			returnPosition[0] = x/2;
			returnPosition[1] = y/2;
		}
		
		return returnPosition;
	}
	
	public String getName() {
		return "LargeNetwork";
	}

}
