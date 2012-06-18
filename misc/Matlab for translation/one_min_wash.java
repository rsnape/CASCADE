package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.MSHalfHourProfile;
import uk.ac.dmu.iesd.cascade.util.profilegenerators.MSOneMinProfile;

public class one_min_wash
{
	//  this submodule calculates the one minute demands for washing from the specif (ic half-hourly demand)

	
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



}



