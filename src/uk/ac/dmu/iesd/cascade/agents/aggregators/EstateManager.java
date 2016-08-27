/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import uk.ac.dmu.iesd.cascade.io.CSVReader;

/**
 * @author Richard
 *
 */
public class EstateManager extends AggregatorAgent
{

	private WeakHashMap<String, List<double[]>> buildingProfiles; 
	String dataDir = "D:\\Dropbox\\Minder\\data\\Optimal_outputs";
	
	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport()
	{
		// TODO Auto-generated method stub
		return null;
	}

	private void importAvailableProfiles(List<String> buildingIds, String CO2Profile) throws FileNotFoundException
	{
		
	    String baseName = dataDir + File.pathSeparator + CO2Profile;
	    for (String building : buildingIds)
		{
			String fileName = baseName + "_"+ building + "_opt_summary.csv";
			CSVReader profileFile = new CSVReader(fileName);
			profileFile.getColumnNames();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#bizPreStep()
	 */
	@Override
	public void bizPreStep()
	{
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#bizStep()
	 */
	@Override
	public void bizStep()
	{
		// TODO Auto-generated method stub

	}
	
	private void initialise() throws FileNotFoundException
	{
		ArrayList<String> buildings = new ArrayList<String>();
		buildings.add("Fin");
		buildings.add("Leu");
		buildings.add("Lon");
		importAvailableProfiles(buildings, "CO2_flat"); //Options for CO2 are "CO2_flat", "CO2_1" and "CO2_2"
		System.out.println(buildingProfiles.toString());
	}

}
