/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

/**
 * @author J. Richard Snape
 * @version $Revision: 1.01 $ $Date: 2010/12/09 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial basic functionality
 * 1.01 - added dot product functionality and normalization
 *
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
		for (int i = 1; i < intArray.length; i++)
		{
			sum = sum + intArray[i];
		}
		return sum;
	}
	
	public static double sum(double[] doubleArray)
	{
		double sum = 0;
		for (int i = 1; i < doubleArray.length; i++)
		{
			sum = sum + doubleArray[i];
		}
		return sum;
	}
	
	public static float sum(float[] floatArray)
	{
		float sum = 0;
		for (int i = 1; i < floatArray.length; i++)
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
	
}
