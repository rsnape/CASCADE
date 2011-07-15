/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Arrays;

/**
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
	
	
	/*
	 * Tried a different implementation here - could test which is quicker...
	 */
	public static double[] convertStringArrayToDoubleArray(String[] array)
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

	
	
	public static int sum(int[] intArray)
	{
		int sum = 0;
		for (int i = 0; i < intArray.length; i++)
		{
			sum = sum + intArray[i];
		}
		return sum;
	}
	
	public static double sum(double[] doubleArray)
	{
		double sum = 0;
		for (int i = 0; i < doubleArray.length; i++)
		{
			sum = sum + doubleArray[i];
		}
		return sum;
	}
	
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
	 * mtimes
	 * 
	 * function to multiply the values of two time series arrays
	 * together element by element
	 * 
	 * @param array1
	 * @param array2
	 * @return array of floats containing multiplied values of time series i.e.
	 * 			[a0b0 a1b1 a2b2 .... anbn]
	 */
	public static float[] mtimes(float[] array1, float[] array2)
	{
		float [] returnArray = null;
		
		if (array1.length != array2.length)
		{
			System.err.println("Tried to convolve arrays of different lengths  " + array1.length + array2.length + " result - undefined");
		}
		else
		{
			returnArray = new float [array1.length];
			for(int i = 0; i < array1.length; i++)
			{
				returnArray[i] = array1[i] * array2[i];
			}
		}
		
		return returnArray;
	}
	
	public static float dotProduct(float[] array1, float[] array2)
	{
		float dotVal = 0;
		
		if (array1.length != array2.length)
		{
			System.err.println("Tried to dot product arrays of different lengths " + array1.length + array2.length + " result - undefined");
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
	
	public static float[] normalizeValues(float[] array)
	{
		return normalizeValues(array, 1f);
	}
	
	public static float[] normalizeValues(float[] array, float maxMagnitude)
	{
		return normalizeValues(array, maxMagnitude, true);
		                                 
	}
	
	public static float[] normalizeValues(float[] array, float maxMagnitude, boolean allowNegative)
	{
		float [] returnArray = null;
		
		if(allowNegative) {
			returnArray = multiply(array, (1 / max(array) * maxMagnitude));
		}
		else
		{
			float minVal = 0;
			minVal = Math.min(minVal, min(array));
			returnArray = multiply(offset(array, minVal), (1 / max(array) * maxMagnitude));
			
		}
		return returnArray;
	}
	
	public static float[] multiply(float[] array, float multiplier)
	{
		float[] returnArray = new float [array.length];
		
		for (int i = 0; i < array.length; i++)
		{
			returnArray[i] = multiplier * array[i];
		}
		 
		return returnArray;
	}
	
	public static float max( float[] array)
	{
		float maxVal = Float.MIN_VALUE;
		
		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] > maxVal) {maxVal = array[i];}			
		}
		
		return maxVal;
	}
	
	public static float min( float[] array)
	{
		float minVal = Float.MAX_VALUE;
		
		for(int i = array.length - 1; i >= 0; --i)
		{
			if (array[i] < minVal) {minVal = array[i];}			
		}
		
		return minVal;
	}
	
	public static float[] offset (float[] array, float offset)
	{
		for(int i = array.length - 1; i >= 0; --i)
		{
			array[i] = array[i] + offset;			
		}
		
		return array;
	}
	
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
	
	
	
	
	
   public static float[] pow2(float[] floatArrayBase) {	
		/*float[] powArray = new float[floatArrayBase.length];
		for(int i = 0; i<floatArrayBase.length; i++) {
			powArray[i]= (float)Math.pow(floatArrayBase[i], 2);	 */
	  return mtimes(floatArrayBase,floatArrayBase);
	}
	
   /**
	 * This utility function returns an array of double values after 
	 * raising the values of first argument (passed float array) to the power of second
	 * argument (exp). 
	 * @param floatArrayBase whose base element values will be raised by <tt>exp</tt> value  
	 * @param exp the exponent    	 	 
	 * @return double array containing the initial array values raised by <tt>exp</tt> value  
	 */
	public static double[] pow(float[] floatArrayBase, float exp ) {	
		
		if (floatArrayBase == null)  {
			return null;   
		}   
		double[] powArray = new double[floatArrayBase.length];
		
		for(int i = 0; i<floatArrayBase.length; i++) {
			powArray[i]=Math.pow(floatArrayBase[i], exp);
		}
		
		return powArray;	
	}
	
	/**
	 * This utility function accept a double array as an argument and returns 
	 * a float array containing converted double values of the passed array (argument).
	 * This is not a good/wise thing to do as information/precision could be lost, but it is
	 * written if it would be necessary to use it!  
	 * @param doubleArray whose  <tt>double</tt> element values will be converted to <tt>float</tt>  
	 * @return array containing the passed array values (as argument) converted to <tt>float</tt>  
	 */
	public static float[] convertDoubleArrayToFlatArray(double[] array){ 
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
	 * This utility function accept a float array as an argument and returns 
	 * a double array containing converted double values of the passed array (argument).
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
	 * This utility function verifies if the sum of the values in
	 * the given array (float) are equal zero. If the sum is zero it returns true; otherwise false
	 * @param   floatArray to be verified if its sum is equal zero 	 
	 * @return true if the sum of array elements are zero; false otherwise 
	 */
	public static boolean isSumEqualZero(float[] floatArray) {	
		boolean sumIsEqualZero = false;
		float sum= sum(floatArray);
		if (sum ==0)
			sumIsEqualZero=true;
		//System.out.println("ArryUtils.isSumEqualZero: "+sum);
		return sumIsEqualZero;
	}
	
	/**
	 * This utility function verifies if the sum of the values in
	 * the given array (int) are equal zero. If the sum is zero it returns true; otherwise false
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
	 * This utility function returns a sub-array copy of a given 2D array (with the same columns' size)
	 * by passing/specifying a startRow and endRow indices.
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
	 * This utility function returns an array copy containing of passed row index of a given 2D array 
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
	 * This utility function calculates the average of an float array values and returns it.
	 * @param   a floatArray  	 
	 * @return average of array's elements/values 
	 */
	public static float avg(float[] floatArray) {
		float avg=0;
		if (floatArray.length !=0) {
			float sum = sum(floatArray);
			avg = sum/floatArray.length;
		}
		return avg;
	}


	/**
	 * This utility function builds an printable string of the elements (values) of a given 2D integer array
	 * and returns it. The string could be used in any standard print/output function (such as println)
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
	 * This utility function builds an printable string of the elements (values) of a given 2D float array
	 * and returns it. The string could be used in any standard print/output function (such as println)
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
}
