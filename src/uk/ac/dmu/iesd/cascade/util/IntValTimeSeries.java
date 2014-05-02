/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Map;

/**
 * @author Richard
 *
 */
public class IntValTimeSeries<K extends Number> extends TimeSeries<K, Integer> {
	
	private boolean interpolate;
	
	public void setInterpolate(boolean b)
	{
		this.interpolate = b;
	}
	
	public boolean getInterpolate()
	{
		return this.interpolate;
	}
	
	public IntValTimeSeries()
	{
		this(false);
	}
	
	public IntValTimeSeries(boolean inter)
	{
		super();
		this.interpolate=inter;
	}
	
	public IntValTimeSeries(Map<K,Integer> init)
	{
		super(init);
		this.interpolate=false;
	}
	
	public IntValTimeSeries(Map<K,Integer> init, boolean inter)
	{
		super(init);
		this.interpolate=inter;
	}
	
	@Override
	public Integer getValue(K thisKey)
	{
		if (!interpolate)
		{
			return super.getValue(thisKey);
		}
		else
		{
			K key1 = datapoints.headMap(thisKey).lastKey();
			Integer val1 = datapoints.get(key1);
			K key2 = datapoints.tailMap(thisKey).firstKey();
			Integer val2 = datapoints.get(key2);
			
			double fraction = (thisKey.doubleValue() - key1.doubleValue()) / (key2.doubleValue() - key1.doubleValue());
			Integer addedVal = (int) (((val2.doubleValue() - val1.doubleValue()) * fraction) + 0.5);
			
			return new Integer(val1+addedVal);
		}		
	}
	
}
