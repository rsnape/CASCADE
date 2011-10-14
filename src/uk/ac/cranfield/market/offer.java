package uk.ac.cranfield.market;

public class offer {
     int startTimeStamp;
     int endTimeStamp;
     double size;
     double price=0.0;
     Aggregator from;
     
     public offer (int startTimeStamp, int endTimeStamp, double size, Aggregator from)
     {
    	 this.size = size;
    	 this.endTimeStamp = endTimeStamp;
    	 this.startTimeStamp= startTimeStamp;
    	 this.from = from;
    	 
    	 //System.out.println(startTimeStamp+"<"+size+">"+endTimeStamp);
     }
}
