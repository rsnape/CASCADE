/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * This is a very simple prosumer which will present a constant load to the system
 * 
 * Note that this may be positive or negative, representing a constant load consumer
 * or constant load generator respectively
 * 
 * @author jsnape
 *
 */
public class ConstantLoadProsumer extends ProsumerAgent {
	private static double DEFAULT_LOAD = 0.5f;
	private double baseConstLoad;
	
	/**
	 * @param baseConstLoad the baseConstLoad to set
	 */
	public void setBaseConstLoad(double baseConstLoad) {
		this.baseConstLoad = baseConstLoad;
	}

	/**
	 * @return the baseConstLoad
	 */
	public double getBaseConstLoad() {
		return baseConstLoad;
	}

	/**
	 * @param context
	 */
	public ConstantLoadProsumer(CascadeContext context) {
		this(context,DEFAULT_LOAD);

	}

	/**
	 * Instantiates a constant load prosumer with the specified load.  Note
	 * that the load should be positive to represent consumption and negative
	 * to represent generation
	 * 
	 * @param context - the context within which this prosumer will be created
	 * @param load - the constant load this prosumer presents
	 */
	public ConstantLoadProsumer(CascadeContext context, double load) {
		super(context);
		this.netDemand = load;
		this.setBaseConstLoad(load);
		context.add(this);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport() {
		return this.getAgentName();
	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#step()
	 */
	@Override
	public void step() {
		// Do nothing on step for this prosumer - nothing changes
	}

}
