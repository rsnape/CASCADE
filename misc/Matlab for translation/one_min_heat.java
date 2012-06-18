% heating
D_min_heat(1:Nspan,1:1440) = 0;

if heat_marker >0
    for i=1:Nspan
        heat_peak = max(D_HHspecific_heat(i,1:17));
        for p=1:17
            if D_HHspecific_heat(i,p) == heat_peak
                a = p;
            end
        end
       for p = 1410:1440 % sets the HH after white meter comes on to the assigned demand level for 30 mins
           D_min_heat(i,p)=D_HHspecific_heat(i,48);
       end
       
       if (i-1) == 0
           for p = 1:30
               D_min_heat(i,p)=D_HHspecific_heat(i,1);
           end
       end
       if (i-1) >0
           if D_HHspecific_heat(i,1) >= D_HHspecific_heat(i-1,48)
               for p=1:30
                   D_min_heat(i,p)=D_HHspecific_heat(i,1);
               end
           end
           if D_HHspecific_heat(i,1) < D_HHspecific_heat(i-1,48)              
                   heat_duration = D_HHspecific_heat(i,1)/heat_peak;
                   R=rand;
                   heat_start = round(R*(30-heat_duration));
                   heat_end = heat_start + heat_duration;
                   if heat_end >= 30
                       heat_end = 29;
                   end
                   for p=1:30
                       if p >= (heat_start+1)
                           if p <= (heat_end+1)
                                D_min_heat(i,p) = heat_peak;
                            end
                           if p > (heat_end+1)
                               D_min_heat(i,p) = 0;
                           end
                       end
                       if p < (heat_start+1)
                           D_min_heat(i,p) = 0;
                       end
                   end
            end
        end
        for p=2:a
            for q=1:30
            D_min_heat(i,((p-1)*30+(q-1)))=D_HHspecific_heat(i,p);
            end 
        end
        for p=a+1:16
               heat_duration = round(30*(D_HHspecific_heat(i,p)/heat_peak));
               R=rand;
               heat_start = round (R*(30-heat_duration));
               heat_end = heat_start + heat_duration;
               if heat_end >= 30
                   heat_end = 29;
               end
               for q = 1:30
                   if q >= (heat_start+1)
                        if q <= (heat_end+1)
                            D_min_heat(i,((p-1)*30+(q-1))) = heat_peak;
                        end
                        if q > (heat_end+1)
                            D_min_heat(i,((p-1)*30+(q-1))) = 0;
                        end
                    end
                    if q < (heat_start + 1)
                        D_min_heat(i,((p-1)*30+(q-1))) = 0;
                    end
                end
        end
        for p=17:47
            for q=1:30
            D_min_heat(i,((p-1)*30+(q-1)))=D_HHspecific_heat(i,p);
            end
        end
    end
end
                   
if consumer_type == 2 %non-domestic
    for i=1:Nspan
        for p = 1:48
            temp=0;
            for q=1:30
                temp=temp+D_min_heat(i,((p-1)*30+q)) ;%works out the total one-min demand for given HH
            end
            temp1 = temp/30; %works out the HH average
            for q = 1:30
                if temp1 > 0
                    D_min_heat(i,((p-1)*30+q)) = D_min_heat(i,((p-1)*30+q))*(D_HHspecific_heat(i,p)/temp1); %scales to give same HH value
                end
            end
        end
    end
end
       
       
       
       
       
        