package uk.ac.dmu.iesd.cascade.context;

public class WrongCustomerTypeException extends RuntimeException{
	
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

public WrongCustomerTypeException() {
	System.err.println("Wrong type of customer found connected to aggregator");
	System.err.println("Check direction of edges in economic network etc.");
}

public WrongCustomerTypeException(Object cause){
	System.err.println("Wrong type of customer found connected to aggregator");
	System.err.println("Check direction of edges in economic network etc.");
	System.err.println(cause);
}
}
