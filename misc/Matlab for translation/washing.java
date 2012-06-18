package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import java.util.Arrays;

import repast.simphony.random.RandomHelper;

//this sub-model is for half-hourly electricty demand for washing appliances
public class washing
{
	//it covers washers, dryers, washer-dryers combined and dishwashers




		private static void washing(MSHalfHourProfile thisProfile, double startDayOfYear, int startDayOfWeek, int Nspan, int consumer_type)
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
							thisProfile.d_washer_UR[48*i + HH]=MSConstArrays.scale_washer_mon_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_washer_mon_UR[HH])+MSConstArrays.const_washer_mon_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_mon_UR[HH]) ; 
							thisProfile.d_dryer_UR[48*i + HH]=MSConstArrays.scale_dryer_mon_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dryer_mon_UR[HH])+MSConstArrays.const_dryer_mon_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_mon_UR[HH])  ;
							thisProfile.d_dish_UR[48*i + HH]=MSConstArrays.scale_dish_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dish_UR[HH])+MSConstArrays.const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_UR[HH])  ;
							thisProfile.d_washer_E7[48*i + HH]=MSConstArrays.scale_washer_mon_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_washer_mon_E7[HH])+MSConstArrays.const_washer_mon_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_mon_E7[HH]);
							thisProfile.d_dryer_E7[48*i + HH]=MSConstArrays.scale_dryer_mon_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dryer_mon_E7[HH])+MSConstArrays.const_dryer_mon_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_mon_E7[HH]);
							thisProfile.d_dish_E7[48*i + HH]=MSConstArrays.scale_dish_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dish_E7[HH])+MSConstArrays.const_dish_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_E7[HH]);
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
							thisProfile.d_washer_UR[48*i + HH]=MSConstArrays.scale_washer_sun_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_washer_sun_UR[HH])+MSConstArrays.const_washer_sun_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_sun_UR[HH]);
							thisProfile.d_dryer_UR[48*i + HH]=MSConstArrays.scale_dryer_sun_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dryer_sun_UR[HH])+MSConstArrays.const_dryer_sun_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_sun_UR[HH]) ;   
							thisProfile.d_dish_UR[48*i + HH]=MSConstArrays.scale_dish_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dish_UR[HH])+MSConstArrays.const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_UR[HH])  ;
							thisProfile.d_washer_E7[48*i + HH]=MSConstArrays.scale_washer_sun_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_washer_sun_E7[HH])+MSConstArrays.const_washer_sun_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_sun_E7[HH]);
							thisProfile.d_dryer_E7[48*i + HH]=MSConstArrays.scale_dryer_sun_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dryer_sun_E7[HH])+MSConstArrays.const_dryer_sun_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_sun_E7[HH]);
							thisProfile.d_dish_E7[48*i + HH]=MSConstArrays.scale_dish_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dish_E7[HH])+MSConstArrays.const_dish_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_E7[HH]);
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
							thisProfile.d_washer_UR[48*i + HH]=MSConstArrays.scale_washer_sat_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_washer_sat_UR[HH])+MSConstArrays.const_washer_sat_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_sat_UR[HH]);
							thisProfile.d_dryer_UR[48*i + HH]=MSConstArrays.scale_dryer_sat_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dryer_sat_UR[HH])+MSConstArrays.const_dryer_sat_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_sat_UR[HH]) ;   
							thisProfile.d_dish_UR[48*i + HH]=MSConstArrays.scale_dish_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dish_UR[HH])+MSConstArrays.const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_UR[HH])  ;
							thisProfile.d_washer_E7[48*i + HH]=MSConstArrays.scale_washer_sat_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_washer_sat_E7[HH])+MSConstArrays.const_washer_sat_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_sat_E7[HH]);
							thisProfile.d_dryer_E7[48*i + HH]=MSConstArrays.scale_dryer_sat_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dryer_sat_E7[HH])+MSConstArrays.const_dryer_sat_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_sat_E7[HH]);
							thisProfile.d_dish_E7[48*i + HH]=MSConstArrays.scale_dish_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dish_E7[HH])+MSConstArrays.const_dish_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_E7[HH]);
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
							thisProfile.d_washer_UR[48*i + HH]=MSConstArrays.scale_washer_wkdays_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_washer_wkdays_UR[HH])+MSConstArrays.const_washer_wkdays_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_wkdays_UR[HH]);
							thisProfile.d_dryer_UR[48*i + HH]=MSConstArrays.scale_dryer_wkdays_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dryer_wkdays_UR[HH])+MSConstArrays.const_dryer_wkdays_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_wkdays_UR[HH]) ;   
							thisProfile.d_dish_UR[48*i + HH]=MSConstArrays.scale_dish_UR[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dish_UR[HH])+MSConstArrays.const_dish_UR[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_UR[HH]) ;
							thisProfile.d_washer_E7[48*i + HH]=MSConstArrays.scale_washer_wkdays_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_washer_wkdays_E7[HH])+MSConstArrays.const_washer_wkdays_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_washer_wkdays_E7[HH]);
							thisProfile.d_dryer_E7[48*i + HH]=MSConstArrays.scale_dryer_wkdays_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dryer_wkdays_E7[HH])+MSConstArrays.const_dryer_wkdays_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dryer_wkdays_E7[HH]);
							thisProfile.d_dish_E7[48*i + HH]=MSConstArrays.scale_dish_E7[HH]*Math.sin((2*Math.PI*(startDayOfYear + i/365))-MSConstArrays.phase_dish_E7[HH])+MSConstArrays.const_dish_E7[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_dish_E7[HH]);
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
	}
