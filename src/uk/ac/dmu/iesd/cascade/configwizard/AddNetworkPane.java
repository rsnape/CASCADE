/**
 * 
 */
package uk.ac.dmu.iesd.cascade.configwizard;

import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;

import org.w3c.dom.Document;

/**
 * @author jsnape
 * 
 */
public class AddNetworkPane extends WizardWorkingPane
{

	/**
	 * @param configObject
	 */
	public AddNetworkPane(Document configObject)
	{
		super();
		this.setName("Add a Network");
		TitledBorder title;
		title = BorderFactory.createTitledBorder(this.getName());
		this.setBorder(title);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.dmu.iesd.cascade.configwizard.WizardWorkingPane#validateAndSaveToConfig
	 * ()
	 */
	@Override
	public boolean validateAndSaveToConfig()
	{
		// TODO Auto-generated method stub
		return true;
	}

}
