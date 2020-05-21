# kafka-rest-ingest

tbd


## Build

```bash
mvn clean package
```


```bash
docker build -t ingest .
```

## Run

```bash
docker run --publish 8080:8080 ingest
```
