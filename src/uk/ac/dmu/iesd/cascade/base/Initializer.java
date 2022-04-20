package uk.ac.dmu.iesd.cascade.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;

import repast.simphony.batch.BatchScenario;
import repast.simphony.context.Context;
import repast.simphony.engine.controller.ControllerActionVisitor;
import repast.simphony.engine.controller.NullAbstractControllerAction;
import repast.simphony.engine.environment.GUIRegistry;
import repast.simphony.engine.environment.GUIRegistryType;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.scenario.ModelInitializer;
import repast.simphony.scenario.Scenario;
import repast.simphony.ui.RSApplication;
import repast.simphony.util.collections.Pair;
import repast.simphony.visualization.IDisplay;
import repast.simphony.visualizationOGL2D.DisplayOGL2D;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.ui.ProsumerProbeListener;
import uk.ac.dmu.iesd.cascade.ui.TicksToDaysFormatter;

/**
 * This initializer class is implemented in order to perform some preparatory setup after
 * the model context is initialised, but prior to the run.  It is triggered when the Initialise
 * button is pressed on the GUI, or when the run starts.
 * 
 * In particular, this is used to customise the GUI prior to the model run.
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/07/18 12:00:00 $
 * 
 */
public class Initializer implements ModelInitializer
{
	/*
	 * Class to customize the GUI by adding an action with functionality in the `runInitialize()`
	 * method to change features as soon as initialize button is clicked.
	 */
	class CascadeGuiCustomiserAction extends NullAbstractControllerAction
	{
		/*
		 * This method is run prior to setting up model context.  In the CASCADE framework, it 
		 * customises the GUI to 
		 *     1a: Change the format of Ticks displayed into years/weeks/days
		 *     1b: Add a Probe to 2D displays as a hook to open other visualisations if required
		 *     1c: Add a User Panel - can be customised in the addUserPanel() method
		 * 
		 * (non-Javadoc)
		 * @see repast.simphony.engine.controller.NullAbstractControllerAction#runInitialize(repast.simphony.engine.environment.RunState, repast.simphony.context.Context, repast.simphony.parameter.Parameters)
		 */
		@Override
		public void runInitialize(RunState runState, Context context, Parameters runParams)
		{
			System.out.println("Begining of model Intializer runInitialize");
			
			// Assumes the main model context is an instance of CascadeContext, so cast it.
			CascadeContext cascadeContext = (CascadeContext) context;
			if (cascadeContext.logger.isTraceEnabled())
			{
				cascadeContext.logger.trace("Initializer:: runInitialize method entered, ticks per day = " + cascadeContext.getNbOfTickPerDay());
			}
			
			
			GUIRegistry guiRegis = runState.getGUIRegistry();

			RSApplication.getRSApplicationInstance().getGui().setTickCountFormatter(new TicksToDaysFormatter(cascadeContext));
			RSApplication.getRSApplicationInstance().getGui().updateTickCountLabel(0);
			Collection typeAndComp = guiRegis.getTypesAndComponents();
			Iterator<Pair> typeAndCompIter = typeAndComp.iterator();

			while (typeAndCompIter.hasNext())
			{
				Pair<GUIRegistryType, Collection<JComponent>> typeAndCompPair = typeAndCompIter.next();
				GUIRegistryType guiRegisType = typeAndCompPair.getFirst();
				if (cascadeContext.logger.isTraceEnabled())
				{
					cascadeContext.logger.trace("guiRegisType: " + guiRegisType);
				}
				
				//Add GUI components for Charts to a collection within the context, so that
				//the context is aware of the charts.
				if (guiRegisType == GUIRegistryType.CHART)
				{
					Collection<JComponent> chartCollection = typeAndCompPair.getSecond();
					cascadeContext.setChartCompCollection(chartCollection);
				}
				if (guiRegisType == GUIRegistryType.DISPLAY)
				{
					//Do nothing at this stage is component is a DISPLAY
				}
				
				Collection<JComponent> compCol = typeAndCompPair.getSecond();
				if (cascadeContext.logger.isTraceEnabled())
				{
					cascadeContext.logger.trace("compCol: " + compCol);
				}
				Iterator<JComponent> compIter = compCol.iterator();
				while (compIter.hasNext())
				{
					JComponent comp = compIter.next();
					if (guiRegisType == GUIRegistryType.CHART)
					{
						if (cascadeContext.logger.isTraceEnabled())
						{
							cascadeContext.logger.trace("chartTitle: " + ((ChartPanel) comp).getChart().getTitle().getText());
						}
					}

					if (cascadeContext.logger.isTraceEnabled())
					{
						cascadeContext.logger.trace(" Comp class: " + comp.getClass());
					}
				}
			}

			//Find 2D display and add a ProbeListener to it for when Prosumers are double clicked
			List<IDisplay> listOfDisplays = guiRegis.getDisplays();
			for (IDisplay display : listOfDisplays)
			{
				if (display instanceof DisplayOGL2D)
				{
					((DisplayOGL2D) display).addProbeListener(new ProsumerProbeListener(cascadeContext));
				}
			}

			// Add a custom user panel if needed
			JPanel customPanel = addUserPanel();
			RSApplication.getRSApplicationInstance().addCustomUserPanel(customPanel);

			if (cascadeContext.logger.isTraceEnabled())
			{
				cascadeContext.logger.trace("Initializer:: ChartSnapshotInterval: " + runParams.getValue("chartSnapshotInterval"));
			}
		}

		@Override
		public String toString()
		{
			return "Custom Action Test";
		}

		@Override
		public void accept(ControllerActionVisitor visitor)
		{
			// Place holder - possibly test this later
		}
		
		private JPanel addUserPanel()
		{
			JPanel customPanel = new JPanel();
			customPanel.add(new JLabel("TestLabel"));
			customPanel.add(new JButton("Test Button"));
			JLabel dayCountLabel = new JLabel();
			dayCountLabel.setText("");
			return customPanel;
		}
	}

	@Override
	public void initialize(Scenario scen, RunEnvironmentBuilder builder)
	{
		if (!scen.getClass().isInstance(BatchScenario.class))
		{
			scen.addMasterControllerAction(new CascadeGuiCustomiserAction());
		}
	}
}