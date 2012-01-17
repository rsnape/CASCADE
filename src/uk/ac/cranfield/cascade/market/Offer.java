package uk.ac.cranfield.cascade.market;


import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;

public class Offer {
     int startTimeStamp;
     int endTimeStamp;
     double size;
     double price=0.0;
     double unitPrice = 0.0;
     double power;
     Trader from;
     
     public Offer (int startTimeStamp, int endTimeStamp, double size, Trader from)
     //Create an offer of a specific size
     {
    	 this.size = size;
    	 this.endTimeStamp = endTimeStamp;
    	 this.startTimeStamp= startTimeStamp;
    	 power = size * (endTimeStamp - startTimeStamp + 1);
    	 this.from = from;
     }
     
    public static Offer newFromTS(int timeStamp, Trader tr)
    //Create a new offer from the trader tr that includes the timeStamp
    {
		int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();	
        int offset = timeStamp-currentTick;
        
        //Return nothing if bad timestamp or nothing to sell
        if((offset < 0) || (offset >= Parameters.tradingHorizon) ||
           (tr.balance.get(offset)<=0))
        	return null;
        
        int st = offset;
        int en = offset;
        double si = tr.balance.get(offset) * RandomHelper.nextDoubleFromTo(0.1, 0.3);
        
        //Move the start time as early as possible
        while((st >0) && (tr.balance.get(st-1) > si)) st--;
        
    	//Move the end time as late as possible
        while((en <Parameters.tradingHorizon-2) && (tr.balance.get(en+1) > si)) en++;
        
        
        Offer o = new  Offer(st+currentTick,en+currentTick,si,tr);
        if(o.power > 100)
        	return o;
        else
        	return null;
    }
    
    public Trader bestOffer(Market m)
    //Iterate through all the traders in a market and find the one with the best offer
    {
    	//Use the default market if none supplied
    	if(m==null) m = Market.defaultM;
    	
        Trader t = null;
        double offer = Double.MIN_VALUE;
        for(Trader s : m.traders)
        {
        	double thisOffer = s.valueOffer(this);
        	if(thisOffer > offer)
        	{
        		thisOffer = offer;
        		t = s;
        	}
        }
        price = offer;
        unitPrice = price / power;
        return t;
    }
    
     
}
