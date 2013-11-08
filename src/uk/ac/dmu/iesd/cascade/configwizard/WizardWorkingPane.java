/**
 * 
 */
package uk.ac.dmu.iesd.cascade.configwizard;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.w3c.dom.Element;

/**
 * @author jsnape
 *
 */
public abstract class WizardWorkingPane extends JPanel
{
	protected abstract boolean validateAndSaveToConfig();
	
	private Element workingElement = null;
	
	public Element getWorkingElement()
	{
		return this.workingElement;
	}

	/**
	 * @param workingElement the workingElement to set
	 */
	public void setWorkingElement(Element workingElement)
	{
		this.workingElement = workingElement;
	}
}
