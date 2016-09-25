/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import cern.colt.Arrays;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * Developed as part of the MINDER project
 * 
 * @author Richard
 *
 */
public class RetailOutlet extends ProsumerAgent
{
	double[] best_profile = null;
	WeakHashMap<String, List<double []>> dayProfiles = new WeakHashMap<String, List<double []>>();
	
	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#step()
	 */
	@Override
	public void step()
	{
		
		if (this.mainContext.getTimeslotOfDay() == 0)
		{
		
		double min_cost = Double.MAX_VALUE;
		best_profile = null;
		
		List<double[]> profiles = this.dayProfiles.get("01-26.inp");

		for (double[] p: profiles)
		{
			double profile_cost = ArrayUtils.sum(ArrayUtils.mtimes(p, this.predictedCostSignal));
			if (profile_cost < min_cost)
			{
				best_profile = p.clone();
				min_cost = profile_cost;
			}
		}
		}
		
		this.setNetDemand(best_profile[this.mainContext.getTimeslotOfDay()]);

	}
		
	public RetailOutlet(String siteName, String co2profile) throws FileNotFoundException
	{
		//Bit hacky - work on parameterising this properly
		Parameters params = RunEnvironment.getInstance().getParameters();
		String dataFileFolderPath = (String) params.getValue("dataFileFolder");

		System.err.println("Path is" + dataFileFolderPath);
		
		String filename = dataFileFolderPath + File.separator + co2profile+"_"+siteName+"_"+"summary_with_consumption.csv";
		
		populateProfiles(filename);
		
		System.out.println(dayProfiles);
	}
	
	/**
	 * @param filename
	 * @throws FileNotFoundException 
	 */
	private void populateProfiles(String filename) throws FileNotFoundException
	{
		CSVReader r = new CSVReader(filename);
		r.parseByColumn(); //Weirdly, parseRaw appears to be broken.
		int startInd = -1;
		String[] cols = r.getColumnNames();
		
		for (int j=0; j < cols.length; j++)
		{
			if (cols[j].equals("E0"))
			{
				startInd = j;
				break;
			}					
		}
		
		if (startInd < 0)
		{
			System.err.println("File " + filename + " does not appear to contain energy profile columns");
			System.err.println("Columns are " + Arrays.toString(cols));
		}
		
		for (int i=0; i < r.getNumRows(); i++)
		{
			String[] rowData = r.getRow(i);
			String input_name = rowData[7]; //This gets column `P3` - TODO improve!!
			String[] tempProfile = new String[48];
			System.arraycopy(rowData, startInd, tempProfile, 0, 48);
			double[] numerical_profile = ArrayUtils.convertStringArrayToDoubleArray(tempProfile);	
			List<double[]> tempList = dayProfiles.get(input_name);
			if (tempList == null)
			{
				tempList = new ArrayList<double[]>();
			}
			tempList.add(numerical_profile);
			dayProfiles.put(input_name, tempList);
		}
		
	}

}
