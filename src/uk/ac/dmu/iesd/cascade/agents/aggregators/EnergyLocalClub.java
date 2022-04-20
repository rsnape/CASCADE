/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.aggregators;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.IllegalParameterException;
import repast.simphony.util.collections.IndexedIterable;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * @author Richard
 *
 */
public class EnergyLocalClub extends SupplierCoAdvancedModel
{
	
	double[] totalDemandPerDay;
	double[] sharePerDay;
	double[] totalGenPerDay;
	double[] importPerDay;
	double[] exportPerDay;
	CSVWriter res;
	CSVWriter hh_net;
	double[] tariff;
	double[] dsp;
	private double localGenPrice;
	private double originalPrice;
	double[] dayCost;
	double[] noSchemeCost;
	
	ArrayList<Double> netDemands;
	ArrayList<Double> localShares;
	private String configString;
	private CSVWriter demandOutput;

	
	/*
	 * This function implements the Energy Local sharing model - specifically
	 * Local generation is first netted off within the prosumer i.e.
	 * if someone uses their own PV, that never "makes it" into the Energy Local model.
	 * 
	 * Next, we find all prosumers with net +ve demand > 0.01 kWh per half hour (i.e. non-noise)
	 * 
	 * We do an equal share of generation between all those prosumers and net off demand. HOWEVER!
	 * If a particular household cannot "soak up" that amount, we keep it in the pool and re-run the procedure.
	 * 
	 * Obviously generators effectively do not participate in the sharing rounds as they are already netted off.
	 */
	public ArrayList<Double> calculateShare()
	{
		this.netDemands = new ArrayList<Double>();
		//ArrayList<Double> monetaryBenefits = new ArrayList<Double>();
		double total_to_share = 0;
		int sharer_count = 0;
		
		for (ProsumerAgent c : this.customers)
		{
			double nd = c.getNetDemand();
			netDemands.add(nd);
			
			if (nd < 0)
			{
				/* There is something to share here */
				total_to_share += -nd;
			}
			else if (nd > 0.01) //arbitrary cut-off constant from Robin.
			{
				sharer_count++;
			}
		}
		
		//boolean all_gen_used = this.getNetDemand() <= 0;
		int share_rounds =1;
			
		ArrayList<Double> originalNetDemands = new ArrayList<Double>(netDemands); //Create a new copy
		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Apparently a sharing total of " + total_to_share + " from the following demands:");
			this.mainContext.logger.debug(netDemands);
		}
		
		
		while (total_to_share > 0.000001 && sharer_count > 0) //Arbitrary precision constant ;p
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("Trying to share " + total_to_share + " between " + sharer_count + " in round " + share_rounds);
			}
			double sharing_proposal = total_to_share / sharer_count;
			ArrayList<Double> postShareNetDemands = new ArrayList<Double>();

			for (double nd : netDemands)
			{
				if (nd >= sharing_proposal)
				{
					postShareNetDemands.add(nd - sharing_proposal);
					total_to_share -= sharing_proposal;
					if (this.mainContext.logger.isDebugEnabled())
					{
						this.mainContext.logger.debug("Removed from total_to_share(Now="+total_to_share+") after sharing with "+postShareNetDemands.size() + "households.");
					}
				} 
				else
				{
					/*
					 * Either this is a net exporter, or the share completely covers the demand
					 */

					if (nd > 0)
					{
						postShareNetDemands.add(0.0);

						/*
						 * This is the case where share covers more than the demand.
						 * 
						 * In this case, they have their demands covered, but the rest remains
						 * in the pot and they cannot share in the next round
						 * 
						 * For net exporters, we do not want to change sharing total at all.
						 */
						total_to_share -= nd;
						sharer_count -= 1;
					}
					else
					{
						//Net exporter
						postShareNetDemands.add(nd);
					}
				}
				
				netDemands = postShareNetDemands;
			}
			
			share_rounds += 1;

		}
	
		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("After sharing, demands are " + netDemands);
			if (total_to_share > 0)
			{
			this.mainContext.logger.debug("In this case, there was a net export of " + total_to_share + ". This should equal the club's net demand where that is negative.");
			}
		}
		
		//Return the amount of locally generated power used up by the community, 
		//Which will simply be the original net demands minus the net demands
		//now
		
		
		ArrayList<Double> retDemands = new ArrayList<Double>();
		int s = this.netDemands.size();
		for (int i=0; i<s; i++)
		{
			//if (originalNetDemands.get(i) > 0)
			{
				retDemands.add(originalNetDemands.get(i) - this.netDemands.get(i));
			}
			//else
			//{
			//	retDemands.add(0.0);
			//}
		}
		return retDemands;
	}
	
	
	
	public void calculateDailyCosts()
	{
		int ts = this.mainContext.getTimeslotOfDay();
				
		if ( ts == 0)
		{
			String appendToFile = writeCosts();
			res.appendText(appendToFile); 
			this.dayCost = new double[localShares.size()];
			this.noSchemeCost = new double[localShares.size()];
			this.totalDemandPerDay = new double[localShares.size()];
			this.sharePerDay = new double[localShares.size()];
			this.totalGenPerDay = new double[localShares.size()];
			this.importPerDay = new double[localShares.size()];
			this.exportPerDay = new double[localShares.size()];

		}
		
		for (int j = 0; j < localShares.size(); j++)
		{
			this.dayCost[j] += (this.localGenPrice*localShares.get(j) + this.tariff[ts]*this.netDemands.get(j));
			this.noSchemeCost[j] += this.originalPrice*(localShares.get(j) + this.netDemands.get(j));
			this.totalDemandPerDay[j] += localShares.get(j) + this.netDemands.get(j);
			this.sharePerDay[j] += localShares.get(j);
			//this.totalGenPerDay[j] +=
			double totDem = 0;
			for (double d : this.netDemands)
			{
				totDem += d;
			}
			if (totDem < 0)
			{
				this.exportPerDay[j] += totDem;
			}
			else
			{
				this.importPerDay[j] += totDem;
			}
			//hh_net.appendText(Double.toString(totDem));
		}



	}
	
	private String writeCosts()
	{
		if (this.dayCost == null || this.netDemands == null)
		{
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(this.mainContext.getTickCount());
		for (double d : this.dayCost)
		{
			sb.append(","+d);
		}
		
		for (double d : this.noSchemeCost)
		{
			sb.append(","+d);
		}
		
		for (double d : this.netDemands)
		{
			sb.append(","+d);
		}
		
		for (double d : this.sharePerDay)
		{
			sb.append(","+d);
		}
		
		return sb.toString();
	}
	
	
	/*
	 * Add in Energy Local specific functionality here
	 * 
	 * (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCoAdvancedModel#bizPreStep()
	 */
	@Override
	public void bizPreStep() {
		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Energy Local club pre-step");
		}
		super.bizPreStep();
		
	}
	
	
	/*
	 * Add in Energy Local specific functionality here
	 * 
	 * (non-Javadoc)
	 * @see uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCoAdvancedModel#bizPreStep()
	 */
	@Override
	public void bizStep() {
		if(this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Entering EnergyLocalClub bizStep");
		}
		super.bizStep();
		this.localShares = this.calculateShare();
		
		if(this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Locally shared generation per household "+localShares);
		}
		
		this.calculateDailyCosts();
		this.writeNetDemand();

	}



	/**
	 *
	 */
	private void writeNetDemand() {
		int t = this.mainContext.getTickCount();
		if ( t < 48) return; //All arrays meaningless until 1 day has passed.
		int ts = this.mainContext.getTimeslotOfDay();
		if (ts == 0)
		{

			IndexedIterable<HouseholdProsumer> list_hhProsumers = this.mainContext.getObjects(HouseholdProsumer.class);

			double[] sum_spaceHeat = new double [this.mainContext.ticksPerDay];
			double[] sum_hotWater = new double [this.mainContext.ticksPerDay];
			double[] sum_other = new double [this.mainContext.ticksPerDay];
			double[] sum_cold = new double [this.mainContext.ticksPerDay];
			double[] sum_wet = new double [this.mainContext.ticksPerDay];
			double[] sum_EV = new double [this.mainContext.ticksPerDay];
			for (HouseholdProsumer hhAgent : list_hhProsumers)
			{
				sum_spaceHeat=ArrayUtils.add(sum_spaceHeat,hhAgent.getHistoricalSpaceHeatDemand());
				sum_hotWater = ArrayUtils.add(sum_hotWater,hhAgent.getHistoricalWaterHeatDemand());
				sum_other = ArrayUtils.add(sum_other,hhAgent.getHistoricalOtherDemand());
				sum_cold = ArrayUtils.add(sum_cold,hhAgent.getHistoricalColdDemand());
				sum_wet = ArrayUtils.add(sum_wet,hhAgent.getHistoricalWetDemand());
				sum_EV = ArrayUtils.add(sum_EV,hhAgent.getHistoricalEVDemand());
			}

			double[] data = new double [8];
			for (int j=0; j<this.mainContext.ticksPerDay; j++)
			{
				data[0] = t-48+j;
				data[1]= sum_spaceHeat[j];
				data[2]= sum_hotWater[j];
				data[3]= sum_cold[j];
				data[4]= sum_wet[j];
				data[5]= sum_EV[j];
				data[6]= sum_other[j];
				data[7]= this.arr_day_D[j];

				this.demandOutput.appendRow(data);
			}
		}
	}



	/**
	 * @param context
	 * @param mb
	 * @param cat
	 * @param type
	 * @param maxDem
	 * @param minDem
	 * @param baseDemand
	 */
	public EnergyLocalClub(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxDem, double minDem,
			double[] baseDemand)
	{
		super(context, mb, cat, type, maxDem, minDem, baseDemand);
		this.init();
	}

	/**
	 * @param context
	 */
	public EnergyLocalClub(CascadeContext context)
	{
		super(context);
		context.logger.info("Adding Energy Local Club");

		this.init();
	}



	/**
	 * 
	 */
	private void init()
	{
		this.originalPrice = 12.0;
        this.tariff = new double[]{
        		 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25,
        		 12,12,12,12,12,12,12,12,12,12,
        		 10,10,10,10,10,10,10,10,10,10,
        		 14,14,14,14,14,14,14,14,
        		 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25, 7.25};

        //DSP is used if signal set non-adaptive
        this.dsp = new double[]{
        		-0.251,	-0.251,	-0.251,	-0.251,	-0.251,	-0.251,	-0.275, -0.275,	-0.275,	-0.275,	-0.275,	-0.275,
        		-0.137,-0.137,	-0.137,	-0.137,	-0.137,	-0.137,	-0.137,	-0.137,
        		-0.157,	-0.157,	-0.157,
        		-0.167,	-0.167,	-0.167,	-0.167,
        		-0.157,	-0.157,	-0.157,	-0.157,-0.157,	-0.157,	-0.157,
        		1,1,1,1,1,1,1,1,
        		-0.24,-0.24,-0.24,-0.24,
        		-0.275,	-0.275};
        this.defaultSignal = this.dsp;

        this.localGenPrice = 7.0;
        
        //Make the default signal for Energy Local the price signal
        this.defaultSignal = ArrayUtils.normalizeValues(this.tariff);

        if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug("Initialising club prices");
		}
        
        /*
         * Initialise an output file.  This really needs streamlining!!
         * 
         */
		String parsedDate = (new SimpleDateFormat("yyyy.MMM.dd.HH_mm_ss_z")).format(new Date());
		String outputDir = (String) RepastEssentials.GetParameter("outputDir");

        // Get a configuration file for the aggregator if it exists.
        // If not - default to using values from the parameter window of the
        // simulation
        try
		{
			this.configString = (String) RepastEssentials.GetParameter("configFileName");
		}
		catch (IllegalParameterException e)
		{
			this.configString = "FromParamWindowNotFile";
		}
        this.aggOutputSubDir = outputDir.concat(File.separator)
				.concat("Seed")
						.concat(Integer.toString(this.mainContext
								.getRandomSeedValue())).concat(this.configString).concat("_").concat(parsedDate);

		String sigMode = RepastEssentials.GetParameter("signalMode").toString();
        this.res = new CSVWriter(outputDir +"/EnergyLocalOutput_Mode"+sigMode+"_"+configString+"_"+parsedDate+".csv" , true);
		this.demandOutput = new CSVWriter(outputDir+"/ELAggNetDemand_Mode"+sigMode+"_"+configString+"_"+parsedDate+".csv" , true);
		ScheduleParameters stop = ScheduleParameters.createAtEnd(ScheduleParameters.LAST_PRIORITY);
		RunEnvironment.getInstance().getCurrentSchedule().schedule(stop,this,"closeFiles");

	}

	public void closeFiles()
	{
		this.res.close();
		this.demandOutput.close();
	}
	@ScheduledMethod(start=0, priority=Consts.AGGREGATOR_PRE_STEP_PRIORITY_FIRST)
	public void writeHeaders()
	{
        ArrayList<String> headers = new ArrayList<String>();
        headers.add("Tick");
        int custCount = this.customers.size();
        for (int i = 0; i < custCount; i++)
        {
        	headers.add("Customer "+i+" cost with Scheme (p)");
        }
        for (int i = 0; i < custCount; i++)
        {
        	headers.add("Customer "+i+" cost no scheme (p)");
        }
        for (int i = 0; i < custCount; i++)
        {
        	headers.add("Customer "+i+" net demand (kWh)");
        }
        for (int i = 0; i < custCount; i++)
        {
        	headers.add("Customer "+i+" PV share");
        }


        String[] headArray = new String[headers.size()];
        headers.toArray(headArray);
        this.res.writeColHeaders(headArray); 
	}
	
	@ScheduledMethod(start=0, priority=Consts.AGGREGATOR_PRE_STEP_PRIORITY_FIRST)
	public void writeDemHeaders()
	{
		String[] headers = {"Tick","Space","Water", "Cold", "Wet", "EV", "Other", "Net"};
		this.demandOutput.writeColHeaders(headers);
	}

}
