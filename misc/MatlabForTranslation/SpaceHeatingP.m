function SpacerP = SpaceHeatingP(S,NoPros,Pros)
%This is an implementation of space heating by heat pump for
%CASCADE which "gaps" i.e. drops load in the most attractive slots.
%Expects a normalised actual price for S
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
Trm = 20; %Fixed room temperature at this stage
CoP = 2.4; %Baseline heat pump performance 
AggL=zeros(1,48); %Initialise total responsive load
BaseL=zeros(1,48);%Initialise total baseline load
L=zeros(1,48);
for i=1:NoPros %Main loop creating prosumer loads
    %Obtain thermal mass (kWh/degC), loss rate(W/degC) and time constant 
    %tor (in timeslots)for the building from the Pros array 
    Tmass= Pros(i,3);Tloss =Pros(i,4); tor= Pros(i,5);
    
    %Create baseline
    for j=1:48
        L(1,j)=(Trm-ExtTemp(1,j))*(Tloss/1000)/CoP; 
    end
    BaseL=BaseL+L;%Add this prosumer to baseline total
    
    %Work out saving from a gap starting at each timeslot in turn for which
    %the temp drop does not exceed 0.5 degrees
    Sav=zeros(2,48); %Holds both savings and no of timeslots to get it
    for j=1:48
        Nt=1; %No of timeslots to be gapped
        Tdrop=0;
        while Tdrop<0.5 && j+Nt-1<49
            Tdrop = Trm - ((Trm-ExtTemp(1,j))*exp(-Nt/tor)+ExtTemp(1,j));%Work out temp drop from gap
            Sav(1,j)=Sav(1,j)+S(1,j+Nt-1); %Saving
            Sav(2,j)=Nt; %No of timeslots to achieve saving
            Nt=Nt+1;
        end
    end
    
    %Now find the maximum saving and implement it
    Savmax=max(Sav(1,:));
    k=find(Sav(1,:)==Savmax);
    Nt=Sav(2,k);
    L=L+(Nt*(Trm-ExtTemp(1,k))*(Tloss/1000)/((48-Nt)*0.9*CoP));%Add gap recovery load
    for m=1:Nt
        L(1,k+m-1)=0;
    end
    AggL=AggL+L;%Add this prosumer to total
end
if max(S)<0.01 %test for null S
    SpacerP = BaseL;
else SpacerP = AggL;
end
% figure
% plot(BaseL)
% sum(BaseL)/2
% figure
% plot(AggL)
% sum(AggL)/2
