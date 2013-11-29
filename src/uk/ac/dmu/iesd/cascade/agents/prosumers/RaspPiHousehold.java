/**
 * 
 */
package uk.ac.dmu.iesd.cascade.agents.prosumers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.visualization.IDisplay;
import repast.simphony.visualizationOGL2D.DisplayOGL2D;
import uk.ac.dmu.iesd.cascade.base.Consts;
import uk.ac.dmu.iesd.cascade.context.CascadeContext;

/**
 * @author richard
 * 
 */
public class RaspPiHousehold extends HouseholdProsumer {
	String piURL = null;

	private boolean fridgeOn = true;

	private boolean heaterOn;

	@ScheduledMethod(start = 100, interval = 1)
	public void controlThePi() {
		/*
		 * if (this.getContext().getTimeslotOfDay() == 0) { if (fridgeOn) {
		 * switchFridgeOff(); } else { switchFridgeOn(); } }
		 */

		setOptions();

		if (this.getWaterHeatProfile()[timeOfDay] > 0.1) {
			if (!heaterOn) {
				switchOnHeat();
			}
		}
		 else {
			 if (heaterOn)
			 {
				switchOffHeat();
			 }
			}

		if (this.isHasColdAppliances() && coldApplianceProfile != null) {
			double fridgeLoad = 0;

			if (this.isHasFridgeFreezer()) {
				fridgeLoad = ((double[]) coldApplianceProfiles
						.get(Consts.COLD_APP_FRIDGEFREEZER))[time
						% coldApplianceProfile.length];

			} else {
				fridgeLoad = ((double[]) coldApplianceProfiles
						.get(Consts.COLD_APP_FRIDGE))[time
						% coldApplianceProfile.length];
			}

			//System.err.print(fridgeLoad + ",");

			if (fridgeOn) {
				if (fridgeLoad == 0) {
					switchFridgeOff();
				}
			} else {
				if (fridgeLoad > 0) {
					switchFridgeOn();
				}
			}

		}
	}

	@ScheduledMethod(start = 0, interval = 0, priority = Consts.PROSUMER_PRIORITY_FIFTH)
	public void probeSpecificAgent()
	{
//	if (this.getAgentID() == 1001)
	{
		ArrayList probed = new ArrayList();
		probed.add(this);
		List<IDisplay> listOfDisplays = RunState.getInstance().getGUIRegistry().getDisplays();
		for (IDisplay display : listOfDisplays) {

			if (display instanceof DisplayOGL2D)
			{
				((DisplayOGL2D) display).getProbeSupport().fireProbeEvent(this, probed);
			}
		}
	}
	}
	
	/**
	 * 
	 */
	private void switchOffHeat() {
		WeakHashMap<String, Object> requestVars = new WeakHashMap<String, Object>();
		requestVars.put("device", "1");
		requestVars.put("value", "0");
		sendPiRequest(requestVars);
		heaterOn = false;

	}

	/**
	 * 
	 */
	private void switchOnHeat() {
		WeakHashMap<String, Object> requestVars = new WeakHashMap<String, Object>();
		requestVars.put("device", "1");
		requestVars.put("value", "1");
		sendPiRequest(requestVars);
		heaterOn = true;

	}

	/**
	 * 
	 */
	public void setOptions() {
		this.costThreshold = Consts.HOUSEHOLD_COST_THRESHOLD;
		this.setPredictedCostSignal(Consts.ZERO_COST_SIGNAL);

		this.transmitPropensitySmartControl = (double) RandomHelper
				.nextDouble();

		this.initializeRandomlyDailyElasticityArray(0, 0.1);

		// this.initializeSimilarlyDailyElasticityArray(0.1d);
		this.setRandomlyPercentageMoveableDemand(0,
				Consts.MAX_DOMESTIC_MOVEABLE_LOAD_FRACTION);

		this.exercisesBehaviourChange = true;
		// pAgent.exercisesBehaviourChange = (RandomHelper.nextDouble() > (1 -
		// Consts.HOUSEHOLDS_WILLING_TO_CHANGE_BEHAVIOUR));

		// TODO: We just set smart meter true here - need more sophisticated way
		// to set for different scenarios
		this.hasSmartMeter = true;

		// pAgent.hasSmartControl = (RandomHelper.nextDouble() > (1 -
		// Consts.HOUSEHOLDS_WITH_SMART_CONTROL));
		this.hasSmartControl = true;

		// TODO: we need to set up wattbox after appliances added. This is all a
		// bit
		// non-object oriented. Could do with a proper design methodology here.
		if (this.hasSmartControl)
			this.setWattboxController();
		
		this.setNumOccupants(2);
		

	}

	/**
	 * 
	 */
	private void switchFridgeOff() {
		WeakHashMap<String, Object> requestVars = new WeakHashMap<String, Object>();
		requestVars.put("device", "0");
		requestVars.put("value", "1");
		sendPiRequest(requestVars);
		fridgeOn = false;

	}

	/**
	 * 
	 */
	private void switchFridgeOn() {
		WeakHashMap<String, Object> requestVars = new WeakHashMap<String, Object>();
		requestVars.put("device", "0");
		requestVars.put("value", "0");
		sendPiRequest(requestVars);
		fridgeOn = true;

	}

	private void sendPiRequest(WeakHashMap<String, Object> map) {
		try {
			StringBuffer getURL = new StringBuffer(this.piURL);
			getURL.append("?");

			for (String k : map.keySet()) {
				getURL.append(k);
				getURL.append("=");
				getURL.append(map.get(k).toString());
				getURL.append("&");
			}
			System.err.println(this.getContext().getTickCount() + " : Trying to send URL " + getURL.toString());
			HttpURLConnection thisConn = (HttpURLConnection) (new URL(
					getURL.toString())).openConnection();
			System.err.println("Request sent, response code : "
					+ thisConn.getResponseCode() + " - "
					+ thisConn.getResponseMessage());
			// thisConn.setRequestMethod("GET");

		} catch (IOException e) {
			System.err.println("Couldn't connect to Pi when attempted");
			e.printStackTrace();
		}

	}

	/**
	 * @param context
	 * @param otherDemandProfile
	 */
	public RaspPiHousehold(CascadeContext context, double[] otherDemandProfile) {
		super(context, otherDemandProfile);

		piURL = "http://192.168.1.102/action";
	}


}
