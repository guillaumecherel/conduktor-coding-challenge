# Usage

Launch the application:

```
sbt run
```

Run the tests:

```
sbt test
```

# Test with a local kafka service

Start a kafka test service locally.

```
docker-compose -f kafka-test/docker-compose.yml up -d
```

Create two topics – `animals` and `seconds` – and send
records to them:

```
kafka-test/produce_msgs.sh
```

This will send a few records to the topic `animals` and will continuously send
records every second to the `seconds` topic.
 
Wait for the topics to be created on the service. Then, connect to the test
kafka broker by entering `localhost:9092` in the application bootstrap address
field. Optionally, enter additionnal properties to send to the KafkaAdmin and
KafkaConsumer in the right-hand panel. Select one of the topics that appear to
see the records and the partitions to consume from.

When finished, stop the kafka service with:

```
docker-compose -f kafka-test/docker-compose.yml down
```
