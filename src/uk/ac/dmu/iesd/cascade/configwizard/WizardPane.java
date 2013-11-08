/**
 * 
 */
package uk.ac.dmu.iesd.cascade.configwizard;

import java.awt.FileDialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.dmu.iesd.cascade.context.CascadeConfigContextBuilder;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author jsnape
 * 
 */
public class WizardPane extends JPanel implements ActionListener
{
	private JButton addProsumers, addAgents, addContextFiles, next, finish;
	JPanel buttonPanel;
	WizardWorkingPane workingPanel;
	private Document configObject;
	private Object configProsumersPanel;
	private static final int DEFAULT_BUTTON_WIDTH = 100;
	private static final int DEFAULT_BUTTON_HEIGHT = 50;
	private static final int DEFAULT_MARGIN = 50;

	/**
	 * @wbp.parser.constructor *
	 */
	public WizardPane(Document thisConfig)
	{
		this(thisConfig, WizardFrame.WIZARD_WINDOW_DEFAULT_WIDTH, WizardFrame.WIZARD_WINDOW_DEFAULT_HEIGHT);
	}

	public WizardPane(Document thisConfig, int w, int h)
	{
		this.configObject = thisConfig;
		this.setSize(w, h);
		this.setLocation(0, 0);
		this.setLayout(null);
		this.buttonPanel = new JPanel();
		this.buttonPanel.setSize(this.getWidth(), this.DEFAULT_BUTTON_HEIGHT + 2 * this.DEFAULT_MARGIN);
		this.buttonPanel.setLocation(0, this.getHeight() - buttonPanel.getHeight());
		this.buttonPanel.setLayout(null);

		this.addAgents = createBottomButton(buttonPanel, "Add Another Aggregator", 50, this.DEFAULT_MARGIN);
		this.addProsumers = createBottomButton(buttonPanel, "Add Prosumers to Aggregator", 200, this.DEFAULT_MARGIN);
		this.addContextFiles = createBottomButton(buttonPanel, "Add Context Files", 350, this.DEFAULT_MARGIN);
		this.buttonPanel.removeAll();

		this.finish = createBottomButton(buttonPanel, "Finish", 500, this.DEFAULT_MARGIN);
		this.finish.setVerticalAlignment(SwingConstants.CENTER);
		this.next = createBottomButton(buttonPanel, "Next", 50, this.DEFAULT_MARGIN);
		this.next.setVerticalAlignment(SwingConstants.CENTER);

		this.buttonPanel.setVisible(true);
		addAgents.doClick();
		this.add(this.buttonPanel);
		this.add(this.workingPanel);
		this.setVisible(true);
	}

	/**
	 * @param addObjectsButtonPanel2
	 * @param string
	 * @param i
	 * @param j
	 * @return
	 */
	private JButton createBottomButton(JPanel addObjectsButtonPanel2,
			String label, int i, int j)
	{
		JButton wizardButton = new JButton("<html><center>" + label + "</center></html>");
		wizardButton.setVerticalAlignment(SwingConstants.TOP);
		addObjectsButtonPanel2.add(wizardButton);
		wizardButton.setLocation(i, j);
		wizardButton.setSize(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT);
		wizardButton.addActionListener(this);
		return wizardButton;
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
		Object src = e.getSource();

		if (src instanceof JButton)
		{
			boolean carryOn = true;

			if (this.workingPanel != null)
			{
				boolean validatedOK = this.workingPanel.validateAndSaveToConfig();
				if (!validatedOK)
				{
					carryOn = triggerWarning();
				}
			}

			if (carryOn)
			{
				WizardWorkingPane nextPane = null;
				if (src == this.addAgents)
				{
					nextPane = new AddAggregatorPane(this.configObject);
				} else if (src == this.addContextFiles)
				{
					nextPane = new AddInputFilePane(this.configObject);

				} else if (src == this.addProsumers)
				{
					Element currentAggregator = this.workingPanel.getWorkingElement();
					nextPane = new AddProsumerPane(this.configObject, currentAggregator);
				} else if (src == this.next)
				{
					if (this.workingPanel instanceof AddAggregatorPane)
					{
						Element currentAggregator = this.workingPanel.getWorkingElement();
						nextPane = new ConfigureAggregatorPane(this.configObject, currentAggregator);
					} else
					{
						nextPane = new ConfigureProsumerPane(this.configObject, this.workingPanel.getWorkingElement());
					}
				}

				if (nextPane != null)
				{

					this.buttonPanel.removeAll();
					if (src == this.next)
					{
						this.buttonPanel.add(this.addContextFiles);
						this.buttonPanel.add(this.addAgents);
						this.buttonPanel.add(this.addProsumers);
					} else
					{
						this.buttonPanel.add(this.next);
					}

					this.buttonPanel.add(this.finish);
					this.buttonPanel.validate();
					this.buttonPanel.repaint();

					replacePanes(nextPane);
				}

				if (src == this.finish)
				{
					JFileChooser fDialog = new JFileChooser(System.getProperty("user.dir"));
					fDialog.setFileFilter(new FileNameExtensionFilter("XML Files","*.xml"));
					
					int returnVal = fDialog.showSaveDialog(null);

					// JFrame saveAs = new JFrame();
					// FileDialog fDialog = new FileDialog(saveAs, "Save",
					// FileDialog.SAVE);
					// fDialog.setFile("*.xml");
					// saveAs.add(fDialog);
					if (returnVal == JFileChooser.APPROVE_OPTION && fDialog.getSelectedFile() != null)
					{
						File f = fDialog.getSelectedFile();

						TransformerFactory transformerFactory = TransformerFactory.newInstance();
						Transformer transformer = null;
						try
						{
							transformer = transformerFactory.newTransformer();
						} catch (TransformerConfigurationException e1)
						{
							System.err.println("Couldn't configure Transformer to turn configuration into file");
							e1.printStackTrace();
						}

						transformer.setOutputProperty(OutputKeys.METHOD, "xml");
						transformer.setOutputProperty(OutputKeys.INDENT, "yes");
						DOMSource source = new DOMSource(this.configObject);
						StreamResult result = new StreamResult(f);

						// Output to console for testing
						// StreamResult result = new StreamResult(System.out);

						try
						{
							transformer.transform(source, result);
						} catch (TransformerException e1)
						{
							System.err.println("Failed to convert the configuration Document object into a file");
							e1.printStackTrace();
						}

						Window w = SwingUtilities.getWindowAncestor(this);
						w.dispose();
					}
				}
			}
		}

	}

	/**
	 * @return
	 */
	private boolean triggerWarning()
	{
		JFrame warningFrame = new JFrame();
		Object[] options = { "Yes", "No" };
		int n = JOptionPane.showOptionDialog(warningFrame, "The data in this window failed to validate and therefore will not be added.  Do you wish to continue?", "Validation failure", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

		return n == 0 ? true : false;
	}

	/**
	 * @param nextPane
	 */
	private void replacePanes(WizardWorkingPane nextPane)
	{
		if (this.workingPanel != null)
		{
			this.remove(this.workingPanel);
			this.validateTree();
		}
		this.workingPanel = nextPane;
		this.workingPanel.setSize(this.getWidth(), this.getHeight() - this.buttonPanel.getHeight());
		this.workingPanel.setLocation(0, 0);
		this.workingPanel.setVisible(true);
		this.workingPanel.validate();

		this.add(this.workingPanel);
		this.validateTree();
		this.setVisible(true);
		this.repaint();

	}

}
