package uk.ac.dmu.iesd.cascade;

import java.awt.Component;
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

import org.jfree.chart.ChartPanel;


import repast.simphony.context.Context;
import repast.simphony.engine.controller.ControllerActionVisitor;
import repast.simphony.engine.controller.NullAbstractControllerAction;
import repast.simphony.engine.environment.GUIRegistry;
import repast.simphony.engine.environment.GUIRegistryType;
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



/**
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/07/18 12:00:00 $
 *
 */
public class Initializer implements ModelInitializer{


	class CascadeNullAbstractControllerAction extends NullAbstractControllerAction {
		

		//@Override
		public void runInitialize(RunState runState, Context context, Parameters runParams) {
			//System.out.println("Begining of runInitialize");
			//System.out.println("this is runInitialize method test");
			// will be executed at initialization.
			CascadeContext cascadeContext = (CascadeContext) context;
			//System.out.println("Initializer:: runInitialize method test: "+cascadeContext.getTickPerDay());
			GUIRegistry guiRegis = runState.getGUIRegistry();

			//Collection <Pair<GUIRegistryType,Collection<JComponent>>> typeAndComp = guiRegis.getTypesAndComponents();
			Collection typeAndComp = guiRegis.getTypesAndComponents();
			//Iterator <Pair<GUIRegistryType,Collection<JComponent>>> typeAndCompIter = typeAndComp.iterator();
			Iterator<Pair> typeAndCompIter = typeAndComp.iterator();

			while ( typeAndCompIter.hasNext() ){
				Pair <GUIRegistryType,Collection<JComponent>> typeAndCompPair = typeAndCompIter.next();
				GUIRegistryType guiRegisType = typeAndCompPair.getFirst();
				//System.out.println("guiRegisType: "+ guiRegisType);
				if (guiRegisType == GUIRegistryType.CHART) {
					Collection <JComponent> chartCollection = typeAndCompPair.getSecond();  
					cascadeContext.setChartCompCollection(chartCollection);
					
					
				}
				if (guiRegisType == GUIRegistryType.DISPLAY) {

				}
				Collection <JComponent> compCol = typeAndCompPair.getSecond();
				//System.out.println("compCol: "+ compCol);
				Iterator<JComponent> compIter= compCol.iterator();
				while ( compIter.hasNext() ){
					JComponent comp = compIter.next();
					if (guiRegisType == GUIRegistryType.CHART) {
						//System.out.println("chartTitle: "+((ChartPanel) comp).getChart().getTitle().getText());
					}
					// System.out.print("Comp Name: "+ comp.getName());
					// System.out.print(" Comp toolTipText: "+ comp.getToolTipText() );
					//System.out.print(" Comp Count: "+ comp.getComponentCount());
					if (comp.getComponentCount()>0) {
						//System.out.print(" SubComp: "+ comp.getComponent(0));
					}
					//System.out.println(" Comp class: "+ comp.getClass());
					
					if(Consts.DEBUG)
					{
						if (Consts.DEBUG_OUTPUT_FILE != "")
						{
						    File file  = new File(Consts.DEBUG_OUTPUT_FILE);
						    PrintStream printStream;
							try {
								printStream = new PrintStream(new FileOutputStream(file));
								System.setOut(printStream);
							    System.out.println("Redirected System.out to this file, namely " + Consts.DEBUG_OUTPUT_FILE);

							} catch (FileNotFoundException e) {
								System.err.println("Couldn't find file with name " + Consts.DEBUG_OUTPUT_FILE);
								System.err.println("Therefore cannot re-direct System.out to this file ");
							}						    
						}
					}

				}

			} 


			List<IDisplay> listOfDisplays =  guiRegis.getDisplays();
			for (IDisplay display : listOfDisplays) {

				if (display instanceof DisplayOGL2D)
				{
					((DisplayOGL2D) display).addProbeListener(new ProsumerProbeListener(cascadeContext));
				}
			}


			//++++++++++++++Add stuff to user panel
			JPanel customPanel = new JPanel();
			customPanel.add(new JLabel("TestLabel"));
			customPanel.add(new JButton("Test Button"));		
			RSApplication.getRSApplicationInstance().addCustomUserPanel(customPanel);
			
			//runParams.
		   // System.out.println("Initializer:: ChartSnapshotInterval: "+runParams.getValue("chartSnapshotInterval"));
			//+++++++++++++++++++++++++++++++++++++



		}


		public String toString() {
			return "Custom Action Test";
		}

		public void accept(ControllerActionVisitor visitor) {
			//System.out.println("Initializer:: accept test "+visitor.toString());

		}
	}

	public void initialize(Scenario scen, RunEnvironmentBuilder builder) {

		scen.addMasterControllerAction(new CascadeNullAbstractControllerAction()); 

	}

}

