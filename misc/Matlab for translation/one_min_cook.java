%this sub-module calculates the one-minute averaged demands for cooking from the assigned half-hourly demand
D_min_kettle(1:Nspan,1:1440) = 0;
D_min_micro(1:Nspan,1:1440)=0;
D_min_hob(1:Nspan,1:1440) = 0;
D_min_oven(1:Nspan,1:1440)=0;

%this section sets up the probability of hob and oven events occuring (based on MTP data for no. of events/annum)
trend_year_5=[1970 1980 1990 2010 2020];
hob_use_trend=[539 408 369 369 369]/365;
oven_use_trend=[586 280 223 175 142]/365;

a=1;
for i=1:5
    if y > trend_year_5(i)% finds where current year fits
        a=a+1;
    end
end
b=(y-trend_year_5(a-1))/(trend_year_5(a)-trend_year_5(a-1));
hob_use_value = hob_use_trend(a-1)+b*(hob_use_trend(a)-hob_use_trend(a-1)); %interpolates between values
oven_use_value = oven_use_trend(a-1)+b*(oven_use_trend(a)-oven_use_trend(a-1));

hob_marker1=0;
hob_marker2=0;
hob_marker3=0;
hob_marker4=0;

if hob_marker == 1 % hob is owned
    hob_end(1:4) = 0;
    hob_start(1:4)=0;
    hob_duration(1:4)=0;
    hob_demand_cycle = [1	0	0	0	0
                        2	0	0	0	0
                        2	1	0	0	0
                        2	2	0	0	0
                        2	2	1	0	0
                        2	2	2	0	0
                        2	2	2	1	0
                        2	2	2	2	0
                        2	2	2	2	1
                        2	2	2	2	2] ;% typical 5 minute ring demand cycle for settings 1 to 10
    
    R=rand ;% determines which hob rings are used
    hob_marker1=1;
    hob_marker2=1;
    hob_marker3=1;
    hob_marker4=1;
    if R<= 0.554
      hob_marker4=0;
      if R<=0.461
         hob_marker3 = 0;
         if R<= 0.165
             hob_marker2=0;
             if R<= 0.024
               hob_marker1=0;
             end
         end
      end
    end
    for i=1:Nspan
        for p=1:48
            if hob_marker1 ==1
                R=rand; %ring 1
                hob_duration(1)=0;
                if R < 0.201
                    hob_duration(1)=round(rand*10)+5;% sets minimum demand to 5min
                end
                if R >= 0.201
                        hob_duration(1)=round(rand*15)+15;
                        if R>= 0.637
                            hob_duration(1)=round(rand*15)+30;
                            if R >=0.846
                                hob_duration(1)=round(rand*15)+45;
                                if R>=0.939
                                    hob_duration(1)=round(rand*60)+60;
                                    if R>=0.975
                                        hob_duration(1)=round(rand*15)+120;
                                    end
                                end
                            end
                        end
                 end
            end
            if hob_marker1 == 0
               hob_duration(1)=0;
            end
            if hob_marker2 == 1
                R=rand ;% ring 2
                hob_duration(2)=0;
                if R < 0.356
                    hob_duration(2)=round(rand*10)+5;% sets minimum demand to 5min
                end
                if R >= 0.356
                        hob_duration(2)=round(rand*15)+15;
                        if R>= 0.756
                            hob_duration(2)=round(rand*15)+30;
                            if R >=0.924
                                hob_duration(2)=round(rand*15)+45;
                                if R>=0.953
                                    hob_duration(2)=round(rand*60)+60;
                                    if R>=0.977
                                        hob_duration(2)=round(rand*15)+120;
                                    end
                                end
                            end
                        end
                 end
            end
            if hob_marker2 == 0 
                hob_duration(2)=0;
            end
            if hob_marker3 == 1
                R=rand ;% ring 3
                hob_duration(3)=0;
                if R < 0.539
                    hob_duration(3)=round(rand*10)+5;% sets minimum demand to 5min
                end
                if R >= 0.539
                        hob_duration(3)=round(rand*15)+15;
                        if R>= 0.845
                            hob_duration(3)=round(rand*15)+30;
                            if R >=0.932
                                hob_duration(3)=round(rand*15)+45;
                                if R>=0.954
                                    hob_duration(3)=round(rand*60)+60;
                                    if R>=0.982
                                        hob_duration(3)=round(rand*15)+120;
                                    end
                                end
                            end
                        end
               end
            end
            if hob_marker3 == 0 
                hob_duration(3)=0;
            end
            if hob_marker4 == 1
                R=rand ;% ring 4
                hob_duration(4)=0;
                if R < 0.669
                    hob_duration(4)=round(rand*10)+5;% sets minimum demand to 5min
                end
                if R >= 0.669
                        hob_duration(4)=round(rand*15)+15;
                        if R>= 0.846
                            hob_duration(4)=round(rand*15)+30;
                            if R >=0.924
                                hob_duration(4)=round(rand*15)+45;
                                if R>=0.946
                                    hob_duration(4)=round(rand*60)+60;
                                    if R>=0.977
                                        hob_duration(4)=round(rand*15)+120;
                                    end
                                end
                            end
                        end
                end
            end
            if hob_marker4 == 0 
                hob_duration(4)=0;
            end
            hob_chance = D_HHspecific_hob(i,p)/(1.5*1.1/hob_use_value); %1.5 kW HH av event gives a daily hob use of 1.1 (for 2 people occ), hob_use_value adjusts to give the MTP trend value
            R=rand;
            if R <= hob_chance
                for a1=1:4
                    if hob_duration(a1) > 0
                        hob_setting(a1)=round(rand*9);% chooses a hob setting between 1 and 10
                        if hob_setting(a1) == 0
                            hob_setting(a1) = 1;
                        end
                        u=hob_duration(a1)/5 ;% no. 5 min cycles
                        v=round(u);
                        if v > u
                            s2=v-1 ;% s stores whole no. of cycles
                            s1=u-v ;% remainder after 5min cycles complete
                        end
                        if v <= u
                            s2=v;
                            s1=u-v;
                        end
                        hob_start(a1)=round(rand*30);% random start time
                        hob_start(a1) = ((p-1)*30)+hob_start(a1) ;% sets up absolute start time in day
                        if hob_start(a1) > hob_end(a1) % checks to see if another event is already running
                                hob_end(a1) = hob_start(a1) + hob_duration(a1) ;% if not, sets up absolute end time in day
                                for f=1:s2 % assigns demand for the whole 5min cycles
                                   for g=1:5
                                      t=hob_start(a1)+(f-1)*5+g;
                                       if t <= 1440
                                            D_min_hob(i,t)= hob_demand_cycle(hob_setting(a1),g)+ D_min_hob(i,t);
                                       end
                                       if t > 1440
                                            if (i+1) <= Nspan
                                                D_min_hob(i+1,t-1440) = hob_demand_cycle(hob_setting(a1),g)+D_min_hob(i+1,t-1440);
                                            end
                                       end
                                    end
                                end
                                for f=1:s1 % assigns demand for any time left over
                                    t=hob_start(a1)+(s2*5)+f;
                                    if t <= 1440
                                        D_min_hob(i,t) = hob_demand_cycle(hob_setting(a1),f) + D_min_hob(i,t);
                                    end
                                    if t > 1440
                                        if (i+1) <= Nspan
                                            D_min_hob(i+1,t-1440) = hob_demand_cycle(hob_setting(a1),f) + D_min_hob(i+1,t-1440);
                                        end
                                    end
                                end
                        end
                  end
              end
          end
      end
  end
end


if consumer_type == 1
    if oven_marker == 1
        oven_end=0;
        for i=1:Nspan
            oven_event_number = 0;           
            for p=1:48
                oven_chance = D_HHspecific_oven(i,p)/(0.58*1.5/oven_use_value);% adjusted to give correct no. of events per day
                R=rand;
                if R<= oven_chance
                    if oven_event_number < 2 % only allows domestic consumers a max. of 2 oven events/day
                        oven_start = round(rand*29)+1 ;% sets a start time within half-hour between 1 and 30
                        oven_level = round(rand*4)+1; % oven setting between 1 and 5
                        oven_duration1 = 5 +(oven_level*3) ;% sets warm-up cycle between 5 and 20 minutes
                        oven_duration2 = round(randn*10 + 35) ;% sets cooking time of normal dist, mean 35mins & std dev 10min
                        if oven_duration2 <0
                            oven_duration2 = 5;
                        end
                        oven_cycle_num = round(oven_duration2/10) ;% finds whole number of 10 min cycles in cooking time
                        ocn = oven_duration2/10;
                        if ocn < oven_cycle_num
                            oven_cycle_num = oven_cycle_num-1;
                        end
                        oven_start = ((p-1)*30)+oven_start;
                        if oven_start > oven_end
                            oven_end = oven_start + oven_duration1 + oven_duration2;
                            oven_event_number = oven_event_number + 1;
                        end
                        for q=1:oven_duration1
                            if (oven_start+q-1) <=1440
                                D_min_oven(i,(oven_start+q-1))=2.5;
                            end
                            if (oven_start+q-1) > 1440
                                if (i+1) <= Nspan
                                    D_min_oven((i+1),(oven_start+q-1-1440)) = 2.5;
                                end
                            end
                        end
                        t = oven_start + oven_duration1;
                        for q = 1:oven_cycle_num
                            for r= 1:10
                                t = t+1;
                                if t <= 1440
                                    if r <= oven_level
                                        D_min_oven(i,t) = 2.0;
                                    end
                                end
                                if t > 1440
                                    if (i+1) <= Nspan
                                        if r <= oven_level
                                            D_min_oven((i+1),(t-1440)) = 2.0;
                                        end
                                    end
                                end
                            end
                        end
                        r=oven_duration2-q*10;
                        if r > 0
                            for q=1:r
                                t=t+1;
                                if t <= 1440
                                    if q <= oven_level
                                        D_min_oven(i,t) = 2.0;
                                    end
                                end
                                if t >1440
                                    if (i+1) <= Nspan
                                        if q <= oven_level
                                            D_min_oven((i+1),(t-1440)) = 2.0;
                                        end
                                    end
                                end
                            end
                        end   
                    end
                end
            end
        end
    end
end
     
if consumer_type == 2 %non-domestic consumers
    for i=1:Nspan
        D_min_cook(i:Nspan,1:1440)=0;
        for p=1:48
            for q=1:30
                D_min_cook(i,((p-1)*30)+q)=D_HHspecific_oven(i,p); % sets 1-min demand to be same as HH
            end
        end
    end
end               
                            
                                       

if microwave_marker == 1
    micro_end = 0;
    
    for i=1:Nspan
        if i>1 
            micro_end=micro_end-1440;
        end
        for p=1:48
            R=rand;
            if R <= 0.20 %probability based on Mansouri et al's data for defrosting events
                micro_size = 0.22;
            end
            if R > 0.20
                micro_size = 1.300; % for cooking/warm-up
            end
            D_min_micro(i,(1+(p-1)*30):(30+(p-1)*30)) = 0.005;
            R=rand;
            if micro_size == 0.22 % defrosting
                micro_factor = 0.12; %adjusts probability to give typical daily duration of 5.5 min per day (calc from Mansouri)
                if R > 0.73 % 73% of defrost events less than 5 min
                   micro_duration = round(4*rand) + 5;  
                        if R > 0.94 %21% are between 5-10 min
                            micro_duration = round(9*rand)+10;% 5% are between 10-20min
                        end
                        if R > 0.99
                            micro_duration = round(9*rand)+20; %ignores events > 30min 
                        end
                end
                if R <= 0.73
                    micro_duration = round(4*rand);
                end
            end
            if micro_size == 1.300 % cooking or warming-up
                micro_factor = 0.87; %adjusts probability to give typical daily duration of 7 min (calc from Mansouri)
                if R > 0.51 % 51% of cooking events less than 5 min
                   micro_duration = round(4*rand) + 5;  
                        if R > 0.80 %29% are between 5-10 min
                            micro_duration = round(9*rand)+10;
                        end
                        if R > 0.94
                            micro_duration = round(9*rand)+20; %ignores events being more than 30min (only 2%)
                        end
                end
                if R <= 0.51
                    micro_duration = round(4*rand);
                end
            end
            D_HHspecific_micro(i,p) = D_HHspecific_microwave(i,p) - 0.005;
            if D_HHspecific_micro(i,p) < 0
                D_HHspecific_micro(i,p) = 0;
            end
            if micro_duration > 0 
                micro_chance = micro_factor*D_HHspecific_micro(i,p)/(micro_duration*micro_size/30);
                R=rand;
                if R < micro_chance
                    micro_start = round(rand*29)+1;
                    micro_start = ((p-1)*30) + micro_start;
                    if micro_start > micro_end
                        micro_end = micro_start + micro_duration;
                        for q = 1:micro_duration
                            if (micro_start+q-1) <=1440
                                D_min_micro(i,(micro_start+q-1)) = micro_size;
                            end
                            if (micro_start + q -1) >1440
                                if (i+1) <= Nspan
                                    D_min_micro(i+1,((micro_start+q-1)-1440)) = micro_size;
                                end
                            end
                        end
                    end
                end
            end
        end
    end
end

if kettle_marker == 1
    kettle_end = 0;
    kettle_size = 2 + rand ;% randomly sizes kettle demand between 2-3 kW
    for i=1:Nspan
        if i>1
            kettle_end=kettle_end-1440;
        end
        for p=1:48
            kettle_duration = round(rand*2)+2;  % randomly sets event length to 2-5 minutes
            kettle_demand = kettle_size*kettle_duration/30  ; % equivalent half-hourly demand
            kettle_chance = D_HHspecific_kettle(i,p)/kettle_demand;
            R=rand;
            if R < kettle_chance
                kettle_start = round(rand*29);
                kettle_start=((p-1)*30)+kettle_start;
                if kettle_start > kettle_end
                    kettle_end = kettle_start + kettle_duration;
                    for q = 1:kettle_duration
                        if (kettle_start+q-1) <=1440
                            D_min_kettle(i,(kettle_start+q-1)) = kettle_size;
                        end
                        if (kettle_start+q-1) > 1440
                            if (i+1) <= Nspan
                                D_min_kettle(i+1,((kettle_start+q-1)-1440))=kettle_size;
                            end
                        end
                    end
                end
            end
        end
    end
end

z=1:1440;
D_min_cook = D_min_micro + D_min_kettle+D_min_hob+D_min_oven;

if consumer_type == 2 %non-domestic
    for i=1:Nspan
        for p = 1:48
            temp=0;
            for q=1:30
                temp=temp+D_min_cook(i,((p-1)*30+q)) ;%works out the total misc one-min demand for given HH
            end
            temp1 = temp/30; %works out the HH average
            for q = 1:30
                if temp1 > 0
                    D_min_cook(i,((p-1)*30+q)) = D_min_cook(i,((p-1)*30+q))*(D_HHspecific_oven(i,p)/temp1);
                end%scales misc to give same HH value
            end
        end
    end
end
                                
                        
                
                
