/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.ui.RefineryUtilities;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.ui.widget.SnapshotTaker;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import cern.jet.random.AbstractDiscreteDistribution;

/**
 * @author Babak Mahdavi
 *
 */
public class ChartUtils {
	
	public static ArrayList<SnapshotTaker> buildChartSnapshotTakers (Collection<JComponent> chartCompCollection) {
		Iterator<JComponent> compIter= chartCompCollection.iterator();
		ArrayList<SnapshotTaker>  snapshotTakerArrList = new ArrayList<SnapshotTaker>();
		while ( compIter.hasNext() ){
			ChartPanel chartComp = (ChartPanel) compIter.next();
			//if (Consts.DEBUG) System.out.println(chartComp.getChart().getTitle().getText());
			SnapshotTaker snapshotTaker = new SnapshotTaker(chartComp);
			snapshotTakerArrList.add(snapshotTaker);
		}
		
		return snapshotTakerArrList;
	}
	
	public static void buildChartSnapshotSchedule(CascadeContext context, int interval) {

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		//ScheduleParameters params = ScheduleParameters.createOneTime(1);
		//if (Consts.DEBUG) System.out.println("chartCompCol: null?: "+getChartCompCollection());
		//if ((chartSnapshotOn) && (getChartCompCollection() != null)){
		ScheduleParameters params = ScheduleParameters.createRepeating(0, interval,ScheduleParameters.LAST_PRIORITY);
		schedule.schedule(params, context, "takeSnapshot"); 

	}
	
	static class ChartAppFrame extends JFrame {
		public ChartAppFrame(String title, JFreeChart chart) {
			super(title);
			final ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setPreferredSize(new Dimension(500, 270));
			setContentPane(chartPanel);
			//setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			/*addWindowListener(new WindowAdapter(  ) {
			      public void windowClosing(WindowEvent we) { 
			    	  dispose(); }
			    }); */		
			
		}
	}

	public static void showChart(JFreeChart chart) {

		final ChartAppFrame chartAppFrame = new ChartAppFrame("ChartUtils", chart);
		chartAppFrame.pack();
		RefineryUtilities.centerFrameOnScreen(chartAppFrame);
		chartAppFrame.setVisible(true);
	
	}
	
	public static void showChart(JFreeChart chart, JButton button) {

		final ChartAppFrame chartAppFrame = new ChartAppFrame("ChartUtils", chart);
		chartAppFrame.getContentPane().add(button);
		chartAppFrame.pack();
		RefineryUtilities.centerFrameOnScreen(chartAppFrame);
		chartAppFrame.setVisible(true);
	
	}

	public static void testProbabilityDistAndShowHistogram(AbstractDiscreteDistribution pd, int numOfVal, int numOfBins) {

		double[] values = new double[numOfVal];
		for (int i=1; i < numOfVal; i++) {
			values[i] = pd.nextInt();
		}

		ChartUtils.showChart(ChartUtils.createHistogram(values, numOfBins));
	}
	
	public static void testProbabilityDistAndShowHistogram(double[] values, int numOfBins) {

		ChartUtils.showChart(ChartUtils.createHistogram(values, numOfBins));
	}
	
	public static JFreeChart createHistogram(double[] values, int numOfBins) {

		 HistogramDataset dataset = new HistogramDataset();
		 dataset.setType(HistogramType.RELATIVE_FREQUENCY);
		 dataset.addSeries("Histogram",values,numOfBins);
		 String plotTitle = "ChartUtils: Histogram"; 
		 String xaxis = "bin #";
		 String yaxis = "val"; 
		 PlotOrientation orientation = PlotOrientation.VERTICAL; 
		 boolean showLegend = false; 
		 boolean toolTips = true;
		 boolean urls = false; 
		 JFreeChart chart = ChartFactory.createHistogram( plotTitle, xaxis, yaxis, 
				 dataset, orientation, showLegend, toolTips, urls);

		 return chart;

	 }
	 

	 public static void saveChartAsPNG(JFreeChart chart) {

		 int width = 500;
		 int height = 300; 

		 try {
			 ChartUtilities.saveChartAsPNG(new File("histogram.PNG"), chart, width, height);
		 } catch (IOException e) {} 

	 }
	 
	 public static void saveChartAsPNG(JFreeChart chart, String name) {

		 int width = 500;
		 int height = 300; 

		 saveChartAsPNG( chart,  name,  width,  height);

	 }
	 
	 public static void saveChartAsPNG(JFreeChart chart, String name, int width, int height) {

		 try {
			 OutputStream fileIO = new FileOutputStream(new File(name));
			 chart.removeLegend();
			 ChartUtilities.writeScaledChartAsPNG(fileIO, chart, width, height, 8, 8);
		 } catch (IOException e) {} 

	 }

	 public static JFreeChart createBarChart(final CategoryDataset dataset) {
	        
	        final JFreeChart chart = ChartFactory.createBarChart(
	            "Bar Chart Demo",         // chart title
	            "Category",               // domain axis label
	            "Value",                  // range axis label
	            dataset,                  // data
	            PlotOrientation.VERTICAL, // orientation
	            true,                     // include legend
	            true,                     // tooltips?
	            false                     // URLs?
	        );


	        chart.setBackgroundPaint(Color.white);

	        final CategoryPlot plot = chart.getCategoryPlot();
	        plot.setBackgroundPaint(Color.lightGray);
	        plot.setDomainGridlinePaint(Color.white);
	        plot.setRangeGridlinePaint(Color.white);

	        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

	        final BarRenderer renderer = (BarRenderer) plot.getRenderer();
	        renderer.setDrawBarOutline(false);
	        
	        final GradientPaint gp0 = new GradientPaint(
	            0.0f, 0.0f, Color.blue, 
	            0.0f, 0.0f, Color.lightGray
	        );
	        final GradientPaint gp1 = new GradientPaint(
	            0.0f, 0.0f, Color.green, 
	            0.0f, 0.0f, Color.lightGray
	        );
	        final GradientPaint gp2 = new GradientPaint(
	            0.0f, 0.0f, Color.red, 
	            0.0f, 0.0f, Color.lightGray
	        );
	        renderer.setSeriesPaint(0, gp0);
	        renderer.setSeriesPaint(1, gp1);
	        renderer.setSeriesPaint(2, gp2);

	        final CategoryAxis domainAxis = plot.getDomainAxis();
	        domainAxis.setCategoryLabelPositions(
	            CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0)
	        );
	        
	        return chart;
	        
	    }

}
