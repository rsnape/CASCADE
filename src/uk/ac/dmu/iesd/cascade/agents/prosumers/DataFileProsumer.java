/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.lang3.StringUtils;

import repast.simphony.essentials.RepastEssentials;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * Very basic prosumer to simply pass through a demand file at the requisite moment.
 * 
 * @author jsnape
 * 
 */
public class DataFileProsumer extends ProsumerAgent
{
	
	private double[] demand;
	private double[] generation;
	private boolean perfectPrediction = true;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#step()
	 */
	@Override
	public void step()
	{
		double stepDemand = 0;
		if (this.demand.length > 0)
		{
			stepDemand = this.demand[this.mainContext.getTickCount()%this.demand.length];
		}
		
		this.setNetDemand(stepDemand-currentGeneration());
	}

	public DataFileProsumer(CascadeContext context, String demandFileName, String genFileName) throws FileNotFoundException
	{
		this.mainContext = context;
		String dataPath = (String) RepastEssentials.GetParameter("dataFileFolder");

		if (StringUtils.isNotBlank(demandFileName))
		{
			File demFile = new File(dataPath, demandFileName);
			CSVReader demandFileReader = new CSVReader(demFile);
			demandFileReader.parseByColumn();
			this.demand = ArrayUtils.convertStringArrayToDoubleArray(demandFileReader.getColumn("demand"));
			this.mainContext.logger.info("Added demand to the prosumer, length : " + this.generation.length);
		}
		else
		{
			this.demand = new double[0];
		}
		
		if (StringUtils.isNotBlank(genFileName))
		{
			File genFile = new File(dataPath, genFileName);
			CSVReader genFileReader = new CSVReader(genFile);
			genFileReader.parseByColumn();
			String[] genVals = genFileReader.getColumn("generation");
			this.mainContext.logger.info("Adding values from column, length " + genVals.length);
			this.generation = ArrayUtils.convertStringArrayToDoubleArray(genVals);
			this.mainContext.logger.info("Added generation to the prosumer, length : " + this.generation.length);
		}
		else
		{
			this.generation = new double[0];
		}
		context.add(this);
	}
	

	@Override
	public double predictedGeneration() {
		double currGen = this.currentGeneration();
		double tomorrowGen = this.generation[(this.mainContext.getTickCount()+this.mainContext.ticksPerDay)%this.generation.length];
		if (perfectPrediction)
		{
			return tomorrowGen;
		}
		else
		{
			return currGen;
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent#currentGeneration()
	 */
	@Override
	public double currentGeneration() {
		double retval = this.generation[this.mainContext.getTickCount()%this.generation.length];
		if (Double.isNaN(retval))
		{
			//Deal with error case of not a number by substituting zero
			retval = 0;
		}
		return retval;
	}

}
