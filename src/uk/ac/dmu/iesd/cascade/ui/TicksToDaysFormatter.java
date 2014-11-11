/**
 * 
 */
package uk.ac.dmu.iesd.cascade.ui;

import repast.simphony.ui.plugin.TickCountFormatter;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * @author jsnape
 * 
 */
public class TicksToDaysFormatter implements TickCountFormatter
{

	private CascadeContext context;

	/*
	 * (non-Javadoc)
	 * 
	 * @see repast.simphony.ui.plugin.TickCountFormatter#format(double)
	 */
	@Override
	public String format(double tick)
	{
		int day = this.context.getDayCount();
		int year = day / Consts.DAYS_PER_YEAR;
		int week = day / Consts.DAYS_PER_WEEK;
		StringBuilder returnBuilder = new StringBuilder();
		returnBuilder.append("Year: ");
		returnBuilder.append(year);
		returnBuilder.append(", Week: ");
		returnBuilder.append(week);
		returnBuilder.append(", Day: ");
		returnBuilder.append(day);
		returnBuilder.append(", Timeslot: ");
		returnBuilder.append(this.context.getTimeslotOfDay());
		returnBuilder.append(" (Raw Tick: ");
		returnBuilder.append(tick);
		returnBuilder.append(")");
		return returnBuilder.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see repast.simphony.ui.plugin.TickCountFormatter#getInitialValue()
	 */
	@Override
	public String getInitialValue()
	{
		// TODO Auto-generated method stub
		return this.format(0);
	}

	public TicksToDaysFormatter(CascadeContext context)
	{
		this.context = context;
	}

}
