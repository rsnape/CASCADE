function [c,ceq] = Trainval(S) 
%This has the nonlinear constraints on S determined by the training pattern
%to make sure the aggregate deviation does not exceed training values.
ceq=0;
c=zeros(2,1);
for n=1:48
    if S(n)>0
        c(1,1)=c(1,1)+S(n);
    elseif S(n)<0
        c(2,1)=c(2,1)+S(n);
    end
end
c(1,1)=c(1,1)-12; %Ensures total of S positives cannot exceed 12
ceq=c(1,1)+c(2,1)+12; %Ensures positive and negative totals of S are equal
