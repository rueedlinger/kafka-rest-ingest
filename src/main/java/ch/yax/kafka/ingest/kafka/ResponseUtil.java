package ch.yax.kafka.ingest.kafka;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {

  public static ResponseEntity<Map<String, Object>> createResponse(
      IngestEvent event, HttpStatus status) {
    Map<String, Object> body = createBody(event, status);
    body.put("message", status.getReasonPhrase());
    return new ResponseEntity<>(body, status);
  }

  public static ResponseEntity<Map<String, Object>> createErrorResponse(
      IngestEvent event, HttpStatus status, String errorMessage) {
    Map<String, Object> body = createBody(event, status);
    body.put("message", errorMessage);
    body.put("error", status.getReasonPhrase());
    return new ResponseEntity<>(body, status);
  }

  public static Map<String, Object> createBody(IngestEvent event, HttpStatus status) {
    final Map<String, Object> body = new HashMap<>();
    body.put("endpointId", event.getEndpointId());
    body.put("timestamp", now());
    body.put("status", status.value());
    body.put("id", event.getId());
    return body;
  }

  public static ZonedDateTime now() {
    return ZonedDateTime.now(ZoneId.systemDefault());
  }
}
