/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import repast.simphony.util.collections.Pair;

/**
 * @author Richard
 * 
 */
public class RHITriggerPair extends Pair<Integer, Integer>
{

	public int getTrigger()
	{
		return this.getFirst();
	}

	public int getSuperTrigger()
	{
		return this.getSecond();
	}

	/**
	 * @param first
	 * @param second
	 */
	public RHITriggerPair(Integer first, Integer second)
	{
		super(first, second);
	}

	/*
	 * An empty constructor, will construct a pair with default zero values
	 */
	public RHITriggerPair()
	{
		super(0, 0);
	}

}
