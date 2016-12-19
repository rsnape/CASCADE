package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualization.IDisplay;
import repast.simphony.visualizationOGL2D.DisplayOGL2D;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.controllers.ISmartController;
import uk.ac.dmu.iesd.cascade.controllers.ProportionalWattboxController;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.4 $ $Date: 2012/01/23 $
 * 
 *          Version history (for intermediate steps see Git repository history)
 * 
 *          1.0 - Initial split of prosumer implementations from the abstract
 *          class representing all prosumers 1.1 - 1.2 - add in more
 *          sophisticated model incorporating displaceable and non-displaceable
 *          demand, appliance ownership etc. (Richard) 1.3 - added heat pump
 *          model and temperature calculation model (Richard) 1.4 - modified and
 *          added the parts that calculate the combined wet and cold profiles
 *          demand which was previously only based on single profile (Babak) 1.5
 *          - code cleanup and documentation (Richard)
 */
public class HouseholdProsumer extends ProsumerAgent
{

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */

	private int lengthOfDemandProfile;

	boolean hasCHP = false;
	boolean hasAirSourceHeatPump = false;
	boolean hasGroundSourceHeatPump = false;
	boolean hasWind = false;
	boolean hasHydro = false;
	// Thermal Generation included so that Household can represent
	// Biomass, nuclear or fossil fuel generation in the future
	// what do we think?
	boolean hasThermalGeneration = false;
	public boolean hasPV = false;
	//TODO: refactor so that we have more object orientation - e.g. Generator interface,
	// inherited to MicroGenerator, RenewableGenerator, ThermalGenerator...
	// implemented as e.g. PVSystem, WindTurbine, NuclearUnit etc.
	// Have prosumers own these.
	int pv_orientation = 0; //Orientation of building in degrees from North.  Pertinent for PV
	boolean hasSolarWaterHeat = false;
	private boolean hasElectricalWaterHeat = false;
	boolean hasElectricalSpaceHeat = false;
	public boolean hasElectricVehicle = false;
	boolean hasElectricalStorage = false; // Do we need to break out various
											// storage technologies?
	boolean hasHotWaterStorage = false;
	boolean hasSpaceHeatStorage = false;

	private boolean hasGas = true;

	/*
	 * the rated power of the various technologies / appliances we are
	 * interested in
	 * 
	 * Do not initialise these by default. They should be initialised when an
	 * instantiated agent is given the boolean attribute which means that they
	 * have access to one of these technologies.
	 */
	double ratedPowerCHP;
	double ratedPowerAirSourceHeatPump;
	double ratedPowerGroundSourceHeatPump;
	double ratedPowerWind;
	double ratedPowerHydro;
	double ratedPowerThermalGeneration;
	public double ratedPowerPV;
	double ratedPowerSolarWaterHeat;
	public double ratedPowerHeatPump;
	double ratedCapacityElectricVehicle; // Note kWh rather than kW
	double ratedCapacityElectricalStorage; // Note kWh rather than kW
	double ratedCapacityHotWaterStorage; // Note kWh rather than kW
	private double dailyHotWaterUsage;

	double ratedCapacitySpaceHeatStorage; // Note - related to thermal mass

	/*
	 * Appliance ownership parameters. All default to false - should be set
	 * whilst building the scenario
	 */
	public boolean hasFridgeFreezer = false;
	public boolean hasRefrigerator = false;
	public boolean hasUprightFreezer = false;
	public boolean hasChestFreezer = false;
	public boolean hasWashingMachine = false;
	public boolean hasWasherDryer = false;
	public boolean hasTumbleDryer = false;
	public boolean hasDishWasher = false;

	// For Households' heating requirements
	/**
	 * Thermal mass of building (expressed in kWh per deg C)
	 */
	protected double buildingThermalMass;
	/**
	 * Heat loss rate for the building (expressed in Watts per degree C, or
	 * Joules per second per deg C)
	 */
	protected double buildingHeatLossRate;

	public double getBuildingHeatLossRate()
	{
		return this.buildingHeatLossRate;
	}

	public double getBuildingThermalMass()
	{
		return this.buildingThermalMass;
	}

	/*
	 * temperature control parameters
	 */
	double[] setPointProfile;
	double[] optimisedSetPointProfile;
	double setPoint;
	double minSetPoint; // The minimum temperature for this Household's building
						// in centigrade (where relevant)
	double maxSetPoint; // The maximum temperature for this Household's building
						// in centigrade (where relevant)
	public double waterSetPoint;
	protected double currentInternalTemp;
	double currentWaterTemp;

	// Occupancy information
	private int numOccupants;
	int numAdults;
	int numChildren;
	int numTeenagers;
	int[] occupancyProfile;

	/*
	 * Specifically, a household may have a certain percentage of demand that it
	 * believes is moveable and / or a certain maximum time shift of demand
	 */
	double percentageMoveableDemand; // This can be set constant, or calculated
										// from available appliances
	int maxTimeShift; // The "furthest" a demand may be moved in time. This can
						// be constant or calculated dynamically.
	/*
	 * Behavioural properties
	 */
	// Learner adoptSmartMeterLearner;
	// Learner adoptSmartControlLearner;
	// Learner consumptionPatternLearner;
	public double transmitPropensitySmartControl;
	double transmitPropensityProEnvironmental;
	double visibilityMicrogen;

	/*
	 * This may or may not be used, but is a threshold cost above which actions
	 * take place for the household
	 */
	public double costThreshold;

	/**
	 * Richard hack to get DEFRA profiles going
	 */
	double microgenPropensity;
	double insulationPropensity;
	double HEMSPropensity;
	public double EVPropensity;
	public double habit;
	public int defraCategory;
	public double[] spaceHeatPumpOn;
	private boolean waterHeaterOn;

	/**
	 * Simulation variables needed throughout
	 */
	int time;
	int timeOfDay;
	private double[] dailyElasticity;

	/**
	 * Available smart devices
	 */
	ISmartController mySmartController;
	WeakHashMap<String, double[]> currentSmartProfiles;

	protected double[] coldApplianceProfile;
	protected WeakHashMap<String, double[]> coldApplianceProfiles;

	public double[] wetApplianceProfile;
	protected WeakHashMap<String, double[]> wetApplianceProfiles;

	private double[] baselineHotWaterVolumeProfile;
	private double[] waterHeatEnergyDemandProfile;
	private double[] waterHeatElectricalDemandProfile;

	// For ease of access to a debug type outputter
	CSVWriter sampleOutput;
	private double[] recordedHeatPumpDemand;

	// Arrays for the day's history (mainly for GUI - may use, write to file or
	// something else in the future)
	private double[] historicalBaseDemand;
	private double[] historicalColdDemand;
	private double[] historicalWetDemand;
	private double[] historicalSpaceHeatDemand;
	private double[] historicalEVDemand;

	/**
	 * Building heat flow time constant (thermal mass or specific heat capacity
	 * / heat loss rate)
	 */
	public double tau;
	private double[] historicalIntTemp;
	private double[] historicalExtTemp;
	private double[] historicalWaterHeatDemand;
	public double freeRunningTemperatureLossPerTickMultiplier;

	private double[] electricVehicleProfile;
	private double[] optimisedEVProfile;

	protected double[] dayHistoryHeatingEnergy;

	private double maxHeatPerTimestep = 12; // 12 kWh per half hour based on a gas boiler of
											// ~30kW at 90% efficiency. This can
											// be made more realistic later

	private int ratedStorageHeaterCapacity;


	private int ratedStorageHeaterPower;

	public boolean hasStorageHeater = false;

	public double azimuth;

	public StorageHeater storageHeater = null;

	/**
	 * Accessor functions (NetBeans style) TODO: May make some of these private
	 * to respect agent conventions of autonomy / realistic simulation of humans
	 */
	public double getCurrentInternalTemp()
	{
		return this.currentInternalTemp;
	}
	
	public double getDemand()
	{
		return this.getNetDemand() +this.currentGeneration();
	}

	public double getSetPoint()
	{
		return this.setPoint;
	}

	public double[] getSetPointProfile()
	{
		return Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
	}

	public double getDailyHotWaterUsage()
	{
		return this.dailyHotWaterUsage;
	}

	public void setDailyHotWaterUsage(double dailyHotWaterUsage)
	{
		this.dailyHotWaterUsage = dailyHotWaterUsage;
	}

	/**
	 * @return the dayHistoryHeatingEnergy
	 */
	public double[] getDayHistoryHeatingEnergy()
	{
		return this.dayHistoryHeatingEnergy;
	}

	/**
	 * @return
	 */
	public double[] getOptimisedSetPointProfile()
	{
		return Arrays.copyOf(this.optimisedSetPointProfile, this.optimisedSetPointProfile.length);
	}

	public double[] getOtherDemandProfile()
	{
		return Arrays.copyOf(this.arr_otherDemandProfile, this.arr_otherDemandProfile.length);
	}

	public boolean isHasElectricVehicle()
	{
		return this.hasElectricVehicle;
	}

	public boolean isHasGas()
	{
		return this.hasGas;
	}

	public void setHasGas(boolean gas)
	{
		this.hasGas = gas;
	}

	public boolean isHasElectricalWaterHeat()
	{
		return this.hasElectricalWaterHeat;
	}

	public void setHasElectricalWaterHeat(boolean electricalWaterHeat)
	{
		this.hasElectricalWaterHeat = electricalWaterHeat;
	}

	public boolean isHasElectricalSpaceHeat()
	{
		return this.hasElectricalSpaceHeat;
	}

	public void setHasElectricalSpaceHeat(boolean electricalSpaceHeat)
	{
		this.hasElectricalSpaceHeat = electricalSpaceHeat;
	}

	public boolean isHasWashingMachine()
	{
		return this.hasWashingMachine;
	}

	public void setHasWashingMachine(boolean washingMachine)
	{
		this.hasWashingMachine = washingMachine;
	}

	public boolean isHasWasherDryer()
	{
		return this.hasWasherDryer;
	}

	public void setHasWasherDryer(boolean washerDryer)
	{
		this.hasWasherDryer = washerDryer;
	}

	public boolean isHasTumbleDryer()
	{
		return this.hasTumbleDryer;
	}

	public void setHasTumbleDryer(boolean tumbleDryer)
	{
		this.hasTumbleDryer = tumbleDryer;
	}

	public boolean isHasDishWasher()
	{
		return this.hasDishWasher;
	}

	public void setHasDishWasher(boolean dishWasher)
	{
		this.hasDishWasher = dishWasher;
	}

	public boolean isHasWetAppliances()
	{
		boolean hasWetAppliances = false;

		hasWetAppliances = (this.hasWashingMachine || this.hasWasherDryer || this.hasTumbleDryer || this.hasDishWasher);

		return hasWetAppliances;
	}

	public boolean isHasRefrigerator()
	{
		return this.hasRefrigerator;
	}

	public boolean isHasFridgeFreezer()
	{
		return this.hasFridgeFreezer;
	}

	public boolean isHasUprightFreezer()
	{
		return this.hasUprightFreezer;
	}

	public boolean isHasChestFreezer()
	{
		return this.hasChestFreezer;
	}

	public boolean isHasColdAppliances()
	{

		boolean hasColdAppliances = false;

		hasColdAppliances = (this.hasFridgeFreezer || this.hasRefrigerator || this.hasUprightFreezer || this.hasChestFreezer);

		return hasColdAppliances;
	}

	public void setColdAppliancesProfiles(WeakHashMap<String, double[]> coldProfile)
	{
		this.coldApplianceProfiles = coldProfile;
	}

	public WeakHashMap<String, double[]> getColdAppliancesProfiles()
	{
		return this.coldApplianceProfiles;
	}

	public double[] getColdAppliancesProfile()
	{
		return this.calculateCombinedColdAppliancesProfile(this.getColdAppliancesProfiles());
	}

	public void setWetAppliancesProfiles(WeakHashMap<String, double[]> wetProfile)
	{
		this.wetApplianceProfiles = wetProfile;
	}

	public WeakHashMap<String, double[]> getWetAppliancesProfiles()
	{
		return this.wetApplianceProfiles;
	}

	public double[] getWetAppliancesProfile()
	{
		return this.calculateCombinedWetAppliancesProfile(this.getWetAppliancesProfiles());
	}

	public int getNumOccupants()
	{
		return this.numOccupants;
	}

	public void setNumOccupants(int numOccupants)
	{
		this.numOccupants = numOccupants;
	}

	public int getDefraCategory()
	{
		return this.defraCategory;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalOtherDemand()
	{
		return this.historicalBaseDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalColdDemand()
	{
		return this.historicalColdDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalWetDemand()
	{
		return this.historicalWetDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalSpaceHeatDemand()
	{
		return this.historicalSpaceHeatDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalWaterHeatDemand()
	{
		return this.historicalWaterHeatDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalEVDemand()
	{
		return this.historicalEVDemand;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalIntTemp()
	{
		return this.historicalIntTemp;
	}

	/**
	 * @return
	 */
	public double[] getHistoricalExtTemp()
	{
		return this.historicalExtTemp;
	}

	/**
	 * @param buildingThermalMass
	 *            the buildingThermalMass to set
	 */
	public void setBuildingThermalMass(double buildingThermalMass)
	{
		this.buildingThermalMass = buildingThermalMass;
	}

	/**
	 * @param buildingHeatLossRate
	 *            the buildingHeatLossRate to set
	 */
	public void setBuildingHeatLossRate(double buildingHeatLossRate)
	{
		this.buildingHeatLossRate = buildingHeatLossRate;
	}

	/**
	 * Returns a string representing the state of this agent. This method is
	 * intended to be used for debugging purposes, and the content and format of
	 * the returned string should include the states (variables/parameters)
	 * which are important for debugging purpose. The returned string may be
	 * empty but may not be <code>null</code>.
	 * 
	 * @return a string representation of this agent's state parameters
	 */
	@Override
	protected String paramStringReport()
	{
		String str = "";
		return str;

	}

	public double getUnadaptedDemand()
	{
		// Cope with tick count being null between project initialisation and
		// start.
		int index = Math.max(((int) RepastEssentials.GetTickCount() % this.arr_otherDemandProfile.length), 0);
		return (this.arr_otherDemandProfile[index]) - this.currentGeneration();
	}

	/**
	 * Method to return the realtime generation of this Household Prosumer (as
	 * distinct from the net demand for the prosumer).
	 * 
	 * @return a double giving the realtime generation of this Household
	 *         Prosumer
	 * 
	 */
	public double currentGeneration()
	{
		double returnAmount = 0;
		
		returnAmount = returnAmount + this.CHPGeneration() + this.windGeneration() + this.hydroGeneration() + this.thermalGeneration()
				+ this.PVGeneration();

		if (returnAmount != 0)
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("HouseholdProsumer:: Generating " + returnAmount);
			}
		}
		return returnAmount;
	}

	/**
	 * @return
	 */
	protected double PVGeneration()
	{
		if (this.hasPV)
		{
			// TODO: get a realistic model of solar production - this just
			// assumes
			// linear relation between insolation and maximum insolation arriving at
			// earth, which is ~1000 W/m^2, which is the power at which nominal rated
			// power is calculated
			//
			// We can introduce the McKenna, Thomson et al model from the CREST spreadsheet
			// to combine azimuth and time of day to get a better version of insolation than
			// `getInsolation()`
			
			double adjusted_insolation = adjustInsolationForTimeAndAzimuth(this.getInsolation());
			
			double p = (adjusted_insolation / Consts.MAX_INSOLATION) * this.ratedPowerPV;
			return p * ((1.0 *Consts.HOURS_PER_DAY) / this.mainContext.ticksPerDay); // convert
																				// power
																				// to
																				// kWh
																				// per
																				// tick
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @param timeOfDay2
	 * @param insolation
	 * @return
	 */
	private double adjustInsolationForTimeAndAzimuth(double insolation) {
		double dayOfYear = this.mainContext.getDayCount()%365;
        // Calculate B
        double B = 360 * (dayOfYear - 81) / 364; //Approximated Day of year (no leap years)
        // Calculate equation of time
        double equationOfTime = (9.87 * Math.sin(2 * B * Math.PI / 180)) - (7.53 * Math.cos(B * Math.PI / 180)) - (1.5 * Math.sin(B * Math.PI / 180));
        // Calculate time correction factor
        double longitude = 0;
        double timeCorrectionFactor = (4 * (longitude - 0)) + equationOfTime; // TODO: Standard longitude of -1 which is a bit off
		int hour = (timeOfDay * Consts.HOURS_PER_DAY) / this.mainContext.ticksPerDay;
		boolean britishSummerTime = true; // TODO: Replace with proper test
		if (britishSummerTime)
		{
			hour -= 1;
		}
		int minute = (((timeOfDay * Consts.HOURS_PER_DAY) % this.mainContext.ticksPerDay) * 60) / this.mainContext.ticksPerDay;
		double hoursBeforeSolarNoon = 12 - (hour + (minute*1.0 / 60) + (timeCorrectionFactor / 60));
		
		// Calculate optical depth
        double opticalDepth = 0.174 + (0.035 * Math.sin(2 * Math.PI * (dayOfYear - 100) / 365));
        // Calculate hour angle
        double hourAngle = 15 * hoursBeforeSolarNoon;
        // Calculate declination
        double declination = 23.45 * Math.sin(2 * Math.PI * (284 + dayOfYear) / 365.25);
        // Calculate solar altitude angle
        double latitude = 52;
        double solarAltitudeAngle = 180 / Math.PI * Math.asin((Math.cos(latitude * Math.PI / 180) * Math.cos(declination * Math.PI / 180) * Math.cos(hourAngle * Math.PI / 180)) + (Math.sin(latitude * Math.PI / 180) * Math.sin(declination * Math.PI / 180)));
        // Calculate azimuth of sun
        double azimuthOfSun = 180 / Math.PI * Math.asin(Math.cos(declination * Math.PI / 180) * Math.sin(hourAngle * Math.PI / 180) / Math.cos(solarAltitudeAngle * Math.PI / 180));
        // Calculate azimuth angle test
        boolean azimuthAngleTest = Math.cos(hourAngle * Math.PI / 180) >= (Math.tan(declination * Math.PI / 180) / Math.tan(declination * Math.PI / 180));

        double adjustedAzimuthOfSun = azimuthOfSun;
        // Calculate adjusted azimuth of sun
        if (!azimuthAngleTest) {
            if (azimuthOfSun > 90) {
                adjustedAzimuthOfSun = azimuthOfSun - 90;
            }
            else if (azimuthOfSun < -90) {
                adjustedAzimuthOfSun = azimuthOfSun + 90;
            }
                else {
                adjustedAzimuthOfSun = azimuthOfSun;
                }
        }
        else if (azimuthOfSun > 0 && azimuthOfSun < 90) {
            adjustedAzimuthOfSun = 180 - azimuthOfSun;
        }
        else if (azimuthOfSun > -90 && azimuthOfSun < 0) {
            adjustedAzimuthOfSun = -180 - azimuthOfSun;
        }
        
        // Calculate solar incident angle on panel
        double slope = 40; // Put panel slope at 40 degrees
        double solarIncidentAngle = Math.cos((Math.cos(solarAltitudeAngle * Math.PI / 180) * Math.cos(adjustedAzimuthOfSun * Math.PI / 180 - azimuth * Math.PI / 180) * Math.sin(slope * Math.PI / 180)) + (Math.sin(solarAltitudeAngle * Math.PI / 180) * Math.cos(slope * Math.PI / 180)));
        // Get clearsky beam radiation at surface (plane tracking the sun)
        if (Math.abs(solarIncidentAngle) > 90) {
            return 0;
        }
        else
        {
            return insolation * Math.cos(solarIncidentAngle * Math.PI / 180);
        }
        
		//return insolation; //Old basic version - just return insolation
	}

	/**
	 * @return
	 */
	private double thermalGeneration()
	{
		// Assume no thermal gen in domestic for now
		return 0;
	}

	/**
	 * @return
	 */
	private double hydroGeneration()
	{
		// Assumer no domestic hydro for now
		return 0;
	}

	/**
	 * @return
	 */
	private double windGeneration()
	{
		if (this.hasWind)
		{
			// TODO: get a realistic model of wind production - this just linear
			// between 2.5 and 12.5 metres per second (5 to 25 mph / knots
			// roughly),
			// zero below, max power above
			return (Math.max((Math.min(this.getWindSpeed(), 12.5d) - 2.5d), 0)) / 20 * this.ratedPowerWind;
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @return
	 */
	private double CHPGeneration()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	private double nonDispaceableDemand()
	{
		// TODO: For expediency just used melody's model for this, however could
		// use
		// finer grained model of lighting, brown and cooking

		// return lightingDemand() + miscBrownDemand() + cookingDemand();
		return this.arr_otherDemandProfile[this.time % this.arr_otherDemandProfile.length];
	}

	/**
	 * Returns the *electrical* load of heating demand Note that if heating is
	 * provided by solar thermal, gas boiler or CHP this demand will be zero.
	 * 
	 * Only demands from immersion heater, heat pump or electrical storage
	 * heating are considered here
	 * 
	 * @return
	 */
	private double getElectricalHeatingDemand(double potentialElecDemand)
	{
		if (this.storageHeater != null)
		{
			this.storageHeater.demandHeat(this.calculateSpaceHeatEnergyDemand(time));
			return this.storageHeater.getDemand(timeOfDay);
		}
		
		this.recordedHeatPumpDemand[this.timeOfDay] = 0;

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("Calculating heating demand");
		}

		if (this.isHasElectricalSpaceHeat())
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace(" ^^^hasElecSpaceheat ");
			}
			// TODO: this assumes only space heat and always uses heat pump -
			// expand for other forms of electrical heating
			this.recordedHeatPumpDemand[this.timeOfDay] += potentialElecDemand / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP;

		}

		// (20/01/12) Check if sum of <recordedHeatPumpDemand> is consistent at
		// end day
		if (this.timeOfDay == 47)
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("SUM(RecordedHeatPumpDemand: " + ArrayUtils.sum(this.recordedHeatPumpDemand));
			}
		}

		if (this.isHasElectricalWaterHeat())
		{
			// TODO: expand for other forms of electrical heating
			this.recordedHeatPumpDemand[this.timeOfDay] += this.getWaterHeatProfile()[this.timeOfDay];
		}

		return this.recordedHeatPumpDemand[this.timeOfDay];
	}

	/**
	 * @return
	 */
	private double wetApplianceDemand()
	{
		if (this.isHasWetAppliances())
		{
			return this.wetApplianceProfile[this.time % this.wetApplianceProfile.length];
		}
		else
		{
			return 0d;
		}
	}

	/**
	 * @return
	 */
	private double getOptimisedEVDemand()
	{
		if (this.hasElectricVehicle)
		{
			return this.optimisedEVProfile[this.time % this.electricVehicleProfile.length];
		}
		else
		{
			return 0d;
		}
	}

	/**
	 * @return
	 */
	private double coldApplianceDemand()
	{
		if (this.isHasColdAppliances())
		{
			return this.coldApplianceProfile[this.time % this.coldApplianceProfile.length];
		}
		else
		{
			return 0d;
		}
	}

	/**
	 * @return
	 */
	private double cookingDemand()
	{
		// Currently incorporated into the base demand
		return 0;
	}

	/**
	 * @return
	 */
	private double miscBrownDemand()
	{
		// Currently incorporated into the base demand
		return 0;
	}

	/**
	 * @return
	 */
	private double lightingDemand()
	{
		// Currently incorporated into the base demand
		return 0;
	}

	/**
	 * Calculates the heat demand for space heating at a given timestep (in kWh)
	 * 
	 * @param timeStep
	 * @return energy required to heat / maintain the space at the set point
	 *         temperature
	 */
	private double calculateSpaceHeatEnergyDemand(int timeStep)
	{

		double deltaT = this.setPoint/* currentInternalTemp */- this.getContext().getAirTemperature(timeStep);
		double requiredTempChange = this.setPoint - this.currentInternalTemp;
		double maintenanceEnergy = ((deltaT * (this.buildingHeatLossRate)) * (Consts.SECONDS_PER_DAY / this.mainContext.ticksPerDay))
				/ Consts.KWH_TO_JOULE_CONVERSION_FACTOR;
		double heatingEnergy = requiredTempChange * this.buildingThermalMass / Consts.DOMESTIC_COP_DEGRADATION_FOR_TEMP_INCREASE; // refer
																																	// to
																																	// Peter's
																																	// email
																																	// (24/01/12)

		if (this.mainContext.logger.isDebugEnabled())
		{
			this.mainContext.logger.debug(maintenanceEnergy + " to maintain temp, plus " + heatingEnergy + " to heat up");
		}

		double[] hotWaterEnergy = ArrayUtils.multiply(this.getBaselineHotWaterVolumeProfile(), Consts.WATER_SPECIFIC_HEAT_CAPACITY
				/ Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (this.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP)));

		double energyDemand = maintenanceEnergy + heatingEnergy + hotWaterEnergy[this.timeOfDay];
		if (energyDemand > this.maxHeatPerTimestep)
		{
			energyDemand = this.maxHeatPerTimestep;
		}

		return energyDemand;
	}

	/**
	 * Calculates the heat pump demand at a given timestep (in kWh)
	 * 
	 * @param timeStep
	 * @return
	 */
	private double calculateHeatDemandAndInternalTemp(int timeStep)
	{
		// demand is the local variable holding the energy demand

		double electricityDemand = 0;
		double deltaT = this.setPoint/* currentInternalTemp */- this.getContext().getAirTemperature(timeStep);

		double requiredTempChange = this.setPoint - this.currentInternalTemp;

		if ((requiredTempChange < Consts.TEMP_CHANGE_TOLERANCE) || (deltaT < Consts.HEAT_PUMP_THRESHOLD_TEMP_DIFF))
		{
			// heat pump off, leave demand at zero and decrement internal
			// temperature
			this.currentInternalTemp -= (this.currentInternalTemp - this.mainContext.getAirTemperature(timeStep))
					* this.freeRunningTemperatureLossPerTickMultiplier;
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("HouseholdProsumer:: Heating system off - currentInternalTemp: " + this.currentInternalTemp);
			}
			this.dayHistoryHeatingEnergy[this.timeOfDay] = 0;

		}
		else
		{
			double energyDemand = this.calculateSpaceHeatEnergyDemand(timeStep);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Adding " + energyDemand + " to the historical heat energy array");
			}
			if (energyDemand > (this.maxHeatPerTimestep))
			{
				// If the energy demand to raise to set point in this particular
				// timestep
				// is greater than that which can be delivered by the HP, cap it
				// at the maximum and
				// raise the internal temperature
				if (this.isHasElectricalSpaceHeat())
				{
					electricityDemand = (this.ratedPowerHeatPump * Consts.DOMESTIC_HEAT_PUMP_SPACE_COP)
							* ((double) 24 / this.mainContext.ticksPerDay);
				}
				this.currentInternalTemp = this.currentInternalTemp
						+ ((this.maxHeatPerTimestep - ((deltaT * (this.buildingHeatLossRate)) * (Consts.SECONDS_PER_DAY / this.mainContext.ticksPerDay))
								/ Consts.KWH_TO_JOULE_CONVERSION_FACTOR) / this.buildingThermalMass);
if (				this.mainContext.logger.isTraceEnabled()) {
				this.mainContext.logger.trace("HouseholdProsumer:: Heating system on max, can't regain set point currentInternalTemp: "
						+ this.currentInternalTemp);
}
				this.dayHistoryHeatingEnergy[this.timeOfDay] = this.maxHeatPerTimestep;

			}
			else
			{
				// If energy demand within the capability of the heatpump,
				// switch it on until set point regained.
				if (this.isHasElectricalSpaceHeat())
				{
					electricityDemand = energyDemand / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP;
				}
				this.currentInternalTemp = this.setPoint;
				this.dayHistoryHeatingEnergy[this.timeOfDay] = energyDemand;
				if (this.mainContext.logger.isTraceEnabled())
				{
					this.mainContext.logger.trace("HouseholdProsumer:: Heatpump on until current internal temperature regains set point");
				}
			}
		}

		/*
		 * if (this.agentID == 1) { if ( *
		 * this.mainContext.logger.isDebugEnabled()) {
		 * this.mainContext.logger.debug("=================="); }
		 * this.mainContext.logger.debug("Tick " + this.time + "; " +
		 * this.agentName + " internal temperature: " +
		 * this.currentInternalTemp); this.mainContext.logger.debug("demand: "+
if (		 * demand); this.mainContext.logger.isDebugEnabled()) {
		 * demand); this.mainContext.logger.debug("buildingHeatLossRate: " +
		 * buildingHeatLossRate);
}
		 * this.mainContext.logger.debug("buildingThermalMass: " +
		 * this.buildingThermalMass); }
		 */

		return electricityDemand;
	}

	/**
	 * Calculates the current internal temperature at the end of the current
	 * timestep
	 * 
	 * @param timeStep
	 * @return
	 */
	private void recordInternalAndExternalTemp(int timeStep)
	{
		double extTemp = this.getContext().getAirTemperature(timeStep);

		this.historicalIntTemp[timeStep % this.historicalIntTemp.length] = this.currentInternalTemp;
		this.historicalExtTemp[timeStep % this.historicalIntTemp.length] = extTemp;
	}

	/**
	 * calculates the water temperature
	 * 
	 * Currently has a very simple linear model of water cooling TODO: replace
	 * linear model with a more sophisticated negative exponential model.
	 * 
	 * @param timeStep
	 * @return
	 */
	private void calculateWaterTemp(int timeStep)
	{
		if (!this.waterHeaterOn)
		{
			this.currentWaterTemp = this.currentWaterTemp - 1;
		}
	}

	/**
	 * @param time
	 * @return double giving sum of baseDemand for the day.
	 */
	private double calculateFixedDayTotalDemand(int time)
	{
		int baseProfileIndex = time % this.arr_otherDemandProfile.length;
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" baseDemandProfile " + Arrays.toString(this.arr_otherDemandProfile));
		}
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(" baseProfileIndex " + baseProfileIndex);
		}
if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace("  calculateFixedDayTotalDemand: array2Sum: "
				+ Arrays.toString(Arrays.copyOfRange(this.arr_otherDemandProfile, baseProfileIndex, baseProfileIndex
						+ this.mainContext.ticksPerDay - 1)));
}
		return ArrayUtils.sum(Arrays.copyOfRange(this.arr_otherDemandProfile, baseProfileIndex, baseProfileIndex
				+ this.mainContext.ticksPerDay - 1));
	}

	/*
	 * Logic helper methods
	 */

	/**
	 * Evaluates the net demand mediated by the prosumers elastic behaviour in a
	 * given half hour.
	 * 
	 * NOTE: As implemented - this method does not enforce total demand parity
	 * over a given day (i.e. integral of netDemand over a day is not
	 * necessarily constant.
	 * 
	 * @param int time - the simulation time in ticks at which to evaluate this
	 *        prosumer's behaviour
	 * @return double myDemand - the demand for this half hour, mediated via
	 *         behaviour change
	 */
	private double evaluateElasticBehaviour(int time)
	{
		double myDemand;
		int timeSinceSigValid = time - this.predictionValidTime;

		// As a basic strategy only the base (non-displaceable) demand is
		// elastic

		/*
		 * double [] avgDemands7Days =
		 * {0.500677033,0.407725724,0.370908322,0.35877468
		 * ,0.347866658,0.324998379
		 * ,0.343484827,0.308885358,0.298035432,0.27947423
		 * ,0.280343265,0.289909821
		 * ,0.303676612,0.333950661,0.426824179,0.616930198
		 * ,0.630372238,0.589740306
		 * ,0.53584991,0.589862047,0.621102996,0.621623774
		 * ,0.727205077,0.845539884
		 * ,0.959784962,0.931100067,0.864783952,0.773823124
		 * ,0.835309411,0.635960097
		 * ,0.663524804,0.781160132,0.8361668,1.010964822
		 * ,1.306060966,1.31329955,
		 * 1.245892295,0.996678877,0.990747587,0.946006202
		 * ,0.944353378,0.954267895
		 * ,0.909130777,0.792284367,0.751724127,0.660532121
		 * ,0.637187946,0.557241803};
		 * 
		 * double [] avgDemands;
		 * 
		 * 
		 * if (mainContext.getDayCount() <
		 * Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE) myDemand =
		 * baseDemandProfile[time % baseDemandProfile.length]; else if
		 * (mainContext.getDayCount() >
		 * Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE &&
		 * mainContext.getDayCount()<
		 * Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE+
		 * Consts.AGGREGATOR_TRAINING_PERIODE) myDemand = avgDemands7Days[time %
		 * avgDemands7Days.length]; else myDemand = baseDemandProfile[time %
		 * baseDemandProfile.length];
		 */

		myDemand = this.arr_otherDemandProfile[time % this.arr_otherDemandProfile.length];
		// if (myDemand <0)
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("*myDemand: " + myDemand);
		}

		// Adapt behaviour somewhat. Note that this does not enforce total
		// demand the same over a day.
		// Note, we can only moderate based on cost signal
		// if we receive it (i.e. if we have smart meter)
		// TODO: may have to refine this - do we differentiate smart meter and
		// smart display - i.e. whether receive only or Tx/Rx

		if (this.hasSmartMeter && this.getPredictedCostSignalLength() > 0)
		{
			double predictedCostSignal = this.getPredictedCostSignal()[timeSinceSigValid % this.getPredictedCostSignalLength()];

			// if (mainContext.getDayCount() >
			// Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE +
			// Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE+
			// Consts.AGGREGATOR_TRAINING_PERIODE)
			// predictedCostNow = predictedCostNow *1000;

			double myE = this.dailyElasticity[time % this.mainContext.ticksPerDay];
			// double eChange =
			// mainContext.hhProsumerElasticityTest.nextDouble();
			// if ( this.mainContext.logger.isTraceEnabled()) {
			// this.mainContext.logger.trace("eChange: "+ eChange);
			// }
			// myE = myE + eChange;
			// myDemand = myDemand * (1 - ((predictedCostNow /
			// Consts.NORMALIZING_MAX_COST) * dailyElasticity[time %
			// this.mainContext.ticksPerDay]));

			double initialDemand = myDemand;
			myDemand = myDemand * (1 - ((predictedCostSignal / Consts.NORMALIZING_MAX_COST) * myE));

			if (myDemand < 0)
			{
				/*
				 * this.mainContext.logger.debug("=================time: "+this.
				 * timeOfDay+ ", id: "+this.agentID + ", occ: "+
if (				 * this.numOccupants); this.mainContext.logger.isDebugEnabled()) {
				 * this.numOccupants); this.mainContext.logger.debug("pos: "+
				 * time % arr_otherDemandProfile.length);
}
				 * 
				 * if ( * this.mainContext.logger.isDebugEnabled()) {
				 * this.mainContext.logger.debug("myElast: "+ myE); }
if (				 * this.mainContext.logger.isDebugEnabled()) {
				 * this.mainContext.logger.debug("costSig: "+
				 * predictedCostSignal);
}
if (				 * this.mainContext.logger.isDebugEnabled()) {
				 * this.mainContext.logger.debug("myDemand (before): "+
				 * initialDemand);
}
if (				 * this.mainContext.logger.isDebugEnabled()) {
				 * this.mainContext.logger.debug("myDemand (befor2): "+
				 * arr_otherDemandProfile[time %
				 * arr_otherDemandProfile.length]);
}
				 * this.mainContext.logger.debug("myDemand (after ): "+myDemand
				 * +", costSignal:"+predictedCostSignal); if ( *
				 * this.mainContext.logger.isDebugEnabled()) {
				 * this.mainContext.logger.debug(); }
if (				 * this.mainContext.logger.isDebugEnabled()) {
				 * this.mainContext.logger.debug("myDemand (all): "+
				 * Arrays.toString(arr_otherDemandProfile));
}
				 * 
				 * if ( * this.mainContext.logger.isDebugEnabled()) {
				 * this.mainContext.logger.debug("====================== "); }
				 */
			}

			if (false)
			{
				// if (mainContext.isBeginningOfDay()) {

				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("============================");
				}
				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("dailyDemand: " + Arrays.toString(this.arr_otherDemandProfile));
				}
				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("dailyElasticity: " + Arrays.toString(this.dailyElasticity));
				}
				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("predictedCostSignal: " + Arrays.toString(this.getPredictedCostSignal()));
				}
				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("predictedCostSignal: " + predictedCostSignal);
				}
				if (this.mainContext.logger.isDebugEnabled())
				{
					this.mainContext.logger.debug("predictedCostNow * myE: " + predictedCostSignal * myE);
				}
if (				this.mainContext.logger.isDebugEnabled()) {
				this.mainContext.logger.debug("dailyElasticity[time % this.mainContext.ticksPerDay]: "
						+ this.dailyElasticity[time % this.mainContext.ticksPerDay]);
}
if (				this.mainContext.logger.isDebugEnabled()) {
				this.mainContext.logger.debug("HouseholdProsumer:: Based on predicted cost = "
						+ predictedCostSignal
						+ " demand set to "
						+ (1 - ((predictedCostSignal / Consts.NORMALIZING_MAX_COST) * this.dailyElasticity[time
								% this.mainContext.ticksPerDay])) + " of initial ");
}
			}
		}

		return myDemand;
	}

	/**
	 * Evaluates the net demand mediated by smart controller behaviour at a
	 * given tick.
	 * 
	 * NOTE: As implemented - this method enforces total demand parity over a
	 * given day (i.e. integral of netDemand over a day is not necessarily
	 * constant.
	 * 
	 * @param int time - the simulation time in ticks at which to evaluate this
	 *        prosumer's behaviour
	 * @return double myDemand - the demand for this half hour, mediated via
	 *         behaviour change
	 */
	private double smartDemand(int time, double potentialElecDemand)
	{

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("HHPro: smartDemand() time: " + time);
		}
		// Evaluate behaviour applies elasticity behaviour to the base
		// (non-displaceable) load.
		double currentBase = this.evaluateElasticBehaviour(time);
		double currentCold = this.coldApplianceDemand();

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("currentColdDemand is: " + currentCold);
		}

		double currentWet = this.wetApplianceDemand();
		double currentEV = this.getOptimisedEVDemand();

		double currentHeatElectricalDemand = 0;

		currentHeatElectricalDemand = this.getElectricalHeatingDemand(potentialElecDemand);

		this.historicalBaseDemand[this.timeOfDay] = currentBase;
		this.historicalWetDemand[this.timeOfDay] = currentWet;
		this.historicalColdDemand[this.timeOfDay] = currentCold;
		this.historicalEVDemand[this.timeOfDay] = currentEV;

		// if (this.getHasElectricalSpaceHeat())
		if (this.isHasElectricalSpaceHeat() && this.isHasElectricalWaterHeat() && this.storageHeater==null)
		{
			this.historicalSpaceHeatDemand[this.timeOfDay] = currentHeatElectricalDemand - this.getWaterHeatProfile()[this.timeOfDay]; // Verify
																																		// this!
		}
		else if (this.isHasElectricalSpaceHeat())
		{
			this.historicalSpaceHeatDemand[this.timeOfDay] = currentHeatElectricalDemand;
		}

		if (this.isHasElectricalWaterHeat())
		{
			this.historicalWaterHeatDemand[this.timeOfDay] = this.getWaterHeatProfile()[this.timeOfDay];
		}

		double returnAmount = currentBase + currentCold + currentWet + currentHeatElectricalDemand + currentEV;

		if (returnAmount != 0)
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("HouseholdProsumer:: Total demand (not net against generation) " + returnAmount);
			}
		}

		return returnAmount;
	}

	private void learnBehaviourChange()
	{
		// TODO: Implement the behavioural (social?) learning in here
	}

	private void smartControlLearn(int time)
	{
		// smart device (optimisation) learning in here
		// in lieu of knowledge of what can "switch off" and "switch on", we
		// assume that
		// the percentage moveable of the day's consumption is what may be time
		// shifted
		double moveableLoad = this.inelasticTotalDayDemand * this.percentageMoveableDemand;
		double[] daysCostSignal = new double[this.mainContext.ticksPerDay];
		double[] daysOptimisedDemand = new double[this.mainContext.ticksPerDay];
		if ((Boolean) RepastEssentials.GetParameter("verboseOutput"))
		{
if (			this.mainContext.logger.isDebugEnabled()) {
			this.mainContext.logger.debug("HouseholdProsumer: predictedCostSignal " + this.getPredictedCostSignal() + " time " + time
					+ " predictionValidTime " + this.predictionValidTime + " daysCostSignal " + daysCostSignal
					+ " this.mainContext.ticksPerDay " + this.mainContext.ticksPerDay);
}
		}
		System.arraycopy(this.getPredictedCostSignal(), time - this.predictionValidTime, daysCostSignal, 0, this.mainContext.ticksPerDay);

		System.arraycopy(this.smartOptimisedProfile, time % this.smartOptimisedProfile.length, daysOptimisedDemand, 0, this.mainContext.ticksPerDay);

		double[] tempArray = ArrayUtils.mtimes(daysCostSignal, daysOptimisedDemand);

		double currentCost = ArrayUtils.sum(tempArray);
		// Algorithm to minimise this whilst satisfying constraints of
		// maximum movable demand and total demand being inelastic.

		double movedLoad = 0;
		double movedThisTime = -1;
		double swapAmount = -1;
		while (movedLoad < moveableLoad && movedThisTime != 0)
		{
			int maxIndex = ArrayUtils.indexOfMax(tempArray);
			int minIndex = ArrayUtils.indexOfMin(tempArray);
			swapAmount = (daysOptimisedDemand[minIndex] + daysOptimisedDemand[maxIndex]) / 2;
			movedThisTime = ((daysOptimisedDemand[maxIndex] - daysOptimisedDemand[minIndex]) / 2);
			movedLoad = movedLoad + movedThisTime;
			daysOptimisedDemand[maxIndex] = swapAmount;
			daysOptimisedDemand[minIndex] = swapAmount;

			tempArray = ArrayUtils.mtimes(daysOptimisedDemand, daysCostSignal);
		}
		System.arraycopy(daysOptimisedDemand, 0, this.smartOptimisedProfile, time % this.smartOptimisedProfile.length, this.mainContext.ticksPerDay);
		if (ArrayUtils.sum(daysOptimisedDemand) != this.inelasticTotalDayDemand)
		{
			// TODO: This always gets triggerd - I wonder if the "day" i'm
			// taking
			// here and in the inelasticdemand method are "off-by-one"
if (			this.mainContext.logger.isTraceEnabled()) {
			this.mainContext.logger.trace("HouseholdProsumer:: optimised signal has varied the demand !!! In error !"
					+ (ArrayUtils.sum(daysOptimisedDemand) - this.inelasticTotalDayDemand));
}
		}

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("HouseholdProsumer:: Saved " + (currentCost - ArrayUtils.sum(tempArray)) + " cost");
		}

	}

	private void learnSmartAdoptionDecision(int time)
	{

		// TODO: implement learning whether to adopt smart control in here
		// Could be a TpB based model.
		double inwardInfluence = 0;
		double internalInfluence = this.HEMSPropensity;
		Iterable<?> socialConnections = RepastEssentials.FindNetwork("socialNetwork").getInEdges(this);
		// Get social influence - note communication is not every tick
		// hence the if clause
		if ((time % (21 * this.mainContext.ticksPerDay)) == 0)
		{

			for (Object thisConn : socialConnections)
			{
				RepastEdge<?> myConn = ((RepastEdge<?>) thisConn);
				if (((HouseholdProsumer) myConn.getSource()).hasSmartControl)
				{

					inwardInfluence = inwardInfluence + myConn.getWeight()
							* ((HouseholdProsumer) myConn.getSource()).transmitPropensitySmartControl;
				}
			}
		}

		double decisionCriterion = inwardInfluence + internalInfluence;
		this.HEMSPropensity = decisionCriterion;
		if (decisionCriterion > (Double) RepastEssentials.GetParameter("smartControlDecisionThreshold"))
		{
			this.hasSmartControl = true;
		}
	}

	public void setBaselineHotWaterVolumeProfile(double[] aBaselineHotWaterVolumeProfile)
	{
		this.baselineHotWaterVolumeProfile = aBaselineHotWaterVolumeProfile;
	}

	public double[] getBaselineHotWaterVolumeProfile()
	{
		return this.baselineHotWaterVolumeProfile;
	}

	/**
	 * @param waterHeatProfile
	 *            the waterHeatProfile to set
	 */
	public void setWaterHeatProfile(double[] waterHeatProfile)
	{
		this.waterHeatElectricalDemandProfile = waterHeatProfile;
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("setWaterHeatProfile:    " + Arrays.toString(waterHeatProfile));
		}
	}

	/**
	 * @return the waterHeatProfile
	 */
	public double[] getWaterHeatProfile()
	{
		return this.waterHeatElectricalDemandProfile;
	}

	public void initializeRandomlyDailyElasticityArray(double from, double to)
	{
		for (int i = 0; i < this.dailyElasticity.length; i++)
		{
			this.dailyElasticity[i] = RandomHelper.nextDoubleFromTo(from, to);
		}
		/*
		 * // (13/02/12) DF // Print out elasticity factor for 1 prosumer for
		 * testing revising elasticity calculation
		 * System.out.print("Prosumer Elasticity: "); for(int i=0; i<
		 * dailyElasticity.length; i++) { System.out.print(dailyElasticity[i] +
		 * if ( * ","); } this.mainContext.logger.isDebugEnabled()) { ","); }
		 * this.mainContext.logger.debug(""); }
		 */
	}

	// this is temporary method for test (can be removed later)
	public void initializeSimilarlyDailyElasticityArray(double val)
	{
		for (int i = 0; i < this.dailyElasticity.length; i++)
		{
			this.dailyElasticity[i] = val;
		}
	}

	public void setRandomlyPercentageMoveableDemand(double from, double to)
	{
		this.percentageMoveableDemand = RandomHelper.nextDoubleFromTo(from, to);
	}

	public void initializeWaterHeatDemand()
	{

		// Estimate the daily hot water usage in accordance with the model found
		// and described
		// in Energy Saving Trust Paper - Measurement of Domestic Hot Water
		// Consumption in Dwellings (2008) prepared for DEFRA.
		double dailyHotWaterUsage = this.mainContext.waterUsageGenerator.nextDouble(Consts.EST_INTERCEPT
				+ (this.getNumOccupants() * Consts.EST_SLOPE), Consts.EST_STD_DEV);
		this.setDailyHotWaterUsage(dailyHotWaterUsage);

		this.waterSetPoint = Consts.DOMESTIC_SAFE_WATER_TEMP;

		// TODO: something more sophisticated to give the baseline water heat
		// requirement

		/*
		 * TODO: the length of hot water profile (along with the heat space
		 * profile) are left unchanged at one single day (unlike others which
		 * are by default 1Y long). This is because the initial design of heat
		 * space and water did not considered variation of set points (etc) for
		 * more than a day. Increasing the size of array containing set points
		 * etc without adjustment in wattbox controller will cause problem. This
		 * needs to be addressed at later stage.
		 */

		// double[] hotWaterNeededProfile = new
		// double[this.lengthOfDemandProfile];
		double[] hotWaterNeededProfile = new double[this.mainContext.ticksPerDay];

		// share the needed water equally between occupants
		double drawOffPerOccupant = dailyHotWaterUsage / this.getNumOccupants();

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("hotWaterNeededProfile: " + Arrays.toString(hotWaterNeededProfile));
		}

		// Assign one draw off per occupant, at a time generated from the
		// statistics gathered
		// in the EST paper referenced above.
		for (int i = 0; i < this.getNumOccupants(); i++)
		{
			hotWaterNeededProfile[this.mainContext.drawOffGenerator.nextInt()] = drawOffPerOccupant;
		}

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("drawOffGenerator x 100times");
		}
		// for (int i = 0; i < 100; i++) {
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace(this.mainContext.drawOffGenerator.nextInt());
		}
		// }

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("AFTER drawOffGen");
		}
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("hotWaterNeededProfile: " + Arrays.toString(hotWaterNeededProfile));
		}

		double[] baselineHotWaterVolumeProfile = Arrays.copyOf(hotWaterNeededProfile, hotWaterNeededProfile.length);
		this.setBaselineHotWaterVolumeProfile(baselineHotWaterVolumeProfile);
		this.waterHeatEnergyDemandProfile = ArrayUtils.multiply(baselineHotWaterVolumeProfile, Consts.WATER_SPECIFIC_HEAT_CAPACITY
				/ Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (this.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP)));

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("setBaselineHotWaterVolumeProfile:    " + Arrays.toString(baselineHotWaterVolumeProfile));
		}
	}

	public void initializeElectWaterHeatPar()
	{

		this.ratedPowerHeatPump = Consts.TYPICAL_HEAT_PUMP_ELEC_RATING;

		if (this.isHasElectricalWaterHeat())
		{
			this.setWaterHeatProfile(ArrayUtils.multiply(this.getBaselineHotWaterVolumeProfile(), Consts.WATER_SPECIFIC_HEAT_CAPACITY
					/ Consts.KWH_TO_JOULE_CONVERSION_FACTOR * (this.waterSetPoint - ArrayUtils.min(Consts.MONTHLY_MAINS_WATER_TEMP))
					/ Consts.DOMESTIC_HEAT_PUMP_WATER_COP));
		}
		else
		{
			if (this.mainContext.logger.isDebugEnabled())
			{
				this.mainContext.logger.debug("No electric water heating!!!  Initialising Electrical water heat load all zero");
			}

			double[] noWaterHeating = new double[this.mainContext.ticksPerDay];
			// double[] noWaterHeating = new double[this.lengthOfDemandProfile];
			Arrays.fill(noWaterHeating, 0);
			this.setWaterHeatProfile(noWaterHeating);
		}

		this.setHasElectricalWaterHeat(true);
	}

	public void initializeElecSpaceHeatPar()
	{
		//initializeElecSpaceHeatPar(Consts.HEAT_TYPE_HP);
		initializeElecSpaceHeatPar(Consts.HEAT_TYPE_STORAGE);
	}
	
	public void initializeElecSpaceHeatPar(String type)
	{
		this.minSetPoint = Consts.HOUSEHOLD_MIN_SETPOINT;
		this.maxSetPoint = Consts.HOUSEHOLD_MAX_SETPOINT;
		
		if (type.equalsIgnoreCase(Consts.HEAT_TYPE_STORAGE))
		{
			this.ratedStorageHeaterCapacity = 50; //kWh storage capacity - starter for 10 value
			this.ratedStorageHeaterPower = 7; //kW for charging power
			// Initialise the base case where heat pump is on all day
			this.spaceHeatPumpOn = new double[this.mainContext.ticksPerDay];
			Arrays.fill(this.spaceHeatPumpOn, 0, 14, 1); //Fill as charging in Econ 7 only
            this.storageHeater = new StorageHeater(this);
		}
		else
		{
			//Default to heat pump

		this.ratedPowerHeatPump = Consts.TYPICAL_HEAT_PUMP_ELEC_RATING;

		/*
		 * TODO: the length of space heat profile (along with the hot water
		 * profile) are left unchanged to one single day (unlike others which
		 * are by default 1Y long). This is because the initial design of heat
		 * space (and hot water) did not considered variation of set points
		 * (etc) for more than a day. Increasing the size of array containing
		 * set points without an adjustment to the wattbox controller will cause
		 * problem. This needs to be addressed at later stage.
		 */

		// Initialise the base case where heat pump is on all day
		this.spaceHeatPumpOn = new double[this.mainContext.ticksPerDay];
		Arrays.fill(this.spaceHeatPumpOn, 1);
		}
		this.setHasElectricalSpaceHeat(true);


	}

	public void setWattboxController()
	{
		this.hasSmartControl = true;
		// this.mySmartController = new WattboxController(this,
		// this.mainContext);
		this.mySmartController = new ProportionalWattboxController(this, this.mainContext);
	}

	private double[] calculateCombinedColdAppliancesProfile(WeakHashMap<String, double[]> coldProfiles)
	{
		double[] fridge_loads = coldProfiles.get(Consts.COLD_APP_FRIDGE);
		double[] freezer_loads = coldProfiles.get(Consts.COLD_APP_FREEZER);
		double[] fridge_freezer_loads = coldProfiles.get(Consts.COLD_APP_FRIDGEFREEZER);
		return ArrayUtils.add(fridge_loads, freezer_loads, fridge_freezer_loads);
	}

	private double[] calculateCombinedWetAppliancesProfile(WeakHashMap<String, double[]> wetApplianceProfiles2)
	{
		double[] washer_loads = wetApplianceProfiles2.get(Consts.WET_APP_WASHER);
		double[] dryer_loads = wetApplianceProfiles2.get(Consts.WET_APP_DRYER);
		double[] dishwasher_loads = wetApplianceProfiles2.get(Consts.WET_APP_DISHWASHER);
		return ArrayUtils.add(washer_loads, dryer_loads, dishwasher_loads);
	}

	/**
	 * This method is used to check whether the 'profile building' period has
	 * completed. the 'profile building' period (the initial part of the
	 * training period, usually 4-7 days) is currently determined by a rarely
	 * changed variable in the Consts class
	 * 
	 * @see Consts#AGGREGATOR_PROFILE_BUILDING_PERIODE
	 * @return true if the profile building period is completed, false otherwise
	 */
	private boolean isAggregateDemandProfileBuildingPeriodCompleted()
	{
		boolean isEndOfProfilBuilding = true;
		int daysSoFar = this.mainContext.getDayCount();
		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("ADPBPC: daySoFar: " + daysSoFar + " < 7?");
		}

		if (daysSoFar < Consts.AGGREGATOR_PROFILE_BUILDING_PERIODE)
		{
			isEndOfProfilBuilding = false;
		}
		return isEndOfProfilBuilding;
	}

	/**
	 * (23/04/12) DF Method to call for returning the household space heat
	 * demand. The calculated space heat demand assumes there is NO smart
	 * controller to optimise the load, i.e. the demands are static throughout a
	 * year.
	 * 
	 * Re-paste into the new framework from old one
	 * 
	 * @return a one-dimensional array of size [this.mainContext.ticksPerDay *
	 *         Consts.NB_OF_DAYS_LOADED_DEMAND]
	 */
	public double[] getSpaceHeatDemandwithoutOptimise()
	{

		int lengthOfProfileArrays = this.mainContext.ticksPerDay * Consts.NB_OF_DAYS_LOADED_DEMAND;
		double[] spaceHeatDemand = new double[lengthOfProfileArrays];

		for (int i = 0; i < lengthOfProfileArrays; i++)
		{
			if (this.hasElectricalSpaceHeat)
			{
				// this assumes only space heat always uses heat pump
				spaceHeatDemand[i] = this.calculateHeatDemandAndInternalTemp(i) / Consts.DOMESTIC_HEAT_PUMP_SPACE_COP;
			}
			else
			{
				// not interested in gas - make this 0
				spaceHeatDemand[i] = 0d;
			}
		}

		return spaceHeatDemand;
	}

	@ScheduledMethod(start = 0, interval = 0, priority = Consts.PROSUMER_PRIORITY_FIFTH)
	public void probeSpecificAgent()
	{
		if (this.getAgentID() == 691)
		{
			ArrayList probed = new ArrayList();
			probed.add(this);
			List<IDisplay> listOfDisplays = RunState.getInstance().getGUIRegistry().getDisplays();
			for (IDisplay display : listOfDisplays)
			{

				if (display instanceof DisplayOGL2D)
				{
					((DisplayOGL2D) display).getProbeSupport().fireProbeEvent(this, probed);
				}
			}
		}

	}

	/******************
	 * 
	 * This method defines the step behaviour of a prosumer agent
	 * 
	 ******************/
	@Override
	public void step()
	{

		// Note the simulation time if needed.
		// Note - Repast can cope with fractions of a tick (a double is
		// returned)
		// but I am assuming here we will deal in whole ticks and alter the
		// resolution should we need

		if (this.mainContext.logger.isTraceEnabled())
		{
if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace("  -------- HouseholdProsumer(" + this.getAgentID() + ") step() ---------- DayCount: "
				+ this.mainContext.getDayCount() + ",Timeslot: " + this.mainContext.getTimeslotOfDay() + ",TickCount: "
				+ this.mainContext.getTickCount());
}
		}

		this.time = (int) RepastEssentials.GetTickCount();
		this.timeOfDay = (this.time % this.mainContext.ticksPerDay);

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("checkWeather");
		}
		
		this.checkWeather(this.time);

		// Do all the "once-per-day" things here

		if ((this.timeOfDay == 0) && this.isHasColdAppliances())
		{
			this.coldApplianceProfile = this.calculateCombinedColdAppliancesProfile(this.coldApplianceProfiles);
		}

		if ((this.timeOfDay == 0) && this.isHasWetAppliances())
		{
			this.wetApplianceProfile = this.calculateCombinedWetAppliancesProfile(this.wetApplianceProfiles);
		}

		if (this.timeOfDay == 0 && this.isAggregateDemandProfileBuildingPeriodCompleted())
		// if (timeOfDay == 0)
		{
			// TODO: decide whether the inelastic day demand is something that
			// needs
			// calculating here
			this.inelasticTotalDayDemand = this.calculateFixedDayTotalDemand(this.time);
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("  HHpro: inelasticTotalDayDemand: " + this.inelasticTotalDayDemand);
			}

			if (this.hasSmartControl)
			{

				if (this.mainContext.logger.isTraceEnabled())
				{
					this.mainContext.logger.trace("--beforCallToUpdate; time: " + this.time);
				}

				this.mySmartController.update(this.time);

				if (this.isHasColdAppliances())
				{
					this.coldApplianceProfile = this.calculateCombinedColdAppliancesProfile(this.coldApplianceProfiles);
				}

				if (this.isHasWetAppliances())
				{
					this.wetApplianceProfile = this.calculateCombinedWetAppliancesProfile(this.wetApplianceProfiles);
				}

				this.currentSmartProfiles = this.mySmartController.getCurrentProfiles();

				// this.coldApplianceProfile =
				// calculateCombinedColdAppliancesProfile(this.coldApplianceProfiles);

				this.optimisedSetPointProfile = this.currentSmartProfiles.get("HeatPump");
				this.setWaterHeatProfile(this.currentSmartProfiles.get("WaterHeat"));

			}

			// ***Richard output test for prosumer behaviour***

			if (this.sampleOutput != null)
			{
				this.sampleOutput.appendText("day: " + this.mainContext.getDayCount() + ", timeStep " + this.time);
				this.sampleOutput.appendText("baseDemandProfile: ");
				this.sampleOutput.appendText(Arrays.toString(this.arr_otherDemandProfile));
				// sampleOutput.appendText("coldApplianceProfile: ");
				// sampleOutput.appendText(Arrays.toString(coldApplianceProfile));
				// sampleOutput.appendText("wetApplianceProfile: ");
				// sampleOutput.appendText(Arrays.toString(wetApplianceProfile));

				this.sampleOutput.writeColHeaders(new String[]
				{ "spaceHeatPumpOn", "recordedHeatPumpDemand", "wetApp", "coldApp" });
				String[][] outputBuilder = new String[4][this.mainContext.ticksPerDay];
				if (this.isHasElectricalSpaceHeat())
				{
					// String[][] outputBuilder = new
					// String[4][spaceHeatPumpOn.length];
					outputBuilder[0] = ArrayUtils.convertDoubleArrayToString(this.spaceHeatPumpOn);
					outputBuilder[1] = ArrayUtils.convertDoubleArrayToString(this.recordedHeatPumpDemand);
				}
				if (this.isHasWetAppliances())
				{
					outputBuilder[2] = ArrayUtils.convertDoubleArrayToString(this.wetApplianceProfile);
				}
				if (this.isHasColdAppliances())
				{
					outputBuilder[3] = ArrayUtils.convertDoubleArrayToString(this
							.calculateCombinedColdAppliancesProfile(this.coldApplianceProfiles));
				}
				this.sampleOutput.appendCols(outputBuilder);
			}

		}

		if (this.mainContext.logger.isTraceEnabled())
		{
			this.mainContext.logger.trace("  HHpro: getHasElectricalSpaceHeat: " + this.isHasElectricalSpaceHeat());
		}

		if (this.isHasElectricalSpaceHeat())
		{
			this.setPoint = this.optimisedSetPointProfile[this.timeOfDay];
		}

		double potentialElecDemand = this.calculateHeatDemandAndInternalTemp(this.time);

		// Every step we do these actions
		if (this.hasSmartControl)
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Setting demand via smart control");
			}
			this.setNetDemand(this.smartDemand(this.time, potentialElecDemand) - this.currentGeneration());
		}
		else if (this.hasSmartMeter && this.exercisesBehaviourChange)
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Setting demand via elasticity and smart meter");
			}
			this.learnBehaviourChange();
			this.setNetDemand(this.evaluateElasticBehaviour(this.time));
			this.learnSmartAdoptionDecision(this.time);
		}
		else
		{
			if (this.mainContext.logger.isTraceEnabled())
			{
				this.mainContext.logger.trace("Setting demand with no smart control or elasticity");
			}

			// No adaptation case - Note that as there has been no adaptation
			// above, smartDemand will simply return the base
			// setNetDemand(arr_otherDemandProfile[time %
			// arr_otherDemandProfile.length] - currentGeneration());
			this.setNetDemand(this.smartDemand(this.time, potentialElecDemand) - this.currentGeneration());
			// learnSmartAdoptionDecision(time);
		}

		// After the heat input has been calculated, re-calculate the internal
		// temperature of the house
		this.recordInternalAndExternalTemp(this.time);

		if (this.mainContext.logger.isTraceEnabled())
		{
if (		this.mainContext.logger.isTraceEnabled()) {
		this.mainContext.logger.trace("     -------- HouseholdProsumer: END ---------- DayCount: " + this.mainContext.getDayCount()
				+ ",Timeslot: " + this.mainContext.getTimeslotOfDay() + ",TickCount: " + this.mainContext.getTickCount());
}
		}

	}

	/**
	 * Constructor
	 * 
	 * Creates a prosumer agent representing a household within the given
	 * context and with a basic demand profile as passed into the constructor.
	 * 
	 * @param context
	 *            - the context within which this agent exists
	 * @param otherDemandProfile
	 *            - a floating point array containing the misc base demand
	 *            (lightening, entertainment, computers, small appliances) for
	 *            this prosumer. Can be arbitrary length.
	 * 
	 */
	public HouseholdProsumer(CascadeContext context, double[] otherDemandProfile)
	{
		super(context);
		this.initialiseHouseholdProsumer(context, otherDemandProfile);

	}

	public void initialiseHouseholdProsumer(CascadeContext context, double[] otherDemandProfile)
	{
		// all the immutable must be initialized here
		// miscDemand includes aggregate of ligtening, entertainment, computers,
		// & small appliances (Kettle, Toaster, Microwave)

		this.setAgentName("Household_" + this.agentID);
		this.mainContext.ticksPerDay = context.getNbOfTickPerDay();

		// this.lengthOfDemandProfile =
		// this.mainContext.ticksPerDay*Consts.NB_OF_DAYS_LOADED_DEMAND;
		this.lengthOfDemandProfile = otherDemandProfile.length;

		if (otherDemandProfile.length % this.mainContext.ticksPerDay != 0)
		{
			System.err.println("HouseholdProsumer: baseDemand array not a whole number of days");
			System.err.println("HouseholdProsumer: Will be truncated and may cause unexpected behaviour");
		}

		// this.arr_otherDemandProfile = new double [otherDemandProfile.length];

		this.arr_otherDemandProfile = new double[this.lengthOfDemandProfile];

		// System.arraycopy(otherDemandProfile, 0, this.arr_otherDemandProfile,
		// 0, otherDemandProfile.length);

		System.arraycopy(otherDemandProfile, 0, this.arr_otherDemandProfile, 0, this.lengthOfDemandProfile);

		// Initialise the smart optimised profile to be the same as base demand
		// smart controller will alter this
		// this.smartOptimisedProfile = new double [otherDemandProfile.length];

		this.smartOptimisedProfile = new double[this.lengthOfDemandProfile];

		// System.arraycopy(otherDemandProfile, 0, this.smartOptimisedProfile,
		// 0, smartOptimisedProfile.length);
		System.arraycopy(otherDemandProfile, 0, this.smartOptimisedProfile, 0, this.smartOptimisedProfile.length);

		// this.dailyElasticity = new double[this.mainContext.ticksPerDay];
		this.dailyElasticity = new double[this.lengthOfDemandProfile];

		// Arrays to hold some history - mainly for gui purposes
		this.historicalBaseDemand = new double[this.mainContext.ticksPerDay];
		this.historicalWetDemand = new double[this.mainContext.ticksPerDay];
		this.historicalColdDemand = new double[this.mainContext.ticksPerDay];
		this.historicalSpaceHeatDemand = new double[this.mainContext.ticksPerDay];
		this.historicalEVDemand = new double[this.mainContext.ticksPerDay];
		this.historicalWaterHeatDemand = new double[this.mainContext.ticksPerDay];
		this.historicalIntTemp = new double[this.mainContext.ticksPerDay];
		this.historicalExtTemp = new double[this.mainContext.ticksPerDay];
		this.recordedHeatPumpDemand = new double[this.mainContext.ticksPerDay];
		this.dayHistoryHeatingEnergy = new double[this.mainContext.ticksPerDay];

		this.waterHeatEnergyDemandProfile = new double[this.mainContext.ticksPerDay];

		this.coldApplianceProfiles = new WeakHashMap<String, double[]>();

		this.initialiseSpaceHeatParams();

	}

	private void initialiseSpaceHeatParams()
	{
		this.setBuildingHeatLossRate(this.mainContext.buildingLossRateGenerator.nextDouble());
		this.setBuildingThermalMass(this.mainContext.thermalMassGenerator.nextDouble());

		// tau is effectively the time constant of the building and has units of
		// seconds
		this.tau = (this.buildingThermalMass * Consts.KWH_TO_JOULE_CONVERSION_FACTOR) / this.buildingHeatLossRate;

		// this multiplier is dimensionless and is used to calculate how much
		// temperature is gained / lost
		// by the building for a given temperature difference between inside and
		// outside (delta T)
		this.freeRunningTemperatureLossPerTickMultiplier = (Consts.SECONDS_PER_DAY / this.mainContext.ticksPerDay) / this.tau;

		this.setPointProfile = new double[this.mainContext.ticksPerDay];
		// this.setPointProfile = new double[this.lengthOfDemandProfile];

		Arrays.fill(this.setPointProfile, 20); // This puts in a flat set point
												// through the day set by the
												// consumer
		// this.setPointProfile =
		// Arrays.copyOf(Consts.BASIC_AVERAGE_SET_POINT_PROFILE,
		// Consts.BASIC_AVERAGE_SET_POINT_PROFILE.length);
		// this.setPointProfile = ArrayUtils.offset(this.setPointProfile,
		// (double) RandomHelper.nextDoubleFromTo(-2, 2));
		this.optimisedSetPointProfile = Arrays.copyOf(this.setPointProfile, this.setPointProfile.length);
		this.setPoint = this.optimisedSetPointProfile[0];
		this.currentInternalTemp = this.setPoint;
	}

	/**
	 * @param generateBEVProfile
	 */
	public void setEVProfile(double[] EVProfile)
	{
		this.electricVehicleProfile = EVProfile;
		this.optimisedEVProfile = EVProfile;
	}

	/**
	 * @param generateBEVProfile
	 */
	public void setOptimisedEVProfile(double[] EVProfile)
	{
		this.optimisedEVProfile = EVProfile;
	}

	/**
	 * @return
	 */
	public double[] getEVProfile()
	{
		return Arrays.copyOf(this.electricVehicleProfile, this.electricVehicleProfile.length);
	}

	/***
	 * Test for Richard - need no arg constructor for shapefile load.
	 */
	public HouseholdProsumer()
	{
		super();
	}

}
