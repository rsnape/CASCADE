/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import repast.simphony.random.RandomHelper;


/**
 * @author jsnape
 *
 */
public class EVProfileGenerator
{
	private static double BEVcapacity = 24;
	private static double PHEVcapacity = 16;
	private static double BEVconsumption =0.17;
	private static double PHEVconsumption=0.2;

	
	public static double[] generateBEVProfile(CascadeContext context, int days)
	{
		double[] profile = new double[days*context.ticksPerDay];

		for (int i = 0; i < days; i++)
		{
			if (RandomHelper.nextDouble() < Consts.JOURNEY_MADE_PROBABILITY)
			{
			int arrTimeslot = context.vehicleArrivalGenerator.nextInt();
			int journeyLength = context.journeyLengthGenerator.nextInt();
			if (journeyLength < 4)
			{
				journeyLength = 4;
			}
			double requiredCharge = journeyLength * BEVconsumption;
			if (requiredCharge > BEVcapacity)
			{
				requiredCharge = BEVcapacity;
			}
			double chargePeriod = context.ticksPerDay-context.ticksPerDay + 14;
			
			double chargePerTimeslot = requiredCharge / chargePeriod;
			
			int start = i*context.ticksPerDay+arrTimeslot;
			if (i==days-1)
			{
				//Need to "wrap the profile"
				for (int j = start; j < profile.length; j++)
				{
					profile[j]=chargePerTimeslot;
				}

				for (int j = 0; j < 14; j++)
				{
					profile[j]=chargePerTimeslot;
				}
			}
			else
			{
				int stop = (i+1)*context.ticksPerDay + 14;
				for (int j = start; j < stop; j++)
				{
					profile[j]=chargePerTimeslot;
				}
			}
			
			}
		}
		
		return profile;
	
	}
	
	public static double[] generatePHEVProfile(CascadeContext context, int days)
	{
		double[] profile = new double[days*context.ticksPerDay];
		for (int i = 0; i < days; i++)
		{
			if (RandomHelper.nextDouble() < Consts.JOURNEY_MADE_PROBABILITY)
			{
			int arrTimeslot = context.vehicleArrivalGenerator.nextInt();
			int journeyLength = context.journeyLengthGenerator.nextInt();
			if (journeyLength < 4)
			{
				journeyLength = 4;
			}
			double requiredCharge = journeyLength * PHEVconsumption;
			if (requiredCharge > PHEVcapacity)
			{
				requiredCharge = PHEVcapacity;
			}
			double chargePeriod = context.ticksPerDay-context.ticksPerDay + 14;
			
			double chargePerTimeslot = requiredCharge / chargePeriod;
			
			int start = i*context.ticksPerDay+arrTimeslot;
			if (i==days-1)
			{
				//Need to "wrap the profile"
				for (int j = start; j < profile.length; j++)
				{
					profile[j]=chargePerTimeslot;
				}

				for (int j = 0; j < 14; j++)
				{
					profile[j]=chargePerTimeslot;
				}
			}
			else
			{
				int stop = (i+1)*context.ticksPerDay + 14;
				for (int j = start; j < stop; j++)
				{
					profile[j]=chargePerTimeslot;
				}
			}
			
			}
		}
		
		return profile;
	}

}
