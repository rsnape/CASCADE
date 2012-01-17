/**
 * 
 */
package uk.ac.cranfield.base.neural;

import java.util.ArrayList;

/**
 * @author grahamfletcher
 *
 */
public class Layer {
	public ArrayList<Neuron> neurons = new ArrayList<Neuron>();
	
	public Layer(int count)
	{
		while(count-- > 0)
			neurons.add(new Neuron());
	}
	
	public void connectInput(Layer input)
	{
		for(Neuron o:neurons)
			for(Neuron i: input.neurons)
				o.connectInput(i);
	}
	
	public void setValues(ArrayList<Double> v)
	{
		v = (ArrayList<Double>)v.clone();
		if(v.size() == neurons.size())
			for(Neuron n: neurons)
				n.setValue(v.remove(0));
	}
	
	public double  setError(ArrayList<Double> v)
	{
		double er = 0.0;
		v = (ArrayList<Double>)v.clone();
		if(v.size() == neurons.size())
			for(Neuron n: neurons)
				er +=n.setError(v.remove(0));
		return er;
	}
	
	public void randomize()
	{
		for(Neuron n : neurons)
			n.randomise();
	}
	
	public double fire()
	{
		double v = 0.0;
		for(Neuron n : neurons)
			v += n.fire();
		return v;
	}
	
	public void reset()
	{
		for(Neuron n : neurons)
			n.reset();
	}
	public double feedBack()
	{
		double v = 0.0;
		for(Neuron n:neurons)
			v+= n.feedBack();
		return v;
	}
	
	public String toString()
	{
		String s = "";
		for(Neuron n : neurons)
		{
			s+= "["+n.ID+".."+n.outputValue+"]";
			for(Connection nI: n.inputs)
				s+="."+nI.input.ID+"("+nI.w+")";
			s+="\n";
		}
		return s;
	}
	
	public double train()
	{
		double v = 0.0;
		for(Neuron n:neurons)
		   v+= n.train();
	    return v;
	}
}
