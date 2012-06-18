% this module calculates values to determine ownership of appliances or end-use needs:

%this section determines values for a and b in order to interpolate trends **********************************************
trend_year_12=[1970 1975 1980 1985 1990 1995 1998 2000 2005 2010 2015 2020];
a=1;
for i=1:12
    if y > trend_year_12(i)
        a=a+1;
    end
end
b=(y-trend_year_12(a-1))/(trend_year_12(a)-trend_year_12(a-1));

%these are trend values:
%water heating (immersion ownership, use in summer, use all year round)
water_own=[0.6 0.65 0.67 0.67 0.66 0.64 0.63 0.63 0.61 0.60 0.58 0.57];
water_summer=[0.74 0.68 0.535 0.433 0.331 0.273 0.262 0.258 0.257 0.252 0.246 0.239];
water_allyear=[0.145 0.16 0.151 0.149 0.148 0.148 0.148 0.149 0.149 0.149 0.149 0.149] ;
% fridge ownership
fridge_own = [0.58 0.77 0.72 0.61 0.52 0.45 0.43 0.43 0.43 0.43 0.43 0.43];
fridge_own_value = fridge_own(a-1) + b*(fridge_own(a)-fridge_own(a-1));
ff_own = [0 0.054 0.185 0.354 0.506 0.584 0.619 0.634 0.65 0.645 0.65 0.65];
ff_own_value = ff_own(a-1) + b*(ff_own(a) - ff_own(a-1));
%washing ownership
washer_own = [0.64 0.71 0.77 0.79 0.77 0.76 0.76 0.76 0.77 0.78 0.78 0.79];
dryer_own = [0.007 0.06 0.17 0.28 0.35 0.35 0.35 0.35 0.34 0.34 0.35 0.35];
dish_own = [0.01 0.04 0.05 0.06 0.13 0.19 0.22 0.24 0.27 0.29 0.30 0.31];
wash_dry_own = [0 0 0.0085 0.045 0.12 0.145 0.147 0.149 0.153 0.153 0.152 0.15];

%this section works out the ownership value:
%water heating 
water_own_value=water_own(a-1)+b*(water_own(a)-water_own(a-1));
water_summer_value=water_summer(a-1)+b*(water_summer(a)-water_summer(a-1));
water_allyear_value=water_allyear(a-1)+b*(water_allyear(a)-water_allyear(a-1));
wash_own_value = washer_own(a-1)+b*(washer_own(a)-washer_own(a-1));
dryer_own_value = dryer_own(a-1)+b*(dryer_own(a)-dryer_own(a-1));
dish_own_value = dish_own(a-1)+b*(dish_own(a)-dish_own(a-1));
wash_dry_own_value = wash_dry_own(a-1)+b*(wash_dry_own(a)-wash_dry_own(a-1));

%this section examines built form ***********************************************************************************
user_entry=input('Is the house: detached(d), semi-detached (s), terraced (t), bungalow (b) or other(o)? d,s,t,b,o','s')
built_form=user_entry;
user_entry = input('When was the house built? pre 1919(1), 1919-1944(2), 1945-1964(3), 1965-1979(4), post 1980(5)')
age=user_entry;
if built_form == 'o' % sets unknown homes to terraces
    built_form = 't'
end
if built_form == 'd' % detached homes - sets different floor areas based on age and value of 'form' (for water heating)
    if age == 1
        form=1;
        floor_area = 155;
    end
    if age == 2
        form = 2;
        floor_area = 127;
    end
    if age == 3
        form = 2;
        floor_area = 126;
    end
    if age == 4
        form = 2;
        floor_area = 126;
    end
    if age == 5
        form = 2;
        floor_area = 131;
    end
end
if built_form == 's' % semis - floor area and form
    if age == 1
        form = 3;
        floor_area = 153;
    end
    if age == 2
        form =4;
        floor_area = 105;
    end
    if age == 3
        form = 5;
        floor_area = 125;
    end
    if age == 4
        form = 6;
        floor_area = 125;
    end
    if age == 5
        form = 6;
        floor_area = 86;
    end
end
if built_form == 't' % floor areas and form for terraces
    if age == 1
        form = 3;
        floor_area = 123;
    end
    if age == 2
        form =4;
        floor_area = 95;
    end
    if age == 3
        form = 5;
        floor_area = 120;
    end
    if age == 4
        form = 6;
        floor_area = 120;
    end
    if age == 5
        form = 6;
        floor_area = 72;
    end
end
if built_form == 'b'
        form = 7;
        user_entry = input ('Is the bungalow detached (d) or a semi (s)?','s')
        if user_entry == 'd'
            if age < 5
                floor_area = 123;
            end
            if age < 3
                floor_area = 125;
            end
            if age == 5
                floor_area = 90;
            end
        end
        if user_entry == 's'
            if age < 5
                floor_area = 109;
            end
            if age == 5
            floor_area = 72;
            end
        end
            
end
form_factor=[1.0 1.03 0.83 0.89 1.03 1.06 1.15];
form_value=form_factor(form);
    

%this section inputs the floor area and finds the number of occupants *********************************************************
user_entry=input('Do you know the floor area of the house?y or n','s')
if user_entry == 'y'
user_entry=input('What is the total floor area of the house, in squ metres?')
floor_area=user_entry;
end
    
user_entry=input('Do you know how many occupants live there, y or n?','s')
if user_entry =='y'
    user_entry=input('Number of occupants =')
    num_occ=user_entry;
end
if user_entry =='n'
    if floor_area <= 450
    num_occ=0.0365*floor_area-0.00004145*(floor_area)*(floor_area);
    end
    if floor_area>450
    num_occ=9/(1+(54.3/floor_area));
    end
    num_occ=round(num_occ);
end
income_num=0 ;% initialises income marker
user_entry=input('Do you know their ACORN lifestyle classification type? y/n','s')
if user_entry == 'n'
    r=rand;
    if r <= 0.06
        social_num = 1
    end
    if r > 0.06
        if r > 0.94
            social_num = 6
        end
        if r > 0.81
            social_num = 5
        end
        if r > 0.63 
            social_num = 4
        end
        if r > 0.37
            social_num = 3
        end
        if r <= 0.37
            social_num = 2
        end
    end
    user_entry1 = input ('Do you know if annual income is above (1) or below(2) national average (£20K)?1,2 or 3(dont know)')
    if user_entry1 == 1
        income_num=2 ;% above average income for lighting
    end
    if user_entry1 == 2
        income_num=1 ;% below average income for lighting
    end
    if user_entry1 == 3
        income_num = 0 ;% uses social group for lighting
    end
end
if user_entry == 'y'
    user_entry1 = input ('A (=1), B(=2), C(=3), D(=4), E(=5) or F(=6)? Enter 1-6')
    social_num = user_entry1;
end
    
% this section finds out about appliances/end-uses
% if ownership is unknown, national statistics are used to assign ownership randomly

% section for space heating ****************************************************************************************************
heat_marker = 0 % non-E7 consumers
if tar == 2 %E7 consumers
user_entry=input('Space heating: are electric storage heaters used?y,n,d','s')
    if user_entry == 'y'
        water_marker=3;
        heat_marker=1;
    elseif user_entry == 'n'
        heat_marker=0;
    else R=rand
        if R<=0.1
        heat_marker=1 ;% assigns space heating to 10% of homes randomly
        else heat_marker=0
        end
    end
end


% section for water heating ***************************************************************************************************
water_marker=4;
user_entry=input('Water heating: is the water heated with electricity?y,n,d','s')
if user_entry == 'y'
    user_entry=input('Is water heated in the summer only(s) or all year round (a)? s,a','s')
    if user_entry == 's'
        water_marker=2 ;%identifies known summer-only users
    end
    if user_entry == 'a'
        water_marker=1 ;%identifies known all-year round users
    end
end
if user_entry == 'n'
    water_marker=0 ;% identifies non-users
end
if user_entry == 'd'
    temp=rand;
    if temp > (water_own_value*form_value)
        water_marker=0 ;% uses probability to determine non-owner
    end
    if water_marker== 4 % this defines homes owning an immersion heater
        temp1=rand;
        if temp1 <= (water_summer_value)
            water_marker=2 ;% homes owning but for summer use only
        end
    end
    if water_marker == 4 % picks up homes with immersion heater but not summer only users
        temp2 =rand;
        if temp2 <= (water_allyear_value)
            water_marker=1 ;%homes owning immersion heaters for all year use
        end
    end 
    if water_marker == 4 % homes owning immersion heaters but not using them
        water_marker=0;
    end
    if water_marker == 3 % home using electricity for space heating - assume all year immersion water heating
        water_marker = 1;
    end
end
R=rand ;% section assigns an immersion coil of 1,2 or 3 kW
if R <=0.67
    if R <=0.33
        water_size=1;
    end
    if R > 0.33
        water_size = 2;
    end
end
if R >0.67
    water_size = 3;
end

%section for cooking: *********************************************************************************************************
%hobs
trend_year_6=[1970 1980 1990 2000 2010 2020];
a=1;
for i=1:6
    if y > trend_year_6(i)
        a=a+1;
    end
end
b=(y-trend_year_6(a-1))/(trend_year_6(a)-trend_year_6(a-1));

hob_own = [0.42 0.43 0.47 0.48 0.46 0.42];
hob_own_value=hob_own(a-1)+b*(hob_own(a)-hob_own(a-1));

oven_own = [0.42 0.44 0.53 0.68 0.64 0.69];
oven_own_value = oven_own(a-1)+b*(oven_own(a)-oven_own(a-1));

user_entry = input('Does the home have an electric hob? y,n,d','s') % finds out if specific information is known re. hobs
if user_entry == 'y'
    hob_marker = 1;
end
if user_entry == 'n'
    hob_marker = 0;
end
if user_entry == 'd'
    R=rand;
    if R > hob_own_value
        hob_marker = 0;
    end
    if R <= hob_own_value
        hob_marker = 1;
    end 
    if heat_marker == 1 % assumes need for ovens & hobs if electricity used for space heating
    hob_marker = 1;
    oven_marker = 1;
    end
end

%ovens
user_entry = input('Does the home have an electric oven? y,n,d','s') % gathers specfic info on ovens
if user_entry == 'y'
    oven_marker = 1;
end
if user_entry == 'n'
    oven_marker = 0;
end
if user_entry == 'd'
    R = rand;
    if R > oven_own_value
        oven_marker = 0;
    end
    if R <= oven_own_value
        oven_marker = 1;
    end
    if hob_marker == 1 % assumes ownership of an electric hob implies ownership of an oven too
    oven_marker = 1;
    end
end


%microwaves, sets a random assignment based on 85% ownership 
micro_socio_weight = [0.78 0.78 0.73 0.92 0.67 0.69]; % variation by ACORN category reported by Mansouri et al (gives average of 77%)
user_entry=input('Does the home have a microwave? y,n,d,','s')
if user_entry == 'y'
    microwave_marker = 1;
end
if user_entry == 'n'
    microwave_marker = 0;
end
if user_entry == 'd'
R=rand;
microwave_marker = 0;
temp = 0.85 * micro_socio_weight(social_num)/0.77;
    if R < temp
        microwave_marker = 1;
    end
end

%kettles, sets a random assignment based on 99% ownership 
user_entry=input('Does the home have a kettle? y,n,d','s')
if user_entry == 'y'
    kettle_marker = 1;
end
if user_entry == 'n'
    kettle_marker = 0;
end
if user_entry == 'd'
R=rand;
kettle_marker = 0;
    if R < 0.99
        kettle_marker = 1;
    end
end

% refrigeration *****************************************************************************************************************
fridge_marker = 0 ;% initialises parameters
freezer_marker = 0;
ff_marker = 0;
freezer_num = 0 ;% assumes 0 or 1 fridge but possibly 0-2 freezers or 0-2 fridge-freezers
ff_num = 0;
cool_num =0 ;
cold_own_value = fridge_own_value + ff_own_value;

user_entry10 = input ('Do you know what cold appliances are owned? y or n','s')
if user_entry10 == 'y'
    user_entry = input('Does the home have a single fridge unit(i.e. not fridge/freezer)? y or n','s')
    if user_entry == 'y'
        fridge_marker = 1;
    end
    if user_entry == 'n'
        fridge_marker = 0;
    end
    user_entry = input ('Does the home have a combined fridge-freezer unit? y or n','s')
    if user_entry == 'y'
        ff_marker = 1;
        user_entry1 = input ('How many fridge-freezers?')
        ff_num = user_entry1;
    end
    if user_entry == 'n'
        ff_marker = 0;
    end
    user_entry = input ('Does the home have a freezer unit? y or n','s')
    if user_entry == 'y'
        freezer_marker = 1;
        user_entry1 = input ('How many freezers?')
        freezer_num = user_entry1;
    end
    if user_entry == 'n'
        freezer_marker =0;
    end
end
if user_entry10 == 'n' % don't know what cold appliances are owned
    R=rand;
    if R <= 0.32
        ff_marker = 1;
        ff_num = 1;
    end
    if R > 0.32
        if R <= 0.613
            fridge_marker = 1;
            freezer_marker = 1;
            freezer_num = 1;
        end
        if R > 0.613
            if R <= 0.695
                fridge_marker = 1;
            end
            if R > 0.695
                if R <= 0.765
                    fridge_marker = 1;
                    ff_marker = 1;
                    ff_num = 1;
                end
                if R > 0.765
                    if R <= 0.849
                        ff_marker = 1;
                        ff_num = 1;
                        freezer_marker = 1;
                        freezer_num = 1;
                    end
                    if R > 0.849
                        if R <= 0.913
                            fridge_marker = 1;
                            ff_marker = 1;
                            ff_num = 1;
                            freezer_marker = 1;
                            freezer_num = 1;
                        end
                        if R > 0.913
                            if R <= 0.94
                                fridge_marker = 1;
                                freezer_marker = 1;
                                freezer_num = 2;
                            end
                            if R > 0.94
                                if R <= 0.97
                                    fridge_marker = 1;
                                    ff_marker = 1;
                                    freezer_marker = 1;
                                    ff_num = 1;
                                    freezer_num = 2;
                                end
                                if R > 0.97
                                    if R <= 0.99
                                        ff_marker = 1;
                                        freezer_marker = 1;
                                        ff_num = 2;
                                        freezer_num = 2;
                                    end
                                    if R > 0.99
                                        fridge_marker = 1;
                                        ff_marker = 1;
                                        freezer_marker = 1;
                                        ff_num = 2;
                                        freezer_num = 2;
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

% washing appliances ********************************************************************************************************
wash_marker = 0;
dryer_marker = 0;
dish_marker = 0;
wash_dry_marker = 0;
wash_own_value1 = [0.96 0.96 0.91 0.99 0.93 0.93]; % Mansouri's dist. of ownerhsip -v- social grouping (average 95% for sample)
wash_own_value = wash_own_value *wash_own_value1(social_num)/0.95; %adjust ownership in relation to social grouping
wash_dry_own_value = wash_dry_own_value * wash_own_value1(social_num)/0.95; % assumes same for washer-dryers

dryer_own_value1 = [0.54 0.54 0.49 0.71 0.63 0.45]; % same for tumble dryers, with Mansouri's average of 55% for sample group
dryer_own_value = dryer_own_value * dryer_own_value1(social_num)/0.55;

R=rand;
if R > (wash_own_value + wash_dry_own_value) % assigns no washing appliance
    wash_marker = 0;
    wash_dry_marker = 0;
end
if R <= (wash_own_value + wash_dry_own_value)
    if R > (wash_own_value) % assigns washers first
        wash_marker = 1;
    end
    if R <= (wash_own_value)%rest have washer_dryers
        wash_dry_marker = 1;
    end
end
%tumble dryers (for combined washer-dryer owners)
if wash_dry_marker == 1
    R=rand;
    if R > 0.15
        dryer_marker = 0;
    end
    if R <= 0.15
        dryer_marker = 1 ;% assumes 15% of washer-dryer owners have tumble dryers
    end
end
% tumble dryers (for washing machines owners)
if wash_dry_marker == 0
    if wash_marker == 1
        R=rand;
        if R > (dryer_own_value - 0.15*wash_dry_own_value)
            dryer_marker = 0;
        end
        if R <= (dryer_own_value - 0.15*wash_dry_own_value)
            dryer_marker = 1;
        end
    end
end
% dishwashers
social_dish_own = [0.57 0.57 0.35 0.48 0.37 0.36] * (dish_own_value/0.43); % adjusts social ownership by national average compared to Mansouri
R=rand;
if R > social_dish_own(social_num)
   dish_marker = 0;
end
if R <= social_dish_own(social_num)
   dish_marker = 1;
end
   
user_entry = input('Does house have a washing machine (not including a washer/dryer combined unit)?y,n,d','s')
if user_entry == 'y'
    wash_marker = 1;
    wash_dry_marker = 0;
end
if user_entry == 'n'
    wash_marker = 0;
end
user_entry = input ('Does house have a combined washer/dryer? y,n,d','s')
if user_entry == 'y'
    wash_dry_marker = 1;
    wash_marker = 0;
end
if user_entry == 'n'
    wash_dry_marker = 0;
end
user_entry = input ('Does house have a tumble-dryer? y,n,d,','s')
if user_entry == 'y'
    dryer_marker = 1;
end
if user_entry == 'n'
    dryer_marker = 0;
end
user_entry = input ('Does house have a dishwasher? y,n,d','s')
if user_entry == 'y'
    dish_marker = 1;
end
if user_entry == 'n'
    dish_marker = 0;
end
