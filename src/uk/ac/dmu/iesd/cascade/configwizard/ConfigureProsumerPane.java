/**
 * 
 */
package uk.ac.dmu.iesd.cascade.configwizard;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author jsnape
 *
 */
public class ConfigureProsumerPane extends WizardWorkingPane implements ActionListener
{
	
	class AttrComponent extends JPanel
	{
		private JTextArea label;
		private JTextField value;
		/**
		 * @param label the label to set
		 */
		public void setLabel(JTextArea label)
		{
			this.label = label;
			this.add(label);
		}
		/**
		 * @return the label
		 */
		public JTextArea getLabel()
		{
			return label;
		}
		/**
		 * @param value the value to set
		 */
		public void setValue(JTextField value)
		{
			this.value = value;
			this.add(value);
		}
		/**
		 * @return the value
		 */
		public JTextField getValue()
		{
			return value;
		}		
	}
	
	JComboBox aggList;
	JTextField numAgents;
	JButton addProsumersToThisAggregator;
	Document configObject;
	ArrayList createdAggregators;
	ButtonGroup opts;
	static String NO_CONFIG = "0";
	static String CONFIG_ALL = "1";
	static String CONFIG_INDIVIDUAL = "2";
	private int configType;
	
	private JPanel contents;
	
	/**
	 * @param configObject2
	 */
	public ConfigureProsumerPane(Document configObject2, Element workingElement)
	{
		super();
		configObject = configObject2;
		this.setWorkingElement(workingElement);
		String proClassName = workingElement.getAttribute("shortName");
		
		this.setName("Configure Prosumer of type " + proClassName);
		TitledBorder title;
		title = BorderFactory.createTitledBorder(this.getName());
		this.setBorder(title);
		this.setSize(WizardFrame.WIZARD_WINDOW_DEFAULT_WIDTH,WizardFrame.WIZARD_WINDOW_DEFAULT_HEIGHT);
		
		this.contents = new JPanel();
		contents.setLayout(new BoxLayout(contents,BoxLayout.Y_AXIS));
		if (proClassName.equals("GenericBMPxTraderAggregator"))
		{
			JPanel confRow = createAtributeConf("type");
			contents.add(confRow);
		}
		else
		{
			JTextArea noConfigMessage = new JTextArea("No config required for this aggregator type");
			contents.add(noConfigMessage);
		}
		
		this.add(contents);
	}

	
	
	/**
	 * @param string
	 * @return
	 */
	private JPanel createAtributeConf(String s)
	{
		AttrComponent retPanel = new AttrComponent();
		retPanel.setLayout(new BoxLayout(retPanel,BoxLayout.X_AXIS));
		retPanel.setLabel(new JTextArea(s));
		retPanel.setValue(new JTextField("Replace this with field value"));
		return retPanel;
	}




	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.configwizard.WizardWorkingPane#validateAndSaveToConfig()
	 */
	@Override
	public boolean validateAndSaveToConfig()
	{
		boolean validatesOK = true;
		
		
		if (validatesOK)
		{
			for (Component c : contents.getComponents())
			{
				if (c instanceof AttrComponent)
				{
					AttrComponent a = (AttrComponent) c;
					Attr attr = configObject.createAttribute(a.getLabel().getText());
					attr.setValue(a.getValue().getText());
					this.getWorkingElement().setAttributeNode(attr);
				}
			}
			
			System.out.println("Configuration attribute added to DOM document");
			/**
			 * Old idea to instatiate the objects here - better to put it into a file and then run from the file
			 */
			
			this.setWorkingElement((Element)this.getWorkingElement().getParentNode());
/*			createdAggregators = new ArrayList();
			for (int i =0; i < n; i++)
			{
				try
				{
					
					createdAggregators.add(selectedAggregator.newInstance());
				} catch (InstantiationException e)
				{
					System.err.println(e.getMessage());
					e.printStackTrace();
				} catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}*/
			
		}
		
		return validatesOK;
	}



	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		if (arg0.getSource() == addProsumersToThisAggregator)
		{
			WizardPane thisPane = (WizardPane)this.getParent();
			validateAndSaveToConfig();
			thisPane.remove(this);
			thisPane.add(new AddProsumerPane(configObject, this.getWorkingElement()));
		}
		else if (arg0.getSource() instanceof JRadioButton)
		{
			configType = Integer.parseInt(((JRadioButton)arg0.getSource()).getActionCommand());
		}

		
	}

	protected JTextField getNumAgents() {
		return numAgents;
	}
}
