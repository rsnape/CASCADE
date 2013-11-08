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
public class AddInputFilePane extends WizardWorkingPane
{

	/**
	 * @param configObject
	 */
	public AddInputFilePane(Document configObject)
	{
		super();
		this.setName("Add a context data file");
		TitledBorder title;
		title = BorderFactory.createTitledBorder(this.getName());
		this.setBorder(title);
	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.configwizard.WizardWorkingPane#validateAndSaveToConfig()
	 */
	@Override
	public boolean validateAndSaveToConfig()
	{
		// TODO Auto-generated method stub
		return true;
	}

}
