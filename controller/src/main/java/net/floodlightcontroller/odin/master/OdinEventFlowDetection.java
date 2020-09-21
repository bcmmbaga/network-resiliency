package net.floodlightcontroller.odin.master;

/**
 * 
 * Odin Applications should use instances of this class to express
 * flow detection requests. One instance of this class represents
 * a single flow detection request.
 * 
 * FIXME: The application should ensure it doesn't install the same
 * flow detection twice.
 * 
 *
 */
public class OdinEventFlowDetection {

   	private String IPSrcAddress;
	private String IPDstAddress;
	private int protocol;
	private int SrcPort;
	private int DstPort;

	
	/**
	 * @return the IPSrcAddress
	 */
	public String getIPSrcAddress() {
		return IPSrcAddress;
	}

	/**
	 * @return the IPDstAddress
	 */
	public String getIPDstAddress() {
		return IPDstAddress;
	}
	
	/**
	 * @return the SrcPort
	 */
	public int getProtocol() {
		return protocol;
	}


	/**
	 * @return the SrcPort
	 */
	public int getSrcPort() {
		return SrcPort;
	}

	/**
	 * @return the SrcPort
	 */
	public int getDstPort() {
		return DstPort;
	}


	/**
	 * Sets a FlowDetection for an event
	 * 
	 */
	public void setFlowDetection (String IPSrcAddress, String IPDstAddress, int protocol, int SrcPort, int DstPort) {

   		this.IPSrcAddress = IPSrcAddress;
		this.IPDstAddress = IPDstAddress;
		this.protocol = protocol;
		this.SrcPort = SrcPort;
		this.DstPort = DstPort;
	}
}
