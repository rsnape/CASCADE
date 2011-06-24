/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi 
 * @version $Revision: 1.02 $ $Date: 2011/06/23 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial basic functionality
 * 1.01 - added dot product functionality and normalization
 * 1.02 - added method isSumEqualZero(floatArray); isSumEqualZero(intArray) [Babak] 
 *
 */
public class ArrayUtils {
	
	public static int[] convertStringArraytoIntArray(String[] sarray) {
		if (sarray != null) {
			int intarray[] = new int[sarray.length];
			for (int i = 0; i < sarray.length; i++) {
				intarray[i] = Integer.parseInt(sarray[i]);
			}
			return intarray;
		}
		return null;
	}
	
	public static float[] convertStringArraytofloatArray(String[] sarray) {
		if (sarray != null) {
		float floatarray[] = new float[sarray.length];
		for (int i = 0; i < sarray.length; i++) {
		floatarray[i] = Float.parseFloat(sarray[i]);
		}
		return floatarray;
		}
		return null;
		}
	
	public static double[] convertStringArraytodoubleArray(String[] sarray) {
		if (sarray != null) {
		double doublearray[] = new double[sarray.length];
		for (int i = 0; i < sarray.length; i++) {
		doublearray[i] = Double.parseDouble(sarray[i]);
		
		}
		return doublearray;
		}
		return null;
		}
	
	
	public static int sum(int[] intArray)
	{
		int sum = 0;
		//for (int i = 1; i < intArray.length; i++)
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
			System.err.println("Tried to dot product arrays of differetn length - undefined");
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
			System.err.println("Tried to dot product arrays of differetn length - undefined");
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
	 * @param  col the number of column whose values are supposed to be returned as array  	 
	 * @return an array (float) containing a copy of the passed column number and 2D array 
	 */
	public static float[] getColCopy(float[][] twoDfloatArray, int col) {
		float[] aCol = new float[twoDfloatArray.length];
		for (int i = 0; i < twoDfloatArray.length; i++) {
	        aCol[i]=twoDfloatArray[i][col];
	    }
		return aCol;
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
	 * This utility function builds an printable string of the elements (values) of a given float array
	 * and returns it. The string could be used in any standard print/output function (such as println)
	 * @param   a floatArray to be printed 	 
	 * @return a ready-to-be-printed string of array values 
	 */
	public static String getPrintableOutputForFloatArray(float[] floatArray) {	
		String output = "["; 
		for (int i = 0; i < floatArray.length; i++) {
			output += " " + floatArray[i];
		}
		output += "]";
		return output;
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
			avgArr[col] = avg(getColCopy(twoDfloatArray, col));
		}
		return avgArr;
	}

	/**
	 * This utility function builds an printable string of the elements (values) of a given 2D float array
	 * and returns it. The string could be used in any standard print/output function (such as println)
	 * @param   a 2D floatArray to be printed 	 
	 * @return a ready-to-be-printed string of array elements/values 
	 */
	public static String getPrintableOutputFor2DFloatArray(float[][] twoDfloatArray) {	
		String output = ""; 
		for (int row = 0; row < twoDfloatArray.length; row++) {
			output += "r"+row+" [";
			for (int col = 0; col < twoDfloatArray[row].length; col++) {
				output += " " + twoDfloatArray[row][col];
			}
			output += "]"+"\n";
		}
		return output;
	}
	

}
