/**
 * 
 */
package uk.ac.cranfield.base.neural;

import java.util.ArrayList;

import repast.simphony.random.RandomHelper;

/**
 * @author grahamfletcher
 *
 */
public class Neuron {
	public ArrayList<Connection> inputs = new ArrayList<Connection>();
	public ArrayList<Connection> outputs = new ArrayList<Connection>();
	public int mode = 0;
	private double inputSum = 0.0;
	public double outputValue = 0.0;
	public double bias = 0.0;
	public double temp = 1.0;
	public double error = 0.0;
	public double dvde = 0.0;
	public int ID = _id++;
	private static int _id = 0;
	
	
	public void connectInput(Neuron n)
	{
		Connection c = new Connection(n,this,0.0);
		inputs.add(c);
		n.outputs.add(c);
	}
		
	public void reset()
	{
		//if (Consts.DEBUG) System.out.print("r");
		mode = 0;
	}
	
	public double fire()
	{   
		if(mode == 0)
		{
			mode = 1;
	        inputSum = bias;
	        
	        for(Connection c : inputs)
	        {
	        	double v = c.value();
	        	if(! Double.isNaN(v)) inputSum += v;
	        }
	        
	        outputValue = inputSum / (temp+Math.abs(inputSum));
		}
		//if (Consts.DEBUG) System.out.println("fire="+outputValue);
	    return outputValue;
	}
	
	public void setValue(double v)
	{
		if(mode == 0)
		{
		  mode = 1;
		  inputSum = Double.NaN ;
		  outputValue = v;
		}
	}
	
	public double setError(double expectedV)
	{
		//if (Consts.DEBUG) System.out.print("m="+mode);
		if(mode < 2)
		{
			if(mode == 0)
			   fire();
		
			error = outputValue-expectedV;
			dvde = 2* error * temp/ Math.pow(Math.abs(inputSum)+temp, 2);
			mode = 2;
		}
		return Math.abs(error);
	}
	
	public double feedBack()
	{
		if (mode == 2) return dvde;
		if (mode == 0) return 0.0;
		
		double vector = 0.0;
		for(Connection c :outputs)
		{
			vector += c.feedback();
		}
		
		dvde = vector * temp / Math.pow(Math.abs(inputSum)+temp, 2);
		mode = 2;
		return dvde;
	}
	
	public void randomise()
	{
		for(Connection c : inputs)
		{
			c.randomise();
		}
		bias = RandomHelper.nextDoubleFromTo(-0.1,0.1);
	}
	
	public double train()
	{
		double v = 0.0;
		for(Connection c : inputs)
			v += Math.abs(c.train());
		
		v+= Math.abs(dvde * Parameters.learningSpeed );
		bias -= dvde * Parameters.learningSpeed ;
		
		return v;
	}

}
