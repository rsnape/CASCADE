/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.agents.aggregators.EquationBasedPriceAggregator;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * @author jsnape
 *
 */
public class ConstantPlusElasticityProsumer extends ConstantLoadProsumer {
    private static double DEFAULT_ELASTICITY = -0.07;
    private double p0;
    private double nextBaseD;
    
	/**
	 * @param context
	 * @param load
	 */
	public ConstantPlusElasticityProsumer(CascadeContext context) {
		super(context);
		this.e_factor = DEFAULT_ELASTICITY;
		this.hasSmartMeter = true;
	}
	
	/**
	 * @param context
	 * @param load
	 */
	public ConstantPlusElasticityProsumer(CascadeContext context, double load) {
		this(context,load,DEFAULT_ELASTICITY);
	}
	
	/**
	 * @param context
	 * @param load
	 */
	public ConstantPlusElasticityProsumer(CascadeContext context, double load, double elasticity) {
		super(context, load);
		//Note - in our conception here, elasticity is negative (i.e. higher price gives lower demand)
		if (elasticity > 0)
		{
			elasticity = 0 - elasticity;
		}
		this.e_factor = elasticity;
		System.out.println("Created prosumer with elasticity = "+this.e_factor);
		this.p0 = 125;
		this.hasSmartMeter = true;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#step()
	 */
	@Override
	public void step() {
		// simply alter the net demand based on price for this step
		// efactor = (p-p0) / (d-d0)
		double d0 = this.getBaseConstLoad()*(0.5+RandomHelper.nextDouble()); //Should this always reference base, or last demand?
		//d0 = this.getNetDemand();
		
		double percentChangeP = (this.getCurrentPrediction() - p0) / ((this.getCurrentPrediction() + p0)/2);
		double percentChangeD = (this.e_factor*(0.5+RandomHelper.nextDouble())) * percentChangeP;
		System.out.println("Setting demand from price "+this.getCurrentPrediction()+", percentage price change "+percentChangeP+" and prior demand "+d0);
		double newD = d0*(percentChangeD + 2)/(2- percentChangeD);
		System.out.println("Resulting in percentage demand change "+percentChangeD+" and new demand "+newD);
		this.setNetDemand(newD);
		//p0 = this.getCurrentPrediction();
	}

}
