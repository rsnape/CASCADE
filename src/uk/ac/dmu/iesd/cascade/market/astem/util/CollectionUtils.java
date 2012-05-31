package uk.ac.dmu.iesd.cascade.market.astem.util;

import java.util.Collection;
import uk.ac.dmu.iesd.cascade.market.astem.util.ArraysUtils;

/**
 * Utility class applicable to collection types.
 * 
 * @author Babak Mahdavi Ardestani
 * @version 1.0 $ $Date: 2012/02/11
 * 
 */


public class CollectionUtils {
	
	public static int numOfRowsWithNegValInCollection (Collection<double[]> collection) {

		int numOfrowsWithNegVal=0;
		for (double[] doubleArray : collection) {
			if (ArraysUtils.isContainNegative(doubleArray)) 
				numOfrowsWithNegVal++;	
		}	
		return numOfrowsWithNegVal;
	}
	
	public static int numOfRowsWithPosValInCollection (Collection<double[]> collection) {

		int numOfrowsWithPosVal=0;
		for (double[] doubleArray : collection) {
			if (!ArraysUtils.isContainNegative(doubleArray)) 
				numOfrowsWithPosVal++;	
		}	
		return numOfrowsWithPosVal;
	}


}
