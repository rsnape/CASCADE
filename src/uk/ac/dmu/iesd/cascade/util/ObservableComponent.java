package uk.ac.dmu.iesd.cascade.util;

import java.util.Enumeration;
import java.util.Vector;

/**
 * This class represents a concrete observable object/component. An IObservable
 * implementer class can simply use an instance of this object and delegate the
 * task of adding, removing observers along with other required tasks to this
 * object (e.g. counting observers, notifying observed ...)
 * <p>
 * An observable implementer object can have one or more observers. An observer
 * may be any object that implements interface <tt>IObserver</tt>. After an
 * IObservable implementer instance changes, an appropriate code will call the
 * <code>IObservable</code>'s <code>notifyObservers</code> method causes all of
 * its observers to be notified of the change by a call to their
 * <code>update</code> method.
 * <p>
 * The default implementation provided in this class will notify Observers in
 * the order in which they registered interest.
 * <p>
 * When an observable object is newly created, its set of observers is empty.
 * Two observers are considered the same if and only if the <tt>equals</tt>
 * method returns true for them.
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.00 $ $Date: 2011/05/23 1:00:00
 * @see IObserver
 * @see IObservable
 */

public class ObservableComponent implements IObservable
{
	private Vector obsVect;

	/**
	 * Construct an ObservableComponent with zero Observers.
	 * */
	public ObservableComponent()
	{
		this.obsVect = new Vector();
	}

	/**
	 * Adds an observer to the set of observers for this object, provided that
	 * it is not the same as some observer already in the set. The order in
	 * which notifications will be delivered to multiple observers is not
	 * specified. See the class comment.
	 * 
	 * @param obs
	 *            an observer to be added.
	 * @throws NullPointerException
	 *             if the parameter o is null.
	 */
	@Override
	public synchronized void addObserver(IObserver obs)
	{
		if (obs == null)
		{
			throw new NullPointerException();
		}
		if (!this.obsVect.contains(obs))
		{
			this.obsVect.addElement(obs);
		}
	}

	/**
	 * Deletes an observer from the set of observers of this object.
	 * 
	 * @param obs
	 *            the observer to be deleted.
	 */
	@Override
	public synchronized void deleteObserver(IObserver obs)
	{
		this.obsVect.removeElement(obs);
	}

	/**
	 * Clears the observer list so that this object no longer has any observers.
	 */
	@Override
	public synchronized void deleteObservers()
	{
		this.obsVect.removeAllElements();
	}

	/**
	 * This method can be called when an the observed object' state (in which
	 * the observer object is interested) has changed. It notifies all of its
	 * observers and then call
	 * <p>
	 * Each observer has its <code>update</code> method called with two
	 * arguments: the observable object and the <code>changeCodeArg</code>
	 * argument.
	 * 
	 * @param obs
	 *            the observed object.
	 * @param changeCodeArg
	 *            the changed code argument.
	 * @see IObserver#update(Object,Object)
	 */
	public void notifyObservers(Object obs, Object changeCodeArg)
	{

		/*
		 * Object[] arrLocal; arrLocal = obsVect.toArray(); for (int i =
		 * arrLocal.length-1; i>=0; i--) ((IObserver)arrLocal[i]).update(obj,
		 * arg);
		 */

		Enumeration myIObserversList = this.obsVect.elements();

		while (myIObserversList.hasMoreElements())
		{
			IObserver anIObserver = (IObserver) myIObserversList.nextElement();
			anIObserver.update(obs, changeCodeArg);
		}
	}

	/**
	 * Returns the number of observers of an <tt>IObservable</tt> object.
	 * 
	 * @return the number of observers of a given object.
	 */
	@Override
	public synchronized int countObservers()
	{
		return this.obsVect.size();
	}

}
