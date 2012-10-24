function Pros = ProsumerGenerator (NoPros)
%This function generates an array Pros with the key prosumer parameters for
%a population size of NoPros
Pros=zeros(NoPros,5);

%Fill column 1 with occupancy, 2 hot water used (litres), 3 thermal mass (kWh/degC)
%4 loss rate(W/degC) for the building and 5 building time constant tor (in timeslots) 
for i=1:NoPros
    n=rand; Occ=1+round(n*4-0.45);%Gives an average occupancy of 2.54
    Pros(i,1)=Occ;
    Vol=46+Occ*(20+rand*12); %Uses EST formula
    Pros(i,2)=Vol;
    n=rand; Tmass= 5+(n*15);Tloss = 50 + (n*450); tor=2*Tmass/(Tloss/1000);
    Pros (i,3)=Tmass;
    Pros(i,4)=Tloss;
    Pros(i,5)=tor;
end

%format compact