function Wetr = WetAppliances(S,NoPros,Pros)
%This is a wet appliance function for CASCADE prosumer experiments.
%Takes its "price" signal S and occupancy from the calling program.
W=2; %This is the electrical load in kW of the prosumer's washing machine.
Wt = 2; % This is the number of timeslots the prosumer's washing machine runs for.
Tw=8; % This is the length of the run time window acceptable to the user.
% Now create a default start time probability by smoothing and normalising
% a misc shape
Wp=[-0.460301702,-0.570667217,-0.49835213,-0.539260904,-0.498906865,-0.553806777,-0.245395496,-0.613885744,-0.698705092,-0.703363678,-0.652772871,-0.707372103,-0.686348532,-0.612648515,-0.590882172,-0.098438303,-0.094491672,-0.327455518,-0.298392989,0.023934516,0.168998346,0.002046067,0.548319786,0.573525384,0.838588713,0.74169689,0.761130348,0.404541177,0.56171317,-0.175909305,0.079060992,0.090031168,0.137090738,0.594375348,0.878658439,0.948730512,0.661581607,0.477455628,0.331968953,0.123102304,0.262539207,0.444915101,0.04668204,0.113390875,0.202291267,-0.080581645,-0.063068214,-0.245361134];
Psw = Wp+1;
for i = 1:45
    Psw(i)= (Psw(i)+Psw(i+1)+Psw(i+2)+Psw(i+3))/4;
end
Pswmax=max(Psw);
for i=1:48
    Psw(i)=Psw(i)/(48*Pswmax);
end
%The above gives an overall probability of running during the day of 0.57.
%Now run the washing machine at a randomly chosen time given the psw probs.
AggL=zeros(1,48); BaseL=zeros(1,48);%This is the aggregate load across all prosumers.
for j=1:NoPros %Main prosumer generating loop
L=zeros(1,48); %Initialise a load array and loop counter
Occ=Pros(j,1);
    for k=1:Occ %run appliances depending on occupancy
       i=1;
       while i<48
            n=rand;
            if n < Psw(i) && i<47
            L(i)=W;
            L(i+1)=W;
            i=i+1;
            elseif n < Psw(i)&& i==48
                L(i)=W;
            else L(i)=0;
            end
        i=i+1;
       end
    end
   AggL=AggL+L;
end %After this AggL contains a baseline distribution of load.
BaseL=AggL;
if max(S)<0.01 %check for null S and
    Wetr=BaseL; %return baseline.
    return
end
%figure; plot(AggL)
%AggLkWh=sum(AggL)/2;
%'Baseline total='
%AggLkWh


%Now run an optimisation which makes the probability of running within the
%user's chosen time window inversely proportionate to the cost.
AggL=zeros(1,48); %reinitialise
for j=1:NoPros
L=zeros(1,48); %Initialise a load array and loop counter
Occ=Pros(j,1);
Wd=zeros(1,Tw); % Initialise a window array
    for k=1:Occ %run appliances depending on occupancy
        i=1;
        while i<48
            if rand < Psw(i)
                if i<=(48-Tw)
                Smax=max(S(i:i+Tw-1));%find highest in window
                    for k = 1:Tw
                        Wd(k)=Smax-S(i+k-1);%fill Wd with values that increase as S falls
                    end
                Wdsum=sum(Wd);
                    for k = 1:Tw
                        Wd(k)=Wd(k)/Wdsum;%normalise Wd
                    end
                    for k = 2:Tw
                        Wd(k)=Wd(k)+Wd(k-1);%create intervals proportionate to Wd
                    end
                N = rand;k=1; %Throw a dice and reset k
                    while Wd(k)<N %find k value to start the washing
                        k=k+1;
                    end
                L(i+k-1)=W;
                L(i+k)=W;
                i=i+Tw-1;
                elseif i<48
                L(i)=W;
                L(i+1)=W;
                i=i+1;
                else L(i)=W;
                end
            else L(i)=0;
            end
        i=i+1;
        end
    end
    AggL=AggL+L;
end
Wetr=AggL;
%figure;plot(AggL)
%AggLkWh=sum(AggL)/2;
%'Dip-filling total ='
%AggLkWh

    

