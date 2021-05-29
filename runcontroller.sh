#!/usr/bin/bash

cd controller/ || exit
java -jar ./target/floodlight.jar -cf ./src/main/resources/floodlightdefault.propertie
