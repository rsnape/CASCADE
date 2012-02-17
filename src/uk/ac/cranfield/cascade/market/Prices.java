/**
 * 
 */
package uk.ac.cranfield.cascade.market;

import repast.simphony.random.RandomHelper;

/**
 * @author grahamfletcher
 *
 */
public class Prices {
	
		public double contractSale=10;
		public double closingSale=10;
		public double sale = 10;
		public double contractPurchase=10;
		public double closingPurchase=10;
		public double purchase = 10;
		
		public double avgContractSale=10;
		public double avgClosingSale=10;
		public double avgContractPurchase=10;
		public double avgClosingPurchase=10;
		public double avgSale = 10;
		public double avgPurchase = 10;
		
		public static double avgGContractSale=10;
		public static double avgGClosingSale=10;
		public static double avgGContractPurchase=10;
		public static double avgGClosingPurchase=10;
		public static double avgGSale = 10;
		public static double avgGPurchase = 10;
		
		public double powerCost=10;
		public double avgPowerCost = 10;
		public static double avgGPowerCost = 10;
		
		public double quantity=10;
		public double qSold = 10;
		public double qPurchased = 10;
		public double avgQuantity = 10;
		public double avgQSold = 10;
		public double avgQPurchased = 10;
	    
		private double reac1 = 1 -  Parameters.reac1 * RandomHelper.nextDoubleFromTo(0.1, 5);
		private double reactiveness = 1 - reac1;

		public String name = "";
		
		public Prices(String name)
		{
			this.name = name;
		}
		
		public void setQSold(double v)
		{
			qSold = v;
			avgQSold = reactiveness * avgQSold + reac1 * v;
		}
		public void setQPurchased(double v)
		{
			qPurchased = v;
			avgQPurchased = reactiveness * avgQPurchased + reac1 * v;
		}
		public void setSale(double v)
		{
			if(v > Parameters.brownOutCost+1)
				{
				//if (Consts.DEBUG) System.out.println("Sale price too big");
				//System.exit(1);
				}
			sale = v;
			avgSale = reactiveness * avgSale + reac1 * v;
			avgGSale = Parameters.reactivnessG * avgGSale + Parameters.reac2 * v;
		}
		public void setPurchase(double v)
		{
			if(v > Parameters.brownOutCost+1)
			{
			//if (Consts.DEBUG) System.out.println("Purchase price too big"+v);
			System.exit(1);
			}
			purchase = v;
			avgPurchase = reactiveness * avgPurchase + reac1 * v;
			avgGPurchase = Parameters.reactivnessG * avgGPurchase + Parameters.reac2 * v;
		}
		public static double getGlobalPowerPrice()
		{
		  return avgGPowerCost;
		}
		
		public String toString() {return name;}
		
		public void setClosingSale(double v){
			//if(v > avgClosingSale *1.2) v = avgClosingSale * 1.2;
			//if(v < avgClosingSale *0.8) v = avgClosingSale * 0.8;
			closingSale = v;
			
			avgClosingSale = avgClosingSale * reactiveness + v * reac1;
		};
		public void setContractSale(double v){
			//if(v > avgContractSale *1.2) v = avgContractSale * 1.2;
			//if(v < avgContractSale *0.8) v = avgContractSale * 0.8;
			contractSale = v;
			
			avgContractSale = avgContractSale * reactiveness + v * reac1;
			avgGContractSale = avgGContractSale * Parameters.reactivnessG + v * Parameters.reac2;
		};
		public void setClosingPurchese(double v){
			//if(v > avgClosingPurchase *1.2) v = avgClosingPurchase * 1.2;
			//if(v < avgClosingPurchase *0.8) v = avgClosingPurchase * 0.8;
			
			closingPurchase = v;
			avgClosingPurchase = avgClosingPurchase * reactiveness + v * reac1;
		}
		public static void setGClosingPurchese(double v){

			avgGClosingPurchase = avgGClosingPurchase * Parameters.reactivnessG + v * Parameters.reac2;
		}
		public static void setGClosingSale(double v){

			avgGClosingSale = avgGClosingSale * Parameters.reactivnessG + v * Parameters.reac2;
		}
		public void setContractPurchase(double v){
			//if(v > avgContractPurchase *1.2) v = avgContractPurchase * 1.2;
			//if(v < avgContractPurchase *0.8) v = avgContractPurchase * 0.8;

			contractPurchase = v;
			avgContractPurchase = avgContractPurchase * reactiveness + v * reac1;
			avgGContractPurchase = avgGContractPurchase * Parameters.reactivnessG + v * Parameters.reac2;
	        //if (Consts.DEBUG) System.out.println("Setting contract Purchase of "+v+" avg now "+avgContractPurchase +" and "+avgGContractPurchase);
		}
		

		
	
}
