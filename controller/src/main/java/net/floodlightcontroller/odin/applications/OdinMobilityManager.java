package net.floodlightcontroller.odin.applications;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.odin.master.OdinApplication;

import net.floodlightcontroller.odin.master.NotificationCallback;
import net.floodlightcontroller.odin.master.NotificationCallbackContext;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.odin.master.OdinEventSubscription;
import net.floodlightcontroller.odin.master.OdinMaster;
import net.floodlightcontroller.odin.master.OdinEventSubscription.Relation;
import net.floodlightcontroller.util.MACAddress;

public class OdinMobilityManager extends OdinApplication {
	protected static Logger log = LoggerFactory.getLogger(OdinMobilityManager.class);

	// a table including each client and its mobility statistics

	private ConcurrentMap<MACAddress, MobilityStats> clientMap = new ConcurrentHashMap<MACAddress, MobilityStats> ();
	private final long HYSTERESIS_THRESHOLD; // milliseconds
	private final long IDLE_CLIENT_THRESHOLD; // milliseconds
	private final long SIGNAL_STRENGTH_THRESHOLD; // dbm

	private final int INTERVAL = 40000; // time before running the application. This leaves you some time for starting the agents

	public OdinMobilityManager () {
		this.HYSTERESIS_THRESHOLD = 2000;
		this.IDLE_CLIENT_THRESHOLD = 6000;
		this.SIGNAL_STRENGTH_THRESHOLD = 27;
	}

	// Used for testing
	public OdinMobilityManager (long hysteresisThresh, long idleClientThresh, long signalStrengthThresh) {
		this.HYSTERESIS_THRESHOLD = hysteresisThresh;
		this.IDLE_CLIENT_THRESHOLD = idleClientThresh;
		this.SIGNAL_STRENGTH_THRESHOLD = signalStrengthThresh;
	}

	/**
	 * Register subscriptions
	 */
	private void init () {
//		log.info("*** OdinMobilityManager initialized ***");
		OdinEventSubscription oes = new OdinEventSubscription();

		
		/* a mobility manager can register a subscription ("*","signal", GREATER_THAN, 180)
		*/
		// FIXME: Add something in order to subscribe the new STA associated to Odin
		// perhaps this can be done in the agent

		//oes.setSubscription("00:0B:6B:84:B2:87", "signal", Relation.GREATER_THAN, 160); //MAC of..?
		oes.setSubscription("*", "signal", Relation.GREATER_THAN, 160);


		NotificationCallback cb = new NotificationCallback() {

			@Override
			public void exec(OdinEventSubscription oes, NotificationCallbackContext cntx) {
				handler(oes, cntx);
			}
		};
	
		/* Before executing this line, make sure the agents declared in poolfile are started */	

		registerSubscription(oes, cb);
	}

	@Override
	public void run() {

		
		/* when the application runs, you need some time to start the agents */
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e){
        		e.printStackTrace();
		}

		init (); 
		

		// Purely reactive, so end.
	}


	/**
	 * This handler will handoff a client in the event of its
	 * agent having failed.
	 *
	 * @param oes
	 * @param cntx
	 */
	private void handler (OdinEventSubscription oes, NotificationCallbackContext cntx) {
		// Check to see if this is a client we're tracking
		OdinClient client = getClientFromHwAddress(cntx.clientHwAddress);

		
		// comment the next line or you will have a log message every packet of the STA
		/*log.debug("Mobility manager: notification from " + cntx.clientHwAddress
				+ " from agent " + cntx.agent.getIpAddress() + " val: " + cntx.value + " at " + System.currentTimeMillis());
		*/
		
		/* The client is not registered in Odin, exit */

		if (client == null)
			return;

//		log.debug("Mobility manager: notification from " + cntx.clientHwAddress
//			+ " from agent " + cntx.agent.getIpAddress() + " val: " + cntx.value + " at " + System.currentTimeMillis());

		long currentTimestamp = System.currentTimeMillis();

		// Assign mobility stats object if not already done
		// add an entry in the clientMap table for this client MAC
		// put the statistics in the table: value of the parameter, timestamp, timestamp
		if (!clientMap.containsKey(cntx.clientHwAddress)) {
			clientMap.put(cntx.clientHwAddress, new MobilityStats(cntx.value, currentTimestamp, currentTimestamp));
		}

		
		// get the statistics of that client
		MobilityStats stats = clientMap.get(cntx.clientHwAddress);
		
		// The client is associated to Odin (it has an LVAP), but it does not have an associated agent
		// If client hasn't been assigned an agent, associate it to the current AP

		if (client.getLvap().getAgent() == null) {
			log.info("Mobility manager: client hasn't been asigned an agent: handing off client " + cntx.clientHwAddress
									+ " to agent " + cntx.agent.getIpAddress() + " at " + System.currentTimeMillis());
			handoffClientToAp(cntx.clientHwAddress, cntx.agent.getIpAddress());
			updateStatsWithReassignment (stats, cntx.value, currentTimestamp);
			return;
		}

		// Check for out-of-range client
		// a client has sent nothing during a certain time
		if ((currentTimestamp - stats.lastHeard) > IDLE_CLIENT_THRESHOLD) {
			//if(client.getLvap().getAgent().getIpAddress() == cntx.agent.getIpAddress())

			//log.info("Mobility manager: out of range client: handing off client " + cntx.clientHwAddress
			//		+ " to agent " + cntx.agent.getIpAddress() + " at " + System.currentTimeMillis());
			//handle longer threshold?
			log.info("Mobility manager: client with MAC address " + cntx.clientHwAddress
					+ " was idle longer than " + IDLE_CLIENT_THRESHOLD/1000 + " sec -> Reassociating it to agent " + cntx.agent.getIpAddress());// + " at " + System.currentTimeMillis());
			handoffClientToAp(cntx.clientHwAddress, cntx.agent.getIpAddress());
			updateStatsWithReassignment (stats, cntx.value, currentTimestamp);
			return;
		}

		// If this notification is from the agent that's hosting the client's LVAP. update MobilityStats.
		// Else, check if we should do a handoff.
		if (client.getLvap().getAgent().getIpAddress().equals(cntx.agent.getIpAddress())) {
			stats.signalStrength = cntx.value;
			stats.lastHeard = currentTimestamp;
		}
		else {
			// Don't bother if we're not within hysteresis period
			if (currentTimestamp - stats.assignmentTimestamp < HYSTERESIS_THRESHOLD)
				return;

			// We're outside the hysteresis period, so compare signal strengths for a handoff
			// I check if the strength is higher (THRESHOLD) than the last measurement stored the
			// last time in the other AP
			if (cntx.value >= stats.signalStrength + SIGNAL_STRENGTH_THRESHOLD) {
				log.info("Mobility manager: comparing signal strengths: " + cntx.value + ">= " + stats.signalStrength + " + " + SIGNAL_STRENGTH_THRESHOLD + " :" + "handing off client " + cntx.clientHwAddress
						+ " to agent " + cntx.agent.getIpAddress() + " at " + System.currentTimeMillis());
				handoffClientToAp(cntx.clientHwAddress, cntx.agent.getIpAddress());
				updateStatsWithReassignment (stats, cntx.value, currentTimestamp);
				return;
			}
		}
	}

	private void updateStatsWithReassignment (MobilityStats stats, long signalValue, long now) {
		stats.signalStrength = signalValue;
		stats.lastHeard = now;
		stats.assignmentTimestamp = now;
	}


	private class MobilityStats {
		public long signalStrength;

		public long lastHeard;			// timestamp where it was heard the last time
		public long assignmentTimestamp;	// timestamp it was assigned

		public MobilityStats (long signalStrength, long lastHeard, long assignmentTimestamp) {
			this.signalStrength = signalStrength;
			this.lastHeard = lastHeard;
			this.assignmentTimestamp = assignmentTimestamp;
		}
	}
}
