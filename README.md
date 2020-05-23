# kafka-rest-ingest

A simple REST (JSON) to Kafka ingest with Avro support.

## Build

tbd

```bash
mvn clean package
```

```bash
docker-compose build
```

## Run

tbd

```bash
docker-compose up
```

## Config

tbd

```bash
kafka-rest-ingest:
    build: .
    ports:
      - "9000:9000"
    environment:

      # Rest Endpoint Port
      SERVER_PORT: 9000

      # Map the Endpoint /publish/foo to a specific topic
      INGEST_ENDPOINTS_FOO_TOPIC: json-topic

      # Map the Endpoint /publish/bar to a specific AVRO topic
      INGEST_ENDPOINTS_BAR_TOPIC: avro-topic
      # The AVRO schema which should for endpoint /publish/bar
      INGEST_ENDPOINTS_BAR_SCHEMA_VALUE: '{"type":"record","name":"schema1","namespace":"test","fields":[{"name":"name","type":"string"},{"name":"age","type":"int"}]}'

      KAFKA_REST_BOOTSTRAP_SERVERS: localhost:909222
      KAFKA_REST_SCHEMA_REGISTRY_URL: 'http://schema-registry:8081'
      KAFKA_PRODUCER_AUTO_REGISTER_SCHEMA: true
      KAFKA_PRODUCER_RETRIES: 10
      KAFKA_PRODUCER_ACKS: 1
```

### Use
publish json to the JSON Endpoint 

```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"foo":"bar","number":1}' \
  http://localhost:9000/publish/foo
```

publish Json to the Avro Endpoint

```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"name":"spock","age":40}' \
  http://localhost:9000/publish/bar
```

### TODO:
- add more tests
- add more config options (https://kafka.apache.org/documentation/#producerconfigs)
- add support load Avro from file (volume)
- add support for fire and forget (non-blocking). Instead result to client (HTTP 202).
- add endpoint security (Basic Auth, etc.)
- add other schema (JSON, Protobuf, etc.)
