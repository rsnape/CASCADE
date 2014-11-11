package uk.ac.dmu.iesd.cascade.util;

/**
 * Interface <code>IObservable</code> defines how an IObservable implementer
 * should behave by basically allowing add, delete and count of observer
 * objects.
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.00 $ $Date: 2011/05/23 1:00:00
 * 
 * @see uk.ac.dmu.iesd.cascade.util.IObservable
 */

public interface IObservable
{
	/**
	 * This method allows an observer to be added to the list of observer
	 * objects
	 * 
	 * @param anIObserver
	 *            the observer (IObsever) who wants to be added and updated when
	 *            a specific state changes or event occurs
	 */
	public abstract void addObserver(IObserver anIObserver);

	public abstract void deleteObserver(IObserver anIObserver);

	public abstract void deleteObservers();

	public abstract int countObservers();
}