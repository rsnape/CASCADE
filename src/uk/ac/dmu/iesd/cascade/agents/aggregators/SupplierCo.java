package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.commons.mathforsimplex.FunctionEvaluationException;
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
import org.apache.log4j.Level;
/*import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
*/
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
//import uk.ac.dmu.iesd.cascade.util.MinimisationFunctionNeuralNetReady;
import uk.ac.dmu.iesd.cascade.util.MinimisationFunctionObjectiveFlatDemand;
import uk.ac.dmu.iesd.cascade.util.WrongCustomerTypeException;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.TrainingSignalFactory;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.TrainingSignalFactory.TRAINING_S_SHAPE;
import cern.colt.list.DoubleArrayList;
import flanagan.math.Matrix;
import flanagan.math.Minimisation;

/**
 * A <em>SupplierCo</em> or a supplier retail company is a concrete object that
 * represents a commercial/business electricity/energy company involved in
 * retail trade with prosumer agents (<code>ProsumerAgent</code>), such as
 * household prosumers (<code>HouseholdProsumer</code>) In other words, a
 * <code>SupplierCo</code> provides electricity/energy to certain types of
 * prosumers (e.g. households) It will be also involved in electricity trade
 * market.
 * <p>
 * 
 * @author Babak Mahdavi
 * @version $Revision: 2.0 $ $Date: 2012/05/18 $
 * 
 *          Version history (for intermediate steps see Git repository history)
 * 
 *          1.0 - created the concrete class of AggregatorAgent kind after the
 *          latter was made abstract upon my suggestion 1.1 - Class name changed
 *          to SupplierCo (previously named RECO) (2012/05/18)
 * 
 */

public class SupplierCo extends BMPxTraderAggregator
{

	/**
	 * the aggregator agent's base name
	 **/
	protected static String agentBaseName = "SupplierCO";

	/**
	 * indicates whether the agent is in 'profile building' mode/period
	 **/
	protected boolean isProfileBuidling = true;

	/**
	 * indicates whether the agent is in training mode/ period
	 **/
	protected boolean isTraining = true;

	/**
	 * This field (S) is "signal" at timeslot i sent to customer's (prosumers)
	 * so that they response to it accordingly. For example, if S=0, the
	 * aggregator can assume that the prosumers respond with their default
	 * behavior, so there is no timeshifting or demand or elastic response to
	 * price and the result is a baseline demand aggregate B When Si is not
	 * zero, the aggregator can calculate the resultant aggregate deviation
	 * (delta_Bi)
	 **/
	double[] arr_i_S; // (S) signal at timeslot i

	double[] arr_i_C_all; // all C values (equal to the size of baseDemand,
							// usually 1 week, 336 entries)

	/**
	 * This 2D-array is used to keep the usual aggregate demand of all prosumers
	 * at each timeslot of the day during both profile building and training
	 * periods (usually about 7 + 48 days). In other words, the length of the
	 * columns of this 2D history array is equal to number of timeslot during a
	 * day and the length of its rows is equal to the number of days the profile
	 * building and training periods last.
	 * 
	 * TODO: if all the aggregator have this default behavior (e.g. building
	 * profile in the same way) this field may stay here otherwise, it will need
	 * to move to the appropriate implementor (e.g. RECO)
	 **/
	double[][] arr_hist_ij_D;

	private boolean firstTimeMinimisation = true;

	// private double[] arr_hist_day_D; //keep a day demand history @TODO: to be
	// removed

	/**
	 * A very simple learning factor implementation. There will be more
	 * sophisticated implementations in time
	 */
	private double alpha;

	List<ProsumerAgent> customers;
	int timeTick;
	int timeslotOfDay;
	int dayOfWeek;

	private TrainingSignalFactory trainingSigFactory;

	/**
	 * This method calculates and returns the price (Pi) per kWh at given
	 * time-slot. (It implements the formula proposed by P. Boait, Formula #2)
	 * Parameters a and b are "fixed pricing" parameters and set by the
	 * aggregator and known to the prosumer's smart meter and other devices so
	 * they react to Pi. The summation of all Si to zero simplifies their
	 * calculation! In practice, it would be helpful to use normalized values
	 * between 1 and -1 to make up S
	 * 
	 * @param timeslot
	 *            time slot of the day (often/usually 1 day = 48 timeslot)
	 * @return Price(Pi) per KWh at given timeslot (i)
	 */
	protected double calculate_Price_P(int timeslot)
	{
		double a = 2d; // the value of a must be set to a fixed price, e.g.
						// ~baseline price
		double b = 0.2d; // this value of b amplifies the S value signal, to
							// reduce or increase the price

		double Si = this.arr_i_S[timeslot];
		double Pi = a + (b * Si);

		return Pi;
	}

	/**
	 * This method calculates and returns the baseline aggregate deviation
	 * (DeltaBi) at given time-slot. It is invoked by aggregator when the Si is
	 * not zero and the aggregator wants to form a response. (It implements the
	 * formula proposed by P. Boait, Formula #3)
	 * 
	 * @param timeslot_i
	 *            time slot of the day (often/usually 1 day = 48 timeslot)
	 * @return Baseline aggregate deviation (DelatBi) at given timeslot (i)
	 */

	private double calculate_deltaB(int timeslot_i)
	{
		double sumOf_SjKijBi = 0d;
		for (int j = 0; j < this.ticksPerDay; j++)
		{
			if (j != timeslot_i)
			{ // i!=j
				sumOf_SjKijBi += this.arr_i_S[j] * this.arr_ij_k[timeslot_i][j] * this.arr_i_B[timeslot_i];
				// sumOf_SjKijBi +=
				// arr_i_S[j]*arr_ij_k[j][timeslot_i]*arr_i_B[timeslot_i];

			}
		}
		double leftSideEq = this.arr_i_S[timeslot_i] * this.arr_ij_k[timeslot_i][timeslot_i] * this.arr_i_B[timeslot_i];

		double deltaBi = leftSideEq + sumOf_SjKijBi;
		return deltaBi;
	}

	/**
	 * This method calculates and returns the demand predicted by the aggregator
	 * in each time-slot, taking account of both elastic and displacement
	 * changes. (It implements the formula proposed by P. Boait, Formula #4)
	 * When e=0 (i.e. all ei=0), then the sum of all Di = sum of all Bi
	 * 
	 * @param timeslot
	 *            time slot of the day (often/usually 1 day = 48 timeslot)
	 * @return Demand (Di) predicted by the aggregator at given timeslot (i)
	 */
	protected double calculate_PredictedDemand_D(int timeslot)
	{
		double Bi = this.arr_i_B[timeslot];
		double Si = this.arr_i_S[timeslot];
		double ei = this.arr_i_e[timeslot];

		double delta_Bi = this.calculate_deltaB(timeslot);

		double Di = Bi + (Si * ei * Bi) + delta_Bi;

		return Di;
	}

	/**
	 * This method calculates and returns "price elasticity factor" (e) at a
	 * given time-slot. (It implements the formula proposed by P. Boait, Formula
	 * #6)
	 * 
	 * @param arr_D
	 *            a double array containing aggregate demand (D) values for a
	 *            timeslot of a day (usually 48 timeslots)
	 * @param arr_B
	 *            a double array containing average baseline aggregate demand
	 *            (B) values for each timeslot of a day (usulaly 48 timeslots)
	 * @param s
	 *            signal value at timeslot i
	 * @param B
	 *            average baseline aggregate demand (B) value at timeslot i
	 * @return elasticity price factor (e) [at timeslot i]
	 */
	protected double calculate_e(double[] arr_D, double[] arr_B, double s, double B)
	{

		double e = 0;
		double sum_D = ArrayUtils.sum(arr_D);
		double sum_B = ArrayUtils.sum(arr_B);
		if ((s != 0) && (B != 0))
		{
			e = (sum_D - sum_B) / (s * B);
		}
		return e;
	}

	/**
	 * This method calculates and returns "displacement factor" (k) at given
	 * time-slots. (It implements the formula proposed by P. Boait, Formula #7
	 * and #8) By stepping Si=1 through all the timeslots over 48 days, the
	 * aggregator obtains complete set of estimates for e and k. By repeating
	 * this training (if necessary) more accurate estimates can be obtained.
	 * 
	 * @param timeslot
	 *            time slot of the day (often/usually 1 day = 48 timeslot)
	 * @return displacement factor (Kij) at given timeslots (i and j)
	 */
	protected double calculate_k(int t_i, int t_j)
	{

		double k_ij = 0;
		double divisor = 1;

		if (t_i == t_j)
		{ // calculate Kii
			double delta_Bi = this.calculate_deltaB(t_i);
			double divident = delta_Bi - (this.arr_i_S[t_i] * this.arr_i_e[t_i] * this.arr_i_B[t_i]);
			divisor = this.arr_i_S[t_i] * this.arr_i_B[t_i];
			k_ij = divident / divisor;
		}

		else
		{ // calculate Kij
			double delta_Bj = this.calculate_deltaB(t_j);
			divisor = this.arr_i_S[t_i] * this.arr_i_B[t_j];
			k_ij = delta_Bj / divisor;
		}

		return k_ij;
	}

	/**
	 * This method calculates the predict demand at each given time-slot.
	 * 
	 * @param t
	 *            timeslot of the day (often/usually 1 day = 48 timeslot)
	 * @return displacement factor (Kij) at given timeslots (i and j)
	 */
	protected void predictDemand(List<ProsumerAgent> customersList, int t)
	{

		double sumDemand = 0;
		for (ProsumerAgent a : customersList)
		{
			sumDemand = sumDemand + a.getNetDemand();
		}
		this.arr_i_B[t] = sumDemand;

	}

	private void setB_and_e(List<ProsumerAgent> customers, int time, boolean isTraining)
	{
		double sumDemand = 0;
		double sum_e = 0;
		for (ProsumerAgent agent : customers)
		{
			sumDemand = sumDemand + agent.getNetDemand();
			if (!isTraining)
			{
				sum_e = sum_e;// +agent.getElasticityFactor(); TODO: Check this
			}
		}

		this.arr_i_B[time] = sumDemand;

		if (!isTraining)
		{
			this.arr_i_e[time] = sum_e;
		}
	}

	/**
	 * This method returns the list of customers (prosusmers) in the economic
	 * network of this aggregator
	 * 
	 * @return List of customers of type <tt> ProsumerAgent</tt>
	 */
	private List<ProsumerAgent> getCustomersList()
	{
		List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
		// List<RepastEdge> linkages =
		// RepastEssentials.GetOutEdges("CascadeContextMain/economicNetwork",
		// this); //Ideally this must be avoided, changing the context name,
		// will create a bug difficult to find
		Network economicNet = this.mainContext.getEconomicNetwork();
		Iterable<RepastEdge> iter = economicNet.getEdges(this);
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(this.getAgentName() + " has " + economicNet.size() + " links in economic network");
		}

		for (RepastEdge edge : iter)
		{
			Object linkSource = edge.getTarget();
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("RECO linkSource " + linkSource);
			}
			if (linkSource instanceof ProsumerAgent)
			{
				// if (!(linkSource instanceof WindGeneratorProsumer)){ //
				// ignore wind farms in economic network
				customers.add((ProsumerAgent) linkSource);
				// }

			}
			else
			{
				throw (new WrongCustomerTypeException(linkSource));
			}
		}
		return customers;
	}

	/**
	 * This method is used to check whether the 'profile building' period has
	 * completed. the 'profile building' period (the initial part of the
	 * training period, usually 4-7 days) is currently determined by a rarely
	 * changed variable in the Consts class
	 * 
	 * @see Consts#AGGREGATOR_PROFILE_BUILDING_PERIODE
	 * @return true if the profile building period is completed, false otherwise
	 */
	private boolean isAggregateDemandProfileBuildingPeriodCompleted()
	{
		boolean isEndOfProfilBuilding = true;
		int daysSoFar = this.mainContext.getDayCount();
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("ADPBPC: daySoFar: " + daysSoFar + " < 7?");
		}

		if (daysSoFar < Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
		{
			isEndOfProfilBuilding = false;
		}
		return isEndOfProfilBuilding;
	}

	/**
	 * This method is used to check whether the 'training' period has completed.
	 * the 'training' period (at least 48 days above 4-7 profile building days)
	 * is currently determined by a rarely changed variable in the Consts class
	 * which starts to be counted after the 'profile building' period has
	 * already completed.
	 * 
	 * @see Consts#AGGREGATOR_TRAINING_PERIODE
	 * @see Consts#AGGREGATOR_PROFILE_BUILDING_PERIODE
	 * @see #isAggregateDemandProfileBuildingPeriodCompleted()
	 * @return true if the training period is completed, false otherwise
	 */
	private boolean isTrainingPeriodCompleted()
	{
		boolean isEndOfTraining = true;
		int daysSoFar = this.mainContext.getDayCount();
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("days so far: " + daysSoFar);
		}
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("TrainingPeriodC: daySoFar: " + daysSoFar + " < 55?");
		}
		if (daysSoFar < (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE))
		{
			isEndOfTraining = false;
		}
		return isEndOfTraining;
	}

	/**
	 * This method is used to update baseline aggregate demand (BAD or simply B)
	 * history matrix by obtaining each customers/prosumers' demand and adding
	 * them up and putting it in the right array cell (i.e. right day [row],
	 * timeslot [column])
	 * 
	 * @param customersList
	 *            the List of cusstumersList (of <code>ProsumerAgent</code>
	 *            type)
	 * @param timeOfDay
	 *            the current timeslot (for a day divided to 48 timeslot, a
	 *            value between 0 to 47)
	 * @param hist_arr_B
	 *            a 2D array for keeping baseline aggregate demand values for
	 *            each timeslot of the day(column) and for different days (row)
	 */
	private void updateAggregateDemandHistoryArray(List<ProsumerAgent> customersList, int timeOfDay, double[][] hist_arr_B)
	{
		double sumDemand = 0d;

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" ====updateHistoryArray==== ");
		}
		for (ProsumerAgent a : customersList)
		{
			sumDemand = sumDemand + a.getNetDemand();
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(" ID: " + a.getAgentID() + " ND: " + a.getNetDemand());
			}

		}

		int dayCount = this.mainContext.getDayCount();

		if (dayCount < hist_arr_B.length)
		{
			hist_arr_B[dayCount][timeOfDay] = sumDemand;
if (			this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger.trace("RECO:: update Total demand for day " + dayCount + " timeslot: " + timeOfDay + " to = "
					+ sumDemand);
}

		}
		else
		{
			System.err.println("RECO:: Trying to add demand for day " + dayCount + " but training array is only " + hist_arr_B.length
					+ " days long.");
		}
	}

	/**
	 * This method calculates the average/ Baseline Aggregate Demands (BAD) for
	 * each timeslot of the day during different days from 2D history array
	 * where each row represent a (different) day and each columns represent a
	 * timeslot of a day (usually divided to half-hour slots) It is basically a
	 * function to provide better readability to the program, as it simply calls
	 * ArrayUtils average calculating function for 2D arrays, which could be
	 * also called direclty
	 * 
	 * @param hist_arr_2D
	 *            a 2D array containing historical baseline aggregate demand
	 *            values for each timeslot of the day(column) and for different
	 *            days (row)
	 * @return double array of average baseline aggregate demands
	 */
	private double[] calculateBADfromHistoryArray(double[][] hist_arr_2D)
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("RECO: calcualteBADfromHistoy: hist_arrr_2D " + ArrayUtils.toString(hist_arr_2D));
		}
		return ArrayUtils.avgCols2DDoubleArray(hist_arr_2D);
	}

	private double calculateAndSetNetDemand(List customersList)
	{

		List<ProsumerAgent> customers = customersList;
		double sumDemand = 0;
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" custmoers list size: " + customers.size());
		}
		for (ProsumerAgent a : customers)
		{

			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(" id: " + a.getAgentID() + " ND: " + a.getNetDemand());
			}
			sumDemand = sumDemand + a.getNetDemand();
			// sum_e = sum_e+a.getElasticityFactor();
		}

		this.setNetDemand(sumDemand);
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("RECO:: calculateAndSetNetDemand: NetDemand set to: " + sumDemand);
		}

		return sumDemand;
	}

	/**
	 * This method builds a predefine signal based on the passed signal type and
	 * a timeslot.
	 * 
	 * @param signalType
	 *            the type of the signal
	 * @param timeslot
	 *            the time of day (usually a day is divided to 48 slots)
	 * @return built signal as array of real numbers (double)
	 */
	private double[] buildSignal(Consts.SIGNAL_TYPE signalType, int timeslot)
	{

		double[] sArr = new double[this.ticksPerDay];

		switch (signalType)
		{
		case S:
			break;

		case S_TRAINING:
			/**
			 * S_TRAINING signal is an array equal to the size of timeslots per
			 * day (usually 48 per day). Att each training day (at least 48
			 * days) a signal consists of s=1 for a specific timeslot of the day
			 * while the rest of timeslots will be s= -1/47
			 */
			int daysSoFar = this.mainContext.getDayCount();
			int indexFor1 = (daysSoFar - Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) % this.ticksPerDay;

			double[] t = this.trainingSigFactory.generateSignal(TRAINING_S_SHAPE.IMPULSE, this.ticksPerDay);
			// double [] t =
			// this.trainingSigFactory.generateSignal(SIGNAL_TYPE.PBORIGINAL,ticksPerDay);

			System.arraycopy(t, 0, sArr, indexFor1, t.length - indexFor1);
			System.arraycopy(t, t.length - indexFor1, sArr, 0, indexFor1);

			/*
			 * sArr[indexFor1] = 1d; if (indexFor1 > 0) { for (int i = 0; i <
			 * indexFor1; i++) { sArr[i] = (-1d/(this.ticksPerDay-1)); //sArr[i]
			 * = 0; } } if (indexFor1 < this.ticksPerDay) { for (int i =
			 * indexFor1+1; i < sArr.length; i++) { sArr[i] =
			 * (-1d/(this.ticksPerDay-1)); //sArr[i] = 0; } }
			 */
			break;

		default: //
			break;
		}

		return sArr;
	}

	/**
	 * This method builds a predefined signal based on the passed signal type.
	 * No timeslot is not necessary to be passed. It bascially calls
	 * buildSignal(SignalType, timeslot) by passing -1 as timeslot for those
	 * signals where the construction does not require a time value.
	 * 
	 * @param signalType
	 *            the type of the signal
	 * @return built signal as array of real numbers (double)
	 * @see #sendSignal(uk.ac.dmu.iesd.cascade.base.Consts.SIGNAL_TYPE,
	 *      double[], List, int)
	 */
	private double[] buildSignal(Consts.SIGNAL_TYPE signalType)
	{
		return this.buildSignal(signalType, -1);
	}

	/**
	 * This method is used send signal to of type S_TRAINING to prosumers
	 * defined by Peter Boait
	 * 
	 * @param broadcasteesList
	 *            the list of broadcastees
	 * @param timeslot
	 *            the time of day (usually a day is divided to 48 slots)
	 * @return true if signal has been sent and received successfully by
	 *         receiver, false otherwise
	 */
	private boolean sendSignal(Consts.SIGNAL_TYPE signalType, double[] signalArr, List broadcasteesList, int timeOfDay)
	{

		boolean isSignalSentSuccessfully = false;

		switch (signalType)
		{
		case S:
			break;
		case S_TRAINING:
			List<ProsumerAgent> paList = broadcasteesList;
			for (ProsumerAgent agent : paList)
			{
				if (this.mainContext.logger.isTraceEnabled())
				{
					this.mainContext.logger.trace("sendSignalType_S, send signal to " + agent.toString());
				}
				// isSignalSentSuccessfully = agent.receiveSignal(arr_i_S,
				// arr_i_S.length, Consts.SIGNAL_TYPE.S);
				isSignalSentSuccessfully = agent.receiveValueSignal(this.arr_i_S, this.arr_i_S.length);
			}
			break;
		default: //
			break;
		}
		return isSignalSentSuccessfully;
	}

	/**
	 * This method broadcasts a signal (of different type) to a list of provided
	 * broadcastees (e.g. Prosumers) at a given timeslot
	 * 
	 * @param signalType
	 *            the type of the signal
	 * @param broadcasteesList
	 *            the list of broadcastees
	 * @param timeslot
	 *            the time of day (usually a day is divided to 48 slots)
	 * @return true if signal has been sent and received successfully by
	 *         receiver, false otherwise
	 */
	private boolean broadcastSignal(Consts.SIGNAL_TYPE signalType, List<?> broadcasteesList, int timeslot)
	{

		boolean isSignalSentSuccessfully = false;

		switch (signalType)
		{
		case S:
			// isSignalSentSuccessfully = sendSignalType_S(broadcasteesList);
			break;
		case S_TRAINING:
			/**
			 * This section is designed to send signal of s[timeslot]=1 and
			 * s[othertimes] = -1/47 (at each timeslot)
			 */
			this.arr_i_S = this.buildSignal(signalType, timeslot);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(timeslot + Arrays.toString(this.arr_i_S));
			}
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(ArrayUtils.isSumEqualZero(this.arr_i_S));
			}
			this.priceSignal = this.arr_i_S;
			isSignalSentSuccessfully = this.sendSignal(signalType, this.arr_i_S, broadcasteesList, timeslot);
			break;

		default: //
			break;
		}

		return isSignalSentSuccessfully;
	}

	/**
	 * This method calculates displacment factors (k) at the end of the day
	 * after S signal has been sent by accepting an array of D values (at the
	 * end of the day), an average baseline aggregate demand (B) array built
	 * during profile building period, the (last) array containing signal (S)
	 * sent during training period, the elasticity factors (e) array and the
	 * reference to the array of k (deplacement factors) where the calculated
	 * values for kii and kji will be placed into it. See Peter Boait formula #7
	 * and #8
	 * 
	 * @param arr_D
	 *            array containing aggregate demand (D) values at the end of the
	 *            day (usually 48 days)
	 * @param arr_B
	 *            array containing average baseline aggregate demand values
	 *            calcualted after profile building period (usually 7 days)
	 * @param arr_S
	 *            array containing signal values sent to customers at the
	 *            begining of the day
	 * @param arr_e
	 *            array containing elasticity factors (e) calcuated so far (at
	 *            the end of each day)
	 * @param arr_k
	 *            the reference to the displacement factor (k) 2D-array where
	 *            the calculated kii and kji values will be placed into it.
	 */
	private void calculateDisplacementFactors_k(double[] arr_D, double[] arr_B, double[] arr_S, double[] arr_e, double[][] arr_k)
	{

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" $ RECO: Calculate k $");
		}

		double e = 0;
		double b = 1;
		double s = 1;
		// double s=-1;
		double deltaB_i = 0;
		int i = ArrayUtils.indexOf(arr_S, s);

		if (i != -1)
		{
			b = arr_B[i];
			deltaB_i = arr_D[i] - b;
			e = arr_e[i];

		}
		else
		{
			System.err.println("Looking for index of " + s + " to train for k values and didn't find it in signal");
		}

		/*
		 * if ( * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" D= "+ arr_D[i]); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" B= "+ b); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" s= "+ s); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" e= "+ e); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" deltaB_i: "+ deltaB_i); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" (s*e*b): "+ (s*e*b)); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" (s*b): "+ (s*b)); }
		 * 
if (		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" deltaB_i - (s*e*b) : "+ (deltaB_i -
		 * (s*e*b)));
}
		 * this.mainContext.logger.debug(" deltaB_i - (s*e*b)/(s*b) : "+
		 * (deltaB_i - (s*e*b))/(s*b)); if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" (s*e*b): "+ (s*e*b)); }
		 */

		arr_k[i][i] = (deltaB_i - (s * e * b)) / (s * b); // 0; <- what does
															// k[i][i] mean?
															// Should it be
															// zero?

		// if (arr_k[i][i] !=0)
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" arr_k[i][i] (" + i + "," + i + ")= " + arr_k[i][i]);
		}

		for (int j = i + 1; j < this.ticksPerDay; j++)
		{

			double b_j = arr_B[j];
			double deltaB_j = arr_D[j] - b_j;
			arr_k[j][i] = deltaB_j / (s * b);
			if (arr_k[j][i] != 0)
			{

				/*
				 * this.mainContext.logger.debug(" arr_k[j][i] ("+j+","+i+")= "+
if (				 * arr_k[j][i]); this.mainContext.logger.isDebugEnabled()) {
				 * arr_k[j][i]); this.mainContext.logger.debug(" where b_j="+b_j
				 * +", arr_D[j]= "+arr_D[j]+", deltaB_j="+deltaB_j);
}
if (				 * this.mainContext.logger.isDebugEnabled()) {
				 * this.mainContext.logger.debug(", and s="+s
				 * +", b= "+b+", s*b= "+(s*b));
}
				 */
			}
		}

		for (int j = i - 1; j >= 0; --j)
		{
			double b_j = arr_B[j];
			double deltaB_j = arr_D[j] - b_j;
			arr_k[j][i] = deltaB_j / (s * b);
			// if (arr_k[j][i] !=0)
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(" arr_k[j][i] (" + j + "," + i + ")= " + arr_k[j][i]);
			}
		}
	}

	/**
	 * This method calculates the elasticity factor (e) for the timeslot of the
	 * day during which S was equal to 1 (S=1), by accepting an arry of D values
	 * (at the end of the day after sending S signal), an average baseline
	 * aggregate demand array built during profile building period, the (last)
	 * array signal sent during training period and the reference to the array
	 * of e (elasticity factors) where the calculated value for the timeslot
	 * when S=1 will be placed into it. It implements Peter Boait formula #6
	 * 
	 * @param arr_D
	 *            array containing aggregate demand (D) values at the end of the
	 *            day (usually 48 days)
	 * @param arr_B
	 *            array containing average baseline aggregate demand values
	 *            calcualted after profile building period (usually 7 days)
	 * @param arr_S
	 *            array containing signal values sent to customers at the
	 *            begining of the day
	 * @param arr_e
	 *            the reference to the elasticity factor (e) array where the
	 *            calcualted e value for the specific timeslot of the day when
	 *            S=1 will be placed into it.
	 * @return double the elasticity factors (e) value for timeslot of the day
	 *         when S=1 (also put into the arr_e passed passed as reference)
	 */
	private double calculateElasticityFactors_e(double[] arr_D, double[] arr_B, double[] arr_S, double[] arr_e)
	{

		double e = 0;
		double b = 1;
		double s = 1; // / Change this value to test for particular index!!!
		// double s=-1;
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" � RECO: Calculate e �");
		}

		double sum_D = ArrayUtils.sum(arr_D);
		double sum_B = ArrayUtils.sum(arr_B);

		int timeslotWhenSwas1 = ArrayUtils.indexOf(arr_S, s);

		if (timeslotWhenSwas1 != -1)
		{
			b = arr_B[timeslotWhenSwas1];
		}

		/*
		 * if ( * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" arr_D= " + Arrays.toString(arr_D)); }
		 * if ( * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" arr_B= " + Arrays.toString(arr_B)); }
		 * if ( * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" sum_D= " + sum_D); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" sum_B= " + sum_B); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" b= " + b); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" s= " + s); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" s= " + (s*b)); } if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug(" sumD-sumB=" + (sum_D - sum_B)); }
		 */

		e = (sum_D - sum_B) / (s * b);

		arr_e[timeslotWhenSwas1] = e;

		return e;
	}

	// (10/02/12) DF
	// Revised elasticity calculation, refer to Peter B's email dated 09/02/12
	// Equation now changed to:
	// sum(Di) - sum(Bi) / sum(Si*Bi)
	// Beware sum(Si*Bi) can result in zero, because of flat/constant Di
	// throughout the whole day
	// Separate method to test before actually commit as the main routine
	private double calculateElasticityFactors_e_new(double[] arr_D, double[] arr_B, double[] arr_S, double[] arr_e)
	{

		double e = 0;
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" � RECO: Calculate e new �");
		}

		double sum_D = ArrayUtils.sum(arr_D);
		double sum_B = ArrayUtils.sum(arr_B);

		double sum_SxB = ArrayUtils.sum(ArrayUtils.mtimes(arr_S, arr_B));

		int timeslotWhenSwas1 = ArrayUtils.indexOf(arr_S, 1f);
		// int timeslotWhenSwas1 = ArrayUtils.indexOf(arr_S, -1f); //TESTT

		if (Math.abs(sum_SxB) > 1e-6)
		{
			e = (sum_D - sum_B) / sum_SxB;
			arr_e[timeslotWhenSwas1] = e;
		}
		else
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("E Factor: " + sum_SxB);
			}
			// Not sure what is the right value to assign
			// Current assumption:
			// if each prosummer will assign e factors (48 timeslot) between 0
			// and 0.1,
			// and also we have sufficiently large amount of prosumers, e.g. 100
			// or more
			// e value for this timslot would be -0.05
			arr_e[timeslotWhenSwas1] = -0.05d;
		}

		return e;
	}

	private double[] minimise_CD_ApacheSimplex(double[] arr_C, double[] arr_B, double[] arr_e, double[][] arr_ij_k, double[] arr_S)
	{
		// private double[] minimise_CD_Apache(double[] arr_C, double[] arr_B,
		// double[] arr_e, double[][] arr_ij_k, double[] arr_S ) throws
		// OptimizationException, FunctionEvaluationException,
		// IllegalArgumentException {
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("---------------RECO: Apache Simplex minimisation (Babak implementation) ---------");
		}

		ArrayRealVector coefficientsArrRealVect = new ArrayRealVector();
		double constantTerm = 0d;

		DoubleArrayList coefficentOf_SjKijBi_ArrList = new DoubleArrayList();

		for (int i = 0; i < arr_S.length; i++)
		{
			constantTerm = constantTerm + (arr_C[i] * arr_B[i]);
			for (int j = 0; j < arr_S.length; j++)
			{
				if (i != j)
				{
					coefficentOf_SjKijBi_ArrList.add(arr_ij_k[i][j] * arr_B[i]);
					if (this.mainContext.logger.isTraceEnabled())
					{
						this.mainContext.logger.trace("RECO: coeff SjKijBj: " + constantTerm);
					}
				}
			}
		}

		for (int i = 0; i < arr_S.length; i++)
		{
			// double s_coeff = (arr_e[i]*arr_B[i])+(arr_ij_k[i][i]*arr_B[i]+
			// coefficentOf_SjKijBi_ArrList.get(i)) ;
			double s_coeff_part1 = (arr_e[i] * arr_B[i]) + (arr_ij_k[i][i]);
			double s_coeff_part2 = 0;
			for (double element : arr_S)
			{
				int offset = i;
				s_coeff_part2 = s_coeff_part2 + coefficentOf_SjKijBi_ArrList.get(i + 47);
				offset += 47;
			}

			coefficientsArrRealVect.append(s_coeff_part1 + s_coeff_part2);
		}

		LinearObjectiveFunction minFunct = new LinearObjectiveFunction(coefficientsArrRealVect, constantTerm);

		double[] constraintCoeff = new double[this.ticksPerDay];
		Arrays.fill(constraintCoeff, 1);
		Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		constraints.add(new LinearConstraint(constraintCoeff, Relationship.EQ, 0));
		RealPointValuePair solution = null;
		try
		{
			solution = new SimplexSolver().optimize(minFunct, constraints, GoalType.MINIMIZE, false);
		}
		catch (OptimizationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// double x = solution.getPoint()[0];
		// double y = solution.getPoint()[1];
		// double min = solution.getValue();

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("RECO: Apache Simplex Solver:: Min value obtained " + solution.getValue());
		}
		// if (solution != null)
		double[] newOpt_S = solution.getPoint();

		return newOpt_S;
	}

	/**
	 * Minimises the Cost times demand function with respect to the signal S
	 * sent to this aggregators' prosumers. Uses the Apache Commons
	 * SimplexSolver to do so
	 * 
	 * @param arr_C
	 * @param arr_B
	 * @param arr_e
	 * @param arr_ij_k
	 * @param arr_S
	 * @return
	 */
	private double[] minimise_CD_Apache(double[] arr_C, double[] arr_B, double[] arr_e, double[][] arr_ij_k, double[] arr_S)
	{
		// private double[] minimise_CD_Apache(double[] arr_C, double[] arr_B,
		// double[] arr_e, double[][] arr_ij_k, double[] arr_S ) throws
		// OptimizationException, FunctionEvaluationException,
		// IllegalArgumentException {
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("---------------RECO: Apache minimisation (SimplexSolver) ---------");
		}

		double[] newOpt_S = Arrays.copyOf(arr_S, arr_S.length);

		SimplexSolver myOpt = new SimplexSolver(1e-3);
		myOpt.setMaxIterations(10000);
		double[] functionCoeffs = new double[this.ticksPerDay];
		double[] displacementConstraintCoeffs = new double[this.ticksPerDay];
		for (int kk = 0; kk < this.ticksPerDay; kk++)
		{

			for (int jj = 0; jj < this.ticksPerDay; jj++)
			{
				functionCoeffs[kk] += arr_ij_k[jj][kk] * arr_B[jj];
			}

			displacementConstraintCoeffs = Arrays.copyOf(functionCoeffs, functionCoeffs.length);

			functionCoeffs[kk] += arr_e[kk] * arr_B[kk];
			functionCoeffs[kk] *= arr_C[kk];
		}
		double constantOffset = ArrayUtils.sum(ArrayUtils.mtimes(arr_C, arr_B));
		LinearObjectiveFunction costFunc = new LinearObjectiveFunction(functionCoeffs, constantOffset);
		double[] constraintCoeffs = new double[this.ticksPerDay];
		Arrays.fill(constraintCoeffs, 1);
		ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		constraints.add(new LinearConstraint(constraintCoeffs, Relationship.EQ, 0));

		/*
		 * Bound the values of the signal to +/- 1 (or another maximal value
		 * stored in Consts) - needed to stop anything shooting off to infinity
		 */
		for (int ll = 0; ll < this.ticksPerDay; ll++)
		{
			double[] coeffs = new double[this.ticksPerDay];
			Arrays.fill(coeffs, 0);
			coeffs[ll] = 1;
			constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, -Consts.NORMALIZING_MAX_COST));
			constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, Consts.NORMALIZING_MAX_COST));
		}

		// An idea - ensure that all delta B due to displacement sums to zero
		// constraints.add(new LinearConstraint(displacementConstraintCoeffs,
		// Relationship.EQ, 0));

		/*
		 * Enforce that Di is greater than zero for all i Note - this only works
		 * with no generation - would need to be be -(MAX_GENERATION) if our
		 * aggregator had generators)
		 */
		for (int i = 0; i < this.ticksPerDay; i++)
		{
			double[] coeffs = new double[this.ticksPerDay];
			for (int j = 0; j < this.ticksPerDay; j++)
			{
				coeffs[j] = arr_ij_k[i][j] * arr_B[i];
			}
			coeffs[i] += arr_e[i] * arr_B[i];
			constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, 0 - arr_B[i]));
		}

		try
		{
			RealPointValuePair optimum = myOpt.optimize(costFunc, constraints, GoalType.MINIMIZE, false);
			newOpt_S = optimum.getPoint();
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Used apache commons Simplex to find optimium " + Arrays.toString(newOpt_S));
			}
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("In " + myOpt.getIterations() + " iterations ");
			}
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Value " + optimum.getValue());
			}
		}
		catch (OptimizationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return newOpt_S;
	}

	private double[] minimise_CD_Apache_Nelder_Mead(double[] arr_C, double[] arr_B, double[] arr_e, double[][] arr_ij_k, double[] arr_S)
	{
		// private double[] minimise_CD_Apache(double[] arr_C, double[] arr_B,
		// double[] arr_e, double[][] arr_ij_k, double[] arr_S ) throws
		// OptimizationException, FunctionEvaluationException,
		// IllegalArgumentException {
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("---------------RECO: Apache minimisation (Nelder Mead) ---------");
		}

		NelderMead apacheNelderMead = new NelderMead();
		// or:
		// NelderMead(double rho, double khi, double gamma, double sigma);
		// rho - reflection coefficient, khi - expansion coefficient
		// gamma - contraction coefficient, sigma - shrinkage coefficient

		/*
		 * RecoMinimisationFunction minFunct = new RecoMinimisationFunction();
		 * 
		 * minFunct.set_C(arr_C);
		 */
		// (26/01/12) Change to test out demand flattening approach
		MinimisationFunctionObjectiveFlatDemand minFunct = new MinimisationFunctionObjectiveFlatDemand();

		minFunct.set_pointer_to_B(arr_B);

		minFunct.addSimpleSumEqualsConstraintForApache(0, 0.01);

		// initial estimates
		// double[] start = arr_i_S;

		// If it's the first time through the optimisation - start from an
		// arbitrary
		// point in search space. Otherwise, start from the last price signal
		double[] start = new double[this.ticksPerDay];
		if (this.firstTimeMinimisation)
		{
			for (int k = 0; k < start.length; k++)
			{
				start[k] = -Math.cos(2 * Math.PI * k / start.length) / 16;
			}
			this.firstTimeMinimisation = false;
		}
		else
		{
			start = this.arr_i_S;
		}

		// apacheNelderMead.setMaxIterations(10000);
		apacheNelderMead.setConvergenceChecker(new SimpleScalarValueChecker(1.0e-10, 1.0e-30));
		// apacheNelderMead.setConvergenceChecker(new
		// SimpleScalarValueChecker(1.0e-2, -1.0));
		// apacheNelderMead.setMaxEvaluations(10000); //how many time function
		// is evaluated

		RealPointValuePair minValue = null;

		try
		{

			minValue = apacheNelderMead.optimize(minFunct, GoalType.MINIMIZE, start);

		}
		catch (@SuppressWarnings("deprecation") OptimizationException e)
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("RECO: Apache Optim Exc (Optim exc): " + e.getCause());
			}
		}
		catch (FunctionEvaluationException e)
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("RECO: Apache Optim Exc (Funct eval exc): " + e.getCause());
			}
		}

		catch (IllegalArgumentException e)
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("RECO: Apache Optim Exc (Illegal arg exc): " + e.getCause());
			}
		}

		/*
		 * minValue.getValue(); minValue.getPoint(); minValue.getPointRef();
		 */

		// double[] newOpt_S= param;
		double[] newOpt_S = minValue.getPoint();

		return newOpt_S;
	}

	/*
	 * private double[] minimise_CD(double[] arr_C, double[] arr_B, double[] arr_e,
	 * double[][] arr_ij_k, double[] arr_S) {
	 * 
	 * Minimisation min = new Minimisation(); MinimisationFunctionNeuralNetReady
	 * minFunct = new MinimisationFunctionNeuralNetReady();
	 * 
	 * minFunct.set_C(arr_C); minFunct.set_B(arr_B); minFunct.set_e(arr_e);
	 * minFunct.set_k(arr_ij_k);
	 * 
	 * // initial estimates // double[] start = arr_i_S;
	 * 
	 * // If it's the first time through the optimisation - start from an //
	 * arbitrary // point in search space. Otherwise, start from the last price
	 * signal double[] start = new double[this.ticksPerDay]; //
	 * if(firstTimeMinimisation ) // { for (int k = 0; k < start.length; k++) {
	 * start[k] = -Math.cos(2 * Math.PI * k / start.length) / 16; }
	 * this.firstTimeMinimisation = false; // } // else // { // start = arr_i_S; //
	 * }
	 * 
	 * // initial step sizes // double[] step = new double[arr_S.length];
	 * 
	 * double ftol = 1e-1; // 1e-15; // convergence tolerance
	 * 
	 * int[] pIndices = new int[this.ticksPerDay]; int[] plusOrMinus = new
	 * int[this.ticksPerDay]; for (int i = 0; i < this.ticksPerDay; i++) {
	 * pIndices[i] = i; plusOrMinus[i] = 1; // step[i] = 0.05; }
	 * 
	 * int direction = 0; double boundary = 0d;
	 * 
	 * min.setNmax(100000); min.addConstraint(pIndices, plusOrMinus, direction,
	 * boundary); min.setConstraintTolerance(1e-3);
	 * 
	 * min.nelderMead(minFunct, start, ftol);
	 * 
	 * // get the minimum value double minimum = min.getMinimum(); if
	 * (this.mainContext.logger.isTraceEnabled()) {
	 * this.mainContext.logger.trace("RECO: Minimum = " + minimum); }
	 * 
	 * // get values of y and z at minimum double[] param = min.getParamValues();
	 * 
	 * // min.print("MinimCD_output.txt");
	 * 
	 * if (this.mainContext.logger.getLevel().equals(Level.TRACE)) { // Print
	 * results to a text file // min.print("MinimCD_output.txt");
	 * 
	 * // Output the results to screen if (this.mainContext.logger.isTraceEnabled())
	 * { this.mainContext.logger.trace("RECO: Minimum = " + min.getMinimum()); } if
	 * (this.mainContext.logger.isTraceEnabled()) {
	 * this.mainContext.logger.trace("RECO: Min (S) sum = " +
	 * ArrayUtils.sum(param)); }
	 * 
	 * for (int i = 0; i < param.length; i++) { if
	 * (this.mainContext.logger.isTraceEnabled()) {
	 * this.mainContext.logger.trace("RECO: Flanagan Value of s at the minimum for "
	 * + i + " ticktime is: " + param[i]); } } if
	 * (this.mainContext.logger.isTraceEnabled()) { this.mainContext.logger.
	 * trace("RECO:: Flanagan optimisation evaluated function " +
	 * minFunct.getNumEvals() + " times"); } }
	 * 
	 * double[] newOpt_S = param; if (this.mainContext.logger.isTraceEnabled()) {
	 * this.mainContext.logger.trace("Minimum achieved is " + min.getMinimum()); }
	 * return newOpt_S; }
	 */

	/*
	 * private double[] minimise_CD_Genetic_Algorithm(double[] arr_C, double[]
	 * arr_B, double[] arr_e, double[][] arr_ij_k, double[] arr_S) { double[]
	 * returnArray = new double[this.ticksPerDay];
	 * 
	 *//*** Richard's Genetic algorithm optimisation ***//*
															 * Configuration conf = new DefaultConfiguration();
															 * 
															 * // Set the fitness function we want to use, which is our
															 * // MinimizingMakeChangeFitnessFunction that we created
															 * earlier. // We construct it with the target amount of
															 * change provided // by the user. //
															 * ---------------------------------------------------------
															 * ---
															 * 
															 * MinimisationFunctionNeuralNetReady geneticMinFunc = new
															 * MinimisationFunctionNeuralNetReady();
															 * geneticMinFunc.set_B(arr_B); geneticMinFunc.set_C(arr_C);
															 * geneticMinFunc.set_e(arr_e);
															 * geneticMinFunc.set_k(arr_ij_k);
															 * geneticMinFunc.addSimpleSumEqualsConstraintForApache(0,
															 * 0.01);
															 * 
															 * try { conf.setFitnessFunction(geneticMinFunc);
															 * 
															 * // Now we need to tell the Configuration object how we
															 * want our // Chromosomes to be setup. We do that by
															 * actually creating a // sample Chromosome and then setting
															 * it on the Configuration // object. As mentioned earlier,
															 * we want our Chromosomes to // each have four genes, one
															 * for each of the coin types. We // want the values of
															 * those genes to be integers, which represent // how many
															 * coins of that type we have. We therefore use the //
															 * IntegerGene class to represent each of the genes. That
															 * class // also lets us specify a lower and upper bound,
															 * which we set // to sensible values for each coin type. //
															 * ---------------------------------------------------------
															 * ----- DoubleGene[] sampleGenes = new DoubleGene[48];
															 * 
															 * for (int ii = 0; ii < sampleGenes.length; ii++) {
															 * sampleGenes[ii] = new DoubleGene(conf,-1, 1); }
															 * 
															 * 
															 * Chromosome sampleChromosome = new
															 * Chromosome(conf,sampleGenes);
															 * 
															 * conf.setSampleChromosome(sampleChromosome);
															 * 
															 * conf.setPopulationSize(1500);
															 * 
															 * Genotype population =
															 * Genotype.randomInitialGenotype(conf); for (int evolutions
															 * = 0; evolutions < 100; evolutions++) {
															 * population.evolve(); } IChromosome bestSolutionSoFar =
															 * population.getFittestChromosome();
															 * 
															 * for (int jj = 0; jj < returnArray.length; jj++) {
															 * returnArray[jj] = 2 * (((DoubleGene)
															 * bestSolutionSoFar.getGene(jj)).doubleValue() - 0.5d); } }
															 * catch (InvalidConfigurationException e) { // TODO
															 * Auto-generated catch block System.err.
															 * println("RECO:: Invalid configuration for genetic algorithm"
															 * ); e.printStackTrace(); } return returnArray; }
															 */
	private void updateCumulativeSaving(double savingAmount)
	{
		int daysSoFar = this.mainContext.getDayCount();
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("days so far: " + daysSoFar);
		}
		if (daysSoFar > (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE))
		{
			this.cumulativeCostSaving += savingAmount;
		}

	}

	// temp funciton - to be removed
	private double calculateNetDemand(List customersList)
	{

		List<ProsumerAgent> customers = customersList;
		double sumDemand = 0;
		for (ProsumerAgent a : customers)
		{
			sumDemand = sumDemand + a.getNetDemand();
		}

		return sumDemand;
	}

	/**
	 * Returns a string representing the state of this agent. This method is
	 * intended to be used for debugging purposes, and the content and format of
	 * the returned string should include the states (variables/parameters)
	 * which are important for debugging purpose. The returned string may be
	 * empty but may not be <code>null</code>.
	 * 
	 * @return a string representation of this agent's state parameters
	 */
	@Override
	protected String paramStringReport()
	{
		String str = "";
		return str;
	}

	protected void broadcastDemandSignal(List<ProsumerAgent> broadcastCusts, double time, int broadcastLength)
	{

		// To avoid computational load (and realistically model a reasonable
		// broadcast strategy)
		// only prepare and transmit the price signal if it has changed.
		if (this.priceSignalChanged)
		{
			// populate the broadcast signal with the price signal starting from
			// now and continuing for
			// broadcastLength samples - repeating copies of the price signal if
			// necessary to pad the
			// broadcast signal out.
			double[] broadcastSignal = new double[broadcastLength];
			int numCopies = (int) Math.floor((broadcastLength - 1) / this.priceSignal.length);
			int startIndex = (int) time % this.priceSignal.length;

			System.arraycopy(this.priceSignal, startIndex, broadcastSignal, 0, this.priceSignal.length - startIndex);
			for (int i = 1; i <= numCopies; i++)
			{
				int addIndex = (this.priceSignal.length - startIndex) * i;
				System.arraycopy(this.priceSignal, 0, broadcastSignal, addIndex, this.priceSignal.length);
			}

			if (broadcastLength > (((numCopies + 1) * this.priceSignal.length) - startIndex))
			{
				System.arraycopy(this.priceSignal, 0, broadcastSignal, ((numCopies + 1) * this.priceSignal.length) - startIndex, broadcastLength
						- (((numCopies + 1) * this.priceSignal.length) - startIndex));
			}

			for (ProsumerAgent a : broadcastCusts)
			{
				// Broadcast signal to all customers - note we simply say that
				// the signal is valid
				// from now currently, in future implementations we may want to
				// be able to insert
				// signals valid at an offset from now.

				a.receiveValueSignal(broadcastSignal, broadcastLength);
			}
		}

		this.priceSignalChanged = false;
	}

	/**
	 * This methods writes the parameters passed by arguments into CSV file
	 * format.
	 * 
	 * @param fileName
	 * @param C
	 * @param NC
	 * @param B
	 * @param D
	 * @param S
	 * @param e
	 * @param k
	 */
	private void writeOutput(String fileName, boolean addInfoHeader, double[] C, double[] NC, double[] B, double[] D, double[] S,
			double[] e, double[][] k)
	{
		int[] ts_arr = new int[this.ticksPerDay];

		for (int i = 0; i < ts_arr.length; i++)
		{
			ts_arr[i] = i;
		}
		String resFileName = fileName + this.mainContext.getDayCount() + ".csv";

		CSVWriter res = new CSVWriter(resFileName, false);

		if (addInfoHeader)
		{

			res.appendText("Random seed= " + this.mainContext.getRandomSeedValue());
			res.appendText("Number of Prosumers= " + this.mainContext.getTotalNbOfProsumers());
			res.appendText("ProfileBuildingPeriod= " + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE);
			res.appendText("TrainingPeriod= " + Consts.AGGREGATOR_TRAINING_PERIODE);
			res.appendText("REEA on?= " + Consts.AGG_RECO_REEA_ON);
			res.appendText("ColdAppliances on?= " + Consts.HHPRO_HAS_COLD_APPL);
			res.appendText("WetAppliances on?= " + Consts.HHPRO_HAS_WET_APPL);
			res.appendText("ElectSpaceHeat on?= " + Consts.HHPRO_HAS_ELEC_SPACE_HEAT);
			res.appendText("ElectWaterHeat on?= " + Consts.HHPRO_HAS_ELEC_WATER_HEAT);
			res.appendText("");
		}

		res.appendText("Timeslots:");
		res.appendRow(ts_arr);
		if (C != null)
		{
			res.appendText("C:");
			res.appendRow(C);
		}
		if (C != null)
		{
			res.appendText("C (normalized):");
			res.appendRow(NC);
		}

		res.appendText("B:");
		res.appendRow(B);
		res.appendText("D (for end of day " + this.mainContext.getDayCount() + "): ");
		res.appendRow(D);
		res.appendText("S (for end of day " + this.mainContext.getDayCount() + "): ");
		res.appendRow(S);
		res.appendText("e (for end of day " + this.mainContext.getDayCount() + "): ");
		res.appendRow(e);
		res.appendText("k (for end of day " + this.mainContext.getDayCount() + "): ");
		res.appendCols(k);
		res.close();

	}

	/**
	 * This methods estimates the error and ajust k and e values accordingly. It
	 * is implemented based on Routine Error Estimation and Adjustment (REEA)
	 * section in the paper.
	 * 
	 * @param arr_i_B
	 * @param arr_i_S
	 * @param arr_i_e
	 * @param arr_ij_k
	 */
	private void errorEstimationAndAdjustment(double[] arr_i_B, double[] arr_hist_1D, double[] arr_i_S, double[] arr_i_e,
			double[][] arr_ij_k)
	{

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" --REEA-- ");
		}

		double[] actualShift = ArrayUtils.add(arr_hist_1D, ArrayUtils.negate(arr_i_B));

		Matrix k = new Matrix(arr_ij_k);
		double[][] bs = new double[1][arr_i_B.length];
		bs[0] = ArrayUtils.mtimes(arr_i_B, arr_i_S);

		Matrix bs_mat = new Matrix(bs);
		bs_mat.transpose();

		double[] bse = new double[this.ticksPerDay];

		bse = ArrayUtils.mtimes(arr_i_S, arr_i_e, arr_i_B);

		double[] bsk = new double[this.ticksPerDay];
		bsk = Matrix.times(bs_mat, k).getRowCopy(0);

		double[] predictedShift = ArrayUtils.add(ArrayUtils.mtimes(arr_i_S, arr_i_e, arr_i_B), (Matrix.times(bs_mat, k).getRowCopy(0)));

		predictedShift = ArrayUtils.add(predictedShift, arr_i_B);
		double[] arr_errorEstim_R = ArrayUtils.add(predictedShift, ArrayUtils.negate(arr_hist_1D));
		arr_errorEstim_R = ArrayUtils.mtimes(arr_errorEstim_R, ArrayUtils.pow(arr_i_B, -1));

		double[] arr_multiplier = ArrayUtils.offset(ArrayUtils.multiply(arr_errorEstim_R, this.alpha), (1 - this.alpha));

		arr_i_e = ArrayUtils.mtimes(arr_i_e, arr_multiplier);

		for (int i = 0; i < arr_ij_k.length; i++)
		{
			arr_ij_k[i] = ArrayUtils.mtimes(arr_ij_k[i], arr_multiplier);
		}
	}

	/**
	 * This methode calcuates saving (or lost) for the aggregator. It updates
	 * daily cost and cumulative saving array lists.
	 * 
	 * @param arr_i_C
	 * @param arr_hist_1D
	 * @param arr_i_B
	 * @param arr_i_S
	 * @param arr_i_e
	 */
	private void costSavingCalculation(double[] arr_i_C, double[] arr_hist_1D, double[] arr_i_B, double[] arr_i_S, double[] arr_i_e)
	{

if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(" --^costSavingCalculation-- Daycount: " + this.mainContext.getDayCount() + ",Timeslot: "
				+ this.mainContext.getTimeslotOfDay() + ",TickCount: " + this.mainContext.getTickCount());
}
		double predCost = 0;
		double actualCost = 0;

		double[] day_predicted_arr_D = new double[this.ticksPerDay];

		for (int i = 0; i < this.ticksPerDay; i++)
		{
			// predCost += arr_i_C[i] * calculate_PredictedDemand_D(i);
			day_predicted_arr_D[i] = this.calculate_PredictedDemand_D(i); // this
																			// was
																			// previously
																			// used
																			// instead
																			// of
																			// B
																			// for
																			// calculation
																			// of
																			// predicated
																			// cost,
																			// can
																			// be
																			// deleted
																			// later
		}

		predCost = ArrayUtils.sum(ArrayUtils.mtimes(arr_i_B, arr_i_C));
		this.dailyPredictedCost.add(predCost);

		actualCost = ArrayUtils.sum(ArrayUtils.mtimes(arr_hist_1D, arr_i_C));
		this.dailyActualCost.add(actualCost);

		this.updateCumulativeSaving(predCost - actualCost);

	}

	@Override
	public void marketPreStep()
	{
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" initializeMarketStep (SupplierCo) " + this.id);
		}
		this.settlementPeriod = this.mainContext.getSettlementPeriod();

		switch (this.settlementPeriod)
		{

		case 13:
			if (this.mainContext.isMarketFirstDay())
			{
				System.arraycopy(ArrayUtils.negate(this.arr_i_B), 0, this.arr_PN, 0, this.arr_i_B.length);
			}
			else
			{
				// System.arraycopy(ArrayUtils.negate(arr_hist_day_D), 0,
				// arr_PN, 0, arr_hist_day_D.length);
				System.arraycopy(ArrayUtils.negate(this.getDayNetDemands()), 0, this.arr_PN, 0, this.getDayNetDemands().length);
			}
			break;
		}
	}

	@Override
	public void bizPreStep()
	{

if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(" ============ SupplierCO pre_step ========= DayCount: " + this.mainContext.getDayCount()
				+ ",Timeslot: " + this.mainContext.getTimeslotOfDay() + ",TickCount: " + this.mainContext.getTickCount());
}
		this.timeTick = this.mainContext.getTickCount();
		this.timeslotOfDay = this.mainContext.getTimeslotOfDay();

		if (this.timeTick == 0)
		{
			this.customers = this.getCustomersList();
		}

		if (this.isAggregateDemandProfileBuildingPeriodCompleted())
		{ // End of history profile building period
			// Set the Baseline demand on the first time through after building
			// period

			if (this.mainContext.getDayCount() == Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
			{
				this.arr_i_B = this.calculateBADfromHistoryArray(ArrayUtils
						.subArrayCopy(this.arr_hist_ij_D, 0, Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE));
			}

			if (!this.isTrainingPeriodCompleted())
			{ // training period, signals should be send S=1 for 48 days
				if (this.mainContext.isBeginningOfDay(this.timeslotOfDay))
				{
					this.arr_i_S = this.buildSignal(Consts.SIGNAL_TYPE.S_TRAINING);
					this.broadcastSignalToCustomers(this.arr_i_S, this.customers);
				}
			} // training period completed
			else
			{ // Begining of the normal operation- both baseline establishing &
				// training periods are completed

				if (this.mainContext.isBeginningOfDay(this.timeslotOfDay))
				{

					if (Consts.AGG_RECO_REEA_ON)
					{
						this.errorEstimationAndAdjustment(this.arr_i_B, this.getDayNetDemands(), this.arr_i_S, this.arr_i_e, this.arr_ij_k);
						// errorEstimationAndAdjustment(arr_i_B, arr_hist_day_D,
						// arr_i_S, arr_i_e, arr_ij_k);
					}

					this.arr_i_norm_C = ArrayUtils.normalizeValues(ArrayUtils.offset(this.arr_i_C, -ArrayUtils.sum(this.arr_i_C)
							/ this.arr_i_C.length));

					// double [] priceSignalTest =
					// {-0.050175966,-0.057895417,-0.061784662,-0.065208969,-0.069459832,-0.072087102,-0.072957939,-0.073762356,-0.074522493,-0.079503974,-0.0777623,-0.07263322,-0.054729409,-0.03354889,-0.007357371,0.010029843,0.022435575,0.028472392,0.035785944,0.038582953,0.040154887,0.043564434,0.045438947,0.045948165,0.046730442,0.045254448,0.045350387,0.044066272,0.041741581,0.040664105,0.040420566,0.042907617,0.04468619,0.047040401,0.046730442,0.041778481,0.035321006,0.030568304,0.024295328,0.019601666,0.014900624,0.010568581,0.01084902,0.010384081,0.005727319,-0.009194983,-0.026139398,-0.04127572};
					// double [] priceSignalTestSq =
					// {-0.05214123,-0.058519428,-0.061637587,-0.064330107,-0.067603658,-0.069588738,-0.070240282,-0.070839287,-0.071402807,-0.075035391,-0.073777246,-0.069997708,-0.055933952,-0.037548357,-0.012193205,0.006238673,0.020170176,0.027184535,0.035888531,0.039277016,0.041195863,0.045393732,0.047722565,0.048357764,0.04933571,0.047492691,0.047612207,0.046015744,0.043143309,0.041819699,0.041521206,0.044581234,0.046785581,0.049723912,0.04933571,0.043188725,0.035328476,0.029655829,0.022314676,0.016930452,0.011631015,0.006830171,0.007138559,0.006627465,0.001558774,-0.014066632,-0.030669314,-0.044475072};

					// arr_i_S = minimise_CD_Genetic_Algorithm(normalizedCosts,
					// arr_i_B, arr_i_e, arr_ij_k, arr_i_S);
					// arr_i_S = minimise_CD_Apache(normalizedCosts, arr_i_B,
					// arr_i_e, arr_ij_k, arr_i_S);
					// arr_i_S = minimise_CD_ApacheSimplex(arr_i_C, arr_i_B,
					// arr_i_e, arr_ij_k, arr_i_S);

					Parameters params = RunEnvironment.getInstance().getParameters();
					switch ((Integer) params.getValue("signalMode"))
					{
					case 0: /* Real Cost version */
						double fixedPrice[] = new double[48];
						for (int i = 0; i < 48; i++)
						{
							fixedPrice[i] = 0;
						}
						this.arr_i_S = fixedPrice;
						break;
					case 1: /*
							 * Differential Price version (actual cost - average
							 * cost)
							 */
						// arr_i_S = arr_i_norm_C;
						Arrays.fill(this.arr_i_S, 0);

						if (this.messageBoard.getMIP() != null)
						{
							this.arr_i_S = this.messageBoard.getMIP();
							this.arr_i_S = ArrayUtils.normalizeValues(this.arr_i_S);
						}

						break;
					case 2: /* Smart signal version */
						this.arr_i_S = this
								.minimise_CD_Apache_Nelder_Mead(this.arr_i_norm_C, this.arr_i_B, this.arr_i_e, this.arr_ij_k, this.arr_i_S);
						break;
					}
					if (this.mainContext.logger.isTraceEnabled())
					{
						this.mainContext.logger.trace(" arr_i_S: " + Arrays.toString(this.arr_i_S));
					}

					this.broadcastSignalToCustomers(this.arr_i_S, this.customers);

					// if (Consts.DEBUG)
					// writeOutput("output2_NormalBiz_day_",false, arr_i_C,
					// arr_i_norm_C, arr_i_B, this.getDayNetDemands(), arr_i_S,
					// arr_i_e, arr_ij_k);
				}

			} // end of begining of normal operation

		} // end of else (history profile building)
if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(" ========== RECO: pre_step END =========== DayCount: " + this.mainContext.getDayCount()
				+ ",Timeslot: " + this.mainContext.getTimeslotOfDay() + ",TickCount: " + this.mainContext.getTickCount());
}
	}

	/**
	 * This method defines how this object behaves (what it does) at at a given
	 * scheduled time throughout the simulation.
	 */
	@Override
	public void bizStep()
	{

if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(" ++++++++++++++ SupplierCO step +++++++++++++ DayCount: " + this.mainContext.getDayCount()
				+ ",Timeslot: " + this.mainContext.getTimeslotOfDay() + ",TickCount: " + this.mainContext.getTickCount());
}
		if (!this.isAggregateDemandProfileBuildingPeriodCompleted())
		{
			this.updateAggregateDemandHistoryArray(this.customers, this.timeslotOfDay, this.arr_hist_ij_D);
		}
		else if (!this.isTrainingPeriodCompleted())
		{
			this.updateAggregateDemandHistoryArray(this.customers, this.timeslotOfDay, this.arr_hist_ij_D);

			if (this.mainContext.isEndOfDay(this.timeslotOfDay))
			{

				double[] arr_last_training_D = ArrayUtils.rowCopy(this.arr_hist_ij_D, this.mainContext.getDayCount());
				double e = this.calculateElasticityFactors_e(arr_last_training_D, this.arr_i_B, this.arr_i_S, this.arr_i_e);

				this.calculateDisplacementFactors_k(arr_last_training_D, this.arr_i_B, this.arr_i_S, this.arr_i_e, this.arr_ij_k);

				// if (mainContext.getDayCount() >
				// ((Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE+Consts.AGGREGATOR_TRAINING_PERIODE))-2)
				// writeOutput("output1_TrainingPhase_day_",true,arr_i_C,
				// arr_i_norm_C, arr_i_B, arr_last_training_D, arr_i_S, arr_i_e,
				// arr_ij_k);
			}

			//
		}

		this.calculateAndSetNetDemand(this.customers);

		// arr_hist_day_D[timeslotOfDay] = getNetDemand();

		if (this.mainContext.isEndOfDay(this.timeslotOfDay))
		{
			this.costSavingCalculation(this.arr_i_C, this.getDayNetDemands(), this.arr_i_B, this.arr_i_S, this.arr_i_e);
		}

		// (30/01/12) DF
		// Write out <code>netDemand<\code> for each day for demand flattening
		// test
		// if(isAggregateDemandProfileBuildingPeriodCompleted() &&
		// isTrainingPeriodCompleted() && mainContext.isEndOfDay(timeslotOfDay))
		// printOutNetDemand4DemandFlatteningTest();

if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace(" ++++++++++ SupplierCO: END ++++++++++++ DayCount: " + this.mainContext.getDayCount()
				+ ",Timeslot: " + this.mainContext.getTimeslotOfDay() + ",TickCount: " + this.mainContext.getTickCount());
}
	}

	/**
	 * Constructs a SupplierCO agent with the context in which is created and
	 * its base demand.
	 * 
	 * @param context
	 *            the context in which this agent is situated
	 * @param baseDemand
	 *            an array containing the base demand
	 */
	// public SupplierCo(CascadeContext context, MarketMessageBoard mb, double[]
	// baseDemand, BMU_CATEGORY cat, BMU_TYPE type, double maxDem, double
	// minDem) {
	public SupplierCo(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxDem, double minDem,
			double[] baseDemand)
	{

		super(context, mb, cat, type, maxDem, minDem, baseDemand);
		// super(context);

		this.ticksPerDay = context.getNbOfTickPerDay();

		if (baseDemand.length % this.ticksPerDay != 0)
		{
			System.err.print("SupplierCo: Error/Warning message from " + this.toString()
					+ ": BaseDemand array imported to aggregator not a whole number of days");
			System.err.println(" SupplierCo:  May cause unexpected behaviour - unless you intend to repeat the signal within a day");
		}
		this.priceSignal = new double[baseDemand.length];
		this.overallSystemDemand = new double[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.overallSystemDemand, 0, this.overallSystemDemand.length);

		// Start initially with a flat price signal of 12.5p per kWh
		// Arrays.fill(priceSignal,125f);
		Arrays.fill(this.priceSignal, 0f);

		// Very basic configuration of predicted customer demand as
		// a Constant. We could be more sophisticated than this or
		// possibly this gives us an aspirational target...
		this.predictedCustomerDemand = new double[this.ticksPerDay];

		// arr_hist_day_D = new double[ticksPerDay];

		this.dailyPredictedCost = new ArrayList<Double>();
		this.dailyActualCost = new ArrayList<Double>();

		// /+++++++++++++++++++++++++++++++++++++++
		this.arr_i_B = new double[this.ticksPerDay];
		this.arr_i_e = new double[this.ticksPerDay];
		this.arr_i_S = new double[this.ticksPerDay];
		this.arr_i_C = new double[this.ticksPerDay];
		this.arr_i_norm_C = new double[this.ticksPerDay];
		this.arr_ij_k = new double[this.ticksPerDay][this.ticksPerDay];
		this.arr_hist_ij_D = new double[Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE + Consts.AGGREGATOR_TRAINING_PERIODE][this.ticksPerDay];

		this.arr_i_C_all = ArrayUtils.normalizeValues(ArrayUtils.pow2(baseDemand), 100); // all
																							// costs
																							// (equivalent
																							// to
																							// size
																							// of
																							// baseDemand,
																							// usually
																							// 1
																							// week)

		// Set up basic learning factor
		this.alpha = 0.1d;
		// this.alpha = 1d;

		this.arr_day_D = new double[this.ticksPerDay];

		this.trainingSigFactory = new TrainingSignalFactory(this.ticksPerDay);
		// +++++++++++++++++++++++++++++++++++++++++++

	}

}
