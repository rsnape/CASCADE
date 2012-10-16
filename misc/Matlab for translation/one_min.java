% this module converts half-hourly demand to one minute averaged demand
PF_min(1:Nspan,1:1440)= 1;

user_entry = input('how many houses in sample?')
house_num=user_entry;

D_min_all(1:house_num,1:1440)=0;


if house_num == 1   
    one_min_lights
    one_min_heat
    one_min_water
    one_min_cooling
    one_min_wash
    one_min_cook
    one_min_misc2

    D_min_total = D_min_heat + D_min_lights + D_min_water+D_min_cooling+D_min_washing +D_min_cook+D_min_misc;%active load
    D_min_total_reactive = D_min_cooling*1.02 + D_min_washing_reactive;
    Z=1:1440;
    plot(Z,D_min_total)
    for i=1:Nspan
        for j=1:1440
            if D_min_total(i,j) > 0
                PF_min(i,j)= cos(atan(D_min_total_reactive(i,j)/D_min_total(i,j)));
            end
        end
    end

end

if house_num >1
    for i=1:Nspan
        for i_house = 1:house_num
            one_min_lights
            one_min_heat
            one_min_water
            one_min_cooling
            one_min_wash
            one_min_cook
            one_min_misc2
        
            D_min_total = D_min_heat + D_min_lights + D_min_water+D_min_cooling+D_min_washing +D_min_cook+D_min_misc;
            D_min_total_reactive = D_min_cooling*1.02 + D_min_washing_reactive;
            for j=1:1440
                 if D_min_total(1,j) > 0
                    PF_min(i_house,j)= cos(atan(D_min_total_reactive(1,j)/D_min_total(1,j)));
                  end
            end
            
            D_min_all(i_house,1:1440) = D_min_total(1,1:1440);
            D_min_all_mean = mean(D_min_all);
            Z=1:1440;
            plot(Z,D_min_all_mean)
            
        end
    end
end








