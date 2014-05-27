/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Richard
 *
 */
@SuppressWarnings("rawtypes")
public abstract class TimeSeries<K extends Comparable, T> {	
	
	SortedMap<K,T> datapoints;

	public T getValue(K time)
	{
		if (datapoints.containsKey(time))
		{
			return datapoints.get(time);
		}
		return datapoints.get(datapoints.headMap(time).lastKey());
	}
	
	public void putValue(K time, T value)
	{
		datapoints.put(time, value);
	}
	
	public K getFirstKeyFollowing(K thisKey)
	{
		SortedMap<K,T> afterMap = datapoints.tailMap(thisKey);
		if (afterMap.size() == 0)
		{
			return null;
		}
		return afterMap.firstKey();
	}
	
	public K getLastKeyBefore(K thisKey)
	{
		SortedMap<K,T> beforeMap = datapoints.headMap(thisKey);
		if (beforeMap.size() == 0)
		{
			return null;
		}
		return beforeMap.lastKey();
	}
	
	public TimeSeries()
	{
		datapoints = new TreeMap<K, T>();
	}
	
	public TimeSeries(Map<K, T> initMap)
	{
		datapoints = new TreeMap<K, T>();

		for (K n : initMap.keySet())
		{
			datapoints.put(n, initMap.get(n));
		}
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for (K thisKey : datapoints.keySet())
		{
			sb.append(thisKey + " : " + datapoints.get(thisKey)+"\n");
		}
		return sb.toString();
	}
}
