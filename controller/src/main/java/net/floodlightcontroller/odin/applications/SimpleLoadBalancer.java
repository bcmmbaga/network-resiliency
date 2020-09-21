package net.floodlightcontroller.odin.applications;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.floodlightcontroller.odin.master.OdinApplication;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.util.MACAddress;

public class SimpleLoadBalancer extends OdinApplication {

	/*do the balancing every minute*/
	private final int INTERVAL = 60000;
	
	/* define the signal threshold to consider moving a client to an AP */
	private final int SIGNAL_THRESHOLD = 0;

	HashSet<OdinClient> clients;

	/* the table has pairs of MAC - IP Address
	* a STA can be heard by more than one agent
	* so the MAC address of a STA may appear more than once (one per each agent who has heard it above the threshold)
	*/
	/* The table is
	* MAC of the STA		IP of the agent
	* 00:00:00:00:00:01		192.168.0.1
	* 00:00:00:00:00:01		192.168.0.2 
	* 00:00:00:00:00:02		192.168.0.1
	* 00:00:00:00:00:03		192.168.0.3 
	*/
	Map<MACAddress, Set<InetAddress>> hearingMap = new HashMap<MACAddress, Set<InetAddress>> ();

	/* This table will be used for storing the status of the new balance 
	* as you fill the table, you distribute and balance the clients between agents
	* For each agent, stores the number of associated clients 
	* The table is
	* IP		Number of associated clients (in order to allow the load balancing between agents)
	* 192.168.0.1		3
	* 192.168.0.2		1
	* 192.168.0.3		2
	*/
	Map<InetAddress, Integer> newMapping = new HashMap<InetAddress, Integer> ();

	
	@Override
	public void run() {
		
		
		while (true) {
			try {
				Thread.sleep(INTERVAL);
				
				/*all the clients Odin has heared (even non-connected) */				
				clients = new HashSet<OdinClient>(getClients());
				
				hearingMap.clear();
				newMapping.clear();
				
				/*
				 * Probe each AP to get the list of MAC addresses that it can "hear".
				 * We define "able to hear" as "signal strength > SIGNAL_THRESHOLD".
				 * 
				 *  We then build the hearing table.
				 *
				 * Note that the hearing table may not match the current distribution
				 *of clients between the APs
				 */
				 
				/* for each of the agents defined in the Poolfile (APs)*/
				for (InetAddress agentAddr: getAgents()) {
					/* FIXME: if the next line is run before the APs are activated,
					*the program blocks here */
					Map<MACAddress, Map<String, String>> vals = getRxStatsFromAgent(agentAddr);
					
					/* for each STA which has contacted that agent (AP) (not necessarily associated) */
					for (Entry<MACAddress, Map<String, String>> vals_entry: vals.entrySet()) {
						
						MACAddress staHwAddr = vals_entry.getKey();
						
						/* for all the clients registered in Odin (those who have an LVAP) */
						for (OdinClient oc: clients) {
							/* 
							* Check four conditions:
							* - the MAC address of the client must be that of the connected STA
							* - the IP address of the STA cannot be null
							* - the IP address of the STA cannot be 0.0.0.0
							* - the received signal must be over the threshold
							*/
							if (oc.getMacAddress().equals(staHwAddr)
									&& oc.getIpAddress() != null
									&& !oc.getIpAddress().getHostAddress().equals("0.0.0.0")
									&& Integer.parseInt(vals_entry.getValue().get("signal")) >= SIGNAL_THRESHOLD) {
							
								/* if the client is in not in the hearing map, I add
								* the MAC address of the STA to the hearing map table
								* and I initialize the table of agents who have heared it
								*/
								if (!hearingMap.containsKey(staHwAddr))
									hearingMap.put(staHwAddr, new HashSet<InetAddress> ());
									
								/* for that MAC address, add the agent (AP) 
								*  in the table
								*/
								hearingMap.get(staHwAddr).add(agentAddr);
							}
						}
					}
				}
				
				balance();
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void balance() {
		
		if (hearingMap.size() == 0)
			return;
		
		/*
		 *  Now that the hearing map is populated, we re-assign
		 *  clients to each AP in a round robin fashion, constrained
		 *  by the hearing map.
		 */
		 
		/* for all the clients associated to Odin */
		for (OdinClient client: clients) {

			InetAddress minNode = null;
			int minVal = 0;
			
			/* if the client does not have an IP address, do nothing */
			if ( client.getIpAddress() == null
					|| client.getIpAddress().getHostAddress().equals("0.0.0.0"))
				continue;

			/* if the MAC of the client is not in the (just built) hearing map, do nothing */			
			if(hearingMap.get(client.getMacAddress()) == null) {
				System.err.println("Skipping for client: " + client.getMacAddress());
				continue;
			}
				
			/* for each agent (AP) in the hearing table who has heared that client */				
			for (InetAddress agentAddr: hearingMap.get(client.getMacAddress())) {
									
				/* if the IP of the agent is not in the table, add it */	
				if (!newMapping.containsKey(agentAddr)) {
					newMapping.put(agentAddr, 0);
				}
				
				/* get the number of clients currently associated to that agent */
				int val = newMapping.get(agentAddr);
				
				/* assign the most suitable agent */
				if (minNode == null || val < minVal) {
					minVal = val;
					minNode = agentAddr;
				}
			}

			if (minNode == null)
				continue;
			
			/* I move the client to another AP
			* see the definition of the function in OdinMaster.java (~/floodlightcontroller/odin/master)
			* If the client is already associated with the AP, the function will ignore
			 * the request. */
			handoffClientToAp(client.getMacAddress(), minNode);
			
			/* increase the number of clients associated to that agent (the one with the fewest number of clients) */
			newMapping.put (minNode, newMapping.get(minNode) + 1);
		}
	}
}
