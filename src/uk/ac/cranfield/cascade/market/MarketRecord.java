package uk.ac.cranfield.cascade.market;

public class MarketRecord
{
	double quantityTraded = 3.0;
	double moneySpent = 3.0;
	int tickSize = 0;
	int time;
	double lastTradePrice = 5;
	double lastBuyOfferPrice = 0.1;
	double lastSellOfferPrice = 25;
	double lastTradeInstantPrice = 0.0;

	void logSale(double price)
	{
		if (price > lastTradePrice * 1.5)
			price = lastTradePrice * 1.5;
		if (price < lastTradePrice * 0.5)
			price = lastTradePrice * 0.5;

		lastTradePrice = 0.9 * lastTradePrice + 0.1 * price;
		lastTradeInstantPrice = price;
	}

	public MarketRecord clone()
	{
		MarketRecord nmr = new MarketRecord();
		nmr.quantityTraded = quantityTraded;
		nmr.moneySpent = moneySpent;
		nmr.tickSize = tickSize;

		nmr.lastTradePrice = lastTradePrice;
		nmr.lastBuyOfferPrice = lastBuyOfferPrice;
		nmr.lastSellOfferPrice = lastSellOfferPrice;

		return nmr;
	}

	public void log(int time)
	{
		// logs the data about a time tick when we are done;
		System.out.println("LOG:Time= " + time + " price=" + lastTradePrice);
	}

}
