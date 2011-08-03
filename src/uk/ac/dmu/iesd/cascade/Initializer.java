package uk.ac.dmu.iesd.cascade;

import java.awt.Component;
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



/**
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/07/18 12:00:00 $
 *
 */
public class Initializer implements ModelInitializer{


	class CascadeNullAbstractControllerAction extends NullAbstractControllerAction {

		//@Override
		public void runInitialize(RunState runState, Context context, Parameters runParams) {
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
			    	 // System.out.print("Comp Name: "+ comp.getName());
			    	 // System.out.print(" Comp toolTipText: "+ comp.getToolTipText() );
			    	  //System.out.print(" Comp Count: "+ comp.getComponentCount());
			    	  if (comp.getComponentCount()>0) {
			    		  //System.out.print(" SubComp: "+ comp.getComponent(0));
			    	  }
			    	  //System.out.println(" Comp class: "+ comp.getClass());
			    		  			    	
			      }
			     
			    } 

		
			List<IDisplay> listOfDisplays =  guiRegis.getDisplays();
			for (IDisplay display : listOfDisplays) {
				//System.out.println("Display.toString: "+display.toString());
				//DisplayOGL2D displayOGL2D = (DisplayOGL2D) display.getClass();
				//System.out.println("Display.class: "+display.getClass());
				//System.out.println("Display.getPanel: "+display.getPanel());
				//System.out.println("Display.getPanel getComp: "+display.getPanel().getComponents());
				//Component[] compArr = display.getPanel().getComponents();
				//for (int i=0; i<compArr.length;i++) {
					//System.out.println("comp: "+compArr[i].getClass().getName());
					//System.out.println("comp: "+compArr[i].);
					
				//}
				
				//System.out.println("Display.class: "+display.getClass().);
				
			}
			
			
			//++++++++++++++Add stuff to user panel
			JPanel customPanel = new JPanel();
			customPanel.add(new JLabel("TestLabel"));
			customPanel.add(new JButton("Test Button"));		
			RSApplication.getRSApplicationInstance().addCustomUserPanel(customPanel);
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

