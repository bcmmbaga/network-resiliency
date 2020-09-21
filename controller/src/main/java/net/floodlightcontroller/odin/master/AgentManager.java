package net.floodlightcontroller.odin.master;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.odin.master.IOdinAgent;
import net.floodlightcontroller.odin.master.OdinAgentFactory;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.odin.master.OdinMaster;


class AgentManager {
	private final ConcurrentHashMap<InetAddress, IOdinAgent> agentMap = new ConcurrentHashMap<InetAddress,IOdinAgent>();
    protected static Logger log = LoggerFactory.getLogger(OdinMaster.class);

    private IFloodlightProviderService floodlightProvider;
    private final ClientManager clientManager;
    private final PoolManager poolManager;

	private final Timer failureDetectionTimer = new Timer();
	private int agentTimeout = 6000;

	protected AgentManager (ClientManager clientManager, PoolManager poolManager) {
		this.clientManager = clientManager;
		this.poolManager = poolManager;
	}

	protected void setFloodlightProvider(final IFloodlightProviderService provider) {
    	floodlightProvider = provider;
    }


    protected void setAgentTimeout (final int timeout) {
    	assert (timeout > 0);
    	agentTimeout = timeout;
    }


    /**
	 * Confirm if the agent corresponding to an InetAddress
	 * is being tracked.
	 *
	 * @param odinAgentInetAddress
	 * @return true if the agent is being tracked
	 */
	protected boolean isTracked(final InetAddress odinAgentInetAddress) {
		return agentMap.containsKey(odinAgentInetAddress);
	}


	/**
	 * Get the list of agents being tracked for a particular pool
	 * @return agentMap
	 */
	protected Map<InetAddress, IOdinAgent> getAgents() {
		return Collections.unmodifiableMap(agentMap);
	}


	/**
	 * Get a reference to an agent
	 *
	 * @param agentInetAddr
	 */
	protected IOdinAgent getAgent(final InetAddress agentInetAddr) {
		assert (agentInetAddr != null);
		return agentMap.get(agentInetAddr);
	}


	/**
	 * Removes an agent from the agent manager
	 *
	 * @param agentInetAddr
	 */
	protected void removeAgent(InetAddress agentInetAddr) {
		synchronized (this) {
			agentMap.remove(agentInetAddr);
		}
	}

	// Handle protocol messages here

	/**
     * Handle a ping from an agent. If an agent was added to the
     * agent map, return true.
     *
     * @param odinAgentAddr
     * @return true if an agent was added
     */
	protected boolean receivePing(final InetAddress odinAgentAddr) {

		//if we receive a ping from a new agent
		if ((!isTracked (odinAgentAddr))&&(!odinAgentAddr.getHostAddress().equals(OdinMaster.getDetectorIpAddress()))) {

			log.debug("Ping message from: " + odinAgentAddr);

		};

    	/*
    	 * If this is not the first time we're hearing from this
    	 * agent, then skip.
    	 */
    	if (odinAgentAddr == null || isTracked (odinAgentAddr)) {
    		return false;
    	}

    	IOFSwitch ofSwitch = null;

		/*
		 * If the OFSwitch corresponding to the agent has already
		 * registered here, then set it in the OdinAgent object.
		 * We avoid registering the agent until its corresponding
		 * OFSwitch has done so.
		 */
		for (IOFSwitch sw: floodlightProvider.getSwitches().values()) {

			/*
			 * We're binding by IP addresses now, because we want to pool
			 * an OFSwitch with its corresponding OdinAgent, if any.
			 */
			String switchIpAddr = ((InetSocketAddress) sw.getChannel().getRemoteAddress()).getAddress().getHostAddress();

			if (switchIpAddr.equals(odinAgentAddr.getHostAddress())) {
				ofSwitch = sw;
				break;
			}
		}

		if (ofSwitch == null)
			return false;

		synchronized (this) {

			/* Possible if a thread has waited
			 * outside this critical region for
			 * too long
			 */
			if (isTracked(odinAgentAddr))
				return false;

			IOdinAgent oa = OdinAgentFactory.getOdinAgent();
			oa.setSwitch(ofSwitch);
			oa.init(odinAgentAddr);
			oa.setLastHeard(System.currentTimeMillis());
			List<String> poolListForAgent = poolManager.getPoolsForAgent(odinAgentAddr);

    		/*
    		 * It is possible that the controller is recovering from a failure,
    		 * so query the agent to see what LVAPs it hosts, and add them
    		 * to our client tracker accordingly.
    		 */
    		for (OdinClient client: oa.getLvapsRemote()) {

    			OdinClient trackedClient = clientManager.getClients().get(client.getMacAddress());

    			if (trackedClient == null){
    				clientManager.addClient(client);
    				trackedClient = clientManager.getClients().get(client.getMacAddress());

    				/*
    				 * We need to find the pool the client was previously assigned to.
    				 * The only information we have at this point is the
    				 * SSID list of the client's LVAP. This can be simplified in
    				 * future by adding a "pool" field to the LVAP struct.
    				 */

    				for (String pool: poolListForAgent) {
    					/*
    					 * Every SSID in every pool is unique, so we need to use only one
    					 * of the lvap's SSIDs to find the right pool.
    					 */
    					String ssid = client.getLvap().getSsids().get(0);
    					if (poolManager.getSsidListForPool(pool).contains(ssid)) {
    						poolManager.mapClientToPool(trackedClient, pool);
    						break;
    					}

    				}
    			}

    			if (trackedClient.getLvap().getAgent() == null) {
    				trackedClient.getLvap().setAgent(oa);
    			}
    			else if (!trackedClient.getLvap().getAgent().getIpAddress().equals(odinAgentAddr)) {
        			/*
        			 * Race condition:
        			 * - client associated at AP1 before the master failure,
        			 * - master crashes.
        			 * - master re-starts, AP2 connects to the master first.
        			 * - client scans, master assigns it to AP2.
        			 * - AP1 now joins the master again, but it has the client's LVAP as well.
        			 * - Master should now clear the LVAP from AP1.
        			 */
    				oa.removeClientLvap(client);
    			}
    		}

   			agentMap.put(odinAgentAddr, oa);

    		log.info("Adding OdinAgent to map: " + odinAgentAddr.getHostAddress());

    		/* This TimerTask checks the lastHeard value
    		 * of the agent in order to handle failure detection
    		 */
    		failureDetectionTimer.scheduleAtFixedRate(new OdinAgentFailureDetectorTask(oa), 1, agentTimeout/2);
		}

		return true;
	}


	private class OdinAgentFailureDetectorTask extends TimerTask {
		private final IOdinAgent agent;

		OdinAgentFailureDetectorTask (final IOdinAgent oa){
			this.agent = oa;
		}

		@Override
		public void run() {
			//log.info("Executing failure check against: " + agent.getIpAddress());
			if ((System.currentTimeMillis() - agent.getLastHeard()) >= agentTimeout) {
				log.error("Agent: " + agent.getIpAddress() + " has timed out");

				/* This is default behaviour, maybe we should
				 * re-assign the client based on some specific
				 * behaviour
				 */

				// TODO: There should be a way to lock the master
				// during such operations
				for (OdinClient oc: agent.getLvapsLocal()) {
					clientManager.getClients().get(oc.getMacAddress()).getLvap().setAgent(null);
				}

				// Agent should now be cleared out
				removeAgent(agent.getIpAddress());
				this.cancel();
			}
		}

	}

}
