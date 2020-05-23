package ch.yax.kafka.ingest.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import ch.yax.kafka.ingest.kafka.IngestEvent;
import ch.yax.kafka.ingest.kafka.KafkaProducer;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping(value = "/publish")
@Slf4j
public class KafkaRestIngestController {

  private final KafkaProducer kafkaProducer;

  public KafkaRestIngestController(KafkaProducer kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  @PostMapping(
      value = "/{eventId}",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public DeferredResult<ResponseEntity<Map<String, Object>>> publish(
      final HttpEntity<String> httpEntity, @PathVariable final String eventId) {

    String payload = httpEntity.getBody();

    IngestEvent event = IngestEvent.builder().payload(payload).endpointId(eventId).build();

    log.info("processing event = '{}'", event);
    return kafkaProducer.publishEvent(event);
  }
}
