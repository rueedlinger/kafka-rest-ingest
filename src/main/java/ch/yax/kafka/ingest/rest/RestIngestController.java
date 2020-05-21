package ch.yax.kafka.ingest.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/publish")
@Slf4j
public class RestIngestController {

  @Autowired
  private Environment environment;

  @GetMapping(value = "/{eventId}", produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> ping(@PathVariable final String eventId) {
    log.info("processing request for event id {}", eventId);
    final Map<String, Object> resp = new HashMap<>();
    resp.put("eventId", eventId);
    resp.put("timestamp", LocalDateTime.now());
    return resp;
  }


  @PostMapping(value = "/{eventId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> publish(final HttpEntity<String> httpEntity,
      @PathVariable final String eventId) {
    log.info("processing request for event id = '{}' with payload = '{}'", eventId,
        httpEntity.getBody());

    try {
      validate(httpEntity.getBody());

      final Map<String, Object> body = new HashMap<>();
      body.put("eventId", eventId);
      body.put("timestamp", LocalDateTime.now());
      body.put("id", generateId());

      return new ResponseEntity<>(body, HttpStatus.OK);

    } catch (final JsonProcessingException ex) {
      log.warn("Payload is not valid json! event id = '{}', payload = '{}'", eventId,
          httpEntity.getBody(), ex);

      final HttpStatus status = HttpStatus.BAD_REQUEST;

      final Map<String, Object> body = new HashMap<>();
      body.put("error", status.getReasonPhrase());
      body.put("status", status.value());
      body.put("message", ex.getOriginalMessage());
      body.put("timestamp", now());
      body.put("id", generateId());

      return new ResponseEntity<>(body, status);
    }

  }

  private String generateId() {
    final UUID uuid = UUID.randomUUID();
    return uuid.toString();
  }

  private void validate(final String payload) throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    objectMapper.readTree(payload);
  }

  private ZonedDateTime now() {
    return ZonedDateTime.now(ZoneId.systemDefault());
  }
}
