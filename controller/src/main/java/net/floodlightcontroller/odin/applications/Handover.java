package net.floodlightcontroller.odin.applications;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.floodlightcontroller.odin.master.*;
import net.floodlightcontroller.util.MACAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handover extends OdinApplication {

    /*do the balancing every minute*/
    private final int INTERVAL = 60000;

    /* define the signal threshold to consider moving a client to an AP */
    private final int SIGNAL_THRESHOLD = 0;

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
    Map<MACAddress, Set<InetAddress>> hearingMap = new HashMap<MACAddress, Set<InetAddress>> ();

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

    protected static Logger log = LoggerFactory.getLogger(Handover.class);
    /* A table including each client and its mobility statistics */
    private final long HYSTERESIS_THRESHOLD; // milliseconds
    private final long IDLE_CLIENT_THRESHOLD; // milliseconds
    private final long SIGNAL_STRENGTH_THRESHOLD; // dbm

    public Handover () {
        this.HYSTERESIS_THRESHOLD = 15000;
        this.IDLE_CLIENT_THRESHOLD = 180000; // Must to be bigger than HYSTERESIS_THRESHOLD
        this.SIGNAL_STRENGTH_THRESHOLD = 0;
    }

    /**
     * Register subscriptions
     */
    private void init () {
        OdinEventSubscription oes = new OdinEventSubscription();
        /* FIXME: Add something in order to subscribe more than one STA */
        oes.setSubscription("*", "signal", OdinEventSubscription.Relation.GREATER_THAN, 0); // All clients

        NotificationCallback cb = new NotificationCallback() {
            @Override
            public void exec(OdinEventSubscription oes, NotificationCallbackContext cntx) {
                handler(oes, cntx);
            }
        };
        /* Before executing this line, make sure the agents declared in poolfile are started */
        registerSubscription(oes, cb);
    }


    @Override
    public void run() {
//        try {
//            Thread.sleep(INTERVAL);
//        } catch (InterruptedException e){
//            e.printStackTrace();
//        }
        //assigmentChannel();
        init ();
    }

    /**
     * This handler will handoff a client in the event of its
     * agent having failed.
     *
     * @param oes
     * @param cntx
     */
    private void handler (OdinEventSubscription oes, NotificationCallbackContext cntx) {
        System.out.println(oes.toString());
        System.out.println(cntx.agent.getIpAddress());
        System.out.println(System.currentTimeMillis() -  cntx.agent.getLastHeard());

        OdinClient client = getClientFromHwAddress(cntx.clientHwAddress);
        String ap5 = "192.168.1.5";
        String ap6 = "192.168.1.6";
        InetAddress agentAddr5 = cntx.agent.getIpAddress();
        InetAddress agentAddr6 = cntx.agent.getIpAddress();
        InetAddress nextAgent = cntx.agent.getIpAddress();

        if (System.currentTimeMillis() - cntx.agent.getLastHeard() >= 10 ) {
            System.out.println("agent down");
        }

    }


}
