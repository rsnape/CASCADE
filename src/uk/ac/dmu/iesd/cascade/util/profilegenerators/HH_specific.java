package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import repast.simphony.random.RandomHelper;
import uk.ac.dmu.iesd.cascade.util.ArrayUtils;
import java.util.Arrays;
import java.util.Date;

class HH_specific
{

	//this module scales or modifies the group average half-hourly demand
	//based on ownership assignment for }-uses and occupancy

	public HH_specific(MSHouseholdCharacteristics thisHousehold, boolean MSownershipCalc, MSHalfHourProfile thisProfile, Date startDate, int Nspan)
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
	 * @return 
	 * 
	 */
	private MSHouseholdCharacteristics ownership() {
		return new MSHouseholdCharacteristics();

	}
}