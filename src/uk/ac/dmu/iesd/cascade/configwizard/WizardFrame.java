/**
 * 
 */
package uk.ac.dmu.iesd.cascade.configwizard;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import repast.simphony.ui.RSApplication;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;

/**
 * @author jsnape
 *
 */
public class WizardFrame extends JFrame
{		
	protected static final int WIZARD_WINDOW_DEFAULT_WIDTH = 650;
	protected static final int WIZARD_WINDOW_DEFAULT_HEIGHT = 450;
	protected static Set<Class<? extends AggregatorAgent>> aggregatorClasses = new HashSet<Class<? extends AggregatorAgent>>();
	protected static Set<Class<? extends ProsumerAgent>> prosumerClasses = new HashSet<Class<? extends ProsumerAgent>>();

	
	private Document config;
	
		public WizardFrame()
		{
			List<Class<?>> agentClasses = RSApplication.getRSApplicationInstance().getCurrentScenario().getContext().getAgentClasses(true);
			
			for (Class a : agentClasses)
			{
				if (AggregatorAgent.class.isAssignableFrom(a) && !Modifier.isAbstract(a.getModifiers()))
				{
					aggregatorClasses.add(a);
				}
				else if (ProsumerAgent.class.isAssignableFrom(a) && !Modifier.isAbstract(a.getModifiers()))
				{
					prosumerClasses.add(a);
				}
			}
		     
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			try
			{
				docBuilder = docFactory.newDocumentBuilder();
				config = docBuilder.newDocument();
				Element rootElement = config.createElement("context");
				config.appendChild(rootElement);
			} catch (ParserConfigurationException e)
			{
				System.err.println("Couldn't configure parser to produce an XML document");
				e.printStackTrace();
			}
	 

			
			this.setTitle("Cascade Context configuration wizard");
		    this.setSize(WIZARD_WINDOW_DEFAULT_WIDTH, WIZARD_WINDOW_DEFAULT_HEIGHT);
		    this.setLocationRelativeTo(null);
		    this.setDefaultCloseOperation(this.DISPOSE_ON_CLOSE); 
			WizardPane contentPanel = new WizardPane(this.config, WIZARD_WINDOW_DEFAULT_WIDTH, WIZARD_WINDOW_DEFAULT_HEIGHT);
		    this.getContentPane().add(contentPanel);		    
		    JList list = new JList();
		    list.setBounds(135, 107, 1, 1);
		    contentPanel.add(list);
		    this.setVisible(true);
		}
}
