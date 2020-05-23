package ch.yax.kafka.ingest.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration()
@ConfigurationProperties(prefix = "ingest")
@ToString
@Slf4j
public class EndpointConfig {
  private final Map<String, Endpoint> endpoints = new HashMap<>();

  @PostConstruct
  public void init() {
    log.info("endpoint config {}", endpoints);
    for (Endpoint endpoint : endpoints.values()) {
      if (endpoint.hasSchema()) {
        log.info("load avro schema for endpoint {}", endpoint);
        endpoint
            .getSchema()
            .setAvro(new org.apache.avro.Schema.Parser().parse(endpoint.getSchema().getValue()));
      }
    }
  }

  public Map<String, Endpoint> getEndpoints() {
    return endpoints;
  }

  public Optional<Endpoint> getEndpointById(String id) {

    Endpoint endpoint = endpoints.get(id);
    if (endpoint == null) {
      return Optional.empty();
    } else {
      return Optional.of(endpoint);
    }
  }

  @Data
  @ToString
  public static class Endpoint {
    private String topic;
    private Schema schema;

    public boolean hasSchema() {
      return schema != null;
    }
  }

  @Data
  @ToString
  public static class Schema {
    private String path;
    private String value;
    private org.apache.avro.Schema avro;
  }
}
