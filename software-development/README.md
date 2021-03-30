# Usage

Launch the application with

```
sbt run
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

In the application user interface started with `sbt run`, connect to the test
kafka broker at the address `localhost` and port `9092`, select one of the
topics that appear to see the records.

Stop the kafka service

```
docker-compose -f kafka-test/docker-compose.yml down
```
