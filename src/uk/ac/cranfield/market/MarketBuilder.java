package uk.ac.cranfield.market;
import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;


public class MarketBuilder implements ContextBuilder<Object> {
	@Override
	public Context build(Context<Object> context) {

		context.setId("market");
		RandomHelper.setSeed(4);
		
		int textCnt = 8;
		for(int i = 0; i < textCnt; i++)
		{	
			double minGen = RandomHelper.nextDoubleFromTo(20000,40000);
			double maxGen = minGen + RandomHelper.nextDoubleFromTo(20000,40000);
			double minGenPrice = RandomHelper.nextDoubleFromTo(0.1, 10);
			double maxGenPrice = minGenPrice + RandomHelper.nextDoubleFromTo(10, 20);
			
			double minD = RandomHelper.nextDoubleFromTo(20000,40000);
			double maxD = minD + RandomHelper.nextDoubleFromTo(20000,40000);
			double maxDPrice = RandomHelper.nextDoubleFromTo(0.1, 10);
			double minDPrice = maxDPrice+RandomHelper.nextDoubleFromTo(10, 20);
			
			
			testAggregator ta = new testAggregator(minGenPrice,maxGenPrice,
					   maxGen,minGen,
					   maxD,maxDPrice,
					   minD,minDPrice);
			ta.stDev =i;
			context.add(ta);
			ScheduleParameters params = ScheduleParameters.createRepeating(1, 1,0);
			RunEnvironment.getInstance().getCurrentSchedule().schedule(params, ta, "updateSupplyDemand");
			
		}
		
		context.add(this);
		ScheduleParameters params = ScheduleParameters.createRepeating(1, 1,1);
		RunEnvironment.getInstance().getCurrentSchedule().schedule(params, this, "runStuff");
		
		
		return context;
	}
	
	public void runStuff()
	{
		Aggregator.runMarket();
	}

}
