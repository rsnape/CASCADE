/**
 * 
 */
package prosumermodel;

/**
 * @author jsnape
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

}
