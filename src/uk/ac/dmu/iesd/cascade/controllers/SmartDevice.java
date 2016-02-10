package uk.ac.dmu.iesd.cascade.controllers;

import uk.ac.dmu.iesd.cascade.util.IObserver;

/**
 * A <em>SmartDevice</em> is an object for energy management/control installed
 * (integrated) in (within) some/most of the prosumers (e.g.
 * HouseholdProsumers). They belong (or will be controlled after trading
 * contract) by aggregator agents (*CO: such as RECO or ESCO) by adding
 * (registering) themselves as observer (of the state / signal) of an aggregator
 * agent. In other words, they will be updated (receiving signals) by the
 * aggregator agent (at its discretion) in which they are dealing with (have
 * contract with).
 * 
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/05/23 12:00:00 $
 * 
 *          Version history (for intermediate steps see Git repository history)
 * 
 */

public class SmartDevice implements IObserver
{

	public SmartDevice()
	{

	}

	@Override
	public void update(Object obs, Object arg)
	{

	}

}
