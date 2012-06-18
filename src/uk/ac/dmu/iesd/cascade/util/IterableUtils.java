/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.*;

/**
 * @author jsnape
 * 
 * Helper methods to deal with groups returned as Iterables
 *
 */

public class IterableUtils {
	
	/**
	 * Helper method to count the members of an arbitrary iterable
	 * 
	 * @param thisIterable - the iterable whose members we wish to count
	 * @return an integer equal to the number of members of the iterable
	 */
	public static int count(Iterable thisIterable)
	{
		int count = 0;
		Iterator counter = thisIterable.iterator();
		
		while(counter.hasNext())
		{
			counter.next();
			count++;
		}

		return count;
	}
	
	public static int count(Iterator thisIterator)
	{
		int count = 0;
		
		while(thisIterator.hasNext())
		{
			thisIterator.next();
			count++;
		}

		return count;
	}
	
	
	/**
	 * This method returns an arrayList of a given iterable 
	 * @author Babak Mahdavi
	 * @param anIterable
	 * @return an ArrayList
	 */
	public static ArrayList Iterable2ArrayList(Iterable anIterable) {
		
		ArrayList iterableItems = new ArrayList();
		Iterator iterable = anIterable.iterator();
		
		while(iterable.hasNext())
			iterableItems.add(iterable.next());
	
		return iterableItems;
	}

}
