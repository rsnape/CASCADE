package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.util.Arrays;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * 
 * The <em>BOD</em> class encapsulates a data structure which 
 * defines Bid and Offer Data used in the UK balancing mechanism electricity market. 
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/04
 * 
 */

public class BOD {
	
	private int ownerID;
	public boolean isAccepted; // set/get possible for SO; get for SC
	public double submittedBO;  //get possible for SO; get for SC
	private double level;   // set/get possible for SO; get for SC
		
	private int sp; //setttlement period

	private int pairID;
		
	private double[] arr_BO; //bidOrOffer
	private double[] arr_propensities; 
	private double[] arr_probabilities;
	//private double marginPC;

	//private double offer; //offer > bid (always) 
	//private double bid;
	
	public int getOwnerID() {
		return this.ownerID;
	}
	
	public int getSP() {
		return this.sp;
	}
	
	public double getLevel() {
		return this.level;
	}
	
	public void setLevel(double l) { // accessible to SO
		this.level =l;
	}
	
	public int getPairID() {
		return this.pairID;
	}
	
	double[] getBOArray() {
		return arr_BO;
	}
	
	double[] getPropensityArray() {
		return this.arr_propensities;
	}
	
	void setPropensityArray(double[] pa) {
		this.arr_propensities = pa;
	}
	
	double[] getProbabilityArray() {
		return this.arr_probabilities;
	}
	
	void setProbabilityArray(double[] pa) {
		this.arr_probabilities = pa;
	}
	
	public double getSubmittedBO() {
		return this.submittedBO;
	}
	
	public void setSubmittedBO(double sBO) {
		this.submittedBO = sBO;
	}
	
	public String toString() {
		
		String toString = "BOD: ownerID: " +ownerID+ ", isAccepted: "+isAccepted +
		", pairID: "+ pairID+ ", sp: " +sp +", level: "+level + ", submittedBO (price): "+ this.submittedBO+
		", probArray: " + Arrays.toString(this.arr_probabilities);
		
		return toString;
	}
	
	/*public Double getOfferInDouble() {
		return new Double(offer);
	}*/
	
	public BOD(int oID, int settlPeriod, int pID, double levelAmount, double[] bidOrOfferArray, double[] propensityArray) {
		this.isAccepted=false;
		this.ownerID=oID;
		this.sp=settlPeriod;
		this.pairID= pID;
		this.level = levelAmount;
		
		arr_BO = new double[ASTEMConsts.BMU_BO_NUM_OF_CHOICE];
		arr_propensities = new double[ASTEMConsts.BMU_BO_NUM_OF_CHOICE];
		arr_probabilities = new double[ASTEMConsts.BMU_BO_NUM_OF_CHOICE];
		
		System.arraycopy(bidOrOfferArray, 0, arr_BO,0, bidOrOfferArray.length );
		System.arraycopy(propensityArray, 0, arr_propensities,0, propensityArray.length);
		
		double sumOfPropensities = ArrayUtils.sum(arr_propensities);
		
		for (int i=0; i<arr_propensities.length; i++) {
			this.arr_probabilities[i] = arr_propensities[i]/sumOfPropensities;
		}
		
		int ind = RandomHelper.getUniform().nextIntFromTo(0, 2); //TODO: JRS : Why pick randomly from first 3, rather than from all choices? JRS
		this.submittedBO = this.arr_BO[ind];
		
	}

}
