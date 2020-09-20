#!/bin/sh

# In order to adapt this script to your setup, you must:
# - modify the IP address of the controller (CTLIP)
# - adapt the names of your wireless devices: wlan0-phy0-mon0; wlan1-phy1-mon1
# - add some routes if you need them (route add)
# - modify the name and the route of the .cli script to be used
# - modify the port used by OpenFlow (6633 by default)

# The order is:
# 1.- Launch this script in all the APs. You will see a message "Now you can launch the controller and press Enter"
# 2.- Launch the Wi-5 odin controller
# 3.- Press ENTER on each of the APs

## Variables
echo "Setting variables"
CTLIP=192.168.1.10         # Controller IP address
SW=br0                      # Name of the bridge
CPINTERFACE="eth1.1"          # Interface for the control plane
DPINTERFACE="eth1.2"       # Interface for the data plane 
TAPINTERFACE="ap"           # tap port for connecting to the Internet
MON0INTERFACE="mon0"        # main wireless interface in monitor mode
MON1INTERFACE="mon1"        # auxiliary wireless interface in monitor mode
WIRELESS0INTERFACE="wlan0"  # main wireless interface
PHY0INTERFACE="phy0"        # main wireless physical interface
VSCTL="ovs-vsctl"           # Command to be used to invoke openvswitch

## Stopping Network Manager
# If Network Manager is running, it may modify the file /sys/kernel/debug/ieee80211/phy0/ath9k/bssid_extra
#of this machine, setting it to ff:ff:ff:ff:ff:ff. So the best option is to switch it off
echo "Stopping Network Manager"
service NetworkManager stop

## Setting interfaces
echo "Setting interfaces"
ifconfig $WIRELESS0INTERFACE down
iw phy $PHY0INTERFACE interface add $MON0INTERFACE type monitor
echo "Added $MON0INTERFACE interface"
sleep 1

ip link set name $MON1INTERFACE dev dummy0
echo "Added $MON1INTERFACE interface"

ifconfig $MON0INTERFACE down
ifconfig $MON1INTERFACE down

iwconfig $MON0INTERFACE mode monitor
# iwconfig $MON1INTERFACE mode monitor

ifconfig $MON0INTERFACE up
echo "$MON0INTERFACE is now up"
ifconfig $MON1INTERFACE up
echo "$MON1INTERFACE is now up"

ifconfig $MON0INTERFACE mtu 1532
ifconfig $MON1INTERFACE mtu 1532

iw $PHY0INTERFACE set channel 1
echo "$PHY0INTERFACE is now in channel 1"

ifconfig $WIRELESS0INTERFACE up
echo "$WIRELESS0INTERFACE is now up"



## Routes
# add these routes in order to permit control from other networks (this is very particular of Unizar)
# traffic from these networks will not go through the default gateway
# route add -net 155.210.158.0 netmask 255.255.255.0 gw 155.210.157.254 eth0
# route add -net 155.210.156.0 netmask 255.255.255.0 gw 155.210.157.254 eth0


## OVS
echo "Restarting OpenvSwitch"
/etc/init.d/openvswitch stop
sleep 1
#rmmod openvswitch-switch
# The next line is added in order to start the controller after stopping openvswitch
read -p "Now you can launch the Wi-5 odin controller and press Enter" pause


# Clean the OpenVSwitch database
if [ -d "/etc/openvswitch" ]; then
  echo "OpenVSwitch folder already exists"
  echo "Cleaning OpenVSwitch database"
  rm /etc/openvswitch/*
else
  echo "OpenVSwitch folder created"
  mkdir /etc/openvswitch
fi
if [ -d "/var/run/openvswitch" ]; then
  rm /var/run/openvswitch/*
fi

# Launch OpenVSwitch
echo "Launching OpenVSwitch"
/etc/init.d/openvswitch start

# Create the bridge
$VSCTL add-br $SW
ifconfig $SW up # In OpenWrt 15.05 the bridge is created down

# Configure the OpenFlow Controller
$VSCTL set-controller $SW tcp:$CTLIP:6633

# Add the data plane ports to OpenVSwitch
for i in $DPINTERFACE ; do
  PORT=$i
  ifconfig $PORT up
  $VSCTL add-port $SW $PORT
done


## Launch click
sleep 3
echo "Launching Click"

## fix click-align: warning: default elementmap missing
## elementmap.xml must be copied to the CLICKPATH
export CLICKPATH=/usr/share/click

click-align agent.cli > aagent.cli
click /root/aagent.cli &
sleep 1
# From this moment, a new tap interface called 'TAPINTERFACE' will be created by Click

# Add the 'TAPINTERFACE' interface to OpenVSwitch
echo "Adding Click interface '$TAPINTERFACE' to OVS"
ifconfig $TAPINTERFACE up            # Putting the interface '$TAPINTERFACE' up
$VSCTL add-port $SW $TAPINTERFACE    # Adding 'TAPINTERFACE' interface (click Interface) to OVS
sleep 1

## OpenVSwitch Rules
# OpenFlow rules needed to make it possible for DHCP traffic to arrive to the Wi-5 odin controller
# It may happen that the data plane port is port 1 and the tap port is port 2
ovs-ofctl add-flow br0 in_port=2,dl_type=0x0800,nw_proto=17,tp_dst=67,actions=output:1,CONTROLLER
ovs-ofctl add-flow br0 in_port=1,dl_type=0x0800,nw_proto=17,tp_dst=68,actions=output:CONTROLLER,2

# It may happen that the data plane port is port 2 and the tap port is port 1
ovs-ofctl add-flow br0 in_port=1,dl_type=0x0800,nw_proto=17,tp_dst=67,actions=output:2,CONTROLLER
ovs-ofctl add-flow br0 in_port=2,dl_type=0x0800,nw_proto=17,tp_dst=68,actions=output:CONTROLLER,1
