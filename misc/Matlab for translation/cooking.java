package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import repast.simphony.random.RandomHelper;

public class cooking
{
	//This sub-model calculates the half-hourly group average domestic electricity demand

	//for cooking


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
}