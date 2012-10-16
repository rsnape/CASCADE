/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.WeakHashMap;
import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;

/**
 * A suite of profile generating methods for various appliance types.
 * 
 * Based on the work of Melody Stokes, see her thesis (2004-ish?)
 * 
 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
 * conversion to kWh.
 * 
 * @author jsnape
 */
public class MelodyStokesGenerator extends BasicProfileGenerator {

	@Override
	public boolean generatesTotalProfile() {
		return true;
	}

	@Override
	public boolean generatesWetProfile() {
		return true;
	}

	@Override
	public boolean generatesColdProfile() {
		return true;
	}

	@Override
	public boolean generatesBrownProfile() {
		return true;
	}

	@Override
	public boolean generatesLightingProfile() {
		return true;
	}

	@Override
	public boolean generatesCookingProfile() {
		return true;
	}

	@Override
	public boolean generatesSpaceHeatProfile() {
		return true;
	}

	@Override
	public boolean generatesWaterHeatProfile() {
		return true;
	}

	@Override
	public boolean generatesMiscProfile() {
		return true;
	}

	@Override
	public double[] getTotalProfile() {
		// TODO Auto-generated method stub
		return super.getTotalProfile();
	}

	@Override
	public double[] getWetProfile() {
		// TODO Auto-generated method stub
		return super.getWetProfile();
	}

	@Override
	public double[] getColdProfile() {
		// TODO Auto-generated method stub
		return super.getColdProfile();
	}

	@Override
	public double[] getBrownProfile() {
		// TODO Auto-generated method stub
		return super.getBrownProfile();
	}

	@Override
	public double[] getLightingProfile() {
		// TODO Auto-generated method stub
		return super.getLightingProfile();
	}

	@Override
	public double[] getCookingProfile() {
		// TODO Auto-generated method stub
		return super.getCookingProfile();
	}

	@Override
	public double[] getSpaceHeatProfile() {
		// TODO Auto-generated method stub
		return super.getSpaceHeatProfile();
	}

	@Override
	public double[] getWaterHeatProfile() {
		// TODO Auto-generated method stub
		return super.getWaterHeatProfile();
	}

	@Override
	public double[] getMiscProfile() {
		// TODO Auto-generated method stub
		return super.getMiscProfile();
	}

	/**
	 * Helper method to use boolean indicators of ownership which simply calls the
	 * method of same name with integer arguments translating true to 1 and false to 0
	 * 
	 * @param numDays
	 * @param fridges
	 * @param fridgeFreezers
	 * @param freezers
	 * @return
	 */
	public static WeakHashMap<String,double[]> melodyStokesColdApplianceGen(int numDays, boolean fridges, boolean fridgeFreezers, boolean freezers)
	{
		//if (Consts.DEBUG) System.out.println("Fridge; FridgeFreezer; Freezer"+ fridges  +" "+ fridgeFreezers + " "+ freezers); 

		//return melodyStokesColdApplianceGen(numDays, fridges ? 1 : 0, freezers ? 1:0, fridgeFreezers ? 1:0);

		return melodyStokesColdApplianceGen(numDays, fridges ? 1 : 0, fridgeFreezers ? 1:0, freezers ? 1:0);

	}

	/**
	 * Helper function to generate only cold appliance profiles for backward compatibility
	 */
	public static WeakHashMap<String,double[]> melodyStokesColdApplianceGen(int numDays, int fridges, int fridgeFreezers, int freezers)
	{
		MSHalfHourProfile coldOnlyProfile = new MSHalfHourProfile();
		MSHouseholdCharacteristics coldHouseChars = new MSHouseholdCharacteristics();
		coldHouseChars.fridge_marker = !(fridges == 0);
		coldHouseChars.fridge_num = fridges;
		coldHouseChars.ff_marker = !(fridgeFreezers == 0);
		coldHouseChars.ff_num = fridgeFreezers;
		coldHouseChars.freezer_marker = !(freezers == 0);
		coldHouseChars.freezer_num = freezers;
		
		//Sort out start date and length etc.
		Date[] startAndEnd = dates_1();
		Date start = startAndEnd[0];		
		Calendar testCal = new GregorianCalendar();
		testCal.setTime(start);
		//Note - store the (int) day of year as a double as it is used throughout
		//the original code in a division which returns a fractional result - 
		//Matlab is int / double agnostic.
		double startDayofYear = testCal.get(Calendar.DAY_OF_YEAR);

		// Note - Java returns days of week as 1-7, Sunday == 1, Saturday == 7
		int startDayofWeek = testCal.get(Calendar.DAY_OF_WEEK);
		
		cooling(coldOnlyProfile, startDayofYear, startDayofWeek, numDays);
		HH_specific(coldHouseChars, false, coldOnlyProfile, start, numDays);
				
		double[] d_fridge = ArrayUtils.multiply(coldOnlyProfile.D_fridge, fridges);
		double[] d_freezer = ArrayUtils.multiply(coldOnlyProfile.D_freezer, fridges);
		double[] d_fridge_freezer = ArrayUtils.multiply(coldOnlyProfile.D_fridge_freezer, fridges);
	
		WeakHashMap<String,double[]> coldProfiles = new WeakHashMap<String,double[]>();

		//Convert kW to kWh
		d_fridge = convertToKWh(d_fridge, true);
		d_freezer = convertToKWh(d_freezer, true);
		d_fridge_freezer = convertToKWh(d_fridge_freezer, true);

		coldProfiles.put(Consts.COLD_APP_FRIDGE, d_fridge);
		coldProfiles.put(Consts.COLD_APP_FREEZER, d_freezer);
		coldProfiles.put(Consts.COLD_APP_FRIDGEFREEZER, d_fridge_freezer);
		coldProfiles.put(Consts.COLD_APP_FRIDGE_ORIGINAL, Arrays.copyOf(d_fridge,d_fridge.length));
		coldProfiles.put(Consts.COLD_APP_FREEZER_ORIGINAL, Arrays.copyOf(d_freezer, d_freezer.length));
		coldProfiles.put(Consts.COLD_APP_FRIDGEFREEZER_ORIGINAL, Arrays.copyOf(d_fridge_freezer,d_fridge_freezer.length));

		return coldProfiles;
	}



	/**	
	 * Java implementation of the matlab code from Melody Stokes' model of
	 * cold appliance demand (washing.m).  Note that this implementation does not account for leap years and 
	 * simply assumes that days per year or where in the year the simulation starts
	 * In effect, it assumes a 1st January start.
	 * 
	 * TODO: If this is to be used extensively, need to make it sensitive to start date etc.
	 * 
	 * @param daysPerYear
	 * @param hasWashingMachine
	 * @param hasWasherDryer
	 * @param hasDishWasher
	 * @param hasTumbleDryer
	 * @return
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	public static double[] melodyStokesWetApplianceGen(int numDays,
			boolean washMachine, boolean washerDryer,
			boolean dishWasher, boolean tumbleDryer) {
		// TODO Auto-generated method stub
		return 	melodyStokesWetApplianceGenWithWeekends(numDays, washMachine ? 1 : 0, washerDryer ? 1:0, dishWasher ? 1:0, tumbleDryer ? 1:0);
	}

	/**
	 * 	
	 * Java implementation of the matlab code from Melody Stokes' model of
	 * cold appliance demand (washing.m).  Note that this implementation does not account for leap years and 
	 * simply assumes that days per year or where in the year the simulation starts
	 * In effect, it assumes a 1st January start.
	 * 	 * 
	 * Note - this model does not take account of different values for Economy 7 (E7) tariff
	 * as this is the effect we hope to model in our wider experiment. Hence, unrestricted (UR)
	 * values are used for the inital profile generation.
	 * 
	 * TODO: If this is to be used extensively, need to make it sensitive to start date etc.
	 * 
	 * @param numDays
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 * @return
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	private static double[]  melodyStokesWetApplianceGenWithWeekends(int numDays, int washMach,
			int washDry, int dishWash, int tumbleDry) {
		// nasty implementation that assumes this starts 1st Jan and that's a Sunday
		// TODO: refine days of week. Possibly add start date to context and maintain day of week etc in there too
		//this sub-model is for half-hourly electricty demand for washing appliances
		//it covers washers, dryers, washer-dryers combined and dishwashers
		
		MSHalfHourProfile wetOnlyProfile = new MSHalfHourProfile();
		MSHouseholdCharacteristics wetHouseChars = new MSHouseholdCharacteristics();
		wetHouseChars.wash_marker = !(washMach == 0);
		wetHouseChars.washer_num = washMach;
		wetHouseChars.wash_dry_marker = !(washDry == 0);
		wetHouseChars.wash_dry_num = washDry;
		wetHouseChars.dish_marker = !(dishWash == 0);
		wetHouseChars.dish_num = dishWash;
		wetHouseChars.dryer_marker = !(tumbleDry == 0);
		wetHouseChars.dryer_num = tumbleDry;
		
		//Sort out start date and length etc.
		Date[] startAndEnd = dates_1();
		Date start = startAndEnd[0];		
		Calendar testCal = new GregorianCalendar();
		testCal.setTime(start);
		//Note - store the (int) day of year as a double as it is used throughout
		//the original code in a division which returns a fractional result - 
		//Matlab is int / double agnostic.
		double startDayofYear = testCal.get(Calendar.DAY_OF_YEAR);

		// Note - Java returns days of week as 1-7, Sunday == 1, Saturday == 7
		int startDayofWeek = testCal.get(Calendar.DAY_OF_WEEK);
		
		washing(wetOnlyProfile, startDayofYear, startDayofWeek, numDays);
		HH_specific(wetHouseChars, false, wetOnlyProfile, start, numDays);
		
		double[] d_washer_UR = ArrayUtils.multiply(wetOnlyProfile.D_washer_UR, washMach);
		double[] d_dryer_UR = ArrayUtils.multiply(wetOnlyProfile.D_dryer_UR, washMach);
		double[] d_dish_UR = ArrayUtils.multiply(wetOnlyProfile.D_dish_UR, washMach);

		//Convert kW to kWh
		d_washer_UR = convertToKWh(d_washer_UR, true);
		d_dryer_UR = convertToKWh(d_dryer_UR, true);
		d_dish_UR = convertToKWh(d_dish_UR, true);
		
		return ArrayUtils.add(d_washer_UR, d_dryer_UR, d_dish_UR);

	}


	/**
	 * 
	 * Java implementation of the matlab code from Melody Stokes' model of
	 * cold appliance demand (washing.m).  Note that this implementation does not account for leap years and 
	 * simply assumes that days per year or where in the year the simulation starts
	 * In effect, it assumes a 1st January start.
	 * 
	 * Note - this model does not take account of different values for Economy 7 (E7) tariff
	 * as this is the effect we hope to model in our wider experiment. Hence, unrestricted (UR)
	 * values are used for the inital profile generation.
	 * 
	 * TODO: If this is to be used extensively, need to make it sensitive to start date etc.
	 * 
	 * @param numDays
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 * @return
	 * 
	 * NOTE: Values for this are given in kW.  Depending on your end use, these may require
	 * conversion to kWh.
	 */
	private static WeakHashMap<String,double[]> melodyStokesWetApplianceGenWithDutyCycles(CascadeContext context,int numDays, int washMach,
			int washDry, int dishWash, int tumbleDry) {
		// Each day of week is treated the same way! 
		//this sub-model is for half-hourly electricty demand for washing appliances
		//it covers washers, dryers, washer-dryers combined and dishwashers

		//final double[]  wet_pdf = {18.91,16.45,13.49,12.52,16.80,14.41,11.13,9.99,13.90,10.18,13.30,15.53,18.79,17.65,21.79,25.72,36.83,43.13,43.94,46.43,49.61,52.02,49.30,45.71,42.85,42.42,39.08,39.67,41.19,40.16,37.68,37.56,37.67,38.10,38.19,37.10,36.46,37.32,39.44,37.77,37.05,35.09,35.13,34.19,29.75,26.68,26.01,21.30};
		//EmpiricalWalker wetApplProbDistGenerator = RandomHelper.createEmpiricalWalker(wet_pdf, Empirical.NO_INTERPOLATION); 

		//ChartUtils.testProbabilityDistAndShowHistogram(wetApplProbDistGenerator, 10000, 48);  //test to make sure the prob dist generate desired outcomes

		double[]  d_washer_UR = new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[]  d_dryer_UR =new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];
		double[]  d_dish_UR =new double[numDays * Consts.MELODY_MODELS_TICKS_PER_DAY];

		double[]  scale_washer_wkdays_UR={0.002d, 0.001d, 0d, 0d, 0.022d, 0.01d, 0d, 0.003d, 0.026d, 0d, 0d, 0.005d, 0.002d, 0.004d, 0.029d, 0.013d, 0.046d, 0.029d, 0.006d, 0.023d, 0.009d, 0.029d, 0.019d, 0.014d, 0.014d, 0.009d, 0.007d, 0.022d, 0.019d, 0.009d, 0.01d, 0.015d, 0.021d, 0.01d, 0.017d, 0.018d, 0.018d, 0.025d, 0.022d, 0.01d, 0.034d, 0.023d, 0.025d, 0.018d, 0.003d, 0.002d, 0.003d, 0.002d};
		double[]  phase_washer_wkdays_UR={1.4d, 1.4d, 0d, 0d, 2.4d, 2.6d, 0d, 5d, 5.4d, 0d, 0d, 2.9d, 4.2d, 2.8d, 0.5d, 0.9d, 1d, 6.2d, 2d, 5.9d, 0d, 0d, 5.9d, 0.5d, 4.6d, 4.4d, 5.2d, 0d, 5.5d, 4.9d, 5.8d, 5.4d, 6d, 5.5d, 6d, 6.2d, 6d, 4.9d, 5.3d, 6d, 6d, 0.5d, 0.7d, 0.4d, 2.5d, 1.9d, 0.6d, 0.4d};
		double[]  const_washer_wkdays_UR={0.011d, 0.005d, 0.002d, 0.001d, 0.025d, 0.016d, 0.004d, 0.003d, 0.016d, 0.004d, 0.005d, 0.02d, 0.039d, 0.032d, 0.072d, 0.081d, 0.186d, 0.262d, 0.257d, 0.279d, 0.26d, 0.239d, 0.216d, 0.18d, 0.171d, 0.164d, 0.127d, 0.116d, 0.131d, 0.116d, 0.103d, 0.108d, 0.112d, 0.127d, 0.125d, 0.123d, 0.113d, 0.114d, 0.119d, 0.098d, 0.1d, 0.094d, 0.089d, 0.08d, 0.05d, 0.036d, 0.031d, 0.016d};
		double[]  stddev_washer_wkdays_UR={0.027d, 0.018d, 0.01d, 0.006d, 0.044d, 0.038d, 0.014d, 0.007d, 0.043d, 0.014d, 0.02d, 0.039d, 0.054d, 0.041d, 0.059d, 0.071d, 0.13d, 0.148d, 0.12d, 0.133d, 0.134d, 0.134d, 0.127d, 0.117d, 0.12d, 0.126d, 0.096d, 0.099d, 0.109d, 0.089d, 0.078d, 0.086d, 0.087d, 0.099d, 0.1d, 0.094d, 0.092d, 0.096d, 0.091d, 0.082d, 0.089d, 0.082d, 0.078d, 0.086d, 0.055d, 0.049d, 0.051d, 0.031d};

		double[]  scale_dryer_wkdays_UR={0.01d, 0.005d, 0.005d, 0.004d, 0d, 0.001d, 0.001d, 0d, 0d, 0d, 0d, 0.033d, 0.035d, 0.027d, 0.025d, 0.028d, 0.03d, 0.033d, 0.05d, 0.06d, 0.075d, 0.077d, 0.069d, 0.08d, 0.088d, 0.104d, 0.095d, 0.08d, 0.09d, 0.082d, 0.068d, 0.099d, 0.094d, 0.109d, 0.093d, 0.086d, 0.081d, 0.046d, 0.019d, 0.024d, 0.038d, 0.027d, 0.01d, 0.004d, 0.01d, 0.013d, 0.02d, 0.018d};
		double[]  phase_dryer_wkdays_UR={2.8d, 3.5d, 3.4d, 4.3d, 0d, 3.7d, 3.7d, 0d, 0d, 0d, 3.7d, 5.6d, 5.6d, 5.5d, 5.7d, 6.1d, 5.9d, 4.7d, 5.1d, 4.9d, 5d, 5d, 5d, 5.2d, 5.2d, 5.3d, 5.1d, 5.1d, 5d, 4.8d, 5.1d, 5.2d, 4.9d, 5.2d, 5.3d, 5.4d, 5.1d, 5.3d, 5.3d, 0.1d, 0.1d, 0.1d, 0.6d, 5.2d, 3.1d, 2.2d, 2.5d, 2.6d};
		double[]  const_dryer_wkdays_UR={0.028d, 0.017d, 0.01d, 0.006d, 0.007d, 0.002d, 0.002d, 0.003d, 0.002d, 0.004d, 0.015d, 0.029d, 0.042d, 0.052d, 0.06d, 0.073d, 0.087d, 0.085d, 0.082d, 0.092d, 0.121d, 0.137d, 0.143d, 0.133d, 0.145d, 0.154d, 0.139d, 0.126d, 0.129d, 0.126d, 0.116d, 0.12d, 0.136d, 0.158d, 0.163d, 0.156d, 0.135d, 0.129d, 0.107d, 0.103d, 0.092d, 0.085d, 0.079d, 0.082d, 0.079d, 0.061d, 0.055d, 0.038d};
		double[]  stddev_dryer_wkdays_UR={0.062d, 0.049d, 0.033d, 0.028d, 0.043d, 0.012d, 0.016d, 0.018d, 0.013d, 0.019d, 0.05d, 0.065d, 0.076d, 0.081d, 0.08d, 0.085d, 0.102d, 0.096d, 0.101d, 0.117d, 0.118d, 0.121d, 0.125d, 0.118d, 0.138d, 0.131d, 0.134d, 0.129d, 0.141d, 0.139d, 0.121d, 0.117d, 0.124d, 0.128d, 0.131d, 0.121d, 0.119d, 0.127d, 0.117d, 0.111d, 0.101d, 0.094d, 0.096d, 0.096d, 0.09d, 0.08d, 0.078d, 0.065d};
		//dishwasher parameters
		double[]  scale_dish_UR={0.004d, 0.015d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.001d, 0.012d, 0.016d, 0d, 0d, 0.005d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0.022d, 0.022d, 0.017d, 0.014d, 0.006d, 0.016d, 0.015d, 0.017d, 0.025d, 0.009d, 0.037d, 0.024d, 0.024d, 0.001d, 0.015d, 0.012d, 0.004d, 0.005d};
		double[]  phase_dish_UR={2.6d, 2.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 2d, 1d, 0.5d, 0d, 0d, 3.4d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 5.2d, 5.1d, 5.3d, 4.7d, 5.2d, 5d, 5d, 5.5d, 5d, 5.8d, 5.7d, 0d, 0.1d, 0d, 0d, 5.7d, 4.4d, 5.5d};
		double[]  const_dish_UR={0.058d, 0.053d, 0.017d, 0.009d, 0.008d, 0.006d, 0.006d, 0.003d, 0.001d, 0.001d, 0.001d, 0.001d, 0.001d, 0.002d, 0.009d, 0.025d, 0.072d, 0.104d, 0.114d, 0.117d, 0.137d, 0.128d, 0.094d, 0.068d, 0.06d, 0.051d, 0.061d, 0.079d, 0.083d, 0.085d, 0.078d, 0.068d, 0.06d, 0.056d, 0.053d, 0.061d, 0.067d, 0.094d, 0.143d, 0.196d, 0.212d, 0.195d, 0.182d, 0.187d, 0.172d, 0.13d, 0.093d, 0.068d};
		double[]  stddev_dish_UR={0.071d, 0.053d, 0.036d, 0.032d, 0.03d, 0.023d, 0.025d, 0.016d, 0.007d, 0.006d, 0.011d, 0.013d, 0.007d, 0.015d, 0.034d, 0.053d, 0.094d, 0.114d, 0.112d, 0.106d, 0.113d, 0.11d, 0.102d, 0.088d, 0.087d, 0.073d, 0.082d, 0.098d, 0.102d, 0.112d, 0.101d, 0.094d, 0.088d, 0.081d, 0.086d, 0.095d, 0.091d, 0.095d, 0.133d, 0.146d, 0.14d, 0.134d, 0.141d, 0.132d, 0.125d, 0.111d, 0.091d, 0.08d};

		int timeslot;

		WeakHashMap<String,double[]> wetProfiles = new WeakHashMap<String,double[]>();

		for (int i = 0; i < numDays; i++)	{
			timeslot = context.wetApplProbDistGenerator.nextInt();
			d_washer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=(washMach + washDry) *  Math.max(0, scale_washer_wkdays_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_washer_wkdays_UR[timeslot])+const_washer_wkdays_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_washer_wkdays_UR[timeslot]));
			d_dryer_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=(tumbleDry + washDry) * Math.max(0, scale_dryer_wkdays_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dryer_wkdays_UR[timeslot])+const_dryer_wkdays_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_dryer_wkdays_UR[timeslot])) ;
			timeslot = context.wetApplProbDistGenerator.nextInt();
			d_dish_UR[i * Consts.MELODY_MODELS_TICKS_PER_DAY +timeslot]=dishWash * Math.max(0, scale_dish_UR[timeslot]*Math.sin((2*Math.PI*(i / Consts.DAYS_PER_YEAR))-phase_dish_UR[timeslot])+const_dish_UR[timeslot]+(RandomHelper.getNormal().nextDouble()*stddev_dish_UR[timeslot])) ;               
		}

		//Convert kW to kWh
		d_washer_UR = ArrayUtils.multiply(d_washer_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());
		d_dryer_UR = ArrayUtils.multiply(d_dryer_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());
		d_dish_UR = ArrayUtils.multiply(d_dish_UR, (double) Consts.HOURS_PER_DAY / context.getNbOfTickPerDay());

		wetProfiles.put(Consts.WET_APP_WASHER, d_washer_UR);
		wetProfiles.put(Consts.WET_APP_DRYER, d_dryer_UR);
		wetProfiles.put(Consts.WET_APP_DISHWASHER, d_dish_UR);
		wetProfiles.put(Consts.WET_APP_WASHER_ORIGINAL, Arrays.copyOf(d_washer_UR,d_washer_UR.length));
		wetProfiles.put(Consts.WET_APP_DRYER_ORIGINAL, Arrays.copyOf(d_dryer_UR,d_dryer_UR.length));
		wetProfiles.put(Consts.WET_APP_DISHWASHER_ORIGINAL, Arrays.copyOf(d_dish_UR,d_dish_UR.length));

		return wetProfiles;

	}

	/**
	 * Translation of the main Melody file (LoadModel.m).  This could be equated to
	 * a "main" type method for this class.
	 * 
	 * @param batch
	 * @param domestic
	 * @param occupancyProfiling
	 * @param commercialBuildingType
	 * @param solarThermal
	 * @param solarPV
	 */
	private static void generate(boolean batch, boolean domestic, boolean occupancyProfiling, int commercialBuildingType, boolean solarThermal, boolean solarPV, boolean oneMinRequired)
	{
		int consumer_type = 0;
		MSHalfHourProfile thisHHProfile = new MSHalfHourProfile();
		MSOneMinProfile thisOneMinProfile =  new MSOneMinProfile();

		if (!batch)
		{
			if (domestic) 
			{
				Date[] startAndEnd = dates_1();
				Date start = startAndEnd[0];
				Date end = startAndEnd[1];
				//This variable holds number of days the simulations is for
				int Nspan = (int) ((end.getTime() - start.getTime()) / (1000*60*60*24)) + 1; 


				Calendar testCal = new GregorianCalendar();
				testCal.setTime(start);
				//Note - store the (int) day of year as a double as it is used throughout
				//the original code in a division which returns a fractional result - 
				//Matlab is int / double agnostic.
				double startDayofYear = testCal.get(Calendar.DAY_OF_YEAR);

				// Note - Java returns days of week as 1-7, Sunday == 1, Saturday == 7
				int startDayofWeek = testCal.get(Calendar.DAY_OF_WEEK);

				// Day of the year on which we change BST to GMT and vice versa.
				// This is currently fixed on a rough average - could be done better from
				// startDate or using a full Calendar implementation.
				int BST_to_GMT_day = 86;
				int GMT_to_BST_day = 298;
				
				MSHouseholdCharacteristics thisHouseChars = new MSHouseholdCharacteristics();

				consumer_type = 1; //domestic
				
				HH_group(consumer_type, thisHHProfile, startDayofYear, startDayofWeek, BST_to_GMT_day, GMT_to_BST_day, Nspan);
				HH_specific(thisHouseChars, false, thisHHProfile, dates_1()[0], Nspan);
				
				double[] one_min_wash_profile;
				
				if (!occupancyProfiling){
					one_min_wash_profile =one_min(thisHouseChars, thisHHProfile, start, Nspan);
				}
				else
				{
					one_min_wash_profile = one_min_profile(thisHHProfile, startDayofWeek, 16, 36, thisHouseChars, start);
				}
				
				double[] HH_wash_with_duty_cycle = aggregate_one_min_kWh_to_half_hour_kWh(convertToKWh(one_min_wash_profile, false));
			}
			else
			{
				consumer_type = 2;

				switch(commercialBuildingType)
				{
				case 1:
					shops(consumer_type);
					break;
				case 2:
					offices(consumer_type);
					break;
				case 3:
					schools(consumer_type);
					break;
				case 4:
					hotels(consumer_type);
					break;
				case 5:
					church(consumer_type);
					break;
				default:
					//Do nothing case - also in original LoadModel.m
				}
			}

		}
		else
		{
			batch();
		}

		if (solarPV)
		{
			double[] PV_min = PV();
			double[] rem = ArrayUtils.add(thisOneMinProfile.D_min_total, ArrayUtils.negate(PV_min));
		}

		if (solarThermal)
		{
			solar_thermal();
		}

	}

	/**
	 * Not currently implemented
	 */
	private static void solar_thermal() {
	}

	/**
	 * Not currently implemented
	 * @return 
	 */
	private static double[] PV() {
		return null;
	}

	/**
	 * Not currently implemented
	 */
	private static void batch() {
	}

	/**
	 * @param consumer_type
	 */
	private static void church(int consumer_type) {
	}

	/**
	 * @param consumer_type
	 */
	private static void hotels(int consumer_type) {
	}

	/**
	 * @param consumer_type
	 */
	private static void schools(int consumer_type) {
	}

	/**
	 * @param consumer_type
	 */
	private static void offices(int consumer_type) {
	}

	/**
	 * @param consumer_type
	 */
	private static void shops(int consumer_type) {
	}

	/**
	 * 
	 * In this implementation, we will do one minute calculations on the wet appliances only
	 * 
	 * @param consumer_type
	 * @param thisHHProfile 
	 * @return 
	 */
	private static double[] one_min(MSHouseholdCharacteristics thisHouse, MSHalfHourProfile thisHHProfile, Date startDate, int Nspan) {
		return one_min_wash_generate(thisHHProfile, Nspan, startDate.getYear(), thisHouse.washer_num, thisHouse.dryer_num, thisHouse.wash_dry_num, thisHouse.dish_num);
	}

	/**
	 * Translation of HH_group.m
	 * 
	 * this module runs the half-hourly demand programmes to calculate the group average 
	 * demands
	 * 
	 * @param consumer_type
	 * @param thisHHProfile 
	 */
	private static void HH_group(int consumer_type, MSHalfHourProfile thisHHProfile, double startDayofYear, int startDayofWeek, int BST_to_GMT_day, int GMT_to_BST_day, int Nspan) 
	{
		if (consumer_type == 1)
		{
			// Note - we call all the methods here.  In reality, we may want to populate
			// only some of the arrays - for instance calculating water and heat load is
			// wasted effort if we do not use them.


			//			thisHHProfile.d_lights = melodyStokesDomesticLightingLoadGen(startAndEnd[0], startAndEnd[1]);
			// 			thisHHProfile.d_cook = melodyStokesDomesticCookingLoadGen(startAndEnd[0], startAndEnd[1]);

			lighting(thisHHProfile, startDayofYear, startDayofWeek, Nspan, consumer_type, BST_to_GMT_day, GMT_to_BST_day);
			cooking(thisHHProfile, startDayofYear, startDayofWeek, Nspan, consumer_type);
			cooling(thisHHProfile, startDayofYear, startDayofWeek, Nspan);
			washing(thisHHProfile, startDayofYear, startDayofWeek, Nspan);
			water(thisHHProfile, startDayofYear, Nspan);
			heat(thisHHProfile, startDayofYear, Nspan);
			misc(thisHHProfile, startDayofYear, startDayofWeek, Nspan);
			thisHHProfile = peak(thisHHProfile, null);
			//the following test out typical total half-hourly demands - using default ownership of appliances

			// D_total_average_E7=D_lights+D_water_E7+D_heat+D_fridge_freezer+D_washer_E7+D_dryer_E7+D_oven+D_kettle+D_microwave+D_misc;
			double[] D_total_average_UR=ArrayUtils.add(thisHHProfile.D_lights, thisHHProfile.D_water_UR, thisHHProfile.D_fridge_freezer, thisHHProfile.D_washer_UR, thisHHProfile.D_dryer_UR, thisHHProfile.D_oven, thisHHProfile.D_kettle, thisHHProfile.D_microwave, thisHHProfile.D_misc);

		}
		else
		{
			//Non domestic not yet translated / implemented
		}
	}



	/**
	 * @param thisHHProfile
	 * @param startDayofYear
	 * @param nspan
	 */
	private static void lighting(MSHalfHourProfile thisProfile, double startDayOfYear, int startDayOfWeek, int Nspan, int consumer_type, int BST_GMT, int GMT_BST)
	{
		// this section only works for test days in a single year at present due to BST / GMT stuff
		// checks the weekday type, sets parameter BST (phase shift for autumn clock change to 0)
		// checks to see if date if before or after clock change in test year; if it is BST=34)

		// for each half-hour, calculates the two annual sine trends and totals them
		// if the total is under the min level, sets demand to min; if over max level, sets demand to max

		// adds on a normally distributed random number - mean 0, std dev 1 & scales by appropriate std dev for model
		// sets negative demands to zero

		// domestic consumers - different lighting demands, depending on day type
		double [] d_lights_sine1 =  new double[thisProfile.d_lights.length];
		double [] d_lights_sine2 =  new double[thisProfile.d_lights.length];
		double [] d_lights_sine =  new double[thisProfile.d_lights.length];

		if ( consumer_type == 1)
		{
			for (int i=0;  i< Nspan;  i++)
			{
				// With this formulation, we get back to the Matlab equivalent of
				// weekDay - i.e. 1-7 with 1 == Sunday.
				int thisDayOfWeek = (startDayOfWeek + i) % 7;
				if (thisDayOfWeek == 0)
					thisDayOfWeek = 7;

				if ( thisDayOfWeek<7)
				{
					if ( thisDayOfWeek == 1)
					{
						int BST=0;
						if ( (startDayOfYear + i) > BST_GMT)
						{
							BST=34;
						}
						if ( (startDayOfYear + i) < GMT_BST)
						{
							BST=34;
						}
						for (int HH=0;  HH< 48;  HH++)
						{
							int n=0;
							d_lights_sine1[48*i + HH]=MSConstArrays.lighting_sinescale1_sun[HH]*Math.sin((2*Math.PI*(((startDayOfYear + i)+BST)/365)-MSConstArrays.lighting_sinephase1_sun[HH]));
							d_lights_sine2[48*i + HH]=MSConstArrays.lighting_sinescale2_sun[HH]*Math.sin((2*Math.PI*(((startDayOfYear + i)-BST)/365)-MSConstArrays.lighting_sinephase2_sun[HH]))+MSConstArrays.lighting_const_sun[HH];
							d_lights_sine[48*i + HH]=d_lights_sine1[48*i + HH]+d_lights_sine2[48*i + HH];
							if ( d_lights_sine[48*i + HH]<= MSConstArrays.lighting_min_sun[HH])
							{
								thisProfile.d_lights[48*i + HH]= MSConstArrays.lighting_min_sun[HH]+ (RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_min_sun[HH]);
								n=1;
							}
							if ( d_lights_sine[48*i + HH]>= MSConstArrays.lighting_max_sun[HH])
							{
								thisProfile.d_lights[48*i + HH]= MSConstArrays.lighting_max_sun[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_max_sun[HH]);
								n=1;
							}
							if ( n==0)
							{
								thisProfile.d_lights[48*i + HH]=d_lights_sine[48*i + HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_sine_sun[HH]);
							}
							if ( thisProfile.d_lights[48*i + HH] < 0)
							{
								thisProfile.d_lights[48*i + HH] = 0;
							}
						}
					}
					int BST=0;
					if ( (startDayOfYear + i) > BST_GMT)
					{
						BST=34;
					}
					if ( (startDayOfYear + i) <GMT_BST)
					{
						BST=34;
					}
					for (int HH=0;  HH< 48;  HH++)
					{
						int n=0;
						d_lights_sine1[48*i + HH]=MSConstArrays.lighting_sinescale1_wkdays[HH]*Math.sin((2*Math.PI*(((startDayOfYear + i)+BST)/365)-MSConstArrays.lighting_sinephase1_wkdays[HH]));
						d_lights_sine2[48*i + HH]=MSConstArrays.lighting_sinescale2_wkdays[HH]*Math.sin((2*Math.PI*(((startDayOfYear + i)-BST)/365)-MSConstArrays.lighting_sinephase2_wkdays[HH]))+MSConstArrays.lighting_const_wkdays[HH];
						d_lights_sine[48*i + HH]=d_lights_sine1[48*i + HH]+d_lights_sine2[48*i + HH];
						if ( d_lights_sine[48*i + HH]<= MSConstArrays.lighting_min_wkdays[HH])
						{
							thisProfile.d_lights[48*i + HH]= MSConstArrays.lighting_min_wkdays[HH]+ (RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_min_wkdays[HH]);
							n=1;
						}
						if ( d_lights_sine[48*i + HH]>= MSConstArrays.lighting_max_wkdays[HH])
						{
							thisProfile.d_lights[48*i + HH]= MSConstArrays.lighting_max_wkdays[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_max_wkdays[HH]);
							n=1;
						}
						if ( n==0)
						{
							thisProfile.d_lights[48*i + HH]=d_lights_sine[48*i + HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_sine_wkdays[HH]);
						}
						if ( thisProfile.d_lights[48*i + HH] < 0)
						{
							thisProfile.d_lights[48*i + HH] = 0;
						}
					}
				}
				if ( thisDayOfWeek == 7)
				{
					int BST=0;
					if ( (startDayOfYear + i) > BST_GMT)
					{
						BST=34;
					}
					if ( (startDayOfYear + i) < GMT_BST)
					{
						BST=34;
					}
					for (int HH=0;  HH< 48;  HH++)
					{
						int n=0;
						d_lights_sine1[48*i + HH]=MSConstArrays.lighting_sinescale1_sat[HH]*Math.sin((2*Math.PI*(((startDayOfYear + i)+BST)/365)-MSConstArrays.lighting_sinephase1_sat[HH]));
						d_lights_sine2[48*i + HH]=MSConstArrays.lighting_sinescale2_sat[HH]*Math.sin((2*Math.PI*(((startDayOfYear + i)-BST)/365)-MSConstArrays.lighting_sinephase2_sat[HH]))+MSConstArrays.lighting_const_sat[HH];
						d_lights_sine[48*i + HH]=d_lights_sine1[48*i + HH]+d_lights_sine2[48*i + HH];
						if ( d_lights_sine[48*i + HH]<= MSConstArrays.lighting_min_sat[HH])
						{
							thisProfile.d_lights[48*i + HH]= MSConstArrays.lighting_min_sat[HH]+ (RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_min_sat[HH]);
							n=1;
						}
						if ( d_lights_sine[48*i + HH]>= MSConstArrays.lighting_max_sat[HH])
						{
							thisProfile.d_lights[48*i + HH]= MSConstArrays.lighting_max_sat[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_max_sat[HH]);
							n=1;
						}
						if ( n==0)
						{
							thisProfile.d_lights[48*i + HH]=d_lights_sine[48*i + HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_sine_sat[HH]);
						}
						if ( thisProfile.d_lights[48*i + HH] < 0)
						{
							thisProfile.d_lights[48*i + HH] = 0;
						}
					}
				}
			}
		}


		//  for non-domestics - sets all days to weekdays
		if ( consumer_type == 2)
		{
			for (int i=0;  i< Nspan;  i++)
			{
				int BST=0;
				if ( (startDayOfYear + i) > BST_GMT)
				{
					BST=34;
				}
				if ( (startDayOfYear + i) < GMT_BST)
				{
					BST=34;
				}
				for (int HH=0;  HH< 48;  HH++)
				{
					int n=0;
					d_lights_sine1[48*i + HH]=MSConstArrays.lighting_sinescale1_wkdays[HH]*Math.sin((2*Math.PI*(((startDayOfYear + i)+BST)/365)-MSConstArrays.lighting_sinephase1_wkdays[HH]));
					d_lights_sine2[48*i + HH]=MSConstArrays.lighting_sinescale2_wkdays[HH]*Math.sin((2*Math.PI*(((startDayOfYear + i)-BST)/365)-MSConstArrays.lighting_sinephase2_wkdays[HH]))+MSConstArrays.lighting_const_wkdays[HH];
					d_lights_sine[48*i + HH]=d_lights_sine1[48*i + HH]+d_lights_sine2[48*i + HH];
					if ( d_lights_sine[48*i + HH]<= MSConstArrays.lighting_min_wkdays[HH])
					{
						thisProfile.d_lights[48*i + HH]= MSConstArrays.lighting_min_wkdays[HH]+ (RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_min_wkdays[HH]);
						n=1;
					}
					if ( d_lights_sine[48*i + HH]>= MSConstArrays.lighting_max_wkdays[HH])
					{
						thisProfile.d_lights[48*i + HH]= MSConstArrays.lighting_max_wkdays[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_max_wkdays[HH]);
						n=1;
					}
					if ( n==0)
					{
						thisProfile.d_lights[48*i + HH]=d_lights_sine[48*i + HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.lighting_stddev_sine_wkdays[HH]);
					}
					if ( thisProfile.d_lights[48*i + HH] < 0)
					{
						thisProfile.d_lights[48*i + HH] = 0;
					}
				}
			}
		}

	}

	/**
	 * @param thisHHProfile
	 * @param startDayofYear
	 * @param nspan
	 */
	private static void washing(MSHalfHourProfile thisProfile, double startDayOfYear, int startDayOfWeek, int Nspan)
	{
		Arrays.fill(thisProfile.d_washer_UR,0);
		Arrays.fill(thisProfile.d_dryer_UR,0);
		Arrays.fill(thisProfile.d_dish_UR,0);
		Arrays.fill(thisProfile.d_washer_E7,0);
		Arrays.fill(thisProfile.d_dryer_E7,0);
		Arrays.fill(thisProfile.d_dish_E7,0);

		for (int i=0;  i< Nspan;  i++)
		{
	    	// With this formulation, we get back to the Matlab equivalent of
	    	// weekDay - i.e. 1-7 with 1 == Sunday.
	    	int thisDayOfWeek = (startDayOfWeek + i) % 7;
	    	if (thisDayOfWeek == 0)
	    		thisDayOfWeek = 7;

			if ( thisDayOfWeek < 3)
			{

				//washing demand for Mondays:
				if ( thisDayOfWeek==2)
				{
					for (int HH=0;  HH< 48;  HH++)
					{
						thisProfile.d_washer_UR[48*i + HH]=MSConstArrays.scale_washer_mon_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_washer_mon_UR[HH])+MSConstArrays.const_washer_mon_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_mon_UR[HH]) ; 
						thisProfile.d_dryer_UR[48*i + HH]=MSConstArrays.scale_dryer_mon_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dryer_mon_UR[HH])+MSConstArrays.const_dryer_mon_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_mon_UR[HH])  ;
						thisProfile.d_dish_UR[48*i + HH]=MSConstArrays.scale_dish_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dish_UR[HH])+MSConstArrays.const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_UR[HH])  ;
						thisProfile.d_washer_E7[48*i + HH]=MSConstArrays.scale_washer_mon_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_washer_mon_E7[HH])+MSConstArrays.const_washer_mon_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_mon_E7[HH]);
						thisProfile.d_dryer_E7[48*i + HH]=MSConstArrays.scale_dryer_mon_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dryer_mon_E7[HH])+MSConstArrays.const_dryer_mon_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_mon_E7[HH]);
						thisProfile.d_dish_E7[48*i + HH]=MSConstArrays.scale_dish_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dish_E7[HH])+MSConstArrays.const_dish_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_E7[HH]);
						if ( thisProfile.d_washer_UR[48*i + HH] <0)
						{
							thisProfile.d_washer_UR[48*i + HH] = 0;
						}
						if ( thisProfile.d_dryer_UR[48*i + HH] < 0)
						{
							thisProfile.d_dryer_UR [48*i + HH] = 0;
						}
						if ( thisProfile.d_dish_UR[48*i + HH] < 0)
						{
							thisProfile.d_dish_UR[48*i + HH] = 0;
						}
						if ( thisProfile.d_washer_E7[48*i + HH] <0)
						{
							thisProfile.d_washer_E7[48*i + HH] = 0;
						}
						if ( thisProfile.d_dryer_E7[48*i + HH] < 0)
						{
							thisProfile.d_dryer_E7 [48*i + HH] = 0;
						}
						if ( thisProfile.d_dish_E7[48*i + HH] < 0)
						{
							thisProfile.d_dish_E7[48*i + HH] = 0;
						}
					}
				}

				//washing demand for Sundays:
				if ( thisDayOfWeek ==1)
				{
					for (int HH=0;  HH< 48;  HH++)
					{
						thisProfile.d_washer_UR[48*i + HH]=MSConstArrays.scale_washer_sun_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_washer_sun_UR[HH])+MSConstArrays.const_washer_sun_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_sun_UR[HH]);
						thisProfile.d_dryer_UR[48*i + HH]=MSConstArrays.scale_dryer_sun_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dryer_sun_UR[HH])+MSConstArrays.const_dryer_sun_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_sun_UR[HH]) ;   
						thisProfile.d_dish_UR[48*i + HH]=MSConstArrays.scale_dish_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dish_UR[HH])+MSConstArrays.const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_UR[HH])  ;
						thisProfile.d_washer_E7[48*i + HH]=MSConstArrays.scale_washer_sun_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_washer_sun_E7[HH])+MSConstArrays.const_washer_sun_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_sun_E7[HH]);
						thisProfile.d_dryer_E7[48*i + HH]=MSConstArrays.scale_dryer_sun_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dryer_sun_E7[HH])+MSConstArrays.const_dryer_sun_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_sun_E7[HH]);
						thisProfile.d_dish_E7[48*i + HH]=MSConstArrays.scale_dish_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dish_E7[HH])+MSConstArrays.const_dish_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_E7[HH]);
						if ( thisProfile.d_washer_UR[48*i + HH] <0)
						{
							thisProfile.d_washer_UR[48*i + HH] = 0;
						}
						if ( thisProfile.d_dryer_UR[48*i + HH] < 0)
						{
							thisProfile.d_dryer_UR [48*i + HH] = 0;
						}
						if ( thisProfile.d_dish_UR[48*i + HH] < 0)
						{
							thisProfile.d_dish_UR[48*i + HH] = 0;
						}
						if ( thisProfile.d_washer_E7[48*i + HH] <0)
						{
							thisProfile.d_washer_E7[48*i + HH] = 0;
						}
						if ( thisProfile.d_dryer_E7[48*i + HH] < 0)
						{
							thisProfile.d_dryer_E7 [48*i + HH] = 0;
						}
						if ( thisProfile.d_dish_E7[48*i + HH] < 0)
						{
							thisProfile.d_dish_E7[48*i + HH] = 0;
						}
					}
				}
			}
			if ( thisDayOfWeek >= 3)
			{

				//washing demand for Saturdays:
				if ( thisDayOfWeek==7)
				{
					for (int HH=0;  HH< 48;  HH++)
					{
						thisProfile.d_washer_UR[48*i + HH]=MSConstArrays.scale_washer_sat_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_washer_sat_UR[HH])+MSConstArrays.const_washer_sat_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_sat_UR[HH]);
						thisProfile.d_dryer_UR[48*i + HH]=MSConstArrays.scale_dryer_sat_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dryer_sat_UR[HH])+MSConstArrays.const_dryer_sat_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_sat_UR[HH]) ;   
						thisProfile.d_dish_UR[48*i + HH]=MSConstArrays.scale_dish_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dish_UR[HH])+MSConstArrays.const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_UR[HH])  ;
						thisProfile.d_washer_E7[48*i + HH]=MSConstArrays.scale_washer_sat_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_washer_sat_E7[HH])+MSConstArrays.const_washer_sat_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_sat_E7[HH]);
						thisProfile.d_dryer_E7[48*i + HH]=MSConstArrays.scale_dryer_sat_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dryer_sat_E7[HH])+MSConstArrays.const_dryer_sat_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_sat_E7[HH]);
						thisProfile.d_dish_E7[48*i + HH]=MSConstArrays.scale_dish_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dish_E7[HH])+MSConstArrays.const_dish_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_E7[HH]);
						if ( thisProfile.d_washer_UR[48*i + HH] <0)
						{
							thisProfile.d_washer_UR[48*i + HH] = 0;
						}
						if ( thisProfile.d_dryer_UR[48*i + HH] < 0)
						{
							thisProfile.d_dryer_UR [48*i + HH] = 0;
						}
						if ( thisProfile.d_dish_UR[48*i + HH] < 0)
						{
							thisProfile.d_dish_UR[48*i + HH] = 0;
						}
						if ( thisProfile.d_washer_E7[48*i + HH] <0)
						{
							thisProfile.d_washer_E7[48*i + HH] = 0;
						}
						if ( thisProfile.d_dryer_E7[48*i + HH] < 0)
						{
							thisProfile.d_dryer_E7 [48*i + HH] = 0;
						}
						if ( thisProfile.d_dish_E7[48*i + HH] < 0)
						{
							thisProfile.d_dish_E7[48*i + HH] = 0;
						}
					}
				}

				//washing demand for weekdays (except Mon):
				if ( thisDayOfWeek < 7)
				{
					for (int HH=0;  HH< 48;  HH++)
					{
						thisProfile.d_washer_UR[48*i + HH]=MSConstArrays.scale_washer_wkdays_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_washer_wkdays_UR[HH])+MSConstArrays.const_washer_wkdays_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_wkdays_UR[HH]);
						thisProfile.d_dryer_UR[48*i + HH]=MSConstArrays.scale_dryer_wkdays_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dryer_wkdays_UR[HH])+MSConstArrays.const_dryer_wkdays_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_wkdays_UR[HH]) ;   
						thisProfile.d_dish_UR[48*i + HH]=MSConstArrays.scale_dish_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dish_UR[HH])+MSConstArrays.const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_UR[HH]) ;
						thisProfile.d_washer_E7[48*i + HH]=MSConstArrays.scale_washer_wkdays_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_washer_wkdays_E7[HH])+MSConstArrays.const_washer_wkdays_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_wkdays_E7[HH]);
						thisProfile.d_dryer_E7[48*i + HH]=MSConstArrays.scale_dryer_wkdays_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dryer_wkdays_E7[HH])+MSConstArrays.const_dryer_wkdays_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_wkdays_E7[HH]);
						thisProfile.d_dish_E7[48*i + HH]=MSConstArrays.scale_dish_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365))-MSConstArrays.phase_dish_E7[HH])+MSConstArrays.const_dish_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_E7[HH]);
						if ( thisProfile.d_washer_UR[48*i + HH] <0)
						{
							thisProfile.d_washer_UR[48*i + HH] = 0;
						}
						if ( thisProfile.d_dryer_UR[48*i + HH] < 0)
						{
							thisProfile.d_dryer_UR [48*i + HH] = 0;
						}
						if ( thisProfile.d_dish_UR[48*i + HH] < 0)
						{
							thisProfile.d_dish_UR[48*i + HH] = 0;
						}
						if ( thisProfile.d_washer_E7[48*i + HH] <0)
						{
							thisProfile.d_washer_E7[48*i + HH] = 0;
						}
						if ( thisProfile.d_dryer_E7[48*i + HH] < 0)
						{
							thisProfile.d_dryer_E7[48*i + HH] = 0;
						}
						if ( thisProfile.d_dish_E7[48*i + HH] < 0)
						{
							thisProfile.d_dish_E7[48*i + HH] = 0;
						}
					}
				}
			}
		}

	}

	/**
	 * @param thisHHProfile
	 * @param startDayofYear
	 * @param nspan
	 */
	public static void cooling(MSHalfHourProfile thisProfile, double startDayOfYear, int startDayOfWeek, int Nspan)
	{
		// this section calculates demand for fridges. The demand is the same basic annual tr} but scaled for each half-hour:
		for (int i=0;  i< Nspan;  i++)
		{
			for (int HH=0;  HH< 48;  HH++)
			{
				thisProfile.d_fridge[48*i + HH]=MSConstArrays.scale_fridge[HH]*Math.sin(2*Math.PI*((startDayOfYear + i)/365)-MSConstArrays.phase_fridge[HH])+MSConstArrays.const_fridge[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_fridge[HH]);
				if ( thisProfile.d_fridge[48*i + HH] < 0)
				{
					thisProfile.d_fridge[48*i + HH] = 0;
				}    
			}
		}


		// this section calculates demand for freezers. It introduces a variable constant value as well as a variable std dev for the random element
		for (int i=0;  i< Nspan;  i++)
		{
			for (int HH=0;  HH< 48;  HH++)
			{
				thisProfile.d_freezer[48*i + HH]=MSConstArrays.scale_freezer[HH]*Math.sin(2*Math.PI*((startDayOfYear + i)/365)-2.05)+MSConstArrays.const_freezer[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_freezer[HH]);
				if ( thisProfile.d_freezer[48*i + HH] < 0)
				{
					thisProfile.d_freezer[48*i + HH] = 0;
				}    
			}
		}


		// this section calculates demand for fridge-freezers.It follows a similar pattern to the freezer model

		// but phase is now also a variable with half-hour
		for (int i=0;  i< Nspan;  i++)
		{
			for (int HH=0;  HH< 48;  HH++)
			{
				thisProfile.d_fridge_freezer[48*i + HH]=MSConstArrays.scale_fridge_freezer[HH]*Math.sin(2*Math.PI*((startDayOfYear + i)/365)-MSConstArrays.phase_fridge_freezer[HH])+MSConstArrays.const_fridge_freezer[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_fridge_freezer[HH]);
				if ( thisProfile.d_fridge_freezer[48*i + HH] < 0)
				{
					thisProfile.d_fridge_freezer[48*i + HH] = 0;
				}
			}
		}
	}

	public static void cooking(MSHalfHourProfile thisProfile, double startDayOfYear, int startDayOfWeek, int Nspan, int consumer_type)
	{
		if ( consumer_type == 1 )
		{
			//domestic consumers with different demand for 3 different day types)
			{
				for (int i=0;  i< Nspan;  i++)
				{
					// With this formulation, we get back to the Matlab equivalent of
					// weekDay - i.e. 1-7 with 1 == Sunday.
					int thisDayOfWeek = (startDayOfWeek + i) % 7;
					if (thisDayOfWeek == 0)
						thisDayOfWeek = 7;

					if ( thisDayOfWeek<7)
					{

						//for Sundays:
						if ( thisDayOfWeek == 1)
						{
							for (int HH=0;  HH< 48;  HH++)
							{
								thisProfile.d_cook[48*i + HH]=MSConstArrays.cooking_sinescale_sun[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365)-MSConstArrays.cooking_sinephase_sun[HH]))+MSConstArrays.cooking_const_sun[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.cooking_stddev_sun[HH]);
								if ( thisProfile.d_cook[48*i + HH] <0)
								{
									thisProfile.d_cook[48*i + HH] = 0;
								}
							}
						}

						//for weekdays:
						if ( thisDayOfWeek > 1)
						{
							for (int HH=0;  HH< 48;  HH++)
							{
								thisProfile.d_cook[48*i + HH]=MSConstArrays.cooking_sinescale_wkdays[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365)-MSConstArrays.cooking_sinephase_wkdays[HH]))+MSConstArrays.cooking_const_wkdays[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.cooking_stddev_wkdays[HH]);
								if ( thisProfile.d_cook[48*i + HH] <0)
								{
									thisProfile.d_cook[48*i + HH] = 0;
								}
							}
						}
					}
					if ( thisDayOfWeek == 7)
					{

						//for Saturdays:
						for (int HH=0;  HH< 48;  HH++)
						{
							thisProfile.d_cook[48*i + HH]=MSConstArrays.cooking_sinescale_sat[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365)-MSConstArrays.cooking_sinephase_sat[HH]))+MSConstArrays.cooking_const_sat[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.cooking_stddev_sat[HH]);
							if ( thisProfile.d_cook[48*i + HH] <0)
							{
								thisProfile.d_cook[48*i + HH] = 0;
							} 
						}
					}
				}
			}

			if ( consumer_type == 2 )
			{
				//non-domestic consumers with same demand any every day (weekday domestic demand)
				for (int i=0;  i< Nspan;  i++)
				{
					for (int HH=0;  HH< 48;  HH++)
					{
						thisProfile.d_cook[48*i + HH]=MSConstArrays.cooking_sinescale_wkdays[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/365)-MSConstArrays.cooking_sinephase_wkdays[HH]))+MSConstArrays.cooking_const_wkdays[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.cooking_stddev_wkdays[HH]);
						if ( thisProfile.d_cook[48*i + HH] <0)
						{
							thisProfile.d_cook[48*i + HH] = 0;
						}
					}
				}
			}
		}
	}

	/**
	 * @param thisHHProfile
	 * @param startDayofYear
	 * @param nspan
	 */
	private static void misc(MSHalfHourProfile thisHHProfile,
			double startDayOfYear, int startDayOfWeek, int Nspan) {

		int consumer_type = 1; // for compatibility with old matlab code - consumer_type = 1 means domestic

		// this submodel calculates a miscellaneous demand for (the group average
		// sets of parameters are provided for (Mondays, Saturdays, Sundays & other weekdays
		// the miscellaneous demand is assumed to apply to both E7 and unrestricted tariff consumers

		double[] d_misc = new double[Nspan * 48];

		if (consumer_type == 1)
		{
			// domestic consumers (different misc demand for (int 4 different day types)
			for (int i=0; i < Nspan; i++)
			{
				// With this formulation, we get back to the Matlab equivalent of
				// weekDay - i.e. 1-7 with 1 == Sunday.
				int thisDayOfWeek = (startDayOfWeek + i) % 7;
				if (thisDayOfWeek == 0)
					thisDayOfWeek = 7;

				if (thisDayOfWeek < 3)
				{

					//misc demand for (int Mondays:
					if (thisDayOfWeek==2)
					{

						for (int HH=0; HH < 48; HH++)
						{

							d_misc[48*i + HH]=MSConstArrays.scale_misc_mon[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/Consts.DAYS_PER_YEAR))-MSConstArrays.phase_misc_mon[HH])+MSConstArrays.const_misc_mon[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_misc_mon[HH]);
							if (d_misc[48*i + HH] <0)
							{

								d_misc[48*i + HH] = 0;
							}
						}
					}
					//misc demand for (int Sundays:
					if (thisDayOfWeek ==1)
					{

						for (int HH=0; HH < 48; HH++)
						{

							d_misc[48*i + HH]=MSConstArrays.scale_misc_sun[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/Consts.DAYS_PER_YEAR))-MSConstArrays.phase_misc_sun[HH])+MSConstArrays.const_misc_sun[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_misc_sun[HH]);
							if (d_misc[48*i + HH] <0)
							{

								d_misc[48*i + HH] = 0;
							}
						}
					}
				}

				if (thisDayOfWeek >= 3)
				{

					//misc demand for (int Saturdays:
					if (thisDayOfWeek==7)
					{

						for (int HH=0; HH < 48; HH++)
						{

							d_misc[48*i + HH]=MSConstArrays.scale_misc_sat[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/Consts.DAYS_PER_YEAR))-MSConstArrays.phase_misc_sat[HH])+MSConstArrays.const_misc_sat[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_misc_sat[HH]);
							if (d_misc[48*i + HH] <0)
							{

								d_misc[48*i + HH] = 0;
							}
						}
					}
					//misc demand for (int weekdays (except Mon):
					if (thisDayOfWeek < 7)
					{

						for (int HH=0; HH < 48; HH++)
						{

							d_misc[48*i + HH]=MSConstArrays.scale_misc_wkdays[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/Consts.DAYS_PER_YEAR))-MSConstArrays.phase_misc_wkdays[HH])+MSConstArrays.const_misc_wkdays[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_misc_wkdays[HH]);
							if (d_misc[48*i + HH] <0)
							{

								d_misc[48*i + HH] = 0;
							}
						}
					}
				}
			}
		}
		if (consumer_type == 2)
		{
			// non-domestic consumers (all same day type = domestic weekday)

			// Not yet implemented TODO

			/*for (int i=0; i < Nspan; i++)
			{

		        for (int HH=0; HH < 48; HH++)
				{

		           d_misc(i,HH)=MSConstArrays.scale_misc_wkdays[HH]*sin((2*pi*(N(i)/Ndays(1)))-MSConstArrays.phase_misc_wkdays[HH])+MSConstArrays.const_misc_wkdays[HH]+(randn*MSConstArrays.stddev_misc_wkdays[HH]);
		           if (d_misc(i,HH) <0)
		   		{

		               d_misc(i,HH) = 0;
		           }
		        }
		    }*/
		}

	}

	/**
	 * @param thisHHProfile
	 * @param startDayofYear
	 * @param nspan
	 */
	private static void heat(MSHalfHourProfile thisHHProfile,
			double startDayofYear, int Nspan) {
		// this sub-model calculates space heating (storage heaters) for a group average of E7 consumers
		// Note assumption of storage heaters
		// and assumption that all with electric heating will be on E7 tariff

		double[] d_heat = new double[Nspan * 48];

		for ( int i=0; i < Nspan; i++)
		{
			for (int HH=0; HH < 48; HH++)
			{
				d_heat[48*i + HH]=MSConstArrays.scale_heat[HH]*Math.sin((2*Math.PI*((startDayofYear+i)/Consts.DAYS_PER_YEAR))-MSConstArrays.phase_heat[HH])+MSConstArrays.const_heat[HH] ;

				if (d_heat[48*i + HH] < MSConstArrays.min_heat[HH])
				{
					d_heat[48*i + HH] = MSConstArrays.min_heat[HH];
				}

				if (d_heat[48*i + HH] > MSConstArrays.max_heat[HH])
				{
					d_heat[48*i + HH] = MSConstArrays.max_heat[HH];
				}

				d_heat[48*i + HH] = d_heat[48*i + HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_heat[HH]) ;

				if (d_heat[48*i + HH] < 0)
				{
					d_heat[48*i + HH] = 0;
				}                
			}
		}
		double temp=ArrayUtils.max(d_heat);

		thisHHProfile.d_heat = ArrayUtils.multiply(d_heat, 1/temp);

	}

	private static void water(MSHalfHourProfile thisHHProfile, double startDayOfYear, int Nspan)
	{
		// this sub-model calculates the half-hourly group average demand for immersion water heating
		// there are separate sets of parameters for E7 and unrestricted tariffs

		for (int i=0; i < Nspan; i++)
		{
			for (int HH=0; HH < 48; HH++)
			{

				thisHHProfile.d_water_UR[48*i + HH]=MSConstArrays.scale_water_UR[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/Consts.DAYS_PER_YEAR))-MSConstArrays.phase_water_UR[HH])+MSConstArrays.const_water_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_water_UR[HH]) ;
				thisHHProfile.d_water_E7[48*i + HH]=MSConstArrays.scale_water_E7[HH]*Math.sin((2*Math.PI*((startDayOfYear + i)/Consts.DAYS_PER_YEAR))-MSConstArrays.phase_water_E7[HH])+MSConstArrays.const_water_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_water_E7[HH])  ;
				if (thisHHProfile.d_water_UR[48*i + HH] <0)
				{
					thisHHProfile.d_water_UR[48*i + HH] = 0;
				}
				if (thisHHProfile.d_water_E7[48*i + HH] < 0)
				{
					thisHHProfile.d_water_E7[48*i + HH]=0;
				}
			}
		}
	}

	/**
	 * @param thisHHProfile
	 * @return
	 */
	private static MSHalfHourProfile peak(MSHalfHourProfile thisHHProfile, Date startDate) {
		// this submodel calculates half-hourly demands for the group average
		//it begins by working out the scaling factors for each end-use demand (annual peak)in kW
		//the trends have a different number of years in the datasets (11,12 and 7)
		// the annual peak demand is interpolated linearly between the data values
		//the routines below find where the test year falls in relation to the data values
		// the model is only valid between 1970 and 2020
		int y = startDate.getYear();

		if (y > 2020 || y <= 1970)
		{
			System.err.println("year entered (" + y+ ") is beyond range of model (valid between 1971 and 2020)");
			y = Math.min(2020, Math.max(y, 1970));
			System.err.println("Reset to " + y);		    
		}

		int a=1;
		for (int i=0; i < 11; i++)
		{
			if (y > MSConstArrays.trend_year_11[i])
			{
				a=a+1;
			}
		}

		double b= (double)(y-MSConstArrays.trend_year_11[a-1])/(MSConstArrays.trend_year_11[a]-MSConstArrays.trend_year_11[a-1]); // interpolate...
		double lighting_peak = MSConstArrays.lighting_trend[a-1]+b*(MSConstArrays.lighting_trend[a]-MSConstArrays.lighting_trend[a-1]);
		double misc_peak = MSConstArrays.misc_trend[a-1]+b*(MSConstArrays.misc_trend[a]-MSConstArrays.misc_trend[a-1]);

		a=1;
		for (int i=0; i < 12; i++)
		{
			if (y > MSConstArrays.trend_year_12[i])
			{
				a=a+1;
			}
		}

		b=(double)(y-MSConstArrays.trend_year_12[a-1])/(MSConstArrays.trend_year_12[a]-MSConstArrays.trend_year_12[a-1]);
		double water_E7_peak = MSConstArrays.water_trend_E7[a-1]+b*(MSConstArrays.water_trend_E7[a]-MSConstArrays.water_trend_E7[a-1]);
		double water_UR_peak = MSConstArrays.water_trend_UR[a-1]+b*(MSConstArrays.water_trend_UR[a]-MSConstArrays.water_trend_UR[a-1]);
		double fridge_peak = MSConstArrays.fridge_trend[a-1]+b*(MSConstArrays.fridge_trend[a]-MSConstArrays.fridge_trend[a-1]);
		double freezer_peak = MSConstArrays.freezer_trend[a-1]+b*(MSConstArrays.freezer_trend[a]-MSConstArrays.freezer_trend[a-1]);
		double fridge_freezer_peak = MSConstArrays.fridge_freezer_trend[a-1]+b*(MSConstArrays.fridge_freezer_trend[a]-MSConstArrays.fridge_freezer_trend[a-1]);
		double washer_peak = MSConstArrays.washer_trend[a-1]+b*(MSConstArrays.washer_trend[a]-MSConstArrays.washer_trend[a-1]);
		double dryer_peak = MSConstArrays.dryer_trend[a-1]+b*(MSConstArrays.dryer_trend[a]-MSConstArrays.dryer_trend[a-1]);
		double dish_peak = MSConstArrays.dish_trend[a-1]+b*(MSConstArrays.dish_trend[a]-MSConstArrays.dish_trend[a-1]);
		double water_own_peak =MSConstArrays.water_own[a-1]+b*(MSConstArrays.water_own[a]-MSConstArrays.water_own[a-1]); // Note - mistake in original peak.m - this mapped back over the array
		// rather than onto a new variable water_own_peak

		a=1;
		for (int i=0; i < 7; i++)
		{
			if (y> MSConstArrays.trend_year_7[i])
			{
				a=a+1;
			}
		}

		b= (double) (y-MSConstArrays.trend_year_7[a-1])/(MSConstArrays.trend_year_7[a]-MSConstArrays.trend_year_7[a-1]);
		double cooker_peak = MSConstArrays.cooker_trend[a-1]+b*(MSConstArrays.cooker_trend[a]-MSConstArrays.cooker_trend[a-1]);
		double kettle_peak = MSConstArrays.kettle_trend[a-1]+b*(MSConstArrays.kettle_trend[a]-MSConstArrays.kettle_trend[a-1]);
		double microwave_peak = MSConstArrays.microwave_trend[a-1]+b*(MSConstArrays.microwave_trend[a]-MSConstArrays.microwave_trend[a-1]);
		double oven_hob_ratio = MSConstArrays.oven_hob_trend [a-1]+b*(MSConstArrays.oven_hob_trend[a]-MSConstArrays.oven_hob_trend[a-1]);

		double heat_peak = (((double)y-1996)/4)*(7.649-5.811) + 5.811;

		//having established the annual peak value, the annual trends in demand for each end-use are scaled accordingly

		thisHHProfile.D_lights		=	ArrayUtils.multiply(thisHHProfile.d_lights, lighting_peak);
		thisHHProfile.D_water_E7	=	ArrayUtils.multiply(thisHHProfile.d_water_E7, water_E7_peak);
		thisHHProfile.D_water_UR	=	ArrayUtils.multiply(thisHHProfile.d_water_UR, water_UR_peak);
		thisHHProfile.D_heat		=	ArrayUtils.multiply(thisHHProfile.d_heat, heat_peak);
		thisHHProfile.D_fridge		=	ArrayUtils.multiply(thisHHProfile.d_fridge, fridge_peak);
		thisHHProfile.D_freezer		=	ArrayUtils.multiply(thisHHProfile.d_freezer, freezer_peak);
		thisHHProfile.D_fridge_freezer=	ArrayUtils.multiply(thisHHProfile.d_fridge_freezer, fridge_freezer_peak);
		thisHHProfile.D_washer_E7	=	ArrayUtils.multiply(thisHHProfile.d_washer_E7,washer_peak*0.180/0.235); //takes account of different peak values in 1996 from LRG data
		thisHHProfile.D_washer_UR	=	ArrayUtils.multiply(thisHHProfile.d_washer_UR,washer_peak);
		thisHHProfile.D_dryer_E7	=	ArrayUtils.multiply(thisHHProfile.d_dryer_E7,dryer_peak*0.309/0.347); //takes account of different peak values in UR and E7
		thisHHProfile.D_dryer_UR	=	ArrayUtils.multiply(thisHHProfile.d_dryer_UR,dryer_peak);
		thisHHProfile.D_dish_E7		=	ArrayUtils.multiply(thisHHProfile.d_dish_E7,dish_peak*0.411); //different LRG peak values
		thisHHProfile.D_dish_UR		=	ArrayUtils.multiply(thisHHProfile.d_dish_UR,dish_peak);
		thisHHProfile.D_hob			=	ArrayUtils.multiply(thisHHProfile.d_cook,cooker_peak*oven_hob_ratio);
		thisHHProfile.D_oven		=	ArrayUtils.multiply(thisHHProfile.d_cook,cooker_peak*(1-oven_hob_ratio));
		thisHHProfile.D_kettle		=	ArrayUtils.multiply(thisHHProfile.d_cook,kettle_peak);
		thisHHProfile.D_microwave	=	ArrayUtils.multiply(thisHHProfile.d_cook,microwave_peak);
		thisHHProfile.D_misc		=	ArrayUtils.multiply(thisHHProfile.d_misc,misc_peak);

		return thisHHProfile;
	}
	
	//this module scales or modifies the group average half-hourly demand
	//based on ownership assignment for }-uses and occupancy

	private static void HH_specific(MSHouseholdCharacteristics thisHousehold, boolean MSownershipCalc, MSHalfHourProfile thisProfile, Date startDate, int Nspan)
	{
		// Tests which tariff we're on.  Note this is because the original model
		// Had different statistical parameters for unrestricted tariff (UR)
		// Economy 7 tariffs (EY) and, in the case of unknown, either is selected.
		/*
		 * Original Matlab code for tariff selection.
		 * 
		 * str = {'Unrestricted' 'Off-peak,eg E7' 'Unknown'};
		 * [tar,v]=listdlg('PromptString','Select tariff','ListString',str, 'SelectionMode','single');
		 * if (tar == 3
		 * R=rand
		 * if (R < 0.25
		 * tar = 2
		 * }
		 * if (R >= 0.25
		 * tar = 1
		 * }
		 * }
		 * */

		// We fix tariff as UR as we are interested in deviations from the "norm"
		// or unrestricted tariff.
		// TODO: check this assumption with others on the project.
		int tar = 1;

		// In the original Matlab model, ownership calculates the ownership of various appliances.
		// We can either use this or the ownership as passed into the function in this implementation.
		if (MSownershipCalc)
		{
			thisHousehold = ownership();
		}
		
		// section for space heating
		if (!thisHousehold.heat_marker)
		{
			// no space heating required
			Arrays.fill(thisProfile.D_HHspecific_heat,0);
		}
		else
		{// space heating required
			thisProfile.D_HHspecific_heat= ArrayUtils.multiply(thisProfile.D_heat, (thisHousehold.floor_area/103.2)); // scaled by floor area, average floor area assumed to be 103.2 squ m
		}

		// section for water heating
		double water_scale_value = 0;
		double water_scale_value1 = 0;
		if (thisHousehold.num_occ > 7)
		{
			water_scale_value = 2.2 ;// for 7+ people
		}
		if (thisHousehold.num_occ <= 7)
		{
			water_scale_value1 = MSConstArrays.water_scale[thisHousehold.num_occ]; // selects weighting from array
		}

		double R = RandomHelper.nextDouble();
		water_scale_value = water_scale_value1*0.6; // randomly scales water demand based on high/average/low/very low demand
		if (R < 0.98)
		{
			water_scale_value = water_scale_value1*0.8;
			if (R < 0.86)
			{
				water_scale_value = water_scale_value1;
				if (R < 0.11)
				{
					water_scale_value = water_scale_value1 * 1.2;
				}
			}
		}

		if (thisHousehold.water_marker == 0)
		{
			// no requirement for water heating
			Arrays.fill(thisProfile.D_HHspecific_water,0);
		}
		else if (thisHousehold.water_marker == 1)
		{// all year round demand for water heating
			if (tar == 1)
			{
				// unrestricted 
				thisProfile.D_HHspecific_water = ArrayUtils.multiply(thisProfile.D_water_UR, water_scale_value);
			}
			else if (tar > 1)
			{
				// off-peak
				thisProfile.D_HHspecific_water= ArrayUtils.multiply(thisProfile.D_water_E7, water_scale_value);
			}
		}
		else if (thisHousehold.water_marker == 2)
		{
			// summer only water heating
			for (int i=0; i < Nspan; i++)
			{
				//Original matlab below - N_serial is an array of serial numbers of every
				// date in the profile being produced.
				//	p=datevec(N_serial(i)) ;// p is date matrix with 2nd element being month number
				int p = (new Date(startDate.getTime()+(i * 24*60*60*1000))).getMonth();

				for (int j = 0; j < 48; j++)
				{
					thisProfile.D_HHspecific_water[i*48 + j]=water_scale_value*thisProfile.D_water_UR[i*48 + j];
				}
				if (tar > 1)
				{
					for (int j = 0; j < 48; j++)
					{
						thisProfile.D_HHspecific_water[i*48 + j]=water_scale_value*thisProfile.D_water_E7[i*48 + j];
					}
				}
				if (p < 6)
				{
					// for months before June
					Arrays.fill(thisProfile.D_HHspecific_water,i,i+48,0);

				}
				if (p > 9)
				{
					// for months after Sept
					Arrays.fill(thisProfile.D_HHspecific_water,i,i+48,0);
				}
			}


			// section for cooking

			double cooking_scale_value = 0;
			if (thisHousehold.num_occ > 3)
			{
				cooking_scale_value = 1.08;
			}
			if (thisHousehold.num_occ <= 3)
			{
				cooking_scale_value = MSConstArrays.cooking_scale[thisHousehold.num_occ];
			}
			if (!thisHousehold.hob_marker)
			{
				Arrays.fill(thisProfile.D_HHspecific_hob,0);
			}
			else if (thisHousehold.hob_marker)
			{
				thisProfile.D_HHspecific_hob = ArrayUtils.multiply(thisProfile.D_hob, cooking_scale_value);
			}
			if (!thisHousehold.oven_marker)
			{
				Arrays.fill(thisProfile.D_HHspecific_oven,0);
			}
			else if (thisHousehold.oven_marker)
			{
				thisProfile.D_HHspecific_oven = ArrayUtils.multiply(thisProfile.D_oven, cooking_scale_value);
			}
			if (!thisHousehold.microwave_marker)
			{
				Arrays.fill(thisProfile.D_HHspecific_microwave, 0);
			}
			else if (thisHousehold.microwave_marker)
			{
				thisProfile.D_HHspecific_microwave = ArrayUtils.multiply(thisProfile.D_microwave, cooking_scale_value);
			}
			if (!thisHousehold.kettle_marker)
			{
				Arrays.fill(thisProfile.D_HHspecific_kettle,0);
			}
			else if (thisHousehold.kettle_marker)
			{
				thisProfile.D_HHspecific_kettle = ArrayUtils.multiply(thisProfile.D_kettle, cooking_scale_value);
			}

			thisProfile.D_HHspecific_cook = ArrayUtils.add(thisProfile.D_HHspecific_hob, thisProfile.D_HHspecific_oven, thisProfile.D_HHspecific_microwave, thisProfile.D_HHspecific_kettle);

			// section for refrigeration
			if (thisHousehold.fridge_marker)
			{
				if (thisHousehold.num_occ <= 5)
				{
					thisProfile.D_HHspecific_fridge = ArrayUtils.multiply(thisProfile.D_fridge, MSConstArrays.cool_scale[thisHousehold.num_occ]); // assumes average oocupancy of 2.4 people
				}
				if (thisHousehold.num_occ >5)
				{
					thisProfile.D_HHspecific_fridge = ArrayUtils.multiply(thisProfile.D_fridge,MSConstArrays.cool_scale[5]);
				}
			}

			if (!thisHousehold.fridge_marker)
			{
				Arrays.fill(thisProfile.D_HHspecific_fridge, 0);
			}

			if (thisHousehold.ff_marker)
			{
				if (thisHousehold.num_occ <= 5)
				{
					thisProfile.D_HHspecific_ff = ArrayUtils.multiply(thisProfile.D_fridge_freezer, MSConstArrays.cool_scale[thisHousehold.num_occ]*thisHousehold.ff_num); // assumes average oocupancy of 2.4 people
				}
				if (thisHousehold.num_occ >5)
				{
					thisProfile.D_HHspecific_ff = ArrayUtils.multiply(thisProfile.D_fridge_freezer, MSConstArrays.cool_scale[5]*thisHousehold.ff_num);
				}
			}
			else
			{
				Arrays.fill(thisProfile.D_HHspecific_ff, 0);
			}

			Arrays.fill(thisProfile.D_HHspecific_freezer, 0);
			if (thisHousehold.freezer_marker)
			{

				for (int i=0; i < thisHousehold.freezer_num; i++)
				{
					R=RandomHelper.getNormal().nextDouble()*0.316; //normally distributed random number with zero mean & std dev of 0.316
					thisProfile.D_HHspecific_freezer = ArrayUtils.add(ArrayUtils.multiply(thisProfile.D_freezer, R), thisProfile.D_HHspecific_freezer);
				}
			}


			thisProfile.D_HHspecific_cold = ArrayUtils.add(thisProfile.D_HHspecific_fridge, thisProfile.D_HHspecific_ff, thisProfile.D_HHspecific_freezer);

			// section for washing appliances
			Arrays.fill(thisProfile.D_HHspecific_wash,0);
			Arrays.fill(thisProfile.D_HHspecific_dry,0);
			Arrays.fill(thisProfile.D_HHspecific_dish,0);

			double wash_scale_value = 0;
			if (thisHousehold.num_occ > 6)
			{
				wash_scale_value = thisHousehold.num_occ*1.7 / 4.77; //based on 1.7 washes/person for 6 people and 4.77 washes/week for 2.4 people
			}
			if (thisHousehold.num_occ <= 6)
			{
				wash_scale_value = MSConstArrays.washing_scale[thisHousehold.num_occ];
			}

			wash_scale_value = wash_scale_value * MSConstArrays.wash_scale_social[thisHousehold.social_num];
			double dry_scale_value = wash_scale_value * MSConstArrays.dry_scale_social[thisHousehold.social_num];

			if (tar == 1)
			{ // unrestricted consumers
				if (thisHousehold.wash_marker)
				{
					thisProfile.D_HHspecific_wash = ArrayUtils.multiply(thisProfile.D_washer_UR, wash_scale_value);
				}
				if (!thisHousehold.wash_marker)
				{
					Arrays.fill(thisProfile.D_HHspecific_wash, 0);
				}
				if (thisHousehold.dryer_marker)
				{
					thisProfile.D_HHspecific_dryer = ArrayUtils.multiply(thisProfile.D_dryer_UR, dry_scale_value);
				}
				if (!thisHousehold.dryer_marker)
				{
					Arrays.fill(thisProfile.D_HHspecific_dryer, 0);
				}
				if (thisHousehold.dish_marker)
				{
					thisProfile.D_HHspecific_dish = Arrays.copyOf(thisProfile.D_dish_UR, thisProfile.D_dish_UR.length);
				}
				if (thisHousehold.dish_marker)
				{
					Arrays.fill(thisProfile.D_HHspecific_dish, 0);
				}
				if (thisHousehold.wash_dry_marker)
				{
					thisProfile.D_HHspecific_wash = ArrayUtils.multiply(thisProfile.D_washer_UR, wash_scale_value);
					thisProfile.D_HHspecific_dryer = ArrayUtils.multiply(thisProfile.D_dryer_UR, wash_scale_value/3);
				}
			}
			else if (tar == 2)
			{ // E7 consumers

				if (thisHousehold.wash_marker)
				{
					thisProfile.D_HHspecific_wash = ArrayUtils.multiply(thisProfile.D_washer_E7, wash_scale_value);
				}
				if (!thisHousehold.wash_marker)
				{
					Arrays.fill(thisProfile.D_HHspecific_wash, 0);
				}
				if (thisHousehold.dryer_marker)
				{
					thisProfile.D_HHspecific_dryer = ArrayUtils.multiply(thisProfile.D_dryer_E7, dry_scale_value);
				}
				if (!thisHousehold.dryer_marker)
				{
					Arrays.fill(thisProfile.D_HHspecific_dryer, 0);
				}
				if (thisHousehold.dish_marker)
				{
					thisProfile.D_HHspecific_dish = Arrays.copyOf(thisProfile.D_dish_E7, thisProfile.D_dish_E7.length);
				}
				if (!thisHousehold.dish_marker)
				{
					Arrays.fill(thisProfile.D_HHspecific_dish, 0);
				}
				if (thisHousehold.wash_dry_marker)
				{
					thisProfile.D_HHspecific_wash = ArrayUtils.multiply(thisProfile.D_washer_E7, wash_scale_value);
					thisProfile.D_HHspecific_dryer = ArrayUtils.multiply(thisProfile.D_dryer_E7, wash_scale_value/3);
				}
			}

			thisProfile.D_HHspecific_washing = ArrayUtils.add(thisProfile.D_HHspecific_wash, thisProfile.D_HHspecific_dryer, thisProfile.D_HHspecific_dish);

			// section for lighting & misc demand
			// occupancy number effect on lighting/misc demand:
			int num_occ_temp = thisHousehold.num_occ;
			if (num_occ_temp > 4)
			{
				num_occ_temp = 4;
			}


			//Scale lighting and misc demand by occupancy - 
			// note that it is assumed that they scale by the same factor
			for (int i=0; i < Nspan; i++)
			{
				for ( int j=0; j < 48; j++)
				{
					thisProfile.D_HHspecific_lights[i * 48 + j] = thisProfile.D_lights[i * 48 + j]*MSConstArrays.light_occ_scale[j][num_occ_temp];
					thisProfile.D_HHspecific_misc[i * 48 + j] = thisProfile.D_misc[i * 48 + j]*MSConstArrays.light_occ_scale[j][num_occ_temp];
				}
			}



			// socio-economic effect on lighting demand:
			if (thisHousehold.income_num == 0)
			{
				int[] social_num_temp = new int[] {1, 2, 2, 3, 3, 4};
				int social_num_value = social_num_temp[thisHousehold.social_num];
				
				//Scale lighting and misc demand by social effects - 
				// note that it is assumed that they scale by the same factor
				for (int i=0; i < Nspan; i++)
				{
					for (int j=0; j < 48; j++)
					{
						thisProfile.D_HHspecific_lights[48*i + j] = thisProfile.D_HHspecific_lights[48*i + j]*MSConstArrays.light_social_scale[j][social_num_value];
						thisProfile.D_HHspecific_misc[48*i + j] = thisProfile.D_HHspecific_misc[48*i + j]*MSConstArrays.light_social_scale[j][social_num_value];
					}
				}
			}
			// income effect on lighting
			if (thisHousehold.income_num > 0)
			{
			
				for (int i=0; i < Nspan; i++)
				{
					for (int j=0; j < 48; j++)
					{
						thisProfile.D_HHspecific_lights[48*i + j] = thisProfile.D_HHspecific_lights[48*i + j]*MSConstArrays.light_income_scale[j][thisHousehold.income_num];
						thisProfile.D_HHspecific_misc[48*i + j] = thisProfile.D_HHspecific_misc[48*i + j]*MSConstArrays.light_income_scale[j][thisHousehold.income_num];
					}
				}
			}

			// section for total demand
			thisProfile.D_HHspecific_total = ArrayUtils.add(thisProfile.D_HHspecific_heat, thisProfile.D_HHspecific_water, thisProfile.D_HHspecific_cook, thisProfile.D_HHspecific_cold, thisProfile.D_HHspecific_washing, thisProfile.D_HHspecific_lights, thisProfile.D_HHspecific_misc);

			//calculates the half-hourly specific reactive component
			// assigns PF as follows: 
			//heating =1; water heat=1; lights=1; cooling=0.7;cooking=1.0;washing=0.93/drying=0.98/dishwashing=1;misc.=1.0
			//reactive load = active load*tan(phi), where phi=cos-1(PF)

			thisProfile.D_HHreactive_total = ArrayUtils.add(ArrayUtils.multiply(thisProfile.D_HHspecific_cold, 1.02 ), ArrayUtils.multiply( thisProfile.D_HHspecific_wash, 0.40), ArrayUtils.multiply(thisProfile.D_HHspecific_dryer, 0.20));

			for (int i=0; i < Nspan; i++)
			{
				for (int j=0; j < 48; j++)
				{
					thisProfile.PF_HH[48*i + j] = Math.cos(Math.atan(thisProfile.D_HHreactive_total[48*i + j]/thisProfile.D_HHspecific_total[48*i + j]));
				}
			}
		}

	}

	/**
	 * 
	 * Generates ownership model.  Currently simply uses the default generator to 
	 * provide a household which owns no appliances, with average socio-economic and physical
	 * characteristics and an occupancy of 2.
	 * 
	 * @return 
	 * 
	 */
	private static MSHouseholdCharacteristics ownership() {
		return new MSHouseholdCharacteristics();

	}

	/**
	 * Rather a place holder at the moment.  In the original (dates_1.m), this had user dialogues
	 * to determine the dates between which the profiles should be generated.  For us now,
	 * this simply does a year based upon a given date.  There may well be better ways...
	 * 
	 * @return
	 */
	private static Date[] dates_1() {
		// Original Matlab defined variables based on user input.  In particular:

		// dstart=start date of simulation (inclusive)
		// dend=inclusive

		// If the consumer type was non-domestic - these were forced to be a whole and single year
		// which was set to be the year of dstart (i.e. dend = Date(dstart.getYear(),12,31) 
		// dstart = Date(dstart.getYear(),1,1)

		// ystart1 = year of simulation start (i.e. dstart.getYear())
		// yend1 = year of simulation end (i.e. dend.getYear())

		// Ndays - number of days in the start year (element 1) and end year (element 2)
		// taking into account  leap years.

		// Nyears - number of years invlolved in the simulation (inclusive) i.e.
		// dend.getYear() - dstart.getYear()

		// BST_GMT - day of start year that clocks go from BST to GMT (i.e. last Sunday in October)
		// GMT_BST - day of start year that clocks go from GMT_TO BST (last sunday in March)
		// Note - this is only done in case that Nyears == 1
		//
		// Nspan = number of days of simulation (inclusive)		

		Date[] returnArray = new Date[2];
		Date startDate = new Date(2010,01,01);
		Date endDate = new Date(2010,12,31);
		returnArray[0] = startDate;
		returnArray[1] = endDate;
		return returnArray;
	}
	
	/**
	 * @param d_fridge
	 * @param b
	 * @return
	 */
	private static double[] convertToKWh(double[] array, boolean halfHourAverages) {
		
		double conversionFactor = 0;
		if (halfHourAverages)
		{
			conversionFactor = 0.5;
		}
		else
		{
			// one minute profile
			conversionFactor = 1d / 60;
		}
		
		
		return ArrayUtils.multiply(array, conversionFactor);
	}
	
	private static double[] one_min_profile(MSHalfHourProfile thisProfile, int startDayOfWeek, int time_leaveHH, int time_returnHH, MSHouseholdCharacteristics thisHouse, Date start)
	{
		
	int Nspan = thisProfile.D_HHspecific_wash.length;
	// sets all specific half-hourly demands for end-uses other than fridges, etc. + heating to zero

	//  during half-hours of non-occupancy on weekdays
	for (int i=0;  i< Nspan;  i++)
	{
		// With this formulation, we get back to the Matlab equivalent of
		// weekDay - i.e. 1-7 with 1 == Sunday.
		int thisDayOfWeek = (startDayOfWeek + i) % 7;
		if (thisDayOfWeek == 0)
			thisDayOfWeek = 7;
		
	    if (thisDayOfWeek < 7)
	    {
	        if (thisDayOfWeek >= 2)
	        {
	            Arrays.fill(thisProfile.D_HHspecific_lights,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_water,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_wash,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_dryer,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_dish,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_hob,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_oven,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_microwave,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_kettle,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	            Arrays.fill(thisProfile.D_HHspecific_misc,i*48 +time_leaveHH, i*48 + time_returnHH, 0);
	        }
	    }
	}

	return one_min(thisHouse, thisProfile, start, Nspan);
	}
	
	private static double[] one_min_wash_generate(MSHalfHourProfile thisHHProfile, int Nspan, int y, int washMachs, int dryers, int washer_dryers, int dishwashers)//y = year of simulation
	{
		double[] D_min_wash = new double[thisHHProfile.D_HHspecific_wash.length * 30];
		double[] D_min_wash_reactive = new double[thisHHProfile.D_HHspecific_wash.length * 30];
		double[] D_min_dish = new double[thisHHProfile.D_HHspecific_dish.length * 30];
		double[] D_min_dish_reactive = new double[thisHHProfile.D_HHspecific_dish.length * 30];
		double[] D_min_dry = new double[thisHHProfile.D_HHspecific_dryer.length * 30];
		double[] D_min_dry_reactive = new double[thisHHProfile.D_HHspecific_dryer.length * 30];


		boolean wash_marker = (washMachs > 0);
		boolean wash_dry_marker = (washer_dryers > 0);
		boolean dryer_marker = (dryers > 0);
		boolean dish_marker = (dishwashers > 0);

		/*D_min_washing (1:Nspan, 1:1440)=0;
		D_min_wash(1:Nspan,1:1440) = 0;
		D_min_dry(1:Nspan,1:1440) = 0;
		D_min_dish(1:Nspan,1:1440) = 0;
		D_min_wash_reactive(1:Nspan,1:1440)=0;
		D_min_dry_reactive(1:Nspan,1:1440) =0;*/

		//  washing machines
		//  first, the relative number of washes at 40, 60 and 90 deg is calculated:
		int a = 1;
		for (int i=0;  i< 8;  i++)
		{
			if ( y > MSConstArrays.trend_year_wash[i])
			{
				a=a+1;
			}
		}

		double b=(double)(y-MSConstArrays.trend_year_wash[a-1])/(MSConstArrays.trend_year_wash[a]-MSConstArrays.trend_year_wash[a-1]);
		double forty_num = MSConstArrays.forty_trend[a-1]+b*(MSConstArrays.forty_trend[a]-MSConstArrays.forty_trend[a-1]);
		double sixty_num = MSConstArrays.sixty_trend[a-1]+b*(MSConstArrays.sixty_trend[a]-MSConstArrays.sixty_trend[a-1]);
		double ninety_num = MSConstArrays.ninety_trend[a-1]+b*(MSConstArrays.ninety_trend[a]-MSConstArrays.ninety_trend[a-1]);


		int wash = 0; 
		//  determines whether wash events occur in washing machines & washer-dryers
		if ( wash_marker)
		{
			wash = 1;
		}

		if (wash_dry_marker)
		{
			wash = 1;
		}

		if ( wash == 1)
		{
			int wash_end = 0;
			int wash_start = 0;
			double wash_demand = 0;
			for (int i=0;  i< Nspan;  i++)
			{

				if ( i>0)
				{
					wash_end=wash_end-1440;
				}

				for (int p=0; p < 48; p++)
				{
					double R = RandomHelper.nextDouble();

					if ( R > (forty_num))
					{
						if ( R > (forty_num+sixty_num))
						{
							a=1 ;
							//  90 deg wash
							wash_demand = 1.58;
						}
						if ( R <= (forty_num+sixty_num))
						{
							a=2 ;
							//  60 deg wash
							wash_demand = 1.099;
						}
					}
					if ( R <= (forty_num))
					{
						a=3 ;
						//  40 deg wash
						wash_demand = 0.852;
					}
					double wash_chance = 0.61*thisHHProfile.D_HHspecific_wash[48*i + p]/wash_demand ;

					// factor of 0.61 added in to give 4.3 cycles/week on average (Mansouri)
					R=RandomHelper.nextDouble() ;                                          
					if ( R < wash_chance)
					{
						wash_start=(int)(RandomHelper.nextDouble()*30);
						if ( wash_start > 30)
						{
							wash_start=30;
						}
						if ( wash_start==0)
						{
							wash_start=1;
						}
						wash_start = ((p-1)*30) + wash_start;

						int wash_flag = 0;
						if ( wash_start <= wash_end)
						{
							wash_flag = 0;
						}
						if ( wash_start > wash_end)
						{
							wash_flag = 1;
						}
						if ( wash_flag >0)
						{
							if ( a==1)
							{
								wash_end = wash_start+86;
								for (int q=0;  q< 86;  q++)
								{
									if ( wash_start+q-1 <= 1440)
									{
										D_min_wash[i*48+wash_start+q-1]=MSConstArrays.ninety_wash[q];
										D_min_wash_reactive[i*48+wash_start+q+1]=MSConstArrays.ninety_wash_reactive[q];
									}
									if ( wash_start+q-1 > 1440)
									{
										if ( (i+1) <= Nspan)
										{
											D_min_wash[(i+1)*48+wash_start+q-1]=MSConstArrays.ninety_wash[q];
											D_min_wash_reactive[(i+1)*48+wash_start+q-1]=MSConstArrays.ninety_wash_reactive[q];
										}
									}
								}
							}
							if ( a==2)
							{
								wash_end=wash_start+58;
								for (int q=0;  q< 58;  q++)
								{
									if ( wash_start+q-1 <= 1440)
									{
										D_min_wash[i*48+wash_start+q-1]=MSConstArrays.sixty_wash[q];
										D_min_wash_reactive[i*48+wash_start+q-1]=MSConstArrays.sixty_wash_reactive[q];
									}
									if ( wash_start+q-1 > 1440)
									{
										if ( (i+1) <= Nspan)
										{
											D_min_wash[(i+1)*48+((wash_start+q-1)-1440)]=MSConstArrays.sixty_wash[q];
											D_min_wash_reactive[(i+1)*48+((wash_start+q-1)-1440)]=MSConstArrays.sixty_wash_reactive[q];
										}
									}
								}
							}
							if ( a==3)
							{
								wash_end=wash_start+48;
								for (int q=0;  q< 48;  q++)
								{
									if ( wash_start+q-1 <= 1440)
									{
										D_min_wash[i*48+wash_start+q-1]=MSConstArrays.forty_wash[q];
										D_min_wash_reactive[i*48+wash_start+q-1]=MSConstArrays.forty_wash_reactive[q];
									}
									if ( wash_start+q-1 > 1440)
									{
										if ( (i+1) <= Nspan)
										{
											D_min_wash[(i+1)*48+((wash_start+q-1)-1440)]=MSConstArrays.forty_wash[q];
											D_min_wash_reactive[(i+1)*48+((wash_start+q-1)-1440)]=MSConstArrays.forty_wash_reactive[q];
										}
									}
								}
							}                
						}              
					}
				}
			}
		}


		//  tumble-dryers
		int dry = 0;
		if ( dryer_marker)
		{
			dry = 1;
		}
		if ( wash_dry_marker)
		{
			dry = 1;
		}

		if ( dry == 1)
		{

			int dry_end = 0;
			int dry_start = 0;
			for (int i=0;  i< Nspan;  i++)
			{
				if ( i==1)
				{
					dry_end=0;
				}
				if ( i>1)
				{
					dry_end=dry_end-1440;
				}

				for (int p=0;  p< 48;  p++)
				{
					double dry_chance = thisHHProfile.D_HHspecific_dryer[i*48+p]/2.29;

					double R=RandomHelper.nextDouble();
					if ( R < dry_chance)
					{
						double R1=RandomHelper.nextDouble();
						if ( R1 > 0.75 )
						{
							//  90min cycle
							a=1;
						}
						if ( R1 <= 0.75 )
						{
							//  120 min cycle
							a=2;
						}
						dry_start = (int)(RandomHelper.nextDouble()*30);
						if ( dry_start > 30)
						{
							dry_start = 30;
						}
						dry_start = ((p-1)*30)+dry_start;
						int dry_flag = 0;
						if ( dry_start <= dry_end)
						{
							dry_flag = 0;
						}
						if ( dry_start > dry_end)
						{
							dry_flag = 1;
						}
						if ( dry_flag > 0)
						{
							if ( a ==1)
							{
								dry_end = dry_start+90;
								for (int q=0;  q< 90;  q++)
								{
									if ( dry_start+q-1 <= 1440)
									{
										D_min_dry[i*48+dry_start+q-1]=MSConstArrays.dry_event[q] ;
										D_min_dry_reactive[i*48+dry_start+q-1]=MSConstArrays.dry_event_reactive[q] ;
									}
									if ( dry_start+q-1 > 1440)
									{
										if ( (i+1) <= Nspan)
										{
											D_min_dry[(i+1)*48+((dry_start+q-1)-1440)]=MSConstArrays.dry_event[q] ;
											D_min_dry_reactive[(i+1)*48+((dry_start+q-1)-1440)]=MSConstArrays.dry_event_reactive[q] ;
										}
									}
								}
							}
							if ( a == 2)
							{
								dry_end = dry_start + 120;
								for (int q=0;  q< 120;  q++)
								{
									if ( dry_start+q-1 <= 1440)
									{
										D_min_dry[i*48+dry_start+q-1]=MSConstArrays.dry_event[q];
										D_min_dry_reactive[i*48+dry_start+q-1]=MSConstArrays.dry_event_reactive[q];
									}
									if ( dry_start+q-1 >1440)
									{
										if ( (i+1) <= Nspan)
										{
											D_min_dry[(i+1)*48+((dry_start+q-1)-1440)]=MSConstArrays.dry_event[q];
											D_min_dry_reactive[(i+1)*48+((dry_start+q-1)-1440)]=MSConstArrays.dry_event_reactive[q];
										}
									}
								}
							}
						}
					}
				}
			} 
		}


		//  dishwashers

		a=1;
		for (int i=0;  i< 7;  i++)
		{
			if ( y > MSConstArrays.trend_year_dish[i])
			{
				a=a+1;
			}
		}
		b=(y-MSConstArrays.trend_year_dish[a-1])/(MSConstArrays.trend_year_dish[a]-MSConstArrays.trend_year_dish[a-1]);
		double dish_temp = MSConstArrays.dish_temp_trend[a-1]+b*(MSConstArrays.dish_temp_trend[a]-MSConstArrays.dish_temp_trend[a-1]);
		if ( y < 1990)
		{
			dish_temp = 0.68;
		}

		if ( dish_marker)
		{

			int dish_end = 0;
			int dish_start = 0;
			for (int i=0;  i< Nspan;  i++)
			{
				if ( i==1)
				{
					dish_end=0;
				}
				if ( i>1)
				{
					dish_end=dish_end-1440;
				}
				for (int p=0;  p< 48;  p++)
				{
					double R=RandomHelper.nextDouble();
					double R1 = RandomHelper.nextDouble();
					int n = 0;
					if ( R <= 0.4 )
					{
						//  programme A
						if ( R1 <= dish_temp )
						{
							//  65 deg wash
							n=1;
						}
						if ( R1 > dish_temp )
						{
							//  55 deg wash
							n=2;
						}
					}
					if ( R > 0.4 )
					{
						//  programme B
						if ( R1 <= dish_temp)
						{
							n=3;
						}
						if ( R1 > dish_temp)
						{
							n=4;
						}
					}
					R= RandomHelper.nextDouble();
					double dish_event_chance = 0.33*thisHHProfile.D_HHspecific_dish[i*48+p]/MSConstArrays.dish_chance[n];

					//  0.33 factor used to make average 0.76 events/day (Mansouri)
					if ( R < dish_event_chance)
					{
						dish_start = (int)(RandomHelper.nextDouble()*30);
						if ( dish_start == 0)
						{
							dish_start=1;
						}
						if ( dish_start > 30)
						{
							dish_start = 30;
						}
						dish_start = ((p-1)*30)+dish_start;
						int dish_flag = 0;
						if ( dish_start <= dish_end)
						{
							dish_flag = 0;
						}
						if ( dish_start > dish_end)
						{
							dish_flag = 1;
						}
						if ( dish_flag > 0)
						{
							dish_end = dish_start + 76;
							for (int q=0;  q< 76;  q++)
							{
								if ( dish_start+q-1 <= 1440)
								{
									D_min_dish[i*48+dish_start+q-1]=MSConstArrays.dish_event[q][n];
								}
								if ( dish_start+q-1 >1440)
								{
									if ( (i+1) <= Nspan)
									{
										D_min_dish[(i+1)*48+((dish_start+q-1)-1440)]=MSConstArrays.dish_event[q][n];
									}
								}
							}
						}
					}
				}
			}
		}	
		
		double[] D_min_washing = ArrayUtils.add(D_min_wash, D_min_dry, D_min_dish);
		double[] D_min_washing_reactive = ArrayUtils.add(D_min_wash_reactive, D_min_dry_reactive);
		// no reactive load for dishwasher (no spin cycle!)

		return D_min_washing;
	}
	
	private static double[] aggregate_one_min_kWh_to_half_hour_kWh(double[] one_min_prof)
	{
		double[] returnArr = new double[one_min_prof.length / 30];
		for (int i = 0; i < one_min_prof.length; i += 30)
		{
			int sum = 0;
			for (int j = 0; j < 30; j++)
			{
				sum += one_min_prof[i+j];
			}
			returnArr[i/30] = sum;
		}
		return returnArr;
	}
}
