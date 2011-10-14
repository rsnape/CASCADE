/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

/**
 * @author jsnape
 *
 */
public class PowerPrice {
	
	/**
	 * private variable indicating the time at which this price is valid
	 * currently implemented as an integer representing number of ticks
	 * from the model run start.
	 */
	private int timeStamp;
	
	/**
	 * the price to be charged at this time
	 */
	private double price;
	
	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}


	public int getTimeStamp() {
		return timeStamp;
	}

	/**
	 * construct a PowerPrice for the given time.
	 * 
	 * @param timeStamp
	 */
	public PowerPrice(int timeStamp) {
		super();
		this.timeStamp = timeStamp;
	}
	
	/**
	 * construct a PowerPrice for the given time and price
	 * 
	 * @param timeStamp
	 * @param price
	 */
	public PowerPrice(int timeStamp, double price) {
		super();
		this.timeStamp = timeStamp;
		this.price = price;
	}


}
