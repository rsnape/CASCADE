/**
 * 
 */
package prosumermodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkFactory;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.context.space.graph.NetworkGenerator;
import repast.simphony.context.space.graph.WattsBetaSmallWorldGenerator;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.graph.Network;
import repast.simphony.space.projection.Projection;

/**
 * @author jrsnape
 * @version $Revision: 1.0 $ $Date: 2010/11/17 17:00:00 $
 */
public class SmartGridCreator implements ContextBuilder<ProsumerAgent> {

	/**
	 * Builds and returns a context. Building a context consists of filling it with
	 * agents, adding projects and so forth. When this is called for the master context
	 * the system will pass in a created context based on information given in the
	 * model.score file. When called for subcontexts, each subcontext that was added
	 * when the master context was built will be passed in.
	 * 
	 * TODO: Need to make this specific so it can only build a SmartGridContext
	 *
	 * @param context
	 * @return the built context.
	 */

	// The context which this Creator acts upon.
	SmartGridContext thisContext;
	// The parameters for the run environment in which this creator is running
	Parameters parm;
	
	enum generatorType {
		COAL, GAS, OIL, NUCLEAR, BIOMASS, WIND, HYDRO, CHP, SOLAR
	}
	
	enum storageType {
		BATTERY, HYDRO, HEAT, FLYWHEEL, EV
	}
	
	/*
	 * builds a SmartGridContext
	 * 
	 * @see prosumermodel.SmartGridContext
	 * @see repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context.Context)
	 */
	public SmartGridContext build(Context context) {
		// Instantiate a Smart Grid Context, passing in the context that was given to this builder
		// to clone any existing parameters
		thisContext = new SmartGridContext(context);

		// get the parameters from the current run environment
		parm = RunEnvironment.getInstance().getParameters();
		String weatherFileName = (String)parm.getValue("weatherFile");
		String dataFileFolderPath = (String)parm.getValue("dataFileFolder");
		String systemDemandFileName = (String)parm.getValue("systemBaseDemandFile");
		String householdAttrFileName = (String)parm.getValue("householdBaseAttributeFile");
		String elecLayoutFilename = (String)parm.getValue("electricalNetworkLayoutFile");
		int numProsumers = (Integer) parm.getValue("defaultProsumersPerFeeder");
		int ticksPerDay = (Integer) parm.getValue("ticksPerDay");

		/*
		 * Read in the necessary data files and store to the context
		 */
		File dataDirectory = new File(dataFileFolderPath);
		
		File weatherFile = new File(dataDirectory, weatherFileName);
		CSVReader weatherReader;
		try {
			weatherReader = new CSVReader(weatherFile);
			weatherReader.parseByColumn();
			
			thisContext.insolation = ArrayUtils.convertStringArraytofloatArray(weatherReader.getColumn("insolation"));
			thisContext.windSpeed = ArrayUtils.convertStringArraytofloatArray(weatherReader.getColumn("windSpeed"));
			thisContext.airTemperature = ArrayUtils.convertStringArraytofloatArray(weatherReader.getColumn("airTemp"));
			thisContext.weatherDataLength = thisContext.insolation.length;
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + weatherFile.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (thisContext.weatherDataLength % ticksPerDay != 0)
		{
			System.err.println("Weather data array not a whole number of days");
			System.err.println("May cause unexpected behaviour");
		}
		
		File systemDemandFile = new File(dataDirectory, systemDemandFileName);
		CSVReader systemBasePriceReader;
		try {
			systemBasePriceReader = new CSVReader(systemDemandFile);
			systemBasePriceReader.parseByColumn();
			float[] systemBasePriceSignal = ArrayUtils.convertStringArraytofloatArray(systemBasePriceReader.getColumn("demand"));
			thisContext.systemPriceSignalDataLength = systemBasePriceSignal.length;
			thisContext.systemPriceSignalData = Arrays.copyOf(systemBasePriceSignal, systemBasePriceSignal.length);
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdAttrFileName);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}		
		if (thisContext.systemPriceSignalDataLength % ticksPerDay != 0)
		{
			System.err.println("Base System Demand array not a whole number of days");
			System.err.println("May cause unexpected behaviour");
		}

		File householdAttrFile = new File(dataDirectory, householdAttrFileName);
		CSVReader demandReader;
		float[] householdBaseDemand = null;
		try {
			demandReader = new CSVReader(householdAttrFile);
			demandReader.parseByColumn();
			householdBaseDemand = ArrayUtils.convertStringArraytofloatArray(demandReader.getColumn("demand"));
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdAttrFileName);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (householdBaseDemand.length % ticksPerDay != 0)
		{
			System.err.println("Household base demand array not a whole number of days");
			System.err.println("May cause unexpected behaviour");
		}

		//Convert base demand to half hourly (or whatever fraction of a day we are working with)
		// NOTE - I am assuming input in kWh.  If in kW, this should average rather than sum!!
		
		// NOT SURE THIS IS A GOOD WAY - SHOULDN'T WE PRE-PROCESS TO THE RIGHT TIME-STEPS?
		// THEREBY MAKING SAME ASSUMPTIONS AS FOR WEATHER ETC

/*//		int sumPeriod = (Integer) parm.getValue("ticksPerDay");
//		int inputLength = householdBaseDemand.length;
//		int sumElements = inputLength / sumPeriod;
//		float[] householdBaseDemandInTicks = new float[sumPeriod];
//		
//		for (int iter = 0; iter < sumPeriod; iter++)
//		{
//			float sumDemand = 0;
//			for (int p = 0; p < sumElements; p++)
//			{
//				sumDemand = sumDemand + householdBaseDemand[(iter*sumElements) + p];
//			}
//			householdBaseDemandInTicks[iter] = sumDemand;
//		}
*/	
		
		/*
		 * Populate the context with agents
		 */
		// First Householders
		
		// TODO: Is this needed?
		// Create a sub-context of householders to allow the social networks (and maybe others)
		// to be differentiated by prosumer type
		// thisContext.addSubContext(new DefaultContext("Households"));
		// Context householdContext = RepastEssentials.CreateNetwork(parentContextPath, netName, isDirected, agentClassName, fileName, format)
		
		for (int i = 0; i < numProsumers; i++) {
			// Introduce variability in agents here or in their constructor
			ProsumerAgent newAgent = createHouseholdProsumer(householdBaseDemand, true);
			thisContext.add(newAgent);			
		}
		
		//Create the household social network before other agent types are added to the context.
		NetworkFactory smartFactory = NetworkFactoryFinder.createNetworkFactory(null);
		
		// create a small world social network
		double beta = 0.1;
		int degree = 4;
		boolean directed = false;
		boolean symmetric = false;
		NetworkGenerator gen = new WattsBetaSmallWorldGenerator(beta, degree, symmetric);
		smartFactory.createNetwork("socialNetwork", thisContext, gen, directed);
		
		
		//Secondly add aggregator(s)
		AggregatorAgent firstAggregator = new AggregatorAgent((String) thisContext.getId(), thisContext.systemPriceSignalData, parm);
		thisContext.add(firstAggregator);
		
		/*
		 * Create the projections needed in the context and add agents to those projections
		 */
		GeographyParameters geoParams = new GeographyParameters();
		Geography geography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("Geography", thisContext, geoParams);

		
		
		// Create null networks for other than social at this point.
		
		// Economic network should be hierarchical aggregator to prosumer 
		// TODO: Decide what economic network between aggregators looks like?
		// TODO: i.e. what is market design for aggregators?
		Network economicNet = smartFactory.createNetwork("economicNetwork", thisContext, directed);
		
		// TODO: replace this with something better.  Next iteration of code
		// should probably take network design from a file
		for (ProsumerAgent thisAgent:(Iterable<ProsumerAgent>) (thisContext.getObjects(ProsumerAgent.class)) )
		{
			economicNet.addEdge(firstAggregator, thisAgent);
		}
		
		// We should create a bespoke network for the electrical networks.
		// ProsumerAgents only - edges should have nominal voltage and capacity
		// attributes.  TODO: How do we deal with transformers??
		Network physicalNet = smartFactory.createNetwork("electricalNetwork", thisContext, directed);
		// TODO: How does info network differ from economic network?
		Network infoNet = smartFactory.createNetwork("infoNetwork", thisContext, directed);
		
		for (ProsumerAgent thisAgent:(Iterable<ProsumerAgent>) (thisContext.getObjects(ProsumerAgent.class)) )
		{
			infoNet.addEdge(firstAggregator, thisAgent);
		}
		System.out.println(thisContext.toString());

		return thisContext;
	}
	
	/*
	 * Helper methods
	 */
	
	/*
	 * This method simply adds a random element to the base profile to create a household demand
	 * It should be over-ridden in the future to use something better - for instance melody's model
	 * or something which time-shifts demand somewhat, or select one of a number of typical profiles
	 * based on occupancy.
	 */
	private float[] createRandomHouseholdDemand(float[] baseProfile){
		float[] newProfile = new float[baseProfile.length];
		
		for (int i = 0; i < newProfile.length; i++)
		{
			newProfile[i] = baseProfile[i] * (float)(1 + 0.3*(RandomHelper.nextDouble() - 0.5));
		}
		
		return newProfile;
	}
	
	/*
	 * Creates a prosumer to represent a pure storage.  Therefore zero
	 * base demand, set the generator type and capacity.
	 * 
	 * TODO: should the base demand be zero or null???
	 * 
	 * @param Capacity - the generation capacity of this agent (kWh)
	 * @param type - the type of this generator (from an enumerator of all types)
	 */
	public ProsumerAgent createStorageProsumer(int Capacity, storageType type) {
		ProsumerAgent thisAgent;
		
		// Create a prosumer with zero base demand
		float[] nilDemand = new float[1];
		nilDemand[0] = 0;
		thisAgent = new ProsumerAgent((String) thisContext.getId(), nilDemand, parm);
		switch (type){
		}
		
		return thisAgent;
	}
	
	/*
	 * Creates a prosumer to represent a pure generator.  Therefore zero
	 * base demand, set the generator type and capacity.
	 * 
	 * TODO: should the base demand be zero or null???
	 * 
	 * @param Capacity - the generation capacity of this agent (kWh)
	 * @param type - the type of this generator (from an enumerator of all types)
	 */
	public ProsumerAgent createPureGenerator(int Capacity, generatorType type) {
		ProsumerAgent thisAgent;
		
		// Create a prosumer with zero base demand
		// TODO: Should this be null?  or zero as implemented?
		float[] nilDemand = new float[1];
		nilDemand[0] = 0;
		thisAgent = new ProsumerAgent((String) thisContext.getId(), nilDemand, parm);
		switch (type){
			
		}
		
		return thisAgent;
	}
	
	/*
	 * Creates a household prosumer with a basic consumption profile as supplied
	 * with or without added noise
	 * 
	 * @param baseProfile - an array of the basic consumption profile for this prosumer (kWh per tick)
	 * @param addNoise - boolean specifying whether or not to add noise to the profile
	 */
	public ProsumerAgent createHouseholdProsumer(float[] baseProfile, boolean addNoise) {
		ProsumerAgent thisAgent;
		
		if (addNoise) 
		{
			thisAgent = new ProsumerAgent((String) thisContext.getId(), createRandomHouseholdDemand(baseProfile), parm);
		}
		else
		{
			thisAgent = new ProsumerAgent((String) thisContext.getId(), baseProfile, parm);
		}
		//thisAgent.exercisesBehaviourChange = (RandomHelper.nextDouble() > 0.5);
		thisAgent.exercisesBehaviourChange = true;
		thisAgent.hasSmartMeter = true;
		thisAgent.costThreshold = 35000;  //Threshold in price signal at which behaviour change is prompted (if agent is willing)
		thisAgent.minSetPoint = 18;
		thisAgent.maxSetPoint = 21;
		thisAgent.currentInternalTemp = 19;
		
		return thisAgent;
	}
	
	/*
	 * Creates a non-domestic prosumer with a basic consumption profile as supplied
	 * with or without added noise
	 * 
	 * @param baseProfile - an array of the basic consumption profile for this prosumer (kWh per tick)
	 * @param addNoise - boolean specifying whether or not to add noise to the profile
	 */
	public ProsumerAgent createNonDomesticProsumer(float[] baseProfile, boolean addNoise) {
		ProsumerAgent thisAgent;
		
		// Create a prosumer with  base demand as specified
		thisAgent = new ProsumerAgent((String) thisContext.getId(),baseProfile, parm);
				
		return thisAgent;
	}

}
