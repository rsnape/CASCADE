function Miscr = Miscellaneous(S,NoPros)
%This is a simple elastically variable Miscellaneous demand model.
%First we need a demand baselne - just 1 day for an occupancy of 2 from
%Babak's csv file.
Miscb=[0.04031214,0.02020068,0.03438186,0.03325406,0.0400951,0.01604309,0.0225,0.0125,0.01,0.06,0.01141667,0.0125,0.05185152,0.11494817,0.22360774,0.33747476,0.27196744,0.19265951,0.19333467,0.17765822,0.1086669,0.17783328,0.16923629,0.18924849,0.11070066,0.14250155,0.11957968,0.30040286,0.20898771,0.17686663,0.06255636,0.11487593,0.22338866,0.38887025,0.51329395,0.50532655,0.56750904,0.68094904,0.48789676,0.36387468,0.32864427,0.28582013,0.37897266,0.42233615,0.34407723,0.35488604,0.31635204,0.26309489
];%Then pick up the balanced baseline "price" signal S taken from CASCADE Misc baseline
e=0.05; %Elasticity factor
%
%Check to see if S is null
if max(S)<0.01
    Miscr=NoPros*Miscb;
    return
end
Miscr=zeros(1,48);
for i=1:48
Miscr(i)= Miscb(i)*(1-S(i)*e);
end
%figure;plot(Miscr-Miscb)%demand response
%
%Finally multiply up to requisite number of prosumers
Miscr=NoPros*Miscr;
%sum(Miscr)/2 %total kWh


