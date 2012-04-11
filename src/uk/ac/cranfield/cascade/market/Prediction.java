package uk.ac.cranfield.cascade.market;

import java.util.ArrayList;
/**
 * 
 */

/**
 * @author grahamfletcher
 *
 */
public class Prediction {
	public int modelTick;
	public ValueSD prediction;			//supply
	public ValueSD prediction2; 		//demand
	public ArrayList<MarginalPriceCurveValues> prediction3;			//marginal price pairs
	public ValueSD prediction4;			//fixed cost of generation (per tick)
	public ValueSD prediction5;			//ramp rate per half hour (assume symmetry in ramp up and ramp down)
	public ValueSD prediction6;			//start up cost 
	public ValueSD prediction7;			//max capacity of plant(s)
	public ValueSD prediction8;			//min capacity of plant(s)
	
	public Prediction(int modelTick, double value, double sd) {
		this.modelTick = modelTick;
		this.prediction = new ValueSD(value, sd);
		this.prediction2 = null;
		this.prediction3 = null;
		this.prediction4 = null;
		this.prediction5 = null;
		this.prediction6 = null;
		this.prediction7 = null;
		this.prediction8 = null;
		
	}
	
	public Prediction(int modelTick, double value, double sd,double value2, double sd2) {
		this.modelTick = modelTick;
		this.prediction = new ValueSD(value, sd);		
		this.prediction2 = new ValueSD(value2, sd2);
		this.prediction3 = null;
		this.prediction4 = null;
		this.prediction5 = null;
		this.prediction6 = null;
		this.prediction7 = null;
		this.prediction8 = null;
	}
	
	public Prediction(int modelTick, double[][] priceCurve, double value, double sd, double value2, double sd2, double value3, double sd3, double value4, double sd4, double value5, double sd5) {
		this.modelTick = modelTick;
		this.prediction = null;		
		this.prediction2 = null;
		for (int i = 0;i<priceCurve.length;i++){
			this.prediction3.add(new MarginalPriceCurveValues((int)priceCurve[i][0], priceCurve[i][1]));
		}
		this.prediction4 = new ValueSD(value, sd);
		this.prediction5 = new ValueSD(value2, sd2);
		this.prediction6 = new ValueSD(value3, sd3);
		this.prediction7 = new ValueSD(value4, sd4);
		this.prediction8 = new ValueSD(value5, sd5);
	}

}

