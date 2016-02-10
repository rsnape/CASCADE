package uk.ac.dmu.iesd.cascade.context;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.WeakHashMap;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkFactory;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.PropertyEquals;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.util.collections.IndexedIterable;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorFactory;
import uk.ac.dmu.iesd.cascade.agents.aggregators.BMPxTraderAggregator;
import uk.ac.dmu.iesd.cascade.agents.aggregators.EquationBasedPriceAggregator;
import uk.ac.dmu.iesd.cascade.agents.aggregators.PassThroughAggregatorWithLag;
import uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCoAdvancedModel;
import uk.ac.dmu.iesd.cascade.agents.aggregators.WindFarmAggregator;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ConstantPlusElasticityProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerFactory;
import uk.ac.dmu.iesd.cascade.agents.prosumers.WindGeneratorProsumer;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.base.FactoryFinder;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.market.astem.operators.PowerExchange;
import uk.ac.dmu.iesd.cascade.market.astem.operators.SettlementCompany;
import uk.ac.dmu.iesd.cascade.market.astem.operators.SystemOperator;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.InitialProfileGenUtils;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.EVProfileGenerator;
import cern.jet.random.Empirical;

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
 * <em>BasicTestContextBuilder</em> is the Repast specific starting point class 
 * (i.e. a <code>ContextBuilder</code>) for 
 * building a context (i.e.{@link CascadeContext}) for a basic test within the
 * <em>Cascade</em> framework.
 * Building a context consists of filling it with agents and other actors/components/etc. and
 * constructing displays/views for the model and so forth.
 * 
 * This particular context creator creates an aggregator
 * which simply relays the price from the market to a number of elastic prosumers.
 * 
 * @author J. Richard Snape
 * @version $Revision: 1.0 $ $Date: 2012/07/31 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Basic creator developed as a cut-down version of CascadeContextBuilder 
 *       (see CascadeContextBuilder.java for revision history therein)
 */

/**
 * Builds and returns a context. Building a context consists of filling it with
 * agents, adding projects and so forth.
 * 
 * @param context
 * @return the built context.
 */
public class BasicWithMarketTestContextBuilder implements ContextBuilder<Object>
{

	private CascadeContext cascadeMainContext; // cascade main context
	private Parameters params; // parameters for the model run environment
	private int numProsumers; // number of Prosumers
	private int percentageOfHHProsWithGas;
	private File dataDirectory;
	private WeakHashMap<Integer, double[]> map_nbOfOccToOtherDemand;
	private SystemOperator sysOp;
	private MarketMessageBoard messageBoard;

	// int ticksPerDay;
	// int numOfOtherDemandColumns;
	// Normal buildingLossRateGenerator = RandomHelper.createNormal(275,75);
	// Normal thermalMassGenerator = RandomHelper.createNormal(12.5, 2.5);

	double[] monthlyMainsWaterTemp = new double[12];

	/*
	 * Read the model environment parameters and initialize arrays
	 */
	private void readParamsAndInitializeArrays()
	{
		// get the parameters from the current run environment
		this.params = RunEnvironment.getInstance().getParameters();
		String dataFileFolderPath = (String) this.params.getValue("dataFileFolder");
		String weatherFileName = (String) this.params.getValue("weatherFile");
		String systemDemandFileName = (String) this.params.getValue("systemBaseDemandFile");
		String householdOtherDemandFilename = (String) this.params.getValue("householdOtherDemandFile");
		// String elecLayoutFilename =
		// (String)params.getValue("electricalNetworkLayoutFile");
		this.numProsumers = (Integer) this.params.getValue("defaultProsumersPerFeeder");
		this.percentageOfHHProsWithGas = (Integer) this.params.getValue("hhWithGasPercentage");
		this.cascadeMainContext.setTotalNbOfProsumers(this.numProsumers);
		int ticksPerDay = (Integer) this.params.getValue("ticksPerDay");
		this.cascadeMainContext.setNbOfTickPerDay(ticksPerDay);
		CascadeContext.verbose = (Boolean) this.params.getValue("verboseOutput");
		CascadeContext.chartSnapshotOn = (Boolean) this.params.getValue("chartSnapshot");
		this.cascadeMainContext.setChartSnapshotInterval((Integer) this.params.getValue("chartSnapshotInterval"));
		this.cascadeMainContext.signalMode = (Integer) this.params.getValue("signalMode");

		this.cascadeMainContext.setRandomSeedValue((Integer) this.params.getValue("randomSeed"));

		// RunEnvironment.getInstance().
		Date startDate;
		try
		{
			startDate = (new SimpleDateFormat("dd/MM/yyyy")).parse((String) this.params.getValue("startDate"));
		}
		catch (ParseException e1)
		{
			// TODO Auto-generated catch block
			System.err.println("CascadeContextBuilder: The start date parameter is in a format which cannot be parsed by this program");
			System.err.println("CascadeContextBuilder: The data will be set to 01/01/2000 by default");
			startDate = new Date(2000, 1, 1);
			e1.printStackTrace();
		}
		this.cascadeMainContext.simulationCalendar = new GregorianCalendar();
		this.cascadeMainContext.simulationCalendar.setTime(startDate);

		/*
		 * Read in the necessary data files and store to the context
		 */
		this.dataDirectory = new File(dataFileFolderPath);
		File weatherFile = new File(this.dataDirectory, weatherFileName);
		CSVReader weatherReader = null;
		File systemDemandFile = new File(this.dataDirectory, systemDemandFileName);
		CSVReader systemBasePriceReader = null;

		File householdOtherDemandFile = new File(this.dataDirectory, householdOtherDemandFilename);

		int lengthOfProfileArrays = ticksPerDay * Consts.NB_OF_DAYS_LOADED_DEMAND;

		try
		{
			weatherReader = new CSVReader(weatherFile);
			weatherReader.parseByColumn();

			double[] insolationArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("insolation"));
			double[] windSpeedArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("windSpeed"));
			double[] airTemperatureArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("airTemp"));
			double[] airDensityArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("airDensity"));

			this.cascadeMainContext.insolationArray = Arrays.copyOf(insolationArray_all, lengthOfProfileArrays);
			this.cascadeMainContext.windSpeedArray = Arrays.copyOf(windSpeedArray_all, lengthOfProfileArrays);
			this.cascadeMainContext.airTemperatureArray = Arrays.copyOf(airTemperatureArray_all, lengthOfProfileArrays);
			this.cascadeMainContext.airDensityArray = Arrays.copyOf(airDensityArray_all, airDensityArray_all.length);

			this.cascadeMainContext.weatherDataLength = lengthOfProfileArrays;

		}
		catch (FileNotFoundException e)
		{
			System.err.println("Could not find file with name " + weatherFile.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (this.cascadeMainContext.weatherDataLength % this.cascadeMainContext.getNbOfTickPerDay() != 0)
		{
			System.err.println("Weather data array not a whole number of days. This may cause unexpected behaviour ");
		}

		try
		{
			systemBasePriceReader = new CSVReader(systemDemandFile);
			systemBasePriceReader.parseByColumn();

			double[] systemBasePriceSignal = ArrayUtils.convertStringArrayToDoubleArray(systemBasePriceReader.getColumn("demand"));
			this.cascadeMainContext.systemPriceSignalDataArray = Arrays.copyOf(systemBasePriceSignal, lengthOfProfileArrays);
			this.cascadeMainContext.systemPriceSignalDataLength = systemBasePriceSignal.length;

		}
		catch (FileNotFoundException e)
		{
			System.err.println("Could not find file with name " + householdOtherDemandFilename);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (this.cascadeMainContext.systemPriceSignalDataLength % this.cascadeMainContext.getNbOfTickPerDay() != 0)
		{
			System.err.println("Base System Demand array not a whole number of days. This may cause unexpected behaviour");
		}

		try
		{
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

			this.map_nbOfOccToOtherDemand = new WeakHashMap<Integer, double[]>();

			for (int i = 1; i <= numOfOtherDemandColumns; i++)
			{

				this.map_nbOfOccToOtherDemand.put(i, ArrayUtils.convertStringArrayToDoubleArray(otherDemandReader.getColumn("occ" + i)));
			}

		}
		catch (FileNotFoundException e)
		{
			System.err.println("Could not find file with name " + householdOtherDemandFilename);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}

		this.monthlyMainsWaterTemp = Arrays.copyOf(Consts.MONTHLY_MAINS_WATER_TEMP, Consts.MONTHLY_MAINS_WATER_TEMP.length);
	}

	/*
	 * private void createTestHHProsumersAndAddThemToContext() {
	 * 
	 * ProsumerFactory prosumerFactory =
	 * FactoryFinder.createProsumerFactory(this.cascadeMainContext);
	 * 
	 * double[] householdBaseDemandArray = null; for (int i = 0; i <
	 * numProsumers; i++) {
	 * 
	 * String demandName = "demand" + RandomHelper.nextIntFromTo(0,
	 * numDemandColumns - 1);
	 * 
	 * if (cascadeMainContext.verbose) { this.cascadeMainContext.logger.debug(
	 * "CascadeContextBuilder: householdBaseDemandArray is initialised with profile "
	 * + demandName); } //householdBaseDemandArray =
	 * ArrayUtils.convertStringArrayToDoubleArray
	 * (otherDemandReader.getColumn(demandName));
	 * 
	 * HHProsumer hhProsAgent =
	 * prosumerFactory.createHHProsumer(householdBaseDemandArray, false);
	 * 
	 * cascadeMainContext.add(hhProsAgent); } }
	 */

	/**
	 * This method initialize the probability distributions used in this model.
	 */
	private void initializeProbabilityDistributions()
	{
		if (this.cascadeMainContext.logger.isDebugEnabled())
		{
			this.cascadeMainContext.logger.debug("Random seed is" + RandomHelper.getSeed());
		}
		double[] drawOffDist = ArrayUtils.multiply(Consts.EST_DRAWOFF, ArrayUtils.sum(Consts.EST_DRAWOFF));
		if (this.cascadeMainContext.logger.isTraceEnabled())
		{
			this.cascadeMainContext.logger.trace("  ArrayUtils.sum(drawOffDist)" + ArrayUtils.sum(drawOffDist));
		}
		this.cascadeMainContext.drawOffGenerator = RandomHelper.createEmpiricalWalker(drawOffDist, Empirical.NO_INTERPOLATION);
if (		this.cascadeMainContext.logger.isTraceEnabled()) {
		this.cascadeMainContext.logger.trace("  ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY)"
				+ ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY));
}

		this.cascadeMainContext.occupancyGenerator = RandomHelper
				.createEmpiricalWalker(Consts.OCCUPANCY_PROBABILITY_ARRAY, Empirical.NO_INTERPOLATION);
		this.cascadeMainContext.waterUsageGenerator = RandomHelper.createNormal(0, 1);

		this.cascadeMainContext.buildingLossRateGenerator = RandomHelper.createNormal(275, 75);
		this.cascadeMainContext.thermalMassGenerator = RandomHelper.createNormal(12.5, 2.5);

		this.cascadeMainContext.coldAndWetApplTimeslotDelayRandDist = RandomHelper.createUniform();

		this.cascadeMainContext.wetApplProbDistGenerator = RandomHelper
				.createEmpiricalWalker(Consts.WET_APPLIANCE_PDF, Empirical.NO_INTERPOLATION);

		// ChartUtils.testProbabilityDistAndShowHistogram(cascadeMainContext.wetApplProbDistGenerator,
		// 10000, 48); //test to make sure the prob dist generate desired
		// outcomes

		// cascadeMainContext.hhProsumerElasticityTest =
		// RandomHelper.createBinomial(1, 0.005);

	}

	private void buildMarket()
	{

		SettlementCompany settlementCo = new SettlementCompany(this.cascadeMainContext);
		this.cascadeMainContext.add(settlementCo);

		this.messageBoard = new MarketMessageBoard();
		this.cascadeMainContext.add(this.messageBoard);
		this.messageBoard.setContext(this.cascadeMainContext);

		this.sysOp = new SystemOperator(this.cascadeMainContext, settlementCo, this.messageBoard);
		this.cascadeMainContext.add(this.sysOp);

		PowerExchange pEx = new PowerExchange(this.cascadeMainContext, this.messageBoard);
		this.cascadeMainContext.add(pEx);

	}

	private void populateContext()
	{
		double[] flatBaseline = new double[48];
		Arrays.fill(flatBaseline, this.numProsumers * 0.5);
		PassThroughAggregatorWithLag a1 = new PassThroughAggregatorWithLag(this.cascadeMainContext, this.messageBoard,
				-this.numProsumers * 1.0, 0, flatBaseline, 48);

		/*
		 * for (int i = 0; i < this.numProsumers; i++) { //Generate a prosumer
		 * with basic constant load somewhere between //0 and 1 kWh per half
		 * hour ConstantPlusElasticityProsumer p = new
		 * ConstantPlusElasticityProsumer(cascadeMainContext,
		 * RandomHelper.nextDouble()); }
		 */

if (		this.cascadeMainContext.logger.isTraceEnabled()) {
		this.cascadeMainContext.logger.trace("context now has "
				+ this.cascadeMainContext.getObjects(ConstantPlusElasticityProsumer.class).size() + " prosumers and "
				+ this.cascadeMainContext.getObjects(EquationBasedPriceAggregator.class).size() + " aggregators");
}

		NetworkFactory networkFactory = NetworkFactoryFinder.createNetworkFactory(null);

		this.createHouseholdProsumersAndAddThemToContext(Consts.RANDOM);

		// TODO: Add method that creates wind farm generators and adds them to
		// the context..

		// @TODO: these 4 methods below will eventually be included in the first
		// method (createHousholdPro...)
		if (Consts.HHPRO_HAS_WET_APPL)
		{
			this.initializeHHProsumersWetAppliancesPar4All();
		}

		if (Consts.HHPRO_HAS_COLD_APPL)
		{
			this.initializeHHProsumersColdAppliancesPar4All();
		}

		if (Consts.HHPRO_HAS_ELEC_WATER_HEAT)
		{
			this.initializeWithoutGasHHProsumersElecWaterHeat();
		}

		if (Consts.HHPRO_HAS_ELEC_SPACE_HEAT)
		{
			this.initializeWithoutGasHHProsumersElecSpaceHeat();
		}

		// if (Consts.HHPRO_HAS_ELEC_VEHICLE)
		// initializeElectricVehicles();

		/*
		 * //Add aggregator(s), currently one; (TODO should become a separate
		 * method later) AggregatorFactory aggregatorFactory =
		 * FactoryFinder.createAggregatorTraderFactory(this.cascadeMainContext,
		 * this.messageBoard);
		 * 
		 * SupplierCoAdvancedModel firstRecoAggregator =
		 * aggregatorFactory.createSupplierCoAdvanced
		 * (cascadeMainContext.systemPriceSignalDataArray);
		 * cascadeMainContext.add(firstRecoAggregator);
		 */

		// Economic network should be hierarchical aggregator to prosumer
		// TODO: Decide what economic network between aggregators looks like?
		// TODO: i.e. what is market design for aggregators?
		Network economicNet = networkFactory.createNetwork("economicNetwork", this.cascadeMainContext, true);

		// TODO: replace this with something better. Next iteration of code
		// should probably take network design from a file
		for (ProsumerAgent prAgent : (Iterable<ProsumerAgent>) (this.cascadeMainContext.getObjects(ProsumerAgent.class)))
		{
			economicNet.addEdge(a1, prAgent);
		}

		economicNet.addProjectionListener(a1); // Aggregator listens for changes
												// to its network

		this.cascadeMainContext.setEconomicNetwork(economicNet);

	}

	private WeakHashMap readGenericAggBaseProfileFiles()
	{
		// String currentDirectory = System.getProperty("user.dir"); //this
		// suppose to be the Eclipse project working space
		// String pathToDataFiles =
		// currentDirectory+ASTEMConsts.DATA_FILES_FOLDER_NAME;
		// File parentDataFilesDirectory = new File(pathToDataFiles);
		// File dmu_BaseProfiles_File = new File(parentDataFilesDirectory,
		// ASTEMConsts.BMU_BASE_PROFILES_FILENAME);

		File dmu_BaseProfiles_File = new File(this.dataDirectory, ASTEMConsts.BMU_BASE_PROFILES_FILENAME);

		WeakHashMap<String, double[]> mapOfTypeName2BaseProfileArray = new WeakHashMap<String, double[]>();

		try
		{
			CSVReader baseProfileCSVReader = new CSVReader(dmu_BaseProfiles_File);
			if (this.cascadeMainContext.logger.isDebugEnabled())
			{
				this.cascadeMainContext.logger.debug("baseProfileCSVReader created");
			}
			baseProfileCSVReader.parseByColumn();

			mapOfTypeName2BaseProfileArray.put("DEM_LARGE", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader
					.getColumn("DEM_LARGE")));
			mapOfTypeName2BaseProfileArray.put("DEM_SMALL", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader
					.getColumn("DEM_SMALL")));
			mapOfTypeName2BaseProfileArray.put("GEN_COAL", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader
					.getColumn("GEN_COAL")));
			mapOfTypeName2BaseProfileArray.put("GEN_CCGT", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader
					.getColumn("GEN_CCGT")));
			mapOfTypeName2BaseProfileArray.put("GEN_WIND", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader
					.getColumn("GEN_WIND")));

		}
		catch (FileNotFoundException e)
		{
			System.err.println("File not found: " + dmu_BaseProfiles_File.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}

		return mapOfTypeName2BaseProfileArray;
	}

	/**
	 * This methods create a number of generic aggregators needed to
	 * 
	 * @param mapOfDMUtypeName2BaselineProfileArray
	 */
	private void createAndAddGenericAggregators(WeakHashMap<String, double[]> mapOfDMUtypeName2BaselineProfileArray)
	{

		AggregatorFactory bmuFactory = new AggregatorFactory(this.cascadeMainContext, this.messageBoard);

		double[] arr_baseline_DEM_LARGE = mapOfDMUtypeName2BaselineProfileArray.get("DEM_LARGE");
		double[] arr_baseline_DEM_SMALL = mapOfDMUtypeName2BaselineProfileArray.get("DEM_SMALL");
		double[] arr_baseline_GEN_COAL = mapOfDMUtypeName2BaselineProfileArray.get("GEN_COAL");
		double[] arr_baseline_GEN_CCGT = mapOfDMUtypeName2BaselineProfileArray.get("GEN_CCGT");
		double[] arr_baseline_GEN_WIND = mapOfDMUtypeName2BaselineProfileArray.get("GEN_WIND");

		int numOfDEM_LARGE = 5;
		int numOfDEM_SMALL = 3;
		int numOfGEN_COAL = 1;
		int numOfGEN_CCGT = 2;
		int numOfGEN_WIND = 64;

		for (int i = 0; i < numOfDEM_LARGE; i++)
		{

			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.DEM_LARGE, arr_baseline_DEM_LARGE);
			this.cascadeMainContext.add(bmu);
		}

		for (int i = 0; i < numOfDEM_SMALL; i++)
		{
			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.DEM_SMALL, arr_baseline_DEM_SMALL);
			this.cascadeMainContext.add(bmu);
		}

		for (int i = 0; i < numOfGEN_COAL; i++)
		{
			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_COAL, arr_baseline_GEN_COAL);
			this.cascadeMainContext.add(bmu);
		}

		for (int i = 0; i < numOfGEN_CCGT; i++)
		{
			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_CCGT, arr_baseline_GEN_CCGT);
			this.cascadeMainContext.add(bmu);
		}

		for (int i = 0; i < numOfGEN_WIND; i++)
		{
			BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_WIND, arr_baseline_GEN_WIND);
			this.cascadeMainContext.add(bmu);
		}

	}

	/*
	 * Builds the <tt> Cascade Context </tt> (by calling other private
	 * sub-methods) In part, it instantiates the Cascade context by passing in
	 * the context that is passed to the builder
	 * 
	 * @see uk.ac.dmu.iesd.cascade.context.CascadeContext
	 * 
	 * @see
	 * repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context
	 * .Context)
	 * 
	 * @return built context
	 */
	@Override
	public CascadeContext build(Context context)
	{

		WeakHashMap<String, double[]> map_dmuTypeNameToBaseProfiles;

		this.cascadeMainContext = new CascadeContext(context); // build
																// CascadeContext
																// by passing
																// the context

		this.readParamsAndInitializeArrays();
		this.initializeProbabilityDistributions();
		this.buildMarket();
		this.populateContext();
		map_dmuTypeNameToBaseProfiles = this.readGenericAggBaseProfileFiles();

		this.createAndAddGenericAggregators(map_dmuTypeNameToBaseProfiles);

		if (CascadeContext.verbose)
		{
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("BasicWithMarketTestContextBuilder: Cascade Main Context created: "
					+ this.cascadeMainContext.toString());
}
		}

		return this.cascadeMainContext;
	}

	private void addHousesAndSuppCo()
	{
		/*
		 * TODO: NEED TO CREATE A MORE GENERIC SETUP STRUCTURE FOR ALL NETWORKS,
		 * AGGREGATORS, PROSUMERS, ETC..
		 */

		// createHouseholdProsumersAndAddThemToContext(2); //pass in parameter
		// nb of occupants, or random
		this.createHouseholdProsumersAndAddThemToContext(Consts.RANDOM);

		// TODO: Add method that creates wind farm generators and adds them to
		// the context..

		// @TODO: these 4 methods below will eventually be included in the first
		// method (createHousholdPro...)
		if (Consts.HHPRO_HAS_WET_APPL)
		{
			this.initializeHHProsumersWetAppliancesPar4All();
		}

		if (Consts.HHPRO_HAS_COLD_APPL)
		{
			this.initializeHHProsumersColdAppliancesPar4All();
		}

		if (Consts.HHPRO_HAS_ELEC_WATER_HEAT)
		{
			this.initializeWithoutGasHHProsumersElecWaterHeat();
		}

		if (Consts.HHPRO_HAS_ELEC_SPACE_HEAT)
		{
			this.initializeWithoutGasHHProsumersElecSpaceHeat();
		}

		if (Consts.HHPRO_HAS_ELEC_VEHICLE)
		{
			this.initializeElectricVehicles();
		}

		// Add aggregator(s), currently one; (TODO should become a separate
		// method later)
		AggregatorFactory aggregatorFactory = FactoryFinder.createAggregatorTraderFactory(this.cascadeMainContext, this.messageBoard);

		SupplierCoAdvancedModel firstRecoAggregator = aggregatorFactory
				.createSupplierCoAdvanced(this.cascadeMainContext.systemPriceSignalDataArray);
		this.cascadeMainContext.add(firstRecoAggregator);

		/**
		 * (02/07/12) DF
		 * 
		 * Add a simple single non domestic aggregator into
		 * <code>cascadeMainContext</code>
		 */
		// SingleNonDomesticAggregator singleNonDomestic =
		// aggregatorFactory.createSingleNonDomesticAggregator(cascadeMainContext.systemPriceSignalDataArray);
		// cascadeMainContext.add(singleNonDomestic);

		// buildSocialNetwork();

		// buildOtherNetworks(firstRecoAggregator); Do add to economic network

		// Add wind farms to the context
		// TODO: Add functionality to allow greater flexibility in the creation
		// of wind farms
		// createWindFarmsAndAddThemToContext();

		// Add Wind Farm Aggregator to context here..
		WindFarmAggregator wFA = aggregatorFactory.createWindFarmAggregator();
		this.cascadeMainContext.add(wFA);

		for (ProsumerAgent prAgent : (Iterable<ProsumerAgent>) (this.cascadeMainContext.getObjects(ProsumerAgent.class)))
		{
			if (prAgent instanceof WindGeneratorProsumer)
			{
				this.cascadeMainContext.getEconomicNetwork().addEdge(wFA, prAgent);
			}
		}

	}

	private void initializeElectricVehicles()
	{
		IndexedIterable<HouseholdProsumer> householdProsumers = this.cascadeMainContext.getObjects(HouseholdProsumer.class);

		for (HouseholdProsumer thisAgent : householdProsumers)
		{
			if (RandomHelper.nextDouble() < 0.25)
			{
				double[] EVprofile;
				if (RandomHelper.nextDouble() < 0.5)
				{
					EVprofile = EVProfileGenerator.generateBEVProfile(this.cascadeMainContext, Consts.NB_OF_DAYS_LOADED_DEMAND);
				}
				else
				{
					EVprofile = EVProfileGenerator.generatePHEVProfile(this.cascadeMainContext, Consts.NB_OF_DAYS_LOADED_DEMAND);
				}
				thisAgent.hasElectricVehicle = true;
				thisAgent.setEVProfile(EVprofile);

			}
		}
	}

	private void createHouseholdProsumersAndAddThemToContext(int occupancyModeOrNb)
	{

		ProsumerFactory prosumerFactory = FactoryFinder.createProsumerFactory(this.cascadeMainContext);

		if (this.cascadeMainContext.logger.isDebugEnabled())
		{
			this.cascadeMainContext.logger.debug("-----------------------------");
		}
		if (this.cascadeMainContext.logger.isDebugEnabled())
		{
			this.cascadeMainContext.logger.debug("% Of HHPros With Gas: " + this.percentageOfHHProsWithGas + "%");
		}

		int nbOfHHProsWithGas = (this.numProsumers * this.percentageOfHHProsWithGas) / 100;
		int nbOfHHProsWithElectricty = this.numProsumers - nbOfHHProsWithGas;

		if (this.cascadeMainContext.logger.isDebugEnabled())
		{
			this.cascadeMainContext.logger.debug("# of HHPros With Gas: " + nbOfHHProsWithGas);
		}
		if (this.cascadeMainContext.logger.isDebugEnabled())
		{
			this.cascadeMainContext.logger.debug("# of HHPros With Electricty: " + nbOfHHProsWithElectricty);
		}

		double[] hhOtherDemandArray = null; // Other elastic demand profiles
											// array consists of electricity
											// demand for cooking, lightening,
											// and brown(entertainment, computer
											// and small appliances)

		for (int i = 0; i < nbOfHHProsWithGas; i++)
		{

			HouseholdProsumer hhProsAgent = prosumerFactory
					.createHouseholdProsumer(this.map_nbOfOccToOtherDemand, occupancyModeOrNb, true, true);
			hhProsAgent.initializeWaterHeatDemand();

			if (!this.cascadeMainContext.add(hhProsAgent))
			{
				System.err.println("Failed to add agent to context!!");
			}
		}

		for (int i = 0; i < nbOfHHProsWithElectricty; i++)
		{

			/***
			 * TODO: JRS - this was using true on the randomize profile which,
			 * in turn, was making demand for a house potentially go -ve!!!
			 */
			HouseholdProsumer hhProsAgent = prosumerFactory
					.createHouseholdProsumer(this.map_nbOfOccToOtherDemand, occupancyModeOrNb, false, false);
			hhProsAgent.initializeWaterHeatDemand();

			if (!this.cascadeMainContext.add(hhProsAgent))
			{
				System.err.println("Failed to add agent to context!!");
			}
		}

if (		this.cascadeMainContext.logger.isDebugEnabled()) {
		this.cascadeMainContext.logger.debug("Total # of HHPros added to context: "
				+ this.cascadeMainContext.getObjects(HouseholdProsumer.class).size());
}
		if (this.cascadeMainContext.logger.isDebugEnabled())
		{
			this.cascadeMainContext.logger.debug("-----------------------------");
		}

	}

	private void initializeHHProsumersWetAppliancesPar4All()
	{

		IndexedIterable<HouseholdProsumer> householdProsumers = this.cascadeMainContext.getObjects(HouseholdProsumer.class);

		/*----------------
		 * Richard's occupancy test code
		 * 
		 * Note that this in effect is assuming that occupancy is independent of 
		 * any of the other assigned variables.  This may not, of course, be true.
		 */

		// assign wet appliance ownership. Based on statistical representation
		// of the BERR 2006 ownership stats
		// with a bias based on occupancy which seems reasonable.
		// TODO: break this out into a separate method. Store constants
		// somewhere? Should they read from file?
		for (HouseholdProsumer thisAgent : householdProsumers)
		{
			int occupancy = thisAgent.getNumOccupants();
			double randomVar = RandomHelper.nextDouble();
			if (this.cascadeMainContext.logger.isTraceEnabled())
			{
				this.cascadeMainContext.logger.trace("randomVar: " + randomVar);
			}
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
			if (!(thisAgent.hasWasherDryer)
					&& ((occupancy >= 3 && randomVar < 0.7) || (occupancy == 2 && randomVar < 0.45) || (occupancy == 1 && randomVar < 0.35)))
			{
				thisAgent.hasTumbleDryer = true;
			}

			randomVar = RandomHelper.nextDouble();
			if (((occupancy >= 3 && randomVar < 0.55) || (occupancy == 2 && randomVar < 0.25) || (occupancy == 1 && randomVar < 0.2)))
			{
				thisAgent.hasDishWasher = true;
			}

			thisAgent
					.setWetAppliancesProfiles(InitialProfileGenUtils
							.melodyStokesWetApplianceGen(this.cascadeMainContext, Consts.NB_OF_DAYS_LOADED_DEMAND, thisAgent.hasWashingMachine, thisAgent.hasWasherDryer, thisAgent.hasDishWasher, thisAgent.hasTumbleDryer));
			/* JRS TEST - REMOVE REMOVE REMOVE */
			this.setWetAppsPerPBMatlabPrototype(thisAgent);

		}

		if (CascadeContext.verbose)
		{
			if (this.cascadeMainContext.logger.isDebugEnabled())
			{
				this.cascadeMainContext.logger.debug("Percentages:");
			}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("households with occupancy 1 : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "numOccupants", 1)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("households with occupancy 2 : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "numOccupants", 2)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("households with occupancy 3 : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "numOccupants", 3)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("households with occupancy 4 : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "numOccupants", 4)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("households with occupancy 5 : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "numOccupants", 5)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("households with occupancy 6 : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "numOccupants", 6)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("households with occupancy 7 : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "numOccupants", 7)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("households with occupancy 8 : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "numOccupants", 8)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("Washing Mach : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasWashingMachine", true)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("Washer Dryer : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasWasherDryer", true)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("Tumble Dryer: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasTumbleDryer", true)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("Dish Washer : "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasDishWasher", true)).query())
					/ householdProsumers.size());
}
		}
	}

	public void setWetAppsPerPBMatlabPrototype(HouseholdProsumer thisAgent)
	{

		WeakHashMap<String, double[]> PB_wash_approx = new WeakHashMap<String, double[]>();
		double[] basicWashProfile = new double[48];
		if (RandomHelper.nextDouble() < 0.57)
		{
			int startSlot = (int) (RandomHelper.nextDouble() * 47);
			basicWashProfile[startSlot] = 1;
			basicWashProfile[startSlot + 1] = 1;
		}

		thisAgent.hasDishWasher = false;
		thisAgent.hasTumbleDryer = false;
		thisAgent.hasWasherDryer = false;
		PB_wash_approx.put(Consts.WET_APP_DRYER, new double[48]);
		PB_wash_approx.put(Consts.WET_APP_DISHWASHER, new double[48]);
		PB_wash_approx.put(Consts.WET_APP_DISHWASHER_ORIGINAL, new double[48]);
		PB_wash_approx.put(Consts.WET_APP_DRYER_ORIGINAL, new double[48]);
		PB_wash_approx.put(Consts.WET_APP_WASHER, basicWashProfile);
		PB_wash_approx.put(Consts.WET_APP_WASHER_ORIGINAL, basicWashProfile.clone());

		thisAgent.setWetAppliancesProfiles(PB_wash_approx);
	}

	/*
	 * private void initializeHHProsumersElecWaterHeat() {
	 * 
	 * //Iterable waterHeatedProsumersIter =
	 * cascadeMainContext.getRandomObjects(HouseholdProsumer.class, (long)
	 * (numProsumers * (Double) params.getValue("elecWaterFraction"))); Iterable
	 * waterHeatedProsumersIter =
	 * cascadeMainContext.getRandomObjects(HouseholdProsumer.class, (long)
	 * (numProsumers));
	 * 
	 * ArrayList prosumersWithElecWaterHeatList =
	 * IterableUtils.Iterable2ArrayList(waterHeatedProsumersIter);
	 * 
if (	 * this.cascadeMainContext.logger.isTraceEnabled()) {
	 * this.cascadeMainContext.logger.trace("ArrayList.size: WaterHeat "+
	 * prosumersWithElecWaterHeatList.size());
}
	 * AgentUtils.assignParameterSingleValue("hasElectricalWaterHeat", true,
	 * prosumersWithElecWaterHeatList.iterator());
	 * 
	 * Iterator iter = prosumersWithElecWaterHeatList.iterator();
	 * 
	 * while (iter.hasNext()) { ((HouseholdProsumer)
	 * iter.next()).initializeElectWaterHeatPar(); } }
	 */

	private void initializeWithoutGasHHProsumersElecSpaceHeat()
	{

		IndexedIterable<HouseholdProsumer> spaceHeatedProsumersIter = this.cascadeMainContext.getObjects(HouseholdProsumer.class);

		for (HouseholdProsumer hhPros : spaceHeatedProsumersIter)
		{
			if (!hhPros.isHasGas())
			{
				hhPros.setHasElectricalSpaceHeat(true);
				hhPros.initializeElecSpaceHeatPar();

			}
		}
	}

	private void initializeWithoutGasHHProsumersElecWaterHeat()
	{

		IndexedIterable<HouseholdProsumer> spaceHeatedProsumersIter = this.cascadeMainContext.getObjects(HouseholdProsumer.class);

		for (HouseholdProsumer hhPros : spaceHeatedProsumersIter)
		{

			if (!hhPros.isHasGas())
			{
				hhPros.setHasElectricalWaterHeat(true);
				hhPros.initializeElectWaterHeatPar();

			}
		}
	}

	/**
	 * This method uses rule set as described in Boait et al draft paper to
	 * assign cold appliance ownership on a stochastic, but statistically
	 * representative, basis.
	 */
	private void initializeHHProsumersColdAppliancesPar4All()
	{
		IndexedIterable<HouseholdProsumer> householdProsumers = this.cascadeMainContext.getObjects(HouseholdProsumer.class);

		for (HouseholdProsumer pAgent : householdProsumers)
		{
			// Set up cold appliance ownership
			if (RandomHelper.nextDouble() < 0.651)
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

			this.cascadeMainContext.logger.trace("Fridge; FridgeFreezer; Freezer: " + pAgent.hasRefrigerator + " "
					+ pAgent.hasFridgeFreezer + " " + (pAgent.hasUprightFreezer || pAgent.hasChestFreezer));

			// pAgent.coldApplianceProfile =
			// InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.DAYS_PER_YEAR,
			// pAgent.hasRefrigerator, pAgent.hasFridgeFreezer,
			// (pAgent.hasUprightFreezer && pAgent.hasChestFreezer));
			pAgent.setColdAppliancesProfiles(InitialProfileGenUtils
					.melodyStokesColdApplianceGen(Consts.NB_OF_DAYS_LOADED_DEMAND, pAgent.hasRefrigerator, pAgent.hasFridgeFreezer, (pAgent.hasUprightFreezer || pAgent.hasChestFreezer)));
		}

		if (CascadeContext.verbose)
		{
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("HHs with Fridge: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasRefrigerator", true)).query()));
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("HHs with FridgeFreezer: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasFridgeFreezer", true)).query()));
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("HHs with UprightFreezer: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasUprightFreezer", true)).query()));
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("HHs with ChestFreezer: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasChestFreezer", true)).query()));
}

if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("HHs with Fridge %: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasRefrigerator", true)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("HHs with FridgeFreezer %: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasFridgeFreezer", true)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("HHs with UprightFreezer %: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasUprightFreezer", true)).query())
					/ householdProsumers.size());
}
if (			this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("HHs with ChestFreezer %: "
					+ (double) IterableUtils.count((new PropertyEquals(this.cascadeMainContext, "hasChestFreezer", true)).query())
					/ householdProsumers.size());
}
		}
	}

}
