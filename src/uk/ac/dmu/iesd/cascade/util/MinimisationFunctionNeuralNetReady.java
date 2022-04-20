/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import org.apache.commons.mathforsimplex.analysis.MultivariateRealFunction;
import org.jgap.Chromosome;
import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

import flanagan.math.Fmath;
import flanagan.math.MinimisationFunction;

/**
 * @author Richard
 * 
 */
public class MinimisationFunctionNeuralNetReady extends FitnessFunction implements MinimisationFunction, MultivariateRealFunction
{

	private static final long serialVersionUID = 1L;

	private double[] arr_C;
	private double[] arr_B;
	private double[] arr_e;
	private double[][] arr_k;
	private boolean hasSimpleSumConstraint = false;
	private boolean lessThanConstraint;
	private double sumConstraintValue;
	private double penaltyWeight = 1.0e10;
	private double sumConstraintTolerance;
	private boolean hasEqualsConstraint = false;
	private int numEvaluations = 0;

	@Override
	public double function(double[] arr_S)
	{
		double m = 0d;

		for (int i = 0; i < arr_S.length; i++)
		{

			double sumOf_SjkijBi = 0;
			for (int j = 0; j < arr_S.length; j++)
			{
				if (i != j)
				{
					sumOf_SjkijBi += arr_S[j] * this.arr_k[i][j] * this.arr_B[i];
				}
			}

			m += this.arr_C[i]
					* (this.arr_B[i] + (arr_S[i] * this.arr_e[i] * this.arr_B[i]) + (arr_S[i] * this.arr_k[i][i] * this.arr_B[i]) + sumOf_SjkijBi);
		}
		this.numEvaluations++;
		m += this.checkPosNegConstraint(arr_S);
		return m;
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

	public void set_C(double[] c)
	{
		this.arr_C = c;
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


	/* (non-Javadoc)
	 * @see org.jgap.FitnessFunction#evaluate(org.jgap.IChromosome)
	 */
	@Override
	protected double evaluate(IChromosome arg0) {
		// TODO Auto-generated method stub

		double[] testArray = ArrayUtils.genesToDouble(arg0.getGenes());
		for (int i = 0; i < testArray.length; i++)
		{
			testArray[i] -= 0.5;
			testArray[i] *= 2;
		}

		return Math.max(1, (100000 - this.value(testArray)));
	}

}
