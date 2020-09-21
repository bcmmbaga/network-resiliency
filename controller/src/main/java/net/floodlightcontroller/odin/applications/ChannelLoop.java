package net.floodlightcontroller.odin.applications;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.Scanner;
import java.io.*;

import net.floodlightcontroller.odin.master.OdinApplication;
import net.floodlightcontroller.odin.master.OdinClient;

public class ChannelLoop extends OdinApplication {

  // IMPORTANT: this application only works if all the agents in the
  //poolfile are activated before 15 seconds.
  // Otherwise, the application looks for an object that does not exist
  //and gets stopped

  private InetAddress apAddr;
  
  private Scanner in = new Scanner(System.in);
  
  @Override
  public void run() {
	
    try {
			Thread.sleep(15000); // Wait 15 seconds to start
		} catch (InterruptedException e) {
	    e.printStackTrace();
	  }

	for (InetAddress agentAddr: getAgents()) { // First Ap in poolfile
		apAddr = agentAddr;
		System.out.println("[ChannelAssignment] AP: " + apAddr );
		break;
	}
      			
    System.out.println("Press \"ENTER\" to change to channel 1..."); // 
    in.nextLine();
    setChannelToAgent(apAddr,1); // Change channel to 1
        
    System.out.println("Press \"ENTER\" to begin channel iteration...");
    in.nextLine();
	int i=1;
	while(i<=11){
		System.out.println("[ChannelAssignment] New channel for AP " + apAddr + ": " + i);
        setChannelToAgent(apAddr,i);
        try{
          Thread.sleep(10000);
        }catch(InterruptedException e) {
          e.printStackTrace();
        }
        if(i==11){
          i=1;
        }else{
          i++;
        }
	}
  }
} 
