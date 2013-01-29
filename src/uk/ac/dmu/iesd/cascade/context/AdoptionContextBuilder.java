package uk.ac.dmu.iesd.cascade.context;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import cern.jet.random.Empirical;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.ContextGeography;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkFactory;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.context.space.graph.NetworkGenerator;
import repast.simphony.context.space.graph.WattsBetaSmallWorldGenerator;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.GUIRegistry;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.PropertyEquals;
import repast.simphony.query.Query;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.DefaultGeography;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.ShapefileLoader;
import repast.simphony.space.gis.ShapefileWriter;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.RandomGridAdder;
import repast.simphony.space.grid.StickyBorders;
import repast.simphony.ui.RSApplication;
import repast.simphony.util.collections.IndexedIterable;
import repast.simphony.visualization.IDisplay;
import repast.simphony.visualizationOGL2D.DisplayOGL2D;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorFactory;
import uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCoAdvancedModel;
import uk.ac.dmu.iesd.cascade.agents.aggregators.WindFarmAggregator;
import uk.ac.dmu.iesd.cascade.agents.prosumers.Household;
import uk.ac.dmu.iesd.cascade.agents.prosumers.Household;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.WindGeneratorProsumer;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.base.FactoryFinder;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
//import uk.ac.dmu.iesd.cascade.styles.HouseholdTwoDStyle;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.InitialProfileGenUtils;
import uk.ac.dmu.iesd.cascade.util.IterableUtils;

public class AdoptionContextBuilder implements ContextBuilder<Household>
{

	boolean writeInitialShapefile = false;
	private Parameters params; // parameters for the model run environment
	private AdoptionContext myContext;
	WeakHashMap<Integer, double[]> map_nbOfOccToOtherDemand;
	private double[] monthlyMainsWaterTemp;

	@SuppressWarnings("unchecked")
	@Override
	public Context build(Context context)
	{
		myContext = new AdoptionContext(context, "01/04/2010");

		readParamsAndInitializeArrays();
		initializeProbabilityDistributions();
		myContext.logger.debug("Got into the context builder");
		myContext.logger.debug("Random seed is " + RepastEssentials.GetParameter("randomSeed").toString());

		if (!context.isEmpty())
		{
			myContext.logger.warn("Context contains some stuff before the build!!");
		}

		myContext.logger.debug("Created Poisson distribution, ID " + myContext.nextThoughtGenerator.toString());
		// myContext.setId("SmartGridTechnologyAdoption");
		// myContext.setTypeID("SmartGridTechnologyAdoption");
		/*
		 * myContext.setNorthLim(52.62); myContext.setSouthLin(52.58);
		 * myContext.setEastLim(-1.05); myContext.setWestLim(-1.1);
		 */
		myContext.PVFITs = new TreeMap<Integer, Integer>();
		myContext.PVFITs.put(4, 370);
		myContext.PVFITs.put(10, 310);
		myContext.PVFITs.put(100, 280);
		myContext.PVFITs.put(5000, 260);

		DefaultGeography<Household> leicesterGeography = new DefaultGeography<Household>("Leicester");

		myContext.addProjection(leicesterGeography);

		/*
		 * File file = new File("/Users/nick/tmp/another.shp"); ShapefileLoader
		 * loader = null; try { loader = new ShapefileLoader(GisAgent.class,
		 * file.toURL(), geography, context); loader.load(); } catch
		 * (MalformedURLException e) { e.printStackTrace(); }
		 */
		CSVReader defraCategories = null;
		CSVReader defraProfiles = null;
		String dataDirectory = RepastEssentials.GetParameter("RootDir").toString() + "/dataFiles";
		String categoryFile = dataDirectory + "/DEFRA_pro_env_categories.csv";
		String profileFile = dataDirectory + "/200profiles.csv";
		String weatherFileName = "weatherProfiles_1Ywind81.csv";

		try
		{
			defraCategories = new CSVReader(categoryFile);
		} catch (FileNotFoundException e)
		{
			System.err.println("AdoptionContextBuilder: File containing DEFRA types not found at " + categoryFile);
			System.err.println("AdoptionContextBuilder: Doesn't look like this will work, terminating");
			System.err.println(e.getMessage());
			System.exit(Consts.BAD_FILE_ERR_CODE);
		}

		try
		{
			defraProfiles = new CSVReader(profileFile);
		} catch (FileNotFoundException e)
		{
			System.err.println("AdoptionContextBuilder: File containing average profiles for DEFRA types not found at " + profileFile);
			System.err.println("AdoptionContextBuilder: Doesn't look like this will work, terminating");
			System.err.println(e.getMessage());
			System.exit(Consts.BAD_FILE_ERR_CODE);
		}

		defraCategories.parseByColumn();
		defraProfiles.parseByColumn();
		Empirical myDist = RandomHelper.createEmpirical(ArrayUtils.convertStringArrayToDoubleArray(defraCategories.getColumn("Population_fraction")), Empirical.NO_INTERPOLATION);

		myContext.logger.trace("Empirical distribution set up to assign DEFRA categories by population fraction");

		ArrayList<Household> households;

		households = new ArrayList<Household>();
		int numHouseholds = 1000;
		for (int i = 0; i < numHouseholds; i++)
		{
			Household thisHousehold = createHouseholdProsumer(this.map_nbOfOccToOtherDemand, Consts.RANDOM, true, false);//(RandomHelper.nextDouble() < 0.7));
			households.add(thisHousehold);
			if (!myContext.add(thisHousehold))
			{
				System.err.println("Adding household to context failed");
			} else
			{
				thisHousehold.setContext(myContext);
			}

			double lon = 52.58 + 0.004 * RandomHelper.nextDouble();
			double lat = -1.05 - 0.005 * RandomHelper.nextDouble();
			Coordinate coord = new Coordinate(lat, lon);
			GeometryFactory fac = new GeometryFactory();
			Point geom = fac.createPoint(coord);
			leicesterGeography.move(thisHousehold, geom);
			thisHousehold.setGeography(leicesterGeography);

			thisHousehold.setPredictedCostSignal(new double[48]);
		}

		/*
		 * File file = new
		 * File(RepastEssentials.GetParameter("RootDir").toString() +
		 * "/dataFiles/probDomesticLE2.shp"); ShapefileLoader loader = null; try
		 * { loader = new ShapefileLoader(Household.class, file.toURL(),
		 * leicesterGeography, myContext);
		 * System.out.println("have shapefile initialised with " +
		 * file.toURL().toString()); loader.load(); } catch
		 * (MalformedURLException e) { e.printStackTrace(); }
		 */

		households = IterableUtils.Iterable2ArrayList(myContext.getObjects(Household.class));
		double observedDistanceMean = (Double) RepastEssentials.GetParameter("ObservedRadiusMean");
		double observedDistanceStd = (Double) RepastEssentials.GetParameter("ObservedRadiusStd");
		RandomHelper.createNormal(observedDistanceMean, observedDistanceStd);

		int tmp = 0;
		for (Household thisHousehold : households)
		{
			thisHousehold.setGeography(leicesterGeography);
			// Need to think about defining the column names as consts, or
			// otherwise working out
			// How we import files, whether column names are pre-ordained, or
			// arbitrary etc.

			int custSegment = 0;
			double choiceVar = RandomHelper.nextDouble();
			int j = 0;
			while (custSegment < 1)
			{
				if (choiceVar < myDist.cdf(j))
				{
					custSegment = j;
				}
				j++;
			}

			myContext.logger.trace(thisHousehold.getAgentName() + "DEFRA Customer segment is" + custSegment);

			thisHousehold.defraCategory = Integer.parseInt(defraCategories.getColumn("DEFRA_category")[custSegment - 1]);
			thisHousehold.economicAbility = Double.parseDouble(defraCategories.getColumn("Economic_ability")[custSegment - 1]);
			thisHousehold.microgenPropensity = Double.parseDouble(defraCategories.getColumn("Microgen_propensity")[custSegment - 1]);
			thisHousehold.insulationPropensity = Double.parseDouble(defraCategories.getColumn("Insulation_propensity")[custSegment - 1]);
			thisHousehold.HEMSPropensity = Double.parseDouble(defraCategories.getColumn("HEMS_propensity")[custSegment - 1]);
			thisHousehold.EVPropensity = Double.parseDouble(defraCategories.getColumn("EV_propensity")[custSegment - 1]);
			thisHousehold.habit = Double.parseDouble(defraCategories.getColumn("Habit_factor")[custSegment - 1]);
			thisHousehold.hasPV = (RandomHelper.nextDouble() / 25 < thisHousehold.microgenPropensity);
			//thisHousehold.hasPV=false; //pre-initialise No PV
			if (thisHousehold.hasPV) thisHousehold.ratedPowerPV=3;

			thisHousehold.setAdoptionThreshold(0.5);
			// thisHousehold.observedRadius = (RandomHelper.nextDouble() * 15);
			thisHousehold.observedRadius = RandomHelper.getNormal().nextDouble();
			myContext.logger.trace(thisHousehold.getAgentName() + " observes " + thisHousehold.observedRadius);
			myContext.logger.trace(thisHousehold.getAgentName() + " has " + thisHousehold.microgenPropensity + " and pre-assigned PV = " + thisHousehold.getHasPV());
			tmp += thisHousehold.hasPV ? 1 : 0;
		}

		myContext.logger.debug(tmp + " households out of " + households.size() + " have PV");
		myContext.logger.info(households.size() + " Households initialized and added to context and geography");

		/*
		 * if (writeInitialShapefile) {
		 * myContext.logger.trace("Writing shapefile of initial conditions");
		 * 
		 * ShapefileWriter testWriter = new ShapefileWriter(leicesterGeography);
		 * File outFile = new File(
		 * "C:/RepastSimphony-2.0-beta/workspace/SmartGridTechnologyAdoption/output/shapeFileInitialisedSeed"
		 * + RepastEssentials.GetParameter("randomSeed") .toString() + ".shp");
		 * try { testWriter.write(leicesterGeography.getLayer(Household.class)
		 * .getName(), outFile.toURL()); } catch (MalformedURLException e) { //
		 * TODO Auto-generated catch block e.printStackTrace(); } }
		 */

		populateContext();
		return myContext;
	}

	/*
	 * Read the model environment parameters and initialize arrays
	 */
	private void readParamsAndInitializeArrays()
	{
		// get the parameters from the current run environment
		params = RunEnvironment.getInstance().getParameters();
		String dataFileFolderPath = (String) params.getValue("dataFileFolder");
		String weatherFileName = (String) params.getValue("weatherFile");
		String systemDemandFileName = (String) params.getValue("systemBaseDemandFile");
		String householdOtherDemandFilename = (String) params.getValue("householdOtherDemandFile");
		// String elecLayoutFilename =
		// (String)params.getValue("electricalNetworkLayoutFile");
		// numProsumers = (Integer)
		// params.getValue("defaultProsumersPerFeeder");
		// percentageOfHHProsWithGas = (Integer)
		// params.getValue("hhWithGasPercentage");
		// myContext.setTotalNbOfProsumers(numProsumers);
		int ticksPerDay = (Integer) params.getValue("ticksPerDay");
		myContext.setNbOfTickPerDay(ticksPerDay);
		// myContext.verbose = (Boolean) params.getValue("verboseOutput");
		// myContext.chartSnapshotOn = (Boolean)
		// params.getValue("chartSnapshot");
		// myContext.setChartSnapshotInterval((Integer)
		// params.getValue("chartSnapshotInterval"));
		// myContext.signalMode = (Integer)params.getValue("signalMode");

		// myContext.setRandomSeedValue((Integer)params.getValue("randomSeed"));
		// myContext.setGasPercentage(percentageOfHHProsWithGas);

		/*
		 * Read in the necessary data files and store to the context
		 */
		File dataDirectory = new File(dataFileFolderPath);
		File weatherFile = new File(dataDirectory, weatherFileName);
		CSVReader weatherReader = null;
		File systemDemandFile = new File(dataDirectory, systemDemandFileName);
		CSVReader systemBasePriceReader = null;

		File householdOtherDemandFile = new File(dataDirectory, householdOtherDemandFilename);

		int lengthOfProfileArrays = ticksPerDay * Consts.NB_OF_DAYS_LOADED_DEMAND;

		try
		{
			weatherReader = new CSVReader(weatherFile);
			weatherReader.parseByColumn();

			double[] insolationArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("insolation"));
			double[] windSpeedArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("windSpeed"));
			double[] airTemperatureArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("airTemp"));
			double[] airDensityArray_all = ArrayUtils.convertStringArrayToDoubleArray(weatherReader.getColumn("airDensity"));

			myContext.insolationArray = Arrays.copyOf(insolationArray_all, insolationArray_all.length);
			myContext.windSpeedArray = Arrays.copyOf(windSpeedArray_all, windSpeedArray_all.length);
			myContext.airTemperatureArray = Arrays.copyOf(airTemperatureArray_all, airTemperatureArray_all.length);
			myContext.airDensityArray = Arrays.copyOf(airDensityArray_all, airDensityArray_all.length);

			myContext.weatherDataLength = windSpeedArray_all.length;// lengthOfProfileArrays;

		} catch (FileNotFoundException e)
		{
			System.err.println("Could not find file with name " + weatherFile.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (myContext.weatherDataLength % myContext.getNbOfTickPerDay() != 0)
		{
			System.err.println("Weather data array not a whole number of days. This may cause unexpected behaviour ");
		}

		/*
		 * try { systemBasePriceReader = new CSVReader(systemDemandFile);
		 * systemBasePriceReader.parseByColumn();
		 * 
		 * double[] systemBasePriceSignal =
		 * ArrayUtils.convertStringArrayToDoubleArray
		 * (systemBasePriceReader.getColumn("demand"));
		 * myContext.systemPriceSignalDataArray =
		 * Arrays.copyOf(systemBasePriceSignal, lengthOfProfileArrays);
		 * myContext.systemPriceSignalDataLength = systemBasePriceSignal.length;
		 * 
		 * } catch (FileNotFoundException e) {
		 * System.err.println("Could not find file with name " +
		 * householdOtherDemandFilename); e.printStackTrace();
		 * RunEnvironment.getInstance().endRun(); } if
		 * (myContext.systemPriceSignalDataLength %
		 * myContext.getNbOfTickPerDay()!= 0) { System.err.println(
		 * "Base System Demand array not a whole number of days. This may cause unexpected behaviour"
		 * ); }
		 */
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

			map_nbOfOccToOtherDemand = new WeakHashMap<Integer, double[]>();

			for (int i = 1; i <= numOfOtherDemandColumns; i++)
			{

				this.map_nbOfOccToOtherDemand.put(i, ArrayUtils.convertStringArrayToDoubleArray(otherDemandReader.getColumn("occ" + i)));
			}

		} catch (FileNotFoundException e)
		{
			System.err.println("Could not find file with name " + householdOtherDemandFilename);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}

		this.monthlyMainsWaterTemp = Arrays.copyOf(Consts.MONTHLY_MAINS_WATER_TEMP, Consts.MONTHLY_MAINS_WATER_TEMP.length);
	}

	/**
	 * This method uses rule set as described in Boait et al draft paper to
	 * assign cold appliance ownership on a stochastic, but statistically
	 * representative, basis.
	 */
	private void initializeHHProsumersColdAppliancesPar4All()
	{
		IndexedIterable<Household> householdProsumers = myContext.getObjects(Household.class);

		for (Household pAgent : householdProsumers)
		{
			// Set up cold appliance ownership
			if (RandomHelper.nextDouble() < 0.651)
			{
				pAgent.hasFridgeFreezer = true;
				if (RandomHelper.nextDouble() < 0.15)
				{
					pAgent.hasRefrigerator = true;
				}
			} else
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

			// if (Consts.DEBUG)
			// System.out.println("Fridge; FridgeFreezer; Freezer: "+
			// pAgent.hasRefrigerator +" "+pAgent.hasFridgeFreezer + " "+
			// (pAgent.hasUprightFreezer || pAgent.hasChestFreezer));

			// pAgent.coldApplianceProfile =
			// InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.DAYS_PER_YEAR,
			// pAgent.hasRefrigerator, pAgent.hasFridgeFreezer,
			// (pAgent.hasUprightFreezer && pAgent.hasChestFreezer));
			pAgent.setColdAppliancesProfiles(InitialProfileGenUtils.melodyStokesColdApplianceGen(Consts.NB_OF_DAYS_LOADED_DEMAND, pAgent.hasRefrigerator, pAgent.hasFridgeFreezer, (pAgent.hasUprightFreezer || pAgent.hasChestFreezer)));
		}

		/*
		 * if(myContext.verbose) { System.out.println("HHs with Fridge: " +
		 * (double) IterableUtils.count((new PropertyEquals(myContext,
		 * "hasRefrigerator",true)).query()));
		 * System.out.println("HHs with FridgeFreezer: " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasFridgeFreezer",true)).query()));
		 * System.out.println("HHs with UprightFreezer: " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasUprightFreezer",true)).query()));
		 * System.out.println("HHs with ChestFreezer: " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasChestFreezer",true)).query()));
		 * 
		 * System.out.println("HHs with Fridge %: " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasRefrigerator",true)).query()) / householdProsumers.size());
		 * System.out.println("HHs with FridgeFreezer %: " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasFridgeFreezer",true)).query()) / householdProsumers.size());
		 * System.out.println("HHs with UprightFreezer %: " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasUprightFreezer",true)).query()) / householdProsumers.size());
		 * System.out.println("HHs with ChestFreezer %: " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasChestFreezer",true)).query()) / householdProsumers.size()); }
		 */
	}

	private void initializeHHProsumersWetAppliancesPar4All()
	{

		IndexedIterable<Household> householdProsumers = myContext.getObjects(Household.class);

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
		for (Household thisAgent : householdProsumers)
		{
			int occupancy = thisAgent.getNumOccupants();
			double randomVar = RandomHelper.nextDouble();
			// if (Consts.DEBUG) System.out.println("randomVar: "+randomVar);
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

			thisAgent.setWetAppliancesProfiles(InitialProfileGenUtils.melodyStokesWetApplianceGen(this.myContext, Consts.NB_OF_DAYS_LOADED_DEMAND, thisAgent.hasWashingMachine, thisAgent.hasWasherDryer, thisAgent.hasDishWasher, thisAgent.hasTumbleDryer));
			/* JRS TEST - REMOVE REMOVE REMOVE */
			// setWetAppsPerPBMatlabPrototype(thisAgent);

		}

		/*
		 * if(myContext.verbose) { System.out.println("Percentages:");
		 * System.out.println("households with occupancy 1 : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "numOccupants",1)).query()) / householdProsumers.size());
		 * System.out.println("households with occupancy 2 : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "numOccupants",2)).query()) / householdProsumers.size());
		 * System.out.println("households with occupancy 3 : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "numOccupants",3)).query()) / householdProsumers.size());
		 * System.out.println("households with occupancy 4 : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "numOccupants",4)).query()) / householdProsumers.size());
		 * System.out.println("households with occupancy 5 : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "numOccupants",5)).query()) / householdProsumers.size());
		 * System.out.println("households with occupancy 6 : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "numOccupants",6)).query()) / householdProsumers.size());
		 * System.out.println("households with occupancy 7 : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "numOccupants",7)).query()) / householdProsumers.size());
		 * System.out.println("households with occupancy 8 : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "numOccupants",8)).query()) / householdProsumers.size());
		 * System.out.println("Washing Mach : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasWashingMachine",true)).query()) / householdProsumers.size());
		 * System.out.println("Washer Dryer : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasWasherDryer",true)).query()) / householdProsumers.size());
		 * System.out.println("Tumble Dryer: " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasTumbleDryer",true)).query()) / householdProsumers.size());
		 * System.out.println("Dish Washer : " + (double)
		 * IterableUtils.count((new PropertyEquals(myContext,
		 * "hasDishWasher",true)).query()) / householdProsumers.size()); }
		 */
	}

	/**
	 * This method initialize the probability distributions used in this model.
	 */
	private void initializeProbabilityDistributions()
	{
		System.out.println("Random seed is" + RandomHelper.getSeed());
		double[] drawOffDist = ArrayUtils.multiply(Consts.EST_DRAWOFF, ArrayUtils.sum(Consts.EST_DRAWOFF));
		// if (Consts.DEBUG) System.out.println("  ArrayUtils.sum(drawOffDist)"+
		// ArrayUtils.sum(drawOffDist));
		myContext.drawOffGenerator = RandomHelper.createEmpiricalWalker(drawOffDist, Empirical.NO_INTERPOLATION);
		// if (Consts.DEBUG)
		// System.out.println("  ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY)"+
		// ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY));

		myContext.occupancyGenerator = RandomHelper.createEmpiricalWalker(Consts.OCCUPANCY_PROBABILITY_ARRAY, Empirical.NO_INTERPOLATION);
		myContext.waterUsageGenerator = RandomHelper.createNormal(0, 1);

		myContext.buildingLossRateGenerator = RandomHelper.createNormal(275, 75);
		myContext.thermalMassGenerator = RandomHelper.createNormal(12.5, 2.5);

		myContext.coldAndWetApplTimeslotDelayRandDist = RandomHelper.createUniform();

		myContext.wetApplProbDistGenerator = RandomHelper.createEmpiricalWalker(Consts.WET_APPLIANCE_PDF, Empirical.NO_INTERPOLATION);

		myContext.journeyLengthGenerator = RandomHelper.createPoisson(Consts.MEAN_JOURNEY_LENGTH);
		myContext.vehicleArrivalGenerator = RandomHelper.createEmpiricalWalker(Consts.CAR_ARRIVAL_PROBABILITY, Empirical.NO_INTERPOLATION);
		// ChartUtils.testProbabilityDistAndShowHistogram(myContext.wetApplProbDistGenerator,
		// 10000, 48); //test to make sure the prob dist generate desired
		// outcomes

		// myContext.hhProsumerElasticityTest = RandomHelper.createBinomial(1,
		// 0.005);

	}

	/*
	 * private void initializeHHProsumersElecWaterHeat() {
	 * 
	 * //Iterable waterHeatedProsumersIter =
	 * myContext.getRandomObjects(Household.class, (long) (numProsumers *
	 * (Double) params.getValue("elecWaterFraction"))); Iterable
	 * waterHeatedProsumersIter = myContext.getRandomObjects(Household.class,
	 * (long) (numProsumers));
	 * 
	 * ArrayList prosumersWithElecWaterHeatList =
	 * IterableUtils.Iterable2ArrayList(waterHeatedProsumersIter);
	 * 
	 * //if (Consts.DEBUG) System.out.println("ArrayList.size: WaterHeat "+
	 * prosumersWithElecWaterHeatList.size());
	 * AgentUtils.assignParameterSingleValue("hasElectricalWaterHeat", true,
	 * prosumersWithElecWaterHeatList.iterator());
	 * 
	 * Iterator iter = prosumersWithElecWaterHeatList.iterator();
	 * 
	 * while (iter.hasNext()) { ((Household)
	 * iter.next()).initializeElectWaterHeatPar(); } }
	 */

	private void initializeWithoutGasHHProsumersElecSpaceHeat()
	{

		IndexedIterable<Household> spaceHeatedProsumersIter = myContext.getObjects(Household.class);

		for (Household hhPros : spaceHeatedProsumersIter)
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

		IndexedIterable<Household> spaceHeatedProsumersIter = myContext.getObjects(Household.class);

		for (Household hhPros : spaceHeatedProsumersIter)
		{
			if (!hhPros.isHasGas())
			{
				hhPros.setHasElectricalWaterHeat(true);
				hhPros.initializeElectWaterHeatPar();

			}
		}
	}

	private void populateContext()
	{
		/*
		 * TODO: NEED TO CREATE A MORE GENERIC SETUP STRUCTURE FOR ALL NETWORKS,
		 * AGGREGATORS, PROSUMERS, ETC..
		 */

		;
		// createHouseholdProsumersAndAddThemToContext(2); //pass in parameter
		// nb of occupants, or random
		// createHouseholdProsumersAndAddThemToContext(Consts.RANDOM);

		// TODO: Add method that creates wind farm generators and adds them to
		// the context..

		// @TODO: these 4 methods below will eventually be included in the first
		// method (createHousholdPro...)
		if (Consts.HHPRO_HAS_WET_APPL)
			initializeHHProsumersWetAppliancesPar4All();

		if (Consts.HHPRO_HAS_COLD_APPL)
			initializeHHProsumersColdAppliancesPar4All();

		if (Consts.HHPRO_HAS_ELEC_WATER_HEAT)
			initializeWithoutGasHHProsumersElecWaterHeat();

		if (Consts.HHPRO_HAS_ELEC_SPACE_HEAT)
			initializeWithoutGasHHProsumersElecSpaceHeat();

		buildSocialNetwork();

		SupplierCoAdvancedModel firstRecoAggregator = new SupplierCoAdvancedModel(myContext);
		myContext.add(firstRecoAggregator);

		/**
		 * (02/07/12) DF
		 * 
		 * Add a simple single non domestic aggregator into
		 * <code>myContext</code>
		 */
		// SingleNonDomesticAggregator singleNonDomestic =
		// aggregatorFactory.createSingleNonDomesticAggregator(myContext.systemPriceSignalDataArray);
		// myContext.add(singleNonDomestic);

		buildOtherNetworks(firstRecoAggregator);

	}

	/**
	 * This method will build all the networks TODO: This method will need to be
	 * refined later At this moment, there is only one aggregator and links are
	 * simply created between this aggregator and all the prosumers in the
	 * context. Later this method (or its breakup(s)) can receive parameters
	 * such as EdgeSource and EdgeTarget to create edges between a source and a
	 * target
	 */
	private void buildOtherNetworks(AggregatorAgent firstAggregator)
	{
		boolean directed = true;

		// Create the household social network before other agent types are
		// added to the context.
		NetworkFactory networkFactory = NetworkFactoryFinder.createNetworkFactory(null);

		// Create null networks for other than social at this point.

		// Economic network should be hierarchical aggregator to prosumer
		// TODO: Decide what economic network between aggregators looks like?
		// TODO: i.e. what is market design for aggregators?
		Network economicNet = networkFactory.createNetwork("economicNetwork", myContext, directed);

		// TODO: replace this with something better. Next iteration of code
		// should probably take network design from a file
		for (ProsumerAgent prAgent : (Iterable<ProsumerAgent>) (myContext.getObjects(ProsumerAgent.class)))
		{
			economicNet.addEdge(firstAggregator, prAgent);
		}

		this.myContext.setEconomicNetwork(economicNet);

		// We should create a bespoke network for the electrical networks.
		// ProsumerAgents only - edges should have nominal voltage and capacity
		// attributes. TODO: How do we deal with transformers??
		Network physicalNet = networkFactory.createNetwork("electricalNetwork", myContext, directed);
		// TODO: How does info network differ from economic network?
		Network infoNet = networkFactory.createNetwork("infoNetwork", myContext, directed);

		for (ProsumerAgent prAgent : (Iterable<ProsumerAgent>) (myContext.getObjects(ProsumerAgent.class)))
		{
			infoNet.addEdge(firstAggregator, prAgent);
		}
	}

	/**
	 * This method will builds the social networks TODO: This method will need
	 * to be refined later At this moment, there is only one aggregator and
	 * links are simply created between this aggregator and all the prosumers in
	 * the context. Later this method (or its breakup(s)) can receive parameters
	 * such as EdgeSource and EdgeTarget to create edges between a source and a
	 * target
	 */
	private void buildSocialNetwork()
	{

		// Create the household social network before other agent types are
		// added to the context.
		NetworkFactory networkFactory = NetworkFactoryFinder.createNetworkFactory(null);
		// create a small world social network
		double beta = 0.1;
		int degree = 2;
		boolean directed = true;
		boolean symmetric = true;
		NetworkGenerator gen = new WattsBetaSmallWorldGenerator(beta, degree, symmetric);
		Network socialNet = networkFactory.createNetwork("socialNetwork", myContext, gen, directed);
		// set weight of each social contact - initially random
		// this will represent the influence a contact may have on another
		// Note that influence of x on y may not be same as y on x - which is
		// realistic

		for (Object thisEdge : socialNet.getEdges())
		{
			((RepastEdge) thisEdge).setWeight(RandomHelper.nextDouble());
		}

		/*
		 * ----- Richard test block to fully connect a certain number of agents
		 * based on a property (in this case DEFRA category
		 */
		Query<Household> cat1Query = new PropertyEquals(myContext, "defraCategory", 1);
		Iterable<Household> cat1Agents = cat1Query.query();

		for (Household prAgent : cat1Agents)
		{
			for (Household target : cat1Agents)
			{
				socialNet.addEdge(prAgent, target, Consts.COMMON_INTEREST_GROUP_EDGE_WEIGHT);
			}
		}

		// Add in some generators

		this.myContext.setSocialNetwork(socialNet);
	}

	Household createHouseholdProsumer(WeakHashMap <Integer, double[]> map_nbOfOcc2OtherDemand, int occupancyModeOrNb, boolean addNoise, boolean hasGas) {
		
		int numOfOccupant = occupancyModeOrNb;
		if (occupancyModeOrNb == Consts.RANDOM) {
			numOfOccupant = myContext.occupancyGenerator.nextInt() + 1;
			if (numOfOccupant > map_nbOfOcc2OtherDemand.size())
				numOfOccupant = map_nbOfOcc2OtherDemand.size();
		}
		double[] arr_otherDemand=null;
		if (false){//myContext.signalMode == Consts.SIGNAL_MODE_SMART) {
		    /*
		     * If the signal is smart, we use one single profile, currently the one with 2 occupants.
		     * If required, instead we can get find the average of all (different occupancy) and use it instead.
		     * Furthermore, if required, the size of this array should be reduced to one single day (currently left at one year) 
		     */
			arr_otherDemand = map_nbOfOcc2OtherDemand.get(2);
		}
		else arr_otherDemand = map_nbOfOcc2OtherDemand.get(numOfOccupant);
		
		return createHouseholdProsumer(arr_otherDemand,  numOfOccupant, addNoise, hasGas);
		
	}

	/**
	 * Creates a household prosumer with a basic consumption profile as supplied
	 * with or without added noise
	 *
	 * NOTE: The addNoise option is unsafe as implemented - under a certain combination of random factors, it can give negative
	 * demands!! DO NOT USE
	 *
	 * @param otherElasticDemandProfile - an array of the basic consumption profile for this prosumer (kWh per tick)
	 * @param addNoise - boolean specifying whether or not to add noise to the profile
	 * @param numOfOccupants - number of occupancy per household, if Consts.RANDOM is passed, it will done randomly  
	 */
	Household createHouseholdProsumer(double[] otherDemandProfileArray,  int numOfOccupants, boolean addNoise, boolean hasGas) {
		
		Household hhProsAgent;
	
		
		int ticksPerDay = myContext.getNbOfTickPerDay();
		
		if (otherDemandProfileArray.length % ticksPerDay != 0)	{
			System.err.println("ProsumerFactory: Household base demand array not a whole number of days");
			System.err.println("ProsumerFactory: May cause unexpected behaviour");
		}
	
		if (addNoise)		
			hhProsAgent = new Household(myContext, randomizeDemandProfile(otherDemandProfileArray));
		else 		
			hhProsAgent = new Household(myContext, otherDemandProfileArray);
	
		hhProsAgent.setNumOccupants(numOfOccupants);
		
		hhProsAgent.setHasGas(hasGas);
		
		/*if (hasGas) {
			hhProsAgent.setHasElectricalSpaceHeat(false);
		}
		else {
			hhProsAgent.setHasElectricalSpaceHeat(true);
			hhProsAgent.initializeElecSpaceHeatPar();
		} */
			
		hhProsAgent.costThreshold = Consts.HOUSEHOLD_COST_THRESHOLD;
		hhProsAgent.setPredictedCostSignal(Consts.ZERO_COST_SIGNAL);
	
		hhProsAgent.transmitPropensitySmartControl = (double) RandomHelper.nextDouble();
	
		hhProsAgent.initializeRandomlyDailyElasticityArray(0, 0.1);
		
		//hhProsAgent.initializeSimilarlyDailyElasticityArray(0.1d);
		hhProsAgent.setRandomlyPercentageMoveableDemand(0, Consts.MAX_DOMESTIC_MOVEABLE_LOAD_FRACTION);
		
		hhProsAgent.exercisesBehaviourChange = false;
		//pAgent.exercisesBehaviourChange = (RandomHelper.nextDouble() > (1 - Consts.HOUSEHOLDS_WILLING_TO_CHANGE_BEHAVIOUR));
		
		//TODO: We just set smart meter true here - need more sophisticated way to set for different scenarios
		hhProsAgent.hasSmartMeter = true;
	
		//pAgent.hasSmartControl = (RandomHelper.nextDouble() > (1 - Consts.HOUSEHOLDS_WITH_SMART_CONTROL));
		//Alter initial smartControl here
		hhProsAgent.hasSmartControl = false;
		
		//TODO: we need to set up wattbox after appliances added.  This is all a bit
		//non-object oriented.  Could do with a proper design methodology here.
		if (hhProsAgent.hasSmartControl)
			hhProsAgent.setWattboxController();
	
		return hhProsAgent;
	}

	/**
	 * Adds a random noise to a profile.
	 * For amplitude multiplies each point on the base profile by a random double uniformly distributed between 0.7 and 1.3 (arbitrary)
	 * then selects a uniformly distributed time based <code> jitterFactor </code> between -0.5 and + 0.5 and shifts the demand in time by <code> jitterFactor </code> timesteps
	 *
	 * NOTE: This is unsafe as implemented - under a certain combination of random factors, it can give negative
	 * demands!! DO NOT USE
	 *
	 * TODO: It should be over-ridden in the future to use something better - for instance melody's model
	 * or something which time-shifts demand somewhat, or select one of a number of typical profiles
	 * based on occupancy.
	 */
	private double[] randomizeDemandProfile(double[] demandProfileArray){
		double[] newProfile = new double[demandProfileArray.length];
	
		//add amplitude randomisation
		for (int i = 0; i < newProfile.length; i++)
		{
			newProfile[i] = demandProfileArray[i] * (double)(1 + 0.3*(RandomHelper.nextDouble() - 0.5));
		}
	
		//add time jitter
		double jitterFactor = RandomHelper.nextDouble() - 0.5d;
	
		//if (Consts.DEBUG) System.out.println("ProsumerFactory: Applying jitter" + jitterFactor);
	
		newProfile[0] = (jitterFactor * newProfile[0]) + ((1 - jitterFactor) * newProfile[newProfile.length - 1]);
		for (int i = 1; i < (newProfile.length - 1); i++)
		{
			newProfile[i] = (jitterFactor * newProfile[i]) + ((1 - jitterFactor) * newProfile[i+1]);
		}
		newProfile[newProfile.length - 1] = (jitterFactor * newProfile[newProfile.length - 1]) + ((1 - jitterFactor) * newProfile[0]);
	
		return newProfile;
	}
}
