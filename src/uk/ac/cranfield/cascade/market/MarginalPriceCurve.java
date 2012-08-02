package uk.ac.cranfield.cascade.market;

import java.util.ArrayList;

public class MarginalPriceCurve {
	double minimumBalance;
	double maximumBalance;
	
	ArrayList<Double> balances = new ArrayList<Double>();
	ArrayList<Double> marginalPrice = new ArrayList<Double>();
	
	
	public MarginalPriceCurve(double minimumBalance, double minBalMP, double maximumBalance, double maxBalMP)
	{
		this.minimumBalance  = minimumBalance;
		this.maximumBalance  = maximumBalance;
		
		balances.add(minimumBalance);
		balances.add(maximumBalance);
		marginalPrice.add(minBalMP);
		marginalPrice.add(maxBalMP);
	}
	
	public void reset(double minimumBalance, double minBalMP, double maximumBalance, double maxBalMP)
	{
		this.minimumBalance  = minimumBalance;
		this.maximumBalance  = maximumBalance;
		balances = new ArrayList<Double>();
		ArrayList<Double> marginalPrice = new ArrayList<Double>();
		balances.add(minimumBalance);
		balances.add(maximumBalance);
		marginalPrice.add(minBalMP);
		marginalPrice.add(maxBalMP);
	}
	private MarginalPriceCurve()
	{
	}
	
	public MarginalPriceCurve clone()
	{
		MarginalPriceCurve mpc = new MarginalPriceCurve();
		mpc.minimumBalance = minimumBalance;
		mpc.maximumBalance = maximumBalance;
		for(Double d:balances)
			mpc.balances.add(d);
		for(Double d:marginalPrice)
			mpc.marginalPrice.add(d);
		return mpc;
	}
	
	
	public void addPrice(double balance, double mP)
	{
	    if((balance < minimumBalance) || (balance > maximumBalance))
	    	return;
	    int i;
	    for(i = 0; balance > balances.get(i); i++);
	    
	    if(balance == balances.get(i))
	    	marginalPrice.set(i, mP);
	    
	    if(balance < balances.get(i))
	    {
	        balances.add(i,balance);
	    	marginalPrice.add(i, mP);
	    }
	}
	
	public double getPrice(double balance)
	{
		if((balance < minimumBalance) || (balance > maximumBalance))
	    	return Double.NaN;
		
		int i;
		for(i = 0; balances.get(i) < balance; i++);
		
		if(balances.get(i) == balance)
		  return marginalPrice.get(i);
		else
			return marginalPrice.get(i-1);
	}
	
	public double sumPrice(double balance)
	{
		double p = 0;
		if((balance < minimumBalance) || (balance > maximumBalance))
	    	return Double.NaN;
		
		int i;
		
		for(i = 0; i < balances.size() ; i++)
		{
			double max;
			if(i == balances.size() - 1)
				max = balances.get(balances.size() - 1);
			else
				max = balances.get(i+1)-1;
			if(max> balance) max = balance;
			
			double w = max - balances.get(i) + 1;
			if(w < 0) w = 0;
			
			p += w * marginalPrice.get(i);
		   		
		}
		
		return p;
	
	}
	
	
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/*
	static void test()
	{
		MarginalPriceCurve mpc1 = new MarginalPriceCurve(10,1,20,2);
		
		test.log( "marginalPriceCurve 1",mpc1.getPrice(10), 1);
		test.log( "marginalPriceCurve 2",mpc1.getPrice(20), 2);
		test.log( "marginalPriceCurve 3",mpc1.getPrice(15), 1);
		
		mpc1.addPrice(15,1.1);
		test.log( "marginalPriceCurve 4",mpc1.getPrice(10), 1);
		test.log( "marginalPriceCurve 5",mpc1.getPrice(20), 2);
		test.log( "marginalPriceCurve 6",mpc1.getPrice(15),1.1);
		test.log( "marginalPriceCurve 7",mpc1.getPrice(11), 1);
		test.log( "marginalPriceCurve 7a",mpc1.getPrice(16), 1.1);
		
		mpc1.addPrice(15,3);
		test.log( "marginalPriceCurve 8",mpc1.getPrice(10), 1);
		test.log( "marginalPriceCurve 9",mpc1.getPrice(20), 2);
		test.log( "marginalPriceCurve 10",mpc1.getPrice(15),3);
		test.log( "marginalPriceCurve 11",mpc1.getPrice(12), 1);
		test.log( "marginalPriceCurve 12", mpc1.getPrice(17), 3);
		
		mpc1 = new MarginalPriceCurve(1,100,10000,20);
		mpc1.addPrice(2,1);
		
		test.log( "marginalPriceCurve 13",mpc1.sumPrice(1),100);
		test.log( "marginalPriceCurve 14", mpc1.sumPrice(10000),1*100+9998*1+1*20);
		
		mpc1.addPrice(5001,2);
		test.log( "marginalPriceCurve 15", mpc1.sumPrice(10000),1*100+4999*1+4999*2+1*20);
		
		test.log( "marginalPriceCurve 16", mpc1.sumPrice(4000),1*100+3999*1);
		test.log( "marginalPriceCurve 17", mpc1.sumPrice(4999),1*100+4998*1);
		test.log( "marginalPriceCurve 18", mpc1.sumPrice(5000),1*100+4999*1);
		test.log( "marginalPriceCurve 19", mpc1.sumPrice(5001),1*100+4999*1+1*2);
		test.log( "marginalPriceCurve 20", mpc1.sumPrice(5002),1*100+4999*1+2*2);
		
		
	}
	
	*/
	

	
}
