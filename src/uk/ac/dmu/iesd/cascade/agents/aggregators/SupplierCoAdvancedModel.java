package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.commons.mathforsimplex.FunctionEvaluationException;
import org.apache.commons.mathforsimplex.optimization.GoalType;
import org.apache.commons.mathforsimplex.optimization.OptimizationException;
import org.apache.commons.mathforsimplex.optimization.RealPointValuePair;
import org.apache.commons.mathforsimplex.optimization.SimpleScalarValueChecker;
import org.apache.commons.mathforsimplex.optimization.direct.NelderMead;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.MinimisationFunctionObjectiveArbitraryArray;
import uk.ac.dmu.iesd.cascade.util.MinimisationFunctionObjectiveFlatDemand;
//import uk.ac.dmu.iesd.cascade.util.MinimisationFunctionObjectiveOvernightWind;
import uk.ac.dmu.iesd.cascade.util.WrongCustomerTypeException;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.TrainingSignalFactory;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.TrainingSignalFactory.TRAINING_S_SHAPE;
import flanagan.math.Matrix;

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
 * @author J. Richard Snape
 * @version $Revision: 2.1 $ $Date: 2014/02/18 $
 * 
 *          Version history (for intermediate steps see Git repository history)
 * 
 *          1.0 - created the concrete class of AggregatorAgent kind after the
 *          latter was made abstract upon my suggestion 1.1 - Class name changed
 *          to SupplierCo (previously named RECO) (2012/05/18) 2.0 2.1 - Cleaned
 *          code - refactored ~1000 lines
 * 
 */

public class SupplierCoAdvancedModel extends AggregatorAgent/* BMPxTraderAggregator */
{

	/**
	 * the aggregator agent's base name
	 **/
	protected static String agentBaseName = "SupplierCO";

	/**
	 * indicates whether the agent is in training mode/ period
	 **/
	protected boolean isTraining = true;

	/**
	 * indicates whether the agent is in 'profile building' mode/period
	 **/
	protected boolean isProfileBuidling = true;

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

	int timeTick;
	int timeslotOfDay;
	int dayOfWeek;

	private TrainingSignalFactory trainingSigFactory;

	private double totalCO2saving;

	public double getTotalCO2saving() {
		return this.totalCO2saving;
	}

	/**
	 * Basic helper method to calculate CO2 emissions and avoided by this
	 * aggregator
	 */
	private void updateTotalCO2Avoided() {
		this.totalCO2saving += (this.arr_i_B[this.timeslotOfDay] - this
				.getNetDemand())
				* Consts.AVERAGE_GRID_CO2_INTENSITY[this.timeslotOfDay];
	}

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
	protected double calculate_Price_P(int timeslot) {
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

	private double calculate_deltaB(int timeslot_i) {
		double sumOf_SjKijBi = 0d;
		for (int j = 0; j < this.ticksPerDay; j++) {
			if (j != timeslot_i) { // i!=j
				sumOf_SjKijBi += this.arr_i_S[j] * this.arr_ij_k[timeslot_i][j]
						* this.arr_i_B[timeslot_i];
				// sumOf_SjKijBi +=
				// arr_i_S[j]*arr_ij_k[j][timeslot_i]*arr_i_B[timeslot_i];

			}
		}
		double leftSideEq = this.arr_i_S[timeslot_i]
				* this.arr_ij_k[timeslot_i][timeslot_i]
				* this.arr_i_B[timeslot_i];

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
	protected double calculate_PredictedDemand_D(int timeslot) {
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
	protected double calculate_e(double[] arr_D, double[] arr_B, double s,
			double B) {

		double e = 0;
		double sum_D = ArrayUtils.sum(arr_D);
		double sum_B = ArrayUtils.sum(arr_B);
		if ((s != 0) && (B != 0)) {
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
	protected double calculate_k(int t_i, int t_j) {

		double k_ij = 0;
		double divisor = 1;

		if (t_i == t_j) { // calculate Kii
			double delta_Bi = this.calculate_deltaB(t_i);
			double divident = delta_Bi
					- (this.arr_i_S[t_i] * this.arr_i_e[t_i] * this.arr_i_B[t_i]);
			divisor = this.arr_i_S[t_i] * this.arr_i_B[t_i];
			k_ij = divident / divisor;
		}

		else { // calculate Kij
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
	protected void predictDemand(List<ProsumerAgent> customersList, int t) {

		double sumDemand = 0;
		for (ProsumerAgent a : customersList) {
			sumDemand = sumDemand + a.getNetDemand();
		}
		this.arr_i_B[t] = sumDemand;

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
	private boolean isAggregateDemandProfileBuildingPeriodCompleted() {
		boolean isEndOfProfileBuilding = true;
		int daysSoFar = this.mainContext.getDayCount();
		if (daysSoFar < Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) {
			isEndOfProfileBuilding = false;
		}
		return isEndOfProfileBuilding;
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
	private boolean isTrainingPeriodCompleted() {
		boolean isEndOfTraining = true;
		int daysSoFar = this.mainContext.getDayCount();
		if (daysSoFar < (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)) {
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
	private void updateAggregateDemandHistoryArray(
			List<ProsumerAgent> customersList, int timeOfDay,
			double[][] hist_arr_B) {
		double sumDemand = 0d;

		if (this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug(" ====updateHistoryArray==== ");
		}
		if (this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger
					.debug("Entering loop through all customers to add their net Demands to history ");
		}
		for (ProsumerAgent a : customersList) {
			sumDemand = sumDemand + a.getNetDemand();

		}

		int dayCount = this.mainContext.getDayCount();

		if (dayCount < hist_arr_B.length) {
			hist_arr_B[dayCount][timeOfDay] = sumDemand;
			if (this.mainContext.logger.isTraceEnabled()) {
				this.mainContext.logger
						.trace("RECO:: update Total demand for day " + dayCount
								+ " timeslot: " + timeOfDay + " to = "
								+ sumDemand);
			}

		} else {
			System.err
					.println("SupplierCoAdvancedModel:: Trying to add demand for day "
							+ dayCount
							+ " but training array is only "
							+ hist_arr_B.length + " days long.");
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
	private double[] calculateBADfromHistoryArray(double[][] hist_arr_2D) {
		if (this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger
					.debug("RECO: calcualteBADfromHistoy: hist_arrr_2D "
							+ ArrayUtils.toString(hist_arr_2D));
		}
		return ArrayUtils.avgCols2DDoubleArray(hist_arr_2D);
	}

	private double calculateAndSetNetDemand(List customersList) {

		List<ProsumerAgent> customers = customersList;
		double sumDemand = 0;
		if (this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug(" customers list size: "
					+ customers.size()
					+ " - entering loop to add all net demands");
		}
		for (ProsumerAgent a : customers) {
			sumDemand = sumDemand + a.getNetDemand();
		}
		this.setNetDemand(sumDemand);
		if (this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger
					.debug("RECO:: calculateAndSetNetDemand: NetDemand set to: "
							+ sumDemand);
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
	private double[] buildSignal(Consts.SIGNAL_TYPE signalType, int timeslot) {

		double[] sArr = new double[this.ticksPerDay];

		switch (signalType) {
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
			int indexFor1 = (daysSoFar - Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
					% this.ticksPerDay;

			double[] t = this.trainingSigFactory.generateSignal(
					TRAINING_S_SHAPE.PBORIGINAL, this.ticksPerDay);

			System.arraycopy(t, 0, sArr, indexFor1, t.length - indexFor1);
			System.arraycopy(t, t.length - indexFor1, sArr, 0, indexFor1);

			break;

		default:
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
	private double[] buildSignal(Consts.SIGNAL_TYPE signalType) {
		return this.buildSignal(signalType, -1);
	}


	/**
	 * Minimises the Cost times demand function with respect to the signal S
	 * sent to this aggregators' prosumers. Uses the Apache Commons
	 * SimplexSolver to do so with the Nelder-Mead non-linear optimisation
	 * algorithm
	 * 
	 * @param arr_C
	 * @param arr_B
	 * @param arr_e
	 * @param arr_ij_k
	 * @param arr_S
	 * @return
	 */
	private double[] minimise_CD_Apache_Nelder_Mead(double[] arr_C,
			double[] arr_B, double[] arr_e, double[][] arr_ij_k, double[] arr_S) {

		NelderMead apacheNelderMead = new NelderMead();

		// MinimisationFunctionObjectiveFlatDemand minFunct = new
		// MinimisationFunctionObjectiveFlatDemand();
		// MinimisationFunctionObjectiveOvernightWind minFunct = new
		// MinimisationFunctionObjectiveOvernightWind();
		MinimisationFunctionObjectiveArbitraryArray minFunct = new MinimisationFunctionObjectiveArbitraryArray(
				this.getPredictedGeneration());

		minFunct.set_pointer_to_B(arr_B);
		minFunct.set_pointer_to_Kneg(this.Kneg);
		minFunct.set_pointer_to_Kpos(this.Kpos);
		minFunct.set_pointer_to_Cavge(this.Cavge);

		minFunct.addSimpleSumEqualsConstraintForApache(0, 0.01);

		// initial estimates
		// double[] start = arr_i_S;

		// If it's the first time through the optimisation - start from an
		// arbitrary
		// point in search space. Otherwise, start from the last price signal
		double[] start = new double[this.ticksPerDay];
		if (this.firstTimeMinimisation) {
			for (int k = 0; k < start.length; k++) {
				start[k] = -Math.cos(2 * Math.PI * k / start.length) / 16;
			}
			// the line below can feed in the baseline demand, appropriately
			// scaled, to the
			// optimisation routine first time
			// start = ArrayUtils.normalizeValues(ArrayUtils.offset(arr_i_B,
			// -ArrayUtils.avg(arr_i_B)));
			this.firstTimeMinimisation = false;
		} else {
			start = this.arr_i_S;
		}

		// apacheNelderMead.setMaxIterations(10000);
		apacheNelderMead.setConvergenceChecker(new SimpleScalarValueChecker(
				1.0e-10, 1.0e-30));
		// apacheNelderMead.setConvergenceChecker(new
		// SimpleScalarValueChecker(1.0e-2, -1.0));
		// apacheNelderMead.setMaxEvaluations(10000); //how many time function
		// is evaluated

		RealPointValuePair minValue = null;

		try {

			minValue = apacheNelderMead.optimize(minFunct, GoalType.MINIMIZE,
					start);

		} catch (@SuppressWarnings("deprecation") OptimizationException e) {
			System.err.println("Optimization Exception");
			if (this.mainContext.logger.isDebugEnabled()) {
				this.mainContext.logger
						.debug("RECO: Apache Optim Exc (Optim exc): "
								+ e.getCause());
			}
		} catch (FunctionEvaluationException e) {
			System.err.println("Functino eval Exception");

			if (this.mainContext.logger.isDebugEnabled()) {
				this.mainContext.logger
						.debug("RECO: Apache Optim Exc (Funct eval exc): "
								+ e.getCause());
			}
		}

		catch (IllegalArgumentException e) {
			System.err.println("Illegal arg exception");

			if (this.mainContext.logger.isDebugEnabled()) {
				this.mainContext.logger
						.debug("RECO: Apache Optim Exc (Illegal arg exc): "
								+ e.getCause());
			}
		}

		if (this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug("Optimisation converged on value "
					+ minValue.getValue() + " at point " + minValue.getPoint()
					+ " with ref " + minValue.getPointRef());
		}

		double[] newOpt_S = minValue.getPoint();

		// The following is to print the predicted demand for the optimal signal
		// -
		// useful for the flat demand case

		// minFunct.setPrintD(true);
		// minFunct.function(newOpt_S);
		// minFunct.setPrintD(false);

		return newOpt_S;
	}

	/**
	 * @return
	 */
	private double[] getPredictedGeneration() {
		return this.predictedGeneration;
	}

	private void updateCumulativeSaving(double savingAmount) {
		int daysSoFar = this.mainContext.getDayCount();
		if (this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger.trace("days so far: " + daysSoFar);
		}
		if (daysSoFar > (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)) {
			this.cumulativeCostSaving += savingAmount;
		}

	}

	private double[] Cavge = new double[this.ticksPerDay];
	private double[][] DeltaBm = new double[Consts.AGGREGATOR_TRAINING_PERIODE][this.ticksPerDay];
	private double[][] SBprodm = new double[Consts.AGGREGATOR_TRAINING_PERIODE][this.ticksPerDay];
	private double[] Kpos = new double[this.ticksPerDay];
	private double[][] Kposm = new double[this.ticksPerDay][this.ticksPerDay];
	private double[] Kneg = new double[this.ticksPerDay];
	private double[][] Knegm = new double[this.ticksPerDay][this.ticksPerDay];

	private double[] predictedGeneration;

	// This is the Energy Local default signal, the 1 should prohibit demand being moved to those slots
	protected double[] defaultSignal = new double[]{-0.251, -0.251, -0.251, -0.251, -0.251, -0.251, -0.275, -0.275, -0.275, -0.275, -0.275, -0.275, -0.137, -0.137, -0.137, -0.137, -0.137, -0.137, -0.137, -0.137, -0.157, -0.157, -0.157, -0.167, -0.167, -0.167, -0.167, -0.157, -0.157, -0.157, -0.157, -0.157, -0.157, -0.157, 1, 1, 1, 1, 1, 1, 1, 1, -0.24, -0.24, -0.24, -0.24, -0.275, -0.275};

	protected String aggOutputSubDir;

	/**
	 * Returns a string representing the state of this agent. This method is
	 * intended to be used for debugging purposes, and the content and format of
	 * the returned string should include the states (variables/parameters)
	 * which are important for debugging purpose. The returned string may be
	 * empty but may not be <code>null</code>.
	 * 
	 */
	@Override
	protected String paramStringReport() {
		String str = "";
		return str;
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
	private void writeOutput(String dirName, String fileName,
			boolean addInfoHeader, double[] C, double[] NC, double[] B,
			double[] D, double[] S, double[] e, double[][] k) {
		File dir = new File(dirName);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		String fullPath = dirName.concat(File.separator).concat(fileName);
		this.writeOutput(fullPath, addInfoHeader, C, NC, B, D, S, e, k);
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
	private void writeOutput(String fileName, boolean addInfoHeader,
			double[] C, double[] NC, double[] B, double[] D, double[] S,
			double[] e, double[][] k) {
		int[] ts_arr = new int[this.ticksPerDay];

		for (int i = 0; i < ts_arr.length; i++) {
			ts_arr[i] = i;
		}
		String resFileName = fileName + String.format("%04d", this.mainContext.getDayCount()) + ".csv";

		CSVWriter res = new CSVWriter(resFileName, false);

		if (addInfoHeader) {

			res.appendText("Random seed= "
					+ this.mainContext.getRandomSeedValue());
			res.appendText("Number of Prosumers= "
					+ this.mainContext.getTotalNbOfProsumers());
			res.appendText("ProfileBuildingPeriod= "
					+ Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE);
			res.appendText("TrainingPeriod= "
					+ Consts.AGGREGATOR_TRAINING_PERIODE);
			res.appendText("REEA on?= " + Consts.AGG_RECO_REEA_ON);
			res.appendText("ColdAppliances on?= " + Consts.HHPRO_HAS_COLD_APPL);
			res.appendText("WetAppliances on?= " + Consts.HHPRO_HAS_WET_APPL);
			res.appendText("ElectSpaceHeat on?= "
					+ Consts.HHPRO_HAS_ELEC_SPACE_HEAT);
			res.appendText("ElectWaterHeat on?= "
					+ Consts.HHPRO_HAS_ELEC_WATER_HEAT);
			res.appendText("");
		}

		res.appendText("Timeslots:");
		res.appendRow(ts_arr);
		if (C != null) {
			res.appendText("C:");
			res.appendRow(C);
		}
		if (C != null) {
			res.appendText("C (normalized):");
			res.appendRow(NC);
		}

		res.appendText("B:");
		res.appendRow(B);
		res.appendText("D (for end of day " + this.mainContext.getDayCount()
				+ "): ");
		res.appendRow(D);
		res.appendText("S (for end of day " + this.mainContext.getDayCount()
				+ "): ");
		res.appendRow(S);
		res.appendText("e (for end of day " + this.mainContext.getDayCount()
				+ "): ");
		res.appendRow(e);
		res.appendText("k (for end of day " + this.mainContext.getDayCount()
				+ "): ");
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
	private void errorEstimationAndAdjustment(double[] arr_i_B,
			double[] arr_hist_1D, double[] arr_i_S, double[] arr_i_e,
			double[][] arr_ij_k) {

		if (this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger.trace(" --REEA-- ");
		}

		double[] actualShift = ArrayUtils.add(arr_hist_1D,
				ArrayUtils.negate(arr_i_B));

		Matrix k = new Matrix(arr_ij_k);
		double[][] bs = new double[1][arr_i_B.length];
		bs[0] = ArrayUtils.mtimes(arr_i_B, arr_i_S);

		Matrix bs_mat = new Matrix(bs);
		bs_mat.transpose();

		double[] bse = new double[this.ticksPerDay];

		bse = ArrayUtils.mtimes(arr_i_S, arr_i_e, arr_i_B);

		double[] bsk = new double[this.ticksPerDay];
		bsk = Matrix.times(bs_mat, k).getRowCopy(0);

		double[] predictedShift = ArrayUtils.add(
				ArrayUtils.mtimes(arr_i_S, arr_i_e, arr_i_B),
				(Matrix.times(bs_mat, k).getRowCopy(0)));

		predictedShift = ArrayUtils.add(predictedShift, arr_i_B);
		double[] arr_errorEstim_R = ArrayUtils.add(predictedShift,
				ArrayUtils.negate(arr_hist_1D));
		arr_errorEstim_R = ArrayUtils.mtimes(arr_errorEstim_R,
				ArrayUtils.pow(arr_i_B, -1));

		double[] arr_multiplier = ArrayUtils.offset(
				ArrayUtils.multiply(arr_errorEstim_R, this.alpha),
				(1 - this.alpha));

		arr_i_e = ArrayUtils.mtimes(arr_i_e, arr_multiplier);

		for (int i = 0; i < arr_ij_k.length; i++) {
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
	private void costSavingCalculation(double[] arr_i_C, double[] arr_hist_1D,
			double[] arr_i_B, double[] arr_i_S, double[] arr_i_e) {

		if (this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger
					.trace(" --^costSavingCalculation-- Daycount: "
							+ this.mainContext.getDayCount() + ",Timeslot: "
							+ this.mainContext.getTimeslotOfDay()
							+ ",TickCount: " + this.mainContext.getTickCount());
		}
		double predCost = 0;
		double actualCost = 0;

		double[] day_predicted_arr_D = new double[this.ticksPerDay];

		for (int i = 0; i < this.ticksPerDay; i++) {
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

	/*
	 * public void marketPreStep() { if ( *
	 * this.mainContext.logger.isTraceEnabled()) {
	 * this.mainContext.logger.trace(" initializeMarketStep (SupplierCo) "
	 * +this.id); settlementPeriod = mainContext.getSettlementPeriod(); }
	 * 
	 * switch (settlementPeriod) {
	 * 
	 * case 13: if (mainContext.isMarketFirstDay())
	 * System.arraycopy(ArrayUtils.negate(arr_i_B), 0, arr_PN, 0,
	 * arr_i_B.length); else
	 * //System.arraycopy(ArrayUtils.negate(arr_hist_day_D), 0, arr_PN, 0,
	 * arr_hist_day_D.length);
	 * System.arraycopy(ArrayUtils.negate(this.getDayNetDemands()), 0, arr_PN,
	 * 0, this.getDayNetDemands().length);
	 * 
	 * break; } }
	 */

	@Override
	public void bizPreStep() {
		if (this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger
					.debug(" ============ SupplierCO pre_step ========= DayCount: "
							+ this.mainContext.getDayCount()
							+ ",Timeslot: "
							+ this.mainContext.getTimeslotOfDay()
							+ ",TickCount: " + this.mainContext.getTickCount());
		}
		this.timeTick = this.mainContext.getTickCount();
		this.timeslotOfDay = this.mainContext.getTimeslotOfDay();

		if (this.isAggregateDemandProfileBuildingPeriodCompleted()) { // End of
																		// history
																		// profile
																		// building
																		// period
																		// Set
																		// the
																		// Baseline
																		// demand
																		// on
																		// the
																		// first
																		// time
																		// through
																		// after
																		// building
																		// period

			if (this.mainContext.getDayCount() == Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) {
				this.arr_i_B = this.calculateBADfromHistoryArray(ArrayUtils
						.subArrayCopy(this.arr_hist_ij_D, 0,
								Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE));
			}

			if (!this.isTrainingPeriodCompleted()) { // training period, signals
														// should be send S=1
														// for 48 days
				if (this.mainContext.isBeginningOfDay(this.timeslotOfDay)) {
					this.arr_i_S = this
							.buildSignal(Consts.SIGNAL_TYPE.S_TRAINING);
					this.broadcastSignalToCustomers(this.arr_i_S,
							this.customers);
					CSVWriter tempWriter = new CSVWriter("NeuralTrainerIn.csv",
							true);
					double[] in = new double[96];
					System.arraycopy(this.arr_day_D, 0, in, 0, 48);
					System.arraycopy(
							this.mainContext.getAirTemperature(
									this.mainContext.getTickCount() - 48, 48),
							0, in, 48, 48);
					tempWriter.appendRow(in);
					tempWriter.close();
					tempWriter = new CSVWriter("NeuralTrainerOut.csv", true);
					tempWriter.appendRow(Arrays.copyOf(this.arr_i_S,
							this.arr_i_S.length));
					tempWriter.close();
				}
			} // training period completed
			else { 
				if (this.mainContext.getDayCount() == Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE
						+ Consts.AGGREGATOR_TRAINING_PERIODE) {
					
					// This is very beginning of the normal operation- both baseline
					// establishing &
					// training periods are completed
					CSVWriter tempWriter = new CSVWriter("KandSBmatOut.csv",
							false);
					tempWriter.appendText("Delta B matrix");
					tempWriter.appendCols(this.DeltaBm);
					tempWriter.appendText("SB product matrix");
					tempWriter.appendCols(this.SBprodm);
					tempWriter.close();

					tempWriter = new CSVWriter("calcProcess.csv", false);
					tempWriter.appendText("Initial values - should be empty");
					tempWriter.appendRow(this.Cavge);
					tempWriter.appendRow(this.Kneg);
					tempWriter.appendRow(this.Kpos);

					for (int deltaRow = 0; deltaRow < Consts.AGGREGATOR_TRAINING_PERIODE; deltaRow++) {
						double cavg = 0;
						double kneg = 0;
						double kpos = 0;

						double[] SBprod = ArrayUtils.colCopy(this.SBprodm,
								deltaRow);
						double[] DeltaB = ArrayUtils.colCopy(this.DeltaBm,
								deltaRow);
						tempWriter.appendText("Extract column" + deltaRow);
						tempWriter.appendRow(SBprod);
						tempWriter.appendRow(DeltaB);

						int div = 0;
						int kPosDiv = 0;
						int kNegDiv = 0;
						for (int i = 0; i < SBprod.length; i++) {
							if (SBprod[i] == 0) {
								cavg += DeltaB[i];
								div += 1;
								tempWriter.appendText("Adding " + DeltaB[i]);
							}
						}
						cavg /= div;

						// Approximate cavg by taking those values "on the axis"
						// i.e. where S
						// was zero. Obviously doesn't work for S without 0
						// values

						tempWriter.appendText("cavg = " + cavg);

						// Do regression properly

						double n = SBprod.length;
						double sumXY = ArrayUtils.sum(ArrayUtils.mtimes(SBprod,
								DeltaB));
						double sumX = ArrayUtils.sum(SBprod);
						double sumY = ArrayUtils.sum(DeltaB);
						double sumXsq = ArrayUtils.sum(ArrayUtils.mtimes(
								SBprod, SBprod));

						double m = (n * sumXY - sumX * sumY)
								/ (n * sumXsq - sumX * sumX);
						cavg = sumY / n - m * (sumX / n);

						// Regression done, find average slope for pos and neg S

						for (int i = 0; i < SBprod.length; i++) {
							if (SBprod[i] < 0) {
								kpos += (DeltaB[i] - cavg) / SBprod[i];
								kPosDiv++;
							}
							if (SBprod[i] > 0) {
								kneg += (DeltaB[i] - cavg) / SBprod[i];
								kNegDiv++;
							}

						}

						kneg /= kNegDiv;
						kpos /= kPosDiv;

						this.Cavge[deltaRow] = cavg;
						this.Kneg[deltaRow] = kneg;
						this.Kpos[deltaRow] = kpos;
						tempWriter.appendRow(this.Cavge);
						tempWriter.appendRow(this.Kneg);
						tempWriter.appendRow(this.Kpos);
					}

					tempWriter.close();

				}
				if (this.mainContext.isBeginningOfDay(this.timeslotOfDay)) {

					if (Consts.AGG_RECO_REEA_ON) {
						this.errorEstimationAndAdjustment(this.arr_i_B,
								this.getDayNetDemands(), this.arr_i_S,
								this.arr_i_e, this.arr_ij_k);
						// errorEstimationAndAdjustment(arr_i_B, arr_hist_day_D,
						// arr_i_S, arr_i_e, arr_ij_k);
					}

					this.arr_i_norm_C = ArrayUtils.normalizeValues(ArrayUtils
							.offset(this.arr_i_C, -ArrayUtils.sum(this.arr_i_C)
									/ this.arr_i_C.length));

					Parameters params = RunEnvironment.getInstance()
							.getParameters();
					switch ((Integer) params.getValue("signalMode")) {
					case 0: /* Play a zero signal to customers */
						double fixedPrice[] = new double[48];
						for (int i = 0; i < 48; i++) {
							fixedPrice[i] = 0;
						}
						this.arr_i_S = fixedPrice;
						break;
					case 1: /*
							 * Differential Price version (actual cost - average
							 * cost)
							 */
						this.arr_i_S = this.arr_i_norm_C;
						break;
					case 2: /* Smart signal version (Nelder-Mead optimisation) */
						this.arr_i_S = this.minimise_CD_Apache_Nelder_Mead(
								this.arr_i_norm_C, this.arr_i_B, this.arr_i_e,
								this.arr_ij_k, this.arr_i_S);
						break;
					case 3: /* Play back scaled version of base demand */
						this.arr_i_S = ArrayUtils.normalizeValues(ArrayUtils
								.offset(this.arr_i_B,
										-ArrayUtils.avg(this.arr_i_B)));
						break;
					case 4:
						double[] sysDem = this.mainContext
								.getSystemPriceSignalData();
						this.arr_i_S = ArrayUtils.normalizeValues(ArrayUtils
								.offset(sysDem, -ArrayUtils.avg(sysDem)));
						break;
					case 5:
						this.arr_i_S = Arrays.copyOf(this.defaultSignal,this.defaultSignal.length) ;
						break;
					case 6:
						//If there is predicted generation, incentivise in that slot
						//Proportionally if there is less generation than demand or 
						//Fully incentivised if gen exceeds demand.
						double[] gen = this.getPredictedGeneration();
						double[] exceedances = ArrayUtils.add(gen, ArrayUtils.negate(this.arr_day_D));
						this.arr_i_S = Arrays.copyOf(this.defaultSignal,this.defaultSignal.length) ;
						for (int j = 0; j < this.defaultSignal.length; j++)
						{
							double thisSlot = gen[j];
							if (thisSlot > 0)
							{
								if (exceedances[j] > 0)
								{
									this.arr_i_S[j] = -1.0; // Heavily pull demand to periods of over generation.
								}
								else
								{
									this.arr_i_S[j] = -thisSlot/this.arr_day_D[j];
								}
							}
						}
						break;
					}

					// This is where the actual broadcast of the signal to all
					// customers occurs.
					this.broadcastSignalToCustomers(this.arr_i_S,
							this.customers);

					this.writeOutput(
							this.aggOutputSubDir,
							"output2_NormalBiz_day_", false, this.arr_i_C,
							this.arr_i_norm_C, this.arr_i_B, this
									.getDayNetDemands(), this.arr_i_S,
							this.Cavge, ArrayUtils.zip(this.Kneg, this.Kpos));
				}

			} // end of begining of normal operation

		} // end of else (history profile building)
		if (this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger
					.trace(" ========== RECO: pre_step END =========== DayCount: "
							+ this.mainContext.getDayCount()
							+ ",Timeslot: "
							+ this.mainContext.getTimeslotOfDay()
							+ ",TickCount: " + this.mainContext.getTickCount());
		}
	}

	/**
	 * The scheduled method to run at the main business priority of the main
	 * step
	 * 
	 */
	@Override
	public void bizStep() {

		if (this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger
					.trace(" ++++++++++++++ SupplierCO step +++++++++++++ DayCount: "
							+ this.mainContext.getDayCount()
							+ ",Timeslot: "
							+ this.mainContext.getTimeslotOfDay()
							+ ",TickCount: " + this.mainContext.getTickCount());
		}
		this.updatePredictedGeneration();

		if (!this.isAggregateDemandProfileBuildingPeriodCompleted()) {
			this.updateAggregateDemandHistoryArray(this.customers,
					this.timeslotOfDay, this.arr_hist_ij_D);
		} else if (!this.isTrainingPeriodCompleted()) {
			this.updateAggregateDemandHistoryArray(this.customers,
					this.timeslotOfDay, this.arr_hist_ij_D);

			if (this.mainContext.isEndOfDay(this.timeslotOfDay)) {
				double[] DeltaB;
				double[] SBprod;
				double[] arr_last_training_D = ArrayUtils.rowCopy(
						this.arr_hist_ij_D, this.mainContext.getDayCount());

				/**
				 * Method to generate k values for positive and negative signal
				 * values. Based on the original Matlab code proposed by Peter
				 * Boait in prototype.
				 */
				int deltaRow = this.mainContext.getDayCount()
						- Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE;

				DeltaB = ArrayUtils.add(arr_last_training_D,
						ArrayUtils.negate(this.arr_i_B));
				SBprod = ArrayUtils.mtimes(this.arr_i_S, this.arr_i_B);
				this.DeltaBm[deltaRow] = DeltaB.clone();
				this.SBprodm[deltaRow] = SBprod.clone();

				if (this.mainContext.getDayCount() > ((Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE + Consts.AGGREGATOR_TRAINING_PERIODE)) - 2) {
					this.writeOutput("output1_TrainingPhase_day_", true,
							this.arr_i_C, this.arr_i_norm_C, this.arr_i_B,
							arr_last_training_D, this.arr_i_S, this.arr_i_e,
							this.arr_ij_k);
				}
			}
		}

		this.calculateAndSetNetDemand(this.customers);
		if (this.isTrainingPeriodCompleted()) {
			this.updateTotalCO2Avoided();
		}
		if (this.mainContext.isEndOfDay(this.timeslotOfDay)) {
			this.costSavingCalculation(this.arr_i_C, this.getDayNetDemands(),
					this.arr_i_B, this.arr_i_S, this.arr_i_e);
		}

	}

	/**
	 * This function updates the predicted generation for the next day for the aggregator
	 * 
	 * If any connected prosumers predict generation, this will be used.  Otherwise,
	 * defaults to the predicted generation tomorrow will be
	 * the same as today at each timestep.
	 */
	private void updatePredictedGeneration() {
		
		//Most basic version - tomorrow will be same as today
		double  tot = 0;
		boolean real_predictions = false;
		for (ProsumerAgent p : customers)
		{
//			if (p instanceof HouseholdProsumer)
//			{
//				HouseholdProsumer hh = (HouseholdProsumer) p;
//				tot += hh.currentGeneration();
//			}
			
			double pred = p.predictedGeneration();
			if (pred >= 0)
			{
				//If *any* prosumers give real predictions, use these in preference to guess
				real_predictions = true;
				tot += pred;
			}
			
		}
		
		if (!real_predictions)
		{
			//Default to tomorrow same as today
			tot = this.getGeneration();
		}
		
		this.predictedGeneration[this.mainContext.getTimeslotOfDay()] = tot;
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
	public SupplierCoAdvancedModel(CascadeContext context,
			MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type,
			double maxDem, double minDem, double[] baseDemand) {

		// super(context, mb, cat, type, maxDem, minDem, baseDemand);
		super(context);

		this.ticksPerDay = context.getNbOfTickPerDay();

		if (baseDemand.length % this.ticksPerDay != 0) {
			System.err
					.print("SupplierCoAdvancedModel: Error/Warning message from "
							+ this.toString()
							+ ": BaseDemand array imported to aggregator not a whole number of days");
			System.err
					.println("SupplierCoAdvancedModel:  May cause unexpected behaviour - unless you intend to repeat the signal within a day");
		}
		this.priceSignal = new double[baseDemand.length];
		this.overallSystemDemand = new double[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.overallSystemDemand, 0,
				this.overallSystemDemand.length);

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
		this.arr_hist_ij_D = new double[Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE
				+ Consts.AGGREGATOR_TRAINING_PERIODE][this.ticksPerDay];

		this.arr_i_C_all = ArrayUtils.normalizeValues(
				ArrayUtils.pow2(baseDemand), 100); // all
													// costs
													// (equivalent
		this.predictedGeneration = new double[this.ticksPerDay]; // (equivalent
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

	public SupplierCoAdvancedModel(CascadeContext context) {

		// super(context,null,null,null,0);
		super(context);

		// Usually a supplier company will be small demand - this can be
		// overridden and is effectively a default here.
		// TODO: should we default like this?
		this.category = Consts.BMU_CATEGORY.DEM_S;
		this.type = Consts.BMU_TYPE.DEM_SMALL;

		this.ticksPerDay = context.getNbOfTickPerDay();
		double[] baseDemand = new double[this.ticksPerDay];
		if (baseDemand.length % this.ticksPerDay != 0) {
			System.err
					.print("SupplierCoAdvancedModel: Error/Warning message from "
							+ this.toString()
							+ ": BaseDemand array imported to aggregator not a whole number of days");
			System.err
					.println("SupplierCoAdvancedModel:  May cause unexpected behaviour - unless you intend to repeat the signal within a day");
		}
		this.priceSignal = new double[baseDemand.length];
		this.overallSystemDemand = new double[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.overallSystemDemand, 0,
				this.overallSystemDemand.length);

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
		this.arr_hist_ij_D = new double[Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE
				+ Consts.AGGREGATOR_TRAINING_PERIODE][this.ticksPerDay];

		this.arr_i_C_all = ArrayUtils.normalizeValues(
				ArrayUtils.pow2(baseDemand), 100); // all
													// costs
		this.predictedGeneration = new double[this.ticksPerDay]; // (equivalent
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
		
		this.aggOutputSubDir = "output".concat(File.separator)
				.concat("Seed"
						.concat(Integer.toString(this.mainContext
								.getRandomSeedValue()))
						.concat("GasFrac")
						.concat(Double.toString(this.mainContext
								.getGasPercentage())));
		// +++++++++++++++++++++++++++++++++++++++++++
	}
}
