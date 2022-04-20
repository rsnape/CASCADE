/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Arrays;

import org.apache.commons.mathforsimplex.analysis.MultivariateRealFunction;
import org.jgap.Chromosome;
import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

import flanagan.math.Fmath;
import flanagan.math.MinimisationFunction;

/**
 * @author J. Richard Snape
 * @author Dennis Fan
 * 
 * 
 * 
 *         New class member to test out Peter B's demand flattening approach
 *         with smart signal Same as class member RecoMinimisationFunction,
 *         apart from the function method is different In this case, equation in
 *         function has change to |Di - Bm|, where <Di> is the same as the
 *         specification described in the paper, and <Bm> is the mean from the
 *         baseline load.
 * 
 *         Last updated: (20/02/14) JRS Re-factored out of Aggregator
 *         implementations
 * 
 *         For full history see git logs.
 * 
 */
public class MinimisationFunctionObjectiveOvernightWind extends FitnessFunction implements MinimisationFunction, MultivariateRealFunction
{

	private static final long serialVersionUID = 1L;

	private double[] arr_B;
	private double[] arr_e;
	private double[][] arr_k;
	private double[] Kneg;
	private double[] Kpos;
	private double[] Cavge;
	private boolean hasSimpleSumConstraint = false;
	private boolean lessThanConstraint;
	private double sumConstraintValue;
	private double penaltyWeight = 1.0e10;
	private double sumConstraintTolerance;
	private double mean_B;
	private boolean hasEqualsConstraint = false;
	private int numEvaluations = 0;
	private int settlementPeriod;
	boolean printD = false;

	@Override
	public double function(double[] arr_S)
	{
		double m = 0d;
		double[] d = new double[this.arr_B.length];
		// mean_B = ArrayUtils.avg(arr_B);

		// Note - interestingly - this will predict Baseline + Cavge for a zero
		// signal. This works. But if you make it predict Baseline for a zero
		// signal, it blows up!! Rather sensitive...
		for (int i = 0; i < arr_S.length; i++)
		{
			if (arr_S[i] < 0)
			{
				d[i] = this.arr_B[i] + (arr_S[i] * this.Kneg[i] * this.arr_B[i]) + this.Cavge[i];
			}
			else
			{
				d[i] = this.arr_B[i] + (arr_S[i] * this.Kpos[i] * this.arr_B[i]) + this.Cavge[i];
			}
			// m += Math.abs(di - mean_B);
		}

		m = ArrayUtils.sum(ArrayUtils.absoluteValues(ArrayUtils.offset(d, -ArrayUtils.avg(d))));

		// For overnight wind - use this
		double[] windDesire = new double[48];
		Arrays.fill(windDesire, -1d / 34);
		for (int i = 0; i < 15; i++)
		{
			windDesire[i] = 1d / 14;
		}
		if (this.printD)
		{
			System.err.println(Arrays.toString(ArrayUtils.offset(ArrayUtils.multiply(windDesire, ArrayUtils.avg(d)), ArrayUtils.avg(d))));
		}

		m = ArrayUtils
				.sum(ArrayUtils.add(d, ArrayUtils.negate(ArrayUtils.offset(ArrayUtils.multiply(windDesire, ArrayUtils.avg(d)), ArrayUtils
						.avg(d)))));

		// m=(ArrayUtils.max(d)/ArrayUtils.avg(d))*1000;
		this.numEvaluations++;
		m += this.checkPlusMin1Constraint(arr_S);
		// m += checkPosNegConstraint(arr_S);

		return m;
	}

	private double checkPlusMin1Constraint(double[] arr_S)
	{
		double penalty = 0;
		double posValueSum = 0;
		double negValueSum = 0;
		for (double element : arr_S)
		{
			if (element > 1 && element > posValueSum)
			{
				posValueSum = element;
			}
			else if (element < -1 && element < negValueSum)
			{
				negValueSum = element;
			}
		}

		if (posValueSum > 1)
		{
			penalty += this.penaltyWeight * Math.pow((posValueSum - 1), 2);
		}

		if (negValueSum < -1)
		{
			penalty += this.penaltyWeight * Math.pow((-1 - negValueSum), 2);
		}

		return penalty;
	}

	/**
	 * Enforce the constraint that all positive values of S must sum to
	 * (maximum) of 1 and -ve values to (minimum) of -1
	 * 
	 * @param arr_S
	 * @return
	 */
	private double checkPosNegConstraint(double[] arr_S)
	{
		double penalty = 0;
		double posValueSum = 0;
		double negValueSum = 0;
		for (double element : arr_S)
		{
			if (element > 0)
			{
				posValueSum += element;
			}
			else
			{
				negValueSum += element;
			}
		}

		/*
		 * if (posValueSum > 1) { penalty += this.penaltyWeight *
		 * Math.pow((posValueSum - 1), 2); }
		 * 
		 * if (negValueSum < -1) { penalty += this.penaltyWeight * Math.pow((-1
		 * - negValueSum), 2); }
		 */

		penalty += this.penaltyWeight * Math.pow((posValueSum + negValueSum), 2);

		return penalty;
	}

	@Override
	public double value(double[] arr_S)
	{
		double penalties = 0;
		// Add on constraint penalties here (as the Apache NelderMead doesn't do
		// constraints itself)
		double sumOfArray = ArrayUtils.sum(arr_S);

		if (this.hasEqualsConstraint && (Math.sqrt(Math.pow(sumOfArray - this.sumConstraintValue, 2)) > this.sumConstraintTolerance))
		{
			penalties = this.penaltyWeight * Fmath.square(this.sumConstraintValue * (1.0 - this.sumConstraintTolerance) - sumOfArray);
		}
		return this.function(arr_S) + penalties;
	}

	public void addSimpleSumEqualsConstraintForApache(double limit, double tolerance)
	{
		this.hasEqualsConstraint = true;
		this.sumConstraintTolerance = tolerance;
		this.sumConstraintValue = limit;

	}

	public void set_B(double[] b)
	{
		this.arr_B = b;
	}

	public void set_e(double[] e)
	{
		this.arr_e = e;
	}

	public void set_k(double[][] k)
	{
		this.arr_k = k;
	}

	public int getNumEvals()
	{
		return this.numEvaluations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jgap.FitnessFunction#evaluate(org.jgap.Chromosome)
	 */
	@Override
	protected double evaluate(IChromosome arg0)
	{

		double[] testArray = ArrayUtils.genesToDouble(arg0.getGenes());
		for (int i = 0; i < testArray.length; i++)
		{
			testArray[i] -= 0.5;
			testArray[i] *= 2;
		}

		return Math.max(1, (100000 - this.value(testArray)));
	}

}
