#!/usr/bin/env bash

TOPIC=$1

# Strict bash
set -euo pipefail

# This script sends records to two topics: animals and seconds. 
# It sends the current date to the latter every seconds forever.  

BROKER=broker:29092

# docker-compose sets the network name to the parent directory name
NETWORK=`realpath $0 | xargs dirname | xargs basename`_default

function kafkacat { 
  docker run --tty \
    --network $NETWORK \
    confluentinc/cp-kafkacat:6.1.0 \
    kafkacat -b $BROKER "$@"
}

kafkacat -C -K: \
  -f '\nKey (%K bytes): %k\t\nValue (%S bytes): %s\n\Partition: %p\tOffset: %o\n--\n' \
  -t $TOPIC
