/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

/**
 * @author jsnape
 *
 */
public class PowerPrediction {


	/**
	 * private variable indicating the time at which this price is valid
	 * currently implemented as an integer representing number of ticks
	 * from the model run start.
	 */
	private int timeStamp;
	
	/**
	 * The mean of the predicted power at the timestamp of this prediction
	 */
	private double meanPredictedPower;
	
	/**
	 * the probabilistic deviation from the mean prediction at +0.5 * Standard deviation
	 */
	private double plusHalfSigmaPrediction;
	
	/**
	 * the probabilistic deviation from the mean prediction at +1 * Standard deviation
	 */
	private double plusOneSigmaPrediction;
	
	/**
	 * the probabilistic deviation from the mean prediction at +2 * Standard deviation
	 */
	private double plusTwoSigmaPrediction;
	
	/**
	 * the probabilistic deviation from the mean prediction at +3 * Standard deviation
	 */
	private double plusThreeSigmaPrediction;
	
	/**
	 * the probabilistic deviation from the mean prediction at -0.5 * Standard deviation
	 */
	private double minusHalfSigmaPrediction;
	
	/**
	 * the probabilistic deviation from the mean prediction at -1 * Standard deviation
	 */
	private double minusOneSigmaPrediction;
	
	/**
	 * the probabilistic deviation from the mean prediction at -2 * Standard deviation
	 */
	private double minusTwoSigmaPrediction;
	
	/**
	 * the probabilistic deviation from the mean prediction at -3 * Standard deviation
	 */
	private double minusThreeSigmaPrediction;
	
	/**
	 * @param meanPredictedPower the meanPredictedPower to set
	 */
	public void setMeanPredictedPower(double meanPredictedPower) {
		this.meanPredictedPower = meanPredictedPower;
	}

	/**
	 * @return the meanPredictedPower
	 */
	public double getMeanPredictedPower() {
		return meanPredictedPower;
	}

	/**
	 * @param plusHalfSigmaPrediction the plusHalfSigmaPrediction to set
	 */
	public void setPlusHalfSigmaPrediction(double plusHalfSigmaPrediction) {
		this.plusHalfSigmaPrediction = plusHalfSigmaPrediction;
	}

	/**
	 * @return the plusHalfSigmaPrediction
	 */
	public double getPlusHalfSigmaPrediction() {
		return plusHalfSigmaPrediction;
	}

	/**
	 * @param plusOneSigmaPrediction the plusOneSigmaPrediction to set
	 */
	public void setPlusOneSigmaPrediction(double plusOneSigmaPrediction) {
		this.plusOneSigmaPrediction = plusOneSigmaPrediction;
	}

	/**
	 * @return the plusOneSigmaPrediction
	 */
	public double getPlusOneSigmaPrediction() {
		return plusOneSigmaPrediction;
	}

	/**
	 * @param plusTwoSigmaPrediction the plusTwoSigmaPrediction to set
	 */
	public void setPlusTwoSigmaPrediction(double plusTwoSigmaPrediction) {
		this.plusTwoSigmaPrediction = plusTwoSigmaPrediction;
	}

	/**
	 * @return the plusTwoSigmaPrediction
	 */
	public double getPlusTwoSigmaPrediction() {
		return plusTwoSigmaPrediction;
	}

	/**
	 * @param plusThreeSigmaPrediction the plusThreeSigmaPrediction to set
	 */
	public void setPlusThreeSigmaPrediction(double plusThreeSigmaPrediction) {
		this.plusThreeSigmaPrediction = plusThreeSigmaPrediction;
	}

	/**
	 * @return the plusThreeSigmaPrediction
	 */
	public double getPlusThreeSigmaPrediction() {
		return plusThreeSigmaPrediction;
	}

	/**
	 * @param minusHalfSigmaPrediction the minusHalfSigmaPrediction to set
	 */
	public void setMinusHalfSigmaPrediction(double minusHalfSigmaPrediction) {
		this.minusHalfSigmaPrediction = minusHalfSigmaPrediction;
	}

	/**
	 * @return the minusHalfSigmaPrediction
	 */
	public double getMinusHalfSigmaPrediction() {
		return minusHalfSigmaPrediction;
	}

	/**
	 * @param minusOneSigmaPrediction the minusOneSigmaPrediction to set
	 */
	public void setMinusOneSigmaPrediction(double minusOneSigmaPrediction) {
		this.minusOneSigmaPrediction = minusOneSigmaPrediction;
	}

	/**
	 * @return the minusOneSigmaPrediction
	 */
	public double getMinusOneSigmaPrediction() {
		return minusOneSigmaPrediction;
	}

	/**
	 * @param minusTwoSigmaPrediction the minusTwoSigmaPrediction to set
	 */
	public void setMinusTwoSigmaPrediction(double minusTwoSigmaPrediction) {
		this.minusTwoSigmaPrediction = minusTwoSigmaPrediction;
	}

	/**
	 * @return the minusTwoSigmaPrediction
	 */
	public double getMinusTwoSigmaPrediction() {
		return minusTwoSigmaPrediction;
	}

	/**
	 * @param minusThreeSigmaPrediction the minusThreeSigmaPrediction to set
	 */
	public void setMinusThreeSigmaPrediction(double minusThreeSigmaPrediction) {
		this.minusThreeSigmaPrediction = minusThreeSigmaPrediction;
	}

	/**
	 * @return the minusThreeSigmaPrediction
	 */
	public double getMinusThreeSigmaPrediction() {
		return minusThreeSigmaPrediction;
	}
	
	/**
	 * @return the timeStamp
	 */
	public int getTimeStamp() {
		return timeStamp;
	}


	/**
	 * construct a PowerPrice for the given time.
	 * 
	 * @param timeStamp
	 */
	public PowerPrediction(int timeStamp) {
		super();
		this.timeStamp = timeStamp;
	}
}