package net.floodlightcontroller.odin.master;

import net.floodlightcontroller.util.MACAddress;

public class NotificationCallbackContext {
	public final MACAddress clientHwAddress;
	public final IOdinAgent agent;
	public final long value;
	public final long client_average; // Average power
	public final int client_triggers; // Number of triggers for average power
	
	public NotificationCallbackContext(final MACAddress clientHwAddress, final IOdinAgent agent, final long value, final long average, final int triggers) {
		this.clientHwAddress = clientHwAddress;
		this.agent = agent;
		this.value = value;
		this.client_average = average;
		this.client_triggers = triggers;
	}
}
