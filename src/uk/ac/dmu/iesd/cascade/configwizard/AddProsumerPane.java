/**
 * 
 */
package uk.ac.dmu.iesd.cascade.configwizard;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;

/**
 * @author jsnape
 * 
 */
public class AddProsumerPane extends WizardWorkingPane implements ActionListener
{
	JComboBox proList;
	JTextField numAgents;
	ArrayList prList;
	Document configObject;
	private ButtonGroup opts;

	/**
	 * @param thisConfig
	 * @param currentAggregator2
	 */
	public AddProsumerPane(Document thisConfig, Element currentAggregator2)// ,
																			// AggregatorAgent
																			// agg)
	{
		super();
		// currentAggregator = agg;
		this.setWorkingElement(currentAggregator2);
		String sName = currentAggregator2.getAttribute("shortName");
		this.setName("Add Prosumers for aggregator " + sName);// +currentAggregator.getAgentName());
		TitledBorder title;
		title = BorderFactory.createTitledBorder(this.getName());
		this.setBorder(title);

		this.configObject = thisConfig;
		this.setSize(WizardFrame.WIZARD_WINDOW_DEFAULT_WIDTH, WizardFrame.WIZARD_WINDOW_DEFAULT_HEIGHT);

		JPanel line1 = new JPanel();
		line1.setBounds(8, 25, this.getWidth() - 20, 60);
		JTextArea proLabel = new JTextArea("Choose the prosumer class here");
		proLabel.setLineWrap(true);
		proLabel.setWrapStyleWord(true);
		proLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		proLabel.setEditable(false);
		proLabel.setBackground(null);
		proLabel.setBounds(10, 0, 125, 44);
		this.proList = new JComboBox(WizardFrame.prosumerClasses.toArray());
		// aggList.setMinimumSize(new Dimension(400,25));
		this.proList.setBounds(144, 0, line1.getWidth() - 170, 25);
		// line1.setLayout(new BoxLayout(line1,BoxLayout.X_AXIS));
		line1.setLayout(null);
		line1.add(proLabel);
		line1.add(this.proList);

		JPanel line2 = new JPanel();
		line2.setBounds(8, 95, this.getWidth() - 20, 60);
		line2.setAlignmentX(Component.LEFT_ALIGNMENT);
		JTextArea numLabel = new JTextArea("How many of these aggregators should be created?");
		numLabel.setLineWrap(true);
		numLabel.setWrapStyleWord(true);
		numLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		numLabel.setEditable(false);
		numLabel.setBackground(null);
		numLabel.setBounds(10, 8, 239, 32);
		this.numAgents = new JTextField("1");
		this.numAgents.setBounds(264, 5, this.getWidth() - 310, 25);
		line2.setLayout(null);
		line2.add(numLabel);
		line2.add(this.numAgents);

		JPanel configOptions = new JPanel();
		configOptions.setBounds(8, 164, 249, 99);
		TitledBorder optBorder = BorderFactory.createTitledBorder("Configuration options");
		configOptions.setBorder(optBorder);
		configOptions.setLayout(new BoxLayout(configOptions, BoxLayout.Y_AXIS));
		JRadioButton opt1 = new JRadioButton("Use default configuration", true);
		// opt1.setActionCommand(this.NO_CONFIG);
		opt1.addActionListener(this);
		JRadioButton opt2 = new JRadioButton("Set config to apply to all these aggregators", false);
		// opt2.setActionCommand(this.CONFIG_ALL);
		opt2.addActionListener(this);
		JRadioButton opt3 = new JRadioButton("Configure each of these individually", false);
		// opt3.setActionCommand(this.CONFIG_INDIVIDUAL);
		opt3.addActionListener(this);

		this.opts = new ButtonGroup();
		this.opts.add(opt1);
		this.opts.add(opt2);
		this.opts.add(opt3);
		configOptions.add(opt1);
		configOptions.add(opt2);
		configOptions.add(opt3);
		this.setLayout(null);

		this.add(line1);
		this.add(line2);
		this.add(configOptions);
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

		boolean validatesOK = true;
		int n = 0;
		Class<? extends ProsumerAgent> selectedProsumer = null;

		String num = this.numAgents.getText();
		try
		{
			n = Integer.parseInt(num);
		}
		catch (NumberFormatException e)
		{
			validatesOK = false;
		}

		if (this.proList.getSelectedIndex() >= 0)
		{
			selectedProsumer = (Class<? extends ProsumerAgent>) this.proList.getSelectedItem();
		}
		else
		{
			validatesOK = false;
		}

		if (validatesOK)
		{
			Element agg = this.getWorkingElement();
			Element proElement = this.configObject.createElement("prosumer");
			Attr className = this.configObject.createAttribute("class");
			className.setValue(selectedProsumer.getName());
			Attr sName = this.configObject.createAttribute("shortName");
			sName.setValue(selectedProsumer.getSimpleName());
			Attr numAggs = this.configObject.createAttribute("number");
			numAggs.setValue(num);
			proElement.setAttributeNode(className);
			proElement.setAttributeNode(numAggs);
			proElement.setAttributeNode(sName);
			this.getWorkingElement().appendChild(proElement);

			this.setWorkingElement(proElement);
		}

		return validatesOK;
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
		// TODO Auto-generated method stub

	}

}
