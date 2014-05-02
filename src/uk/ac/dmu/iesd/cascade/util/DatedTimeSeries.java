/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author Richard
 *
 */
public class DatedTimeSeries<K extends Long, T> extends TimeSeries<K, T> {
	
	private Calendar cal = new GregorianCalendar();

}
