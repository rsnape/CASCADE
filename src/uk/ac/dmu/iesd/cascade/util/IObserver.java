package uk.ac.dmu.iesd.cascade.util;

/**
 * Interface <code>IObserver</code> defines how an IObserver implementer should
 * behave by basically implementing the update method, so that those objects
 * wanting to be informed about changes in observable objects get
 * notified/updated.
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.00 $ $Date: 2011/05/23 1:00:00
 * 
 * @see uk.ac.dmu.iesd.cascade.util.IObservable
 */

public interface IObserver
{
	/**
	 * This method is called whenever the observed object is changed. An
	 * application/object/agent calls an <tt>Observable</tt> object's
	 * <code>notifyObservers</code> method to have all the object's observers
	 * notified of the change.
	 * 
	 * @param obs
	 *            the observable object.
	 * @param arg
	 *            an argument passed to the <code>notifyObservers</code> method.
	 */
	public abstract void update(Object obs, Object arg);
}
