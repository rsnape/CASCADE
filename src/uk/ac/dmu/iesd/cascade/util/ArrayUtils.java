/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.jgap.Gene;
import org.jgap.impl.DoubleGene;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.collections.Pair;

/**
 * A helper class with static methods to aid in the manipulation of arrays
 * IMPLEMENTATION NOTE - (except where explicitly stated) methods result in an array as output,
 * 						the methods do not work directly on the array(s) passed in
 * 						but rather create a new array and pass this back as the 
 * 						return value.
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi 
 * @version $Revision: 1.05 $ $Date: 2011/06/28 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial basic functionality
 * 1.01 - added dot product functionality and normalization
 * 1.02 - added method isSumEqualZero(floatArray); isSumEqualZero(intArray) [Babak] 
 * 1.03 - added avg, avgCols2DFloatArry
 * 1.04 - added getPrintableOutputFor2DFloatArrary, getPrintableOutputForFloatArray, 
 *              getPrintableOutputFor2DIntArrary, getPrintableOutputForIntArrary
 * 1.05 - added subArrayCopy, rowCopy     
 * 1.06 - added replaceRange function        
 */
public class ArrayUtils {

	public static int[] convertStringArrayToIntArray(String[] sarray) {
		if (sarray != null) {
			int intarray[] = new int[sarray.length];
			for (int i = 0; i < sarray.length; i++) {
				intarray[i] = Integer.parseInt(sarray[i]);
			}
			return intarray;
		}
		return null;
	}

	public static float[] convertStringArrayToFloatArray(String[] sarray) {
		if (sarray != null) {
			float floatarray[] = new float[sarray.length];
			for (int i = 0; i < sarray.length; i++) {
				floatarray[i] = Float.parseFloat(sarray[i]);
			}
			return floatarray;
		}
		return null;
	}

	public static double[] convertStringArrayToDoubleArray(String[] sarray) {
		if (sarray != null) {
			double doubleArray[] = new double[sarray.length];
			for (int i = 0; i < sarray.length; i++) {
				doubleArray[i] = Double.parseDouble(sarray[i]);
			}
			return doubleArray;
		}
		return null;
	}


	/*
	 * Tried a different implementation here - could test which is quicker...
	 */
	public static double[] convertStringArrayToDoubleArray2(String[] array)
	{
		int i = 0;
		double[] returnArray = new double[array.length];
		for(String s: array)
		{
			returnArray[i] = Double.parseDouble(s);
			i++;
		}
		return returnArray;
	}


	/**
	 * return the sum of the members of the array passed into the method
	 * 
	 * @param intArray
	 * @return an <code>int</code> which is the sum of the <code>int</code> members of the array passed in
	 */
	public static int sum(int[] intArray)
	{
		int sum = 0;
		for (int i = 0; i < intArray.length; i++)
		{
			sum = sum + intArray[i];
		}
		return sum;
	}

	/**
	 * return the sum of the members of the array passed into the method
	 * 
	 * @param doubleArray
	 * @return an <code>double</code> which is the sum of the <code>double</code> members of the array passed in
	 */
	public static double sum(double[] doubleArray)
	{
		double sum = 0;
		for (int i = 0; i < doubleArray.length; i++)
		{
			sum = sum + doubleArray[i];
		}
		return sum;
	}

	/**
	 * return the sum of the members of the array passed into the method
	 * 
	 * @param floatArray
	 * @return an <code>float</code> which is the sum of the <code>float</code> members of the array passed in
	 */
	public static float sum(float[] floatArray)
	{
		float sum = 0;
		for (int i = 0; i < floatArray.length; i++)
		{
			sum = sum + floatArray[i];
		}
		return sum;
	}

	/**
	 * function to multiply the values of an arbitrary number of arrays
	 * together element by element
	 * 
	 * @param arrays arbitrary number of floating point arrays
	 * @return array of <code>float</code>s containing multiplied values i.e. <code>returnArray[i] = a1[i] * a2[i] * ... * an[i]</code>
	 */
	public static float[] mtimes(float[]... arrays)
	{
		float [] returnArray = new float[arrays[1].length];
		Arrays.fill(returnArray, 1f);

		for (float[] nextArray : arrays)
		{
			if (nextArray.length != returnArray.length)
			{
				System.err.println("ArrayUtils: Tried to convolve arrays of different lengths  " + returnArray.length + " and " + nextArray.length + " result - undefined");
				returnArray = null;
			}
			else
			{
				for(int i = 0; i < returnArray.length; i++)
				{
					returnArray[i] = returnArray[i] * nextArray[i];
				}
			}
		}

		return returnArray;
	}

	/**
	 * function to multiply the values of an arbitrary number of arrays
	 * together element by element
	 * 
	 * @param arrays arbitrary number of floating point arrays
	 * @return array of <code>double</code>s containing multiplied values i.e. <code>returnArray[i] = a1[i] * a2[i] * ... * an[i]</code>
	 */
	public static double[] mtimes(double[]... arrays)
	{
		double [] returnArray = new double[arrays[0].length];
		Arrays.fill(returnArray, 1d);

		for (double[] nextArray : arrays)
		{
			if (nextArray.length != returnArray.length)
			{
				System.err.println("ArrayUtils: Tried to convolve arrays of different lengths  " + returnArray.length + " and " + nextArray.length + " result - undefined");
				returnArray = null;
			}
			else
			{
				for(int i = 0; i < returnArray.length; i++)
				{
					returnArray[i] = returnArray[i] * nextArray[i];
				}
			}
		}

		return returnArray;
	}

	/**
	 * calculates the dot (or inner) product of two input arrays
	 * 
	 * @param array1
	 * @param array2
	 * @return the dot product of the input arrays.
	 */
	public static float dotProduct(float[] array1, float[] array2)
	{
		float dotVal = 0;

		if (array1.length != array2.length)
		{
			System.err.println("ArryUtils: Tried to dot product arrays of different lengths " + array1.length + array2.length + " result - undefined");
		}
		else
		{
			for(int i = 0; i < array1.length; i++)
			{
				dotVal = dotVal + array1[i] * array2[i];
			}
		}

		return dotVal;
	}

	/**
	 * normalize array values to lie in the range -1 <= memberValue <= +1
	 * the relative value of the array values is preserved
	 * 
	 * @param array1
	 * @return normalized array of <code>float</code>s
	 */
	public static float[] normalizeValues(float[] array)
	{
		return normalizeValues(array, 1f);
	}

	/**
	 * normalize array values to lie in the range -1 <= memberValue <= +1
	 * the relative value of the array values is preserved
	 * 
	 * @param array1
	 * @return normalized array of <code>double</code>s
	 */
	public static double[] normalizeValues(double[] array)
	{
		return normalizeValues(array, 1d);
	}

	/**
	 * normalize array values to lie in the range -maxMagnitude <= memberValue <= +maxMagnitude
	 * the relative value of the array values is preserved
	 * 
	 * @param array1
	 * @param maxMagnitude
	 * @return normalized array of <code>float</code>s
	 */
	public static float[] normalizeValues(float[] array, float maxMagnitude)
	{
		return normalizeValues(array, maxMagnitude, true);

	}

	/**
	 * normalize array values to lie in the range -maxMagnitude <= memberValue <= +maxMagnitude
	 * the relative value of the array values is preserved
	 * 
	 * @param array1
	 * @param maxMagnitude
	 * @return normalized array of <code>double</code>s
	 */
	public static double[] normalizeValues(double[] array, double maxMagnitude)
	{
		return normalizeValues(array, maxMagnitude, true);

	}

	/**
	 * normalize array values to lie in the range 
	 * <li>-maxMagnitude <= memberValue <= +maxMagnitude if allowNegatives is true
	 * <li>0 <= memberValue <= + maxMagnitude if allowNegatives is false
	 * the relative value of the array values is preserved
	 * 
	 * @param array1
	 * @param maxMagnitude
	 * @param allowNegatives
	 * @return normalized array of <code>float</code>s
	 */
	public static float[] normalizeValues(float[] array, float maxMagnitude, boolean allowNegative)
	{
		float [] returnArray = null;

		//By definition if maxAbs = 0 then array filled with zeros and return with zeros.
		float maxAbs = max(absoluteValues(array));
		if(Float.compare(maxAbs, 0.0f) == 0) 
		{
			return array;
		}
				
		float normalisationConstant = (1 / maxAbs);

		returnArray = multiply(array, (normalisationConstant * maxMagnitude));

		if(!allowNegative) 
		{
			float minVal = 0;
			minVal = Math.min(minVal, min(array));
			returnArray = multiply(offset(returnArray, -minVal), ((1/max(returnArray)) * maxMagnitude));

		}
		return returnArray;
	}

	/**
	 * normalize array values to lie in the range 
	 * <li>-maxMagnitude <= memberValue <= +maxMagnitude if allowNegatives is true
	 * <li>0 <= memberValue <= + maxMagnitude if allowNegatives is false
	 * the relative value of the array values is preserved
	 * 
	 * @param array1
	 * @param maxMagnitude
	 * @param allowNegatives
	 * @return normalized array of <code>float</code>s
	 */
	public static double[] normalizeValues(double[] array, double maxMagnitude, boolean allowNegative)
	{
		double [] returnArray = null;
		//By definition if maxAbs = 0 then array filled with zeros and return with zeros.
		double maxAbs = max(absoluteValues(array));

		if(Double.compare(maxAbs, 0.0d) == 0) 
		{
			return array;
		}
				
		double normalisationConstant = (1 / maxAbs);

		returnArray = multiply(array, (normalisationConstant * maxMagnitude));

		if(!allowNegative) 
		{
			double minVal = 0;
			minVal = Math.min(minVal, min(array));
			returnArray = multiply(offset(returnArray, -minVal), ((1/max(returnArray)) * maxMagnitude));

		}
		return returnArray;
	}

	/**
	 * returns an array containing the absolute values of the members of the input array
	 * 
	 * @param array
	 * @return 
	 */
	public static float[] absoluteValues(float[] array)
	{
		float[] returnArray = new float [array.length];

		for(int i = array.length - 1; i >= 0; --i)
		{
			returnArray[i] = Math.abs(array[i]);			
		}		

		return returnArray;
	}

	/**
	 * returns an array containing the absolute values of the members of the input array
	 * 
	 * @param array
	 * @return 
	 */
	public static double[] absoluteValues(double[] array)
	{
		double[] returnArray = new double [array.length];

		for(int i = array.length - 1; i >= 0; --i)
		{
			returnArray[i] = Math.abs(array[i]);			
		}		

		return returnArray;
	}

	/**
	 * multiplies each member of the input array by the specified multiplier
	 * 
	 * @param array
	 * @param multiplier
	 * @return 
	 */
	public static float[] multiply(float[] array, float multiplier)
	{
		float[] returnArray = new float [array.length];

		for (int i = 0; i < array.length; i++)
		{
			returnArray[i] = multiplier * array[i];
		}

		return returnArray;
	}

	public static double[] multiply(double[] array, double multiplier)
	{
		//Early return if the multiplier is 1
		if (multiplier == 1)
		{
			return Arrays.copyOf(array, array.length);
		}
		
		double[] returnArray = new double [array.length];

		for (int i = 0; i < array.length; i++)
		{
			returnArray[i] = multiplier * array[i];
		}

		return returnArray;
	}

	/**
	 * returns the maximum value of the input array
	 * 
	 * @param array
	 * @return <code>float</code> which is the maximum value of the input array
	 */
	public static float max( float[] array)
	{
		float maxVal = - Float.MAX_VALUE;

		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] > maxVal) {maxVal = array[i];}			
		}

		return maxVal;
	}

	/**
	 * returns the maximum value of the input array
	 * 
	 * @param array
	 * @return <code>double</code> which is the maximum value of the input array
	 */
	public static double max( double[] array)
	{
		double maxVal = - Double.MAX_VALUE;

		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] > maxVal) {maxVal = array[i];}			
		}

		return maxVal;
	}

	/**
	 * returns the minimum value of the input array
	 * 
	 * @param array
	 * @return <code>float</code> which is the minimum value of the input array
	 */
	public static float min( float[] array)
	{
		float minVal = Float.MAX_VALUE;

		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] < minVal) {minVal = array[i];}			
		}

		return minVal;
	}

	/**
	 * returns the minimum value of the input array
	 * 
	 * @param array
	 * @return <code>float</code> which is the minimum value of the input array
	 */
	public static double min( double[] array)
	{
		double minVal = Double.MAX_VALUE;

		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] < minVal) {minVal = array[i];}			
		}

		return minVal;
	}

	/**
	 * offsets each member of the input array by the specified offset
	 * 
	 * @param array
	 * @param offset
	 * @return 
	 */
	public static double[] offset (double[] array, double offset)
	{
		double[] returnArr = new double[array.length];
		
		for(int i = array.length - 1; i >= 0; --i)
		{
			returnArr[i] = array[i] + offset;			
		}

		return returnArr;
	}

	/**
	 * offsets each member of the input array by the specified offset
	 * 
	 * @param array
	 * @param offset
	 * @return 
	 */
	public static float[] offset (float[] array, float offset)
	{
		float[] returnArr = new float[array.length];
		for(int i = array.length - 1; i >= 0; --i)
		{
			returnArr[i] = array[i] + offset;			
		}

		return returnArr;
	}

	/**
	 * find the index of the member of the input array with the maximum value (note real value, not absolute magnitude)
	 * 
	 * @param array
	 * @return the index of the maximum value in the input array
	 */
	public static int indexOfMax( float[] array)
	{
		int index = 0;
		float maxVal = Float.MIN_VALUE;

		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] > maxVal) {maxVal = array[i]; index = i;}			
		}

		return index;
	}

	/**
	 * find the index of the member of the input array with the maximum value (note real value, not absolute magnitude)
	 * 
	 * @param array
	 * @return the index of the maximum value in the input array
	 */
	public static int indexOfMax(double[] array)
	{
		int index = 0;
		double maxVal = Double.MIN_VALUE;

		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] > maxVal) {maxVal = array[i]; index = i;}			
		}

		return index;
	}


	/**
	 * find the index of the member of the input array with the minimum value (note real value, not absolute magnitude)
	 * 
	 * @param array
	 * @return the index of the minimum value in the input array
	 */
	public static int indexOfMin( float[] array)
	{
		int index = 0;
		float minVal = Float.MAX_VALUE;

		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] < minVal) {minVal = array[i]; index = i;}			
		}

		return index;
	}

	/**
	 * find the index of the member of the input array with the minimum value (note real value, not absolute magnitude)
	 * 
	 * @param array
	 * @return the index of the minimum value in the input array
	 */
	public static int indexOfMin(double[] array)
	{
		int index = 0;
		double minVal = Double.MAX_VALUE;

		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] < minVal) {minVal = array[i]; index = i;}			
		}

		return index;
	}

	/**
	 * raise each member of the input array to the power two (or square each member)
	 * 
	 * @param doubleArrayBase
	 * @return an array containing the squared values i.e. <code>return[i] = inputArray[i] ^ 2</code>
	 */
	public static double[] pow2(double[] doubleArrayBase) {	
		/*float[] powArray = new float[floatArrayBase.length];
		for(int i = 0; i<floatArrayBase.length; i++) {
			powArray[i]= (float)Math.pow(floatArrayBase[i], 2);	 */
		return mtimes(doubleArrayBase,doubleArrayBase);
	}

	/**
	 * This utility function returns an array of double values after 
	 * raising the values of first argument (passed float array) to the power of second
	 * argument (exp).
	 * If the return value by Math.pow would be a NaN or Infinity, the value is 
	 * set to 0. 
	 *  
	 * @param floatArrayBase whose base element values will be raised by <tt>exp</tt> value  
	 * @param exp the exponent    	 	 
	 * @return float array containing the initial array values raised by <tt>exp</tt> value  
	 */
	/*
	public static double[] pow(float[] floatArrayBase, float exp ) {	

		if (floatArrayBase == null)  {
			return null;   
		}   
		double[] powArray = new double[floatArrayBase.length];

		for(int i = 0; i<floatArrayBase.length; i++) {
			double val = Math.pow(floatArrayBase[i], exp);

			powArray[i]=val;
		}

		return powArray;	
	} */

	/**
	 * This utility function returns an array of double values after 
	 * raising the values of first argument (passed float array) to the power of second
	 * argument (exp).
	 * If the return value by Math.pow would be a NaN or Infinity, the value is 
	 * set to 0. 
	 *  
	 * @param doubleArrayBase whose base element values will be raised by <tt>exp</tt> value  
	 * @param exp the exponent    	 	 
	 * @return double array containing the initial array values raised by <tt>exp</tt> value  
	 */
	public static double[] pow(double[] doubleArrayBase, double exp ) {	

		if (doubleArrayBase == null)  {
			return null;   
		}   
		double[] powArray = new double[doubleArrayBase.length];

		for(int i = 0; i<doubleArrayBase.length; i++) {
			double val = Math.pow(doubleArrayBase[i], exp);
			/*if (Double.isNaN(val)) {
				val =0;
				this.mainContext.logger.debug("ArrayUtils:: pow(), calculated val was NaN and set to 0");
			}

			if (Double.isInfinite(val)) {
				this.mainContext.logger.debug("ArrayUtils:: pow(), calculated val was Infinity and set to 0");
				val =0;
			} */
			powArray[i]=val;
		}

		return powArray;	
	}

	/**
	 * This utility function accept a double array as an argument and returns 
	 * a float array containing converted double values of the passed array (argument).
	 * This is not a good/wise thing to do as information/precision could be lost, but it is
	 * written if it would be necessary to use it! 
	 *  
	 * @param doubleArray whose  <tt>double</tt> element values will be converted to <tt>float</tt>  
	 * @return array containing the passed array values (as argument) converted to <tt>float</tt>  
	 */
	public static float[] convertDoubleArrayToFloatArray(double[] array){ 
		if (array == null)  {
			return null;   
		}   
		float[] floatArray = new float[array.length];    
		for (int i = 0; i < array.length; i++)  {
			floatArray[i] = (float) array[i];    
		}   
		return floatArray; 
	}

	/**
	 * This utility function accept a double array as an argument and returns 
	 * a float array containing converted double values of the passed array (argument).
	 * This is not a good/wise thing to do as information/precision could be lost, but it is
	 * written if it would be necessary to use it! 
	 *  
	 * @param doubleArray whose  <tt>double</tt> element values will be converted to <tt>float</tt>  
	 * @return array containing the passed array values (as argument) converted to <tt>float</tt>  
	 */
	public static float[][] convertDoubleArrayToFloatArray(double[][] array){ 

		float[][] floatArray = new float[array.length][array[0].length];    
		for (int i = 0; i < array.length; i++)  {
			floatArray[i] = convertDoubleArrayToFloatArray(array[i]);    
		}   
		return floatArray; 
	}

	/**
	 * This utility function accept a float array as an argument and returns 
	 * a double array containing converted double values of the passed array (argument).
	 * 
	 * @param floatArray whose  <tt>float</tt> element values will be converted to <tt>double</tt>  
	 * @return double array containing the passed array values (as argument) converted to <tt>double</tt>  
	 */
	public static double[] convertFloatArrayToDoubleArray(float[] floatArray){ 
		if (floatArray == null)  {
			return null;   
		}   
		double[] doubleArray = new double[floatArray.length];    
		for (int i = 0; i < floatArray.length; i++)  {
			doubleArray[i] = floatArray[i];    
		}   
		return doubleArray; 
	}

	/**
	 * This utility function returns the index where the passed value is found.
	 * If there is no such a value, it returns -1 as index.
	 * If there are more than one occurrence, the index of the last one found is returned.
	 * 
	 * @param floatArray whose index for occurrence of <tt>val</tt> will be searched  
	 * @param val the value whose index is sought   	 	 
	 * @return index of the array where <tt>val</tt> has been found  
	 */
	public static int indexOf( float[] floatArray, float val)
	{
		int index = -1;

		for(int i = floatArray.length - 1; i >= 0; --i)	{
			if (floatArray[i] == val) {index = i;}			
		}
		return index;
	}

	/**
	 * This utility function returns the index where the passed value is found.
	 * If there is no such a value, it returns -1 as index.
	 * If there are more than one occurrence, the index of the last one found is returned.
	 * 
	 * @param doubleArray whose index for occurrence of <tt>val</tt> will be searched  
	 * @param val the value whose index is sought   	 	 
	 * @return index of the array where <tt>val</tt> has been found  
	 */
	public static int indexOf( double[] doubleArray, double val)
	{
		int index = -1;

		for(int i = doubleArray.length - 1; i >= 0; --i)	{
			if (doubleArray[i] == val) {index = i;}			
		}
		return index;
	}


	/**
	 * This utility function verifies if the sum of the values in
	 * the given array (float) are equal zero. If the sum is zero it returns true; otherwise false
	 * 
	 * @param   floatArray to be verified if its sum is equal zero 	 
	 * @return true if the sum of array elements are zero; false otherwise 
	 */
	public static boolean isSumEqualZero(float[] floatArray) {	
		return isSumEqualZero(convertFloatArrayToDoubleArray(floatArray));
	}
	
	/**
	 * This utility function verifies if the sum of the values in
	 * the given array (float) are equal zero. If the sum is zero it returns true; otherwise false
	 * 
	 * @param   floatArray to be verified if its sum is equal zero 	 
	 * @return true if the sum of array elements are zero; false otherwise 
	 */
	public static boolean isSumEqualZero(double[] floatArray) {	
		boolean sumIsEqualZero = false;
		double sum= sum(floatArray);
		if (sum ==0)
			sumIsEqualZero=true;
		this.mainContext.logger.trace("ArryUtils.isSumEqualZero: "+sum);
		return sumIsEqualZero;
	}

	/**
	 * This utility function verifies if the sum of the values in
	 * the given array (int) are equal zero. If the sum is zero it returns true; otherwise false
	 * 
	 * @param   intArray to be verified if its sum is equal zero 	 
	 * @return true if the sum of array elements are zero; false otherwise 
	 */
	public static boolean isSumEqualZero(int[] intArray) {	
		boolean sumIsEqualZero = false;
		float sum= sum(intArray);
		if (sum ==0)
			sumIsEqualZero=true;
		return sumIsEqualZero;
	}

	/**
	 * This utility function returns a copy of the specific column of a passed 2D float array
	 * as a one dimensional float array.
	 *  
	 * @param  twoDfloatArray a 2D float array whose column is supposed to be fetched and return
	 * @param  colNb the number of column whose values are supposed to be returned as array  	 
	 * @return an array (float) containing a copy of the passed column number and 2D array 
	 */
	public static float[] colCopy(float[][] twoDfloatArray, int colNb) {
		float[] aCol = new float[twoDfloatArray.length];
		for (int i = 0; i < twoDfloatArray.length; i++) {
			aCol[i]=twoDfloatArray[i][colNb];
		}
		return aCol;
	}

	/**
	 * This utility function returns a copy of the specific column of a passed 2D double array
	 * as a one dimensional double array.
	 *  
	 * @param  twoDfloatArray a 2D double array whose column is supposed to be fetched and return
	 * @param  colNb the number of column whose values are supposed to be returned as array  	 
	 * @return an array (double) containing a copy of the passed column number and 2D array 
	 */
	public static double[] colCopy(double[][] twoDdoubleArray, int colNb) {
		double[] aCol = new double[twoDdoubleArray.length];
		for (int i = 0; i < twoDdoubleArray.length; i++) {
			aCol[i]=twoDdoubleArray[i][colNb];
		}
		return aCol;
	}

	/**
	 * This utility function returns a sub-array copy of a given 2D array (with the same columns' size)
	 * by passing/specifying a startRow and endRow indices.
	 * 
	 * @param twoDfloatArray the original 2D float array from which a sub-array is built
	 * @param startRow a starting row index which indicates the beginning row of the sub-array 
	 * @param endRow an ending row index which indicates the last row of original array included in the sub-array 
	 * @return an 2D sub-array (float) containing a copy of the passed 2D-array up from startRow to endRow
	 */
	public static float[][] subArrayCopy(float[][] twoDfloatArray, int startRow, int endRow) {
		if(startRow>endRow)throw new IllegalArgumentException("endRow is bigger than startRow");
		int subArrRowLength = endRow-startRow;
		float[][] subArr = new float[subArrRowLength][twoDfloatArray[0].length];

		for (int i = 0; i < subArrRowLength; i++) {
			for (int col = 0; col < twoDfloatArray[0].length; col++) {
				subArr[i][col]=twoDfloatArray[i+startRow][col];
			}
		}
		return subArr;
	}

	/**
	 * This utility function returns a sub-array copy of a given 2D array (with the same columns' size)
	 * by passing/specifying a startRow and endRow indices.
	 * 
	 * @param twoDdoubleArray the original 2D double array from which a sub-array is built
	 * @param startRow a starting row index which indicates the beginning row of the sub-array 
	 * @param endRow an ending row index which indicates the last row of original array included in the sub-array 
	 * @return an 2D sub-array (double) containing a copy of the passed 2D-array up from startRow to endRow
	 */
	public static double[][] subArrayCopy(double[][] twoDdoubleArray, int startRow, int endRow) {
		if(startRow>endRow)throw new IllegalArgumentException("endRow is bigger than startRow");
		int subArrRowLength = endRow-startRow;
		double[][] subArr = new double[subArrRowLength][twoDdoubleArray[0].length];

		for (int i = 0; i < subArrRowLength; i++) {
			for (int col = 0; col < twoDdoubleArray[0].length; col++) {
				subArr[i][col]=twoDdoubleArray[i+startRow][col];
			}
		}
		return subArr;
	}

	/**
	 * This utility function returns an array copy containing of passed row index of a given 2D array
	 *  
	 * @param twoDfloatArray the original 2D float array from which a row will be copied and return as an array
	 * @param row a row index indicating the row which needs to be taken out (copied and returned)
	 * @return an array (float) containing a copy of its needed row (index passed as argument)
	 */
	public static float[] rowCopy(float[][] twoDfloatArray, int row) {
		float[] rowCopyArr = new float[twoDfloatArray[0].length];
		for (int col = 0; col < twoDfloatArray[0].length; col++) {
			rowCopyArr[col]=twoDfloatArray[row][col];
		}
		return rowCopyArr;
	}

	/**
	 * This utility function returns an array copy containing of passed row index of a given 2D array
	 *  
	 * @param twoDdoubleArray the original 2D double array from which a row will be copied and return as an array
	 * @param row a row index indicating the row which needs to be taken out (copied and returned)
	 * @return an array (double) containing a copy of its needed row (index passed as argument)
	 */
	public static double[] rowCopy(double[][] twoDdoubleArray, int row) {
		double[] rowCopyArr = new double[twoDdoubleArray[0].length];
		for (int col = 0; col < twoDdoubleArray[0].length; col++) {
			rowCopyArr[col]=twoDdoubleArray[row][col];
		}
		return rowCopyArr;
	}

	/**
	 * This utility function calculates the average of an float array values and returns it.
	 * 
	 * @param   a floatArray  	 
	 * @return average of array's elements/values 
	 */
	public static float avg(float[] floatArray) {
		float avg=0f;
		if (floatArray.length !=0) {
			float sum = sum(floatArray);
			avg = sum/(float)floatArray.length;
		}
		return avg;
	}

	/**
	 * This utility function calculates the average of a double array values and returns it.
	 * 
	 * @param   a doubleArray  	 
	 * @return average of array's elements/values 
	 */
	public static double avg(double[] doubleArray) {
		double avg=0d;
		if (doubleArray.length !=0) {
			double sum = sum(doubleArray);
			avg = sum/(double)doubleArray.length;
		}
		return avg;
	}



	/**
	 * This utility function builds an printable string of the elements (values) of a given 2D integer array
	 * and returns it. The string could be used in any standard print/output function (such as println)
	 * 
	 * @param   a 2D intArray to be printed 	 
	 * @return a ready-to-be-printed string of array elements/values 
	 */
	public static float[] avgCols2DFloatArray(float[][] twoDfloatArray) {	
		float[] avgArr = new float[twoDfloatArray[0].length];
		for (int col = 0; col < twoDfloatArray[0].length; col++) {
			avgArr[col] = avg(colCopy(twoDfloatArray, col));
		}
		return avgArr;
	}

	/**
	 * This utility function builds an printable string of the elements (values) of a given 2D integer array
	 * and returns it. The string could be used in any standard print/output function (such as println)
	 * 
	 * @param   a 2D intArray to be printed 	 
	 * @return a ready-to-be-printed string of array elements/values 
	 */
	public static double[] avgCols2DDoubleArray(double[][] twoDDoubleArray) {	
		double[] avgArr = new double[twoDDoubleArray[0].length];
		for (int col = 0; col < twoDDoubleArray[0].length; col++) {
			avgArr[col] = avg(colCopy(twoDDoubleArray, col));
		}
		return avgArr;
	}

	/**
	 * This utility function builds an printable string of the elements (values) of a given 2D float array
	 * and returns it. The string could be used in any standard print/output function (such as println)
	 * 
	 * @param   a 2D floatArray to be printed 	 
	 * @return a ready-to-be-printed string of array elements/values 
	 */
	public static String toString(float[][] twoDfloatArray) {	
		String output = ""; 
		for (int row = 0; row < twoDfloatArray.length; row++) {
			float [] aRowArray = rowCopy(twoDfloatArray, row);
			output += "r"+row+Arrays.toString(aRowArray);
			output +="\n";
		}
		return output;
	}

	/**
	 * This utility function builds an printable string of the elements (values) of a given 2D double array
	 * and returns it. The string could be used in any standard print/output function (such as println)
	 * 
	 * @param   a 2D doubleArray to be printed 	 
	 * @return a ready-to-be-printed string of array elements/values 
	 */
	public static String toString(double[][] twoDdoubleArray) {	
		String output = ""; 
		for (int row = 0; row < twoDdoubleArray.length; row++) {
			double [] aRowArray = rowCopy(twoDdoubleArray, row);
			output += "r"+row+Arrays.toString(aRowArray);
			output +="\n";
		}
		return output;
	}

	/**
	 * Add together an arbitrary number of arrays by index i.e. <code>return[i] = a1[i] + a2[i] + ... + an[i]</code>
	 * 
	 * @param arrays arbitrary number of floating point arrays
	 * @return an array which is the sum of the input arrays
	 */
	public static float[] add(float[]... arrays)
	{
		int arrayLength = arrays[0].length;
		float[] returnArray = new float[arrayLength];
		for ( float[] array : arrays)
		{
			for (int i = 0; i < arrayLength; i++)
			{
				returnArray[i] = returnArray[i] + array[i];
			}
		}

		return returnArray;
	}

	/**
	 * Add together an arbitrary number of arrays by index i.e. <code>return[i] = a1[i] + a2[i] + ... + an[i]</code>
	 * 
	 * @param arrays arbitrary number of floating point arrays
	 * @return an array which is the sum of the input arrays
	 */
	public static double[] add(double[]... arrays)
	{
		int arrayLength = arrays[0].length;
		double[] returnArray = new double[arrayLength];
		for ( double[] array : arrays)
		{
			for (int i = 0; i < arrayLength; i++)
			{
				returnArray[i] = returnArray[i] + array[i];
			}
		}

		return returnArray;
	}

	/**
	 * negate the member values of an array 
	 * 
	 * @param array
	 * @return an array where <code>return[i] = - array[i]</code>
	 */
	public static float[] negate(float[] array)
	{
		float[] returnArray = new float[array.length];
		for (int i = 0; i < array.length; i++)
		{
			returnArray[i] = -array[i];
		}
		return returnArray;
	}

	/**
	 * negate the member values of an array 
	 * 
	 * @param array
	 * @return an array where <code>return[i] = - array[i]</code>
	 */
	public static double[] negate(double[] array)
	{
		double[] returnArray = new double[array.length];
		for (int i = 0; i < array.length; i++)
		{
			returnArray[i] = -array[i];
		}
		return returnArray;
	}

	/**
	 * Replaces the portion of the array passed in as the first argument with the content
	 * of the second.
	 * 
	 * NOTE: acts directly on the array passed in.
	 * 
	 * @param destArray
	 * @param replacementArray
	 * @param startIndex
	 */
	public static void replaceRange(float[] destArray, float[] replacementArray,int startIndex)
	{
		// TODO Auto-generated method stub
		for (int counter = 0; counter < replacementArray.length; counter++)
		{
			destArray[startIndex + counter] = replacementArray[counter];
		}
	}

	/**
	 * Replaces the portion of the array passed in as the first argument with the content
	 * of the second.
	 * 
	 * NOTE: acts directly on the array passed in.
	 * 
	 * @param destArray
	 * @param replacementArray
	 * @param startIndex
	 */
	public static void replaceRange(double[] destArray, double[] replacementArray,int startIndex)
	{
		// TODO Auto-generated method stub
		for (int counter = 0; counter < replacementArray.length; counter++)
		{
			destArray[startIndex + counter] = replacementArray[counter];
		}
	}


	/**
	 * @param boolArray
	 * @return
	 */
	public static String[] convertBooleanArrayToString(boolean[] boolArray) 
	{
		// TODO Auto-generated method stub
		String[] returnArray = new String[boolArray.length];

		for (int i = 0; i < boolArray.length; i++)
		{
			returnArray[i] = Boolean.toString(boolArray[i]);
		}

		return returnArray;
	}

	/**
	 * @param floatArray
	 * @return
	 */
	public static String[] convertFloatArrayToString(float[] floatArray) 
	{
		// TODO Auto-generated method stub
		String[] returnArray = new String[floatArray.length];

		for (int i = 0; i < floatArray.length; i++)
		{
			returnArray[i] = Float.toString(floatArray[i]);
		}

		return returnArray;
	}

	/**
	 * @param doubleArray
	 * @return
	 */
	public static String[] convertDoubleArrayToString(double[] doubleArray) 
	{
		// TODO Auto-generated method stub
		String[] returnArray = new String[doubleArray.length];

		for (int i = 0; i < doubleArray.length; i++)
		{
			returnArray[i] = Double.toString(doubleArray[i]);
		}

		return returnArray;
	}

	/**
	 * @param copyOfRange
	 * @param n
	 */
	public static int[] findNSmallestIndices(double[] doubleArray, int n) {
		// TODO Poor algorithm - improve
		ArrayList<Pair<Integer,Double>> returnArrayList = new ArrayList<Pair<Integer,Double>>();
		ArrayList<Pair<Integer,Double>> tiesList = new ArrayList<Pair<Integer,Double>>();

		double tiedValue;
		boolean tiesToResolve = false;
		int[] returnArray = new int[n];
		double[] workingArray = Arrays.copyOf(doubleArray, doubleArray.length);

		if (n < 1)
		{
			System.err.println("ArrayUtils: Trying to find " + n + " smallest indices. Zero or negative count makes no sense");
		}
		if (doubleArray.length < n)
		{
			System.err.println("Trying to find the " + n + " smallest elements of an array with only " + doubleArray.length + " elements!");
		}

		//First check for the easy case, where there are no ties to worry about
		Arrays.sort(workingArray);	
		tiedValue = workingArray[n];
		if(workingArray[n-1] == workingArray[n])
		{
			tiesToResolve = true;
		}

		if (!tiesToResolve)
		{
			//No ties to resolve, so just fill the return Array with the elements less than tiedValue 
			//iterate through for any smaller
			for (int i = 0; i < doubleArray.length; i++)
			{
				Pair<Integer, Double> thisElement = new Pair(i,doubleArray[i]);
				if (thisElement.getSecond() < tiedValue)
				{
					returnArrayList.add(thisElement);
				}
			}

			if (returnArrayList.size() != n)
			{
				System.err.println("Find n smallest failed - sort reported non-tied, but didn't find clear n smallest");
				System.err.println(Arrays.toString(workingArray) + ", " + n + " smallest = " + returnArrayList.toString());
				}
		}
		else
		{
			//We have ties to resolve
			for (int i = 0; i < doubleArray.length; i++)
			{
				Pair<Integer, Double> thisElement = new Pair(i,doubleArray[i]);

				if (doubleArray[i] < tiedValue)
				{
					returnArrayList.add(thisElement);
				}
				else if (doubleArray[i] == tiedValue)
				{
					tiesList.add(thisElement);
				}
			}

			int tiesToSelect = n - returnArrayList.size();
			
			for (int l = 0; l < tiesToSelect; l++)
			{
				int selectedPair = RandomHelper.nextIntFromTo(0, tiesList.size() - 1);
				returnArrayList.add(tiesList.get(selectedPair));
				tiesList.remove(selectedPair);
			}
		}

		for (int k = 0; k < n; k++)
		{
			returnArray[k] = returnArrayList.get(k).getFirst();
		}

		return returnArray;

	}

	/**
	 * @param genes
	 * @return
	 */
	public static double[] genesToDouble(Gene[] genes) {
		// TODO Auto-generated method stub
		double[] returnArray = new double[genes.length];
		int i = 0;
		for(Gene thisGene : genes)
		{
			if(!(thisGene instanceof DoubleGene))
			{
				System.err.println("ArrayUtils: Can't change a non-Double Gene to a Double type!!");
			}
			returnArray[i] = (Double) thisGene.getAllele();	
			i++;
		}

		return returnArray;
	}

	/**
	 * @param kneg
	 * @param kpos
	 * @return
	 */
	public static double[][] zip(double[]... arrs) {
		double[][] returnArr = new double[arrs.length][arrs[0].length];
		
		int i = 0;
		while (i<arrs.length)
		{
			returnArr[i] = arrs[i].clone();
			i++;
		}
		
		return returnArr;
	}
}
