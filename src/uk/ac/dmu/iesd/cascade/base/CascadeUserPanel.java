/**
 * 
 */
package uk.ac.dmu.iesd.cascade.base;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import uk.ac.dmu.iesd.cascade.configwizard.WizardFrame;
import uk.ac.dmu.iesd.cascade.context.CascadeConfigContextBuilder;

/**
 * @author jsnape
 *
 */
public class CascadeUserPanel extends JPanel implements ActionListener, PropertyChangeListener
{

	private File builderFile;
	private JButton openButton, designButton, buildButton;
	private JFileChooser contextConfigFileChooser;
	private JFormattedTextField fileNameBox = new JFormattedTextField();
	private JLabel label = new JLabel("Choose context config file:");
	private CascadeConfigContextBuilder currentCascadeConfig;


	/**
	 * 
	 */
	public CascadeUserPanel()
	{
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		//++++++++++++++Add stuff to user panel
		this.add(label);
		JPanel loadFromFile = new JPanel();
		contextConfigFileChooser = new JFileChooser();
		fileNameBox.setColumns(40);
		loadFromFile.add(fileNameBox);
		//loadFromFile.add(contextConfigFileChooser);
		openButton = new JButton();
		openButton.setIcon(new ImageIcon("C:\\RepastSimphony-2.0-beta\\workspace\\Cascade\\icons\\document-open-2.png"));
		loadFromFile.add(openButton);
		this.add(loadFromFile);
		designButton = new JButton("Build Experiment from Scratch");
		this.add(designButton);		
		this.add(new JSeparator(JSeparator.HORIZONTAL),BorderLayout.LINE_START);
		buildButton = new JButton("Build from config now");
		this.add(buildButton);
		openButton.addActionListener(this);
		buildButton.addActionListener(this);
		fileNameBox.addPropertyChangeListener("value",this);
		designButton.addActionListener(this);

	}

	private void openFileDialog()
	{
	    int returnVal = contextConfigFileChooser.showOpenDialog(null);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	       this.builderFile = contextConfigFileChooser.getSelectedFile();
	       this.fileNameBox.setText(this.builderFile.getAbsolutePath());
	       this.fileNameBox.setValue(this.builderFile.getAbsolutePath());
	    }
	}
	
	private void triggerDesigner()
	{
		currentCascadeConfig = new CascadeConfigContextBuilder();
		JFrame wizardStarter = new WizardFrame();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
        //Handle open button action.
        if (e.getSource() == openButton) {
            this.openFileDialog();
            }
        //Handle save button action.
        else if (e.getSource() == buildButton) {
            if (this.builderFile != null)
            {
            	System.err.println("PLACEHOLDER!!! this is where we'll call the XML reader on file "+ this.builderFile.getAbsolutePath());
            }
        }
        else if (e.getSource() == designButton) {
        	triggerDesigner();
        }
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent eProp)
	{		
	    Object source = eProp.getSource();
	    if (source == fileNameBox) 
	    {
	        String fName = (String)fileNameBox.getValue();
	        if (fName == null)
	        {
	        	fName = fileNameBox.getText();
	        }
	        if (builderFile == null || builderFile.getAbsolutePath() != fName)
	        {
	        	File newFile = new File(fName);
	        	if (newFile.isFile())
	        	{
	        		builderFile = newFile;
	        	}
	        	else
	        	{
	        		builderFile = null;
	        	}
	        }
	        
	        fileNameBox.setValue(fName);
	    }
	}

}
