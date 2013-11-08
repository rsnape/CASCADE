package uk.ac.dmu.iesd.cascade.market.astem.base;


/**
 * This class defines the constants that are used to control
 * different aspect of the ASTEM (market) model. 
 * 
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/02/06
 */

public final class ASTEMConsts {
	
	/***********************************
	 * Model constants
	 **********************************/

	/*-----------------
	 * Model control 
	 *-----------------*/	

	public static String FILE_CHART_FORMAT_EXT = ".png";
	
	//datFiles directory is supposed to be find under the current directory which
	// would be normally where this project resides in Eclipse working space   
	//public static String DATA_FILES_FOLDER_NAME = "\\dataFiles"; //data files are us
	
	public static String BMU_BASE_PROFILES_FILENAME = "BMU_BaseProfiles.csv";  //DEM and GEN profiles


	/*---------------------
	 * Units of Measurements
	 *----------------------*/	
	public static int T_PER_DAY = 48; //time/tick (interval) per day	
	public static final int MINUTES_PER_DAY = 1440;
	public static final int DAYS_PER_YEAR = 360;
	public static int SUMMER=180;
	
	public static int MONDAY=0;
	public static int TUESDAY=1;
	public static int WENDSDAY=2;
	public static int THURSDAY=3;
	public static int FRIDAY=4;
	public static int SATURDAY=5;
	public static int SUNDAY=6;
	

	/*-----------------
	 * Exceptions' IDs 
	 *-----------------*/	
	public static int EXCEPT_DIFFERENT_CLASS_EXPECTED = 1;
	public static int EXCEPT_FILE_NOT_FOUND = 2;
	public static int EXCEPT_ARRAY_SIZES_DIFFERNT = 3;
	
	/***********************************
	 * BMU 
	 **********************************/

	public static int BMU_BO_NUM_OF_CHOICE= 10;
	
	public static int BMU_COAL_MAXCAP= 1200;
	public static int BMU_CCGT_MAXCAP= 800;
	public static int BMU_WIND_MAXCAP= 150;
	
	public static int BMU_LARGEDEM_MAXDEM = -1000;
	public static int BMU_SMALLDEM_MAXDEM = -500;
	
	public static int BMU_LARGEDEM_MINDEM = -300;
	public static int BMU_SMALLDEM_MINDEM = -100;
	
	public static double BMU_MARGINPC_GEN_ = 0.05;
	
	
	/*---------------------
	 * BMU types 
	 *---------------------*/
	//public static int BMU_TYPE_T_GEN = 1; //directly connected to Transmission sys. (e.g. large generation plants)
	//public static int BMU_TYPE_E_GENDEM = 2; //[Plants]: unites Embedded into a dist. sys. (e.g. DGs or large demand sites)
	//public static int BMU_TYPE_I_GENDEM = 3; //[Demand sites]:unites relating to an Interconnector
	//public static int BMU_TYPE_S_DEM = 4; //[Demand sites]:unites relating to Suppliers (e.g. aggregate demand of suppliers' consumers)

	
	
	/*---------------------
	 * PX Product type IDs 
	 *---------------------*/
	public static int PX_PRODUCT_ID_2H = 1;  //12x4 sp = 48
	public static int PX_PRODUCT_ID_4H = 4;  //6x8  sp = 48
	public static int PX_PRODUCT_ID_8H = 8; //3x16 sp = 48

	/*---------------------
	 * PX Imbalance Volume Threshold Factor 
	 *---------------------*/
	public static int PX_IMBAL_FACTOR = 20;  //12x4 sp = 48
	public static double PX_IMBAL_MULTIFACTOR = 0.8;  //12x4 sp = 48
	
	
	/*
	public static enum IMBAL_TYPE {
		TWO_HOUR, FOUR_HOUR, EIGHT_HOUR, 
	} */
		
	/*------------------------
	 * Scheduling Priorities  
	 *-------------------------*/
	/*public static final double PRIORITY_FIRST  = 500;  // BMU
	public static final double PRIORITY_SECOND = 400;  // so
	public static final double PRIORITY_THIRD  = 300;  // sc
	public static final double PRIORITY_FOURTH = 200;  // px  */
	
	
	/*------------------------
	 * Roth-Erev Learner Parameters  
	 *-------------------------*/
	public static double EXP_COAL = 0.5;
	public static double REG_COAL = 0.9;
	
	public static double EXP_CCGT = 0.5;
	public static double REG_CCGT = 0.9;
	
	public static double EXP_WIND = 0.5;
	public static double REG_WIND = 0.9;
	
	public static double EXP_LARGEDEM = 0.5;
	public static double REG_LARGEDEM = 0.9;
	
	public static double EXP_SMALLDEM = 0.5;
	public static double REG_SMALLDEM = 0.9;
		
	
}

