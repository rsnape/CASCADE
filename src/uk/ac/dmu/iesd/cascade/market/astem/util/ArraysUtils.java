package uk.ac.dmu.iesd.cascade.market.astem.util;

import java.util.Arrays;

/**
 * Utility class providing a series of methods for array types.
 * 
 * @author Babak Mahdavi Ardestani
 * @version 1.0 $ $Date: 2012/02/04
 * 
 */

public class ArraysUtils
{

	/**
	 * This utility method checks whether a given array contains any negative
	 * value. It returns <tt>true </tt> if it does, <tt>false</tt> if it does
	 * not
	 * 
	 * */
	public static boolean isContainNegative(double[] doubleArray)
	{
		boolean negativeFound = false;
		int i = 0;
		while (i < doubleArray.length && !negativeFound)
		{
			if (doubleArray[i] < 0)
			{
				negativeFound = true;
			}
			else
			{
				i++;
			}
		}
		return negativeFound;
	}

	/**
	 * This utility function returns a copy of the specific column of a passed
	 * 2D double array as a one dimensional double array.
	 * 
	 * @param twoDfloatArray
	 *            a 2D double array whose column is supposed to be fetched and
	 *            return
	 * @param colNb
	 *            the number of column whose values are supposed to be returned
	 *            as array
	 * @return an array (double) containing a copy of the passed column number
	 *         and 2D array
	 */
	public static double[] colCopy(double[][] twoDdoubleArray, int colNb)
	{
		double[] aCol = new double[twoDdoubleArray.length];
		for (int i = 0; i < twoDdoubleArray.length; i++)
		{
			aCol[i] = twoDdoubleArray[i][colNb];
		}
		return aCol;
	}

	public static double[] avgOfCols2DDoubleArray(double[][] twoDDoubleArray)
	{
		double[] avgArr = new double[twoDDoubleArray[0].length];
		for (int col = 0; col < twoDDoubleArray[0].length; col++)
		{
			avgArr[col] = ArraysUtils.avg(ArraysUtils.colCopy(twoDDoubleArray, col));
		}

		return avgArr;
	}

	public static double[] sumOfCols2DDoubleArray(double[][] twoDDoubleArray)
	{
		double[] sumArr = new double[twoDDoubleArray[0].length];
		for (int col = 0; col < twoDDoubleArray[0].length; col++)
		{
			sumArr[col] = ArraysUtils.sum(ArraysUtils.colCopy(twoDDoubleArray, col));
		}

		return sumArr;
	}

	/**
	 * This utility function returns an array copy containing of passed row
	 * index of a given 2D array
	 * 
	 * @param twoDdoubleArray
	 *            the original 2D double array from which a row will be copied
	 *            and return as an array
	 * @param row
	 *            a row index indicating the row which needs to be taken out
	 *            (copied and returned)
	 * @return an array (double) containing a copy of its needed row (index
	 *         passed as argument)
	 */
	public static double[] rowCopy(double[][] twoDdoubleArray, int row)
	{
		double[] rowCopyArr = new double[twoDdoubleArray[0].length];
		for (int col = 0; col < twoDdoubleArray[0].length; col++)
		{
			rowCopyArr[col] = twoDdoubleArray[row][col];
		}
		return rowCopyArr;
	}

	/**
	 * This utility function builds an printable string of the elements (values)
	 * of a given 2D double array and returns it. The string could be used in
	 * any standard print/output function (such as println)
	 * 
	 * @param a
	 *            2D doubleArray to be printed
	 * @return a ready-to-be-printed string of array elements/values
	 */
	public static String toString(double[][] twoDdoubleArray)
	{
		String output = "";
		for (int row = 0; row < twoDdoubleArray.length; row++)
		{
			double[] aRowArray = ArraysUtils.rowCopy(twoDdoubleArray, row);
			output += "r" + row + Arrays.toString(aRowArray);
			output += "\n";
		}
		return output;
	}

	public static double avg(double[] doubleArray)
	{
		double avg = 0d;
		if (doubleArray.length != 0)
		{
			double sum = ArraysUtils.sum(doubleArray);
			avg = sum / doubleArray.length;
		}
		return avg;
	}

	public static double sum(double[] doubleArray)
	{
		double sum = 0;
		for (double element : doubleArray)
		{
			sum = sum + element;
		}
		return sum;
	}

	public static double[] add(double[]... doubleArrays)
	{
		double[] resArray = new double[doubleArrays[0].length];
		for (double[] array : doubleArrays)
		{
			for (int i = 0; i < doubleArrays[0].length; i++)
			{
				resArray[i] = resArray[i] + array[i];
			}
		}
		return resArray;
	}

	public static int indexOfGivenVal(double[] doubleArray, double val)
	{
		int index = -1;
		for (int i = 0; i < doubleArray.length; i++)
		{
			if (doubleArray[i] == val)
			{
				index = i;
			}
		}
		return index;
	}

}
