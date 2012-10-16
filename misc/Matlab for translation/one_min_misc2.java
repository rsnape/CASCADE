% this module creates a random mix of miscellaneous appliances for the 1-min demand
% using the allocated half-hourly misc. demand

%the typical daily kWh for a range of misc. appliances are used to determine the relative probability that
%the miscellaneous demand arises from their use. 
% Appliances are: TV, PC, Colour monitor, Hi Fi, Iron, Toaster, Digital box, Shaver, Alarm/security, Vac cleaner, Fan heater...
%  Chargers, Coffee maker, deep fat fryer, Hob extractor, VCR, Stereo (portable), CD/DVD, Hair dryer, Food processor, Printer


prob_app = [0.374 0.510 0.588 0.644 0.701 0.743 0.778 0.811 0.842 0.872 0.898 0.922 0.944 0.961 0.974 0.982 0.988 0.994 0.997 0.998 1.00]; % probability (based on average daily kWh)
app_demand = [0.090 0.115 0.100 0.250 1.500 0.167 0.045 0.167 0.005 0.400 0.700 0.400 0.600 0.900 0.144 0.070 0.028 0.050 0.250 0.067 0.007]; %typical demand per half-hour
app_time_min = [15 15 15 15 15 5 15 5  1440 5 15 240 15 10 15 15 15 15 5 5 5]; %minimum time for appliances
app_time_max = [240 240 240 30 120 5 240 15 1440 30 240 480 240 20 120 120 30 120 20 15 15];
D_min_misc(1:Nspan, 1:1440) = 0;
D_misc_temp(1:48) = 0;
app_start=1;
app_needs = 0;
D1=0;

if consumer_type == 1
    chance_factor = 20;
end
if consumer_type == 2
    chance_factor = 100;
end
for i=1:Nspan
    D_misc_temp = D_HHspecific_misc(i,1:48);
    for p=1:48
        D1=0; %D1 collects up the demand already assigned in a given HH
        D2=0; %D2 collects the demand that will be assigned for a new event
        for q=1:30
            D1 = D1+D_min_misc(i,((p-1)*30)+q);
        end
        D1=D1/30;
        D_misc_temp(p) = D_misc_temp(p) - D1 ;%works out what demand is left in a given HH
        for k=1:chance_factor % allows 20 chances (domestic) to find an appliance that has a typical half hourly demand less than that available in a given half hour
            R=rand;
            for j=1:21
                if R > prob_app(j) %selects an appliance based using the typical hourly energy demand as a probability, e.g. TV most likely appliance
                    a_num=j+1; % sets the appliance number (1=TV, 2=PC,etc)
                end
                if R < prob_app(1)
                    a_num=1;
                end
            end
            app_start = round(rand*30)+1 ;%random start to appliance event
            if app_start == 31
                app_start = 30 ;% makes sure start is during current half-hour
            end
            app_duration = app_time_min(a_num) + round(rand*(app_time_max(a_num)-app_time_min(a_num))) ;% random duration time
            for q=1:30
                if q > app_start
                    if q < (app_start + app_duration) % if event is between start and finish times
                        D2 = app_demand(a_num)+ D2 ;% works out total demand for current HH
                    end
                end
            end
            D2 = D2/30 ;% works out the kW average for the current HH
            if D_misc_temp(p) >= D2 % triggers event if there's enough demand left in HH
                D_misc_temp(p) = D_misc_temp(p) - D2 ; %deducts half-hourly demand for the event in current HH          
                if D_misc_temp(p) >= app_demand(a_num) % i.e. if any demand is left over in a given HH
                app_start = app_start + ((p-1)*30) ;% sets up an absolute start time
                app_end = app_start + app_duration ;% sets up absolute end time
                    for t=app_start:app_end
                        if t <= 1440
                            D_min_misc(i,t)=D_min_misc(i,t)+app_demand(a_num);
                        end
                        if t > 1440
                            if (i+1) <= Nspan
                                D_min_misc(i+1,t-1440)=D_min_misc(i+1,t-1440)+app_demand(a_num);
                            end
                        end
                    end
                end
            end
            if D_misc_temp(p) < 0.005
                for q =1:30
                    D_min_misc(i,((p-1)*30)+q)= 0.005 + D_min_misc(i,((p-1)*30)+q) ;
                end
                D_misc_temp(p)=0;
            end
        end
    end
end

if consumer_type == 2 %non-domestic
    for i=1:Nspan
        for p = 1:48
            temp=0;
            for q=1:30
                temp=temp+D_min_misc(i,((p-1)*30+q)) ;%works out the total misc one-min demand for given HH
            end
            temp1 = temp/30; %works out the HH average
            for q = 1:30
                if temp1 > 0
                    D_min_misc(i,((p-1)*30+q)) = D_min_misc(i,((p-1)*30+q))*(D_HHspecific_misc(i,p)/temp1); %scales misc to give same HH value
                end
                if temp1 == 0
                    D_min_misc(i,((p-1)*30+q)) = 0;
                end
            end
        end
    end
end
        
                
                    
                
                
                
                
                
                  
           
            

