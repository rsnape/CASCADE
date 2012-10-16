package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import repast.simphony.random.RandomHelper;

public class cooling
{
	// This sub-model calculates the half-hourly group average domestic electricity demand for cooling

	public cooling(MSHalfHourProfile thisProfile, double startDayOfYear, int startDayOfWeek, int Nspan)
	{
		// this section calculates demand for fridges. The demand is the same basic annual tr} but scaled for each half-hour:
		for (int i=0;  i< Nspan;  i++)
		{
			for (int HH=0;  HH< 48;  HH++)
			{
				thisProfile.d_fridge[48*i + HH]=MSConstArrays.scale_fridge[HH]*Math.sin(2*Math.PI*(startDayOfYear + i/365)-MSConstArrays.phase_fridge[HH])+MSConstArrays.const_fridge[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_fridge[HH]);
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
				thisProfile.d_freezer[48*i + HH]=MSConstArrays.scale_freezer[HH]*Math.sin(2*Math.PI*(startDayOfYear + i/365)-2.05)+MSConstArrays.const_freezer[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_freezer[HH]);
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
				thisProfile.d_fridge_freezer[48*i + HH]=MSConstArrays.scale_fridge_freezer[HH]*Math.sin(2*Math.PI*(startDayOfYear + i/365)-MSConstArrays.phase_fridge_freezer[HH])+MSConstArrays.const_fridge_freezer[HH]+(RandomHelper.getNormal().nextDouble()*MSConstArrays.stddev_fridge_freezer[HH]);
				if ( thisProfile.d_fridge_freezer[48*i + HH] < 0)
				{
					thisProfile.d_fridge_freezer[48*i + HH] = 0;
				}
			}
		}
	}
}