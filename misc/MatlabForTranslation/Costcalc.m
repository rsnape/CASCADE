function Pcostmin =Costcalc(S)
%This is a costing function called by the Aggregator's optimisation routine
%i.e. fmincon
global B coeffs_pos coeffs_neg Dpredmin Dpred Cavge Kpos Kneg Cost

% %Predict the response from the aggregator's models using the regression
% %values
% for j=1:48
%     if S(1,j)>0
%         Dpredmin(1,j)=B(1,j)+coeffs_pos(1,j)+(B(1,j)*S(1,j)*coeffs_pos(2,j));
%     elseif S(1,j)<0
%         Dpredmin(1,j)=B(1,j)+coeffs_neg(1,j)+(B(1,j)*S(1,j)*coeffs_neg(2,j));
%     end
% end
% 
% %Now cost it out and return result
% Pcostmin = sum(Dpredmin.*Cost)/(1000*2);

%Predict the response from the aggregator's models using the simple method
for j=1:48
    if S(1,j)>0
        Dpred(1,j)=B(1,j)+Cavge(1,j)+(B(1,j)*S(1,j)*Kpos(1,j));
    elseif S(1,j)<0
        Dpred(1,j)=B(1,j)+Cavge(1,j)+(B(1,j)*S(1,j)*Kneg(1,j));
    end
end
Pcostmin = sum(Dpred.*Cost)/(1000*2);
