import java.util.Arrays;

public class one_min_profile
{
//  this module applies a 'lock-out' to account for occupants 

//  being out of the house on weekdays between defined times
	//time_leaveHH contains the half hour in which the occupants leave the house on a weekday
	// and time_returnHH the time the slot in which they return.
one_min_profile(MSHalfHourProfile thisProfile, int startDayOfWeek, int time_leaveHH, int time_returnHH)
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

one_min();
}

/**
 * 
 */
private void one_min() {
	// TODO Auto-generated method stub
	
}
