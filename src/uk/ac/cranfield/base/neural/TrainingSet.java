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
public class TrainingSet {
    int inputSize;
    int outputSize;
    
    private class example{
    	public ArrayList<Double> inputs;
    	public ArrayList<Double> outputs;
    	public double absError;
    }
    private ArrayList<example> examples= new ArrayList<example>();
    
    
    
    
    public TrainingSet(int inputSize, int outputSize)
    {
    	this.inputSize = inputSize;
    	this.outputSize = outputSize;
    }
    
    public void addExample(ArrayList<Double> in, ArrayList<Double> out)
    {
    	if ((in.size() == inputSize) && (out.size() == outputSize))
    	{
    		example e = new example();
    		
    		e.inputs = (ArrayList<Double>) in.clone();
    		e.outputs = (ArrayList<Double>) out.clone();
    		examples.add(RandomHelper.nextIntFromTo(0,examples.size()),e);
    	}
    }
    
    public void addExample(ArrayList<Double> in, ArrayList<Double> out, int maxSize)
    {
    	addExample(in,out);
    	while(examples.size() > maxSize)
    	{
    		if(RandomHelper.nextIntFromTo(0, 100)< 90)
    			examples.remove(examples.size()-1);
    		else
    			examples.remove(0);
    	}
    }
    
    public double testAll(Network n)
    {
    	if((n.inputLayer.neurons.size() != inputSize) ||
    		(n.outputLayer.neurons.size() != outputSize))
    		return 0;
    	
    	double error = 0.0;
    	
    	for(int i = 0; i < examples.size(); i++)
    	{
    		n.reset();
    		n.inputLayer.setValues(examples.get(i).inputs);
    		error += n.outputLayer.setError(examples.get(i).outputs);
    	}
    	
    	return error;
    }
    
    public double learnAll(Network n)
    {
   	
    	double error = 0.0;
    	
    	for(int i = 0; i < examples.size(); i++)	
    		error += learnExample(n,i);
    	return error;
    }
    
    public double learnRandom(Network n, int cnt)
    {
    	double error = 0.0;
    	
    	while(cnt-- > 0)
    	{
    		int i = RandomHelper.nextIntFromTo(0, examples.size()-1);
    		error += learnExample(n,i);
    	}
    	
    	return error;
    }
    
    
    public double learnExample(Network n, int i)
    {
    	double error = 0.0;
    	
    	if((n.inputLayer.neurons.size() != inputSize) ||
    		(n.outputLayer.neurons.size() != outputSize))
    		return 0;
    	
    	if((i < 0) || (i >= examples.size()))
    		return 0;
   
    	n.reset();
    	n.inputLayer.setValues(examples.get(i).inputs);
    	
    	//error +=n.outputLayer.setError(examples.get(i).outputs);
    	
    	examples.get(i).absError = Math.abs(n.outputLayer.setError(examples.get(i).outputs));
    	error += examples.get(i).absError;
    	n.inputLayer.feedBack();
    	n.train();
    	
    	
    	//Move the example up in the example list if it's error is bigger than the one abouve
    	if(i > 0)
    		if(examples.get(i).absError > examples.get(i-1).absError)
    		{
    			example e = examples.get(i);
    			examples.set(i, examples.get(i-1));
    			examples.set(i-1, e);
    		}
    		
    	
    	return error;
    }
    
    
    
}
