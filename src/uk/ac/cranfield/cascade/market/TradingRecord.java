package uk.ac.cranfield.cascade.market;

public class TradingRecord
{
	double physicalMinimum;
	double physicalMaximum;
	MarginalPriceCurve mPCurve;
	Configs cfg;
	double physicalPlanned;
	double quantityTraded;
	double monetry;
	int tickSize = 1;
	int time = 0;

	public String toString()
	{
		String s = "";

		s += " max=" + physicalMaximum;
		s += " min=" + physicalMinimum;
		s += " pln=" + physicalPlanned;
		s += " qtr=" + quantityTraded;
		s += " mon=" + monetry;
		// s +=" val="+value();

		return s;
	}

	public void enforceLims()
	{
		if (physicalPlanned < physicalMinimum)
			physicalPlanned = physicalMinimum;
		if (physicalPlanned > physicalMaximum)
			physicalPlanned = physicalMaximum;

	}

	public double rampPos()
	{
		if (physicalPlanned == physicalMinimum)
			return 0;
		return (physicalPlanned - physicalMinimum) / (physicalMaximum - physicalMinimum);
	}

	public TradingRecord(MarginalPriceCurve mPC, Configs cfg)
	{
		this.physicalMinimum = mPC.minimumBalance;
		this.physicalMaximum = mPC.maximumBalance;
		this.cfg = cfg;
		mPCurve = mPC;
		physicalPlanned = physicalMinimum + Math.random() * (physicalMaximum - physicalMinimum);
		quantityTraded = 0;
		monetry = 0;
	}

	public TradingRecord clone()
	{
		TradingRecord ntr = new TradingRecord(mPCurve, cfg);
		ntr.physicalPlanned = physicalPlanned;
		ntr.quantityTraded = quantityTraded;
		ntr.monetry = monetry;
		ntr.tickSize = tickSize;
		return ntr;
	}

	public double value(MarketRecord m)
	{
		enforceLims();

		// Add on the value of anything already bought/sold

		// System.out.print("["+monetry);
		double p1;

		// Add on the expected value of clearing the position
		// thisValue += market.estimateValue(i, plannedPhysicalBalance -
		// tr.quantityTraded )
		if (physicalPlanned + quantityTraded > 0)
			// selling
			p1 = (physicalPlanned + quantityTraded) * (m.lastTradePrice * 0.5);
		// p1=(physicalPlanned + quantityTraded)*(m.lastBuyOfferPrice * 0.45 +
		// m.lastTradePrice *0.45);
		else
			// BUying
			p1 = (physicalPlanned + quantityTraded) * (m.lastTradePrice * 1.5);
		// p1 = (physicalPlanned + quantityTraded)*(m.lastSellOfferPrice * 0.55
		// + m.lastTradePrice *0.55);
		// System.out.print(","+p1);

		// Take off the marginal prices of achieving the physical position
		double p2 = (mPCurve.sumPrice(physicalPlanned));
		// if(Double.isNaN(p1) || Double.isNaN(p2) || Double.isNaN(monetry))
		// System.out.println(p1+" "+mPCurve.maximumBalance+" "+physicalPlanned);

		return monetry + p1 - p2;
	}

	public double optomiseSingle(MarketRecord m)
	{
		// Optomisez this single record with no respect to the other steps in
		// the plan

		// Calculte the cost gradient w.r.t physical planned
		TradingRecord lower = clone();
		TradingRecord upper = clone();
		double oldPP;

		double bestV = value(m);
		double bestPP = physicalPlanned;

		for (int i = 0; i < 10; i++)
		{
			int itrCnt = 0;

			do
			{
				oldPP = physicalPlanned;
				lower.physicalPlanned = physicalPlanned - 0.1;
				upper.physicalPlanned = physicalPlanned + 0.1;
				if (lower.physicalPlanned < lower.mPCurve.minimumBalance)
					lower.physicalPlanned = lower.mPCurve.minimumBalance;
				if (upper.physicalPlanned > upper.mPCurve.maximumBalance)
					upper.physicalPlanned = upper.mPCurve.maximumBalance;

				double valueGrad = (upper.value(m) - lower.value(m)) / (upper.physicalPlanned - lower.physicalPlanned);

				// Move the physical planed to increase value
				physicalPlanned += valueGrad;

				// Check that the physical planned is still in bounds
				if (physicalPlanned < mPCurve.minimumBalance)
					physicalPlanned = mPCurve.minimumBalance;
				if (physicalPlanned > mPCurve.maximumBalance)
					physicalPlanned = mPCurve.maximumBalance;
				// System.out.println("["+(upper.physicalPlanned -
				// lower.physicalPlanned)+"]"+physicalPlanned);
			}
			while ((Math.abs(physicalPlanned - oldPP) > 0.1) && (itrCnt++ < 1000));
			double v = value(m);
			if (v > bestV)
			{
				bestV = v;
				bestPP = physicalPlanned;
			}

			if (i == 0)
				physicalPlanned = mPCurve.minimumBalance;
			else if (i == 1)
				physicalPlanned = mPCurve.maximumBalance;
			else
				physicalPlanned = mPCurve.minimumBalance + Math.random() * (mPCurve.maximumBalance - mPCurve.minimumBalance);

		}

		physicalPlanned = bestPP;
		return bestV;
	}

	public double optomiseGDStep(MarketRecord m)
	{
		// Perform on Gradient desent step in an optomisation process
		// Calculte the cost gradient w.r.t physical planned
		TradingRecord lower = clone();
		TradingRecord upper = clone();

		lower.physicalPlanned = physicalPlanned - 0.1;
		upper.physicalPlanned = physicalPlanned + 0.1;
		if (lower.physicalPlanned < lower.mPCurve.minimumBalance)
			lower.physicalPlanned = lower.mPCurve.minimumBalance;
		if (upper.physicalPlanned > upper.mPCurve.maximumBalance)
			upper.physicalPlanned = upper.mPCurve.maximumBalance;

		double valueGrad = (upper.value(m) - lower.value(m)) / (upper.physicalPlanned - lower.physicalPlanned);

		// Move the physical planed to increase value
		return valueGrad;

	}
}
