/**
 * 
 */
package uk.ac.cranfield.base.neural;

import repast.simphony.random.RandomHelper;

/**
 * @author grahamfletcher
 *
 */
public class Connection {
	public Neuron input, output;
	public double w =0.0;
	public Connection (Neuron input, Neuron output, double w){
		this.input = input;
		this.output = output;
		this.w = w;
	}
	
	public double value()
	{
		return input.fire() * w;
	}
	
	public double feedback()
	{
		return output.feedBack() * w;
	}
	
	public void randomise()
	{
		w = RandomHelper.nextDoubleFromTo(-0.1, 0.1);
	}
	
	public double train()
	{
		if((output.mode > 1) && (input.mode > 0))
		{
			double c = Parameters.learningSpeed  * output.dvde * input.outputValue;
			w -= c;
			return Math.abs(c);
		}
		return 0.0;
	}
	

}
