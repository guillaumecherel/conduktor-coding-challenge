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

docker run --network $NETWORK confluentinc/cp-kafka:6.1.0 kafka-topics \
    --bootstrap-server $BROKER --create --if-not-exists --topic animals \
    --partitions 1
    
docker run --network $NETWORK confluentinc/cp-kafka:6.1.0 kafka-topics \
    --bootstrap-server $BROKER --create --if-not-exists --topic seconds \
    --partitions 2
    

echo "Sending records…"
echo -e "$ANIMALS" | kafkacat -t animals -K: -P

i=0
while true
do
    i=$((i + 1))
    partition=$((i % 2))
    echo $i | kafkacat -t seconds -p $partition -P 
    sleep 1
done
