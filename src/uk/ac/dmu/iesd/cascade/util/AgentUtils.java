/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.collections.FilteredIterator;
import repast.simphony.util.collections.IndexedIterable;
import cern.jet.random.Empirical;

/**
 * @author jsnape
 * @version $Revision: 1.01 $ $Date: 2012/06/21 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial basic functionality
 * 1.01 - added dot product functionality and normalization
 *
 */

public class AgentUtils {

	/*
	 * Assign agents a given parameter value based on an input probability distribution
	 * This implementation requires a JavaBean type setters for the parameter.
	 * 
	 * NOTE: It is the responsibility of the caller to ensure that the values passed
	 * have the  type required by the setter method.  Failure to do so will
	 * result in an exception
	 * 
	 * TODO: I think an EmpiricalWalker would suit this better!!
	 * 
	 * @parameter paramName - the name of the parameter to set in each agent
	 * @parameter values - an (ordered) array of values for the specified parameter
	 * @parameter probs - an (ordered) array of the probability of agents being assigned corresponding value in the values array above
	 * @parameter agents - an IndexedIterable of the agents to which we will assign the parameter
	 */
	public static void assignProbabilisticDiscreteParameter(String paramName, Object[] values, double[] probs, Iterable agents)
	{
		//set up the distribution for drawing values
		Empirical myDist = RandomHelper.createEmpirical(probs, Empirical.NO_INTERPOLATION);

		//find the agent class
		for (Object thisAgent : agents)
		{
			PropertyDescriptor modProp = null;
			try {
				for (PropertyDescriptor prop : Introspector.getBeanInfo(thisAgent.getClass()).getPropertyDescriptors())
				{
					if (prop.getName().contentEquals(paramName))
					{
						modProp = prop;
					}
				}
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				System.err.println("AgentUtils: failed to get find Bean Info for agent " + thisAgent.toString());
				e.printStackTrace();
			}


			if (modProp != null)
			{
				int i = 0;
				double choiceVar = RandomHelper.nextDouble();
				int selectedVal = 0;
				while (selectedVal < 1)
				{
					if (choiceVar < myDist.cdf(i))
					{
						selectedVal = i;
					}
					i++;
				}

				if (selectedVal >= values.length)
				{
					selectedVal = values.length - 1;
				}

				try {
					modProp.getWriteMethod().invoke(thisAgent, values[selectedVal]);
				} catch (Exception e) {
					System.err.println("AgentUtils: Caught an exception while invoking the bean setter method on propert " + modProp.toString() + " on agent " + thisAgent.toString());
					System.err.println("Likely to cause unintended behaviour");
					e.printStackTrace();
				}
			}
			else
			{
				System.err.println("Agent " + thisAgent.toString() + " doesn't have property " + paramName);
				System.err.println("Cannot complete the action");
			}
		}
	}

	/*
	 * Assign agents a given parameter value based on a list of input values
	 * This implementation requires a JavaBean type setters for the parameter
	 * 
	 * 
	 * NOTE: It is the responsibility of the caller to ensure that the values passed
	 * have the  type required by the setter method.  Failure to do so will
	 * result in an exception
	 * 
	 * @parameter paramName - the name of the parameter to set in each agent
	 * @parameter values - an (ordered) array of values for the specified parameter.  Note this must have the same
	 * 						number of members as there are agents.
	 * @parameter agents - an IndexedIterable of the agents to which we will assign the parameter
	 */
	public static void assignParameters(String paramName, Object[] values, Iterable agents)
	{		
		int size = 0;
		while(agents.iterator().hasNext())
		{
			agents.iterator().next();
			size++;
		}
		if (values.length != size)
		{
			System.err.println("Mismatched number of values for agents.  There are " + values.length + " values for " + size + "agents.");
			System.err.println("This method has failed - it is likely that simulation will give unintended results.");
			return;
		}

		int i = 0;
		for (Object thisAgent : agents)
		{
			PropertyDescriptor modProp = null;
			try {
				for (PropertyDescriptor prop : Introspector.getBeanInfo(thisAgent.getClass()).getPropertyDescriptors())
				{
					if (prop.getName().contentEquals(paramName))
					{
						modProp = prop;
					}
				}
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				System.err.println("failed to get find Bean Info for agent " + thisAgent.toString());

				e.printStackTrace();
			}

			if (modProp != null)
			{
				try {
					modProp.getWriteMethod().invoke(thisAgent, values[i]);
				} catch (Exception e) {
					System.err.println("Caught an exception while invoking the bean setter method on propert " + modProp.toString() + " on agent " + thisAgent.toString());
					System.err.println("Likely to cause unintended behaviour");
					e.printStackTrace();
				}
			}
			else
			{
				System.err.println("Agent " + thisAgent.toString() + " doesn't have property " + paramName);
				System.err.println("Cannot complete the action");
			}
			i++;
		}
	}

	/*
	 * Assign agents a given parameter value based on a list of input values
	 * This implementation requires a JavaBean type setters for the parameter
	 * 
	 *  
	 * NOTE: It is the responsibility of the caller to ensure that the value passed
	 * has the type required by the setter method.  Failure to do so will
	 * result in an exception
	 * 
	 * @parameter paramName - the name of the parameter to set in each agent
	 * @parameter value - the value to be assigned to the specified parameter. 
	 * @parameter agents - an IndexedIterable of the agents to which we will assign the parameter
	 */
	public static void assignParameterSingleValue(String paramName, Object value, Iterable agents)
	{
		
		for (Object thisAgent : agents)
		{

			PropertyDescriptor modProp = null;
			try {
				for (PropertyDescriptor prop : Introspector.getBeanInfo(thisAgent.getClass()).getPropertyDescriptors())
				{
					if (prop.getName().contentEquals(paramName))
					{
						modProp = prop;
					}
				}
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				System.err.println("AgentUtils: failed to get find Bean Info for agent " + thisAgent.toString());
				e.printStackTrace();
			}

			if (modProp != null)
			{
				try {
					modProp.getWriteMethod().invoke(thisAgent, value);
				} catch (Exception e) {
					System.err.println("AgentUtils: Caught an exception while invoking the bean setter method on propert " + modProp.toString() + " on agent " + thisAgent.toString());
					System.err.println("Likely to cause unintended behaviour");
					e.printStackTrace();
				}
			}
			else
			{
				System.err.println("AgentUtils: Agent " + thisAgent.toString() + " doesn't have property " + paramName);
				System.err.println("Cannot complete the action");
			}
		}
	}


	public static void assignParameterSingleValue(String paramName, Object value, Iterator agents)
	{
		while (agents.hasNext())
		{
			Object thisAgent = agents.next();
			PropertyDescriptor modProp = null;
			try {
				for (PropertyDescriptor prop : Introspector.getBeanInfo(thisAgent.getClass()).getPropertyDescriptors())
				{
					if (prop.getName().contentEquals(paramName))
					{
						modProp = prop;
					}
				}
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				System.err.println("AgentUtils: failed to get find Bean Info for agent " + thisAgent.toString());
				e.printStackTrace();
			}

			if (modProp != null)
			{
				try {
					modProp.getWriteMethod().invoke(thisAgent, value);
				} catch (Exception e) {
					System.err.println("AgentUtils: Caught an exception while invoking the bean setter method on propert " + modProp.toString() + " on agent " + thisAgent.toString());
					System.err.println("Likely to cause unintended behaviour");
					e.printStackTrace();
				}
			}
			else
			{
				System.err.println("AgentUtils: Agent " + thisAgent.toString() + " doesn't have property " + paramName);
				System.err.println("Cannot complete the action");
			}
		}
	}

}




