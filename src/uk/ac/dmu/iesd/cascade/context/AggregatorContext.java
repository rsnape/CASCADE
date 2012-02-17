/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import repast.simphony.context.Context;

/**
 * @author bmahda00
 *
 */
public class AggregatorContext extends CascadeContext {
	
		public AggregatorContext (Context context){
			super(context);
		}
		
		/**
		 * This method return the number of <tt> tickPerDay </tt>
		 * @override
		 * @return <code>parent tickPerDay</code>
		 */
		public int getNbOfTickPerDay() {
			//if (Consts.DEBUG) System.out.println("AggContext getTickPerDay: this "+this.ticksPerDay);
			//if (Consts.DEBUG) System.out.println("AggContext getTickPerDay: super "+super.getTickPerDay());

			return super.getNbOfTickPerDay();
		}
	

}
