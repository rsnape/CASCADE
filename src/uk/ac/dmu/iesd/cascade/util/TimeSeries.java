/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Map;
import java.util.SortedMap;

/**
 * @author Richard
 *
 */
@SuppressWarnings("rawtypes")
public abstract class TimeSeries<K extends Comparable, T> {
	
	SortedMap<K,T> datapoints;

	public T getValue(K time)
	{
		return datapoints.get(datapoints.headMap(time).lastKey());
	}
	
	public void putValue(K time, T value)
	{
		datapoints.put(time, value);
	}
	
	public TimeSeries()
	{
		//null constructor
	}
	
	public TimeSeries(Map<K, T> initMap)
	{
		for (K n : initMap.keySet())
		{
			datapoints.put(n, initMap.get(n));
		}
	}


}
