/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.awt.Font;

import javax.swing.JTextArea;

/**
 * @author jsnape
 *
 */
public class GUIUtils
{
	
	public static JTextArea makeWrappableLabel(String labelText)
	{
		JTextArea wrappedLabel = new JTextArea(labelText);
		wrappedLabel.setLineWrap(true);
		wrappedLabel.setWrapStyleWord(true);
		wrappedLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		wrappedLabel.setEditable(false);
		wrappedLabel.setBackground(null);
		return wrappedLabel;
	}

}
