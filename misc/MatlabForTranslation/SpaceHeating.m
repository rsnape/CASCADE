function Spacer = SpaceHeating(S,NoPros,Pros)
%This is an initial exploration of space heating by heat pump for
%CASCADE which "gaps" i.e. drops load in slots with above average cost with
%a probability proportionate to the increase in cost above the average.
%
%Use a balanced baseline "price" signal S taken from calling program
load Single_day_ext_temp.dat;%Read in an external temperature file
ExtTemp=zeros(1,48);
n=1;
for j=1:24 %Interpolate 24 hour data to 48 values
    ExtTemp(n)=Single_day_ext_temp(1,j);
    n=n+1;
    if j<24
    ExtTemp(n)=Single_day_ext_temp(1,j)+(Single_day_ext_temp(1,j+1)-Single_day_ext_temp(1,j))/2;
    n=n+1;
    else
    ExtTemp(n)=Single_day_ext_temp(1,j)-0.25;   
    end
end
%
Tex=sum(ExtTemp)/48;
Trm = 20; %Fixed room temperature at this stage
Nt = 2; %Fixed no of timeslots at this stage
CoP = 2.4; %Baseline heat pump performance 
%Begin by eliminating the negative signal slots as we won't want to gap them
%but only if S is not null.
if max(S)>0.01
    for i=1:48
        if S(i)<0
            S(i)=0;
        end
    end
    Ssum = sum(S); %Normalise S
    for i=1:48
        S(i)=S(i)/Ssum;
    end
    for i = 2:48
       S(i)=S(i)+S(i-1);%create intervals proportionate to original S
    end
end
AggL=zeros(1,48); %Initialise total responsive load
BaseL=zeros(1,48);%Initialise total baseline load
L=zeros(1,48);
for i=1:NoPros %Main loop creating prosumer loads
    %Obtain thermal mass (kWh/degC), loss rate(W/degC) and time constant 
    %tor (in timeslots)for the building from the Pros array 
    Tmass= Pros(i,3);Tloss =Pros(i,4); tor= Pros(i,5);
    for j=1:48
    L(1,j) =(Trm-ExtTemp(1,j))*(Tloss/1000)/CoP; %Initialise load array with baseline
    end
    BaseL=BaseL+L;
    
    %Now distribute gapping
    Tdrop = Trm - ((Trm-Tex)*exp(-Nt/tor)+Tex);%Work out temp drop from gap
    if Tdrop<0.5 && max(S)>0.01 %If S not null do response
        L=L+(Nt*(Trm-Tex)*(Tloss/1000)/((48-Nt)*0.9*CoP));%Add gap recovery load
        n=rand;k=1; %throw dice and initialise index
        while S(k)<n %search entire 48 steps
            k=k+1;
        end
        L(k)=0;
        if Nt == 2 && k<48
            L(k+1)=0;
        end
    end
    AggL=AggL+L;%Add this prosumer to total
end
if max(S)<0.01 %test for null S
    Spacer = BaseL;
else Spacer = AggL;
end
% figure
% plot(BaseL)
% sum(BaseL)/2
% figure
% plot(AggL)
% sum(AggL)/2