/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological;

/**
 * @author jsnape
 * 
 */
public class SimpleRelationship implements Relationship
{

	boolean isDirected;
	Construct from;
	Construct to;
	double weight;

	/*
	 * (non-Javadoc)
	 * 
	 * @see behaviour.psychological.Relationship#getDirected()
	 */
	@Override
	public boolean getDirected()
	{
		// TODO Auto-generated method stub
		return this.isDirected;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see behaviour.psychological.Relationship#getFromConstruct()
	 */
	@Override
	public Construct getFromConstruct()
	{
		// TODO Auto-generated method stub
		return this.from;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see behaviour.psychological.Relationship#getToConstruct()
	 */
	@Override
	public Construct getToConstruct()
	{
		// TODO Auto-generated method stub
		return this.to;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see behaviour.psychological.Relationship#getWeight()
	 */
	@Override
	public double getWeight()
	{
		// TODO Auto-generated method stub
		return this.weight;
	}

	public SimpleRelationship(Construct from, Construct to, double weight, boolean directed)
	{
		this.from = from;
		this.to = to;
		this.weight = weight;
		this.isDirected = directed;
	}

	@Override
	public String toString()
	{
		StringBuilder message = new StringBuilder();
		message.append(super.toString());
		message.append("\n");
		message.append("This is a");
		if (this.isDirected)
		{
			message.append("directed");
		}
		else
		{
			message.append("undirected");
		}
		message.append("relationship from " + this.from.getName() + " to " + this.to.getName() + " with weight: " + this.weight);
		return message.toString();
	}

}
