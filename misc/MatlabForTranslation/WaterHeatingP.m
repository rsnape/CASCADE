function HwatrP = WaterHeatingP(S,NoPros,Pros)
%This is a domestic hot water model for CASCADE. Begins with a probability 
%profile generation similar to the wet appliance model, but in this case the
%probability profile is from EEF. Finds cheapest timeslot before need.
Pwa=[0.004301293,0.000521697,0.000338338,0.000299165,8.78051E-05,1.67855E-05,3.24446E-05,5.08281E-05,3.26634E-05,2.09805E-05,0.00015978,0.005901148,0.017718346,0.050724817,0.029537896,0.040438993,0.045556441,0.019837576,0.024321321,0.011326311,0.004404451,0.016316149,0.011234929,0.011003286,0.002357359,0.013866024,0.02130702,0.020923151,0.016316284,0.010772871,0.012157543,0.011163138,0.010806869,0.041156671,0.040614103,0.044057789,0.048337571,0.053560723,0.042255463,0.040053663,0.03331174,0.02316115,0.008231065,0.00782774,0.017627877,0.108753399,0.0717727,0.005424644
];
%Requires a real normalised price as the signal
S=S+1;
%plot(S)
Tcold = 18; Thot = 50; CoP=2;%temp of cold feed and heated water also hp CoP
%Main loop of prosumers - initialise occupancy and aggregate load arrays. 
Occ=0;Occtot=0;AggL=zeros(1,48); BaseL=zeros(1,48);L=zeros(1,48);
for i=1:NoPros
    %Take occupancy and hot water volume from the Pros array
    Occ=Pros(i,1); Vol=Pros(i,2);
    %Energy required to heat up the water
    Ew = Vol*(Thot-Tcold)*4.181/3600; %result in kWh
    %Add tank losses and apply CoP to give electrical energy needed in kWh
    Ee = (Ew + 3.5)/CoP; Wa=Ee*2;%convert to power in single timeslot
    L=zeros(1,48);Wd=zeros(1,48);
    for j=1:48 %Baseline loop for this prosumer
        n=rand;
        if n<Pwa(j)
            L(j)=Wa;
        end
    end
    BaseL=BaseL+L;
    L=zeros(1,48);
    Cost=zeros(1,48);
    for j=1:48 %Demand response loop for this prosumer
        n=rand;
        if n<Pwa(j)
            %Cost out each prior timeslot
            for m=1:j
                Cost(1,m)=S(1,m)*Wa;
                n=m+1;
                while n<=j
                Cost(1,m)=Cost(1,m)+S(1,n)*(Vol*0.5*4.181*2/3600);
                n=n+1;
                end
            end
            Costmin=min(Cost(1:j)); %find lowest cost in window before j
            k=find(Cost==Costmin);
            L(k)=Wa;
            for m=k:j
                L(m)=L(m)+(Vol*0.5*4.181*2/3600);%Top up for losses
            end
        end
    end
    AggL=AggL+L;
end
if max(S)<0.01 %check for null S, if so
    HwatrP=BaseL;%return baseline
else HwatrP = AggL;
end

% sum(BaseL)/2
% figure;plot(BaseL)
% 
% sum(AggL)/2
% figure;plot(AggL)
% figure; plot(S)



