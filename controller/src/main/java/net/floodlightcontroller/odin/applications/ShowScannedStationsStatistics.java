package net.floodlightcontroller.odin.applications;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Scanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import net.floodlightcontroller.odin.master.OdinApplication;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.odin.master.OdinMaster.ScannParams;
import net.floodlightcontroller.util.MACAddress;

import org.apache.commons.io.output.TeeOutputStream;

public class ShowScannedStationsStatistics extends OdinApplication {

// IMPORTANT: this application only works if all the agents in the
//poolfile are activated before the end of the INITIAL_INTERVAL.
// Otherwise, the application looks for an object that does not exist
//and gets stopped

// SSID to scan
private final String SCANNED_SSID = "*";

//Scann params
private ScannParams SCANN_PARAMS;

// Scanning agents
Map<InetAddress, Integer> scanningAgents = new HashMap<InetAddress, Integer> ();
int result; // Result for scanning

HashSet<OdinClient> clients;

  @Override
  public void run() {
	
	this.SCANN_PARAMS = getInterferenceParams();
    try {
			Thread.sleep(SCANN_PARAMS.time_to_start);
		} catch (InterruptedException e) {
	    e.printStackTrace();
	  }
	
	while (true) {
      try {
        Thread.sleep(SCANN_PARAMS.reporting_period);
        
		clients = new HashSet<OdinClient>(getClients());
		
		// Write on file integration
		PrintStream stdout = System.out; // To enable return to console
		FileOutputStream fos = null;
        PrintStream ps = null;
		
		if(SCANN_PARAMS.filename.length()>0){
            File f = new File(SCANN_PARAMS.filename); // FIXME: Add parameter to poolfile

            try {
                fos = new FileOutputStream(f);
                //we will want to print in standard "System.out" and in "file"
                TeeOutputStream myOut=new TeeOutputStream(System.out, fos);
                ps = new PrintStream(myOut, true); //true - auto-flush after println
                System.setOut(ps); // Both outputs enabled
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		
		System.out.println("[ShowScannedStationsStatistics] External Monitoring Table"); 
		System.out.println("[ShowScannedStationsStatistics] ================"); 
		System.out.println("[ShowScannedStationsStatistics]");

		//For each channel
		for (int num_channel = 1 ; num_channel <= 11 ; ++num_channel) {

			scanningAgents.clear();
			System.out.println("[ShowScannedStationsStatistics] Scanning channel " + num_channel);

			// For each Agent
			System.out.println("[ShowScannedStationsStatistics] Request for scanning during the interval of  " + SCANN_PARAMS.scanning_interval + " ms in SSID " + SCANNED_SSID);	
			for (InetAddress agentAddr: getAgents()) {
	  
				System.out.println("[ShowScannedStationsStatistics] Agent: " + agentAddr);
				if(SCANN_PARAMS.filename.length()>0)
                    System.setOut(stdout); // Return to only console
 				
				// Request statistics
				result = requestScannedStationsStatsFromAgent(agentAddr, num_channel, SCANNED_SSID);		
				scanningAgents.put(agentAddr, result);
			}					
				
			try {
				Thread.sleep(SCANN_PARAMS.scanning_interval + SCANN_PARAMS.added_time);
				} 
			catch (InterruptedException e) {
							e.printStackTrace();
				}
			
			for (InetAddress agentAddr: getAgents()) {
                
                if(SCANN_PARAMS.filename.length()>0)
                    System.setOut(ps); // Both outputs enabled
				System.out.println("[ShowScannedStationsStatistics]");
				System.out.println("[ShowScannedStationsStatistics] Agent: " + agentAddr + " in channel " + num_channel);

				// Reception statistics 
				if (scanningAgents.get(agentAddr) == 0) {
					System.out.println("[ShowScannedStationsStatistics] Agent BUSY during scanning operation");
					continue;				
				}		
				Map<MACAddress, Map<String, String>> vals_rx = getScannedStationsStatsFromAgent(agentAddr, SCANNED_SSID);

				// for each STA scanned by the Agent
				for (Entry<MACAddress, Map<String, String>> vals_entry_rx: vals_rx.entrySet()) {
				// NOTE: the clients currently scanned MAY NOT be the same as the clients who have been associated		
					MACAddress staHwAddr = vals_entry_rx.getKey();
					boolean isWi5Sta = false;
					boolean isWi5Lvap= false;
					System.out.println("\tStation MAC: " + staHwAddr);
					System.out.println("\t\tnum packets: " + vals_entry_rx.getValue().get("packets"));
					System.out.println("\t\tavg rate: " + vals_entry_rx.getValue().get("avg_rate") + " kbps");
					System.out.println("\t\tavg signal: " + vals_entry_rx.getValue().get("avg_signal") + " dBm");
					System.out.println("\t\tavg length: " + vals_entry_rx.getValue().get("avg_len_pkt") + " bytes");
					System.out.println("\t\tair time: " + vals_entry_rx.getValue().get("air_time") + " ms");						
					System.out.println("\t\tinit time: " + vals_entry_rx.getValue().get("first_received") + " sec");
					System.out.println("\t\tend time: " + vals_entry_rx.getValue().get("last_received") + " sec");
					
					for (OdinClient oc: clients) {  // all the clients currently associated							
						if (oc.getMacAddress().equals(staHwAddr)) {
							System.out.println("\t\tAP of client: " + oc.getLvap().getAgent().getIpAddress());
							System.out.println("\t\tChannel of AP: " + getChannelFromAgent(oc.getLvap().getAgent().getIpAddress()));
							System.out.println("\t\tCode: Wi-5 STA");
							System.out.println("");
							isWi5Sta = true;
							break;
						}
						if (oc.getLvap().getBssid().equals(staHwAddr)){
							System.out.println("\t\tAP of client: " + oc.getLvap().getAgent().getIpAddress());
							System.out.println("\t\tChannel of AP: " + getChannelFromAgent(oc.getLvap().getAgent().getIpAddress()));
							System.out.println("\t\tCode: Wi-5 LVAP");		
							System.out.println("");
							isWi5Lvap = true;
							break;
						}
					}
					if (isWi5Sta) {
						continue;
					}
					if (isWi5Lvap) {
						continue;
					}		
					System.out.println("\t\tAP of client: unknown");
					System.out.println("\t\tChannel of AP: unknown");
					if(vals_entry_rx.getValue().get("equipment").equals("AP")){
                        System.out.println("\t\tCode: non-Wi-5 AP");
					}else{
                        System.out.println("\t\tCode: non-Wi-5 STA");
					}
					System.out.println("");		
				}
		    }
		}
		if(SCANN_PARAMS.filename.length()>0){
            System.setOut(stdout); // Return to only console
            closeQuietly(fos); // Close Stream
        }
		promptEnterKey(); // FIXME: Maybe we want to save multiple scannings, changing the filename in each iteration
		
	  } catch (InterruptedException e) {
	      e.printStackTrace();
	    }
	}
  }
public void promptEnterKey(){ // Function to ask for a key
   System.out.println("Press \"ENTER\" to continue...");
   Scanner scanner = new Scanner(System.in);
   scanner.nextLine();
}
void closeQuietly(FileOutputStream out) { // Function to close the stream
    try { out.flush(); out.close(); } catch(Exception e) {} 
}
} 
