function ColdrP = ColdAppliancesP(S,NoPros)
%This is a function modelling a load-distributing cold appliance for
%CASCADE which "gaps" i.e. drops load in the most attractive timeslot.
%Uses a "price" signal S provided by the aggregator.
F=0.15; %This is the electrical load in kW of the prosumer's fridge/freezer.
Nt = 1; % This is the number of timeslots the prosumer's fridge can be cut off for.
%Check to see if S is null, if so compute baseline
if max(S)<0.01
    L=zeros(1,48);
    BaseL=L+F;
    ColdrP=NoPros*BaseL;
    return
end
%If S is valid, look for most expensive slot
AggL=zeros(1,48); %Initialise total load
L=zeros(1,48);
for i=1:NoPros %Main loop creating prosumer loads
    L(1,1:48)=F+((Nt*F)/48); %Initialise load array including redistributed load before gaps
    Smax=max(S);
    k=find(S==Smax);
    L(1,k)=0;
    AggL=AggL+L;%Add this prosumer to total
end
ColdrP=AggL;
% figure
% plot(AggL)
% sum(AggL)/2
% figure
% plot(S)
