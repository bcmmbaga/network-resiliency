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

import net.floodlightcontroller.odin.master.OdinMaster.ScannParams;

import org.apache.commons.io.output.TeeOutputStream;

public class ShowMatrixOfDistancedBs extends OdinApplication {

// IMPORTANT: this application only works if all the agents in the
//poolfile are activated before the end of the INITIAL_INTERVAL.
// Otherwise, the application looks for an object that does not exist
//and gets stopped

// SSID to scan
private final String SCANNED_SSID = "odin_init";

// Scann params
private ScannParams SCANN_PARAMS;

// Scanning agents
Map<InetAddress, Integer> scanningAgents = new HashMap<InetAddress, Integer> ();
int result; // Result for scanning

// Matrix
private String matrix = "";
private String avg_dB = "";


  @Override
  public void run() {
	
	this.SCANN_PARAMS = getMatrixParams();
    try {
			Thread.sleep(SCANN_PARAMS.time_to_start);
		} catch (InterruptedException e) {
	    e.printStackTrace();
	  }
	
	while (true) {
      try {
        Thread.sleep(SCANN_PARAMS.reporting_period);
        matrix = "";
        		
		System.out.println("[ShowMatrixOfDistancedBs] Matrix of Distance"); 
		System.out.println("[ShowMatrixOfDistancedBs] =================="); 
		System.out.println("[ShowMatrixOfDistancedBs]");

		//For channel SCANNING_CHANNEL
		System.out.println("[ShowMatrixOfDistancedBs] Scanning channel " + SCANN_PARAMS.channel);
		System.out.println("[ShowMatrixOfDistancedBs]");

		for (InetAddress beaconAgentAddr: getAgents()) {
			scanningAgents.clear();
			System.out.println("[ShowMatrixOfDistancedBs] Agent to send measurement beacon: " + beaconAgentAddr);	
			
			// For each Agent
			System.out.println("[ShowMatrixOfDistancedBs] Request for scanning during the interval of  " + SCANN_PARAMS.scanning_interval + " ms in SSID " + SCANNED_SSID);	
			for (InetAddress agentAddr: getAgents()) {
	  			if (agentAddr != beaconAgentAddr) {
					System.out.println("[ShowMatrixOfDistancedBs] Agent: " + agentAddr);	
 				
					// Request distances
					result = requestScannedStationsStatsFromAgent(agentAddr, SCANN_PARAMS.channel, SCANNED_SSID);		
					scanningAgents.put(agentAddr, result);
				}
			}					
				
			// Request to send measurement beacon
			if (requestSendMesurementBeaconFromAgent(beaconAgentAddr, SCANN_PARAMS.channel, SCANNED_SSID) == 0) {
					System.out.println("[ShowMatrixOfDistancedBs] Agent BUSY during measurement beacon operation");
					continue;				
			}

			try {
				Thread.sleep(SCANN_PARAMS.scanning_interval + SCANN_PARAMS.added_time);
				} 
			catch (InterruptedException e) {
							e.printStackTrace();
				}
			
			// Stop sending meesurement beacon
			stopSendMesurementBeaconFromAgent(beaconAgentAddr);
			
			matrix = matrix + beaconAgentAddr.toString().substring(1);

			for (InetAddress agentAddr: getAgents()) {			
				if (agentAddr != beaconAgentAddr) {

					System.out.println("[ShowMatrixOfDistancedBs]");
					System.out.println("[ShowMatrixOfDistancedBs] Agent: " + agentAddr + " in channel " + SCANN_PARAMS.channel);

					// Reception distances
					if (scanningAgents.get(agentAddr) == 0) {
						System.out.println("[ShowMatrixOfDistancedBs] Agent BUSY during scanning operation");
						continue;				
					}		
					Map<MACAddress, Map<String, String>> vals_rx = getScannedStationsStatsFromAgent(agentAddr,SCANNED_SSID);

					// for each STA scanned by the Agent
					for (Entry<MACAddress, Map<String, String>> vals_entry_rx: vals_rx.entrySet()) {
					// NOTE: the clients currently scanned MAY NOT be the same as the clients who have been associated		
						MACAddress APHwAddr = vals_entry_rx.getKey();
						avg_dB = vals_entry_rx.getValue().get("avg_signal");
						System.out.println("\tAP MAC: " + APHwAddr);
						System.out.println("\tavg signal: " + avg_dB + " dBm");
						if(avg_dB.length()>6){
                            matrix = matrix + "\t" + avg_dB.substring(0,6) + " dBm";
						}else{
                            matrix = matrix + "\t" + avg_dB + " dBm   ";
						}
					}

				}else{
                    matrix = matrix + "\t----------";
				}   
			}
			matrix = matrix + "\n";
		}
		//Print matrix
		System.out.println("[ShowMatrixOfDistancedBs] ==================");
        System.out.println(matrix);            
		System.out.println("[ShowMatrixOfDistancedBs] ==================");	
	  } catch (InterruptedException e) {
	      e.printStackTrace();
	    }
	}
  }
} 
