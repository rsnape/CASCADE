package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import repast.simphony.random.RandomHelper;


// This module calculates the half-hourly average demands for lighting
// 

// Lighting demand parameters for weekdays:

public class lighting
{

	public lighting(MSHalfHourProfile thisProfile, double startDayOfYear, int startDayOfWeek, int Nspan, int consumer_type, int BST_GMT, int GMT_BST)
	{
		// this section only works for test days in a single year at present due to BST / GMT stuff

		// checks the weekday type, sets parameter BST (phase shif (t for autumn clock change to 0)

		// checks to see if ( date if before or after clock change in test year; if it is BST=34)


		// for each half-hour, calculates the two annual sine tr}s and totals them

		// if ( the total is under the min level, sets demand to min; if over max level, sets demand to max)


		//  adds on a normally distributed random number - mean 0, std dev 1 & scales by appropriate std dev for model

		// sets negative demands to zero


		// domestic consumers - dif (ferent lighting demands, dep}ing on day type)

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
						if ( startDayOfYear + i > BST_GMT)
						{
							BST=34;
						}
						if ( startDayOfYear + i < GMT_BST)
						{
							BST=34;
						}
						for (int HH=0;  HH< 48;  HH++)
						{
							int n=0;
							d_lights_sine1[48*i + HH]=MSConstArrays.lighting_sinescale1_sun[HH]*Math.sin((2*Math.PI*((startDayOfYear + i+BST)/365)-MSConstArrays.lighting_sinephase1_sun[HH]));
							d_lights_sine2[48*i + HH]=MSConstArrays.lighting_sinescale2_sun[HH]*Math.sin((2*Math.PI*((startDayOfYear + i-BST)/365)-MSConstArrays.lighting_sinephase2_sun[HH]))+MSConstArrays.lighting_const_sun[HH];
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
					if ( startDayOfYear + i > BST_GMT)
					{
						BST=34;
					}
					if ( startDayOfYear + i <GMT_BST)
					{
						BST=34;
					}
					for (int HH=0;  HH< 48;  HH++)
					{
						int n=0;
						d_lights_sine1[48*i + HH]=MSConstArrays.lighting_sinescale1_wkdays[HH]*Math.sin((2*Math.PI*((startDayOfYear + i+BST)/365)-MSConstArrays.lighting_sinephase1_wkdays[HH]));
						d_lights_sine2[48*i + HH]=MSConstArrays.lighting_sinescale2_wkdays[HH]*Math.sin((2*Math.PI*((startDayOfYear + i-BST)/365)-MSConstArrays.lighting_sinephase2_wkdays[HH]))+MSConstArrays.lighting_const_wkdays[HH];
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
					if ( startDayOfYear + i > BST_GMT)
					{
						BST=34;
					}
					if ( startDayOfYear + i < GMT_BST)
					{
						BST=34;
					}
					for (int HH=0;  HH< 48;  HH++)
					{
						int n=0;
						d_lights_sine1[48*i + HH]=MSConstArrays.lighting_sinescale1_sat[HH]*Math.sin((2*Math.PI*((startDayOfYear + i+BST)/365)-MSConstArrays.lighting_sinephase1_sat[HH]));
						d_lights_sine2[48*i + HH]=MSConstArrays.lighting_sinescale2_sat[HH]*Math.sin((2*Math.PI*((startDayOfYear + i-BST)/365)-MSConstArrays.lighting_sinephase2_sat[HH]))+MSConstArrays.lighting_const_sat[HH];
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
				if ( startDayOfYear + i > BST_GMT)
				{
					BST=34;
				}
				if ( startDayOfYear + i < GMT_BST)
				{
					BST=34;
				}
				for (int HH=0;  HH< 48;  HH++)
				{
					int n=0;
					d_lights_sine1[48*i + HH]=MSConstArrays.lighting_sinescale1_wkdays[HH]*Math.sin((2*Math.PI*((startDayOfYear + i+BST)/365)-MSConstArrays.lighting_sinephase1_wkdays[HH]));
					d_lights_sine2[48*i + HH]=MSConstArrays.lighting_sinescale2_wkdays[HH]*Math.sin((2*Math.PI*((startDayOfYear + i-BST)/365)-MSConstArrays.lighting_sinephase2_wkdays[HH]))+MSConstArrays.lighting_const_wkdays[HH];
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
}
