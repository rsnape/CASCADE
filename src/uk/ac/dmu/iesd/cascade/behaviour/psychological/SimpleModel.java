/**
 * 
 */
package uk.ac.dmu.iesd.cascade.behaviour.psychological;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import uk.ac.dmu.iesd.cascade.behaviour.*;


/**
 * @author jsnape
 *
 */
public class SimpleModel {
	
	private Vector<Construct> constructs = new Vector<Construct>();
	private Vector<Relationship> relationships = new Vector<Relationship>();
	private String modelName;

	public void addConstruct(Construct c)
	{
		this.constructs.add(c);
	}

	public void addRelationship(Relationship r)
	{
		this.relationships.add(r);
	}
	
	public void addConstructs(Collection<Construct> c)
	{
		this.constructs.addAll(c);
	}

	public void addRelationships(Collection<Relationship> r)
	{
		this.relationships.addAll(r);
	}
	
	public void addConstructs(Construct[] c)
	{
		this.constructs.addAll(Arrays.asList(c));
	}
	
	public void addRelationships(Relationship[] r)
	{
		this.relationships.addAll(Arrays.asList(r));
	}
	
	public void drawModel()
	{
		// TODO Add code here to plot out the model - either into a file
		// or to stdout
	}
	
	public SimpleModel()
	{
		super();
	}
	

	public SimpleModel(Construct[] constructs)
	{
		super();
		this.constructs.addAll(Arrays.asList(constructs));
	}
	
	public SimpleModel(Construct[] constructs, Relationship[] relationships)
	{
		super();
		this.constructs.addAll(Arrays.asList(constructs));
		this.relationships.addAll(Arrays.asList(relationships));
	}
	
	public String toString()
	{
		StringBuilder message = new StringBuilder();
		message.append(super.toString());
		message.append("\n");
		message.append("This is a psychological model named " + this.modelName);
		message.append("\n");
		message.append("It contains " + constructs.size() + "constructs, connected by " + relationships.size() + "relationships.");
		message.append("\n");
		message.append("Constructs: ");
		message.append(Arrays.toString(constructs.toArray()));
		message.append("\n");
		message.append("Relationships: ");
		message.append(Arrays.toString(relationships.toArray()));
		
		return message.toString();
	}

}
