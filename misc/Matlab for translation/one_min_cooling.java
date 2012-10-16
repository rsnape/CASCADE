% this submodule calculates one-minute averaged demand from half-hourly data for refrigeration
D_min_cooling(1:Nspan,1:1440)=0;
%fridges

if consumer_type == 1
    for i=1:Nspan
        D_min_fridge(i:Nspan,1:1440) = 0;
        if fridge_marker == 1
            if num_occ <= 5
                fridge_size = 0.060*cool_scale(num_occ); % assumes average oocupancy of 2.4 people has fridge size 0.060 (mean)
            end
            if num_occ >5
                fridge_size = 0.060*cool_scale(5);
            end
        
            for p = 1:48
                fridge_duration = round((D_HHspecific_fridge(i,p)/fridge_size)*30);% assumes one event every half-hour
                if fridge_duration > 30
                    fridge_duration = 30;
                end
                R=rand;
                fridge_start = round(R*(30-fridge_duration));% uses a random start time for the event
                if fridge_start+fridge_duration > 30
                    fridge_start = fridge_start - 1;
                end
                fridge_end = fridge_start + fridge_duration;
                for q = 1:30
                    if q >= fridge_start
                        if q <= fridge_end
                            D_min_fridge(i,((p-1)*30)+q) = fridge_size ;% sets demand to fridge peak when event occurs
                        end
                    end
                end
            end
        end
    end
end
if consumer_type == 2
    for i=1:Nspan
        D_min_fridge(i:Nspan,1:1440)=0;
        for p=1:48
            for q=1:30
                D_min_fridge(i,((p-1)*30)+q)=D_HHspecific_fridge(i,p); % sets 1-min demand to be same as HH
            end
        end
    end
end


%fridge-freezers (generally as for fridges)
for i=1:Nspan
    D_min_ff(i:Nspan,1:1440) = 0;
    if ff_marker == 1
        if num_occ <= 5
            ff_size = 0.120*cool_scale(num_occ); % assumes average oocupancy of 2.4 people has fridge size 0.120 (mean)
        end
        if num_occ >5
            ff_size = 0.120*cool_scale(5);
        end
        for n = 1:ff_num % can be more than one fridge-freezer
            for p=1:48
                ff_duration=round((D_HHspecific_ff(i,p)/(ff_num*ff_size))*30);
                if ff_duration > 30
                    ff_duration = 30;
                end
                R=rand;
                ff_start = round (R*30-ff_duration);
                if ff_start+ff_duration > 30
                    ff_start = ff_start -1;
                end
                ff_end = ff_start + ff_duration;
                for q=1:30
                    if q >= ff_start
                        if q <= ff_end
                            D_min_ff(i,((p-1)*30)+q) = ff_size+D_min_ff(i,((p-1)*30)+q);
                        end
                    end
                end                    
            end
        end
    end
end

%freezers
for i=1:Nspan
    D_min_freezer(i:Nspan,1:1440) = 0;
    if freezer_marker == 1
        for n = 1:freezer_num % can be more than one freezer
            freezer_size(n) = randn*0.02 + 0.093; % randomly assigns freezer peak value, normal dist. (mean 0.093, std dev 0.02)
            for p=1:48
                freezer_duration=round((D_HHspecific_freezer(i,p)/(freezer_num*freezer_size(n)))*30);
                if freezer_duration > 30
                    freezer_duration = 30;
                end
                R=rand;
                freezer_start = round (R*30-freezer_duration);
                if freezer_start+freezer_duration > 30
                    freezer_start = freezer_start -1;
                end
                freezer_end = freezer_start + freezer_duration;
                for q=1:30
                    if q >= freezer_start
                        if q <= freezer_end
                            D_min_freezer(i,((p-1)*30)+q) = freezer_size(n)+D_min_freezer(i,((p-1)*30)+q);
                        end
                    end
                end                    
            end
        end
    end
end

D_min_cooling = D_min_fridge+D_min_ff+D_min_freezer ;% total cooling demand

if consumer_type == 2 %non-domestic
    for i=1:Nspan
        for p = 1:48
            temp=0;
            for q=1:30
                temp=temp+D_min_cooling(i,((p-1)*30+q)) ;%works out the total misc one-min demand for given HH
            end
            temp1 = temp/30; %works out the HH average
            for q = 1:30
                D_min_cooling(i,((p-1)*30+q)) = D_min_cooling(i,((p-1)*30+q))*(D_HHspecific_fridge(i,p)/temp1); %scales misc to give same HH value
            end
        end
    end
end