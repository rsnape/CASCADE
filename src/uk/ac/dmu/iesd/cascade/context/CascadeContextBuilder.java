package uk.ac.dmu.iesd.cascade.context;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;

import org.jfree.ui.RefineryUtilities;

import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
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
import repast.simphony.engine.environment.GUIRegistry;
import repast.simphony.engine.environment.GUIRegistryType;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.IAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.ParameterConstants;
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
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;
import repast.simphony.util.collections.Pair;
import uk.ac.cranfield.cascade.aggregators.TestBattryConsumers;
import uk.ac.cranfield.cascade.aggregators.TestConsumer;
import uk.ac.cranfield.cascade.market.*;
import uk.ac.dmu.iesd.cascade.Consts;
//import uk.ac.dmu.iesd.cascade.Consts.GENERATOR_TYPE;
//import uk.ac.dmu.iesd.cascade.Consts.STORAGE_TYPE;
import uk.ac.dmu.iesd.cascade.FactoryFinder;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.test.HHProsumer;
import uk.ac.dmu.iesd.cascade.util.*;
import repast.simphony.scenario.ModelInitializer;


/**
 * CASCADE Project Version [ Model Built version] (Version# for the entire project/ as whole)
 * @version $Revision: 2.00 $ $Date: 2011/10/05 
 * 
 * Major changes for this submission include: 
 * • All the elements ('variable' declarations, including data structures consisting of values 
 * [constant] or variables) of the type 'float' (32 bits) have been changed to 'double' (64 bits) 
 * 
 */


/**
 * <em>CascadeContextBuilder</em> is the Repast specific starting point class 
 * (i.e. a <code>ContextBuilder</code>) for 
 * building the context (i.e.{@link CascadeContext}) for the <em>Cascade</em> framework.
 * Building a context consists of filling it with agents and other actors/components/etc. and
 * constructing displays/views for the model and so forth. 
 * 
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.4 $ $Date: 2011/11/15 14:00:00 $
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
 * 1.4 - New populateContext() method, where creation of HHPro agents along with their 
 *       electricity consumption can be dis/operationalized (i.e. controllable); Babak
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
	//Normal buildingLossRateGenerator = RandomHelper.createNormal(275,75);
	//Normal thermalMassGenerator = RandomHelper.createNormal(12.5, 2.5);

	double[] monthlyMainsWaterTemp = new double[12];

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
		cascadeMainContext.setTotalNbOfProsumers(numProsumers);
		cascadeMainContext.setNbOfTickPerDay((Integer) params.getValue("ticksPerDay"));
		cascadeMainContext.verbose = (Boolean) params.getValue("verboseOutput");
		cascadeMainContext.chartSnapshotOn = (Boolean) params.getValue("chartSnapshot");
		cascadeMainContext.setChartSnapshotInterval((Integer) params.getValue("chartSnapshotInterval"));
		
		cascadeMainContext.setRandomSeedValue((Integer)params.getValue("randomSeed"));

		// RunEnvironment.getInstance().
		Date startDate;
		try {
			startDate = (new SimpleDateFormat("dd/MM/yyyy")).parse((String) params.getValue("startDate"));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			System.err.println("CascadeContextBuilder: The start date parameter is in a format which cannot be parsed by this program");
			System.err.println("CascadeContextBuilder: The data will be set to 01/01/2000 by default");
			startDate = new Date(2000,1,1);
			e1.printStackTrace();
		}
		cascadeMainContext.simulationCalendar = new GregorianCalendar();
		cascadeMainContext.simulationCalendar.setTime(startDate);

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

			cascadeMainContext.insolationArray = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("insolation"));
			cascadeMainContext.windSpeedArray = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("windSpeed"));
			cascadeMainContext.airTemperatureArray = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("airTemp"));
			cascadeMainContext.weatherDataLength = cascadeMainContext.insolationArray.length;

		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + weatherFile.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (cascadeMainContext.weatherDataLength % cascadeMainContext.getNbOfTickPerDay() != 0)
		{
			System.err.println("Weather data array not a whole number of days. This may cause unexpected behaviour ");
		}

		try {
			systemBasePriceReader = new CSVReader(systemDemandFile);
			systemBasePriceReader.parseByColumn();
			double[] systemBasePriceSignal = ArrayUtils.convertStringArrayToDoubleArray(systemBasePriceReader.getColumn("demand"));
			cascadeMainContext.systemPriceSignalDataLength = systemBasePriceSignal.length;
			cascadeMainContext.systemPriceSignalDataArray = Arrays.copyOf(systemBasePriceSignal, systemBasePriceSignal.length);
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdAttrFileName);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}		
		if (cascadeMainContext.systemPriceSignalDataLength % cascadeMainContext.getNbOfTickPerDay()!= 0)
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


	private void createTestHHProsumersAndAddThemToContext() {

		ProsumerFactory prosumerFactory = FactoryFinder.createProsumerFactory(this.cascadeMainContext);

		double[] householdBaseDemandArray = null;
		for (int i = 0; i < numProsumers; i++) {

			String demandName = "demand" + RandomHelper.nextIntFromTo(0, numDemandColumns - 1);

			if (cascadeMainContext.verbose)
			{
				System.out.println("CascadeContextBuilder: householdBaseDemandArray is initialised with profile " + demandName);
			}
			householdBaseDemandArray = ArrayUtils.convertStringArrayToDoubleArray(baseDemandReader.getColumn(demandName));

			HHProsumer hhProsAgent = prosumerFactory.createHHProsumer(householdBaseDemandArray, false);

			cascadeMainContext.add(hhProsAgent);			
		} 
	}

	private void createHouseholdProsumersAndAddThemToContext(int occupancyNb) {

		ProsumerFactory prosumerFactory = FactoryFinder.createProsumerFactory(this.cascadeMainContext);

		double[] householdMiscDemandArray = null; //Misc demand profile array consists of electricity demand for lightening, entertainment, computer and small appliances  
		for (int i = 0; i < numProsumers; i++) {

			String demandName = "demand" + RandomHelper.nextIntFromTo(0, numDemandColumns - 1);

			if (cascadeMainContext.verbose)
			{
				System.out.println("CascadeContextBuilder: householdBaseDemandArray is initialised with profile " + demandName);
			}

			householdMiscDemandArray = ArrayUtils.convertStringArrayToDoubleArray(baseDemandReader.getColumn(demandName));

			HouseholdProsumer hhProsAgent = prosumerFactory.createHouseholdProsumer(householdMiscDemandArray, false, occupancyNb);

			cascadeMainContext.add(hhProsAgent);			
		} 
	}

	private void initializeHHProsumersWetAppliancesPar4All() {

		IndexedIterable<HouseholdProsumer> householdProsumers = cascadeMainContext.getObjects(HouseholdProsumer.class);

		/*----------------
		 * Richard's occupancy test code
		 * 
		 * Note that this in effect is assuming that occupancy is independent of 
		 * any of the other assigned variables.  This may not, of course, be true.
		 */

		//assign wet appliance ownership.  Based on statistical representation of the BERR 2006 ownership stats
		// with a bias based on occupancy which seems reasonable.
		// TODO: break this out into a separate method.  Store constants somewhere?  Should they read from file?
		for (HouseholdProsumer thisAgent : householdProsumers)
		{
			int occupancy = thisAgent.getNumOccupants();
			double randomVar = RandomHelper.nextDouble();
			//System.out.println("randomVar: "+randomVar);
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

			//populate the initial heating profile from the above baseline demand for hot water
			//thisAgent.wetApplianceProfile = InitialProfileGenUtils.melodyStokesWetApplianceGen(Consts.DAYS_PER_YEAR, thisAgent.hasWashingMachine, thisAgent.hasWasherDryer, thisAgent.hasDishWasher, thisAgent.hasTumbleDryer);
			
			thisAgent.setWetAppliancesProfiles(InitialProfileGenUtils.melodyStokesWetApplianceGen(this.cascadeMainContext,Consts.DAYS_PER_YEAR, thisAgent.hasWashingMachine, thisAgent.hasWasherDryer, thisAgent.hasDishWasher, thisAgent.hasTumbleDryer));

		}

		if(cascadeMainContext.verbose)
		{
			System.out.println("Percentages:");
			System.out.println("households with occupancy 1 : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",1)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 2 : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",2)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 3 : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",3)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 4 : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",4)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 5 : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",5)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 6 : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",6)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 7 : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",7)).query()) / householdProsumers.size());
			System.out.println("households with occupancy 8 : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "numOccupants",8)).query()) / householdProsumers.size());
			System.out.println("Washing Mach : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasWashingMachine",true)).query()) / householdProsumers.size());
			System.out.println("Washer Dryer : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasWasherDryer",true)).query()) / householdProsumers.size());
			System.out.println("Tumble Dryer: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasTumbleDryer",true)).query()) / householdProsumers.size());
			System.out.println("Dish Washer : " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasDishWasher",true)).query()) / householdProsumers.size());
		}
	}
	
	private void setHHProsumersElecWaterHeatBasedOnFraction() {

		Iterable waterHeatedProsumersIter = cascadeMainContext.getRandomObjects(HouseholdProsumer.class, (long) (numProsumers * (Double) params.getValue("elecWaterFraction")));
		ArrayList prosumersWithElecWaterHeatList = IterableUtils.Iterable2ArrayList(waterHeatedProsumersIter);

		//System.out.println("ArrayList.size: WaterHeat "+ prosumersWithElecWaterHeatList.size());
		AgentUtils.assignParameterSingleValue("hasElectricalWaterHeat", true, prosumersWithElecWaterHeatList.iterator());

		Iterator iter = prosumersWithElecWaterHeatList.iterator();

		while (iter.hasNext()) {
			((HouseholdProsumer) iter.next()).initializeElectWaterHeatPar();
		}
	}

	private void setHHProsumersElecSpaceHeatBasedOnFraction() {
		Iterable spaceHeatedProsumersIter = cascadeMainContext.getRandomObjects(HouseholdProsumer.class, (long) (numProsumers * (Double) params.getValue("elecSpaceFraction")));

		ArrayList prosumersWithElecSpaceHeatList = IterableUtils.Iterable2ArrayList(spaceHeatedProsumersIter);

		//System.out.println("ArrayList.size: Space Heat "+ prosumersWithElecSpaceHeatList.size());

		AgentUtils.assignParameterSingleValue("hasElectricalSpaceHeat", true, prosumersWithElecSpaceHeatList.iterator());

		Iterator iter = prosumersWithElecSpaceHeatList.iterator();

		while (iter.hasNext()) {
			((HouseholdProsumer) iter.next()).initializeElecSpaceHeatPar();
		}
	}


	/**
	 * This method uses rule set as described in Boait et al draft paper to assign
	 * cold appliance ownership on a stochastic, but statistically representative, basis.
	 */
	private void initializeHHProsumersColdAppliancesPar4All()
	{
		IndexedIterable<HouseholdProsumer> householdProsumers = cascadeMainContext.getObjects(HouseholdProsumer.class);

		for (HouseholdProsumer pAgent : householdProsumers)
		{
			// Set up cold appliance ownership
			if(RandomHelper.nextDouble() < 0.651)
			{
				pAgent.hasFridgeFreezer = true;
				if (RandomHelper.nextDouble() < 0.15)
				{
					pAgent.hasRefrigerator = true;
				}
			}
			else
			{
				if (RandomHelper.nextDouble() < 0.95)
				{
					pAgent.hasRefrigerator = true;
				}
				if (RandomHelper.nextDouble() < 0.835)
				{
					pAgent.hasUprightFreezer = true;
				}
			}

			if (RandomHelper.nextDouble() < 0.163)
			{
				pAgent.hasChestFreezer = true;
			}
			
			System.out.println("Fridge; FridgeFreezer; Freezer: "+  pAgent.hasRefrigerator +" "+pAgent.hasFridgeFreezer + " "+ (pAgent.hasUprightFreezer || pAgent.hasChestFreezer)); 

			//pAgent.coldApplianceProfile = InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.DAYS_PER_YEAR, pAgent.hasRefrigerator, pAgent.hasFridgeFreezer, (pAgent.hasUprightFreezer && pAgent.hasChestFreezer));
			pAgent.setColdAppliancesProfiles(InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.DAYS_PER_YEAR, pAgent.hasRefrigerator, pAgent.hasFridgeFreezer, (pAgent.hasUprightFreezer || pAgent.hasChestFreezer)));
		}
		
		if(cascadeMainContext.verbose)
		{
			System.out.println("HHs with Fridge: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasRefrigerator",true)).query()));
			System.out.println("HHs with FridgeFreezer: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasFridgeFreezer",true)).query()));
			System.out.println("HHs with UprightFreezer: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasUprightFreezer",true)).query()));
			System.out.println("HHs with ChestFreezer: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasChestFreezer",true)).query()));

			System.out.println("HHs with Fridge %: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasRefrigerator",true)).query()) / householdProsumers.size());
			System.out.println("HHs with FridgeFreezer %: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasFridgeFreezer",true)).query()) / householdProsumers.size());
			System.out.println("HHs with UprightFreezer %: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasUprightFreezer",true)).query()) / householdProsumers.size());
			System.out.println("HHs with ChestFreezer %: " + (double) IterableUtils.count((new PropertyEquals(cascadeMainContext, "hasChestFreezer",true)).query()) / householdProsumers.size());
		}
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
			System.out.println("CascadeContextBuilder: There are " + IterableUtils.count(cat1Agents) + " category 1 agents");
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
	private void buildOtherNetworks(AggregatorAgent firstAggregator) {
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
	}


	/**
	 * This method initialize the probability distributions
	 * used in this model. 
	 */
	private void initializeProbabilityDistributions() {

		double[] drawOffDist = ArrayUtils.multiply(Consts.EST_DRAWOFF, ArrayUtils.sum(Consts.EST_DRAWOFF));
		//System.out.println("  ArrayUtils.sum(drawOffDist)"+ ArrayUtils.sum(drawOffDist));
		cascadeMainContext.drawOffGenerator = RandomHelper.createEmpiricalWalker(drawOffDist, Empirical.NO_INTERPOLATION);
		//System.out.println("  ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY)"+ ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY));

		cascadeMainContext.occupancyGenerator = RandomHelper.createEmpiricalWalker(Consts.OCCUPANCY_PROBABILITY_ARRAY, Empirical.NO_INTERPOLATION);
		cascadeMainContext.waterUsageGenerator = RandomHelper.createNormal(0, 1);

		cascadeMainContext.buildingLossRateGenerator = RandomHelper.createNormal(275,75);
		cascadeMainContext.thermalMassGenerator = RandomHelper.createNormal(12.5, 2.5);
		
		cascadeMainContext.coldAndWetApplTimeslotDelayRandDist = RandomHelper.createUniform(); 
		
		cascadeMainContext.wetApplProbDistGenerator = RandomHelper.createEmpiricalWalker(Consts.WET_APPLIANCE_PDF, Empirical.NO_INTERPOLATION);
		
		//ChartUtils.testProbabilityDistAndShowHistogram(cascadeMainContext.wetApplProbDistGenerator, 10000, 48);  //test to make sure the prob dist generate desired outcomes

		//cascadeMainContext.hhProsumerElasticityTest = RandomHelper.createBinomial(1, 0.005);
		
	}

	/**
	 * This method builds the schedules directly/manually.
	 * The motivation behind its built was related to the reported bug 
	 * in the random seed, and hence order of scheduling and reproducibility.  
	 * It is not currently in used. 
	 */
	private void buildSchedulesDirectly(){

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		IndexedIterable <ProsumerAgent> prosumerIndIter = cascadeMainContext.getObjects(ProsumerAgent.class);
		ScheduleParameters prosumerScheduleParams = ScheduleParameters.createRepeating(0, 1);
		schedule.scheduleIterable(prosumerScheduleParams, prosumerIndIter, "step", true);

		IndexedIterable <AggregatorAgent> aggregatorIndIter= cascadeMainContext.getObjects(AggregatorAgent.class);
		ScheduleParameters aggregatorScheduleParams = ScheduleParameters.createRepeating(0, 1, ScheduleParameters.LAST_PRIORITY);
		schedule.scheduleIterable(aggregatorScheduleParams, aggregatorIndIter, "step", true);

	}

	private void cranfieldMarketModelIntegrationTest () {

		double minD = RandomHelper.nextDoubleFromTo(40000,80000);
		double maxD = minD + RandomHelper.nextDoubleFromTo(40000,80000);
		double maxDPrice = 2;
		double minDPrice = 30;
		double minGen = RandomHelper.nextDoubleFromTo(400,800);
		double maxGen = minGen + RandomHelper.nextDoubleFromTo(400,800);
		double minGenPrice = RandomHelper.nextDoubleFromTo(0.1, 10);
		double maxGenPrice = minGenPrice + RandomHelper.nextDoubleFromTo(10, 20);
		
			
		
		for(int i = 0; i < 0; i++)
		{
			TestConsumer ta = new TestConsumer(
		              minDPrice,maxDPrice,
		              minD, maxD);
			cascadeMainContext.add(ta);

			ScheduleParameters params = ScheduleParameters.createRepeating(1,1,2);
			RunEnvironment.getInstance().getCurrentSchedule().schedule(params, ta, "updateSupplyDemand");
			
			
		}
		
		for(int i = 0; i < 2; i++)
		{	
			

				
			testAggregator ta = new testAggregator(minGenPrice,maxGenPrice,
		              minGen, maxGen,
		              1000,1001,
		              0, 1,0);
			cascadeMainContext.add(ta);
			ScheduleParameters params = ScheduleParameters.createRepeating(1, 1,2);
			RunEnvironment.getInstance().getCurrentSchedule().schedule(params, ta, "updateSupplyDemand");
        }
		
		cascadeMainContext.add(Market.defaultM );
		ScheduleParameters params = ScheduleParameters.createRepeating(1, 1,ScheduleParameters.LAST_PRIORITY);
		RunEnvironment.getInstance().getCurrentSchedule().schedule(params, Market.defaultM, "runMarket");
		
		
		
	/*    IndexedIterable <testAggregator> testAggIter = cascadeMainContext.getObjects(testAggregator.class);

		for (testAggregator it : testAggIter ) {
			System.out.println(it);
		}*/
		
	
		
	}

	private void populateContext_test() {
		
		createTestHHProsumersAndAddThemToContext();
		
		AggregatorFactory aggregatorFactory = FactoryFinder.createAggregatorFactory(this.cascadeMainContext);
		RECO firstRecoAggregator = aggregatorFactory.createRECO(cascadeMainContext.systemPriceSignalDataArray);
		cascadeMainContext.add(firstRecoAggregator);
		
		buildSocialNetwork(); 

		buildOtherNetworks(firstRecoAggregator);
	}


	private void populateContext() {

		//createHouseholdProsumersAndAddThemToContext(1); //pass in parameter nb of occupants, or random
		createHouseholdProsumersAndAddThemToContext(Consts.RANDOM);

		//@TODO: these 4 methods below will eventually be included in the first method (createHousholdPro...)
		if (Consts.HHPRO_HAS_WET_APPL)
			initializeHHProsumersWetAppliancesPar4All();

		if (Consts.HHPRO_HAS_COLD_APPL)
			initializeHHProsumersColdAppliancesPar4All();

		if (Consts.HHPRO_HAS_ELEC_WATER_HEAT)
			setHHProsumersElecWaterHeatBasedOnFraction();

		if (Consts.HHPRO_HAS_ELEC_SPACE_HEAT)
			setHHProsumersElecSpaceHeatBasedOnFraction();

		//Add aggregator(s), currently one;  (TODO should become a separate method later)
		AggregatorFactory aggregatorFactory = FactoryFinder.createAggregatorFactory(this.cascadeMainContext);
		RECO firstRecoAggregator = aggregatorFactory.createRECO(cascadeMainContext.systemPriceSignalDataArray);
		cascadeMainContext.add(firstRecoAggregator);
		
		buildSocialNetwork(); 

		buildOtherNetworks(firstRecoAggregator);
	}


	/*
	 * Builds the <tt> Cascade Context </tt> (by calling other private sub-methods)
	 * In part, it instantiates the Cascade context by passing in the context that is passed to 
	 * the builder
	 * @see uk.ac.dmu.iesd.cascade.context.CascadeContext
	 * @see repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context.Context)
	 * @return built context
	 */
	public CascadeContext build(Context context) {

		//System.out.println("CascadeContextBuilder");

		cascadeMainContext = new CascadeContext(context); //build CascadeContext by passing the context

		readParamsAndInitializeArrays();
		initializeProbabilityDistributions();

		//populateContext_test();

		populateContext();
		
		PopulationUtils.testAndPrintHouseholdApplianceProportions(cascadeMainContext);

		cranfieldMarketModelIntegrationTest();

		if (cascadeMainContext.verbose)	
			System.out.println("CascadeContextBuilder: Cascade Main Context created: "+cascadeMainContext.toString());

		return cascadeMainContext;
	}




}

