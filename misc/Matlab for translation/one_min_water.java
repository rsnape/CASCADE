% this module calculates the one-minute averaged demands for water heaters from half-hourly demand
%water_size determines the demand of the immersion heater (calculated in ownership)

D_min_water(1:Nspan,1:1440)=0; % initialises minute demands for water heating

if water_marker == 0 % no water heating required
end

if water_marker > 0 % water heating required (=1 for all year use, =2 for summer only)
    for i=1:Nspan
        for j=1:48
            water_event_chance = D_HHspecific_water(i,j)/(water_size*0.5); %assumes events last 15 min
            R=rand;
            if water_event_chance <= R
                for p = 1:30
                    D_min_water(i,((j-1)*30+p))=0;
                end
            end
            if water_event_chance > R
                water_duration = 30*D_HHspecific_water(i,j)/water_size;
                if water_duration < 15
                    water_duration = 15;
                end
                if water_duration >30
                    water_duration = 30;
                end
                R1=rand;
                water_start = round(R1*(30-water_duration));
                water_end = water_start + water_duration;
                if water_end > 30
                    water_end = 30;
                end
                for p=1:30
                    if p > water_end
                        D_min_water(i,((j-1)*30+p)) = 0;
                    end
                    if p < water_end
                        if p < water_start
                            D_min_water(i,((j-1)*30+p)) = 0;
                        end
                        if p >= water_start
                            D_min_water(i,((j-1)*30+p)) = water_size;
                        end
                    end
                end
            end
        end
    end
end
    
if consumer_type == 2 %non-domestic
    for i=1:Nspan
        for p = 1:48
            temp=0;
            for q=1:30
                temp=temp+D_min_water(i,((p-1)*30+q)) ;%works out the total misc one-min demand for given HH
            end
            temp1 = temp/30; %works out the HH average
            for q = 1:30
                if temp1 > 0
                    D_min_water(i,((p-1)*30+q)) = D_min_water(i,((p-1)*30+q))*(D_HHspecific_water(i,p)/temp1); %scales misc to give same HH value
                    
                end
            end
        end
    end
end
                    
