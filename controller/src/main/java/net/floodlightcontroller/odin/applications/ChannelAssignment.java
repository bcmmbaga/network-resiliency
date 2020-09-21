package net.floodlightcontroller.odin.applications;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Scanner;
import java.io.*;

import net.floodlightcontroller.odin.master.OdinApplication;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.util.MACAddress;

import net.floodlightcontroller.odin.master.OdinMaster.ChannelAssignmentParams;

import org.apache.commons.io.output.TeeOutputStream;

import com.mathworks.toolbox.javabuilder.*;
import wi5.*;
import java.lang.*;

public class ChannelAssignment extends OdinApplication {

  // IMPORTANT: this application only works if all the agents in the
  //poolfile are activated before the end of the INITIAL_INTERVAL.
  // Otherwise, the application looks for an object that does not exist
  //and gets stopped

  // SSID to scan
  private final String SCANNED_SSID = "odin_init";

  // Scann params
  private ChannelAssignmentParams CHANNEL_PARAMS;

  // Scanning agents
  Map<InetAddress, Integer> scanningAgents = new HashMap<InetAddress, Integer> ();
  int result; // Result for scanning

  // Matrix
  private String matrix = "";
  private String avg_dB = "";
  
  // Algorithm results
  
  private int[][] channels = null;
  
  private long time = 0L; // Compare timestamps in ms
  
  private int number_scans = 0;
  
  private int[] txpowerAPs = null;
  
  private int[] channelAPs = null;
  
  private double[] coefII = {0.65, 0.8, 0.6, 0.4, 0.2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; // To calculate the trigger
  
  private int userInt;
  
  private Scanner in = new Scanner(System.in);
  
  HashSet<OdinClient> clients;

  @Override
  public void run() {
	
	this.CHANNEL_PARAMS = getChannelAssignmentParams();
	String operationMode = CHANNEL_PARAMS.mode;
    try {
			Thread.sleep(CHANNEL_PARAMS.time_to_start);
		} catch (InterruptedException e) {
	    e.printStackTrace();
    }
    
    int numAPs = getAgents().size();
    double[][] pathLosses = new double[numAPs][numAPs];
    double[][] matrixII = new double[numAPs][numAPs];
	
    txpowerAPs = new int[numAPs];
    channelAPs = new int[numAPs];
    
    int j=0;
    
    // Write on file integration
    PrintStream stdout = System.out; // To enable return to console
    FileOutputStream fos = null;
    PrintStream ps = null;
        
    if(CHANNEL_PARAMS.filename.length()>0){
      File f = new File(CHANNEL_PARAMS.filename); 

      try {
        fos = new FileOutputStream(f, true); // If the file exists, it will append data to the eof
        //we will want to print in standard "System.out" and in "file"
        TeeOutputStream myOut=new TeeOutputStream(stdout, fos);
        ps = new PrintStream(myOut, true); //true - auto-flush after println
        System.setOut(ps); // Both outputs enabled
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
	
	while (true) {
	
      try {
        
        matrix = "";
        boolean isValidforChAssign = true;
		int row = 0, column = 0;
		if(operationMode.equals("manual")){
          promptEnterKey();
          System.out.println("[ChannelAssignment] ======== Agents information ========");
        }
		
        // Get TxPower and channels from Agents and change channel if needed
        for (InetAddress AgentAddr: getAgents()) {
            channelAPs[j] = getChannelFromAgent(AgentAddr);
            txpowerAPs[j] = getTxPowerFromAgent(AgentAddr);
            if(operationMode.equals("manual")){
              System.out.println("[ChannelAssignment] [ " + j + " ]");
              System.out.println("[ChannelAssignment] Agent:" + AgentAddr);
              System.out.println("[ChannelAssignment]\tCurrent channel: " + channelAPs[j]);
              System.out.println("[ChannelAssignment]\tTxPower: " + txpowerAPs[j] + " dBm");
              System.out.print("[ChannelAssignment] Select channel for AP " + AgentAddr + "[1-11]:");
			  userInt = in.nextInt(); // FIXME assume user will use 1-11
			  System.out.println("[ChannelAssignment] ===================================");	
			  setChannelToAgent(AgentAddr,userInt);
			  channelAPs[j] = userInt;
			}
            j++;
        }
        
        // Associate STAs to specific Agent
        clients = new HashSet<OdinClient>(getClients());
        if(clients.size()>0){
          if(operationMode.equals("manual")){
            System.out.println("\n[ChannelAssignment] ======== Clients information ========");
            for (OdinClient oc: clients) {
              MACAddress eth = oc.getMacAddress(); // client MAC
              InetAddress clientAddr = oc.getIpAddress();
              InetAddress agentAddr = oc.getLvap().getAgent().getIpAddress();
              System.out.println("[ChannelAssignment] Client " + clientAddr + " in agent " + agentAddr);
              System.out.print("[ChannelAssignment] Select Agent for Client " + clientAddr + "[0-"+(j-1)+"]:");
              userInt = in.nextInt();// FIXME assume user will use 0-j-1
              System.out.println("[ChannelAssignment] ===================================");
              InetAddress[] agentsArray = getAgents().toArray(new InetAddress[0]);
              handoffClientToAp(eth, agentsArray[userInt]);
            }
            promptEnterKey();
          }
        }
        j = 0;
        
        
        
		time = System.currentTimeMillis();
        		
		System.out.println("[ChannelAssignment] Matrix of Distance"); 
		System.out.println("[ChannelAssignment] =================="); 
		System.out.println("[ChannelAssignment]");

		//For channel SCANNING_CHANNEL
		System.out.println("[ChannelAssignment] Scanning channel " + CHANNEL_PARAMS.channel);
		System.out.println("[ChannelAssignment]");

		for (InetAddress beaconAgentAddr: getAgents()) {
			scanningAgents.clear();
			System.out.println("[ChannelAssignment] Agent to send measurement beacon: " + beaconAgentAddr);	
			
			// For each Agent
			System.out.println("[ChannelAssignment] Request for scanning during the interval of  " + CHANNEL_PARAMS.scanning_interval + " ms in SSID " + SCANNED_SSID);	
			for (InetAddress agentAddr: getAgents()) {
			
	  			if (agentAddr != beaconAgentAddr) {
					System.out.println("[ChannelAssignment] Agent listening: " + agentAddr);	
					// Request distances
					result = requestScannedStationsStatsFromAgent(agentAddr, CHANNEL_PARAMS.channel, SCANNED_SSID);		
					scanningAgents.put(agentAddr, result);
				}
			}					
	
			// Request to send measurement beacon
			if (requestSendMesurementBeaconFromAgent(beaconAgentAddr, CHANNEL_PARAMS.channel, SCANNED_SSID) == 0) {
					System.out.println("[ChannelAssignment] Agent BUSY during measurement beacon operation");
					isValidforChAssign = false;
					continue;				
			}

			try {
				Thread.sleep(CHANNEL_PARAMS.scanning_interval + CHANNEL_PARAMS.added_time);
				} 
			catch (InterruptedException e) {
							e.printStackTrace();
				}
			
			// Stop sending meesurement beacon
			stopSendMesurementBeaconFromAgent(beaconAgentAddr);
			
			matrix = matrix + beaconAgentAddr.toString().substring(1);

			for (InetAddress agentAddr: getAgents()) {			
				if (agentAddr != beaconAgentAddr) {

					System.out.println("[ChannelAssignment]");
					System.out.println("[ChannelAssignment] Agent: " + agentAddr + " in channel " + CHANNEL_PARAMS.channel);

					// Reception distances
					if (scanningAgents.get(agentAddr) == 0) {
						System.out.println("[ChannelAssignment] Agent BUSY during scanning operation");
						isValidforChAssign = false;
						continue;				
					}		
					Map<MACAddress, Map<String, String>> vals_rx = getScannedStationsStatsFromAgent(agentAddr,SCANNED_SSID);
					System.out.println("[ChannelAssignment] Timestamp - Scan: " + System.currentTimeMillis() + " ms since epoch");
					
					boolean isMultiple = false; // In case there are multiple replies from agent
					
					for (Entry<MACAddress, Map<String, String>> vals_entry_rx: vals_rx.entrySet()) {
						// NOTE: the clients currently scanned MAY NOT be the same as the clients who have been associated		
						MACAddress APHwAddr = vals_entry_rx.getKey();
						avg_dB = vals_entry_rx.getValue().get("avg_signal");
						double losses_dB = txpowerAPs[row] - Double.parseDouble(avg_dB);
						System.out.println("\tAP MAC: " + APHwAddr);
						System.out.println("\tAP TxPower: " + txpowerAPs[row] + " dBm");
						System.out.println("\tavg signal: " + avg_dB + " dBm");
						System.out.println("\tpathloss: " + losses_dB + " dB");
						System.out.println("\tfrom channel: " + channelAPs[row] + " to channel: "+channelAPs[column]);
						int channelDistance = Math.abs(channelAPs[row]-channelAPs[column]);
						System.out.println("\tII coef: " + coefII[channelDistance]);
						
						if(!isMultiple) {
                            double losses = Math.pow(10.0, losses_dB / 10.0); // Linear power
                            double average = 0;
                            if(number_scans!=0){
                                average = Math.pow(10.0, (pathLosses[row][column]) / 10.0); // Linear power average;
                            }
                            average = average  + ((losses - average)/(number_scans +1)); // Cumulative moving average
                            pathLosses[row][column] = 10.0*Math.log10(average); //Average power in dBm
							double avg_signal_dB_II = txpowerAPs[row] - pathLosses[row][column];
							//System.out.println("\tavg_signal_II: " + avg_signal_dB_II);
							double avg_signal_II = Math.pow(10.0, avg_signal_dB_II / 10.0);
							if(coefII[channelDistance]==0){
                                matrixII[row][column] = 0;
							}else{
                                matrixII[row][column] = 10.0*Math.log10(coefII[channelDistance]*avg_signal_II);//II in dB
							}
							avg_dB = String.valueOf(pathLosses[row][column]); // Average string
                            if(avg_dB.length()>6){
                                matrix = matrix + "\t" + avg_dB.substring(0,6) + " dB";
                            }else{
                                matrix = matrix + "\t" + avg_dB + " dB   ";
                            }

                            if(++column >= numAPs) {
                                column = 0;
                                row ++;		
                            }
                            isMultiple = true;
						}
						else
						{	// If there are multiple replies, pathLosses is not valid
							isValidforChAssign = false;
							System.out.println("[ChannelAssignment] ===================================");
							System.out.println("[ChannelAssignment] ERROR - Multiple replies from agent " + agentAddr);
							System.out.println("[ChannelAssignment] ===================================");											
						}
					}

				}else{
                    matrix = matrix + "\t----------";
                    pathLosses[row][column] = 0;
                    matrixII[row][column] = 0;
                    if(++column >= numAPs) {
                            column = 0;
                            row ++;		
                    }
				}   
			}
			matrix = matrix + "\n";
		}
		//Print matrix
		System.out.println("[ChannelAssignment] === MATRIX OF PATHLOSS (dB) ===");
		System.out.println("[ChannelAssignment]     " + (number_scans+1) + " scans\n");
        System.out.println(matrix);            
		System.out.println("[ChannelAssignment] =================================");	
		System.out.println("[ChannelAssignment] Scanning done in: " + (System.currentTimeMillis()-time) + " ms\n");
		System.out.println("[ChannelAssignment] =================================");
		System.out.println("[ChannelAssignment] = MATRIX OF INTERFERENCE IMPACT =\n");
		double sumII = 0;
		for (double[] arrayCoefII: matrixII) {

            System.out.print("[ ");
            for (double coef_II: arrayCoefII) {
                System.out.print(String.format("%.2f",coef_II)+" ");
            }
            System.out.println("]");
        }
        double sumIIlineal = 0;
        for (double[] arrayCoefII: matrixII) {

            for (double coef_II: arrayCoefII) {
                if(coef_II!=0.0){
                    double coef_II_lineal = Math.pow(10.0, coef_II / 10.0);
                    sumIIlineal += coef_II_lineal;
                }
            }
        }
		if(sumIIlineal!=0){ // The algorithm is triggered only if sumII not 0
			sumII = 10.0*Math.log10(sumIIlineal);//II in dB;
        }
		System.out.println("[ChannelAssignment] =================================\n");
        if(number_scans < (CHANNEL_PARAMS.number_scans-1)){
            number_scans++;
            Thread.sleep(CHANNEL_PARAMS.pause);
            continue;
        }
		
		// End of loop for iteration, as result, a moving mean of the matrix
        time = System.currentTimeMillis();
        if((Double.compare(sumII,CHANNEL_PARAMS.threshold.doubleValue())<0)||(sumII==0)){   
            System.out.println("[ChannelAssignment] Interference Impact: " + String.format("%.2f",sumII));
			System.out.println("[ChannelAssignment] Threshold: " + CHANNEL_PARAMS.threshold); // Print Threshold
            System.out.println("[ChannelAssignment] ChannelAssignment not necessary");
            System.out.println("[ChannelAssignment] =================================");
            System.out.println("[ChannelAssignment] Idle for " + CHANNEL_PARAMS.idle_time + " seconds\n");
            number_scans = 0;
            pathLosses = new double[numAPs][numAPs];
            matrixII = new double[numAPs][numAPs];
            Thread.sleep(CHANNEL_PARAMS.idle_time*1000);
            continue;
        }else{
			if(isValidforChAssign) {
				System.out.println("[ChannelAssignment] Interference Impact: " + String.format("%.2f",sumII));
				System.out.println("[ChannelAssignment] Threshold: " + CHANNEL_PARAMS.threshold); // Print Threshold
				channels = this.getChannelAssignments(pathLosses, CHANNEL_PARAMS.method); // Method: 1 for WI5, 2 for RANDOM, 3 for LCC
				System.out.println("[ChannelAssignment] Timestamp - Algorithm: " + System.currentTimeMillis() + " ms since epoch");
				int i=0;
				for (InetAddress agentAddr: getAgents()) {
					System.out.println("[ChannelAssignment] Setting AP " + agentAddr + " to channel: " + channels[0][i]);
					setChannelToAgent(agentAddr,channels[0][i]);
					i++;
				}
			}else{
				System.out.println("[ChannelAssignment] Matrix not valid for channel assignment");
			}
		}
		System.out.println("[ChannelAssignment] Processing done in: " + (System.currentTimeMillis()-time) + " ms");
		System.out.println("[ChannelAssignment] =================================");
		System.out.println("[ChannelAssignment] Idle for " + CHANNEL_PARAMS.idle_time + " seconds\n");
		number_scans = 0;
		pathLosses = new double[numAPs][numAPs];
		matrixII = new double[numAPs][numAPs];
		Thread.sleep(CHANNEL_PARAMS.idle_time*1000);
	  } catch (InterruptedException e) {
	      e.printStackTrace();
      }
	}
  }
  
    private int[][] getChannelAssignments(double[][] pathLosses, int methodType) {

		//System.out.println(System.getProperty("java.library.path"));

		MWNumericArray n = null;   /* Stores method_number */
		Object[] result = null;    /* Stores the result */
		Wi5 channelFinder= null;     /* Stores magic class instance */

		MWNumericArray pathLossMatrix = null;
		
		MWNumericArray channelsArray = null;
		int[][] channels = null;
		
		try
		{	/* Convert and print inputs */
			pathLossMatrix = new MWNumericArray(pathLosses, MWClassID.DOUBLE);
			n = new MWNumericArray(methodType, MWClassID.DOUBLE);
			/* Create new ChannelAssignment object */

			channelFinder = new Wi5();
			result = channelFinder.getChannelAssignments(1, pathLossMatrix, n);

			/* Compute magic square and print result */
			System.out.println("[ChannelAssignment] =======CHANNEL ASSIGNMENTS=======");
			System.out.println(result[0]); // result is type Object
			System.out.println("[ChannelAssignment] =================================");
			
			channelsArray = (MWNumericArray) result[0]; // Object to 2D MWNumericArray
			channels = (int[][]) channelsArray.toIntArray(); // 2D MWNumericArray to int[][]
		}
		catch (Exception e)
		{
			System.out.println("Exception: " + e.toString());
		}

		finally
		{
			/* Free native resources */
			MWArray.disposeArray(pathLossMatrix);
			MWArray.disposeArray(result);
			MWArray.disposeArray(channelsArray);
			if (channelFinder != null)
				channelFinder.dispose();
            return channels;
		}
	}
	public void promptEnterKey(){ // Function to ask for a key
      System.out.println("Press \"ENTER\" to continue...");
      Scanner scanner = new Scanner(System.in);
      scanner.nextLine();
    }
} 
