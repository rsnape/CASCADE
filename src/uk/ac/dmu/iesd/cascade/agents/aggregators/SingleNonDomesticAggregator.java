/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.util.collections.IndexedIterable;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * @author denis
 *
 */
public class SingleNonDomesticAggregator extends BMPxTraderAggregator {

	private double[] 	SingleNonDomesticPower;			// of length 48 * Consts.NB_OF_DAYS_LOADED_DEMAND
	private double[] 	HouseholdPower;					// of length 48 * Consts.NB_OF_DAYS_LOADED_DEMAND
	private double[] 	SubsetOfNormaliseINDOPower;		// of length 48 * Consts.NB_OF_DAYS_LOADED_DEMAND
	
	private double[] 	INDOPower;						// of length 48 * 365
	private double[] 	NormaliseINDOPower;				// of length 48 * 365
	
	private double 		k;
	private int			lengthOfProfile;
	
	CascadeContext 		context;
	
	/**
	 *  Read in INDO values (national total power) from file -
	 *  assume the file is always called "INDO.csv", change as 
	 *  appropriate
	 *  
	 * @param Filename - CSV file contains a year power data (GW) for every half an hour
	 */
	private void ReadINDOValuesFromFile(String Filename){
		
		// get the parameters from the current run environment
		File dataDirectory = new File((String)RunEnvironment.getInstance().getParameters().getValue("dataFileFolder"));
		File INDODemandFile = new File(dataDirectory, Filename);
		try {
			// get the whole year of INDO values from file
			CSVReader INDODemandReader = new CSVReader(INDODemandFile);
			INDODemandReader.parseByColumn();
			this.INDOPower = ArrayUtils.convertStringArrayToDoubleArray(INDODemandReader.getColumn("demand"));
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name INDO.csv");
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		// normalise <code>INDOPower</code> between the range 0 and 1 
		// NOTE:
		// If use, ArrayUtils.offset() WILL offset value on existing array, NOT returning a copy of it - BEWARE!
		this.NormaliseINDOPower = ArrayUtils.normalizeValues(INDOPower);
		this.SubsetOfNormaliseINDOPower = new double[lengthOfProfile];
		this.SingleNonDomesticPower = new double[lengthOfProfile];
	}
	
	/**
	 * Extract energy (demand) from each household, and calculate aggregrate energy for all
	 * household prosumers. Demand is in kWh for every half an hour, calculate energy (GW) based 
	 * on the aggregrate demand
	 * 
	 * Assume <code>Household</code> is fixed for 7 days * 48 time slot long, each week has exactly
	 * the same profile, i.e. week1 = week2 = week3, ..., and so forth
	 * 
	 * @param context
	 */
	private void CalHouseholdPower(CascadeContext context) {
		
		this.HouseholdPower = new double[lengthOfProfile];
		Arrays.fill(this.HouseholdPower, 0);
		
		IndexedIterable<HouseholdProsumer> householdProsumers = context.getObjects(HouseholdProsumer.class);
		// xxxDemands are in kWh, which is at a half-hourly interval 
		for (HouseholdProsumer pAgent : householdProsumers) {	
			double[] OtherDemand = pAgent.getOtherDemandProfile();
			double[] WetDemand = pAgent.getWetAppliancesProfile();
			double[] ColdDemand = pAgent.getColdAppliancesProfile();
			// Demand profiles are extracted before any optimisation from smart controller
			double[] DailyHotWaterDemand = pAgent.getWaterHeatProfile();
			double[] HotWaterDemand = new double[lengthOfProfile];
			for(int i=0; i<Consts.NB_OF_DAYS_LOADED_DEMAND; i++) {
				ArrayUtils.replaceRange(HotWaterDemand, DailyHotWaterDemand, i*ticksPerDay);
			}
			
			double[] SpaceHeatDemand = pAgent.getSpaceHeatDemandwithoutOptimise();
			
			this.HouseholdPower = ArrayUtils.add(this.HouseholdPower, OtherDemand, WetDemand, ColdDemand, HotWaterDemand, SpaceHeatDemand);
		}
		
		// Calculate power instead based on total xxxDemands in GW
		this.HouseholdPower = ArrayUtils.multiply(this.HouseholdPower, 2d/1e9);
	}
	
	/**
	 * Calculate a simple industries & commercial power based on calculated <code>k</code>
	 */
	private void CalSingleNonDomesticPower() {
		
		if(Consts.NB_OF_DAYS_LOADED_DEMAND != 365) {
			for(int i=0; i<lengthOfProfile; i++) {
				this.SingleNonDomesticPower[i] = this.k * this.SubsetOfNormaliseINDOPower[i] - this.HouseholdPower[i];
			}
		} else {
			for(int i=0; i<lengthOfProfile; i++) {
				this.SingleNonDomesticPower[i] = this.k * this.NormaliseINDOPower[i] - this.HouseholdPower[i];
			}
		}
		
	}

	/**
	 * Print values out to file to check calculation
	 */
	private void CheckCalculation() {
		
		FileWriter writer;
		try {
			int offset = (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) * 48;
			//System.out.println("-> " + (RepastEssentials.GetTickCount() - offset));
			if((RepastEssentials.GetTickCount() - offset) == 0) {
				writer = new FileWriter("IndustriesAndCommercialOutput.csv", false);
				writer.append("k");
				writer.append(",");
				writer.append("INDO Power");
				writer.append(",");
				writer.append("Normalise INDO Power");
				writer.append(",");
				writer.append("Household Power");
				writer.append(",");
				writer.append("Industries & Commercial Power");
				writer.append("\n");
			} else {
				writer = new FileWriter("IndustriesAndCommercialOutput.csv", true);
			}
			for(int i=0; i<lengthOfProfile; i++) {
				writer.append(""+this.k);
				writer.append(",");
				writer.append(""+this.INDOPower[i]);
				writer.append(",");
				writer.append(""+this.SubsetOfNormaliseINDOPower[i]);
				writer.append(",");
				writer.append(""+this.HouseholdPower[i]);
				writer.append(",");
				writer.append(""+this.SingleNonDomesticPower[i]);
				writer.append("\n");
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void updateSingleNonDomesticPower() {
		
		// calculate <code>k</code>
		// Based on Peter B's simple equation
		// sum(kA-D) = 2sum(D)	where A is normalised INDO power vector, D is total household power vector
		// sum(kA) - sum(D) = 2sum(D)
		// sum(kA) = 3sum(D)
		// k = 3sum(D)/sum(A)
		
		// get relevant normalise INDO Power
		// <code>HouseholdPower</code> assume the same for each week
		int offset = (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) * this.ticksPerDay;
		
		if((RepastEssentials.GetTickCount() - offset) % 336 != 0)
			return;
		// avoid doing this at the end of any year
		if((RepastEssentials.GetTickCount() - offset) != 0) {
			if((RepastEssentials.GetTickCount() - offset) % 17520 == 0)
				return;
		}
		
		if(Consts.NB_OF_DAYS_LOADED_DEMAND != 365) {
			int start =  mainContext.getTickCount() - offset;
			int end = start + lengthOfProfile;
			int i = 0;
			//System.out.println("[updateSingleNonDomesticPower()] From: " + start + " to: " + end);
			for(int j=start; j<end; j++) {
				SubsetOfNormaliseINDOPower[i] = NormaliseINDOPower[j];
				i++;
			}
			this.k = (Consts.RATIO_AGANIST_HOUSEHOLD + 1) * ArrayUtils.sum(HouseholdPower) / ArrayUtils.sum(SubsetOfNormaliseINDOPower);
		}
		else {
			this.k = (Consts.RATIO_AGANIST_HOUSEHOLD + 1) * ArrayUtils.sum(HouseholdPower) / ArrayUtils.sum(NormaliseINDOPower);
		}
		
		CalSingleNonDomesticPower();
		
		double[] totalSpaceHeatDemand = new double[lengthOfProfile];
		for(int i=0; i<lengthOfProfile; i++) {
			totalSpaceHeatDemand[i] = 0d;
		}
		IndexedIterable<HouseholdProsumer> householdProsumers = this.context.getObjects(HouseholdProsumer.class);
		for (HouseholdProsumer pAgent : householdProsumers) {
			double[] SpaceHeatDemand = pAgent.getSpaceHeatDemandwithoutOptimise();
			totalSpaceHeatDemand = ArrayUtils.add(totalSpaceHeatDemand, SpaceHeatDemand);
		}
		for(int i=0; i<lengthOfProfile; i++) {
			System.out.print(totalSpaceHeatDemand[i] + ",");
		}
		System.out.println("\n");
		
		// update single non domestic demand
		//double [] singleNonDomesticDemand = new double[this.ticksPerDay];
		// this.SingleNonDomesticPower is 48 * 7 long
		
		
		CheckCalculation();
		
	}
	
	public void updateSingleNonDomesticDemand() {
		
		int dayOfWeek = -1;
		int offset = mainContext.getTickCount() - (Consts.AGGREGATOR_TRAINING_PERIODE + Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) * this.ticksPerDay;
		double [] SingleNonDomesticDemand = new double[this.ticksPerDay];
		
		offset = offset % (this.lengthOfProfile);
		// find the relevant index to copy power value, and convert into kWh in <code>singleNonDomesticDemand</code>
		if(offset == 0d) {
			dayOfWeek = 0;
		} else if(offset % (this.ticksPerDay*6) == 0d) {
			dayOfWeek = 6;
		} else if(offset % (this.ticksPerDay*5) == 0d) {
			dayOfWeek = 5;
		} else if(offset % (this.ticksPerDay*4) == 0d) {
			dayOfWeek = 4;
		} else if(offset % (this.ticksPerDay*3) == 0d) {
			dayOfWeek = 3;
		} else if(offset % (this.ticksPerDay*2) == 0d) {
			dayOfWeek = 2;
		} else if(offset % this.ticksPerDay == 0d) {
			dayOfWeek = 1;
		}
		
		if(dayOfWeek < 0) 
			return;
		
		int start = this.ticksPerDay * dayOfWeek;
		int end = start + this.ticksPerDay;
		int i = 0;
		//System.out.println("[updateSingleNonDomesticDemand()] From: " + start + " to: " + end);
		for(int j=start; j<end; j++) {
			// convert from GW to kWh on half hourly timeslot
			SingleNonDomesticDemand[i] = this.SingleNonDomesticPower[j] * 1e9/2d;
			i++;
		}
		if(Consts.UPDATE_PHYSICAL_NODE_TO_MARKET) 
			System.arraycopy(ArrayUtils.negate(SingleNonDomesticDemand), 0, arr_PN, 0, SingleNonDomesticDemand.length);
		
	}
	
	public void marketPreStep() {
	
		/**
		 * (02/07/12) DF
		 * 
		 * Update k value (scale factor) for singleNonDomesticPower
		 * */
		
		if(Consts.USE_SINGLE_NON_DOMESTIC_AGGREGATOR) {
			this.updateSingleNonDomesticPower();
			this.updateSingleNonDomesticDemand();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#paramStringReport()
	 */
	@Override
	protected String paramStringReport() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#bizPreStep()
	 */
	@Override
	public void bizPreStep() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent#bizStep()
	 */
	@Override
	public void bizStep() {
		// TODO Auto-generated method stub
		
	}
	
	public SingleNonDomesticAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxDem, double minDem, double[] baseDemand) {
		
		super(context, mb, cat, type, maxDem, minDem, baseDemand);
		this.ticksPerDay = context.getNbOfTickPerDay();
		
		this.lengthOfProfile = ticksPerDay * Consts.NB_OF_DAYS_LOADED_DEMAND;
		
		this.context = context;
		
		ReadINDOValuesFromFile("INDO.csv");
		CalHouseholdPower(context);
		
	}

}
