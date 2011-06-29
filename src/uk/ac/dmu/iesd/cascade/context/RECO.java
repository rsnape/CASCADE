/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import static repast.simphony.essentials.RepastEssentials.FindNetwork;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import bsh.This;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import flanagan.*;
import flanagan.math.*;
import flanagan.analysis.*;
import flanagan.math.Matrix;

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
	 * Constructs a RECO agent with the context in which is created and its
	 * base demand.
	 * @param context the context in which this agent is situated
	 * @param baseDemand an array containing the base demand  
	 */
	public RECO(CascadeContext context, float[] baseDemand) {

		super(context);
		//System.out.println("RECO created ");
		this.ticksPerDay = context.getTickPerDay();
		//this.contextName = myContext;
		this.overallSystemDemandLength = baseDemand.length;
		this.priceSignalLength = baseDemand.length;
		//System.out.println("RECO ticksPerDay "+ ticksPerDay);
	
		if (overallSystemDemandLength % ticksPerDay != 0)
		{
			System.err.print("Error/Warning message from "+this.toString()+": BaseDemand array imported to aggregator not a whole number of days");
			System.err.println(" May cause unexpected behaviour - unless you intend to repeat the signal within a day");
		}
		this.priceSignal = new float [priceSignalLength];
		this.overallSystemDemand = new float [overallSystemDemandLength];
		System.arraycopy(baseDemand, 0, this.overallSystemDemand, 0, overallSystemDemandLength);
		//Start initially with a flat price signal of 12.5p per kWh
		Arrays.fill(priceSignal,125f);
		
		//Very basic configuration of predicted customer demand as 
		// a Conssant.  We could be more sophisticated than this or 
		// possibly this gives us an aspirational target...
		this.predictedCustomerDemand = new float[ticksPerDay];
		//Put in a constant predicted demand
		//Arrays.fill(this.predictedCustomerDemand, 5);
		//Or - put in a variable one
		for (int j = 0; j < ticksPerDay; j++)
		{
			this.predictedCustomerDemand[j] = baseDemand[j] / 7000;
		}
		this.predictedCustomerDemandLength = ticksPerDay;
		
		///+++++++++++++++++++++++++++++++++++++++
		this.arr_i_B = new float [ticksPerDay];
		this.arr_i_e = new float [ticksPerDay];
		this.arr_i_S = new float [ticksPerDay];
		this.arr_i_C = new float [ticksPerDay];
		this.arr_ij_k = new float [ticksPerDay][ticksPerDay];
		this.hist_arr_ij_D = new float [Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE+Consts.AGGREGATOR_TRAINING_PERIODE][ticksPerDay];

		//this.arr_i_B = baseDemand; 

		//+++++++++++++++++++++++++++++++++++++++++++
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
	 * This method is used to check whether the 'profile building' period has completed.
	 * the 'profile building' period (the initial part of the training period, usually 4-7 days) is 
	 * currently determined by a rarely changed variable in the Consts class
	 * @see Consts#AGGREGATOR_PROFILE_BUILDING_PERIODE
	 * @return true if the profile building period is completed, false otherwise 
	 */
	private boolean isAggregateDemandProfileBuildingPeriodCompleted() {
		boolean isEndOfProfilBuilding = true;
	    int daysSoFar = getCountDay();
	    //System.out.println("BaslineB days so far: "+daysSoFar);
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
	    int daysSoFar = getCountDay();
	    //System.out.println("days so far: "+daysSoFar);
		if (daysSoFar < (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE))
			isEndOfTraining = false;
		return isEndOfTraining;		
	}
	
	/**
	 * This method returns the elapse of time in number of days.
	 * It depends on how a day is initially defined. If a day is divided up to 48 timeslots, 
	 * then the second day starts at timeslot 49. 
	 * However, in order to have it usefully workable with arrays, the first day is returned as 0, second day as 1 and so forth.
	 * @return the elapsed time in terms of number of day, starting from 0
	 * TODO: this method can be a cascade utility method, which means as a public method it can place in
	 * classes such as environment/CascadeContext.
	 */
	private int getCountDay() {
		int overallElapsedTime = (int) RepastEssentials.GetTickCount();
	    int daysSoFar = overallElapsedTime/this.ticksPerDay;
		return daysSoFar;	
	}
	
	/**
	 * This method is used to update baseline aggregate demand (BAD or simply B) 
	 * history matrix by obtaining each customers/prosumers' demand and adding them up and putting it in the right array cell
	 * (i.e. right day [row], timeslot [column]) 
	 * @param customersList the List of cusstumersList (of <code>ProsumerAgent</code> type)
	 * @param timeOfDay the current timeslot (for a day divided to 48 timeslot, a value between 0 to 47)
	 * @param hist_B_arr a 2D array for keeping baseline aggregate demand values for each timeslot of the day(column) and for different days (row)  
	 */
	private void updateAggregateDemandHistoryArray(List<ProsumerAgent> customersList,int timeOfDay, float[][] hist_B_arr) {
		float sumDemand = 0;
		//System.out.println(" customersList size "+customersList.size());
		//System.out.println(" RECO is calling prosumer getND at ticktime "+ RepastEssentials.GetTickCount());

		for (ProsumerAgent a : customersList) {
			sumDemand = sumDemand + a.getNetDemand();
			//System.out.println("enter loop: agentND "+a.getNetDemand());
		}	
	    int dayCount = getCountDay();
	   //System.out.println("sumDemand: "+sumDemand +" timeOfDay: "+timeOfDay);
		//this.hist_B_ij_arr[dayCount][timeOfDay]=sumDemand;
		hist_B_arr[dayCount][timeOfDay]=sumDemand;
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
	 * This method broadcasts a signal (of different type) to a list of provided broadcastees (e.g. Prosumers)
	 * at a given timeslot 
	 * @param signalType the type of the signal
	 * @param broadcasteesList the list of broadcastees
	 * @param timeslot the time of day (usually a day is divided to 48 slots)  
	 * @return true if signal has been sent and received successfully by receiver, false otherwise 
	 */
	private float[] buildSignal(Consts.SIGNAL_TYPE signalType, int timeslot) {
		
		float[] sArr = new float[this.ticksPerDay];
		
		switch (signalType) { 
		case S: 
			break;
			
		case S_TRAINING: 
			
			sArr[timeslot] = 1;
			if (timeslot > 0) {
				for (int i = 0; i < timeslot; i++) {
					sArr[i] = (-1f/(this.ticksPerDay-1));
				}
			}
			if (timeslot < this.ticksPerDay) {
				for (int i = timeslot+1; i < sArr.length; i++) {
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
				//System.out.println("sendSignalType_S, send signal to " + a.toString());
				isSignalSentSuccessfully = agent.receiveSignal(arr_i_S, arr_i_S.length, Consts.SIGNAL_TYPE.S);
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
			//System.out.println(ArrayUtils.getPrintableOutputForFloatArray(signalArr));
			//System.out.println(ArrayUtils.isSumEqualZero(signalArr));
			isSignalSentSuccessfully = sendSignal(signalType, arr_i_S, broadcasteesList, timeslot);
			break;

		default:  //
			break;
		}

		return isSignalSentSuccessfully;
	}
	

	/**
	 * This method calculates elasticity factors (e) for each timeslot during a day (usally 48)
	 * by accepting a 48 days D values for obtaining during training phase (by sending s=1 signals),
	 * and a average baseline aggregate demand array built during profile building period.
	 * @param hist_arr2D_D a 2D array containing aggregate demand (D) values obtaining during traning periods (usually 48 days)
	 * @param arr_B an array containing average baseline aggregate demand values calcualted after profile building period (usually 7 days)
	 * @return float array of elasticity factors (e) 
	 * @see #calculate_e(float[], float[], float, float)

	 */
	private float[] calculateElasticityFactors_e(float[][] training_arr2D_D, float[] arr_B) {	
		
		float[] arr_D;
		float[] arr_e = new float[this.ticksPerDay];
		for (int i = 0; i< this.ticksPerDay; i++) {
			arr_D = ArrayUtils.rowCopy(training_arr2D_D, i);
			arr_e[i]= calculate_e(arr_D, arr_B, 1f, arr_B[i]);
		}
		return arr_e;
		
	}
	
	

	/**
	 * This method defines how this object behaves (what it does)
	 * at at a given scheduled time throughout the simulation. 
	 */
	//@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public void step() {

		// Define the return value variable.
		//boolean returnValue = true;

		// Note the simulation time if needed.
		double time = RepastEssentials.GetTickCount();
		int timeOfDay = (int) (time % ticksPerDay);
		
		//System.out.println(timeOfDay+ " RECO step called at ticktime "+ RepastEssentials.GetTickCount());

		
		List<ProsumerAgent> customers = getCustomersList();
		
		/*List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
		//List<RepastEdge> linkages = RepastEssentials.GetOutEdges("CascadeContextMain/economicNetwork", this); //Ideally this must be avoided, changing the context name, will create a bug difficult to find
		Network economicNet = this.mainContext.getEconomicNetwork();
		Iterable<RepastEdge> iter = economicNet.getEdges();


		if(Consts.DEBUG) {
			//System.out.println("Agent " + agentID + " has " + linkages.size() + "links in economic network");

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
		} */

		/*
		for (RepastEdge edge : linkages) {
			Object linkSource = edge.getTarget();
			if (linkSource instanceof ProsumerAgent){
				customers.add((ProsumerAgent) linkSource);    		
			}
			else
			{
				throw (new WrongCustomerTypeException(linkSource));
			}
		} */

		float sumDemand = 0;
		//float sum_e =0;
		for (ProsumerAgent a : customers)
		{
			sumDemand = sumDemand + a.getNetDemand();
			//sum_e = sum_e+a.getElasticityFactor();
		}

		setNetDemand(sumDemand);
		//Set the predicted demand for next day to the sum of the demand at this time today.
		//TODO: This is naive

		predictedCustomerDemand[timeOfDay] = sumDemand;
		

		//TODO I've started too complicated here - first put out flat prices (as per today), then E7, then stepped ToU, then a real dynamic one like this...

		//setPriceSignalFlatRate(125f);
		//setPriceSignalEconomySeven(125f, 48f);

		// Co-efficients estimated from Figure 4 in Roscoe and Ault
		setPriceSignalRoscoeAndAult(0.0006f, 12f, 40f);

		//Here, we simply broadcast the electricity value signal each midnight
		if (timeOfDay == 0) {

			int broadcastLength; // we may choose to broadcast a subset of the price signal, or a repeated pattern
			broadcastLength = priceSignal.length; // but in this case we choose not to

			broadcastDemandSignal(customers, time, broadcastLength);
		}   
		
		
		//****************** Babak Test ******************
		float [][]arr = new float[5][4];
		for (int row = 0; row < arr.length; row++) {
			for (int col = 0; col < arr[row].length; col++) {
				arr[row][col]= row*1f;
			}
		}
			
		//System.out.println(ArrayUtils.getPrintableOutputForFloatArray(ArrayUtils.avgCols2DFloatArray(arr)));
			
		List<ProsumerAgent> customers2 = getCustomersList();
		
	    if (!isAggregateDemandProfileBuildingPeriodCompleted()) { // history profile building period
	    	//updateBaselineAggregateDemandHistory(customers2, timeOfDay, histProfileBuilding_B_ij_arr);
	    	updateAggregateDemandHistoryArray(customers2, timeOfDay, hist_arr_ij_D); 
	    }
	    else { //End of history profile building period 
	    	
	    	//histAvg_B_i_arr = ArrayUtils.avgCols2DFloatArray(hist_B_ij_arr);
	    	//System.out.println(ArrayUtils.getPrintableOutputFor2DFloatArray(this.hist_B_ij_arr));
	    	//System.out.println(ArrayUtils.getPrintableOutputFor2DFloatArray(ArrayUtils.getSubArrayCopy(hist_B_ij_arr,0,Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)));
	    	//histAvg_B_i_arr = calculateAverageBADfromHistory(histProfileBuilding_B_ij_arr);
	    	//histAvg_B_i_arr = calculateAverageBADfromHistory(hist_B_ij_arr);
	    	//System.out.println(ArrayUtils.getPrintableOutputForFloatArray(histAvg_B_i_arr));
	    	//System.out.println("=====");
	    	//float [][] buildingProfilePeriod = ArrayUtils.getSubArrayCopy(hist_B_ij_arr, Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE);
	    	arr_i_B = calculateBADfromHistoryArray(ArrayUtils.subArrayCopy(hist_arr_ij_D,0,Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE));
	    	//System.out.println(ArrayUtils.getPrintableOutputForFloatArray(histAvg_B_i_arr));


	    	if (!isTrainingPeriodCompleted()) {  //training period 
	    		//signals should be send S=1 for 48 days
	    		broadcastSignal(Consts.SIGNAL_TYPE.S_TRAINING, customers2, timeOfDay);
		    	updateAggregateDemandHistoryArray(customers2, timeOfDay, hist_arr_ij_D);
		    	
	    	}
	    	else { //End of training period 
	    		float [][] trainingPeriodBAD = ArrayUtils.subArrayCopy(hist_arr_ij_D, Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE, hist_arr_ij_D.length);
	    		arr_i_e = calculateElasticityFactors_e(trainingPeriodBAD,arr_i_B);
	    		//System.out.println(ArrayUtils.getPrintableOutputForFloatArray(arr_i_e));	

	    		
	    		//System.out.println(ArrayUtils.getPrintableOutputFor2DFloatArray(ArrayUtils.getSubArrayCopy(hist_B_ij_arr,0,Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)));	
	    		//System.out.println(ArrayUtils.getPrintableOutputFor2DFloatArray(hist_B_ij_arr));	
	    		//System.out.println(ArrayUtils.getPrintableOutputFor2DFloatArray(trainingPeriodBAD));	
	    	
	    		//Calculate k and e
	    		
	    	}

	    }
	
		
	
		
	
		
		
		//System.out.println("predictTimeslotDemand("+(timeOfDay+1)+ "): "+ calcualte_PredictedTimeslotDemand_Di(timeOfDay));
		

		//----- Babak Network test ----------------------------------------------
	/*	Network costumerNetwork = FindNetwork("BabakTestNetwork");
		
		costumerNetwork.getEdges(this);
		//System.out.println("costumerNework: "+ costumerNetwork.getName());
		Iterable costumersIter = costumerNetwork.getEdges(this);
		
		//System.out.println("costumerIter: "+ costumersIter.toString());
		for (Object thisConn: costumersIter)
		{
			RepastEdge linkEdge = ((RepastEdge) thisConn);
			HouseholdProsumer hhPro = (HouseholdProsumer) linkEdge.getTarget();
			System.out.println(this.toString()+ " costumer is: "+ hhPro.toString()); 
			if (hhPro.getAgentName().matches("HH-Pro1")) 
				costumerNetwork.removeEdge(linkEdge);
			
		}
		System.out.println("==================="); */
		// -- End of test --------------------------------------------------------
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
			int numCopies = (int) Math.floor((broadcastLength - 1) / priceSignalLength);
			int startIndex = (int) time % priceSignalLength;

			System.arraycopy(priceSignal,startIndex,broadcastSignal,0,priceSignalLength - startIndex);
			for (int i = 1; i <= numCopies; i++)
			{
				int addIndex = (priceSignalLength - startIndex) * i;
				System.arraycopy(priceSignal, 0, broadcastSignal, addIndex, priceSignalLength);
			}

			if (broadcastLength > (((numCopies + 1) * priceSignalLength) - startIndex))
			{
				System.arraycopy(priceSignal, 0, broadcastSignal, ((numCopies + 1) * priceSignalLength) - startIndex, broadcastLength - (((numCopies + 1) * priceSignalLength) - startIndex));
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


}
