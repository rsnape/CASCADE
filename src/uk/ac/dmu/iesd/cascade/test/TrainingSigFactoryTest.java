/**
 * 
 */
package uk.ac.dmu.iesd.cascade.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import uk.ac.dmu.iesd.cascade.util.ChartUtils;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.TrainingSignalFactory;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.TrainingSignalFactory.SIGNAL_TYPE;

/**
 * @author jsnape
 *
 * TODO: Should really integrate JUnit tests into all our classes to ensure they don't regress
 */
public class TrainingSigFactoryTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int testLength = 63;
		
		TrainingSignalFactory defaultFactory = new TrainingSignalFactory();
		TrainingSignalFactory lengthFactory = new TrainingSignalFactory(testLength);
		
		if (defaultFactory.getSignalLength() != 48)
		{
			System.err.println("Factory not initialised to default correctly");
		}
		if (lengthFactory.getSignalLength() != testLength)
		{
			System.err.println("Factory not initialised to 336 length correctly");
		}
		
		ArrayList<double[]> sigs = new ArrayList<double[]>();
		DefaultCategoryDataset tempDataset = new DefaultCategoryDataset();
		for (SIGNAL_TYPE t : SIGNAL_TYPE.values())
		{
			double [] s = defaultFactory.generateSignal(t);
			sigs.add(s);
			System.out.println(Arrays.toString(s));
			for (int i = 0; i < s.length ; i++)
			{
				tempDataset.addValue((Number)s[i], t.name(), i);
			}
		}
		
			// based on the dataset we create the chart
			JFreeChart tempChart = ChartFactory.createLineChart("Training signals", "tick", "value",tempDataset, PlotOrientation.VERTICAL, 
					true,                   // include legend
					true,
					false
			);
			//And display it
			ChartUtils.showChart(tempChart);
			

	}

}
