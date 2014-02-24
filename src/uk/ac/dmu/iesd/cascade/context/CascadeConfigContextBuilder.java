/**
 * 
 */
package uk.ac.dmu.iesd.cascade.context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.WeakHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent;
import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorFactory;
import uk.ac.dmu.iesd.cascade.agents.aggregators.BMPxTraderAggregator;
import uk.ac.dmu.iesd.cascade.agents.aggregators.PassThroughAggregatorWithLag;
import uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCo;
import uk.ac.dmu.iesd.cascade.agents.aggregators.SupplierCoAdvancedModel;
import uk.ac.dmu.iesd.cascade.agents.prosumers.HouseholdProsumer;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerAgent;
import uk.ac.dmu.iesd.cascade.agents.prosumers.ProsumerFactory;
import uk.ac.dmu.iesd.cascade.base.Consts;
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
 * Implementation of the Repast ContextBuilder object designed to take input from a CACADE framework
 * context file and build the context according to that specification.
 * 
 * Currently v0.1 - lots of strong assumptions about users' knowledge of the internals of agent implementations
 * 
 * TODO: Implement class level annotation for configurable variables and distributions
 * TODO: Ensure generic across all implementations of {@link ProsumerAgent} and {@link AggregatorAgent}
 * 
 * @author jsnape
 * 
 */
public class CascadeConfigContextBuilder implements ContextBuilder<Object>
{
	private Parameters params;
	private CascadeContext cascadeMainContext; // cascade main context
	private File dataDirectory;
	private SystemOperator sysOp;
	private MarketMessageBoard messageBoard;
	private File configFile;
	private WeakHashMap<String, double[]> mapOfTypeName2BaseProfileArray;
	private WeakHashMap<Integer, double[]> map_nbOfOccToOtherDemand;
	ProsumerFactory proFactory;
	Network economicNet;
	AggregatorFactory bmuFactory;
	Document doc;

	/***
	 * Implementation of a Context builder, taking both parameters from the GUI
	 * as specified in standard Repast Simphony operation and a
	 * CascadeConfig.xml file to populate agents into the context.
	 * 
	 * @see repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context.Context)
	 */
	@Override
	public Context build(Context<Object> context)
	{
		cascadeMainContext = new CascadeContext(context); //build CascadeContext by passing the context
		readParamsAndInitializeArrays();
		// XMLReader myR = readConfigFile(configFile);
		// decodeConfigFile();
		initializeProbabilityDistributions(); // This initializes default values - can be overridden in the population
		
		readConfigFile();
		
		//check whether to build market.  Default is to build it, unless explicitly specified not to.

		NodeList marketElements = this.doc.getDocumentElement().getElementsByTagName("market");
		if (marketElements.getLength() == 0)
		{
			System.out.println("No market element found, so building market as default");
				buildMarket();
		}
		else
		{
			 // TODO: should we handle multiple markets?
			System.out.println("Market element value : " + marketElements.item(0).getChildNodes().item(0).getNodeValue());
			if (!marketElements.item(0).getChildNodes().item(0).getNodeValue().equals("false"))
			{
				buildMarket();
			}
		}


		populateContext();

		// TODO Auto-generated method stub
		return cascadeMainContext;
	}

	/**
	 * 
	 */
	private void readConfigFile()
	{
		try
		{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			this.doc = docBuilder.parse(this.configFile);

			// normalize text representation
			this.doc.getDocumentElement().normalize();

		} catch (SAXParseException err)
		{
			System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println(" " + err.getMessage());

		} catch (SAXException e)
		{
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();

		}catch (ParserConfigurationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
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
		cascadeMainContext.drawOffGenerator = RandomHelper.createEmpiricalWalker(drawOffDist, Empirical.NO_INTERPOLATION);
		// if (Consts.DEBUG)
		// System.out.println("  ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY)"+
		// ArrayUtils.sum(Consts.OCCUPANCY_PROBABILITY_ARRAY));

		cascadeMainContext.occupancyGenerator = RandomHelper.createEmpiricalWalker(Consts.OCCUPANCY_PROBABILITY_ARRAY, Empirical.NO_INTERPOLATION);
		cascadeMainContext.waterUsageGenerator = RandomHelper.createNormal(0, 1);

		cascadeMainContext.buildingLossRateGenerator = RandomHelper.createNormal(275, 75);
		cascadeMainContext.thermalMassGenerator = RandomHelper.createNormal(12.5, 2.5);

		cascadeMainContext.coldAndWetApplTimeslotDelayRandDist = RandomHelper.createUniform();

		cascadeMainContext.wetApplProbDistGenerator = RandomHelper.createEmpiricalWalker(Consts.WET_APPLIANCE_PDF, Empirical.NO_INTERPOLATION);

		// ChartUtils.testProbabilityDistAndShowHistogram(cascadeMainContext.wetApplProbDistGenerator,
		// 10000, 48); //test to make sure the prob dist generate desired
		// outcomes

		// cascadeMainContext.hhProsumerElasticityTest =
		// RandomHelper.createBinomial(1, 0.005);

	}

	private void buildMarket()
	{

		SettlementCompany settlementCo = new SettlementCompany(cascadeMainContext);
		cascadeMainContext.add(settlementCo);

		messageBoard = new MarketMessageBoard();

		sysOp = new SystemOperator(cascadeMainContext, settlementCo, messageBoard);
		cascadeMainContext.add(sysOp);

		PowerExchange pEx = new PowerExchange(cascadeMainContext, messageBoard);
		cascadeMainContext.add(pEx);

	}

	private void populateContext()
	{
		bmuFactory = new AggregatorFactory(cascadeMainContext, this.messageBoard);
		proFactory = new ProsumerFactory(cascadeMainContext);
		NetworkFactory networkFactory = NetworkFactoryFinder.createNetworkFactory(null); // unclear
																							// what
																							// the
																							// hints
																							// map
																							// is
																							// for
																							// in
																							// this
																							// call
																							// -
																							// so
																							// leave
																							// it
																							// null
		economicNet = networkFactory.createNetwork("economicNetwork", cascadeMainContext, true);
		this.cascadeMainContext.setEconomicNetwork(economicNet);
		
		readGenericAggBaseProfileFiles();

		Element configRoot = doc.getDocumentElement();

			NodeList aggregators = configRoot.getElementsByTagName("aggregator");

			for (int i = 0; i < aggregators.getLength(); i++)
			{
				Element a = (Element) aggregators.item(i); // cast is safe due
															// to how we
															// retrieved
															// aggregators list
				String className = a.getAttribute("class");
				Class thisAgg;
				try
				{
					thisAgg = Class.forName(className);

				if (AggregatorAgent.class.isAssignableFrom(thisAgg))
				{
					String type = a.getAttribute("type");
					int num = Integer.valueOf(a.getAttribute("number"));
					

					for (int ii = 0; ii < num; ii++)
					{
					// We can instantiate this Aggregator and carry on
					String shortName = thisAgg.getSimpleName();
					System.err.println("Adding " + num + " of class " + shortName);
					if (shortName.equals("GenericBMPxTraderAggregator"))
					{
						System.err.println("Adding a GenericBMPx aggregator, type " + type);
						BMPxTraderAggregator bmu = bmuFactory.createGenericBMPxTraderAggregator(Consts.BMU_TYPE.valueOf(type), (double[]) mapOfTypeName2BaseProfileArray.get(type));
						cascadeMainContext.add(bmu);
					} 
					else if (shortName.equals("SupplierCoAdvancedModel"))
					{
						SupplierCoAdvancedModel suppCo = bmuFactory.createSupplierCoAdvanced(cascadeMainContext.systemPriceSignalDataArray);
						cascadeMainContext.add(suppCo);
						addHouseholdsToAggregator(a, suppCo);
	 
					} 
					else if (shortName.equals("SupplierCo"))
					{
						SupplierCo suppCo = bmuFactory.createSupplierCo(cascadeMainContext.systemPriceSignalDataArray);
						cascadeMainContext.add(suppCo);
						addHouseholdsToAggregator(a, suppCo);
					} 
					else if (shortName.equals("WindFarmAggregator"))
					{
					}
					else if (shortName.equals("PassThroughAggregatorWithLag"))
					{					
						double[] flatBaseline = new double[48];
						Arrays.fill(flatBaseline,ASTEMConsts.BMU_SMALLDEM_MINDEM*1.1);
						PassThroughAggregatorWithLag a1 = new PassThroughAggregatorWithLag(cascadeMainContext,messageBoard, ASTEMConsts.BMU_SMALLDEM_MAXDEM, ASTEMConsts.BMU_SMALLDEM_MINDEM, flatBaseline,48);
						addHouseholdsToAggregator(a, a1);
						this.economicNet.addProjectionListener(a1); //Testing a feature whereby aggregators listen to changes in their network TODO: Keep?
					}
					else
					{
						//Try for a basic invocation with the context only
						try
						{
							AggregatorAgent a1 = (AggregatorAgent) thisAgg.getConstructor(CascadeContext.class).newInstance(this.cascadeMainContext);
							addProsumersToAggregator(a,a1);
						} catch (Exception e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
					}
					}
				} else
				{
					System.err.println("Config file specifies an aggregator with a class that is not an aggregator agent : " + className);
				}
			} catch (ClassNotFoundException e1)
			{
				System.err.println("Class " + className + " in the config file is not available to the classloader");
				System.err.println("Building population from file will continue, but won't populate agents of this class");
				e1.printStackTrace();
			}
			}
		

	}
	
	/**
	 * @param a
	 * @param a1
	 */
	private void addProsumersToAggregator(Element a, AggregatorAgent a1)
	{
		System.err.println("In here with " + a1.getAgentName());
		NodeList prosumers = a.getElementsByTagName("prosumer");
		for (int j = 0; j < prosumers.getLength(); j++)
		{
			Element p = (Element) prosumers.item(j);
			String className = p.getAttribute("class");
			Class thisAgg;
			try
			{
				thisAgg = Class.forName(className);
				int num = Integer.parseInt(p.getAttribute("number"));
				for (int i = 0; i < num; i++)
				{
				ProsumerAgent p1 = (ProsumerAgent) thisAgg.getConstructor(CascadeContext.class).newInstance(this.cascadeMainContext);
		
				this.economicNet.addEdge(a1,p1);
				}
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

	private void addHouseholdsToAggregator(Element a, AggregatorAgent suppCo)
	{
		System.err.println("In here with " + suppCo.getAgentName());
		NodeList prosumers = a.getElementsByTagName("prosumer");
		for (int j = 0; j < prosumers.getLength(); j++)
		{
			Element p = (Element) prosumers.item(j);
			int numHH = Integer.valueOf(p.getAttribute("number"));
			System.err.println("Adding " + numHH + " households");
			int occNum = Consts.RANDOM;
			int gasPercentage = 0;
			if (p.hasChildNodes())
			{
				//Alter the prosumer generation distributions based on this
				// At the moment - relies on a well formed XML file - only specifying these for households
				double bLMean = Double.parseDouble(p.getElementsByTagName("lossRateMean").item(0).getChildNodes().item(0).getNodeValue());
				double bLSD = Double.parseDouble(p.getElementsByTagName("lossRateSD").item(0).getChildNodes().item(0).getNodeValue());
				double tMMean = Double.parseDouble(p.getElementsByTagName("thermalMassMean").item(0).getChildNodes().item(0).getNodeValue());
				double tMSD = Double.parseDouble(p.getElementsByTagName("thermalMassSD").item(0).getChildNodes().item(0).getNodeValue());
				NodeList fixedOccupancy = p.getElementsByTagName("occupants");
				
				if (fixedOccupancy.getLength() == 1)
				{
					occNum = Integer.parseInt(fixedOccupancy.item(0).getChildNodes().item(0).getNodeValue());
				}		
				NodeList gas = p.getElementsByTagName("percentageGas");
				if (gas.getLength() == 1)
				{
					gasPercentage =  Integer.parseInt(gas.item(0).getChildNodes().item(0).getNodeValue());
				}
				cascadeMainContext.buildingLossRateGenerator = RandomHelper.createNormal(bLMean, bLSD);
				cascadeMainContext.thermalMassGenerator = RandomHelper.createNormal(tMMean, tMSD);
				
				//cascadeMainContext.occupancyGenerator = RandomHelper.createEmpiricalWalker(Consts.OCCUPANCY_PROBABILITY_ARRAY, Empirical.NO_INTERPOLATION);
			}
			
			int numGas = gasPercentage * numHH / 100;
			boolean wGas = true;
			for (int k = 0; k < numHH; k++)
			{
				if (k >= numGas)
				{
					wGas = false;
				}
				HouseholdProsumer hhProsAgent = proFactory.createHouseholdProsumer(map_nbOfOccToOtherDemand, occNum, true, wGas);
				cascadeMainContext.add(hhProsAgent);
				economicNet.addEdge(suppCo, hhProsAgent);
			}			
			
			if (Consts.HHPRO_HAS_WET_APPL)
				initializeHHProsumersWetAppliancesPar4All();

			if (Consts.HHPRO_HAS_COLD_APPL)
				initializeHHProsumersColdAppliancesPar4All();

			if (Consts.HHPRO_HAS_ELEC_WATER_HEAT)
				initializeWithoutGasHHProsumersElecWaterHeat();

			if (Consts.HHPRO_HAS_ELEC_SPACE_HEAT)
				initializeWithoutGasHHProsumersElecSpaceHeat();
			
		}
	}

	private void readGenericAggBaseProfileFiles()
	{
		// String currentDirectory = System.getProperty("user.dir"); //this
		// suppose to be the Eclipse project working space
		// String pathToDataFiles =
		// currentDirectory+ASTEMConsts.DATA_FILES_FOLDER_NAME;
		// File parentDataFilesDirectory = new File(pathToDataFiles);
		// File dmu_BaseProfiles_File = new File(parentDataFilesDirectory,
		// ASTEMConsts.BMU_BASE_PROFILES_FILENAME);

		File dmu_BaseProfiles_File = new File(dataDirectory, ASTEMConsts.BMU_BASE_PROFILES_FILENAME);

		this.mapOfTypeName2BaseProfileArray = new WeakHashMap<String, double[]>();

		try
		{
			CSVReader baseProfileCSVReader = new CSVReader(dmu_BaseProfiles_File);
			System.out.println("baseProfileCSVReader created");
			baseProfileCSVReader.parseByColumn();

			mapOfTypeName2BaseProfileArray.put("DEM_LARGE", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("DEM_LARGE")));
			mapOfTypeName2BaseProfileArray.put("DEM_SMALL", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("DEM_SMALL")));
			mapOfTypeName2BaseProfileArray.put("GEN_COAL", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("GEN_COAL")));
			mapOfTypeName2BaseProfileArray.put("GEN_CCGT", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("GEN_CCGT")));
			mapOfTypeName2BaseProfileArray.put("GEN_WIND", ArrayUtils.convertStringArrayToDoubleArray(baseProfileCSVReader.getColumn("GEN_WIND")));

		} catch (FileNotFoundException e)
		{
			System.err.println("File not found: " + dmu_BaseProfiles_File.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
	}
	
	private void initializeElectricVehicles()
	{
	IndexedIterable<HouseholdProsumer> householdProsumers = cascadeMainContext.getObjects(HouseholdProsumer.class);

	for (HouseholdProsumer thisAgent : householdProsumers)
	{
		if (RandomHelper.nextDouble() < 0.25)
		{
			double [] EVprofile;
			if (RandomHelper.nextDouble() < 0.5)
			{
				EVprofile = EVProfileGenerator.generateBEVProfile(cascadeMainContext,Consts.NB_OF_DAYS_LOADED_DEMAND);
			}
			else
			{
				EVprofile = EVProfileGenerator.generatePHEVProfile(cascadeMainContext,Consts.NB_OF_DAYS_LOADED_DEMAND);
			}
			thisAgent.hasElectricVehicle=true;
			thisAgent.setEVProfile(EVprofile);
			

		}
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
	

	/*
	 * Read the model environment parameters and initialize arrays
	 */
	private void readParamsAndInitializeArrays()
	{
		// get the parameters from the current run environment
		params = RunEnvironment.getInstance().getParameters();
		String dataFileFolderPath = (String) params.getValue("dataFileFolder");
		
		System.err.println("Path is" + dataFileFolderPath);
		dataDirectory = new File(dataFileFolderPath);

		String weatherFileName = (String) params.getValue("weatherFile");
		String systemDemandFileName = (String) params.getValue("systemBaseDemandFile");
		String householdOtherDemandFilename = (String) params.getValue("householdOtherDemandFile");
		// String elecLayoutFilename =
		// (String)params.getValue("electricalNetworkLayoutFile");
		// numProsumers = (Integer)
		// params.getValue("defaultProsumersPerFeeder");
		// percentageOfHHProsWithGas = (Integer)
		// params.getValue("hhWithGasPercentage");
		this.configFile = new File(dataDirectory, ((String) params.getValueAsString("configFileName")));
		// cascadeMainContext.setTotalNbOfProsumers(numProsumers);
		int ticksPerDay = (Integer) params.getValue("ticksPerDay");
		cascadeMainContext.setNbOfTickPerDay(ticksPerDay);
		cascadeMainContext.verbose = (Boolean) params.getValue("verboseOutput");
		cascadeMainContext.chartSnapshotOn = (Boolean) params.getValue("chartSnapshot");
		cascadeMainContext.setChartSnapshotInterval((Integer) params.getValue("chartSnapshotInterval"));
		cascadeMainContext.signalMode = (Integer) params.getValue("signalMode");

		cascadeMainContext.setRandomSeedValue((Integer) params.getValue("randomSeed"));

		// RunEnvironment.getInstance().
		Date startDate;
		try
		{
			startDate = (new SimpleDateFormat("dd/MM/yyyy")).parse((String) params.getValue("startDate"));
		} catch (ParseException e1)
		{
			// TODO Auto-generated catch block
			System.err.println("CascadeContextBuilder: The start date parameter is in a format which cannot be parsed by this program");
			System.err.println("CascadeContextBuilder: The data will be set to 01/01/2000 by default");
			startDate = new Date(2000, 1, 1);
			e1.printStackTrace();
		}
		cascadeMainContext.simulationCalendar = new GregorianCalendar();
		cascadeMainContext.simulationCalendar.setTime(startDate);

		/*
		 * Read in the necessary data files and store to the context
		 */

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

			cascadeMainContext.insolationArray = Arrays.copyOf(insolationArray_all, lengthOfProfileArrays);
			cascadeMainContext.windSpeedArray = Arrays.copyOf(windSpeedArray_all, lengthOfProfileArrays);
			cascadeMainContext.airTemperatureArray = Arrays.copyOf(airTemperatureArray_all, lengthOfProfileArrays);
			cascadeMainContext.airDensityArray = Arrays.copyOf(airDensityArray_all, airDensityArray_all.length);

			cascadeMainContext.weatherDataLength = lengthOfProfileArrays;

		} catch (FileNotFoundException e)
		{
			System.err.println("Could not find file with name " + weatherFile.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (cascadeMainContext.weatherDataLength % cascadeMainContext.getNbOfTickPerDay() != 0)
		{
			System.err.println("Weather data array not a whole number of days. This may cause unexpected behaviour ");
		}

		try
		{
			systemBasePriceReader = new CSVReader(systemDemandFile);
			systemBasePriceReader.parseByColumn();

			double[] systemBasePriceSignal = ArrayUtils.convertStringArrayToDoubleArray(systemBasePriceReader.getColumn("demand"));
			cascadeMainContext.systemPriceSignalDataArray = Arrays.copyOf(systemBasePriceSignal, lengthOfProfileArrays);
			cascadeMainContext.systemPriceSignalDataLength = systemBasePriceSignal.length;

		} catch (FileNotFoundException e)
		{
			System.err.println("Could not find file with name " + householdOtherDemandFilename);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (cascadeMainContext.systemPriceSignalDataLength % cascadeMainContext.getNbOfTickPerDay() != 0)
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

		// this.monthlyMainsWaterTemp =
		// Arrays.copyOf(Consts.MONTHLY_MAINS_WATER_TEMP,
		// Consts.MONTHLY_MAINS_WATER_TEMP.length);
	}
	


}
