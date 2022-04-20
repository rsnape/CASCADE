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
import javax.swing.SwingConstants;

import repast.simphony.userpanel.ui.UserPanelCreator;
import uk.ac.dmu.iesd.cascade.configwizard.WizardFrame;
import uk.ac.dmu.iesd.cascade.context.CascadeConfigContextBuilder;

/**
 * @author jsnape
 * 
 */
public class CascadeUserPanel extends JPanel implements ActionListener, PropertyChangeListener, UserPanelCreator
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
		// ++++++++++++++Add stuff to user panel
		this.add(this.label);
		JPanel loadFromFile = new JPanel();
		this.contextConfigFileChooser = new JFileChooser();
		this.fileNameBox.setColumns(40);
		loadFromFile.add(this.fileNameBox);
		// loadFromFile.add(contextConfigFileChooser);
		this.openButton = new JButton();
		this.openButton.setIcon(new ImageIcon("C:\\RepastSimphony-2.0-beta\\workspace\\Cascade\\icons\\document-open-2.png"));
		loadFromFile.add(this.openButton);
		this.add(loadFromFile);
		this.designButton = new JButton("Build Experiment from Scratch");
		this.add(this.designButton);
		this.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.LINE_START);
		this.buildButton = new JButton("Build from config now");
		this.add(this.buildButton);
		this.openButton.addActionListener(this);
		this.buildButton.addActionListener(this);
		this.fileNameBox.addPropertyChangeListener("value", this);
		this.designButton.addActionListener(this);

	}

	private void openFileDialog()
	{
		int returnVal = this.contextConfigFileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			this.builderFile = this.contextConfigFileChooser.getSelectedFile();
			this.fileNameBox.setText(this.builderFile.getAbsolutePath());
			this.fileNameBox.setValue(this.builderFile.getAbsolutePath());
		}
	}

	private void triggerDesigner()
	{
		this.currentCascadeConfig = new CascadeConfigContextBuilder();
		JFrame wizardStarter = new WizardFrame();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		// Handle open button action.
		if (e.getSource() == this.openButton)
		{
			this.openFileDialog();
		}
		// Handle save button action.
		else if (e.getSource() == this.buildButton)
		{
			if (this.builderFile != null)
			{
				System.err.println("PLACEHOLDER!!! this is where we'll call the XML reader on file " + this.builderFile.getAbsolutePath());
			}
		}
		else if (e.getSource() == this.designButton)
		{
			this.triggerDesigner();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent eProp)
	{
		Object source = eProp.getSource();
		if (source == this.fileNameBox)
		{
			String fName = (String) this.fileNameBox.getValue();
			if (fName == null)
			{
				fName = this.fileNameBox.getText();
			}
			if (this.builderFile == null || this.builderFile.getAbsolutePath() != fName)
			{
				File newFile = new File(fName);
				if (newFile.isFile())
				{
					this.builderFile = newFile;
				}
				else
				{
					this.builderFile = null;
				}
			}

			this.fileNameBox.setValue(fName);
		}
	}

	/* (non-Javadoc)
	 * @see repast.simphony.userpanel.ui.UserPanelCreator#createPanel()
	 */
	@Override
	public JPanel createPanel() {
		// TODO Auto-generated method stub
		return this;
	}

}
