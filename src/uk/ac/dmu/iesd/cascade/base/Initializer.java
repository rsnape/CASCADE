package uk.ac.dmu.iesd.cascade.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/07/18 12:00:00 $
 * 
 */
public class Initializer implements ModelInitializer {

	// private TickListener tickListener = null;

	class CascadeGuiCustomiserAction extends
			NullAbstractControllerAction {

		// @Override
		public void runInitialize(RunState runState, Context context,
				Parameters runParams) {
			// if (Consts.DEBUG)
			this.mainContext.logger.trace("Begining of runInitialize");
			// if (Consts.DEBUG)
			this.mainContext.logger.trace("this is runInitialize method test");
			// will be executed at initialization.

			/*
			 * ISchedule schedule =
			 * RunEnvironment.getInstance().getCurrentSchedule();
			 * schedule.schedule(ScheduleParameters.createOneTime(-0.1), new
			 * IAction() { //0.001 Nick public void execute() { Parameters
			 * params = RunEnvironment.getInstance().getParameters(); Number
			 * seed = (Number)params.getValue(ParameterConstants.
			 * DEFAULT_RANDOM_SEED_USAGE_NAME);
			 * RandomHelper.setSeed(seed.intValue());
			 * 
			 * } });
			 */

			CascadeContext cascadeContext = (CascadeContext) context;
			// if (Consts.DEBUG)
			this.mainContext.logger.trace("Initializer:: runInitialize method test: "+cascadeContext.getTickPerDay());
				GUIRegistry guiRegis = runState.getGUIRegistry();

				RSApplication
						.getRSApplicationInstance()
						.getGui()
						.setTickCountFormatter(
								new TicksToDaysFormatter(cascadeContext));
				RSApplication.getRSApplicationInstance().getGui()
						.updateTickCountLabel(0);
				// Collection <Pair<GUIRegistryType,Collection<JComponent>>>
				// typeAndComp = guiRegis.getTypesAndComponents();
				Collection typeAndComp = guiRegis.getTypesAndComponents();
				// Iterator <Pair<GUIRegistryType,Collection<JComponent>>>
				// typeAndCompIter = typeAndComp.iterator();
				Iterator<Pair> typeAndCompIter = typeAndComp.iterator();

				while (typeAndCompIter.hasNext()) {
					Pair<GUIRegistryType, Collection<JComponent>> typeAndCompPair = typeAndCompIter
							.next();
					GUIRegistryType guiRegisType = typeAndCompPair.getFirst();
					this.mainContext.logger.trace("guiRegisType: "+
					// guiRegisType);
					if (guiRegisType == GUIRegistryType.CHART) {
						Collection<JComponent> chartCollection = typeAndCompPair
								.getSecond();
						cascadeContext.setChartCompCollection(chartCollection);

					}
					if (guiRegisType == GUIRegistryType.DISPLAY) {

					}
					Collection<JComponent> compCol = typeAndCompPair
							.getSecond();
					this.mainContext.logger.trace("compCol: "+
					// compCol);
					Iterator<JComponent> compIter = compCol.iterator();
					while (compIter.hasNext()) {
						JComponent comp = compIter.next();
						if (guiRegisType == GUIRegistryType.CHART) {
							// if (Consts.DEBUG)
							this.mainContext.logger.trace("chartTitle: "+((ChartPanel)
							// comp).getChart().getTitle().getText());
						}
						// if (Consts.DEBUG) System.out.print("Comp Name: "+
						// comp.getName());
						// if (Consts.DEBUG)
						// System.out.print(" Comp toolTipText: "+
						// comp.getToolTipText() );
						// if (Consts.DEBUG) System.out.print(" Comp Count: "+
						// comp.getComponentCount());
						if (comp.getComponentCount() > 0) {
							// if (Consts.DEBUG) System.out.print(" SubComp: "+
							// comp.getComponent(0));
						}
						this.mainContext.logger.trace(" Comp class: "+
						// comp.getClass());
					}

				}

				List<IDisplay> listOfDisplays = guiRegis.getDisplays();
				for (IDisplay display : listOfDisplays) {

					if (display instanceof DisplayOGL2D) {
						((DisplayOGL2D) display)
								.addProbeListener(new ProsumerProbeListener(
										cascadeContext));
					}
				}

				// ++++++++++++++Add stuff to user panel
				JPanel customPanel = new JPanel();
				customPanel.add(new JLabel("TestLabel"));
				customPanel.add(new JButton("Test Button"));
				JLabel dayCountLabel = new JLabel();
				dayCountLabel.setText("");

				/*
				 * if (tickListener != null) {
				 * tickListener.tickCountUpdated(mainContext.getTickCount()); }
				 */

				RSApplication.getRSApplicationInstance().addCustomUserPanel(
						customPanel);

			//	RSApplication.getRSApplicationInstance().getGui().setTickCountFormatter(new TicksToDaysFormatter(cascadeContext));
				RSApplication.getRSApplicationInstance().getGui()
						.updateTickCountLabel(0);

				// runParams.
				// if (Consts.DEBUG)
				this.mainContext.logger.trace("Initializer:: ChartSnapshotInterval: "+runParams.getValue("chartSnapshotInterval"));
				// +++++++++++++++++++++++++++++++++++++


			if (Consts.DEBUG) {
				if (Consts.DEBUG_OUTPUT_FILE != "") {
					File file = new File(Consts.DEBUG_OUTPUT_FILE);
					PrintStream printStream;
					try {
						printStream = new PrintStream(
								new FileOutputStream(file));
						System.setOut(printStream);
						if (Consts.DEBUG)
						{
							System.out
									.println("Redirected System.out to this file, namely "
											+ Consts.DEBUG_OUTPUT_FILE);
							this.mainContext.logger.debug("Initializer action added with batch mode = " + runState.getRunInfo().isBatch());
						}

					} catch (FileNotFoundException e) {
						System.err.println("Couldn't find file with name "
								+ Consts.DEBUG_OUTPUT_FILE);
						System.err
								.println("Therefore cannot re-direct System.out to this file ");
					}
				}
			}
		}

		public String toString() {
			return "Custom Action Test";
		}

		public void accept(ControllerActionVisitor visitor) {
			// if (Consts.DEBUG)
			this.mainContext.logger.trace("Initializer:: accept test "+visitor.toString());

		}
	}

	public void initialize(Scenario scen, RunEnvironmentBuilder builder) {
			scen.addMasterControllerAction(new CascadeGuiCustomiserAction());

	}

}
