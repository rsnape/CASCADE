/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import repast.simphony.util.collections.Pair;

/**
 * @author Richard
 *
 */
public class RHITriggerPair extends Pair<Integer, Integer> {
	
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
	public RHITriggerPair(Integer first, Integer second) {
		super(first, second);
	}
	
	public RHITriggerPair(int[] triggers) {
		this(triggers[0], triggers[1]);
		if (triggers.length != 2)
		{
			System.err.println("You shouldn't create a pair with an array that hasn't got two members!!!");
			System.err.println("The constructor has used the first two elements as the pair by default - watch out for unexpected results!");
		}
	}

	/*
	 * An empty constructor, will construct a pair with default zero values
	 */
	public RHITriggerPair()
	{
		this(0,0);
	}
	
}
