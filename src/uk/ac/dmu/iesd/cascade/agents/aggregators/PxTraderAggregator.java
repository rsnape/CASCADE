package uk.ac.dmu.iesd.cascade.agents.aggregators;

import uk.ac.dmu.iesd.cascade.base.Consts.BMU_CATEGORY;
import uk.ac.dmu.iesd.cascade.base.Consts.BMU_TYPE;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;
import uk.ac.dmu.iesd.cascade.market.IPxTrader;
import uk.ac.dmu.iesd.cascade.market.astem.operators.MarketMessageBoard;


/**
 * @author Babak Mahdavi Ardestani
 * @author Vijay Pakka
 * @version 1.0 $ $Date: 2012/05/16
 */

public abstract class PxTraderAggregator extends AggregatorAgent implements IPxTrader {
	
	protected MarketMessageBoard messageBoard;
	
	public PxTraderAggregator(CascadeContext context, MarketMessageBoard mb, BMU_CATEGORY cat, BMU_TYPE type, double maxGen, double minDem) {
		super(context, cat, type, maxGen, minDem);
		messageBoard = mb;
	}
	
	public void marketStep() {

		int settlementPeriod = mainContext.getSettlementPeriod();
		
		/*
		switch (settlementPeriod) {

		case 13: 
			this.arr_PN = initializePN();
			//System.out.println("arr_PN: "+Arrays.toString(arr_PN));

			break;
		case 19: 
			list_PX_products = messageBoard.getPxProductList();
			list_BSOD = generateBSOforPX(list_PX_products, arr_PN);
			//TestHelper.printListOfBSOD(list_BSOD);

			break;
		case 21: 
			//System.out.println("PN (BEFORE UPDATE): "+Arrays.toString(arr_PN));
			this.arr_PN = updatePN(list_BSOD, arr_PN);
			//System.out.println("PN (AFTER UPDATE): "+Arrays.toString(arr_PN));

			break;
		case 27: 
			list_PX_products = messageBoard.getPxProductList();
			list_BSOD = generateBSOforPX(list_PX_products, arr_PN);
			break;
		case 29: 
			this.arr_PN = updatePN(list_BSOD, arr_PN);
			break;
		case 35: 
			list_PX_products = messageBoard.getPxProductList();
			list_BSOD = generateBSOforPX(list_PX_products, arr_PN);
			break;
		case 37: 
			arr_PN = updatePN(list_BSOD, arr_PN);
			break;

		case 45: 
			//this.arr_PN = updatePN(list_BSOD, arr_PN);
			//FPN = value return by updatePN()
			break;

		case 47: //end of day
			arr_oldPN = updateOldPN(arr_PN, arr_oldPN);
			//System.out.println("arr_oldPN: "+ Arrays.toString(arr_oldPN));
			arr_Margin = updateMarginForBM(arr_oldPN);
			//System.out.println("arr_Margin: "+ Arrays.toString(arr_Margin));

			break;
		}

		if (mainContext.isFirstDay()  && settlementPeriod ==47) {	
			list_BOD = initializeBOD(0, arr_Margin);
			//System.out.println("BODs after initialisation:");
			//TestHelper.printListOfBODs(list_BOD);

		}
		else if (mainContext.isSecondDay() && settlementPeriod !=0 ) {	 
			list_BOD = updateBOD(list_BOA, settlementPeriod,arr_Margin);

		}
		else if (!mainContext.isFirstDay() && !mainContext.isSecondDay() ) {
			list_BOD = updateBOD(list_BOA, settlementPeriod,arr_Margin);

		} 
		*/

	}  
}
