/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Richard
 * 
 */
@SuppressWarnings("rawtypes")
public abstract class TimeSeries<K extends Comparable, T>
{

	SortedMap<K, T> datapoints;
	T beforeAllTimesValue = null;

	/**
	 * @param beforeAllTimesValue
	 *            the beforeAllTimesValue to set
	 */
	public void setBeforeAllTimesValue(T beforeAllTimesValue)
	{
		this.beforeAllTimesValue = beforeAllTimesValue;
	}

	public T getValue(K time)
	{

		if (this.datapoints.containsKey(time))
		{
			return this.datapoints.get(time);
		}

		SortedMap prevVals = this.datapoints.headMap(time);

		T retVal = this.beforeAllTimesValue;

		if (!prevVals.isEmpty())
		{
			retVal = this.datapoints.get(prevVals.lastKey());
		}
		return retVal;
	}

	public void putValue(K time, T value)
	{
		this.datapoints.put(time, value);
	}

	public K getFirstKeyFollowing(K thisKey)
	{
		SortedMap<K, T> afterMap = this.datapoints.tailMap(thisKey);
		if (afterMap.size() == 0)
		{
			return null;
		}
		return afterMap.firstKey();
	}

	public K getLastKeyBefore(K thisKey)
	{
		SortedMap<K, T> beforeMap = this.datapoints.headMap(thisKey);
		if (beforeMap.size() == 0)
		{
			return null;
		}
		return beforeMap.lastKey();
	}

	public TimeSeries()
	{
		this.datapoints = new TreeMap<K, T>();
	}

	public TimeSeries(Map<K, T> initMap)
	{
		this.datapoints = new TreeMap<K, T>();

		for (K n : initMap.keySet())
		{
			this.datapoints.put(n, initMap.get(n));
		}
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for (K thisKey : this.datapoints.keySet())
		{
			sb.append(thisKey + " : " + this.datapoints.get(thisKey) + "\n");
		}
		return sb.toString();
	}
}
