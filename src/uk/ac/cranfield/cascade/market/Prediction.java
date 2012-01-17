package uk.ac.cranfield.cascade.market;
/**
 * 
 */

/**
 * @author grahamfletcher
 *
 */
public class Prediction {
	public int modelTick;
	public ValueSD prediction;
	public ValueSD prediction2; 
	
	public Prediction(int modelTick, double value, double sd) {
		this.modelTick = modelTick;
		this.prediction = new ValueSD(value, sd);
		this.prediction2 = null;
	}
	
	public Prediction(int modelTick, double value, double sd,double value2, double sd2) {
		this.modelTick = modelTick;
		this.prediction = new ValueSD(value, sd);
		this.prediction2 = new ValueSD(value2, sd2);
	}

}

