package net.floodlightcontroller.odin.master;

import java.net.InetAddress;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.odin.master.IOdinAgent;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.util.MACAddress;

/**
 * 
 * Stub OdinAgent class to be used for testing. Wi5 NOTE: we introduce a new variable 
 * channel to map the physical Wi-Fi channel used by the access point.
 * 
 * @author Lalith Suresh <suresh.lalith@gmail.com>
 * 
 */
class StubOdinAgent implements IOdinAgent {

	private IOFSwitch sw = null;
	private InetAddress ipAddr = null;
	private long lastHeard;
	private int channel; 
	private ConcurrentSkipListSet<OdinClient> clientList = new ConcurrentSkipListSet<OdinClient>();
	private int freq;
	private int chan;
	private int lastScan;
	private int txpower;
	
	@Override
	public void addClientLvap(OdinClient oc) {
		clientList.add(oc);
	}

	@Override
	public InetAddress getIpAddress() {
		return ipAddr;
	}
	
	@Override
	public Map<MACAddress, Map<String, String>> getTxStats() {
		return null;
	}
	
	@Override
	public Map<MACAddress, Map<String, String>> getRxStats() {
		return null;
	}

	@Override
	public IOFSwitch getSwitch() {
		return sw;
	}

	@Override
	public Set<OdinClient> getLvapsRemote() {
		return clientList;
	}

	@Override
	public int init(InetAddress host) {
		this.ipAddr = host;
		
		return 0;
	}

	@Override
	public void removeClientLvap(OdinClient oc) {
		clientList.remove(oc);
	}

	@Override
	public void setSwitch(IOFSwitch sw) {
		this.sw = sw;
	}


	public long getLastHeard () {
		return lastHeard;
	} 
	
	public void setLastHeard (long t) {
		this.lastHeard = t;
	}

	@Override
	public Set<OdinClient> getLvapsLocal() {
		return clientList;
	}

	@Override
	public void setSubscriptions(String subscriptionList) {
		// Do nothing.
	}

	@Override
	public void updateClientLvap(OdinClient oc) {		
	}

	@Override
	public void sendProbeResponse(MACAddress clientHwAddr, MACAddress bssid,
			Set<String> ssidLists) {
	}
	
	@Override
	public void setChannel(int channel) {
		this.channel = channel;
	}
	
	public int getChannel() {
		return channel;
	}
	
	@Override
	public void sendChannelSwitch(MACAddress clientHwAddr, MACAddress bssid, List<String> ssidList, int channel){
		// Do nothing.
	}
	
	@Override
	public int convertFrequencyToChannel(int freq) {
		return chan;
	}
	
	@Override
	public int convertChannelToFrequency(int chan) {
		return freq;
	}
	
	@Override
	public int scanClient(MACAddress clientHwAddr, int channel, int time){
		return lastScan;
	}

	@Override
	public int requestScannedStationsStats (int channel, String ssid){
		return 1;
	}


	@Override
	public Map<MACAddress, Map<String, String>> getScannedStationsStats (String ssid){
		return null;
	}


	@Override
	public int requestSendMesurementBeacon (int channel, String ssid){
		return 1;
	}
		
	@Override
	public int stopSendMesurementBeacon (){
		return 1;
	}
	
	public String setDetectorIpAddress(){
		return null;	
	}
	
	public int getTxPower() {
		return txpower;
	}
	
	@Override
	public String getScannedStaRssi (){
		return null;
	}


}
