package net.floodlightcontroller.odin.master;
import java.net.InetAddress;


public class FlowDetectionCallbackContext {
	public InetAddress odinAgentAddr;
	public final String IPSrcAddress;
	public final String IPDstAddress;
	public final int protocol;
	public final int SrcPort;
	public final int DstPort;

	
	public FlowDetectionCallbackContext(final InetAddress odinAgentAddr, final String IPSrcAddress, final String IPDstAddress, final int protocol, final int SrcPort, final int DstPort) {
   		this.odinAgentAddr = odinAgentAddr;
		this.IPSrcAddress = IPSrcAddress;
		this.IPDstAddress = IPDstAddress;
		this.protocol = protocol;
		this.SrcPort = SrcPort;
		this.DstPort = DstPort;

	}
}