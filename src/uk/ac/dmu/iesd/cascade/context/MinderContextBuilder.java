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
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorFactory;
import uk.ac.dmu.iesd.cascade.agents.aggregators.BMPxTraderAggregator;
import uk.ac.dmu.iesd.cascade.agents.aggregators.EquationBasedPriceAggregator;
import uk.ac.dmu.iesd.cascade.agents.aggregators.EquationBasedPriceAggregatorWithLag;
import uk.ac.dmu.iesd.cascade.agents.aggregators.EstateManager;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ConstantPlusElasticityProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.RetailOutlet;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.market.astem.base.ASTEMConsts;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;
import uk.ac.dmu.iesd.cascade.market.astem.operators.PowerExchange;
import uk.ac.dmu.iesd.cascade.market.astem.operators.SettlementCompany;
import uk.ac.dmu.iesd.cascade.market.astem.operators.SystemOperator;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import cern.jet.random.Empirical;

/**
 * CASCADE Project Version [ Model Built version] (Version# for the entire project/ as whole)
 * @version $Revision: 2.00 $ $Date: 2011/10/05 
 * 
 * Major changes for this submission include: 
 * � All the elements ('variable' declarations, including data structures consisting of values 
 * [constant] or variables) of the type 'float' (32 bits) have been changed to 'double' (64 bits) (Babak Mahdavi)
 * 
 * CASCADE Project Version [ Model Built version] (Version# for the entire project/ as whole)
 *  @version $Revision: 3.00 $ $Date: 2012/05/30
 *  � Restructure of the entire project packages after with the integration with the ASTEM market model (Babak Mahdavi)
 *   
 */

/**
 * <em>MinderContextBuilder</em> is the Repast specific starting point class 
 * (i.e. a <code>ContextBuilder</code>) for 
 * building a context (i.e.{@link CascadeContext}) for the feasibility test of
 * MINDER project concept applied to retail spaces within the
 * <em>Cascade</em> framework.
 * Building a context consists of filling it with agents and other actors/components/etc. and
 * constructing displays/views for the model and so forth.
 * 
 * This particular context creator creates an aggregator
 * which simply relays the price from the market to a number of elastic prosumers.
 * 
 * @author J. Richard Snape
 * @version $Revision: 1.0 $ $Date: 2016/07/28 $
 * 
 * Version history (for intermediate steps see Git repository history)
 * 
 * 1.0 - Minder creator developed as a modification of BasicTestContextBuilder.java
 *     
 */

/**
 * Builds and returns a context. Building a context consists of filling it with
 * agents, adding projects and so forth.
 * 
 * @param context
 * @return the built context.
 */
public class MinderContextBuilder implements ContextBuilder<Object> {

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
	private void readParamsAndInitializeArrays() {
		// get the parameters from the current run environment
		this.params = RunEnvironment.getInstance().getParameters();
		String dataFileFolderPath = (String) this.params
				.getValue("dataFileFolder");
		String weatherFileName = (String) this.params.getValue("weatherFile");
		String systemDemandFileName = (String) this.params
				.getValue("systemBaseDemandFile");
		String householdOtherDemandFilename = (String) this.params
				.getValue("householdOtherDemandFile");
		// String elecLayoutFilename =
		// (String)params.getValue("electricalNetworkLayoutFile");
		this.numProsumers = (Integer) this.params
				.getValue("numControlledBuildings");
		this.percentageOfHHProsWithGas = (Integer) this.params
				.getValue("hhWithGasPercentage");
		this.cascadeMainContext.setTotalNbOfProsumers(this.numProsumers);
		int ticksPerDay = (Integer) this.params.getValue("ticksPerDay");
		this.cascadeMainContext.setNbOfTickPerDay(ticksPerDay);
		CascadeContext.verbose = (Boolean) this.params
				.getValue("verboseOutput");
		CascadeContext.chartSnapshotOn = (Boolean) this.params
				.getValue("chartSnapshot");
		this.cascadeMainContext.setChartSnapshotInterval((Integer) this.params
				.getValue("chartSnapshotInterval"));
		this.cascadeMainContext.signalMode = (Integer) this.params
				.getValue("signalMode");

		this.cascadeMainContext.setRandomSeedValue((Integer) this.params
				.getValue("randomSeed"));

		// RunEnvironment.getInstance().
		Date startDate;
		try {
			startDate = (new SimpleDateFormat("dd/MM/yyyy"))
					.parse((String) this.params.getValue("startDate"));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			System.err
					.println("CascadeContextBuilder: The start date parameter is in a format which cannot be parsed by this program");
			System.err
					.println("CascadeContextBuilder: The data will be set to 01/01/2000 by default");
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
		File systemDemandFile = new File(this.dataDirectory,
				systemDemandFileName);
		CSVReader systemBasePriceReader = null;

		File householdOtherDemandFile = new File(this.dataDirectory,
				householdOtherDemandFilename);

		int lengthOfProfileArrays = ticksPerDay
				* Consts.NB_OF_DAYS_LOADED_DEMAND;

		try {
			weatherReader = new CSVReader(weatherFile);
			weatherReader.parseByColumn();

			double[] insolationArray_all = ArrayUtils
					.convertStringArrayToDoubleArray(weatherReader
							.getColumn("insolation"));
			double[] windSpeedArray_all = ArrayUtils
					.convertStringArrayToDoubleArray(weatherReader
							.getColumn("windSpeed"));
			double[] airTemperatureArray_all = ArrayUtils
					.convertStringArrayToDoubleArray(weatherReader
							.getColumn("airTemp"));

			this.cascadeMainContext.insolationArray = Arrays.copyOf(
					insolationArray_all, lengthOfProfileArrays);
			this.cascadeMainContext.windSpeedArray = Arrays.copyOf(
					windSpeedArray_all, lengthOfProfileArrays);
			this.cascadeMainContext.airTemperatureArray = Arrays.copyOf(
					airTemperatureArray_all, lengthOfProfileArrays);

			this.cascadeMainContext.weatherDataLength = lengthOfProfileArrays;

		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name "
					+ weatherFile.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (this.cascadeMainContext.weatherDataLength
				% this.cascadeMainContext.getNbOfTickPerDay() != 0) {
			System.err
					.println("Weather data array not a whole number of days. This may cause unexpected behaviour ");
		}

		try {
			systemBasePriceReader = new CSVReader(systemDemandFile);
			systemBasePriceReader.parseByColumn();

			double[] systemBasePriceSignal = ArrayUtils
					.convertStringArrayToDoubleArray(systemBasePriceReader
							.getColumn("demand"));
			this.cascadeMainContext.systemPriceSignalDataArray = Arrays.copyOf(
					systemBasePriceSignal, lengthOfProfileArrays);
			this.cascadeMainContext.systemPriceSignalDataLength = systemBasePriceSignal.length;

		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name "
					+ householdOtherDemandFilename);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (this.cascadeMainContext.systemPriceSignalDataLength
				% this.cascadeMainContext.getNbOfTickPerDay() != 0) {
			System.err
					.println("Base System Demand array not a whole number of days. This may cause unexpected behaviour");
		}

		try {
			CSVReader otherDemandReader = new CSVReader(
					householdOtherDemandFile);

			otherDemandReader.parseByColumn();
			int numOfOtherDemandColumns = otherDemandReader
					.columnsStarting("occ");

			if (numOfOtherDemandColumns == 0) {
				System.err
						.println("The household demand data files appears to have no demand data columns");
				System.err
						.println("Demand data columns should be headed 'demand' followed by an integer e.g. 'demand0', 'demand1'...");
				System.err
						.println("Proceeding with no demand data would cause failure, so the program will now terminate");
				System.err.println("Please check file "
						+ householdOtherDemandFile.getAbsolutePath());
				System.exit(1);
			}

			this.map_nbOfOccToOtherDemand = new WeakHashMap<Integer, double[]>();

			for (int i = 1; i <= numOfOtherDemandColumns; i++) {

				this.map_nbOfOccToOtherDemand.put(i, ArrayUtils
						.convertStringArrayToDoubleArray(otherDemandReader
								.getColumn("occ" + i)));
			}

		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name "
					+ householdOtherDemandFilename);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}

		this.monthlyMainsWaterTemp = Arrays.copyOf(
				Consts.MONTHLY_MAINS_WATER_TEMP,
				Consts.MONTHLY_MAINS_WATER_TEMP.length);
	}


	/**
	 * This method initialize the probability distributions used in this model.
	 */
	private void initializeProbabilityDistributions() {
		if (this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("Random seed is"
					+ RandomHelper.getSeed());
		}
		double[] drawOffDist = ArrayUtils.multiply(Consts.EST_DRAWOFF,
				ArrayUtils.sum(Consts.EST_DRAWOFF));
		if (this.cascadeMainContext.logger.isTraceEnabled()) {
			this.cascadeMainContext.logger
					.trace("  ArrayUtils.sum(drawOffDist)"
							+ ArrayUtils.sum(drawOffDist));
		}
		this.cascadeMainContext.drawOffGenerator = RandomHelper
				.createEmpiricalWalker(drawOffDist, Empirical.NO_INTERPOLATION);
		if (this.cascadeMainContext.logger.isTraceEnabled()) {
			this.cascadeMainContext.logger
					.trace("  ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY)"
							+ ArrayUtils
									.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY));
		}

		this.cascadeMainContext.occupancyGenerator = RandomHelper
				.createEmpiricalWalker(Consts.OCCUPANCY_PROBABILITY_ARRAY,
						Empirical.NO_INTERPOLATION);
		this.cascadeMainContext.waterUsageGenerator = RandomHelper
				.createNormal(0, 1);

		this.cascadeMainContext.buildingLossRateGenerator = RandomHelper
				.createNormal(275, 75);
		this.cascadeMainContext.thermalMassGenerator = RandomHelper
				.createNormal(12.5, 2.5);

		this.cascadeMainContext.coldAndWetApplTimeslotDelayRandDist = RandomHelper
				.createUniform();

		this.cascadeMainContext.wetApplProbDistGenerator = RandomHelper
				.createEmpiricalWalker(Consts.WET_APPLIANCE_PDF,
						Empirical.NO_INTERPOLATION);



	}

	private void buildMarket() {

		SettlementCompany settlementCo = new SettlementCompany(
				this.cascadeMainContext);
		this.cascadeMainContext.add(settlementCo);

		this.messageBoard = new MarketMessageBoard();
		this.cascadeMainContext.add(this.messageBoard);
		this.messageBoard.setContext(this.cascadeMainContext);

		this.sysOp = new SystemOperator(this.cascadeMainContext, settlementCo,
				this.messageBoard);
		this.cascadeMainContext.add(this.sysOp);

		PowerExchange pEx = new PowerExchange(this.cascadeMainContext,
				this.messageBoard);
		this.cascadeMainContext.add(pEx);

	}

	private void populateContext() throws FileNotFoundException {

		EstateManager a1 = new EstateManager(this.cascadeMainContext);
		this.cascadeMainContext.add(a1);

		String[] buildingNames = new String[] {"Lon", "Leu", "Fin"};

		//String[] co2profiles = new String[]{"co2_1"};
		String[] co2profiles = new String[]{"co2_1","co2_2","co2_flat"};

		for (int i = 0; i < this.numProsumers; i++) {
			// Generate a prosumer with basic constant load somewhere between
			// 0 and 1 kWh per half hour
			RetailOutlet p = new RetailOutlet( this.cascadeMainContext,
					// buildingNames[RandomHelper.nextIntFromTo(0,2)], co2profile);
					buildingNames[i%3], co2profiles);
			if (!this.cascadeMainContext.add(p))
			{
				System.err.println("Failed to add RetailOutlet agent to context!!");
			}
			double[] predictedCostSignal = new double[48];
			Arrays.fill(predictedCostSignal, 1);
			for (int j =0; j < 48; j++)
			{
				predictedCostSignal[j] = (48.0-j)/48;
			}
			
			p.setPredictedCostSignal(predictedCostSignal);
		}

		if (this.cascadeMainContext.logger.isDebugEnabled()) {
			this.cascadeMainContext.logger.debug("context now has "
					+ this.cascadeMainContext.getObjects(
						RetailOutlet.class).size()
					+ " retail outlets and "
					+ this.cascadeMainContext.getObjects(
							EstateManager.class).size()
					+ " Estate Manager aggregators");
		}

		NetworkFactory networkFactory = NetworkFactoryFinder
				.createNetworkFactory(null);

		// Economic network should be hierarchical aggregator to prosumer
		// TODO: Decide what economic network between aggregators looks like?
		// TODO: i.e. what is market design for aggregators?
		Network economicNet = networkFactory.createNetwork("economicNetwork",
				this.cascadeMainContext, true);

		// TODO: replace this with something better. Next iteration of code
		// should probably take network design from a file
		for (ProsumerAgent prAgent : (Iterable<ProsumerAgent>) (this.cascadeMainContext
				.getObjects(ProsumerAgent.class))) {
			economicNet.addEdge(a1, prAgent);
		}

		this.cascadeMainContext.setEconomicNetwork(economicNet);

	}

	private WeakHashMap readGenericAggBaseProfileFiles() {
		// String currentDirectory = System.getProperty("user.dir"); //this
		// suppose to be the Eclipse project working space
		// String pathToDataFiles =
		// currentDirectory+ASTEMConsts.DATA_FILES_FOLDER_NAME;
		// File parentDataFilesDirectory = new File(pathToDataFiles);
		// File dmu_BaseProfiles_File = new File(parentDataFilesDirectory,
		// ASTEMConsts.BMU_BASE_PROFILES_FILENAME);

		File dmu_BaseProfiles_File = new File(this.dataDirectory,
				ASTEMConsts.BMU_BASE_PROFILES_FILENAME);

		WeakHashMap<String, double[]> mapOfTypeName2BaseProfileArray = new WeakHashMap<String, double[]>();

		try {
			CSVReader baseProfileCSVReader = new CSVReader(
					dmu_BaseProfiles_File);
			if (this.cascadeMainContext.logger.isDebugEnabled()) {
				this.cascadeMainContext.logger
						.debug("baseProfileCSVReader created");
			}
			baseProfileCSVReader.parseByColumn();

			mapOfTypeName2BaseProfileArray.put("DEM_LARGE", ArrayUtils
					.convertStringArrayToDoubleArray(baseProfileCSVReader
							.getColumn("DEM_LARGE")));
			mapOfTypeName2BaseProfileArray.put("DEM_SMALL", ArrayUtils
					.convertStringArrayToDoubleArray(baseProfileCSVReader
							.getColumn("DEM_SMALL")));
			mapOfTypeName2BaseProfileArray.put("GEN_COAL", ArrayUtils
					.convertStringArrayToDoubleArray(baseProfileCSVReader
							.getColumn("GEN_COAL")));
			mapOfTypeName2BaseProfileArray.put("GEN_CCGT", ArrayUtils
					.convertStringArrayToDoubleArray(baseProfileCSVReader
							.getColumn("GEN_CCGT")));
			mapOfTypeName2BaseProfileArray.put("GEN_WIND", ArrayUtils
					.convertStringArrayToDoubleArray(baseProfileCSVReader
							.getColumn("GEN_WIND")));

		} catch (FileNotFoundException e) {
			System.err.println("File not found: "
					+ dmu_BaseProfiles_File.getAbsolutePath());
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
	private void createAndAddGenericAggregators(
			WeakHashMap<String, double[]> mapOfDMUtypeName2BaselineProfileArray) {

		AggregatorFactory bmuFactory = new AggregatorFactory(
				this.cascadeMainContext, this.messageBoard);

		double[] arr_baseline_DEM_LARGE = mapOfDMUtypeName2BaselineProfileArray
				.get("DEM_LARGE");
		double[] arr_baseline_DEM_SMALL = mapOfDMUtypeName2BaselineProfileArray
				.get("DEM_SMALL");
		double[] arr_baseline_GEN_COAL = mapOfDMUtypeName2BaselineProfileArray
				.get("GEN_COAL");
		double[] arr_baseline_GEN_CCGT = mapOfDMUtypeName2BaselineProfileArray
				.get("GEN_CCGT");
		double[] arr_baseline_GEN_WIND = mapOfDMUtypeName2BaselineProfileArray
				.get("GEN_WIND");

		int numOfDEM_LARGE = 5;
		int numOfDEM_SMALL = 2;
		int numOfGEN_COAL = 2;
		int numOfGEN_CCGT = 3;
		int numOfGEN_WIND = 44;

		for (int i = 0; i < numOfDEM_LARGE; i++) {

			BMPxTraderAggregator bmu = bmuFactory
					.createGenericBMPxTraderAggregator(BMU_TYPE.DEM_LARGE,
							arr_baseline_DEM_LARGE);
			this.cascadeMainContext.add(bmu);
		}

		for (int i = 0; i < numOfDEM_SMALL; i++) {
			BMPxTraderAggregator bmu = bmuFactory
					.createGenericBMPxTraderAggregator(BMU_TYPE.DEM_SMALL,
							arr_baseline_DEM_SMALL);
			this.cascadeMainContext.add(bmu);
		}

		for (int i = 0; i < numOfGEN_COAL; i++) {
			BMPxTraderAggregator bmu = bmuFactory
					.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_COAL,
							arr_baseline_GEN_COAL);
			this.cascadeMainContext.add(bmu);
		}

		for (int i = 0; i < numOfGEN_CCGT; i++) {
			BMPxTraderAggregator bmu = bmuFactory
					.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_CCGT,
							arr_baseline_GEN_CCGT);
			this.cascadeMainContext.add(bmu);
		}

		for (int i = 0; i < numOfGEN_WIND; i++) {
			BMPxTraderAggregator bmu = bmuFactory
					.createGenericBMPxTraderAggregator(BMU_TYPE.GEN_WIND,
							arr_baseline_GEN_WIND);
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
	public CascadeContext build(Context context) {

		WeakHashMap<String, double[]> map_dmuTypeNameToBaseProfiles;

		this.cascadeMainContext = new CascadeContext(context); // build
																// CascadeContext
																// by passing
																// the context

		this.readParamsAndInitializeArrays();
		this.initializeProbabilityDistributions();
		this.buildMarket();
		try {
			this.populateContext();
		} catch (FileNotFoundException e) {
			System.err.println("One of the files needed to populate the context is missing!!!");
 			e.printStackTrace();
 			System.exit(1);
		}
		map_dmuTypeNameToBaseProfiles = this.readGenericAggBaseProfileFiles();

		this.createAndAddGenericAggregators(map_dmuTypeNameToBaseProfiles);

		if (CascadeContext.verbose) {
			if (this.cascadeMainContext.logger.isDebugEnabled()) {
				this.cascadeMainContext.logger
						.debug("CascadeContextBuilder: Cascade Main Context created: "
								+ this.cascadeMainContext.toString());
			}
		}

		return this.cascadeMainContext;
	}

}
