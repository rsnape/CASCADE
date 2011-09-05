package uk.ac.dmu.iesd.cascade.ui;


import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.IAction;
import repast.simphony.engine.schedule.NonModelAction;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.ui.RSApplication;
import repast.simphony.ui.probe.Probe;
import repast.simphony.ui.probe.ProbePanelCreator;
import repast.simphony.visualization.ProbeEvent;
import repast.simphony.visualization.ProbeListener;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.context.HouseholdProsumer;

public class ProsumerProbeListener implements ProbeListener {

	/**
	 * Ordered ArrayList of charts spawned by this listener
	 */
	ArrayList<JFreeChart> charts = new ArrayList<JFreeChart>();
	/**
	 * Ordered ArrayList of agents that have been probed and called this listener
	 */
	ArrayList<HouseholdProsumer> probedAgents = new ArrayList<HouseholdProsumer>();

	  @NonModelAction
	  static class ProbeUpdater implements IAction {

	    private ProsumerProbeListener probeListener;

	    public ProbeUpdater(ProsumerProbeListener display) {
	      this.probeListener = display;
	    }

	    public void execute() {
	    	probeListener.scheduledUpdate();
	      
	    }
	  }

	
	@Override
	public void objectProbed(ProbeEvent evt) {
		// TODO Auto-generated method stub
		List<?> probedObjects = evt.getProbedObjects();

		for(Object thisObj : probedObjects)
		{
			if (thisObj instanceof HouseholdProsumer)
			{
				HouseholdProsumer thisAgent = (HouseholdProsumer) thisObj;
				probedAgents.add(thisAgent);
				ProbePanelCreator newProbe = new ProbePanelCreator(thisAgent);
				Probe myProbe = newProbe.getProbe("TestProbe", true);
				myProbe.addPropertyChangeListener(RSApplication.getRSApplicationInstance().getGui());
				JFrame agentProbeFrame = new JFrame("Prosumer probe frame for " + thisAgent.toString());
				agentProbeFrame.setAlwaysOnTop(true);
				agentProbeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				agentProbeFrame.getContentPane().setLayout(new GridLayout(2, 1));

				DefaultCategoryDataset tempDataset = createTemperatureDataset(thisAgent);
				// based on the dataset we create the chart
				JFreeChart tempChart = createTemperatureChart(tempDataset, "Previous day temperatures - " + thisAgent.getAgentName());
				charts.add(tempChart);
				// we put the chart into a panel
				ChartPanel tempChartPanel = new ChartPanel(tempChart);
				// default size
				tempChartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
				
				DefaultCategoryDataset dataset = createDataset(thisAgent);
				// based on the dataset we create the chart
				JFreeChart chart = createChart(dataset, "Previous day demand by type - " + thisAgent.getAgentName());
				charts.add(chart);
				// we put the chart into a panel
				ChartPanel chartPanel = new ChartPanel(chart);
				// default size
				chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

				agentProbeFrame.getContentPane().add(tempChartPanel);
				agentProbeFrame.getContentPane().add(chartPanel);

				//Display the window.
				agentProbeFrame.pack();
				agentProbeFrame.setVisible(true);
			}

		}
	}

	/**
	 * Creates a sample dataset 
	 */
	private  DefaultCategoryDataset createDataset(HouseholdProsumer thisAgent) {
		DefaultCategoryDataset result = new DefaultCategoryDataset();

		float[] arr1 = thisAgent.getHistoricalSpaceHeatDemand();
		float[] arr2 = thisAgent.getHistoricalWaterHeatDemand();
		float[] arr3 = thisAgent.getHistoricalBaseDemand();
		float[] arr4 = thisAgent.getHistoricalColdDemand();
		float[] arr5 = thisAgent.getHistoricalWetDemand();

		for (int i = 0; i < 48 ; i++)
		{
			result.addValue((Number)arr1[i], "Space Heat", i);
			result.addValue((Number)arr2[i], "Water Heat", i);
			result.addValue((Number)arr3[i], "Base", i);
			result.addValue((Number)arr4[i], "Cold", i);
			result.addValue((Number)arr5[i], "Wet", i);

		}
		
		return result;

	}
	
	/**
	 * Creates a sample dataset 
	 */
	private  DefaultCategoryDataset createTemperatureDataset(HouseholdProsumer thisAgent) {
		DefaultCategoryDataset result = new DefaultCategoryDataset();

		float[] arr1 = thisAgent.getSetPointProfile();
		float[] arr2 = thisAgent.getOptimisedSetPointProfile();
		float[] arr3 = thisAgent.getHistoricalExtTemp();
		float[] arr4 = thisAgent.getHistoricalIntTemp();

		for (int i = 0; i < 48 ; i++)
		{
			result.addValue((Number)arr1[i], "Set Point", i);
			result.addValue((Number)arr2[i], "Optimised Set Point", i);
			result.addValue((Number)arr3[i], "External Temp", i);
			result.addValue((Number)arr4[i], "Internal Temp", i);
		}
		
		return result;
	}


	/**
	 * Creates a chart
	 */
	private JFreeChart createChart(DefaultCategoryDataset dataset, String title) {

		JFreeChart chart = ChartFactory.createStackedBarChart(
				title, //chart title 
				"Half hour", // Domain label
				"kWh", //Range label
				dataset,                // data
				PlotOrientation.VERTICAL, 
				true,                   // include legend
				true,
				false
		);

		//Alter appearance here

		return chart;

	}
	
	/**
	 * Creates a chart
	 */
	private JFreeChart createTemperatureChart(DefaultCategoryDataset dataset, String title) {

		JFreeChart chart = ChartFactory.createLineChart(
				title, //chart title 
				"Half hour", // Domain label
				"degrees C", //Range label
				dataset,                // data
				PlotOrientation.VERTICAL, 
				true,                   // include legend
				true,
				false
		);

		//Alter appearance here

		return chart;

	}

	//@ScheduledMethod(start = 0, interval = 48, shuffle = true, priority = Consts.PROBE_PRIORITY)
	public void scheduledUpdate()
	{
		//Bit of a hack based on the thought that the ArrayLists will both be in the same order
		//Could be we should have a Collection of Pairs to 
		//Make this more explicit.
		int i = 0;
		for (HouseholdProsumer testProbed : probedAgents)
		{
			charts.get(i).getCategoryPlot().setDataset(createTemperatureDataset(testProbed));
			charts.get(i+1).getCategoryPlot().setDataset(createDataset(testProbed));
			i += 2;
		}

	}

	public ProsumerProbeListener(CascadeContext context)
	{
		super();
		RunEnvironment.getInstance().getCurrentSchedule().schedule(ScheduleParameters.createRepeating(RepastEssentials.GetTickCount()+1, context.getTickPerDay(),
                ScheduleParameters.END), new ProbeUpdater(this));
	}


}


