package ch.yax.kafka.ingest.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import ch.yax.kafka.ingest.config.EndpointConfiguration;
import ch.yax.kafka.ingest.config.EndpointConfiguration.Endpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/publish")
@Slf4j
public class RestIngestController {

  @Autowired private EndpointConfiguration endpoints;

  @Autowired private KafkaTemplate<Object, Object> template;

  @PostMapping(
      value = "/{eventId}",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> publish(
      final HttpEntity<String> httpEntity, @PathVariable final String eventId) {
    log.info(
        "processing request for eventId = '{}' with payload = '{}'", eventId, httpEntity.getBody());

    Optional<Endpoint> endpoint = endpoints.getEndpointById(eventId);

    if (endpoint.isPresent()) {
      return processEvent(httpEntity, eventId);
    } else {
      log.warn("Endpoint for eventId = '{}' does not exist.", eventId);
      return createErrorResponse(
          eventId,
          HttpStatus.NOT_FOUND,
          String.format("Endpoint for eventId = '%s' does not exist.", eventId));
    }
  }

  private ResponseEntity<Map<String, Object>> processEvent(
      HttpEntity<String> httpEntity, @PathVariable String eventId) {
    try {
      validate(httpEntity.getBody());
      // TODO: send message
      return createResponse(eventId, HttpStatus.OK);

    } catch (final JsonProcessingException ex) {
      log.warn(
          "Payload is not valid json! eventId = '{}', payload = '{}'",
          eventId,
          httpEntity.getBody(),
          ex);
      return createErrorResponse(eventId, HttpStatus.BAD_REQUEST, ex.getOriginalMessage());
    }
  }

  private void validate(final String payload) throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    objectMapper.readTree(payload);
  }

  private ZonedDateTime now() {
    return ZonedDateTime.now(ZoneId.systemDefault());
  }

  private Map<String, Object> createBody(String eventId, HttpStatus status) {
    final Map<String, Object> body = new HashMap<>();
    body.put("eventId", eventId);
    body.put("timestamp", now());
    body.put("status", status.value());
    body.put("message", status.getReasonPhrase());
    return body;
  }

  private ResponseEntity<Map<String, Object>> createResponse(String eventId, HttpStatus status) {
    return new ResponseEntity<>(createBody(eventId, status), status);
  }

  private ResponseEntity<Map<String, Object>> createErrorResponse(
      String eventId, HttpStatus status, String error) {
    Map<String, Object> body = createBody(eventId, status);
    body.put("error", error);
    return new ResponseEntity<>(body, status);
  }
}
