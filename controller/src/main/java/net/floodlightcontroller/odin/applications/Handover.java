package net.floodlightcontroller.odin.applications;

import net.floodlightcontroller.odin.master.OdinApplication;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.util.MACAddress;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Handover extends OdinApplication{

    HashSet<OdinClient> clients;


    /* the table has pairs of MAC - IP Address
     * a STA can be heard by more than one agent
     * so the MAC address of a STA may appear more than once (one per each agent who has heard it above the threshold)
     */
    /* The table is
     * MAC of the STA		IP of the agent
     * 00:00:00:00:00:01		192.168.0.1
     * 00:00:00:00:00:01		192.168.0.2
     * 00:00:00:00:00:02		192.168.0.1
     * 00:00:00:00:00:03		192.168.0.3
     */
    Map<MACAddress, Set<InetAddress>> hearingMap = new HashMap<MACAddress, Set<InetAddress>>();

    /* This table will be used for storing the status of the new balance
     * as you fill the table, you distribute and balance the clients between agents
     * For each agent, stores the number of associated clients
     * The table is
     * IP		Number of associated clients (in order to allow the load balancing between agents)
     * 192.168.0.1		3
     * 192.168.0.2		1
     * 192.168.0.3		2
     */
    Map<InetAddress, Integer> newMapping = new HashMap<InetAddress, Integer> ();

    @Override
    public void run() {

            while(true){
                try{
                    Thread.sleep(60000);

                    //all the clients Odin has heard (even non-connected)
                    clients = new HashSet<OdinClient>(getClients());

                    hearingMap.clear();
                    newMapping.clear();

                    System.out.println(clients);

                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
    }
}