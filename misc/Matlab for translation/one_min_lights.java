% this sub-module calculates the one-minute demands from the half-hourly averaged demands
% lighting
D_min_lights(1:Nspan,1:1440)=0;
for i=1:Nspan
    D_min_lights(1:Nspan, 1:1440)=0;
    for j=1:48
        bulb_num = D_HHspecific_lights(i,j)/0.073 ;
        bulb_num1 = round(bulb_num);
        a = (bulb_num1-bulb_num);
        if a > 0
            bulb_num1 = bulb_num1 - 1;
        end
        b = bulb_num - bulb_num1;
        R=rand;
        if R <= b
            bulb_num1 = bulb_num1 + 1;
        end
        for p = 1:bulb_num1
            bulb_start = round(rand * 30);
            if bulb_start == 31
                bulb_start = 30;
            end
            R = round(rand*9)+1;
            if R >9
                R=9;
            end
            bulb_1 = [ 1 2 3 5 9 17 28 50 92];
            bulb_2 = [0 0 1 3 7 10 21 41 167];
            bulb_11 = bulb_1(R);
            bulb_22 = bulb_2(R);
            bulb_duration= round(rand*bulb_22)+bulb_11;
            bulb_start1 = (j-1)*30+bulb_start +1;
            bulb_end = (j-1)*30+bulb_start+bulb_duration +1;
            for q = bulb_start1:bulb_end
                if q > 1440
                    if i+1 <= Nspan
                    D_min_lights (i+1,q-1440)=0.073 +D_min_lights(i+1,q-1440);
                    end
                end
                if q <= 1440
                D_min_lights(i,q) = 0.073+D_min_lights(i,q);
                end
            end
        end
        lights_sum = 0;
        for r = ((j-1)*30+1):(j*30)
            lights_sum = lights_sum + D_min_lights(i,r);
        end
        lights_sum=lights_sum/30;
        if lights_sum>0
            for s = ((j-1)*30+1):(j*30)
                D_min_lights(i,s) = D_min_lights(i,s)*D_HHspecific_lights(i,j)/lights_sum;
            end
        end
    end
end


            
        