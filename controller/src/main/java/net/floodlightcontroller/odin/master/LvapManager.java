package net.floodlightcontroller.odin.master;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;

import net.floodlightcontroller.util.MACAddress;

public class LvapManager {
			
	/**
	 * Get the default flow table entries that Odin associates
	 * with each LVAP
	 * 
	 * @param inetAddr IP address to use for the flow
	 * @return a list of flow mods
	 */
	public List<OFMessage> getDefaultOFModList(InetAddress inetAddr) {
		OFFlowMod flow1 = new OFFlowMod();
		{
			OFMatch match = new OFMatch();
			match.fromString("in_port=2,dl_type=0x0800,nw_src=" + inetAddr.getHostAddress());
			
			OFActionOutput actionOutput = new OFActionOutput ();
			actionOutput.setPort((short) 1);
			actionOutput.setLength((short) OFActionOutput.MINIMUM_LENGTH);
			
			List<OFAction> actionList = new ArrayList<OFAction>();
			actionList.add(actionOutput);
			
		
			flow1.setCookie(12345);
			flow1.setPriority((short) 200); 
			flow1.setMatch(match);
			flow1.setIdleTimeout((short) 0);
			flow1.setActions(actionList);
	        flow1.setLength(U16.t(OFFlowMod.MINIMUM_LENGTH
	        		+ OFActionOutput.MINIMUM_LENGTH));	
		}
		OFFlowMod flow2 = new OFFlowMod();
		{
			OFMatch match = new OFMatch();
			match.fromString("in_port=1,dl_type=0x0800,nw_dst=" + inetAddr.getHostAddress());
			
			OFActionOutput actionOutput = new OFActionOutput ();
			actionOutput.setPort((short) 2);
			actionOutput.setLength((short) OFActionOutput.MINIMUM_LENGTH);
			
			List<OFAction> actionList = new ArrayList<OFAction>();
			actionList.add(actionOutput);
			
		
			flow2.setCookie(12345);
			flow2.setPriority((short) 200);
			flow2.setMatch(match);
			flow2.setIdleTimeout((short) 0);
			flow2.setActions(actionList);
	        flow2.setLength(U16.t(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
		}
	
		ArrayList<OFMessage> list = new ArrayList<OFMessage>();
		
//		list.add(flow1);
//		list.add(flow2);
		
		return list;
	}

}
