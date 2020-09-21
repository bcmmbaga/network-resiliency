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

public class ShowStatistics extends OdinApplication {

// IMPORTANT: this application only works if all the agents in the
//poolfile are activated before the end of the INITIAL_INTERVAL.
// Otherwise, the application looks for an object that does not exist
//and gets stopped

// this interval is for allowing the agents to connect to the controller
private final int INITIAL_INTERVAL = 30000; // in ms

// this interval this interval is the period for showing the statistics
private final int REPORTING_PERIOD = 15000; // in ms

HashSet<OdinClient> clients;

  @Override
  public void run() {
    try {
			Thread.sleep(INITIAL_INTERVAL);
		} catch (InterruptedException e) {
	    e.printStackTrace();
	  }

    while (true) {
      try {
        Thread.sleep(REPORTING_PERIOD);
        clients = new HashSet<OdinClient>(getClients());
          /*
					* If a handoff has happened during the statistic gathering period,
					* it may happen that a client appears in the statistics of two agents
					* because it has been handed off from one agent to another
					*/

		System.out.println("[ShowStatistics] List of Agents:"); //JOSE

	  // for each Agent
	  for (InetAddress agentAddr: getAgents()) {
	  
	    System.out.println("[ShowStatistics] Agent: " + agentAddr);
   
		// Transmission statistics
		System.out.println("[ShowStatistics] Last ping heard from agent " + agentAddr + " " + getLastHeardFromAgent(agentAddr));

	    Map<MACAddress, Map<String, String>> vals_tx = getTxStatsFromAgent(agentAddr);  // all the clients who have statistics
    
		// Reception statistics
	    Map<MACAddress, Map<String, String>> vals_rx = getRxStatsFromAgent(agentAddr);

	    for (OdinClient oc: clients) {  // all the clients currently associated
	    // NOTE: the clients currently associated MAY NOT be the same as the clients who have statistics

	    //System.out.println("\n[ShowStatistics] Corrected read");
	    
         // for each STA associated to the Agent
	     for (Entry<MACAddress, Map<String, String>> vals_entry_rx: vals_rx.entrySet()) {

	      MACAddress staHwAddr = vals_entry_rx.getKey();
			 if (oc.getMacAddress().equals(staHwAddr) && oc.getIpAddress() != null && !oc.getIpAddress().getHostAddress().equals("0.0.0.0")) {
			     System.out.println("\tUplink station MAC: " + staHwAddr + " IP: " + oc.getIpAddress().getHostAddress());
			    System.out.println("\t\tnum packets: " + vals_entry_rx.getValue().get("packets"));
			    System.out.println("\t\tavg rate: " + vals_entry_rx.getValue().get("avg_rate") + " kbps");
			    System.out.println("\t\tavg signal: " + vals_entry_rx.getValue().get("avg_signal") + " dBm");
			    System.out.println("\t\tavg length: " + vals_entry_rx.getValue().get("avg_len_pkt") + " bytes");
			   System.out.println("\t\tair time: " + vals_entry_rx.getValue().get("air_time") + " ms");						
			   System.out.println("\t\tinit time: " + vals_entry_rx.getValue().get("first_received") + " sec");
			   System.out.println("\t\tend time: " + vals_entry_rx.getValue().get("last_received") + " sec");
				  System.out.println("");
			}
		  }
		 // for each STA associated to the Agent
	     for (Entry<MACAddress, Map<String, String>> vals_entry_tx: vals_tx.entrySet()) {
	       MACAddress staHwAddr = vals_entry_tx.getKey();
            if (oc.getMacAddress().equals(staHwAddr) && oc.getIpAddress() != null && !oc.getIpAddress().getHostAddress().equals("0.0.0.0")) {
	          System.out.println("\tDownlink station MAC: " + staHwAddr + " IP: " + oc.getIpAddress().getHostAddress());
	          System.out.println("\t\tnum packets: " + vals_entry_tx.getValue().get("packets"));
	          System.out.println("\t\tavg rate: " + vals_entry_tx.getValue().get("avg_rate") + " kbps");
	          System.out.println("\t\tavg signal: " + vals_entry_tx.getValue().get("avg_signal") + " dBm");
	          System.out.println("\t\tavg length: " + vals_entry_tx.getValue().get("avg_len_pkt") + " bytes");
	          System.out.println("\t\tair time: " + vals_entry_tx.getValue().get("air_time") + " ms");						
	          System.out.println("\t\tinit time: " + vals_entry_tx.getValue().get("first_received") + " sec");
	          System.out.println("\t\tend time: " + vals_entry_tx.getValue().get("last_received") + " sec");
	          System.out.println("");
	    	}

	          }
	        }	    
	      }
	    } catch (InterruptedException e) {
	      e.printStackTrace();
	    }
    }
  }
}
