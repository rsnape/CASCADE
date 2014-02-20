/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import org.apache.commons.mathforsimplex.analysis.MultivariateRealFunction;
import org.jgap.Chromosome;
import org.jgap.FitnessFunction;

import flanagan.math.Fmath;
import flanagan.math.MinimisationFunction;

/**
 * @author Richard
 *
 */
public class MinimisationFunctionNeuralNetReady extends FitnessFunction implements MinimisationFunction, MultivariateRealFunction  {

		private static final long serialVersionUID = 1L;

		private double[] arr_C;
		private double[] arr_B;
		private double[] arr_e;		
		private double[][] arr_k;
		private boolean hasSimpleSumConstraint = false;
		private boolean lessThanConstraint;
		private double sumConstraintValue;
		private double penaltyWeight  = 1.0e10;
		private double sumConstraintTolerance;
		private boolean hasEqualsConstraint = false;
		private int numEvaluations = 0;

		public double function (double[] arr_S) {
			double m =0d;

			for (int i=0; i<arr_S.length; i++){

				double sumOf_SjkijBi =0;
				for (int j=0; j<arr_S.length; j++){
					if (i != j)
						sumOf_SjkijBi += arr_S[j] * arr_k[i][j] * arr_B[i];
				}

				m += arr_C[i] * (arr_B[i] + (arr_S[i]*arr_e[i]*arr_B[i]) + (arr_S[i]*arr_k[i][i]*arr_B[i]) + sumOf_SjkijBi);
			}
			numEvaluations++;
			m += checkPosNegConstraint(arr_S);
			return m;
		} 

		/**
		 * Enforce the constraint that all positive values of S must sum to (maximum) of 1
		 * and -ve values to (minimum) of -1
		 * @param arr_S
		 * @return
		 */
		private double checkPosNegConstraint(double[] arr_S) {
			double penalty = 0;
			double posValueSum = 0;
			double negValueSum = 0;
			for (int i = 0; i < arr_S.length; i++)
			{
				if (arr_S[i] > 0)	{
					posValueSum += arr_S[i];
				}
				else	{
					negValueSum += arr_S[i];
				}
			}

			if (posValueSum > 1) {
				penalty += this.penaltyWeight * Math.pow((posValueSum - 1), 2);
			}
			
			if (negValueSum < -1)	{
				penalty += this.penaltyWeight * Math.pow((-1 - negValueSum), 2);
			}
			
			return penalty;
		}

		public double value (double[] arr_S) {
			double penalties = 0;
			// Add on constraint penalties here (as the Apache NelderMead doesn't do constraints itself)
			double sumOfArray = ArrayUtils.sum(arr_S);


			if (this.hasEqualsConstraint  && (Math.sqrt(Math.pow(sumOfArray - sumConstraintValue, 2)) > this.sumConstraintTolerance))	{
				penalties = this.penaltyWeight*Fmath.square(sumConstraintValue*(1.0-this.sumConstraintTolerance)-sumOfArray);
			}
			return function(arr_S) + penalties;
		}

		public void addSimpleSumEqualsConstraintForApache(double limit, double tolerance)	{
			this.hasEqualsConstraint = true;
			this.sumConstraintTolerance = tolerance;
			this.sumConstraintValue = limit;

		}

		public void set_C(double [] c) {
			arr_C = c;
		}
		

		public void set_B(double [] b) {
			arr_B = b;
		}

		public void set_e(double [] e) {
			arr_e = e;
		}

		public void set_k(double [][] k ) {
			arr_k = k;
		}

		public int getNumEvals() {
			return numEvaluations;
		}

		/* (non-Javadoc)
		 * @see org.jgap.FitnessFunction#evaluate(org.jgap.Chromosome)
		 */
		@Override
		protected int evaluate(Chromosome arg0) {

			double[] testArray = ArrayUtils.genesToDouble(arg0.getGenes());
			for (int i = 0; i < testArray.length; i++)	{
				testArray[i] -= 0.5;
				testArray[i] *= 2;
			}

			return (int) Math.max(1,(100000 - value(testArray)));
		}


}
