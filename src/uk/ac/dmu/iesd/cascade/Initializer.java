package uk.ac.dmu.iesd.cascade;

import java.awt.Component;
import java.util.List;

import javax.swing.JComponent;

import repast.simphony.context.Context;
import repast.simphony.engine.controller.ControllerActionVisitor;
import repast.simphony.engine.controller.NullAbstractControllerAction;
import repast.simphony.engine.environment.GUIRegistry;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.scenario.ModelInitializer;
import repast.simphony.scenario.Scenario;
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
			List<IDisplay> listOfDisplays =  guiRegis.getDisplays();
			for (IDisplay display : listOfDisplays) {
				//System.out.println("Display.toString: "+display.toString());
				//DisplayOGL2D displayOGL2D = (DisplayOGL2D) display.getClass();
				//System.out.println("Display.class: "+display.getClass());
				//System.out.println("Display.getPanel: "+display.getPanel());
				//System.out.println("Display.getPanel getComp: "+display.getPanel().getComponents());
				Component[] compArr = display.getPanel().getComponents();
				for (int i=0; i<compArr.length;i++) {
					//System.out.println("comp: "+compArr[i].getClass().getName());
					//System.out.println("comp: "+compArr[i].);
					
				}
				
				//System.out.println("Display.class: "+display.getClass().);
				
			}
			 
			


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

