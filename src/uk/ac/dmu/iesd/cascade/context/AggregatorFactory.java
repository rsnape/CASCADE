package uk.ac.dmu.iesd.cascade.context;

/**
 * This class is a factory for creating instances of <tt>AggregatorAgent</tt> concrete subclasses 
 * (e.g. <code>RECO</code>)
 * Its public creator method's signatures are defined by {@link IAggregatorFactory} interface.
 *   
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/19 14:00:00 $
 */

public class AggregatorFactory implements IAggregatorFactory{
	
CascadeContext cascadeMainContext;

	public AggregatorFactory(CascadeContext context) {
		this.cascadeMainContext= context;
	}
	
	public RECO createRECO(double[] baseProfile){
		RECO aCO = new RECO(cascadeMainContext,baseProfile);
		return aCO;
	}
	
	public UtilityCo createUtilityCo(float[] baseProfile){
		return new UtilityCo(cascadeMainContext,baseProfile);
	}

}
