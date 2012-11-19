package uk.ac.dmu.iesd.cascade.context;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.WeakHashMap;

import cern.jet.random.Empirical;
import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkFactory;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.context.space.graph.NetworkGenerator;
import repast.simphony.context.space.graph.WattsBetaSmallWorldGenerator;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.*;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.collections.IndexedIterable;
import uk.ac.dmu.iesd.cascade.market.IPxTrader;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.market.astem.operators.PowerExchange;
import uk.ac.dmu.iesd.cascade.market.astem.operators.SettlementCompany;
import uk.ac.dmu.iesd.cascade.market.astem.operators.SystemOperator;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorFactory;
import uk.ac.dmu.iesd.cascade.agents.aggregators.BMPxTraderAggregator;
import uk.ac.dmu.iesd.cascade.agents.aggregators.SingleNonDomesticAggregator;
import uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCo;
import uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCoAdvancedModel;
import uk.ac.dmu.iesd.cascade.agents.aggregators.WindFarmAggregator;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.WindGeneratorProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerFactory;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.base.FactoryFinder;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.test.HHProsumer;
import uk.ac.dmu.iesd.cascade.util.*;


/**
 * CASCADE Project Version [ Model Built version] (Version# for the entire project/ as whole)
 * @version $Revision: 2.00 $ $Date: 2011/10/05 
 * 
 * Major changes for this submission include: 
 * • All the elements ('variable' declarations, including data structures consisting of values 
 * [constant] or variables) of the type 'float' (32 bits) have been changed to 'double' (64 bits) (Babak Mahdavi)
 * 
 * CASCADE Project Version [ Model Built version] (Version# for the entire project/ as whole)
 *  @version $Revision: 3.00 $ $Date: 2012/05/30
 *  • Restructure of the entire project packages after with the integration with the ASTEM market model (Babak Mahdavi)
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
 * @version $Revision: 1.5 $ $Date: 2012/05/14 $
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
 * 1.5  ASTEM market model has been integrated (Babak) / May 2012
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
	private File dataDirectory;
	private int percentageOfHHProsWithGas;
	private WeakHashMap <Integer, double[]> map_nbOfOccToOtherDemand;
	
	private SystemOperator sysOp;
	
	private MarketMessageBoard messageBoard;
	

	//int ticksPerDay;
	//int numOfOtherDemandColumns;
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
		String householdOtherDemandFilename = (String)params.getValue("householdOtherDemandFile");
		//String elecLayoutFilename = (String)params.getValue("electricalNetworkLayoutFile");
		numProsumers = (Integer) params.getValue("defaultProsumersPerFeeder");
		percentageOfHHProsWithGas = (Integer) params.getValue("hhWithGasPercentage");
		cascadeMainContext.setTotalNbOfProsumers(numProsumers);
		int ticksPerDay = (Integer) params.getValue("ticksPerDay");
		cascadeMainContext.setNbOfTickPerDay(ticksPerDay);
		cascadeMainContext.verbose = (Boolean) params.getValue("verboseOutput");
		cascadeMainContext.chartSnapshotOn = (Boolean) params.getValue("chartSnapshot");
		cascadeMainContext.setChartSnapshotInterval((Integer) params.getValue("chartSnapshotInterval"));
		cascadeMainContext.signalMode = (Integer)params.getValue("signalMode");
		
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
		dataDirectory = new File(dataFileFolderPath);
		File weatherFile = new File(dataDirectory, weatherFileName);
		CSVReader weatherReader = null;
		File systemDemandFile = new File(dataDirectory, systemDemandFileName);
		CSVReader systemBasePriceReader = null;
		
		File householdOtherDemandFile = new File(dataDirectory, householdOtherDemandFilename);

		int lengthOfProfileArrays = ticksPerDay*Consts.NB_OF_DAYS_LOADED_DEMAND;
		
		try {
			weatherReader = new CSVReader(weatherFile);
			weatherReader.parseByColumn();
			
			double [] insolationArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("insolation"));
			double [] windSpeedArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("windSpeed"));
			double [] airTemperatureArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("airTemp"));
			double [] airDensityArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("airDensity"));
						
			cascadeMainContext.insolationArray =Arrays.copyOf(insolationArray_all, insolationArray_all.length);
			cascadeMainContext.windSpeedArray =  Arrays.copyOf(windSpeedArray_all, windSpeedArray_all.length);
			cascadeMainContext.airTemperatureArray = Arrays.copyOf(airTemperatureArray_all, airTemperatureArray_all.length);
			cascadeMainContext.airDensityArray = Arrays.copyOf(airDensityArray_all, airDensityArray_all.length);
			
			cascadeMainContext.weatherDataLength = windSpeedArray_all.length;//lengthOfProfileArrays;
			

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
			cascadeMainContext.systemPriceSignalDataArray = Arrays.copyOf(systemBasePriceSignal, lengthOfProfileArrays);
			cascadeMainContext.systemPriceSignalDataLength = systemBasePriceSignal.length;
			
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdOtherDemandFilename);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}		
		if (cascadeMainContext.systemPriceSignalDataLength % cascadeMainContext.getNbOfTickPerDay()!= 0)
		{
			System.err.println("Base System Demand array not a whole number of days. This may cause unexpected behaviour");
		}

		try {
			CSVReader otherDemandReader = new CSVReader(householdOtherDemandFile);
			
			otherDemandReader.parseByColumn();
			int numOfOtherDemandColumns = otherDemandReader.columnsStarting("occ");

			if (numOfOtherDemandColumns == 0)
			{
				System.err.println("The household demand data files appears to have no demand data columns");
				System.err.println("Demand data columns should be headed 'demand' followed by an integer e.g. 'demand0', 'demand1'...");
				System.err.println("Proceeding with no demand data would cause failure, so the program will now terminate");
				System.err.println("Please check file " + householdOtherDemandFile.getAbsolutePath());
				System.exit(1);
			}
			
			map_nbOfOccToOtherDemand = new WeakHashMap <Integer, double[]> (); 
			
			
			for (int i=1; i<=numOfOtherDemandColumns; i++) {
				
				  this.map_nbOfOccToOtherDemand.put(i,ArrayUtils.convertStringArrayToDoubleArray(otherDemandReader.getColumn("occ"+i)));
			}	

		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdOtherDemandFilename);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}

		this.monthlyMainsWaterTemp = Arrays.copyOf(Consts.MONTHLY_MAINS_WATER_TEMP, Consts.MONTHLY_MAINS_WATER_TEMP.length);
	}

 /*
	private void createTestHHProsumersAndAddThemToContext() {

		ProsumerFactory prosumerFactory = FactoryFinder.createProsumerFactory(this.cascadeMainContext);

		double[] householdBaseDemandArray = null;
		for (int i = 0; i < numProsumers; i++) {

			String demandName = "demand" + RandomHelper.nextIntFromTo(0, numDemandColumns - 1);

			if (cascadeMainContext.verbose)
			{
				System.out.println("CascadeContextBuilder: householdBaseDemandArray is initialised with profile " + demandName);
			}
			//householdBaseDemandArray = ArrayUtils.convertStringArrayToDoubleArray(otherDemandReader.getColumn(demandName));

			HHProsumer hhProsAgent = prosumerFactory.createHHProsumer(householdBaseDemandArray, false);

			cascadeMainContext.add(hhProsAgent);			
		} 
	}  */
	
	private void createWindFarmsAndAddThemToContext(){
		ProsumerFactory prosumerFactory = FactoryFinder.createProsumerFactory(this.cascadeMainContext);
		
		for (int numWindFarms = 0; numWindFarms < 11; numWindFarms++){
			
			WindGeneratorProsumer genProsAgent = prosumerFactory.createWindGenerator(2, Consts.GENERATOR_TYPE.WIND, 20);
			
			genProsAgent.offset = 0;//96/(numWindFarms+1);
			double [] hubHeights = new double[] {75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0,75.0};
			double [] cp = new double[] {0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3};
			double [] minWindSpeed = new double[] {2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5};
			double [] maxWindSpeed = new double[] {25,25,25,25,25,25,25,25,25,25,25,25,25,25,25,25,25,25,25,25};
			double [] capacity = new double[] {2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6,2.0E6};
			double [] bladeLength = new double[] {40,40,40,40,40,40,40,40,40,40,40,40,40,40,40,40,40,40,40,40};
			genProsAgent.setUpWindFarm(50.0, 0.0, 20, 1, 1, hubHeights, cp, minWindSpeed, maxWindSpeed, capacity, bladeLength);
		
			if (!cascadeMainContext.add(genProsAgent))	{
				System.err.println("Failed to add wind farm to context!!");
			}
		
		}
	}

	private void createHouseholdProsumersAndAddThemToContext(int occupancyModeOrNb) {

		ProsumerFactory prosumerFactory = FactoryFinder.createProsumerFactory(this.cascadeMainContext);
		
		System.out.println("-----------------------------");
		System.out.println("% Of HHPros With Gas: "+ percentageOfHHProsWithGas+"%");
		
		int nbOfHHProsWithGas = (numProsumers * percentageOfHHProsWithGas)/100;
		int nbOfHHProsWithElectricty = numProsumers - nbOfHHProsWithGas;
		
		System.out.println("# of HHPros With Gas: "+ nbOfHHProsWithGas);
		System.out.println("# of HHPros With Electricty: "+ nbOfHHProsWithElectricty);

		double[] hhOtherDemandArray = null; //Other elastic demand profiles array consists of electricity demand for cooking, lightening, and brown(entertainment, computer and small appliances)
		
		for (int i = 0; i < nbOfHHProsWithGas; i++) {

			HouseholdProsumer hhProsAgent = prosumerFactory.createHouseholdProsumer(map_nbOfOccToOtherDemand, occupancyModeOrNb, true, true);

			if (!cascadeMainContext.add(hhProsAgent))	{
				System.err.println("Failed to add agent to context!!");
			}
		} 
		
		for (int i = 0; i < nbOfHHProsWithElectricty; i++) {

			HouseholdProsumer hhProsAgent = prosumerFactory.createHouseholdProsumer(map_nbOfOccToOtherDemand, occupancyModeOrNb, true, false);

			if (!cascadeMainContext.add(hhProsAgent))	{
				System.err.println("Failed to add agent to context!!");
			}
		} 
			
		System.out.println("Total # of HHPros added to context: " + cascadeMainContext.getObjects(HouseholdProsumer.class).size()); 
		System.out.println("-----------------------------");

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
			//if (Consts.DEBUG) System.out.println("randomVar: "+randomVar);
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
	
			thisAgent.setWetAppliancesProfiles(InitialProfileGenUtils.melodyStokesWetApplianceGen(this.cascadeMainContext,Consts.NB_OF_DAYS_LOADED_DEMAND, thisAgent.hasWashingMachine, thisAgent.hasWasherDryer, thisAgent.hasDishWasher, thisAgent.hasTumbleDryer));
			/*JRS TEST - REMOVE REMOVE REMOVE */
			setWetAppsPerPBMatlabPrototype(thisAgent);

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
	
	public void setWetAppsPerPBMatlabPrototype(HouseholdProsumer thisAgent)
	{

	
	WeakHashMap<String,double[]> PB_wash_approx = new WeakHashMap<String, double[]>();
	double[] basicWashProfile = new double[48];
	if (RandomHelper.nextDouble() < 0.57)
	{
		int startSlot = (int) (RandomHelper.nextDouble()*47);
		basicWashProfile[startSlot] = 1;
		basicWashProfile[startSlot+1] = 1;
	}
	
	thisAgent.hasDishWasher = false;
	thisAgent.hasTumbleDryer = false;
	thisAgent.hasWasherDryer = false;
	PB_wash_approx.put(Consts.WET_APP_DRYER,new double[48]);
	PB_wash_approx.put(Consts.WET_APP_DISHWASHER,new double[48]);
	PB_wash_approx.put(Consts.WET_APP_DISHWASHER_ORIGINAL,new double[48]);
	PB_wash_approx.put(Consts.WET_APP_DRYER_ORIGINAL,new double[48]);
	PB_wash_approx.put(Consts.WET_APP_WASHER,basicWashProfile);
	PB_wash_approx.put(Consts.WET_APP_WASHER_ORIGINAL,basicWashProfile.clone());

	
	thisAgent.setWetAppliancesProfiles(PB_wash_approx);	
	}

  /*
	private void initializeHHProsumersElecWaterHeat() {

		//Iterable waterHeatedProsumersIter = cascadeMainContext.getRandomObjects(HouseholdProsumer.class, (long) (numProsumers * (Double) params.getValue("elecWaterFraction")));
		Iterable waterHeatedProsumersIter = cascadeMainContext.getRandomObjects(HouseholdProsumer.class, (long) (numProsumers));

		ArrayList prosumersWithElecWaterHeatList = IterableUtils.Iterable2ArrayList(waterHeatedProsumersIter);

		//if (Consts.DEBUG) System.out.println("ArrayList.size: WaterHeat "+ prosumersWithElecWaterHeatList.size());
		AgentUtils.assignParameterSingleValue("hasElectricalWaterHeat", true, prosumersWithElecWaterHeatList.iterator());

		Iterator iter = prosumersWithElecWaterHeatList.iterator();

		while (iter.hasNext()) {
			((HouseholdProsumer) iter.next()).initializeElectWaterHeatPar();
		}
	} */

	private void initializeWithoutGasHHProsumersElecSpaceHeat() {
		
		IndexedIterable <HouseholdProsumer> spaceHeatedProsumersIter = cascadeMainContext.getObjects(HouseholdProsumer.class);
		
		for (HouseholdProsumer hhPros: spaceHeatedProsumersIter) {
			if (!hhPros.isHasGas()) {
				hhPros.setHasElectricalSpaceHeat(true);
				hhPros.initializeElecSpaceHeatPar();

			}
		}
	}
	
	private void initializeWithoutGasHHProsumersElecWaterHeat() {

		IndexedIterable <HouseholdProsumer> spaceHeatedProsumersIter = cascadeMainContext.getObjects(HouseholdProsumer.class);
		
		for (HouseholdProsumer hhPros: spaceHeatedProsumersIter) {
			if (!hhPros.isHasGas()) {
				hhPros.setHasElectricalWaterHeat(true);
				hhPros.initializeElectWaterHeatPar();

			}
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
			
			//if (Consts.DEBUG) System.out.println("Fridge; FridgeFreezer; Freezer: "+  pAgent.hasRefrigerator +" "+pAgent.hasFridgeFreezer + " "+ (pAgent.hasUprightFreezer || pAgent.hasChestFreezer)); 

			//pAgent.coldApplianceProfile = InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.DAYS_PER_YEAR, pAgent.hasRefrigerator, pAgent.hasFridgeFreezer, (pAgent.hasUprightFreezer && pAgent.hasChestFreezer));
			pAgent.setColdAppliancesProfiles(InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.NB_OF_DAYS_LOADED_DEMAND, pAgent.hasRefrigerator, pAgent.hasFridgeFreezer, (pAgent.hasUprightFreezer || pAgent.hasChestFreezer)));
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
		System.out.println("Random seed is" + RandomHelper.getSeed());
		double[] drawOffDist = ArrayUtils.multiply(Consts.EST_DRAWOFF, ArrayUtils.sum(Consts.EST_DRAWOFF));
		//if (Consts.DEBUG) System.out.println("  ArrayUtils.sum(drawOffDist)"+ ArrayUtils.sum(drawOffDist));
		cascadeMainContext.drawOffGenerator = RandomHelper.createEmpiricalWalker(drawOffDist, Empirical.NO_INTERPOLATION);
		//if (Consts.DEBUG) System.out.println("  ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY)"+ ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY));

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
	
	/*
	private void buildNetworkOfRegisteredTraders() {
		//boolean directed = true;
		NetworkFactory networkFactory = NetworkFactoryFinder.createNetworkFactory(null);	
		Network bmuNet = networkFactory.createNetwork("RegisteredBMUsNetwork", cascadeMainContext, true);
		for (IPxTrader bmuAgent:(Iterable<IPxTrader>) (cascadeMainContext.getObjects(IPxTrader.class)))	{
			bmuNet.addEdge(sysOp, bmuAgent);
		}
		cascadeMainContext.setNetworkOfRegisteredPxTraders(bmuNet);
	}  */
	
	private void buildMarket() {
		
		SettlementCompany settlementCo = new SettlementCompany(cascadeMainContext);
		cascadeMainContext.add(settlementCo);
		
		messageBoard = new MarketMessageBoard();

		sysOp = new SystemOperator(cascadeMainContext, settlementCo, messageBoard);
		cascadeMainContext.add(sysOp);
		
		PowerExchange pEx = new PowerExchange(cascadeMainContext, messageBoard);
		cascadeMainContext.add(pEx);
			
	}

	/*private void populateContext_test() {
		createTestHHProsumersAndAddThemToContext();
		
		AggregatorFactory aggregatorFactory = FactoryFinder.createAggregatorFactory(this.cascadeMainContext);
		RECO firstRecoAggregator = aggregatorFactory.createRECO(cascadeMainContext.systemPriceSignalDataArray);
		cascadeMainContext.add(firstRecoAggregator);
		
		buildSocialNetwork(); 

		buildOtherNetworks(firstRecoAggregator);
	} */


	private void populateContext() {
		/* 
		 * TODO: NEED TO CREATE A MORE GENERIC SETUP STRUCTURE FOR ALL NETWORKS, AGGREGATORS, PROSUMERS, ETC.. 
		 */

		//createHouseholdProsumersAndAddThemToContext(2); //pass in parameter nb of occupants, or random
		createHouseholdProsumersAndAddThemToContext(Consts.RANDOM);
		
		//TODO: Add method that creates wind farm generators and adds them to the context..

		//@TODO: these 4 methods below will eventually be included in the first method (createHousholdPro...)
		if (Consts.HHPRO_HAS_WET_APPL)
			initializeHHProsumersWetAppliancesPar4All();

		if (Consts.HHPRO_HAS_COLD_APPL)
			initializeHHProsumersColdAppliancesPar4All();

		if (Consts.HHPRO_HAS_ELEC_WATER_HEAT)
			initializeWithoutGasHHProsumersElecWaterHeat();

		if (Consts.HHPRO_HAS_ELEC_SPACE_HEAT)
			initializeWithoutGasHHProsumersElecSpaceHeat();

		//Add aggregator(s), currently one;  (TODO should become a separate method later)
		AggregatorFactory aggregatorFactory = FactoryFinder.createAggregatorTraderFactory(this.cascadeMainContext, this.messageBoard);
		
		SupplierCoAdvancedModel firstRecoAggregator = aggregatorFactory.createSupplierCoAdvanced(cascadeMainContext.systemPriceSignalDataArray);
		cascadeMainContext.add(firstRecoAggregator);
		
		/**
		 * (02/07/12) DF
		 * 
		 * Add a simple single non domestic aggregator into <code>cascadeMainContext</code>
		 */
//		SingleNonDomesticAggregator singleNonDomestic = aggregatorFactory.createSingleNonDomesticAggregator(cascadeMainContext.systemPriceSignalDataArray);
//		cascadeMainContext.add(singleNonDomestic);
		
//		buildSocialNetwork(); 

		buildOtherNetworks(firstRecoAggregator);
		
		//Add wind farms to the context
		//TODO: Add functionality to allow greater flexibility in the creation of wind farms
		createWindFarmsAndAddThemToContext();
		
		// Add Wind Farm Aggregator to context here..
		WindFarmAggregator wFA = aggregatorFactory.createWindFarmAggregator();
		cascadeMainContext.add(wFA);
		
		for (ProsumerAgent prAgent:(Iterable<ProsumerAgent>) (cascadeMainContext.getObjects(ProsumerAgent.class)) )
			{
				if (prAgent instanceof WindGeneratorProsumer) {
					cascadeMainContext.getEconomicNetwork().addEdge(wFA, prAgent);
				}
			}

	}
	
	
	private WeakHashMap readGenericAggBaseProfileFiles() {

		//String currentDirectory = System.getProperty("user.dir"); //this suppose to be the Eclipse project working space
		//String pathToDataFiles = currentDirectory+ASTEMConsts.DATA_FILES_FOLDER_NAME;
		//File parentDataFilesDirectory = new File(pathToDataFiles);		
		//File dmu_BaseProfiles_File = new File(parentDataFilesDirectory, ASTEMConsts.BMU_BASE_PROFILES_FILENAME);
		
		File dmu_BaseProfiles_File = new File(dataDirectory, ASTEMConsts.BMU_BASE_PROFILES_FILENAME);
		
		WeakHashMap<String, double[]> mapOfTypeName2BaseProfileArray = new WeakHashMap<String, double[]> ();
		
		try {
			CSVReader baseProfileCSVReader = new CSVReader(dmu_BaseProfiles_File);
			System.out.println("baseProfileCSVReader created");
			baseProfileCSVReader.parseByColumn();
			
			mapOfTypeName2BaseProfileArray.put("DEM_LARGE", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("DEM_LARGE")));
			mapOfTypeName2BaseProfileArray.put("DEM_SMALL", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("DEM_SMALL")));
			mapOfTypeName2BaseProfileArray.put("GEN_COAL", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("GEN_COAL")));
			mapOfTypeName2BaseProfileArray.put("GEN_CCGT", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("GEN_CCGT")));
			mapOfTypeName2BaseProfileArray.put("GEN_WIND", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("GEN_WIND")));
			
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + dmu_BaseProfiles_File.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		
		return mapOfTypeName2BaseProfileArray;
	}
	
	/**
	 * This methods create a number of generic aggregators needed to  
	 * @param mapOfDMUtypeName2BaselineProfileArray
	 */
	private void createAndAddGenericAggregators(WeakHashMap<String, double[]> mapOfDMUtypeName2BaselineProfileArray){

		AggregatorFactory  bmuFactory = new AggregatorFactory(cascadeMainContext, this.messageBoard);

		double[] arr_baseline_DEM_LARGE =  mapOfDMUtypeName2BaselineProfileArray.get("DEM_LARGE");
		double[] arr_baseline_DEM_SMALL =  mapOfDMUtypeName2BaselineProfileArray.get("DEM_SMALL");
		double[] arr_baseline_GEN_COAL  =  mapOfDMUtypeName2BaselineProfileArray.get("GEN_COAL");
		double[] arr_baseline_GEN_CCGT  =  mapOfDMUtypeName2BaselineProfileArray.get("GEN_CCGT");
		double[] arr_baseline_GEN_WIND  =  mapOfDMUtypeName2BaselineProfileArray.get("GEN_WIND");

		int numOfDEM_LARGE = 1;
		int numOfDEM_SMALL = 1;
		int numOfGEN_COAL  = 1;
		int numOfGEN_CCGT  = 1;
		int numOfGEN_WIND  = 0; 


		for (int i=0; i< numOfDEM_LARGE; i++) {

			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.DEM_LARGE, arr_baseline_DEM_LARGE);
			cascadeMainContext.add(bmu);
		}

		for (int i=0; i< numOfDEM_SMALL; i++) {
			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.DEM_SMALL, arr_baseline_DEM_SMALL);
			cascadeMainContext.add(bmu);
		}

		for (int i=0; i< numOfGEN_COAL; i++) {
			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_COAL, arr_baseline_GEN_COAL);
			cascadeMainContext.add(bmu);
		}

		for (int i=0; i< numOfGEN_CCGT; i++) {
			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_CCGT, arr_baseline_GEN_CCGT);
			cascadeMainContext.add(bmu);
		}

		for (int i=0; i< numOfGEN_WIND; i++) {
			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_WIND, arr_baseline_GEN_WIND);
			cascadeMainContext.add(bmu);
		}

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

		WeakHashMap<String, double[]> map_dmuTypeNameToBaseProfiles;

		cascadeMainContext = new CascadeContext(context); //build CascadeContext by passing the context

		readParamsAndInitializeArrays();
		initializeProbabilityDistributions();
				
		buildMarket();
		
		populateContext();
		
		map_dmuTypeNameToBaseProfiles = readGenericAggBaseProfileFiles();
		
		createAndAddGenericAggregators(map_dmuTypeNameToBaseProfiles);
		

		if (cascadeMainContext.verbose)	
			System.out.println("CascadeContextBuilder: Cascade Main Context created: "+cascadeMainContext.toString());

		return cascadeMainContext;
	}

}

