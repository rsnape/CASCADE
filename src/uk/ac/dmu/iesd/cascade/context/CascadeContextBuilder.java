package uk.ac.dmu.iesd.cascade.context;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;

import cern.jet.random.Normal;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkFactory;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.context.space.graph.NetworkGenerator;
import repast.simphony.context.space.graph.WattsBetaSmallWorldGenerator;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.controller.NullAbstractControllerAction;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.*;
import repast.simphony.random.RandomHelper;
import repast.simphony.scenario.ModelInitializer;
import repast.simphony.scenario.Scenario;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.projection.Projection;
import repast.simphony.ui.RSApplication;
import repast.simphony.util.collections.IndexedIterable;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.Consts.GENERATOR_TYPE;
import uk.ac.dmu.iesd.cascade.Consts.STORAGE_TYPE;
import uk.ac.dmu.iesd.cascade.FactoryFinder;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.util.*;
import repast.simphony.scenario.ModelInitializer;

/**
 * <em>CascadeContextBuilder</em> is the Repast specific starting point class 
 * (i.e. a <code>ContextBuilder</code>) for 
 * building the context (i.e.{@link CascadeContext}) for the <em>Cascade</em> framework.
 * Building a context consists of filling it with agents and other actors/components/etc. and
 * constructing displays/views for the model and so forth. 
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.3 $ $Date: 2011/06/21 12:52:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial scenario creator
 * 1.1 - amend to accommodate refactor of Prosumer into abstract class and 
 * 		specific inherited classes.
 * 1.2 - Class name has been changed;
 *       Restructured the build method by adding private sub-methods;
 *       ContextBuilder type has been changed from ProsumerAgent to Object
 *       Babak 
 * 1.3 - Added facility to pre-condition household prosumer agents with statistically determined
 * 		 occupancy and appliance ownership.  This enables far greater detail of
 * 		 demand calculation within the prosumer agents.  Richard
 * 
 */


/**
 * Builds and returns a context. Building a context consists of filling it with
 * agents, adding projects and so forth. 
 * 
 * @param context
 * @return the built context.
 */
public class CascadeContextBuilder implements ContextBuilder<Object> {
	
	private CascadeContext cascadeMainContext;  // cascade main context
	private Parameters params; // parameters for the model run environment 	
	private int numProsumers; //number of Prosumers
	CSVReader baseDemandReader = null;

	//int ticksPerDay;
	int numDemandColumns;
	Normal thermalMassGenerator = RandomHelper.createNormal(275,75);
	Normal buildingLossRateGenerator = RandomHelper.createNormal(12.5, 2.5);
	float[] monthlyMainsWaterTemp = new float[12];

	/*
	 * Builds the <tt> Cascade Context </tt> (by calling other private sub-methods)
	 * @see uk.ac.dmu.iesd.cascade.context.CascadeContext
	 * @see repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context.Context)
	 * @return built context
	 */
	public CascadeContext build(Context context) {
		// Instantiate the Cascade context, passing in the context that was given to this builder
		// to clone any existing parameters
		//tempCont =context;
		cascadeMainContext = new CascadeContext(context); //build CascadeContext by passing the context
		readParamsAndInitializeArrays();
		populateContext();
	
		//buildNetworks();
		
		//++++++++++++++Add stuff to user panel
		JPanel customPanel = new JPanel();
		customPanel.add(new JLabel("TestLabel"));
		customPanel.add(new JButton("Test Button"));		
		RSApplication.getRSApplicationInstance().addCustomUserPanel(customPanel);
		//+++++++++++++++++++++++++++++++++++++
		
		if (cascadeMainContext.verbose)	
			System.out.println("Cascade Main Context created: "+cascadeMainContext.toString());
		return cascadeMainContext;
	}

	/*
	 * Read the model environment parameters and initialize arrays
	 */
	private void readParamsAndInitializeArrays() {
		// get the parameters from the current run environment
		params = RunEnvironment.getInstance().getParameters();
		String dataFileFolderPath = (String)params.getValue("dataFileFolder");
		String weatherFileName = (String)params.getValue("weatherFile");
		String systemDemandFileName = (String)params.getValue("systemBaseDemandFile");
		String householdAttrFileName = (String)params.getValue("householdBaseAttributeFile");
		String elecLayoutFilename = (String)params.getValue("electricalNetworkLayoutFile");
		numProsumers = (Integer) params.getValue("defaultProsumersPerFeeder");
		cascadeMainContext.setTickPerDay((Integer) params.getValue("ticksPerDay"));
		cascadeMainContext.verbose = (Boolean) params.getValue("verboseOutput");
		Date startDate;
		try {
			startDate = (new SimpleDateFormat("dd/MM/yyyy")).parse((String) params.getValue("startDate"));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			System.err.println("The start date parameter is in a format which cannot be parsed by this program");
			System.err.println("The data will be set to 01/01/2000 by default");
			startDate = new Date(2000,1,1);
			e1.printStackTrace();
		}
		cascadeMainContext.currentDate = new GregorianCalendar();
		cascadeMainContext.currentDate.setTime(startDate);

		/*
		 * Read in the necessary data files and store to the context
		 */
		File dataDirectory = new File(dataFileFolderPath);
		File weatherFile = new File(dataDirectory, weatherFileName);
		CSVReader weatherReader = null;
		File systemDemandFile = new File(dataDirectory, systemDemandFileName);
		CSVReader systemBasePriceReader = null;
		File householdAttrFile = new File(dataDirectory, householdAttrFileName);


		try {
			weatherReader = new CSVReader(weatherFile);
			weatherReader.parseByColumn();

			cascadeMainContext.insolationArray = ArrayUtils.convertStringArrayToFloatArray(weatherReader.getColumn("insolation"));
			cascadeMainContext.windSpeedArray = ArrayUtils.convertStringArrayToFloatArray(weatherReader.getColumn("windSpeed"));
			cascadeMainContext.airTemperatureArray = ArrayUtils.convertStringArrayToFloatArray(weatherReader.getColumn("airTemp"));
			cascadeMainContext.weatherDataLength = cascadeMainContext.insolationArray.length;

		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + weatherFile.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (cascadeMainContext.weatherDataLength % cascadeMainContext.getTickPerDay() != 0)
		{
			System.err.println("Weather data array not a whole number of days. This may cause unexpected behaviour ");
		}

		try {
			systemBasePriceReader = new CSVReader(systemDemandFile);
			systemBasePriceReader.parseByColumn();
			float[] systemBasePriceSignal = ArrayUtils.convertStringArrayToFloatArray(systemBasePriceReader.getColumn("demand"));
			cascadeMainContext.systemPriceSignalDataLength = systemBasePriceSignal.length;
			cascadeMainContext.systemPriceSignalDataArray = Arrays.copyOf(systemBasePriceSignal, systemBasePriceSignal.length);
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdAttrFileName);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}		
		if (cascadeMainContext.systemPriceSignalDataLength % cascadeMainContext.getTickPerDay()!= 0)
		{
			System.err.println("Base System Demand array not a whole number of days. This may cause unexpected behaviour");
		}

		try {
			baseDemandReader = new CSVReader(householdAttrFile);
			baseDemandReader.parseByColumn();
			numDemandColumns = baseDemandReader.columnsStarting("demand");

			if (numDemandColumns == 0)
			{
				System.err.println("The household demand data files appears to have no demand data columns");
				System.err.println("Demand data columns should be headed 'demand' followed by an integer e.g. 'demand0', 'demand1'...");
				System.err.println("Proceeding with no demand data would cause failure, so the program will now terminate");
				System.err.println("Please check file " + householdAttrFile.getAbsolutePath());
				System.exit(1);
			}


		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdAttrFileName);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}

		this.monthlyMainsWaterTemp = Arrays.copyOf(Consts.MONTHLY_MAINS_WATER_TEMP, Consts.MONTHLY_MAINS_WATER_TEMP.length);
	}


	/**
	 * Populate the context(by creating agents, actors, objects, etc)
	 */
	private void populateContext() {
		
		//Convert base demand to half hourly (or whatever fraction of a day we are working with)
		// NOTE - I am assuming input in kWh.  If in kW, this should average rather than sum!!

		// NOT SURE THIS IS A GOOD WAY - SHOULDN'T WE PRE-PROCESS TO THE RIGHT TIME-STEPS?
		// THEREBY MAKING SAME ASSUMPTIONS AS FOR WEATHER ETC
		/*
		 * Populate the context with agents
		 */
		// First Householders

		// TODO: Is this needed?
		// Create a sub-context of householders to allow the social networks (and maybe others)
		// to be differentiated by prosumer type
		// thisContext.addSubContext(new DefaultContext("Households"));
		// Context householdContext = RepastEssentials.CreateNetwork(parentContextPath, netName, isDirected, agentClassName, fileName, format)

		ProsumerFactory prosumerFactory = FactoryFinder.createProsumerFactory(this.cascadeMainContext);

		/* -----------
		 * Richard's DEFRA TEST *****
		 * 
		 * replaced the loop here with the DEFRA consumers
		 */
		float[] householdBaseDemandArray = null;
		for (int i = 0; i < numProsumers; i++) {

			String demandName = "demand" + RandomHelper.nextIntFromTo(0, numDemandColumns - 1);
			if (cascadeMainContext.verbose)
			{
				System.out.println("householdBaseDemandArray is initialised with profile " + demandName);
			}
			householdBaseDemandArray = ArrayUtils.convertStringArrayToFloatArray(baseDemandReader.getColumn(demandName));

			HouseholdProsumer hhProsAgent = prosumerFactory.createHouseholdProsumer(householdBaseDemandArray, true);
			//TODO: We just set smart meter true here - need more sophisticated way to set for different scenarios
			hhProsAgent.hasSmartMeter = true;
			hhProsAgent.hasSmartControl = true;
			cascadeMainContext.add(hhProsAgent);			
		} 

		/*String dataFileFolderPath = (String)params.getValue("dataFileFolder");
		String DEFRAcatFileName = dataFileFolderPath + "\\" + (String)params.getValue("defraCategories");
		String DEFRAprofileFileName = dataFileFolderPath + "\\" + (String)params.getValue("defraProfiles");

		cascadeMainContext.addAll(prosumerFactory.createDEFRAHouseholds(numProsumers, DEFRAcatFileName, DEFRAprofileFileName));*/
		IndexedIterable<HouseholdProsumer> householdProsumers = cascadeMainContext.getObjects(HouseholdProsumer.class);

		/*----------------
		 * Richard's occupancy test code
		 * 
		 * Note that this in effect is assuming that occupancy is independent of 
		 * any of the other assigned variables.  This may not, of course, be true.
		 */
		AgentUtils.assignProbabilisticDiscreteParameter("numOccupants", Consts.NUM_OF_OCCUPANTS_ARRAY, Consts.OCCUPANCY_PROBABILITY_ARRAY, householdProsumers);

		//assign wet appliance ownership.  Based on statistical representation of the BERR 2006 ownership stats
		// with a bias based on occupancy which seems reasonable.
		// TODO: break this out into a separate method.  Store constants somewhere?  Should they read from file?
		for (HouseholdProsumer thisAgent : householdProsumers)
		{
			int occupancy = thisAgent.getNumOccupants();
			double randomVar = RandomHelper.nextDouble();
			if ((occupancy >= 2 && randomVar < 0.85) || (occupancy == 1 && randomVar < 0.62))
			{
				thisAgent.hasWashingMachine = true;
			}

			randomVar = RandomHelper.nextDouble();
			if (!(thisAgent.hasWashingMachine) && ((occupancy >= 2 && randomVar < 0.75) || (occupancy == 1 && randomVar < 0.55)))
			{
				thisAgent.hasWasherDryer = true;
			}

			randomVar = RandomHelper.nextDouble();
			if (!(thisAgent.hasWasherDryer) && ((occupancy >= 3 && randomVar < 0.7) || (occupancy == 2 && randomVar < 0.45) || (occupancy == 1 && randomVar < 0.35)))
			{
				thisAgent.hasTumbleDryer = true;
			}

			randomVar = RandomHelper.nextDouble();
			if (((occupancy >= 3 && randomVar < 0.55) || (occupancy == 2 && randomVar < 0.25) || (occupancy == 1 && randomVar < 0.2)))
			{
				thisAgent.hasDishWasher = true;
			}

			thisAgent.setBuildingHeatLossRate((float) buildingLossRateGenerator.nextDouble());
			thisAgent.setBuildingThermalMass((float) thermalMassGenerator.nextDouble());

			thisAgent.wetApplianceProfile = InitialProfileGenUtils.melodyStokesWetApplianceGen(Consts.DAYS_PER_YEAR, thisAgent.hasWashingMachine, thisAgent.hasWasherDryer, thisAgent.hasDishWasher, thisAgent.hasTumbleDryer);


		}

		//assign electric water heating and space heating
		//This uses a different algorithm - first get all the agents we want and then assign the value
		//NOTE - these are assumed independent - almost certainly NOT!!!
		//TODO: Sort this out
		Iterable waterHeated = cascadeMainContext.getRandomObjects(HouseholdProsumer.class, (long) (numProsumers * (Float) params.getValue("elecWaterFraction")));
		Iterable spaceHeated = cascadeMainContext.getRandomObjects(HouseholdProsumer.class, (long) (numProsumers * (Float) params.getValue("elecSpaceFraction")));
		AgentUtils.assignParameterSingleValue("hasElectricalWaterHeat", true, waterHeated);
		AgentUtils.assignParameterSingleValue("hasElectricalSpaceHeat", true, spaceHeated);

		if(cascadeMainContext.verbose)
		{
			System.out.println("Percentages:");
			System.out.println("households with occupancy 1 : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",1)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 2 : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",2)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 3 : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",3)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 4 : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",4)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 5 : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",5)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 6 : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",6)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 7 : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",7)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 8 : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",8)).query()) / householdProsumers.size());
			System.out.println("Washing Mach : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasWashingMachine",true)).query()) / householdProsumers.size());
			System.out.println("Washer Dryer : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasWasherDryer",true)).query()) / householdProsumers.size());
			System.out.println("Tumble Dryer: " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasTumbleDryer",true)).query()) / householdProsumers.size());
			System.out.println("Dish Washer : " + (float) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasDishWasher",true)).query()) / householdProsumers.size());
		}
		
		buildSocialNetwork();

		//Secondly add aggregator(s)
		AggregatorFactory aggregatorFactory = FactoryFinder.createAggregatorFactory(this.cascadeMainContext);
		RECO firstAggregator = aggregatorFactory.createRECO(cascadeMainContext.systemPriceSignalDataArray);
		//AggregatorAgent firstAggregator = new AggregatorAgent(cascadeMainContext, cascadeMainContext.systemPriceSignalDataArray);
		cascadeMainContext.add(firstAggregator);
		
		buildNetworks(firstAggregator);
	}
	
	
	/**
	 * This method will builds the social networks
	 * TODO: This method will need to be refined later
	 * At this moment, there is only one aggregator and links are simply created
	 * between this aggregator and all the prosumers in the context.
	 * Later this method (or its breakup(s)) can receive parameters such as EdgeSource and EdgeTarget 
	 * to create edges between a source and a target
	 */
	private void buildSocialNetwork() {
		
		//Create the household social network before other agent types are added to the context.
		NetworkFactory networkFactory = NetworkFactoryFinder.createNetworkFactory(null);	
	// create a small world social network
	double beta = 0.1;
	int degree = 2;
	boolean directed = true;
	boolean symmetric = true;
	NetworkGenerator gen = new WattsBetaSmallWorldGenerator(beta, degree, symmetric);
	Network socialNet = networkFactory.createNetwork("socialNetwork", cascadeMainContext, gen, directed);
	//set weight of each social contact - initially random
	//this will represent the influence a contact may have on another
	//Note that influence of x on y may not be same as y on x - which is realistic


	for (Object thisEdge : socialNet.getEdges())
	{
		((RepastEdge) thisEdge).setWeight(RandomHelper.nextDouble());			
	}

	/* -----
	 * Richard test block to fully connect a certain number of agents based
	 * on a property (in this case DEFRA category
	 */
	Query<HouseholdProsumer> cat1Query = new PropertyEquals(cascadeMainContext, "defraCategory",1);
	Iterable<HouseholdProsumer> cat1Agents = cat1Query.query();

	if(Consts.DEBUG)
	{
		System.out.println(" There are " + IterableUtils.count(cat1Agents) + " category 1 agents");
	}

	for (HouseholdProsumer prAgent : cat1Agents)
	{
		for (HouseholdProsumer target : cat1Agents)
		{
			socialNet.addEdge(prAgent, target, Consts.COMMON_INTEREST_GROUP_EDGE_WEIGHT);
		}
	}

	//Add in some generators
	
	this.cascadeMainContext.setSocialNetwork(socialNet);
}
	
	/**
	 * This method will build all the networks
	 * TODO: This method will need to be refined later
	 * At this moment, there is only one aggregator and links are simply created
	 * between this aggregator and all the prosumers in the context.
	 * Later this method (or its breakup(s)) can receive parameters such as EdgeSource and EdgeTarget 
	 * to create edges between a source and a target
	 */
	private void buildNetworks(AggregatorAgent firstAggregator) {
		boolean directed = true;
		
		//Create the household social network before other agent types are added to the context.
		NetworkFactory networkFactory = NetworkFactoryFinder.createNetworkFactory(null);		
		
		/*
		 * Create the projections needed in the context and add agents to those projections
		 */
		GeographyParameters geoParams = new GeographyParameters();
		Geography geography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("Geography", cascadeMainContext, geoParams);

		// Create null networks for other than social at this point.

		// Economic network should be hierarchical aggregator to prosumer 
		// TODO: Decide what economic network between aggregators looks like?
		// TODO: i.e. what is market design for aggregators?
		Network economicNet = networkFactory.createNetwork("economicNetwork", cascadeMainContext, directed);
		
		// TODO: replace this with something better.  Next iteration of code
		// should probably take network design from a file
		for (ProsumerAgent prAgent:(Iterable<ProsumerAgent>) (cascadeMainContext.getObjects(ProsumerAgent.class)) )
		{
			economicNet.addEdge(firstAggregator, prAgent);
		}
		
		this.cascadeMainContext.setEconomicNetwork(economicNet);

		
		// We should create a bespoke network for the electrical networks.
		// ProsumerAgents only - edges should have nominal voltage and capacity
		// attributes.  TODO: How do we deal with transformers??
		Network physicalNet = networkFactory.createNetwork("electricalNetwork", cascadeMainContext, directed);
		// TODO: How does info network differ from economic network?
		Network infoNet = networkFactory.createNetwork("infoNetwork", cascadeMainContext, directed);
		
				
		for (ProsumerAgent prAgent:(Iterable<ProsumerAgent>) (cascadeMainContext.getObjects(ProsumerAgent.class)) )
		{
			infoNet.addEdge(firstAggregator, prAgent);
		}
		
		
		
		//+++Babak TESTS +++++++++++++++++++++++++++++++++++++
		
		/*
		//AggregatorContext aggContext2 = new AggregatorContext(tempCont);
		//aggContext2.getTickPerDay()
		//System.out.println(" cascadeContext. getTick perday: "+this.cascadeMainContext.getTickPerDay());
		//System.out.println(" aggContext2. getTick perday: "+aggContext2.getTickPerDay());

		AggregatorFactory aggregatorFactory = FactoryFinder.createAggregatorFactory(this.cascadeMainContext);
		//AggregatorFactory aggregatorFactory = FactoryFinder.createAggregatorFactory(aggContext2);
	
		RECO secondAggregator = aggregatorFactory.createRECO(cascadeMainContext.systemPriceSignalDataArray);
		cascadeMainContext.add(secondAggregator);
		
		//aggContext2.add(secondAggregator);
		
		ProsumerFactory prosumerFactroy = FactoryFinder.createProsumerFactory(this.cascadeMainContext);
		//ProsumerFactory prosumerFactroy = FactoryFinder.createProsumerFactory(aggContext2);

		HouseholdProsumer hhPros1 = prosumerFactroy.createHouseholdProsumer(householdBaseDemandArray, true);
		hhPros1.setAgentName("HH-Pro1");
		cascadeMainContext.add(hhPros1);
		HouseholdProsumer hhPros2 = prosumerFactroy.createHouseholdProsumer(householdBaseDemandArray, true);
		hhPros2.setAgentName("HH-Pro2");
		cascadeMainContext.add(hhPros2);
		HouseholdProsumer hhPros3 = prosumerFactroy.createHouseholdProsumer(householdBaseDemandArray, true);
		hhPros3.setAgentName("HH-Pro3");
		cascadeMainContext.add(hhPros3);
		HouseholdProsumer hhPros4 = prosumerFactroy.createHouseholdProsumer(householdBaseDemandArray, true);
		hhPros4.setAgentName("HH-Pro4");
		cascadeMainContext.add(hhPros4);
		
		//Network recoCostumersNet = networkFactory.createNetwork(firstAggregator.toString(), cascadeMainContext, directed);
	
		//Network aggTestNet = networkFactory.createNetwork("BabakTestNetwork", cascadeMainContext, directed);
		//AggregatorContext aggContext = new AggregatorContext();
		//aggContext2.add(hhPros3);
		//aggContext2.add(hhPros4);
		
		//cascadeMainContext.addSubContext(aggContext2);
		//Network aggTestNet = networkFactory.createNetwork("BabakTestNetwork", aggContext2, directed);
		Network aggTestNet = networkFactory.createNetwork("BabakTestNetwork", cascadeMainContext, directed);

		aggTestNet.addEdge(firstAggregator, hhPros1);
		aggTestNet.addEdge(firstAggregator, hhPros2);
		
		aggTestNet.addEdge(secondAggregator, hhPros3);
		aggTestNet.addEdge(secondAggregator, hhPros4);
		
	 */
		// ------End of tests ---------------------------


	}


}

