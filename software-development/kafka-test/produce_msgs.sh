#!/usr/bin/env bash

# Strict bash
set -euo pipefail

# This script sends records to two topics: animals and seconds. 
# It sends the current date to the latter every seconds forever.  

BROKER=broker:29092

# docker-compose sets the network name from the parent directory name
NETWORK=`realpath $0 | xargs dirname | xargs basename`_default

function kafkacat { 
  docker run --interactive \
    --network $NETWORK \
    confluentinc/cp-kafkacat:6.1.0 \
    kafkacat -b $BROKER "$@"
}

ANIMALS="1:Turtle
2:Elephant
3:Moon fish"

function seconds {
  while true
  do
    date
    sleep 1
  done

}

echo -e "$ANIMALS" | kafkacat -t animals -K: -P
seconds | kafkacat -t seconds -P
