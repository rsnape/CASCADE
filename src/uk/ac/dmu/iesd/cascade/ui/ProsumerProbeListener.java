package uk.ac.dmu.iesd.cascade.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.jfree.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.CategoryTableXYDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.Rotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.IAction;
import repast.simphony.engine.schedule.NonModelAction;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.ui.RSApplication;
import repast.simphony.ui.probe.Probe;
import repast.simphony.ui.probe.ProbePanelCreator;
import repast.simphony.util.collections.IndexedIterable;
import repast.simphony.visualization.ProbeEvent;
import repast.simphony.visualization.ProbeListener;

import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.ChartUtils;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

public class ProsumerProbeListener implements ProbeListener {

	/**
	 * Ordered ArrayList of charts spawned by this listener
	 */
	ArrayList<JFreeChart> charts = new ArrayList<JFreeChart>();
	/**
	 * Ordered ArrayList of agents that have been probed and called this listener
	 */
	ArrayList<HouseholdProsumer> probedAgents = new ArrayList<HouseholdProsumer>();
	
	CascadeContext mainContext;

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


	/**
	 * Modification: modified and added new probing elements such as pie and line charts for
	 * monitoring the electricity consumption (Babak Mahdavi) 
	 */
	@Override
	public void objectProbed(ProbeEvent evt) {
		// TODO Auto-generated method stub
		List<?> probedObjects = evt.getProbedObjects();
		
		for(Object thisObj : probedObjects)
		{
			if (thisObj instanceof HouseholdProsumer)
			{
				final HouseholdProsumer thisAgent = (HouseholdProsumer) thisObj;
				probedAgents.add(thisAgent);
				ProbePanelCreator newProbe = new ProbePanelCreator(thisAgent);
				Probe myProbe = newProbe.getProbe("TestProbe", true);
				myProbe.addPropertyChangeListener(RSApplication.getRSApplicationInstance().getGui());
				JFrame agentProbeFrame = new JFrame("Prosumer probe frame for " + thisAgent.toString());
				agentProbeFrame.setAlwaysOnTop(false);
				agentProbeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				agentProbeFrame.getContentPane().setLayout(new BoxLayout(agentProbeFrame.getContentPane(), BoxLayout.PAGE_AXIS));

				JPanel propertiesPanel = new JPanel(new BorderLayout());
				propertiesPanel.setSize(500,100);
				Label title = new Label(thisAgent.getAgentName() + " properties");
				title.setFont(new Font("Arial", Font.BOLD,32));
				title.setAlignment(Label.CENTER);
				//propertiesPanel.setLayout(new GridLayout(2,1));
				propertiesPanel.add(title, BorderLayout.PAGE_START);
				JPanel variablesBox = new JPanel();
				variablesBox.setFont(new Font("Arial", Font.BOLD,10));
				//variablesBox.setLayout(new BoxLayout(variablesBox, BoxLayout.PAGE_AXIS));
				variablesBox.setLayout(new GridLayout(3,2));
				variablesBox.setBorder(new EmptyBorder(10,10,10,10));
				variablesBox.setBackground(Color.WHITE);
				variablesBox.add(new JLabel("Has Gas? " + thisAgent.isHasGas()));
				variablesBox.add(new JLabel("Tau (secs) = " + String.format("%.2f", thisAgent.tau)));
				variablesBox.add(new JLabel("M (kWh / deg C) = " + String.format("%.2f", thisAgent.buildingThermalMass)));
				variablesBox.add(new JLabel("L (W / deg C) = " + String.format("%.2f", thisAgent.buildingHeatLossRate)));
				variablesBox.add(new JLabel("Occupants = " + thisAgent.getNumOccupants()));
				propertiesPanel.add(variablesBox,BorderLayout.CENTER);
				propertiesPanel.setBackground(Color.WHITE);
				agentProbeFrame.getContentPane().add(propertiesPanel);
				
				if (thisAgent.isHasElectricalSpaceHeat())
				{
					DefaultCategoryDataset tempDataset = createTemperatureDataset(thisAgent);
					// based on the dataset we create the chart
					JFreeChart tempChart = createLineChart(tempDataset, "Previous day temperatures", "Half hour", "degrees C");
					charts.add(tempChart);
					// we put the chart into a panel
					ChartPanel tempChartPanel = new ChartPanel(tempChart);
					// default size
					tempChartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
					agentProbeFrame.getContentPane().add(tempChartPanel);
				}

				CategoryTableXYDataset dataset = createDataset(thisAgent);
				// based on the dataset we create the chart
				final JFreeChart chart = createChart(dataset, "Previous day demand by type");
				chart.getXYPlot().getRangeAxis().setRange(0, 4.0);
				TickUnits onekwh = new TickUnits();
				onekwh.add(new NumberTickUnit(1.0));
				chart.getXYPlot().getRangeAxis().setStandardTickUnits(onekwh);
				TickUnits fourTicks = new TickUnits();
				fourTicks.add(new NumberTickUnit(4));
				chart.getXYPlot().getDomainAxis().setStandardTickUnits(fourTicks);
				
				
				charts.add(chart);
				
				// we put the chart into a panel
				ChartPanel chartPanel = new ChartPanel(chart);
				// default size
				chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

				agentProbeFrame.getContentPane().add(chartPanel);
				
				JButton saveAsButton = new JButton("Click to Save as PNG");
				saveAsButton.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						String name = thisAgent.getAgentName()+"_demand_at_tick_"+mainContext.getTickCount()+".png";
						ChartUtils.saveChartAsPNG(chart, name);
					}
				});
				
				agentProbeFrame.getContentPane().add(saveAsButton);

				DefaultPieDataset pieDataset = createPieDataset(thisAgent);
				JFreeChart pieChart = createPieChart(pieDataset, "Demands Proportion");
				ChartPanel pieChartPanel = new ChartPanel(pieChart);
				pieChartPanel.setPreferredSize(new java.awt.Dimension(300, 240));
				agentProbeFrame.getContentPane().add(pieChartPanel);
				charts.add(pieChart);
				
				JButton loadsProfilesLineChartButton = new JButton("Click here to see demands Line Chart of this HHPros");
				loadsProfilesLineChartButton.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						//IndexedIterable<HouseholdProsumer> iIterOfHouseholdProsumers = mainContext.getObjects(HouseholdProsumer.class);
						//DefaultCategoryDataset loadsDataset = createDataset(thisAgent);
						CategoryTableXYDataset loadsDataset = createDataset(thisAgent);
						//JFreeChart loadsChart = createLineChart(loadsDataset, "Demand Profiles Chart (hh_"+thisAgent.getAgentID()+", @t="+mainContext.getTickCount()+")", "Half hour", "Load (KWh)");
						//ChartUtils.showChart(loadsChart);
					}
				});
				
				agentProbeFrame.getContentPane().add(loadsProfilesLineChartButton);
				
				JButton allPieChartButton = new JButton("Click here to see demands Pie Chart of  ALL HHPros");
				allPieChartButton.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
					   //System.out.println(" mouseClicked: ");
						IndexedIterable<HouseholdProsumer> iIterOfHouseholdProsumers = mainContext.getObjects(HouseholdProsumer.class);
						ArrayList<HouseholdProsumer> list_hhProsumers = IterableUtils.Iterable2ArrayList(iIterOfHouseholdProsumers);
						DefaultPieDataset pieDatasetOfAvgDemandOfAllHHProsConsumption = createPieDataset4AvgDemandOfAllHHPros(list_hhProsumers);
						JFreeChart pieChart4AllHHProsConsumption = createPieChart(pieDatasetOfAvgDemandOfAllHHProsConsumption, "AVG Demands Proportion of All HHPros (@t=" +mainContext.getTickCount()+")");
						ChartUtils.showChart(pieChart4AllHHProsConsumption);
						
					}
				});
				
				agentProbeFrame.getContentPane().add(allPieChartButton);
				
				JButton allBarChartButton = new JButton("Click here to see demands Stacked Bar of  ALL HHPros");
				allBarChartButton.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
					   //System.out.println(" mouseClicked: ");
						IndexedIterable<HouseholdProsumer> iIterOfHouseholdProsumers = mainContext.getObjects(HouseholdProsumer.class);
						ArrayList<HouseholdProsumer> list_hhProsumers = IterableUtils.Iterable2ArrayList(iIterOfHouseholdProsumers);
						CategoryTableXYDataset barDatasetOfAllHHProsConsumption = createStackedBarDataset4DemandOfAllHHPros(list_hhProsumers);
						int now = mainContext.getTickCount();
						int start = now - (now % mainContext.ticksPerDay);
						final JFreeChart barChart4AllHHProsConsumption = createChart(barDatasetOfAllHHProsConsumption, "Total Demands Proportion of All HHPros (from " + start + " to " + (start+mainContext.ticksPerDay) + " evaluated @t=" +now+")");
						TickUnits fourTicks = new TickUnits();
						fourTicks.add(new NumberTickUnit(4));
						TickUnits kwhTicks = new TickUnits();
						kwhTicks.add(new NumberTickUnit(200));
						barChart4AllHHProsConsumption.getXYPlot().getDomainAxis().setStandardTickUnits(fourTicks);
						barChart4AllHHProsConsumption.getXYPlot().getRangeAxis().setRange(0, 1700);
						barChart4AllHHProsConsumption.getXYPlot().getRangeAxis().setStandardTickUnits(kwhTicks);

						JButton saveAsButton = new JButton("Click to Save as PNG");
						saveAsButton.addMouseListener(new MouseAdapter() {
							public void mouseClicked(MouseEvent e) {
								String name = "all_households_demand_at_tick_"+mainContext.getTickCount()+".png";
								ChartUtils.saveChartAsPNG(barChart4AllHHProsConsumption, name);
							}
						});
						ChartUtils.showChart(barChart4AllHHProsConsumption, saveAsButton);
						
					}
				});
				
				agentProbeFrame.getContentPane().add(allBarChartButton);
	
				//Display the window.
				agentProbeFrame.pack();
				agentProbeFrame.setVisible(true);
			}

		}
	}

	/**
	 * Creates a sample dataset 
	 */
	private  CategoryTableXYDataset createDataset(HouseholdProsumer thisAgent) {
		CategoryTableXYDataset result = new CategoryTableXYDataset();

		double[] arr1 = thisAgent.getHistoricalSpaceHeatDemand();
		double[] arr2 = thisAgent.getHistoricalWaterHeatDemand();
		double[] arr3 = thisAgent.getHistoricalOtherDemand();
		double[] arr4 = thisAgent.getHistoricalColdDemand();
		double[] arr5 = thisAgent.getHistoricalWetDemand();
		double[] arr6 = thisAgent.getHistoricalEVDemand();

		for (int i = 0; i < 48 ; i++)
		{
			result.add(i,arr1[i], "Space Heat");
			result.add(i,arr2[i], "Water Heat");
			result.add(i, arr3[i], "Other");
			result.add(i, arr4[i], "Cold");
			result.add(i, arr5[i], "Wet");
			result.add(i, arr6[i],"EV");
			/*result.addValue((Number)arr1[i], "Space Heat", i);
			result.addValue((Number)arr2[i], "Water Heat", i);
			result.addValue((Number)arr3[i], "Other", i);
			result.addValue((Number)arr4[i], "Cold", i);
			result.addValue((Number)arr5[i], "Wet", i);
			result.addValue((Number)arr6[i],"EV", i);*/


		}

		return result;

	}

	/**
	 * Creates a sample dataset 
	 */
	private  DefaultCategoryDataset createTemperatureDataset(HouseholdProsumer thisAgent) {
		DefaultCategoryDataset result = new DefaultCategoryDataset();

		double[] arr1 = thisAgent.getSetPointProfile();
		double[] arr2 = thisAgent.getOptimisedSetPointProfile();
		double[] arr3 = thisAgent.getHistoricalExtTemp();
		double[] arr4 = thisAgent.getHistoricalIntTemp();

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
	 * Creates and returns a category dataset of the demand of a given HHProsumer to 
	 * be used in a line chart.
	 * @author Babak Mahdavi
	 * @param hhAgent
	 * @return
	 */
	private  DefaultCategoryDataset createLoadsProfileDataset(HouseholdProsumer hhAgent) {
		DefaultCategoryDataset dcDataset = new DefaultCategoryDataset();

		double[] arr_spaceHeat = hhAgent.getHistoricalSpaceHeatDemand();
		double[] arr_hotWater = hhAgent.getHistoricalWaterHeatDemand();
		double[] arr_otherDemand = hhAgent.getHistoricalOtherDemand();
		double[] arr_cold = hhAgent.getHistoricalColdDemand();
		double[] arr_wet = hhAgent.getHistoricalWetDemand();
		double[] arr_elecVehicle = hhAgent.getHistoricalEVDemand();

		for (int i = 0; i < arr_otherDemand.length ; i++)	{
			dcDataset.addValue((Number)arr_spaceHeat[i], "SH", i);
			dcDataset.addValue((Number)arr_hotWater[i], "WH", i);
			dcDataset.addValue((Number)arr_otherDemand[i], "O", i);
			dcDataset.addValue((Number)arr_cold[i], "C", i);
			dcDataset.addValue((Number)arr_wet[i], "W", i);
			dcDataset.addValue((Number)arr_elecVehicle[i],"EV", i);


		}

		return dcDataset;
	}
	

	/**
	 * Creates and returns a pie chart dataset using the demand of a given HHProsumer
	 * @author Babak Mahdavi
	 * @param hhAgent
	 * @return
	 */
	private  DefaultPieDataset createPieDataset(HouseholdProsumer hhAgent) {
		
		DefaultPieDataset pieDataset = new DefaultPieDataset();

		double[] arr_spaceHeat = hhAgent.getHistoricalSpaceHeatDemand();
		double[] arr_hotWater = hhAgent.getHistoricalWaterHeatDemand();
		double[] arr_otherDemand = hhAgent.getHistoricalOtherDemand();
		double[] arr_cold = hhAgent.getHistoricalColdDemand();
		double[] arr_wet = hhAgent.getHistoricalWetDemand();
		double[] arr_elecVehicle = hhAgent.getHistoricalEVDemand();


		pieDataset.setValue("SH", ArrayUtils.sum(arr_spaceHeat));		
		pieDataset.setValue("WH", ArrayUtils.sum(arr_hotWater));
		pieDataset.setValue("O", ArrayUtils.sum(arr_otherDemand));
		pieDataset.setValue("C", ArrayUtils.sum(arr_cold));
		pieDataset.setValue("W", ArrayUtils.sum(arr_wet));
		pieDataset.setValue("EV", ArrayUtils.sum(arr_elecVehicle));
		
		return pieDataset;

	}
	

	
	/**
	 * Creates and returns a pie chart dataset using the demand of all HHProsumers
	 * @author Babak Mahdavi
	 * @param list_hhProsumers
	 * @return
	 */
	@Deprecated
	private  DefaultPieDataset createPieDataset4AllHHProsumers(List<HouseholdProsumer> list_hhProsumers) {

		DefaultPieDataset pieDataset = new DefaultPieDataset();

		double sum_spaceHeat=0;
		double sum_hotWater=0;
		double sum_other=0;
		double sum_cold=0;
		double sum_wet=0;
		double sum_EV=0;

		for (HouseholdProsumer hhAgent : list_hhProsumers) {
			sum_spaceHeat += ArrayUtils.sum(hhAgent.getHistoricalSpaceHeatDemand());
			sum_hotWater += ArrayUtils.sum(hhAgent.getHistoricalWaterHeatDemand());
			sum_other += ArrayUtils.sum(hhAgent.getHistoricalOtherDemand());
			sum_cold += ArrayUtils.sum(hhAgent.getHistoricalColdDemand());
			sum_wet += ArrayUtils.sum(hhAgent.getHistoricalWetDemand());
			sum_EV += ArrayUtils.sum(hhAgent.getHistoricalEVDemand());

		}

		pieDataset.setValue("SH", sum_spaceHeat);
		pieDataset.setValue("WH", sum_hotWater);
		pieDataset.setValue("O", sum_other);
		pieDataset.setValue("C", sum_cold);
		pieDataset.setValue("W", sum_wet);
		pieDataset.setValue("EV", sum_EV);

		return pieDataset;

	}
	
	/**
	 * Creates and returns a pie chart dataset using the average demand of all HHProsumers
	 * @author Babak Mahdavi
	 * @param list_hhProsumers
	 * @return
	 */
	private CategoryTableXYDataset createStackedBarDataset4DemandOfAllHHPros(List<HouseholdProsumer> list_hhProsumers) {

		CategoryTableXYDataset totalDemandPerType = new CategoryTableXYDataset();

		double[] tot_spaceHeat=new double[48];
		double[] tot_hotWater=new double[48];;
		double[] tot_other=new double[48];
		double[] tot_cold=new double[48];
		double[] tot_wet=new double[48];
		double[] tot_EV=new double[48];
		
		double nbOfHHPros = list_hhProsumers.size();

		for (HouseholdProsumer hhAgent : list_hhProsumers) {
			tot_spaceHeat = ArrayUtils.add(tot_spaceHeat,hhAgent.getHistoricalSpaceHeatDemand());
			tot_hotWater = ArrayUtils.add(tot_hotWater,hhAgent.getHistoricalWaterHeatDemand());
			tot_other = ArrayUtils.add(tot_other,hhAgent.getHistoricalOtherDemand());
			tot_cold = ArrayUtils.add(tot_cold,hhAgent.getHistoricalColdDemand());
			tot_wet = ArrayUtils.add(tot_wet,hhAgent.getHistoricalWetDemand());
			tot_EV = ArrayUtils.add(tot_EV,hhAgent.getHistoricalEVDemand());
		}

		for (int i = 0; i < tot_cold.length; i++)
		{
		totalDemandPerType.add(i, tot_spaceHeat[i], "Space Heat");
		totalDemandPerType.add(i, tot_hotWater[i], "Hot Water");
		totalDemandPerType.add(i, tot_other[i], "Other");
		totalDemandPerType.add(i, tot_cold[i], "Cold");
		totalDemandPerType.add(i, tot_wet[i], "Wet");
		totalDemandPerType.add(i, tot_EV[i], "EV");
		}

		return totalDemandPerType;

	}
	
	/**
	 * Creates and returns a pie chart dataset using the average demand of all HHProsumers
	 * @author Babak Mahdavi
	 * @param list_hhProsumers
	 * @return
	 */
	private  DefaultPieDataset createPieDataset4AvgDemandOfAllHHPros(List<HouseholdProsumer> list_hhProsumers) {

		DefaultPieDataset pieDataset = new DefaultPieDataset();

		double sum_spaceHeat=0;
		double sum_hotWater=0;
		double sum_other=0;
		double sum_cold=0;
		double sum_wet=0;
		double sum_EV=0;

		
		double nbOfHHPros = list_hhProsumers.size();

		for (HouseholdProsumer hhAgent : list_hhProsumers) {
			sum_spaceHeat += ArrayUtils.sum(hhAgent.getHistoricalSpaceHeatDemand());
			sum_hotWater += ArrayUtils.sum(hhAgent.getHistoricalWaterHeatDemand());
			sum_other += ArrayUtils.sum(hhAgent.getHistoricalOtherDemand());
			sum_cold += ArrayUtils.sum(hhAgent.getHistoricalColdDemand());
			sum_wet += ArrayUtils.sum(hhAgent.getHistoricalWetDemand());
			sum_EV += ArrayUtils.sum(hhAgent.getHistoricalEVDemand());

		}
		
		double avg_spaceHeat= sum_spaceHeat/nbOfHHPros;
		double avg_hotWater=sum_hotWater/nbOfHHPros;
		double avg_other= sum_other/nbOfHHPros;
		double avg_cold= sum_cold/nbOfHHPros;
		double avg_wet= sum_wet/nbOfHHPros;
		double avg_EV = sum_EV/nbOfHHPros;

		pieDataset.setValue("SH", avg_spaceHeat);
		pieDataset.setValue("WH", avg_hotWater);
		pieDataset.setValue("O", avg_other);
		pieDataset.setValue("C", avg_cold);
		pieDataset.setValue("W", avg_wet);
		pieDataset.setValue("EV", avg_EV);

		return pieDataset;

	}
	

	/**
	 * Creates and returns a pie chart (of JFreeChart) with customized label 
	 * @author Babak Mahdavi
	 * @param dataset
	 * @param title
	 * @return
	 */
	private JFreeChart createPieChart(DefaultPieDataset dataset, String title) {

		//final JFreeChart pieChart = ChartFactory.createPieChart3D(
		  final JFreeChart pieChart = ChartFactory.createPieChart(

				title,
				dataset,                // data
				true,                   // include legend
				true,
				false
		);
        
        PiePlot plot = (PiePlot) pieChart.getPlot();        
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(" {0}: {1} ({2})"));
		return pieChart;
	}

	/**
	 * Creates a chart
	 */
	private JFreeChart createChart(CategoryTableXYDataset dataset, String title) {

		JFreeChart chart = ChartFactory.createXYBarChart(
				null, //chart title 
				"Half hour", // Domain label
				false,
				"kWh", //Range label
				dataset,                // data
				PlotOrientation.VERTICAL, 
				true,                   // include legend
				true,
				false);
		
		StackedXYBarRenderer rend = new StackedXYBarRenderer();
		chart.getXYPlot().setRenderer(rend);
		ValueAxis ax = chart.getXYPlot().getRangeAxis();
		RectangleInsets insets = ax.getLabelInsets();
		insets.extendWidth(100);
		ax.setLabelInsets(insets, true);


				// OPTIONAL CUSTOMISATION COMPLETED.
		

		//Alter appearance here

		return chart;

	}

	/**
	 * Creates and returns a line chart (of JFreeChart type) using a given category dataset, title and y-axis label.
	 * @author Babak Mahdavi 
	 * @param dataset
	 * @param title
	 * @param xLabel
	 * @param yLabel
	 * @return
	 */
	private JFreeChart createLineChart(DefaultCategoryDataset dataset, String title, String xLabel, String yLabel) {

		JFreeChart chart = ChartFactory.createLineChart(
				title, //chart title 
				xLabel, // Domain label
				yLabel, //Range label
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
	 *
	 * Modification: modified and added the pie chart update (Babak Mahdavi) 
	 */
	
	//@ScheduledMethod(start = 0, interval = 48, shuffle = true, priority = Consts.PROBE_PRIORITY)
	public void scheduledUpdate()
	{
		//Bit of a hack based on the thought that the ArrayLists will both be in the same order
		//Could be we should have a Collection of Pairs to 
		//Make this more explicit.
		int i = 0;
		for (HouseholdProsumer hhProbedAgent : probedAgents)
		{
			if(hhProbedAgent.isHasElectricalSpaceHeat())
			{
				charts.get(i).getCategoryPlot().setDataset(createTemperatureDataset(hhProbedAgent));
				//				charts.get(i+1).getCategoryPlot().setDataset(createDataset(hhProbedAgent));				

				charts.get(i+1).getXYPlot().setDataset(createDataset(hhProbedAgent));				
			    ((PiePlot) charts.get(i+2).getPlot()).setDataset(this.createPieDataset(hhProbedAgent));
				
				i += 3;
			}
			else
			{
				//				charts.get(i).getCategoryPlot().setDataset(createDataset(hhProbedAgent));

				charts.get(i).getXYPlot().setDataset(createDataset(hhProbedAgent));
			    ((PiePlot) charts.get(i+1).getPlot()).setDataset(this.createPieDataset(hhProbedAgent));

				i += 2;
			}

		}

	}

	public ProsumerProbeListener(CascadeContext context)
	{
		super();
		RunEnvironment.getInstance().getCurrentSchedule().schedule(ScheduleParameters.createRepeating(RepastEssentials.GetTickCount()+1, context.getNbOfTickPerDay(),
				ScheduleParameters.END), new ProbeUpdater(this));
		
		mainContext = context;
	
	
	}


}


