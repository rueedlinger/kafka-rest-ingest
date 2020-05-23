package ch.yax.kafka.ingest.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import ch.yax.kafka.ingest.config.EndpointConfig;
import ch.yax.kafka.ingest.config.EndpointConfig.Endpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.errors.SerializationException;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import tech.allegro.schema.json2avro.converter.AvroConversionException;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

@RestController
@RequestMapping(value = "/publish")
@Slf4j
public class RestIngestController {

  private final EndpointConfig endpointConfig;
  private final KafkaTemplate<Object, Object> rawKafkaTemplate;
  private final KafkaTemplate<Object, Object> avroKafkaTemplate;
  private final JsonAvroConverter converter = new JsonAvroConverter();

  public RestIngestController(
      EndpointConfig endpointConfig,
      KafkaTemplate<Object, Object> rawKafkaTemplate,
      KafkaTemplate<Object, Object> avroKafkaTemplate) {
    this.endpointConfig = endpointConfig;
    this.rawKafkaTemplate = rawKafkaTemplate;
    this.avroKafkaTemplate = avroKafkaTemplate;
  }

  @PostMapping(
      value = "/{eventId}",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public DeferredResult<ResponseEntity<Map<String, Object>>> publish(
      final HttpEntity<String> httpEntity, @PathVariable final String eventId) {
    log.debug(
        "processing request for eventId = '{}' with payload = '{}'", eventId, httpEntity.getBody());

    Optional<Endpoint> endpoint = endpointConfig.getEndpointById(eventId);

    if (endpoint.isPresent()) {
      return processEvent(httpEntity, eventId, endpoint.get());
    } else {
      log.warn("Endpoint for eventId = '{}' does not exist.", eventId);

      DeferredResult<ResponseEntity<Map<String, Object>>> result = new DeferredResult<>();
      result.setErrorResult(
          createErrorResponse(
              eventId,
              HttpStatus.NOT_FOUND,
              String.format("Endpoint for eventId = '%s' does not exist.", eventId)));

      return result;
    }
  }

  private DeferredResult<ResponseEntity<Map<String, Object>>> processEvent(
      HttpEntity<String> httpEntity, String eventId, Endpoint endpoint) {
    try {
      validate(httpEntity.getBody());

      ListenableFuture<SendResult<Object, Object>> future =
          sendMessage(httpEntity.getBody(), endpoint);

      final DeferredResult<ResponseEntity<Map<String, Object>>> response = new DeferredResult<>();

      future.addCallback(
          new ListenableFutureCallback<>() {

            @Override
            public void onSuccess(SendResult<Object, Object> result) {
              log.debug(
                  "Message sent to topic = '{}' with offset = '{}' and partition = '{}'",
                  result.getRecordMetadata().topic(),
                  result.getRecordMetadata().offset(),
                  result.getRecordMetadata().partition());
              response.setResult(createResponse(eventId, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable throwable) {
              log.error("Unable to send message (eventId = '{}')", eventId, throwable);
              response.setErrorResult(
                  createErrorResponse(
                      eventId, HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage()));
            }
          });

      return response;

    } catch (final JsonProcessingException ex) {
      log.warn(
          "Payload is not valid JSON for eventId = '{}' with payload = '{}'",
          eventId,
          httpEntity.getBody(),
          ex);

      DeferredResult<ResponseEntity<Map<String, Object>>> errorResult = new DeferredResult<>();
      errorResult.setErrorResult(
          createErrorResponse(eventId, HttpStatus.BAD_REQUEST, getCauseAsString(ex)));
      return errorResult;
    } catch (AvroConversionException ex) {
      log.warn(
          "Failed to convert JSON payload to Avro for eventId = '{}' with payload = '{}'",
          eventId,
          httpEntity.getBody(),
          ex);

      DeferredResult<ResponseEntity<Map<String, Object>>> errorResult = new DeferredResult<>();
      errorResult.setErrorResult(
          createErrorResponse(eventId, HttpStatus.BAD_REQUEST, getCauseAsString(ex)));
      return errorResult;
    } catch (SerializationException ex) {
      log.warn(
          "Failed to serialize Avro for eventId = '{}' with payload = '{}'",
          eventId,
          httpEntity.getBody(),
          ex.getCause());

      DeferredResult<ResponseEntity<Map<String, Object>>> errorResult = new DeferredResult<>();

      errorResult.setErrorResult(
          createErrorResponse(eventId, HttpStatus.BAD_REQUEST, getCauseAsString(ex)));
      return errorResult;
    }
  }

  @Nullable
  private String getCauseAsString(Exception ex) {
    String message = ex.getMessage();
    if (ex.getCause() != null && ex.getCause().getMessage() != null) {
      message += ". " + ex.getCause().getMessage();
    }
    return message;
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
    body.put("error", status.getReasonPhrase());
    return body;
  }

  private ResponseEntity<Map<String, Object>> createResponse(String eventId, HttpStatus status) {
    return new ResponseEntity<>(createBody(eventId, status), status);
  }

  private ResponseEntity<Map<String, Object>> createErrorResponse(
      String eventId, HttpStatus status, String errorMessage) {
    Map<String, Object> body = createBody(eventId, status);
    body.put("message", errorMessage);
    return new ResponseEntity<>(body, status);
  }

  private ListenableFuture<SendResult<Object, Object>> sendMessage(
      String payload, Endpoint endpoint) {
    if (endpoint.hasSchema()) {
      GenericData.Record record =
          converter.convertToGenericDataRecord(payload.getBytes(), endpoint.getSchema().getAvro());
      return avroKafkaTemplate.send(endpoint.getTopic(), record);
    } else {
      return rawKafkaTemplate.send(endpoint.getTopic(), payload);
    }
  }
}
