package uk.ac.dmu.iesd.cascade.market.astem.util;

import java.util.Comparator;

import uk.ac.dmu.iesd.cascade.agents.aggregators.BOD;
import uk.ac.dmu.iesd.cascade.market.data.BSOD;

/**
 * 
 *  <em>SortComparatorUtils</em> consists of comparator implementations of different object/class 
 * which compare two arguments for order. 
 * For an ascending order, for example, the compare method of the Comparator interface should return 
 * a negative integer, zero, or a positive integer as the first argument
 * is less than, equal to, or greater than the second.
 * 
 * @author Babak Mahdavi Ardestani
 * @version 1.0 $ $Date: 2012/02/11
 * 
 */

public class SortComparatorUtils {
	
   public static final Comparator<BOD>
	SO_SUBMITTED_BO_TOPDOWN_DESCENDING_ORDER =	 new Comparator<BOD>() {
		public int compare(BOD bod1, BOD bod2) {
			int res=0;
			if (bod2.getSubmittedBO() < bod1.getSubmittedBO())
				res=-1;
			else if (bod2.getSubmittedBO() > bod1.getSubmittedBO())
				res = 1;
			return res;
		}
	}; 
	
	public static final Comparator<BOD>
	SO_SUBMITTED_BO_TOPDOWN_ASCENDING_ORDER =	 new Comparator<BOD>() {
		public int compare(BOD bod1, BOD bod2) {
			int res=0;
			if (bod2.getSubmittedBO() < bod1.getSubmittedBO())
				res=1;
			else if (bod2.getSubmittedBO() > bod1.getSubmittedBO())
				res = -1;
			return res;
		}
	}; 
	
	public static final Comparator<BSOD>
	PX_PRICE_ASCENDING_ORDER =	 new Comparator<BSOD>() {
		public int compare(BSOD bsod1, BSOD bsod2) {
			int res=0;
			if (bsod2.getPrice() < bsod1.getPrice())
				res=-1;
			else if (bsod2.getPrice() > bsod1.getPrice())
				res = 1;
			return res;
		}
	}; 
	
	public static final Comparator<BSOD>
	PX_PRICE_DESCENDING_ORDER =	 new Comparator<BSOD>() {
		public int compare(BSOD bsod1, BSOD bsod2) {
			int res=0;
			if (bsod2.getPrice() < bsod1.getPrice())
				res=1;
			else if (bsod2.getPrice() > bsod1.getPrice())
				res = -1;
			return res;
		}
	}; 
	
}
