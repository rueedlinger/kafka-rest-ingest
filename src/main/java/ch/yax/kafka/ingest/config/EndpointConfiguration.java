package ch.yax.kafka.ingest.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration()
@ConfigurationProperties(prefix = "ingest")
@ToString
@Slf4j
public class EndpointConfiguration {
  private final Map<String, Endpoint> endpoints = new HashMap<>();

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
    private boolean avro = false;
  }
}
