package net.floodlightcontroller.odin.applications;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.odin.master.OdinApplication;

import net.floodlightcontroller.odin.master.FlowDetectionCallback;
import net.floodlightcontroller.odin.master.FlowDetectionCallbackContext;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.odin.master.OdinEventFlowDetection;
import net.floodlightcontroller.odin.master.OdinMaster;

public class FlowDetectionManager extends OdinApplication {
	protected static Logger log = LoggerFactory.getLogger(FlowDetectionManager.class);

	private final String IPSrcAddress;				// Handle a IPSrcAddress or all IPSrcAddress ("*")
	private final String IPDstAddress;				// Handle a IPDstAddress or all IPDstAddress ("*")
	private final int protocol;						// Handle a protocol or all protocol ("*")
	private final int SrcPort;						// Handle a SrcPort or all SrcPort ("*")
	private final int DstPort;						// Handle a DstPort or all DstPort ("*")							

	public FlowDetectionManager () {
		this.IPSrcAddress = "*";
		this.IPDstAddress = "*";
		this.protocol = 0;
		this.SrcPort = 0;
		this.DstPort = 0;
	}

	/**
	 * Register flow detection
	 */
	private void init () {
		OdinEventFlowDetection oefd = new OdinEventFlowDetection();
		oefd.setFlowDetection(this.IPSrcAddress, this.IPSrcAddress, this.protocol, this.SrcPort, this.DstPort); 
		FlowDetectionCallback cb = new FlowDetectionCallback() {
			@Override
			public void exec(OdinEventFlowDetection oefd, FlowDetectionCallbackContext cntx) {
					handler(oefd, cntx);
			}
		};
		/* Before executing this line, make sure the agents declared in poolfile are started */	
		registerFlowDetection(oefd, cb);
	}

	@Override
	public void run() {
		/* When the application runs, you need some time to start the agents */
		this.giveTime(10000);
		init (); 
	}
	
	/**
	 * This method show detected flows
	 *
	 * @param oefd
	 * @param cntx
	 */
	private void handler (OdinEventFlowDetection oefd, FlowDetectionCallbackContext cntx) {

		log.info("[FlowDetectionManager] Detected flow");
		log.info("[FlowDetectionManager]     -> Source IP: " + cntx.IPSrcAddress);
		log.info("[FlowDetectionManager]     -> Destination IP: " + cntx.IPDstAddress);
		log.info("[FlowDetectionManager]     -> Protocol IP: " + cntx.protocol);
		log.info("[FlowDetectionManager]     -> Source Port: " + cntx.SrcPort);
		log.info("[FlowDetectionManager]     -> Destination Port: " + cntx.DstPort);
		log.info("[FlowDetectionManager] from agent: " + cntx.odinAgentAddr + " at " + System.currentTimeMillis());
		log.info("");
		log.info("");
	}
	
	/**
	 * Sleep
	 *
	 * @param time
	 */
	private void giveTime (int time) {
		try {
					Thread.sleep(time);
			} catch (InterruptedException e) {
					e.printStackTrace();
			}
	}
	
}