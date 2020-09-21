package net.floodlightcontroller.odin.master;


import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.util.MACAddress;

@JsonSerialize(using=OdinAgentSerializer.class)
public interface IOdinAgent {

	
	/**
	 * Probably need a better identifier
	 * @return the agent's IP address
	 */
	public InetAddress getIpAddress ();
	
	
	/**
	 * Get a list of VAPs that the agent is hosting
	 * @return a list of OdinClient entities on the agent
	 */
	public Set<OdinClient> getLvapsRemote ();
	
	
	/**
	 * Return a list of LVAPs that the master knows this
	 * agent is hosting. Between the time an agent has
	 * crashed and the master detecting the crash, this
	 * can return stale values.
	 * 
	 * @return a list of OdinClient entities on the agent
	 */
	public Set<OdinClient> getLvapsLocal ();
	
	/**
	 * Retrieve Tx-stats from the OdinAgent.
	 * 
	 *  @return A map of stations' MAC addresses to a map
	 *  of properties and values.
	 */
	public Map<MACAddress, Map<String, String>> getTxStats ();

	
	/**
	 * Retrive Rx-stats from the OdinAgent.
	 * 
	 *  @return A map of stations' MAC addresses to a map
	 *  of properties and values.
	 */
	public Map<MACAddress, Map<String, String>> getRxStats ();
	
	
	/**
	 * To be called only once, intialises a connection to the OdinAgent's
	 * control socket. We let the connection persist so as to save on
	 * setup/tear-down messages with every invocation of an agent. This
	 * will also help speedup handoffs. This process can be ignored
	 * in a mock agent implementation
	 * 
	 * @param host Click based OdinAgent host
	 * @return 0 on success, -1 otherwise
	 */
	public int init (InetAddress host);
	
	
	/**
	 * Get the IOFSwitch for this agent
	 * @return ofSwitch
	 */
	public IOFSwitch getSwitch ();
	
	
	/**
	 * Set the IOFSwitch entity corresponding to this agent
	 * 
	 * @param sw the IOFSwitch entity for this agent
	 */
	public void setSwitch (IOFSwitch sw);
	
	
	/**
	 * Remove an LVAP from the AP corresponding to this agent
	 * 
	 * @param staHwAddr The STA's ethernet address
	 */
	public void removeClientLvap (OdinClient oc);
	
		
	/**
	 * Add an LVAP to the AP corresponding to this agent
	 * 
	 * @param staHwAddr The STA's ethernet address
	 * @param staIpAddr The STA's IP address
	 * @param vapBssid	The STA specific BSSID
	 * @param staEssid	The STA specific SSID
	 */
	public void addClientLvap (OdinClient oc);
	
	
	/**
	 * Update a virtual access point with possibly new IP, BSSID, or SSID
	 * 
	 * @param staHwAddr The STA's ethernet address
	 * @param staIpAddr The STA's IP address
	 * @param vapBssid The STA specific BSSID
	 * @param staEssid The STA specific SSID
	 */
	public void updateClientLvap(OdinClient oc);
	
	
	public void sendProbeResponse(MACAddress clientHwAddr, MACAddress bssid, Set<String> ssidLists);
	
	/**
	 * Returns timestamp of last heartbeat from agent
	 * @return Timestamp
	 */
	public long getLastHeard (); 
	
	
	/**
	 * Set the lastHeard timestamp of a client
	 * @param t timestamp to update lastHeard value
	 */
	public void setLastHeard (long t);
	
	
	/**
	 * Set subscriptions
	 * @param subscriptions 
	 * @param t timestamp to update lastHeard value
	 */
	public void setSubscriptions (String subscriptionList);
	
	
	/**
	 * Set the AP into a channel	
	 *  
	 * @param  Channel number
	 * @author Luis Sequeira <sequeira@unizar.es>
	 * 
	 */
	public void setChannel(int channel);
	

	/**
	 * Get channel
	 * 
	 * @return Channel number
	 * @author Luis Sequeira <sequeira@unizar.es>
	 * 
	 */
	public int getChannel();
	
	
	/**
	 * Channel Switch Announcement to Client
	 * 
	 * @param Client MAC
	 * @param BSSID
	 * @param SSID list
	 * @param Channel
	 * @author Luis Sequeira <sequeira@unizar.es>
	 * 
	 */
	public void sendChannelSwitch(MACAddress clientHwAddr, MACAddress bssid, List<String> ssidList, int channel);
	
	
	/**
	 * Convert Frequency to Channel in 2.4 GHz and 5 GHz
	 * 
	 * @param Frequency
	 * @author Luis Sequeira <sequeira@unizar.es>
	 * 
	 */
	public int convertFrequencyToChannel(int freq);
	
	
	/**
	 * Convert Channel to Frequency in 2.4 GHz and 5 GHz
	 * 
	 * @param Channel
	 * @author Luis Sequeira <sequeira@unizar.es>
	 * 
	 */
	public int convertChannelToFrequency(int chan);
	

	/**
	 * Scanning for a client in a specific agent (AP)
	 * 
	 * @param Client MAC
	 * @param Channel
	 * @param Scanning time
	 * @return Signal power
	 * 
	 */
	public int scanClient (MACAddress clientHwAddr, int channel, int time);



	/**
	 * Request scanned stations statistics from the agent
	 * @param agentAddr InetAddress of the agent
	 * @param #channel to scan
	 * @param time interval to scan
	 * @param ssid to scan (always is *)
	 * @ If request is accepted return 1, otherwise, return 0
	 */
	public int requestScannedStationsStats (int channel, String ssid);


	/**
	 * Retreive scanned stations statistics from the agent
	 * @param agentAddr InetAddress of the agent
	 * @return Key-Value entries of each recorded statistic for each station 
	 */
	public Map<MACAddress, Map<String, String>> getScannedStationsStats (String ssid);


	/**
	 * Request scanned stations statistics from the agent
	 * @param agentAddr InetAddress of the agent
	 * @param #channel to send mesurement beacon
	 * @param time interval to send mesurement beacon
	 * @param ssid to scan (e.g odin_init)
	 * @ If request is accepted return 1, otherwise, return 0
	 */
	public int requestSendMesurementBeacon (int channel, String ssid);
		

	/**
	 * Stop sending mesurement beacon from the agent
	 * 
	 * @param agentAddr InetAddress of the agent
	 * 
	 */
	public int stopSendMesurementBeacon ();
	
	/**
	 * Get TxPower
	 * 
	 * @return TxPower in dBm
	 * 
	 */
	public int getTxPower();
	
	/**
	 * Returns the Detector IP address added in poolfile
	 * 
	 * @return Detector InetAddress
	 */
	public String setDetectorIpAddress ();
	
	/**
	 * Retreive scanned wi5 stations rssi from the agent
	 * @param agentAddr InetAddress of the agent
	 * @return Key-Value entries of each recorded rssi for each wi5 station 
	 */
	public String getScannedStaRssi ();	
		
}
