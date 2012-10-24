function Coldr = ColdAppliances(S,NoPros)
%This is a function modelling a load-distributing cold appliance for
%CASCADE which "gaps" i.e. drops load in slots with above average cost with
%a probability proportionate to the increase in cost above the average.
%Uses a "price" signal S provided by the aggregator.
F=0.15; %This is the electrical load in kW of the prosumer's fridge/freezer.
Nt = 1; % This is the number of timeslots the prosumer's fridge can be cut off for.
%Check to see if S is null, if so compute baseline
if max(S)<0.01
    L=zeros(1,48);
    BaseL=L+F;
    Coldr=NoPros*BaseL;
    return
end
%If S is valid.
%Begin by eliminating the negative signal slots as we won't want to gap them
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
   S(i)=S(i)+S(i-1);%create intervals proportionate to S
end
AggL=zeros(1,48); %Initialise total load
L=zeros(1,48);
for j=1:NoPros %Main loop creating prosumer loads
    L(1:48)=F+((Nt*F)/48); %Initialise load array including redistributed load before gaps
    n=rand;k=1; %throw dice and initialise index
    while S(k)<n
        k=k+1;
    end
    L(k)=0;
    if Nt == 2 && k<48
        L(k+1)=0;
    end
    AggL=AggL+L;%Add this prosumer to total
end
Coldr=AggL;
%figure
%plot(AggL)
%sum(AggL)/2
