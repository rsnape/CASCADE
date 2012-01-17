/**
 * 
 */
package uk.ac.cranfield.base.neural;

import java.util.ArrayList;

/**
 * @author grahamfletcher
 *
 */
public class Network {
	public ArrayList<Layer> layers = new ArrayList<Layer>();
	public Layer inputLayer;
	public Layer outputLayer;
	
	public void randomize()
	{
		for(Layer l : layers)
			l.randomize();
	}
	
	public double train()
	{
		double v = 0.0;
		for(Layer l : layers)
			v += l.train();
		return v;
	}
	
	public void reset()
	{
		for(Layer l : layers)
			l.reset();
	}

	public double fire()
	{
		return outputLayer.fire();
	}
	
	public double feedBack()
	{
		return inputLayer.feedBack();
	}
	
	public String toString()
	{
		String s = "";
		
		for(Layer l : layers)
			s += l;
		return s;
			
	}
	
	
}
