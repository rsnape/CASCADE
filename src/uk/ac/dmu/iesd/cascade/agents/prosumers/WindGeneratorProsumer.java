package uk.ac.dmu.iesd.cascade.agents.prosumers;

import repast.simphony.essentials.RepastEssentials;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.1 $ $Date: 2011/05/18 12:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 1.0 - Initial split of categories of prosumer from the abstract
 * class representing all prosumers
 * 1.1 - eliminated redundant methods (inherited from superclass ProsumerAgent)
 */
public class WindGeneratorProsumer extends GeneratorProsumer {

	/*
	 * Configuration options
	 * 
	 * All are set false initially - they can be set true on instantiation to
	 * allow fine-grained control of agent properties
	 */
	int numTurbines;
	/*
     * New Wind Turbine Generator by STS
     * This utilises weather data to determine the power output of a turbine.
     * At this stage only considering horizontal axis turbines.
     */
    double obsHeight; // Windspeed observation height (default 10m).
    double windDirection;
    double [] hubHeight; // Height of the hub of the wind turbine [m] - should relate to the blade length and therefore the area covered by rotor (Ar).
    boolean available; // Used to determine if the turbine is available or not.
    double Pw;  // Power extracted from wind [W].
    double rho; // Air density [kg/m3].
    double lambda; // Tip speed ratio.
    double lambda_i;
    double vt; // Blade tip speed [m/s].
    double [] vw; // Wind speed at hub height [m/s].
    double theta; // Pitch angle of rotor blades [û].
    double Ar; // Area covered by rotor [m2].
    double Zo; // Roughness Length [m]
    double obukhovLength; // Length in terms of classifying stability
    double beta = 4.8;
    double gamma = 19.3; // Not utilised
    double [] stability; //Atmospheric stability
    double [] location = new double [2]; // latitude, longitude
    double [] bladeLength; // Length of blade from hub to tip (all turbines assumed horizontal axis 3-blade)
    double [] maxPower; // The maximum power capacity of the turbine
    double [] cutInWindSpeed; //Wind speed below which the turbine is not operating
    double [] shutDownWindSpeed; //Wind Speed above which the turbine is not operating
    double [] efficiency;//efficiency of the turbine in converting potential wind power (0 - 1.0)
    
    
    /*
     * This is a short-term solution to offset the time in reading the weather data. In
     * future it is intended that the weather data will be determined for the given location 
     * (i.e. worked out according to the latitude and longitude of the site.
     */
    public int offset;
	
    /*
     * Set turbine efficiency
     */
    private void setEfficiency(double [] cp){
    	efficiency = cp;
    }
    
    /*
     * Set Individual turbine efficiency
     */
    private void setEfficiency(int i, double cp){
    	efficiency[i] = cp;
    }
    
    /*
     * Get turbine efficiencies
     */
    private double [] getEfficiency(){
    	return efficiency;
    }
    
    /*
     * Get individual turbine efficiency
     */
    private double getEfficiency(int i){
    	return efficiency[i];
    }
    
    /*
     * Set the maximum operating wind speeds for all turbines
     */
    private void setShutDownWindSpeed(double [] cW){
    	shutDownWindSpeed = cW;
    }
    
    /*
     * Set the maximum operating wind speed of a given turbine
     */
    private void setShutDownWindSpeed(int i, double cW){
    	shutDownWindSpeed[i] = cW;
    }
    
    /*
     * Get the maximum operating wind speeds for each turbine
     */
    private double getShutDownWindSpeed(int i){
    	return shutDownWindSpeed[i];
    }
    
    /*
     * Get the maximum operating wind speeds for all turbines
     */
    private double [] getShutDownWindSpeed(){
    	return shutDownWindSpeed;
    }
    
    
    /*
     * Set the minimum operating wind speeds for all turbines
     */
    private void setCutInWindSpeed(double [] cW){
    	cutInWindSpeed = cW;
    }
    
    /*
     * Set the minimum operating wind speed of a given turbine
     */
    private void setCutInWindSpeed(int i, double cW){
    	cutInWindSpeed[i] = cW;
    }
    
    /*
     * Get the minimum operating wind speeds for each turbine
     */
    private double getCutInWindSpeed(int i){
    	return cutInWindSpeed[i];
    }
    
    /*
     * Get the minimum operating wind speeds for all turbines
     */
    private double [] getCutInWindSpeed(){
    	return cutInWindSpeed;
    }
    
    /*
     * Set the maximum power rating of the turbine in Watts
     */
    private void setMaxPower(double [] mP){
    	maxPower = mP;
    	double sum = 0.0;
    	for (int i=0; i<mP.length; i++){
    		 sum = sum + mP[i];
    	}
    	this.ratedPowerWind = sum;
    }
    
    /*
     * Set the maximum power rating of a given turbine in Watts
     */
    private void setMaxPower(int i, double mP){
    	maxPower[i] = mP;
    }
    
    /*
     * Get the max Power rating of a given turbine in Watts
     */
    private double getMaxPower(int i){
    	return maxPower[i];
    }
    
    /*
     * Get the max power ratings of all turbines in Watts
     */
    private double [] getMaxPower(){
    	return maxPower;
    }
    
    /*
     * Set the location of this wind farm generator
     */
    private void setLocation(double latitude, double longitude){
    	location[0] = latitude;
    	location[1] = longitude;
    }
    
    /*
     * Get the location of this wind farm generator
     */
    private double[] getLocation(){
    	return location;
    }
    
    /*
     * Get the latitude point of the wind farm only
     */
    private double getLatitude(){
    	return location[0];
    }
    
    /*
     * Get the longitude point of the wind farm only
     */
    private double getLongitude(){
    	return location[1];
    }
    
    /*
     * Set the length of blades for each turbine
     */
    private void setBladeLength(double [] bL){
    	bladeLength = bL;
    }
    
    /*
     * Set a particular turbines blade length 
     */
    private void setBladeLength(int i, double bL){
    	bladeLength[i] = bL;
    }
    
    /*
     * Get the blade length of all turbines
     */
    private double [] getBladeLengths(){
    	return bladeLength;
    }
    
    /*
     * Get the blade length of a particular turbine
     */
    private double getBladeLength(int i){
    	return bladeLength[i];
    }
    
    /* Monitored value of windspeed corrected for terrain type affecting wind stability of flow
     * and height of turbine hub being different to that of height ofwind observation station. 
     */
	private void setWindSpeedAtHubHeight(){
        // This is from the IOP Journal of Physics Conference Series 75 (2007) paper
        // titled Influence of different wind profiles due to varying atmospheric stability on the
        // fatigue life of wind turbines. Which is derived from Roland B. Stull. An Introduction to Boundary Layer Meteorology pp 383 - 386
		
		for (int i=0; i<numTurbines; i++){
			setStability(i);
			vw[i] = getWindSpeed()*( (Math.log(hubHeight[i]/Zo) - stability[i]*(hubHeight[i]/obukhovLength)) / (Math.log(obsHeight/Zo) - stability[i]*(obsHeight/obukhovLength)) );
		}
	}
	
	/*
	 * Sets the Obukhov length relating to the stability of the wind flow over given terrain.
	 * The current chart is taken from IOP Conf paper that takes it from another study ( based on off-shore values)
	 */
	private void setObukhovLength(int stable) {
        
        // random value from length range according to stability type
        switch(stable) {
            case 0 : //This is if very stable
                obukhovLength = 0;//random value between 0 and 200
                break;
            case 1 : //This is if stable
                obukhovLength = 200; //random value between 200 and 1000
                break;
            case 2 : //This is if near neutral
                obukhovLength = 1000; // modulus value greater than 1000 - considered positive for simplicity of stability function (i.e. condition z/L > 0 must be met)
            case 3 : //This is if unstable
                obukhovLength = -200; // random value between -1000 and -200
            case 4: //This is very unstable
                obukhovLength = -1; // random value between -200 and 0  
        }
        
    }
	
	/*
	 * Stability set using the Businger-Dyer formulation
	 */
    private void setStability(int i) {
        if ((hubHeight[i]/obukhovLength) > 0) {
            stability[i] = -1*beta*(hubHeight[i]/obukhovLength);
        }
        else if ((hubHeight[i]/obukhovLength) < 0) {
            double x =  Math.pow(1-(15*hubHeight[i]/obukhovLength),0.25); 
            stability[i] = 2*Math.log((1+x)/2) + Math.log((1+Math.pow(x,2))/2) - 2*Math.atan(x) + (Math.PI/2);
        }
        else { // Statically neutral flow z/L = 0
            stability[i] = 0;
        }
    }
	
	/*
	 * Setting the surface roughness according to the type of terrain, to then be used
	 * in calculating wind speed at hub height of wind turbine.
	 */
    private void setSurfaceRoughness(int Terrain){
        //The following values taken from WebMET.com - meterological resource centre
            switch (Terrain) {
                case 0: //This is for offshore turbines
                    Zo = 0.0002; 
                    break;
                case 1: //This is for open flat land no obstacles
                    Zo = 0.005;
                    break;
                case 2: //This is for open flat land with some isolated obstacles
                    Zo = 0.03;
                    break;
                case 3: //This is for farm land with some large obstacles
                    Zo = 0.1;
                    break;
                case 4: //This is for farm land (high crops) and scattered obstacles
                    Zo = 0.25;
                    break;
                case 5: //This is for parkland
                    Zo = 0.5;
                    break;
                case 6: //This is for forested or suburban areas
                    Zo = 1.0;
                    break;
                }
        }
    
    private void setHubHeight(double [] H){
    	hubHeight = H;
    }
    
    private double getSurfaceRoughness() {
        return Zo;
    }
    
    
    //Equation from IEEE Transactions on Power Systems Vol. 18. No. 1, 2003
   // Pw = 0.5*rho*Ar*cp*(vw^3);
    
    // cp is a function of lamda and theta with a reasonable approximation
    // for various wind turbine types. According to IEEE ToPS 18(1), 2003 paper.
    
   // cp = 0.73*( (151/lamda_i) - (0.58*theta) - (0.002*(theta^2.14)) - 13.2)*EXP(-18.4/lambda_i);
    
  //  lambda_i = 1 / ( (1/(lambda - 0.02*theta)) - (0.003/(theta^3 +1)) );

	/*
	 * TODO - need some operating characteristic parameters here - e.g. time to
	 * start ramp up generation etc. etc.
	 */
	/*
	 * Accessor functions (NetBeans style) TODO: May make some of these private
	 * to respect agent conventions of autonomy / realistic simulation of humans
	 */

	/**
	 * Returns a string representing the state of this agent. This method is
	 * intended to be used for debugging purposes, and the content and format of
	 * the returned string should include the states (variables/parameters)
	 * which are important for debugging purpose. The returned string may be
	 * empty but may not be <code>null</code>.
	 * 
	 * @return a string representation of this agent's state parameters
	 */
	protected String paramStringReport() {
		String str = "";
		return str;

	}
/*
	public double getUnadaptedDemand() {
		// Cope with tick count being null between project initialisation and
		// start.
		int index = Math
				.max(((int) RepastEssentials.GetTickCount() % arr_otherDemandProfile.length),
						0);
		return (arr_otherDemandProfile[index]) - currentGeneration();
	}
*/
	/*
	 * Step behaviour
	 */

	/******************
	 * This method defines the step behaviour of a wind generator agent This
	 * represents a farm of 1 or more turbines i.e. anything that is purely wind
	 * - can be a single turbine or many
	 * 
	 * Input variables: none
	 * 
	 * Return variables: none
	 ******************/
	//@ScheduledMethod(start = 0, interval = 1, priority = Consts.PROSUMER_PRIORITY_FIFTH) //shuffle = true)
	public void step() {
		// Define the return value variable. Set this false if errors
		// encountered.
		boolean returnValue = true;

		// Note the simulation time if needed.
		// Note - Repast can cope with fractions of a tick (a double is
		// returned)
		// but I am assuming here we will deal in whole ticks and alter the
		// resolution should we need
		int time = (int) RepastEssentials.GetTickCount();
		int timeOfDay = (time % this.mainContext.ticksPerDay);
		CascadeContext myContext = this.getContext();

		if ((time - offset) < 0 ) {
			checkWeather(0);
		}
		else {
			checkWeather(time - offset);
		}
		/*
		 * Set wind speed at each turbines hub height
		 */
		setWindSpeedAtHubHeight();

		// Do all the "once-per-day" things here
		//if (timeOfDay == 0) {
		//	inelasticTotalDayDemand = calculateFixedDayTotalDemand(time);
		//}

		/*
		 * As wind is non-dispatchable, simply return the current generation
		 * each time. The only adaptation possible would be shutting down some /
		 * all turbines
		 * 
		 * TODO - implement turbine shutdown
		 */
		setNetDemand(0 - currentGeneration());

		// Return (this will be false if problems encountered).
		// return returnValue;

	}

	/**
	 * @return
	 */
	protected double currentGeneration() {
		double returnAmount = 0;
		// There is potential in this structure to provide an individual power output for each turbine owned by the generator agent.
		// Currently they are all set to be the same.
		for (int i=0; i<this.numTurbines; i++){
			returnAmount = returnAmount + windGeneration(i, bladeLength[i], efficiency[i], cutInWindSpeed[i], shutDownWindSpeed[i], maxPower[i]);
		}
/*		if (Consts.DEBUG) {
			if (returnAmount != 0) {
				 System.out.println("WindGeneratorProsumer: Generating " + returnAmount);
			}
		}
*/
		return returnAmount;
	}

	/**
	 * @return - double - containing the current generation for this wind generator [W]
	 * @param bladeLength - double - containing length of blade (radius of turbine) [m]
	 * @param cp -  double - Performance coefficient or power coefficient [-]
	 * @param cutInWindSpeed - double - speed of wind below which the turbine does not operate [m/s]
	 * @param shutDownWindSpeed - double - speed of wind above which the turbine is switched off [m/s]
	 * @param rPower - double - rated power of the turbine [W]
	 */
	private double windGeneration(int i, double bL, double cp, double cW, double sW, double rPower ) {
		if (hasWind) {
			Ar = Math.PI * Math.pow(bL, 2);
            if(vw[i] > cW && vw[i] < sW) {
                this.Pw = 0.5*getAirDensity()*Ar*cp*Math.pow(vw[i], 3); 
            }
            else {
            	this.Pw = 0.0;
            }
		}
        else {
        	this.Pw = 0.0;
        }
		
		// Simple check to ensure the turbine doesn't exceed the rated maximum Power of the turbine. 
		if (this.Pw > rPower) this.Pw = rPower;
            
        return this.Pw ;
		
	}

	/**
	 * @param time
	 * @return double giving sum of baseDemand for the day.
	 */
/*	private double calculateFixedDayTotalDemand(int time) {
		int baseProfileIndex = time % arr_otherDemandProfile.length;
		return ArrayUtils.sum(Arrays.copyOfRange(arr_otherDemandProfile,
				baseProfileIndex, baseProfileIndex + ticksPerDay - 1));
	}
*/
	/*
	 * Constructor function(s)
	 */
	public WindGeneratorProsumer(CascadeContext context, double[] baseDemand,
			double capacity) {
		/*
		 * If number of wind turbines not specified, assume 1
		 */
		this(context, baseDemand, capacity, 1);
	}

	public WindGeneratorProsumer(CascadeContext context, double[] baseDemand,
			double capacity, int turbines) {
		
		
		super(context);
		
		this.hasWind = true;
		this.ratedPowerWind = capacity;
		this.numTurbines = turbines;
		this.percentageMoveableDemand = 0;
		this.maxTimeShift = 0;
		if (baseDemand.length % this.mainContext.ticksPerDay != 0) {
			System.err.print("Error/Warning message from "+this.getClass()+": BaseDemand array not a whole number of days.");
			System.err.println("WindGeneratorProsumer: Will be truncated and may cause unexpected behaviour");
		}
		this.arr_otherDemandProfile = new double[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.arr_otherDemandProfile, 0,
				baseDemand.length);
		// Initialise the smart optimised profile to be the same as base demand
		// smart controller will alter this
		this.smartOptimisedProfile = new double[baseDemand.length];
		System.arraycopy(baseDemand, 0, this.smartOptimisedProfile, 0,
				smartOptimisedProfile.length);
	}
	
	public void setUpWindFarm(double lat, double lon, int nTurbines, int terrain, int stable, double [] heights, double[] eff, double [] minWS, double [] maxWS, double [] capacity, double [] bL) {
		
		// Set the size of arrays according to number of Turbines.
		this.vw = new double[nTurbines];
		this.hubHeight = new double[nTurbines];
	    this.stability = new double[nTurbines]; 
	    this.bladeLength = new double[nTurbines]; 
	    this.maxPower = new double[nTurbines]; 
	    this.cutInWindSpeed = new double[nTurbines]; 
	    this.shutDownWindSpeed = new double[nTurbines]; 
	    this.efficiency = new double[nTurbines];
	    
	    this.obsHeight = 10;
	   
	    //this.offset = 0; // All wind farms currently have same offset so located in same region (i.e. same weather profile).
		
		/*
		 * Set location of wind farm
		 */
		setLocation(lat, lon);
		
		/*
		 * Set the number of turbines
		 */
		this.numTurbines = nTurbines;
		
		/*
		 * Set the hub height(s)
		 */
		setHubHeight(heights);
		
		/*
		 * Set blade Length(s)
		 */
		setBladeLength(bL);
		
		/*
		 * Set the Obukhov length
		 */
		setObukhovLength(stable);
		
		/*
		 * Set the surface roughness
		 */
		setSurfaceRoughness(terrain);
		
		/*
		 * Set the maximum rated capacity of each turbine
		 */
		setMaxPower(capacity);
		
		setEfficiency(eff);
		
		setCutInWindSpeed(minWS);
		
		setShutDownWindSpeed(maxWS);
		
		
	}

}
