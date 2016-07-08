/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

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
	WeakHashMap<String, List<double []>> day_profiles = new WeakHashMap<String, List<double []>>();
	
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
		// TODO Auto-generated method stub
		
		double min_cost = Double.MAX_VALUE;
		double[] best_profile = null;
		
		List<double[]> profiles = this.day_profiles.get("01-26.inp");

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
	
	public RetailOutlet(String filename) throws FileNotFoundException
	{
		populateProfiles(filename);
	}

	/**
	 * @param filename
	 * @throws FileNotFoundException 
	 */
	private void populateProfiles(String filename) throws FileNotFoundException
	{
		CSVReader r = new CSVReader(filename);
		r.parseRaw();
		for (int i=0; i < r.getNumRows(); i++)
		{
			String[] rowData = r.getRow(i);
			String input_name = rowData[6];
			String[] tempProfile = new String[19];
			System.arraycopy(rowData, 7, tempProfile, 0, 19);
			double[] numerical_profile = ArrayUtils.convertStringArrayToDoubleArray(tempProfile);	
			List<double[]> tempList = day_profiles.get(input_name);
			if (tempList == null)
			{
				tempList = new ArrayList<double[]>();
			}
			tempList.add(numerical_profile);
			day_profiles.put(input_name, tempList);
		}
		
	}

}
