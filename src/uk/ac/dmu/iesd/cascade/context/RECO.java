/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import static repast.simphony.essentials.RepastEssentials.FindNetwork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import cern.colt.list.DoubleArrayList;
import bsh.This;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.ContextUtils;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import flanagan.*;
import flanagan.math.*;
import flanagan.analysis.*;
import flanagan.math.Matrix;

import org.apache.commons.mathforsimplex.FunctionEvaluationException;
import org.apache.commons.mathforsimplex.analysis.MultivariateRealFunction;
import org.apache.commons.mathforsimplex.linear.ArrayRealVector;
import org.apache.commons.mathforsimplex.optimization.GoalType;
import org.apache.commons.mathforsimplex.optimization.OptimizationException;
import org.apache.commons.mathforsimplex.optimization.RealPointValuePair;
import org.apache.commons.mathforsimplex.optimization.SimpleScalarValueChecker;
import org.apache.commons.mathforsimplex.optimization.direct.NelderMead;
import org.apache.commons.mathforsimplex.optimization.linear.LinearConstraint;
import org.apache.commons.mathforsimplex.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.mathforsimplex.optimization.linear.Relationship;
import org.apache.commons.mathforsimplex.optimization.linear.SimplexSolver;
import org.jgap.*;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;

/**
 * A <em>RECO</em> or a Retail Company is a concrete object that represents 
 * a commercial/business electricity/energy company involved in retail trade with
 * prosumer agents (<code>ProsumerAgent</code>), such as household prosumers (<code>HouseholdProsumer</code>)
 * In other words, a <code>RECO</code> provides electricity/energy to certain types of prosumers (e.g. households)
 * It will be also involved in wholesale trade market.<p>
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/18 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history)
 * 
 * 1.0 -  created the concrete split of categories of prosumer from the abstract class representing all prosumers
 * 
 */

public class RECO extends AggregatorAgent{


	class RecoMinimisationFunction extends FitnessFunction implements MinimisationFunction, MultivariateRealFunction {

		private static final long serialVersionUID = 1L;

		private float[] arr_C;
		private float[] arr_B;
		private float[] arr_e;		
		private float[][] arr_k;
		private boolean hasSimpleSumConstraint = false;
		private boolean lessThanConstraint;
		private double sumConstraintValue;
		private double penaltyWeight  = 1.0e30;
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
			return m;
		} 

		public double value (double[] arr_S)
		{
			double penalties = 0;
			// Add on constraint penalties here (as the Apache NelderMead doesn't do constraints itself)
			double sumOfArray = ArrayUtils.sum(arr_S);


			if (this.hasEqualsConstraint  && (Math.sqrt(Math.pow(sumOfArray - sumConstraintValue, 2)) > this.sumConstraintTolerance))
			{
				penalties = this.penaltyWeight*Fmath.square(sumConstraintValue*(1.0-this.sumConstraintTolerance)-sumOfArray);
			}
			return function(arr_S) + penalties;
		}

		private void addSimpleSumEqualsConstraintForApache(double limit, double tolerance)
		{
			this.hasEqualsConstraint = true;
			this.sumConstraintTolerance = tolerance;
			this.sumConstraintValue = limit;

		}

		public void set_C(float [] c) {
			arr_C = c;
		}

		public void set_B(float [] b) {
			arr_B = b;
		}

		public void set_e(float [] e) {
			arr_e = e;
		}

		public void set_k(float [][] k ) {
			arr_k = k;
		}

		public int getNumEvals()
		{
			return numEvaluations;
		}

		/* (non-Javadoc)
		 * @see org.jgap.FitnessFunction#evaluate(org.jgap.Chromosome)
		 */
		@Override
		protected int evaluate(Chromosome arg0) {

			double[] testArray = ArrayUtils.genesToDouble(arg0.getGenes());
			for (int i = 0; i < testArray.length; i++)
			{
				testArray[i] -= 0.5;
				testArray[i] *= 2;
			}

			return (int) Math.max(1,(100000 - value(testArray)));
		}

	}

/**
 * Babak test implementation
 **/


	/*	class  RecoMultivariateRealFunction implements MultivariateRealFunction /*,  RealConvergenceChecker  {

		private float[] arr_C;
		private float[] arr_B;
		private float[] arr_e;		
		private float[][] arr_k;

		public double value (double[] arr_S) {
			double m =0d;

			for (int i=0; i<arr_S.length; i++){

				double sumOf_SjkijBi =0;
				for (int j=0; j<arr_S.length; j++){
					if (i != j)
						sumOf_SjkijBi += arr_S[j] * arr_k[i][j] * arr_B[i];
				}

				m+= arr_C[i] * (arr_B[i] + (arr_S[i]*arr_e[i]*arr_B[i]) + (arr_S[i]*arr_k[i][i]*arr_B[i]) + sumOf_SjkijBi);
			}

			return m;
		} 

		public void set_C(float [] c) {
			arr_C = c;
		}

		public void set_B(float [] b) {
			arr_B = b;
		}

		public void set_e(float [] e) {
			arr_e = e;
		}

		public void set_k(float [][] k ) {
			arr_k = k;
		}

		public boolean converged(int iteration, RealPointValuePair previous, RealPointValuePair current) {

			return true;

		} 

	} */

	/**
	 * the aggregator agent's base name  
	 **/	
	protected static String agentBaseName = "RECO";

	/**
	 * indicates whether the agent is in training mode/ period
	 **/	
	protected boolean isTraining = true;

	/**
	 * indicates whether the agent is in 'profile building' mode/period  
	 **/	
	protected boolean isProfileBuidling = true;


	/**
	 * This field (e) is "price elasticity factor" at timeslot i
	 * It is extend to which total demand in the day is reduced or increased by the value of S
	 * (given S*e) 
	 * there are 48 of them (day divided into 48 timeslots)
	 * When a single no zero value of S is broadcast by the aggregator in the ith timeslot, 
	 * the total aggregated response from the prosumers will involve changes to demand form 
	 * baseline in some or all timeslots. If those changes are added up for the day, the 
	 * net value which may be + or - tells us the value of S*e. 
	 * Since we know S, we can get the e for the ith timeslot in which the S was broadcast. 
	 **/
	float[] arr_i_e; 

	/**
	 * This field (k) is "displacement factor" at timeslot ij
	 * There are 48^2 of them (48 values at each timeslot; a day divided into 48 timeslots)
	 * It is calculated in the training process. 
	 **/
	float[][] arr_ij_k; 

	/**
	 * This field (S) is "signal" at timeslot i sent to customer's (prosumers)
	 * so that they response to it accordingly. For example, if S=0, the aggregator can 
	 * assume that the prosumers respond with their default behavior, so there is 
	 * no timeshifting or demand or elastic response to price and the result is a baseline
	 * demand aggregate B
	 * When Si is not zero, the aggregator can calculate 
	 * the resultant aggregate deviation (delta_Bi)
	 **/
	float[] arr_i_S;  // (S) signal at timeslot i

	float[] arr_i_B;  // (B) baseline at timeslot i

	/**
	 * This field (C) is the "marginal cost" per KWh in the ith timeslot 
	 * (which aggregator can predict 24 hours ahead).
	 * This is the cost for the part that cosumer's demand that cannot be predicted further
	 * ahead and supplied via long term contracts because it is variable due to weather, TV schedules, or owns
	 * generators not being able to meet part of the predicated demand. 
	 * For the inital experiment, C would be proportional to national demand Ni with 
	 * the option to make it Ni^2 or fractional power
	 * 
	 * TODO: RS - I think this is so generic it should go in the AggregatorAgent super-class
	 **/
	float[] arr_i_C; 

	/**
	 * This 2D-array is used to keep the usual aggregate demand of all prosumers at each timeslot 
	 * of the day during both profile building and training periods (usually about 7 + 48 days). 
	 * In other words, the length of the columns of this 2D history array is equal to number of timeslot 
	 * during a day and the length of its rows is equal to the number of days the profile building and training 
	 * periods last.
	 *  
	 * TODO: if all the aggregator have this default behavior (e.g. building profile in the same way)
	 * this field may stay here otherwise, it will need to move to the appropriate implementor (e.g. RECO) 
	 **/
	float[][] hist_arr_ij_D;

	private boolean firstTimeMinimisation = true;

	private float[][] weekDemandHistory;

	private float[] daysDemandHistory;

	/**
	 * A very simple learning factor implementation.  There will be more
	 * sophisticated implementations in time
	 */
	private float alpha;

	private ArrayList<Float> dailyPredictedCost;

	private ArrayList<Float> dailyActualCost; 

	/**
	 * This array is used to keep the average (i.e. baseline) of aggregate demands (D)
	 * (usually kept in the 2D history D array (hist_D_ij_arr))  
	 * TODO: if all the aggregator have this default behavior (e.g. building profile in the same way)
	 * this field may stay here otherwise, it will need to move to the appropriate implementor (e.g. RECO) 
	 **/
	//float[] histAvg_B_i_arr; 


	/**
	 * This method calculates and returns the price (Pi) per kWh 
	 * at given time-slot.
	 * (It implements the formula proposed by P. Boait, Formula #2) 
	 * Parameters a and b are "fixed pricing" parameters  and set by the aggregator and 
	 * known to the prosumer's smart meter and other devices so they react to Pi.  
	 * The summation of all Si to zero simplifies their calculation!
	 * In practice, it would be helpful to use normalized values between 1 and -1 to make up S
	 * @param   timeslot time slot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return Price(Pi) per KWh at given timeslot (i) 
	 */
	protected float calculate_Price_P(int timeslot) {
		float a = 2f; // the value of a must be set to a fixed price, e.g. ~baseline price 
		float b = 0.2f;  //this value of b amplifies the S value signal, to reduce or increase the price

		float Si = this.arr_i_S[timeslot];
		float Pi = a+ (b*Si);

		return Pi;
	}


	/**
	 * This method calculates and returns the baseline aggregate deviation (DeltaBi) at 
	 * given time-slot.
	 * It is invoked by aggregator when the Si is not zero and the aggregator
	 * wants to form a response.
	 * (It implements the formula proposed by P. Boait, Formula #3) 
	 * @param   timeslot_i time slot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return Baseline aggregate deviation (DelatBi) at given timeslot (i) 
	 */

	private float calculate_deltaB(int timeslot_i) {	
		float sumOf_SjKijBi=0;
		for (int j = 0; j < ticksPerDay; j++) {
			if (j != timeslot_i) // i!=j
				sumOf_SjKijBi = this.arr_i_S[timeslot_i]*this.arr_ij_k[timeslot_i][j]*this.arr_i_B[timeslot_i];
		}
		float leftSideEq = this.arr_i_S[timeslot_i]*this.arr_ij_k[timeslot_i][timeslot_i]*this.arr_i_B[timeslot_i];
		float deltaBi = leftSideEq + sumOf_SjKijBi;
		return deltaBi;
	}

	/**
	 * This method calculates and returns the demand predicted by the aggregator in each
	 * time-slot, taking account of both elastic and displacement changes.
	 * (It implements the formula proposed by P. Boait, Formula #4)
	 * When e=0 (i.e. all ei=0), then the sum of all Di = sum of all Bi
	 * @param   timeslot time slot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return Demand (Di) predicted by the aggregator at given timeslot (i) 
	 */
	protected float calcualte_PredictedDemand_D(int timeslot) {
		float Bi = this.arr_i_B[timeslot];
		float Si = this.arr_i_S[timeslot];
		float ei = this.arr_i_e[timeslot];

		float delta_Bi = calculate_deltaB(timeslot);

		float Di= Bi + (Si*ei*Bi) + delta_Bi;
		return Di;
	}

	/**
	 * This method calculates and returns "price elasticity factor" (e) at a given time-slot.
	 * (It implements the formula proposed by P. Boait, Formula #6)
	 * @param arr_D a float array containing aggregate demand (D) values for a timeslot of a day (usually 48 timeslots)
	 * @param arr_B a float array containing average baseline aggregate demand (B) values for each timeslot of a day (usulaly 48 timeslots) 
	 * @param s signal value at timeslot i
	 * @param B average baseline aggregate demand (B) value at timeslot i	 	 
	 * @return elasticity price factor (e) [at timeslot i]
	 */
	protected float calculate_e(float[] arr_D, float[] arr_B, float s, float B) {

		float e=0;
		float sum_D = ArrayUtils.sum(arr_D);
		float sum_B = ArrayUtils.sum(arr_B);
		if (( s!=0) && (B!=0))
			e = (sum_D - sum_B) / (s*B);
		return e;
	}


	/**
	 * This method calculates and returns "displacement factor" (k) at given time-slots.
	 * (It implements the formula proposed by P. Boait, Formula #7 and #8)
	 * By stepping Si=1 through all the timeslots over 48 days, the aggregator obtains
	 * complete set of estimates for e and k. By repeating this training (if necessary) more
	 * accurate estimates can be obtained. 
	 * @param   timeslot time slot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return displacement factor (Kij) at given timeslots (i and j) 
	 */
	protected float calculate_k(int t_i, int t_j) {

		float k_ij = 0;
		float divisor = 1;

		if (t_i == t_j) {  // calculate Kii
			float delta_Bi= this.calculate_deltaB(t_i);
			float divident = delta_Bi - (this.arr_i_S[t_i] * this.arr_i_e[t_i] * this.arr_i_B[t_i]);
			divisor= this.arr_i_S[t_i] * this.arr_i_B[t_i];
			k_ij = divident/divisor;
		}

		else {  // calculate Kij
			float delta_Bj= this.calculate_deltaB(t_j);
			divisor= this.arr_i_S[t_i] * this.arr_i_B[t_j];
			k_ij = delta_Bj /divisor;
		}

		return k_ij;
	}

	/**
	 * This method calculates the predict demand at each given time-slot.
	 * @param   t timeslot of the day (often/usually 1 day = 48 timeslot)	 
	 * @return displacement factor (Kij) at given timeslots (i and j) 
	 */
	protected void predictDemand(List<ProsumerAgent> customersList, int t) {

		float sumDemand = 0;
		for (ProsumerAgent a : customersList)
		{
			sumDemand = sumDemand + a.getNetDemand();
		}
		this.arr_i_B[t] = sumDemand;

	}

	private void setB_and_e(List<ProsumerAgent> customers, int time, boolean isTraining) {
		float sumDemand = 0;
		float sum_e =0;
		for (ProsumerAgent agent : customers) {
			sumDemand = sumDemand + agent.getNetDemand();
			if (!isTraining)
				sum_e = sum_e+agent.getElasticityFactor();
		}

		this.arr_i_B[time]=sumDemand;

		if (!isTraining)
			this.arr_i_e[time]= sum_e;
	}

	/**
	 * This method returns the cost of buying wholesale electricity for this aggregator
	 * at the current tick
	 * 
	 * TODO: RS - I think this is so generic it should go in the AggregatorAgent super-class
	 */
	public float getCurrentCost()
	{
		return arr_i_C[(int) RepastEssentials.GetTickCount() % ticksPerDay];
	}


	/**
	 * This method returns the list of customers (prosusmers) 
	 * in the economic network of this aggregator
	 * @return List of customers of type <tt> ProsumerAgent</tt>  
	 */
	private List<ProsumerAgent> getCustomersList() {
		List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
		//List<RepastEdge> linkages = RepastEssentials.GetOutEdges("CascadeContextMain/economicNetwork", this); //Ideally this must be avoided, changing the context name, will create a bug difficult to find
		Network economicNet = this.mainContext.getEconomicNetwork();
		Iterable<RepastEdge> iter = economicNet.getEdges();
		if(Consts.DEBUG) {
			System.out.println(This.class+" " +this.toString()+ " has "+ economicNet.size() + " links in economic network");
		}

		for (RepastEdge edge : iter) {
			Object linkSource = edge.getTarget();
			//System.out.println("RECO linkSource " + linkSource);
			if (linkSource instanceof ProsumerAgent){
				customers.add((ProsumerAgent) linkSource);    		
			}
			else
			{
				throw (new WrongCustomerTypeException(linkSource));
			}
		}
		return customers;
	}

	/**
	 * This method is used to check whether the 'profile building' period has completed.
	 * the 'profile building' period (the initial part of the training period, usually 4-7 days) is 
	 * currently determined by a rarely changed variable in the Consts class
	 * @see Consts#AGGREGATOR_PROFILE_BUILDING_PERIODE
	 * @return true if the profile building period is completed, false otherwise 
	 */
	private boolean isAggregateDemandProfileBuildingPeriodCompleted() {
		boolean isEndOfProfilBuilding = true;
		int daysSoFar = mainContext.getCountDay();

		if (daysSoFar < Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
			isEndOfProfilBuilding = false;
		return isEndOfProfilBuilding;	
	}

	/**
	 * This method is used to check whether the 'training' period has completed.
	 * the 'training' period (at least 48 days above 4-7 profile building days) is 
	 * currently determined by a rarely changed variable in the Consts class 
	 * which starts to be counted after the 'profile building' period has already completed.
	 * @see Consts#AGGREGATOR_TRAINING_PERIODE
	 * @see Consts#AGGREGATOR_PROFILE_BUILDING_PERIODE
	 * @see #isAggregateDemandProfileBuildingPeriodCompleted()
	 * @return true if the profile building period is completed, false otherwise 
	 */
	private boolean isTrainingPeriodCompleted() {
		boolean isEndOfTraining = true;
		int daysSoFar = mainContext.getCountDay();
		//System.out.println("days so far: "+daysSoFar);
		if (daysSoFar < (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE))
			isEndOfTraining = false;
		return isEndOfTraining;		
	}


	/**
	 * This method is used to update baseline aggregate demand (BAD or simply B) 
	 * history matrix by obtaining each customers/prosumers' demand and adding them up and putting it in the right array cell
	 * (i.e. right day [row], timeslot [column]) 
	 * @param customersList the List of cusstumersList (of <code>ProsumerAgent</code> type)
	 * @param timeOfDay the current timeslot (for a day divided to 48 timeslot, a value between 0 to 47)
	 * @param hist_arr_B a 2D array for keeping baseline aggregate demand values for each timeslot of the day(column) and for different days (row)  
	 */
	private void updateAggregateDemandHistoryArray(List<ProsumerAgent> customersList,int timeOfDay, float[][] hist_arr_B) {
		float sumDemand = 0;
		if(Consts.DEBUG)
		{
			System.out.println("Updating the aggregator demand history array at tick time " + RepastEssentials.GetTickCount() + " customersList size "+customersList.size());
		}

		for (ProsumerAgent a : customersList) {
			sumDemand = sumDemand + a.getNetDemand();
			if(Consts.DEBUG)
			{
				System.out.println("Got Net Demand for agent " + a.getAgentName() + " = "+a.getNetDemand());
			}
		}

		if (Consts.DEBUG)
		{
			System.out.println("Total demand at tick " + RepastEssentials.GetTickCount() + " = " + sumDemand);
		}

		int dayCount = mainContext.getCountDay();

		if (dayCount < hist_arr_B.length)
		{
			hist_arr_B[dayCount][timeOfDay]=sumDemand;
		}
		else
		{
			System.err.println("Trying to add demand for day " + dayCount + " but training array is only " + hist_arr_B.length + " days long.");
		}
	}


	/**
	 * This method calculates the average/ Baseline Aggregate Demands (BAD)
	 * for each timeslot of the day during different days from 2D history array where 
	 * each row represent a (different) day and each columns represent a timeslot of a day (usually divided to half-hour slots)
	 * It is basically a function to provide better readability to the program, as it simply calls 
	 * ArrayUtils average calculating function for 2D arrays, which could be also called direclty
	 * @param hist_arr_2D a 2D array containing historical baseline aggregate demand values for each timeslot of the day(column) and for different days (row)
	 * @return float array of average baseline aggregate demands 
	 */
	private float[] calculateBADfromHistoryArray(float[][] hist_arr_2D) {	
		return ArrayUtils.avgCols2DFloatArray(hist_arr_2D);	
	}


	/**
	 * This method builds a predefine signal based on the passed signal type and a timeslot. 
	 * @param signalType the type of the signal
	 * @param timeslot the time of day (usually a day is divided to 48 slots)  
	 * @return built signal as array of real numbers (float) 

	 */
	private float[] buildSignal(Consts.SIGNAL_TYPE signalType, int timeslot) {

		float[] sArr = new float[this.ticksPerDay];

		switch (signalType) { 
		case S: 
			break;

		case S_TRAINING: 
			/** S_TRAINING signal is an array equal to the size of timeslots per day (usually 48 per day).
			 * Att each training day (at least 48 days) a signal consists of s=1 for a specific timeslot of the day
			 * while the rest of timeslots will be s= -1/47
			 */
			int daysSoFar = mainContext.getCountDay();
			int indexFor1 = (daysSoFar - Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)%this.ticksPerDay; 
			sArr[indexFor1] = 1f;
			if (indexFor1 > 0) {
				for (int i = 0; i < indexFor1; i++) {
					sArr[i] = (-1f/(this.ticksPerDay-1));
				}
			}
			if (indexFor1 < this.ticksPerDay) {
				for (int i = indexFor1+1; i < sArr.length; i++) {
					sArr[i] = (-1f/(this.ticksPerDay-1));
				}
			}
			break;

		default:  //
			break;
		}

		return sArr;
	}


	/**
	 * This method builds a predefined signal based on the passed signal type.
	 * No timeslot is not necessary to be passed. 
	 * It bascially calls buildSignal(SignalType, timeslot) by passing -1 as timeslot
	 * for those signals where the construction does not require a time value. 
	 * @param signalType the type of the signal
	 * @return built signal as array of real numbers (float) 
	 * @see #sendSignal(uk.ac.dmu.iesd.cascade.Consts.SIGNAL_TYPE, float[], List, int)
	 */
	private float[] buildSignal(Consts.SIGNAL_TYPE signalType) {
		return buildSignal(signalType,-1);
	}

	/**
	 * This method is used send signal to of type S_TRAINING to prosumers
	 * defined by Peter Boait 
	 * @param broadcasteesList the list of broadcastees
	 * @param timeslot the time of day (usually a day is divided to 48 slots)  
	 * @return true if signal has been sent and received successfully by receiver, false otherwise 
	 */
	private boolean sendSignal(Consts.SIGNAL_TYPE signalType, float[] signalArr, List broadcasteesList, int timeOfDay) {

		boolean isSignalSentSuccessfully = false;

		switch (signalType) { 
		case S: 
			break;
		case S_TRAINING: 
			List <ProsumerAgent> paList = broadcasteesList;
			for (ProsumerAgent agent : paList){
				//System.out.println("sendSignalType_S, send signal to " + agent.toString());
				//isSignalSentSuccessfully = agent.receiveSignal(arr_i_S, arr_i_S.length, Consts.SIGNAL_TYPE.S);
				isSignalSentSuccessfully = agent.receiveValueSignal(arr_i_S, arr_i_S.length);
			}
			break;
		default:  //
			break;
		}
		return isSignalSentSuccessfully;
	}

	/**
	 * This method broadcasts a signal (of different type) to a list of provided broadcastees (e.g. Prosumers)
	 * at a given timeslot 
	 * @param signalType the type of the signal
	 * @param broadcasteesList the list of broadcastees
	 * @param timeslot the time of day (usually a day is divided to 48 slots)  
	 * @return true if signal has been sent and received successfully by receiver, false otherwise 
	 */
	private boolean broadcastSignal(Consts.SIGNAL_TYPE signalType, List<?> broadcasteesList, int timeslot) {

		boolean isSignalSentSuccessfully = false;

		switch (signalType) { 
		case S: 
			//isSignalSentSuccessfully = sendSignalType_S(broadcasteesList);
			break;
		case S_TRAINING: 
			/**This section is designed to send signal of s[timeslot]=1 and s[othertimes] = -1/47 (at each timeslot)*/
			this.arr_i_S = buildSignal(signalType,timeslot);
			//System.out.println(timeslot+Arrays.toString(arr_i_S));
			//System.out.println(ArrayUtils.isSumEqualZero(arr_i_S));
			this.priceSignal = arr_i_S;
			isSignalSentSuccessfully = sendSignal(signalType, arr_i_S, broadcasteesList, timeslot);
			break;

		default:  //
			break;
		}

		return isSignalSentSuccessfully;
	}


	/**
	 * This method broadcasts a passed signal array (of float values) to a list of passed customers (e.g. Prosumers)
	 * @param signalArr signal (array of real/float numbers) to be broadcasted
	 * @param customerList the list of customers (of ProsumerAgent type)
	 * @return true if signal has been sent and received successfully by the receiver, false otherwise 
	 */
	private boolean broadcastSignalToCustomers(float[] signalArr, List<ProsumerAgent> customerList) {

		boolean isSignalSentSuccessfully = false;
		//Next line only needed for GUI output at this stage
		this.priceSignal = new float[signalArr.length];
		System.arraycopy(signalArr, 0, this.priceSignal, 0, signalArr.length);
		//List  aList = broadcasteesList;
		//List <ProsumerAgent> paList = aList;	

		for (ProsumerAgent agent : customerList){			
			isSignalSentSuccessfully = agent.receiveValueSignal(signalArr, signalArr.length);
		}
		return isSignalSentSuccessfully;
	}

	/**
	 * This method calculates displacment factors (k) at the end of the day after S signal has been sent 
	 * by accepting an array of D values (at the end of the day),
	 * an average baseline aggregate demand (B) array built during profile building period, 
	 * the (last) array containing signal (S) sent during training period,
	 * the elasticity factors (e) array  and the reference to the array of k (deplacement factors)
	 * where the calculated values for kii and kji will be placed into it.   
	 * @param arr_D array containing aggregate demand (D) values at the end of the day  (usually 48 days)
	 * @param arr_B array containing average baseline aggregate demand values calcualted after profile building period (usually 7 days)
	 * @param arr_S array containing signal values sent to customers at the begining of the day
	 * @param arr_e array containing elasticity factors (e) calcuated so far (at the end of each day) 
	 * @param arr_k the reference to the displacement factor (k) 2D-array where the calculated kii and kji values will be placed into it. 
	 */
	private void calculateDisplacementFactors_k(float[] arr_D, float[] arr_B, float[] arr_S, float[] arr_e, float[][] arr_k) {	

		float e=0;
		float b =1;
		float s=1;
		float deltaB_i=0;

		int i =  ArrayUtils.indexOf(arr_S, 1f);
		if (i != -1 )	 {	
			b = arr_B[i];
			deltaB_i = arr_D[i] - b;
			e = arr_e[i];
		}

		arr_k[i][i] = (deltaB_i - (s*e*b)) / (s*b);//0;  <- what does k[i][i] mean? Should it be zero?

		for (int j = i+1; j < this.ticksPerDay; j++) {

			float b_j = arr_B[j];
			float deltaB_j = arr_D[j] - b_j;
			arr_k[j][i] =  deltaB_j  / (s*b);
		}

		for(int j = i-1; j >= 0; --j) {
			float b_j = arr_B[j];
			float deltaB_j = arr_D[j] - b_j;
			arr_k[j][i] =  deltaB_j  / (s*b);
		}
	}

	/**
	 * This method calculates the elasticity factor (e) for the timeslot of the day during which
	 * S was equal to 1 (S=1), by accepting an arry of D values (at the end of the day after sending S signal),
	 * an average baseline aggregate demand array built during profile building period, 
	 * the (last) array signal sent during training period and the reference to the array of e (elasticity factors)
	 * where the calculated value for the timeslot when S=1 will be placed into it.   
	 * @param arr_D array containing aggregate demand (D) values at the end of the day  (usually 48 days)
	 * @param arr_B array containing average baseline aggregate demand values calcualted after profile building period (usually 7 days)
	 * @param arr_S array containing signal values sent to customers at the begining of the day
	 * @param arr_e the reference to the elasticity factor (e) array where the calcualted e value for the specific timeslot of the day when S=1 will be placed into it. 
	 * @return float the elasticity factors (e) value for timeslot of the day when S=1 (also put into the arr_e passed passed as reference) 
	 */
	private float calculateElasticityFactors_e(float[] arr_D, float[] arr_B, float[] arr_S, float[] arr_e) {	

		float e=0;
		float b =1;
		float s=1;

		float sum_D = ArrayUtils.sum(arr_D);
		float sum_B = ArrayUtils.sum(arr_B);

		int timeslotWhenSwas1 =  ArrayUtils.indexOf(arr_S, 1f);
		if (timeslotWhenSwas1 != -1 )	 {	
			b = arr_B[timeslotWhenSwas1];
		}

		e = (sum_D - sum_B) / (s*b);

		arr_e[timeslotWhenSwas1] = e;

		return e;	
	}

	
	private float[] minimise_CD_ApacheSimplex(float[] arr_C, float[] arr_B, float[] arr_e, float[][] arr_ij_k, float[] arr_S ) {
		//private float[] minimise_CD_Apache(float[] arr_C, float[] arr_B, float[] arr_e, float[][] arr_ij_k, float[] arr_S ) throws OptimizationException, FunctionEvaluationException, IllegalArgumentException {
		System.out.println("---------------RECO: Apache Simplex minimisation (Babak implementation) ---------");
		
		ArrayRealVector coefficientsArrRealVect = new ArrayRealVector();
		double constantTerm =0d;
		
		DoubleArrayList coefficentOf_SjKijBi_ArrList = new DoubleArrayList();
		
		for (int i=0; i<arr_S.length; i++){
			constantTerm = constantTerm +(arr_C[i]*arr_B[i]);
			for (int j=0; j<arr_S.length; j++){
				if (i != j) {
					coefficentOf_SjKijBi_ArrList.add(arr_ij_k[i][j] * arr_B[i]);
					//System.out.println("RECO: coeff SjKijBj: "+constantTerm);
				}
			}
		}
		
		
		
		for (int i=0; i<arr_S.length; i++){
			//double s_coeff = (arr_e[i]*arr_B[i])+(arr_ij_k[i][i]*arr_B[i]+ coefficentOf_SjKijBi_ArrList.get(i)) ;
			double s_coeff_part1 = (arr_e[i]*arr_B[i])+(arr_ij_k[i][i]);
			double s_coeff_part2 =0;
			for (int j=0; j<arr_S.length; j++){
				int offset=i;
				s_coeff_part2 = s_coeff_part2+ coefficentOf_SjKijBi_ArrList.get(i+47);
				offset += 47;
			}

			coefficientsArrRealVect.append(s_coeff_part1+s_coeff_part2);
		}
		
		//System.out.println("RECO: coeff SjKijBi size: "+coefficentOf_SjKijBi_ArrList.size());
		
		//System.out.println("RECO: coeff all dimension: "+coefficientsArrRealVect.getDimension());
		
		//System.out.println("RECO: constant term: "+constantTerm);
		
		LinearObjectiveFunction minFunct = new LinearObjectiveFunction(coefficientsArrRealVect, constantTerm);
		
		double[] constraintCoeff = new double[this.ticksPerDay];
		Arrays.fill(constraintCoeff, 1);
		Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		constraints.add(new LinearConstraint(constraintCoeff, Relationship.EQ, 0));
		RealPointValuePair solution =null;
		try {
			solution = new SimplexSolver().optimize(minFunct, constraints, GoalType.MINIMIZE, false);
		} catch (OptimizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//double x = solution.getPoint()[0];
		//double y = solution.getPoint()[1];
		//double min = solution.getValue();

		System.out.println("RECO: Apache Simplex Solver:: Min value obtained " + solution.getValue());
      //if (solution != null)
		float[] newOpt_S= ArrayUtils.convertDoubleArrayToFloatArray(solution.getPoint());

		return newOpt_S;
	}
 

	/**
	 * Minimises the Cost times demand function with respect to the signal S sent to 
	 * this aggregators' prosumers.  Uses the Apache Commons SimplexSolver to do so
	 * 
	 * @param arr_C
	 * @param arr_B
	 * @param arr_e
	 * @param arr_ij_k
	 * @param arr_S
	 * @return
	 */
	private float[] minimise_CD_Apache(float[] arr_C, float[] arr_B, float[] arr_e, float[][] arr_ij_k, float[] arr_S ) {
		//private float[] minimise_CD_Apache(float[] arr_C, float[] arr_B, float[] arr_e, float[][] arr_ij_k, float[] arr_S ) throws OptimizationException, FunctionEvaluationException, IllegalArgumentException {
		//System.out.println("---------------RECO: Apache minimisation (SimplexSolver) ---------");

		float[] newOpt_S = Arrays.copyOf(arr_S, arr_S.length);

		SimplexSolver myOpt = new SimplexSolver(1e-3);
		myOpt.setMaxIterations(10000);
		double[] functionCoeffs = new double[ticksPerDay];
		double[] displacementConstraintCoeffs = new double[ticksPerDay];
		for (int kk = 0; kk < ticksPerDay; kk++)
		{
			
			for (int jj = 0; jj < ticksPerDay; jj++)
			{
				functionCoeffs[kk] += arr_ij_k[jj][kk] * arr_B[jj];
			}
			
			displacementConstraintCoeffs = Arrays.copyOf(functionCoeffs, functionCoeffs.length);
			
			functionCoeffs[kk] += arr_e[kk] * arr_B[kk];
			functionCoeffs[kk] *= arr_C[kk];
		}
		double constantOffset = ArrayUtils.sum(ArrayUtils.mtimes(arr_C,arr_B));
		LinearObjectiveFunction costFunc = new LinearObjectiveFunction(functionCoeffs,constantOffset);
		double[] constraintCoeffs = new double[ticksPerDay];
		Arrays.fill(constraintCoeffs, 1);
		ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		constraints.add(new LinearConstraint(constraintCoeffs, Relationship.EQ, 0));
		
		/*
		 * Bound the values of the signal to +/- 1 (or another maximal value stored in Consts)
		 * - needed to stop anything shooting off to infinity
		 */
		for (int ll = 0; ll < ticksPerDay; ll++)
		{
			double[] coeffs = new double[ticksPerDay];
			Arrays.fill(coeffs, 0);
			coeffs[ll] = 1;
			constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, -Consts.NORMALIZING_MAX_COST));
			constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, Consts.NORMALIZING_MAX_COST));
		}
		
		//An idea - ensure that all delta B due to displacement sums to zero
		//constraints.add(new LinearConstraint(displacementConstraintCoeffs, Relationship.EQ, 0));
		
		/*
		 * Enforce that Di is greater than zero for all i
		 * Note - this only works with no generation - would need to be be 
		 * -(MAX_GENERATION) if our aggregator had generators)
		 */
		for (int i = 0; i < ticksPerDay; i++)
		{
			double[] coeffs = new double[ticksPerDay];
			for (int j = 0; j < ticksPerDay; j++)
			{
				coeffs[j] = arr_ij_k[i][j] * arr_B[i];
			}
			coeffs[i] += arr_e[i] * arr_B[i];
			constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, 0 - arr_B[i]));
		}
	
		
		try {
			RealPointValuePair optimum = myOpt.optimize(costFunc,constraints,GoalType.MINIMIZE,false);
			newOpt_S = ArrayUtils.convertDoubleArrayToFloatArray(optimum.getPoint());
			System.out.println("Used apache commons Simplex to find optimium " + Arrays.toString(newOpt_S));
			System.out.println("In " + myOpt.getIterations() + " iterations ");
			System.out.println("Value " + optimum.getValue());
		} catch (OptimizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return newOpt_S;
	}

	private float[] minimise_CD_Apache_Nelder_Mead(float[] arr_C, float[] arr_B, float[] arr_e, float[][] arr_ij_k, float[] arr_S ) {
		//private float[] minimise_CD_Apache(float[] arr_C, float[] arr_B, float[] arr_e, float[][] arr_ij_k, float[] arr_S ) throws OptimizationException, FunctionEvaluationException, IllegalArgumentException {
		//System.out.println("---------------RECO: Apache minimisation (Nelder Mead) ---------");


		NelderMead apacheNelderMead = new NelderMead();
		//or:
		//NelderMead(double rho, double khi, double gamma, double sigma);
		//rho - reflection coefficient, khi - expansion coefficient
		//gamma - contraction coefficient,  sigma - shrinkage coefficient

		RecoMinimisationFunction minFunct = new RecoMinimisationFunction();

		minFunct.set_C(arr_C);
		minFunct.set_B(arr_B);
		minFunct.set_e(arr_e);
		minFunct.set_k(arr_ij_k);

		minFunct.addSimpleSumEqualsConstraintForApache(0, 0.01);

		// initial estimates
		//double[] start =  ArrayUtils.convertFloatArrayToDoubleArray(arr_i_S);

		//If it's the first time through the optimisation - start from an arbitrary
		//point in search space.  Otherwise, start from the last price signal
		double[] start = new double[this.ticksPerDay];
		if(firstTimeMinimisation )
		{
			Arrays.fill(start, 0.1);
			firstTimeMinimisation = false;
		}
		else 
		{
			start =  ArrayUtils.convertFloatArrayToDoubleArray(arr_i_S);
		}

		//apacheNelderMead.setMaxIterations(10);
		//apacheNelderMead.setConvergenceChecker(new SimpleScalarValueChecker(1.0e-10, 1.0e-30));
		apacheNelderMead.setConvergenceChecker(new SimpleScalarValueChecker(1.0e-2, -1.0));
		//apacheNelderMead.setMaxEvaluations(10);  //how many time function is evaluated

		RealPointValuePair minValue=null;

		try {

			minValue = apacheNelderMead.optimize(minFunct, GoalType.MINIMIZE,  start); 

		}
		catch (@SuppressWarnings("deprecation") OptimizationException e) {
			System.out.println( "RECO: Apache Optim Exc (Optim exc): "+e.getCause() );
		} 
		catch ( FunctionEvaluationException e) {
			System.out.println( "RECO: Apache Optim Exc (Funct eval exc): "+e.getCause() );
		}

		catch (IllegalArgumentException e) {
			System.out.println( "RECO: Apache Optim Exc (Illegal arg exc): "+e.getCause() );
		}
		/*

		minValue.getValue();
		minValue.getPoint();
		minValue.getPointRef(); */

		//float[] newOpt_S= ArrayUtils.convertDoubleArrayToFloatArray(param);
		float[] newOpt_S= ArrayUtils.convertDoubleArrayToFloatArray(minValue.getPoint());

		if (Consts.DEBUG)
		{
			// Print results to a text file
			//min.print("MinimCD_output.txt");

			// Output the results to screen
			System.out.println("RECO: Apache NelderMead:: Minimum = " + minValue.getValue());
			System.out.println("RECO: Apache NelderMead:: Min (S) sum = " + ArrayUtils.sum(newOpt_S));

			for (int i=0; i< newOpt_S.length; i++) {
				System.out.println("Apache value of s at the minimum for "+i +" ticktime is: " + newOpt_S[i]);
			}
			System.out.println("Apache optimisation evaluated function " + minFunct.getNumEvals() + " times");
		}

		return newOpt_S;
	}


	private float[] minimise_CD(float[] arr_C, float[] arr_B, float[] arr_e, float[][] arr_ij_k, float[] arr_S ) {

		Minimisation min = new Minimisation();
		RecoMinimisationFunction minFunct = new RecoMinimisationFunction();

		minFunct.set_C(arr_C);
		minFunct.set_B(arr_B);
		minFunct.set_e(arr_e);
		minFunct.set_k(arr_ij_k);

		// initial estimates
		//double[] start =  ArrayUtils.convertFloatArrayToDoubleArray(arr_i_S);

		//If it's the first time through the optimisation - start from an arbitrary
		//point in search space.  Otherwise, start from the last price signal
		double[] start = new double[this.ticksPerDay];
		//if(firstTimeMinimisation )
		{
			Arrays.fill(start, 0.1);
			firstTimeMinimisation = false;
		}
		//else
		//{
		//	start =  ArrayUtils.convertFloatArrayToDoubleArray(arr_i_S);
		//}

		// initial step sizes
		//double[] step = new double[arr_S.length];


		double ftol = 1e-1; //1e-15;   // convergence tolerance

		int[] pIndices = new int[this.ticksPerDay];
		int[] plusOrMinus = new int[this.ticksPerDay];
		for (int i=0; i<this.ticksPerDay; i++)
		{
			pIndices[i] = i;
			plusOrMinus[i] = 1;
			//step[i] = 0.05;
		}

		int direction =0;
		double boundary =0d;

		min.setNmax(100000);
		min.addConstraint(pIndices, plusOrMinus, direction, boundary);
		min.setConstraintTolerance(1e-3);

		min.nelderMead(minFunct, start, ftol);

		// get the minimum value
		double minimum = min.getMinimum();
		System.out.println("RECO: Minimum = " + minimum);

		// get values of y and z at minimum
		double[] param = min.getParamValues();

		//min.print("MinimCD_output.txt");

		if (Consts.DEBUG)
		{
			// Print results to a text file
			//min.print("MinimCD_output.txt");

			// Output the results to screen
			System.out.println("RECO: Minimum = " + min.getMinimum());
			System.out.println("RECO: Min (S) sum = " + ArrayUtils.sum(param));

			for (int i=0; i< param.length; i++) {
				System.out.println("RECO: Flanagan Value of s at the minimum for "+i +" ticktime is: " + param[i]);
			}
			System.out.println("Flanagan optimisation evaluated function " + minFunct.getNumEvals() + " times");
		}


		float[] newOpt_S= ArrayUtils.convertDoubleArrayToFloatArray(param);
		//System.out.println("Minimum achieved is " + min.getMinimum());
		return newOpt_S;
	}

	private float[] minimise_CD_Genetic_Algorithm(float[] arr_C, float[] arr_B, float[] arr_e, float[][] arr_ij_k, float[] arr_S)
	{
		float[] returnArray = new float[ticksPerDay];

		/*** Richard's Genetic algorithm optimisation ***/
		Configuration conf = new DefaultConfiguration();

		// Set the fitness function we want to use, which is our
		// MinimizingMakeChangeFitnessFunction that we created earlier.
		// We construct it with the target amount of change provided
		// by the user.
		// ------------------------------------------------------------

		RecoMinimisationFunction geneticMinFunc =  new RecoMinimisationFunction();
		geneticMinFunc.set_B(arr_B);
		geneticMinFunc.set_C(arr_C);
		geneticMinFunc.set_e(arr_e);
		geneticMinFunc.set_k(arr_ij_k);
		geneticMinFunc.addSimpleSumEqualsConstraintForApache(0, 0.01);

		try 
		{
			conf.setFitnessFunction( geneticMinFunc );


			// Now we need to tell the Configuration object how we want our
			// Chromosomes to be setup. We do that by actually creating a
			// sample Chromosome and then setting it on the Configuration
			// object. As mentioned earlier, we want our Chromosomes to
			// each have four genes, one for each of the coin types. We
			// want the values of those genes to be integers, which represent
			// how many coins of that type we have. We therefore use the
			// IntegerGene class to represent each of the genes. That class
			// also lets us specify a lower and upper bound, which we set
			// to sensible values for each coin type.
			// --------------------------------------------------------------
			DoubleGene[] sampleGenes = new DoubleGene[ 48 ];

			for (int ii = 0; ii < sampleGenes.length; ii++)
			{
				sampleGenes[ii] = new DoubleGene(-1,1);
			}

			Chromosome sampleChromosome = new Chromosome(sampleGenes );

			conf.setSampleChromosome( sampleChromosome );

			conf.setPopulationSize(1500);

			Genotype population = Genotype.randomInitialGenotype( conf );
			for (int evolutions = 0; evolutions < 100; evolutions++)
			{
				population.evolve();
			}
			Chromosome bestSolutionSoFar = population.getFittestChromosome();

			for (int jj = 0; jj < returnArray.length; jj++)
			{
				returnArray[jj] = 2 * ((float) ((DoubleGene) bestSolutionSoFar.getGene(jj)).doubleValue() - 0.5f);
			}
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			System.err.println("Invalid configuration for genetic algorithm");
			e.printStackTrace();
		}
		return returnArray;
	}


	/**
	 * This method defines how this object behaves (what it does)
	 * at at a given scheduled time throughout the simulation. 
	 */
	//@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void step() {

		// Note the simulation time if needed.
		double time = RepastEssentials.GetTickCount();
		int timeOfDay = (int) (time % ticksPerDay);
		int dayOfWeek = ((CascadeContext) ContextUtils.getContext(this)).simulationCalendar.getTime().getDay();

		List<ProsumerAgent> customers = getCustomersList();

		float sumDemand = 0;
		//float sum_e =0;
		for (ProsumerAgent a : customers)
		{
			sumDemand = sumDemand + a.getNetDemand();
			//sum_e = sum_e+a.getElasticityFactor();

		}

		setNetDemand(sumDemand);


		if (!isAggregateDemandProfileBuildingPeriodCompleted()) 
		{ 
			updateAggregateDemandHistoryArray(customers, timeOfDay, hist_arr_ij_D); 
		}
		else 
		{ 
			//End of history profile building period 

			//Set the Baseline demand on the first time through after building period
			if (mainContext.getCountDay() == Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
			{
				arr_i_B = calculateBADfromHistoryArray(ArrayUtils.subArrayCopy(hist_arr_ij_D,0,Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE));
			}

			if (Consts.DEBUG)
			{
				System.out.println("Baseline demand set to " + Arrays.toString(arr_i_B));
			}

			if (!isTrainingPeriodCompleted()) 
			{  //training period 
				//signals should be send S=1 for 48 days
				//System.out.println("bc signal at time: "+RepastEssentials.GetTickCount());
				if (mainContext.isBeginningOfDay(timeOfDay)) 
				{

					//System.out.print("day: "+mainContext.getCountDay()+" timeOfDay: "+timeOfDay);
					//System.out.println("  timetick: "+mainContext.getCurrentTimeslotForDay());
					arr_i_S = buildSignal(Consts.SIGNAL_TYPE.S_TRAINING);

					//System.out.println(Arrays.toString(arr_i_S));
					broadcastSignalToCustomers(arr_i_S, customers);
				}

				//broadcastSignal(Consts.SIGNAL_TYPE.S_TRAINING, customers, timeOfDay);
				updateAggregateDemandHistoryArray(customers, timeOfDay, hist_arr_ij_D);

				if (mainContext.isEndOfDay(timeOfDay)) 
				{
					System.out.print("-----RECO: Training period--------------");
					System.out.print("End of day: "+mainContext.getCountDay()+" timeOfDay: "+timeOfDay);
					System.out.println(" timetick: "+mainContext.getCurrentTimeslotForDay());
					float [] last_arr_D = ArrayUtils.rowCopy(hist_arr_ij_D, mainContext.getCountDay());
					float e = calculateElasticityFactors_e(last_arr_D,arr_i_B,arr_i_S, arr_i_e);
					calculateDisplacementFactors_k(last_arr_D, arr_i_B, arr_i_S, arr_i_e, arr_ij_k);

					//System.out.println("e: "+e);
					//System.out.println("B: "+ Arrays.toString(arr_i_B));
					//System.out.println("D: "+ Arrays.toString(last_arr_D));
					//System.out.println("S: "+ Arrays.toString(arr_i_S));
					//System.out.println("e: "+Arrays.toString(arr_i_e));
					//System.out.println("k: ");
					//System.out.println(ArrayUtils.toString(arr_ij_k));	

					if (mainContext.getCountDay() == 54) {

						int [] ts_arr = new int[ticksPerDay];

						for (int i=0; i<ts_arr.length; i++){
							ts_arr[i] = i;	
						}

						CSVWriter res = new CSVWriter("Res_EndOfDay54_EndOfTrainingDay48_rs1.csv", true);
						//CSVWriter res = new CSVWriter("Res_EndOfDay18_EndOfTrainingDay11_rs1.csv", true);

						res.appendText("Timeslots:");
						res.appendRow(ts_arr);
						res.appendText("B:");
						res.appendRow(arr_i_B);
						res.appendText("D (for end of day "+mainContext.getCountDay()+"): ");
						res.appendRow(last_arr_D);
						res.appendText("S (for end of day "+mainContext.getCountDay()+"): ");
						res.appendRow(arr_i_S);
						res.appendText("e (for end of day "+mainContext.getCountDay()+"): ");
						res.appendRow(arr_i_e);
						res.appendText("k (for end of day "+mainContext.getCountDay()+"): ");
						res.appendCols(arr_ij_k);
						res.close(); 

					} 



					/*	if (mainContext.isBeginningOfDay(timeOfDay) && mainContext.isDayChangedSince(Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)) {

		    		float [] last_arr_D = ArrayUtils.rowCopy(hist_arr_ij_D, mainContext.getCountDay()-1);

		    		float e = calculateElasticityFactors_e(last_arr_D,arr_i_B,arr_i_S, arr_i_e);
		    	//	arr_ij_k = this.calculateDisplacementFactors_k(last_arr_D, arr_i_B, arr_i_S, arr_i_e);

		    	} */


				}
			}
			else 
			{ 
				/* Both the baseline establishing period and the training period are complete */

				if(Consts.DEBUG)
				{
					System.out.println("---End of training reached ----");
					System.out.print("day: "+mainContext.getCountDay());
					System.out.println("  timetick: "+mainContext.getCurrentTimeslotForDay());
				}

				/* Real/Usual business here */  

				//Richard Test to stimulate prosumer behaviour
				if (mainContext.isBeginningOfDay(timeOfDay)) 
				{
					//Implement Peter B's very simplistic "Routine error estimation and adjustment" learning

					float[] actualShift = ArrayUtils.add(daysDemandHistory, ArrayUtils.negate(arr_i_B));
					Matrix k = new Matrix(arr_ij_k);
					double[][] b = new double[1][arr_i_B.length];
					b[0] = ArrayUtils.convertFloatArrayToDoubleArray(ArrayUtils.mtimes(arr_i_B,arr_i_S));
					Matrix Bm = new Matrix(b);
					Bm.transpose();			
					float[] predictedShift= ArrayUtils.add(ArrayUtils.mtimes(arr_i_S,arr_i_e, arr_i_B), ArrayUtils.convertDoubleArrayToFloatArray((Matrix.times(Bm, k).getRowCopy(0))));

					float[] errorVector = ArrayUtils.mtimes(actualShift, ArrayUtils.convertDoubleArrayToFloatArray(ArrayUtils.pow(predictedShift,-1)));

					if(Consts.DEBUG)
					{
						System.out.println("error vector is " + Arrays.toString(errorVector));
					}

					float[] multiplier = ArrayUtils.offset(ArrayUtils.multiply(errorVector, alpha), (1 - alpha));

					if(Consts.DEBUG)
					{
						System.out.println("e before " + Arrays.toString(arr_i_e));
					}
					arr_i_e = ArrayUtils.mtimes(arr_i_e, multiplier);
					if(Consts.DEBUG)
					{
						System.out.println("e after " + Arrays.toString(arr_i_e));
					}
					/*System.out.println("Rows " + arr_ij_k.length + " Columns " + arr_ij_k[1].length);
					Matrix k = new Matrix(arr_ij_k);
					System.out.println("After Matrix init, k columns " + k.getNumberOfColumns() + " k rows " + k.getNumberOfRows());
					double[][] multiplicationVector = new double[1][multiplier.length];
					multiplicationVector[0] = ArrayUtils.convertFloatArrayToDoubleArray(multiplier);
					Matrix multVector = new Matrix(multiplicationVector);
					Matrix new_k = Matrix.times(k, Matrix.transpose(multVector));
					arr_ij_k = ArrayUtils.convertDoubleArrayToFloatArray(new_k.getArrayCopy());*/

					for (int i = 0; i < arr_ij_k.length; i++)
					{
						arr_ij_k[i] = ArrayUtils.mtimes(arr_ij_k[i], multiplier);
					}

					if (Consts.DEBUG) 
					{

						String fileName = new String("output for day "+(mainContext.getCountDay()-1)+".csv");

						int [] ts_arr = new int[ticksPerDay];

						for (int i=0; i<ts_arr.length; i++){
							ts_arr[i] = i;	
						}

						CSVWriter res = new CSVWriter(fileName, false);
						//CSVWriter res = new CSVWriter("Res_EndOfDay18_EndOfTrainingDay11_rs1.csv", true);

						res.appendText("Timeslots:");
						res.appendRow(ts_arr);
						res.appendText("B:");
						res.appendRow(arr_i_B);
						res.appendText("D (for end of day "+(mainContext.getCountDay()-1)+"): ");
						res.appendRow(daysDemandHistory);
						res.appendText("S (for end of day "+(mainContext.getCountDay()-1)+"): ");
						res.appendRow(arr_i_S);
						res.appendText("e (for end of day "+(mainContext.getCountDay()-1)+"): ");
						res.appendRow(arr_i_e);
						res.appendText("k (for end of day "+(mainContext.getCountDay()-1)+"): ");
						res.appendCols(arr_ij_k);
						res.close(); 

					} 

					//Replace the historical demand for the day of the week before this
					//with the demand of yesterday
					// TODO: Could be more sophisticated and have a rolling or weighted average
					this.weekDemandHistory[dayOfWeek] = daysDemandHistory;

					//arr_i_S = ArrayUtils.normalizeValues(minimise_CD(arr_i_C, arr_i_B, arr_i_e, arr_ij_k, arr_i_S));
					//System.out.println(Arrays.toString(Arrays.copyOfRange(arr_i_C, (int) time % arr_i_C.length, ((int)time % arr_i_C.length) + ticksPerDay)));

					/*** One possible way to introduce feedback and allow the signal to change. ***/
					/*** Richard's test version below - with normalisation etc.***/
					//arr_i_C = ArrayUtils.normalizeValues(ArrayUtils.pow2(this.predictedCustomerDemand));

					float[] normalizedCosts = ArrayUtils.normalizeValues((Arrays.copyOfRange(arr_i_C, (int) time % arr_i_C.length, ((int)time % arr_i_C.length) + ticksPerDay)));
					//float[] normalizedCosts = (Arrays.copyOfRange(arr_i_C, (int) time % arr_i_C.length, ((int)time % arr_i_C.length) + ticksPerDay));
					//System.out.println(Arrays.toString(normalizedCosts));
					//System.out.println(Arrays.toString(arr_i_S));
					
					//arr_i_S = minimise_CD_ApacheSimplex(arr_i_C, arr_i_B, arr_i_e, arr_ij_k, arr_i_S);
					//System.out.println("RECO S by Babak's implementation" + Arrays.toString(arr_i_S));

					//arr_i_S = minimise_CD(normalizedCosts, arr_i_B, arr_i_e, arr_ij_k, arr_i_S);
					//System.out.println("Flanagan : " + Arrays.toString(arr_i_S));
					//System.out.println("Apache : " + Arrays.toString(minimise_CD_Apache_Nelder_Mead(normalizedCosts, arr_i_B, arr_i_e, arr_ij_k, arr_i_S)));

					//arr_i_S = minimise_CD_Genetic_Algorithm(normalizedCosts, arr_i_B, arr_i_e, arr_ij_k, arr_i_S);
					//System.out.println("Genetic : " + Arrays.toString(arr_i_S));

					arr_i_S = minimise_CD_Apache(normalizedCosts, arr_i_B, arr_i_e, arr_ij_k, arr_i_S);



					//Test with flat price
					//this.setPriceSignalFlatRate(1f);
					// Test with Econ 7
					//setPriceSignalEconomySeven(15, 7);
					//arr_i_S = Arrays.copyOf(priceSignal, arr_i_S.length);
					//System.out.println(Arrays.toString(arr_i_S));
					//arr_i_S = ArrayUtils.normalizeValues(arr_i_S);
					//System.out.println(Arrays.toString(arr_i_S));

					broadcastSignalToCustomers(arr_i_S, customers);

				}

			}

		}

		//Things to do right at the end of the step

		daysDemandHistory[timeOfDay] = sumDemand;
		//Set the predicted demand for next day to the sum of the demand at this time on the following day last week today.

		if(weekDemandHistory[(dayOfWeek + 1) % 7] != null)
		{
			predictedCustomerDemand[timeOfDay] = weekDemandHistory[(dayOfWeek + 1) % 7][timeOfDay];
		}
		else
		{
			predictedCustomerDemand[timeOfDay] = sumDemand;
		}

		if(Consts.DEBUG)
		{
			System.out.println("predictTimeslotDemand("+(timeOfDay+1)+ "): "+ calcualte_PredictedDemand_D(timeOfDay));

		}

		if (mainContext.isEndOfDay(timeOfDay)) 
		{
			float pred_cost = 0;
			float[] costs = Arrays.copyOfRange(arr_i_C, ((int) time) % arr_i_C.length, ((int) time) % arr_i_C.length + ticksPerDay);
			for (int i = 0; i < ticksPerDay; i++)
			{
				pred_cost += costs[i] * calcualte_PredictedDemand_D(i);
			}
			this.dailyPredictedCost.add(pred_cost);
			this.dailyActualCost.add(ArrayUtils.sum(ArrayUtils.mtimes(daysDemandHistory, costs)));
		}

		if(Consts.DEBUG)
		{
			for (int j = 0; j<this.dailyActualCost.size(); j++)
			{
				System.out.println("For day " + j + " predicted cost was " + this.dailyPredictedCost.get(j)+ " actual cost was " + this.dailyPredictedCost.get(j));
			}
		}
	}

	/**
	 * Returns a string representing the state of this agent. This 
	 * method is intended to be used for debugging purposes, and the 
	 * content and format of the returned string should include the states (variables/parameters)
	 * which are important for debugging purpose.
	 * The returned string may be empty but may not be <code>null</code>.
	 * 
	 * @return  a string representation of this agent's state parameters
	 */
	protected String paramStringReport(){
		String str="";
		return str;

	}


	private void broadcastDemandSignal(List<ProsumerAgent> broadcastCusts, double time, int broadcastLength) {

		// To avoid computational load (and realistically model a reasonable broadcast strategy)
		// only prepare and transmit the price signal if it has changed.
		if(priceSignalChanged)
		{
			//populate the broadcast signal with the price signal starting from now and continuing for
			//broadcastLength samples - repeating copies of the price signal if necessary to pad the
			//broadcast signal out.
			float[] broadcastSignal= new float[broadcastLength];
			int numCopies = (int) Math.floor((broadcastLength - 1) / priceSignal.length);
			int startIndex = (int) time % priceSignal.length;

			System.arraycopy(priceSignal,startIndex,broadcastSignal,0,priceSignal.length - startIndex);
			for (int i = 1; i <= numCopies; i++)
			{
				int addIndex = (priceSignal.length - startIndex) * i;
				System.arraycopy(priceSignal, 0, broadcastSignal, addIndex, priceSignal.length);
			}

			if (broadcastLength > (((numCopies + 1) * priceSignal.length) - startIndex))
			{
				System.arraycopy(priceSignal, 0, broadcastSignal, ((numCopies + 1) * priceSignal.length) - startIndex, broadcastLength - (((numCopies + 1) * priceSignal.length) - startIndex));
			}

			for (ProsumerAgent a : broadcastCusts){
				// Broadcast signal to all customers - note we simply say that the signal is valid
				// from now currently, in future implementations we may want to be able to insert
				// signals valid at an offset from now.
				if (Consts.DEBUG)
				{
					//System.out.println("Broadcasting to " + a.sAgentID);
				}
				//System.out.println(" RECO is sending signal at ticktime "+ RepastEssentials.GetTickCount());

				a.receiveValueSignal(broadcastSignal, broadcastLength);
			}
		}

		priceSignalChanged = false;
	}

	/**
	 * Constructs a RECO agent with the context in which is created and its
	 * base demand.
	 * @param context the context in which this agent is situated
	 * @param baseDemand an array containing the base demand  
	 */
	public RECO(CascadeContext context, float[] baseDemand) {

		super(context);
		if (Consts.DEBUG)
		{
			System.out.println("RECO created ");
		}

		this.ticksPerDay = context.getTickPerDay();

		if (Consts.DEBUG)
		{
			System.out.println("RECO ticksPerDay "+ ticksPerDay);
		}

		if (baseDemand.length % ticksPerDay != 0)
		{
			System.err.print("RECO: Error/Warning message from "+this.toString()+": BaseDemand array imported to aggregator not a whole number of days");
			System.err.println(" RECO:  May cause unexpected behaviour - unless you intend to repeat the signal within a day");
		}
		this.priceSignal = new float [baseDemand.length];
		this.overallSystemDemand = new float [baseDemand.length];
		System.arraycopy(baseDemand, 0, this.overallSystemDemand, 0, overallSystemDemand.length);

		//Start initially with a flat price signal of 12.5p per kWh
		//Arrays.fill(priceSignal,125f);
		Arrays.fill(priceSignal,0f);


		// Very basic configuration of predicted customer demand as 
		// a Constant.  We could be more sophisticated than this or 
		// possibly this gives us an aspirational target...
		this.predictedCustomerDemand = new float[ticksPerDay];
		//Put in a constant predicted demand
		//Arrays.fill(this.predictedCustomerDemand, 5);
		//Or - put in a variable one
		for (int j = 0; j < ticksPerDay; j++)
		{
			this.predictedCustomerDemand[j] = baseDemand[j] / 7000;
		}

		daysDemandHistory = new float[ticksPerDay];
		weekDemandHistory = new float[7][ticksPerDay];
		this.dailyPredictedCost = new ArrayList<Float>();
		this.dailyActualCost = new ArrayList<Float>();

		///+++++++++++++++++++++++++++++++++++++++
		this.arr_i_B = new float [ticksPerDay];
		this.arr_i_e = new float [ticksPerDay];
		this.arr_i_S = new float [ticksPerDay];
		this.arr_i_C = new float [ticksPerDay];
		this.arr_ij_k = new float [ticksPerDay][ticksPerDay];
		this.hist_arr_ij_D = new float [Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE+Consts.AGGREGATOR_TRAINING_PERIODE][ticksPerDay];

		arr_i_C = ArrayUtils.normalizeValues(ArrayUtils.pow2(baseDemand),100);

		//this.arr_i_B = baseDemand; 

		//Set up basic learning factor
		this.alpha = 0.1f;

		//+++++++++++++++++++++++++++++++++++++++++++
	}


}
