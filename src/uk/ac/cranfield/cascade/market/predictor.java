/**
 * 
 */
package uk.ac.cranfield.cascade.market;
import java.util.ArrayList;

import repast.simphony.random.RandomHelper;

import uk.ac.cranfield.base.neural.*;

/**
 * @author grahamfletcher
 *
 */
public class predictor {
   Network n;
   TrainingSet t = new TrainingSet(7, 2);
   
   public double train()
   {
	   double e = 0.0;
      
	   e += t.learnRandom(n,10000);
      
      return e / 1;
      
   }
   
   public double test()
   {
	   return t.testAll(n);
   }
   
   public predictor()
   {
	   n = new Network();
	   
	   //Create the input and output layers
	   n.inputLayer=new Layer(7);
	   n.outputLayer = new Layer(2);
	   
	   //Create the hidden layers
	   Layer m = new Layer(6);
	   //Layer m1 = new Layer(6);
	   
	   n.layers.add(n.inputLayer);
	   n.layers.add(n.outputLayer);
	   n.layers.add(m);
	   //n.layers.add(m1);
	   
	   //connect the network up fully connected
	   n.outputLayer.connectInput(m);
	   //n.outputLayer.connectInput(m1);
	   
	   //m1.connectInput(m);
	   
	   //m1.connectInput(n.inputLayer);
	   m.connectInput(n.inputLayer);
	   
	   //Randomize the weights etc
	   n.randomize();
   }
  
   public double addExample(double unitCostD, double unitCostS, double gSP, double lS, double lD, double buyTreaderPurcahsePPU, double buyTraderSellPPU, double sellTraderPurchasePPU, double sellTraderSellPPU)
   {
	   
	   gSP = gSP/ Parameters.supplyScale;
	   lS = lS / Parameters.supplyScale;
	   lD = lD / Parameters.supplyScale;
	 
	   ArrayList<Double> in = new ArrayList<Double>();
	   in.add(gSP);
	   in.add(lS);
	   in.add(lD);
	   in.add(codeCost(buyTreaderPurcahsePPU));
	   in.add(codeCost(buyTraderSellPPU));
	   in.add(codeCost(sellTraderPurchasePPU));
	   in.add(codeCost(sellTraderSellPPU));
	   
	   ArrayList<Double> out = new ArrayList<Double>();
	   out.add(codeCost(unitCostS));
	   out.add(codeCost(unitCostD));
	   
	   
	   if(RandomHelper.nextIntFromTo(0,100) <= 0)
	       t.addExample(in, out, 1005);
	   
	   //Test the example as well
	   n.reset();
	   n.inputLayer.setValues(in);
	   //n.outputLayer.fire();
	   //return decodeCost( n.outputLayer.neurons.get(0).outputValue);
	   return n.outputLayer.setError(out);
   }
   
   public ArrayList<Double> predict(double gSP, double lS, double lD, double buyTreaderPurcahsePPU, double buyTraderSellPPU, double sellTraderPurchasePPU, double sellTraderSellPPU)
   {
	   
	   gSP = gSP/ Parameters.supplyScale;
	   lS = lS / Parameters.supplyScale;
	   lD = lD / Parameters.supplyScale;
	 
	   ArrayList<Double> in = new ArrayList<Double>();
	   in.add(gSP);
	   in.add(lS);
	   in.add(lD);
	   in.add(codeCost(buyTreaderPurcahsePPU));
	   in.add(codeCost(buyTraderSellPPU));
	   in.add(codeCost(sellTraderPurchasePPU));
	   in.add(codeCost(sellTraderSellPPU));
	   
	   n.reset();
	   n.inputLayer.setValues(in);
	   n.outputLayer.fire();
	   
	   //return decodeCost( n.outputLayer.neurons.get(0).outputValue);
	   
	   ArrayList<Double> preds = new ArrayList<Double>();
	   for(Neuron p : n.outputLayer.neurons)
		   preds.add(decodeCost(p.outputValue ));
	   
	   return preds;
       
   }
   
   public static double codeCost(double v)
   {
	   if (v < Parameters.minCost ) v = Parameters.minCost;
	   if (v > Parameters.maxCost ) v = Parameters.maxCost;
	   
	   v = (v - Parameters.minCost)/ (Parameters.maxCost - Parameters.minCost);
	   v = (v * 1.8) - 0.9;
	   
	   return v;
   }
   
   public static double decodeCost(double v)
   {
	   
	   v = (v + 0.9) / 1.8;
	   v = v * (Parameters.maxCost - Parameters.minCost);
	   v = v + Parameters.minCost;
	   
	   return v;
   }
   
   
}
