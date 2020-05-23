package ch.yax.kafka.ingest.kafka;

import static ch.yax.kafka.ingest.kafka.ResponseUtil.createErrorResponse;
import static ch.yax.kafka.ingest.kafka.ResponseUtil.createResponse;

import ch.yax.kafka.ingest.config.EndpointConfig;
import ch.yax.kafka.ingest.config.EndpointConfig.Endpoint;
import ch.yax.kafka.ingest.kafka.callback.DeferredResultCallback;
import ch.yax.kafka.ingest.kafka.callback.LoggingCallback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.context.request.async.DeferredResult;
import tech.allegro.schema.json2avro.converter.AvroConversionException;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

@Slf4j
@Component
public class KafkaProducer {

  private final EndpointConfig endpointConfig;
  private final KafkaTemplate<Object, Object> rawKafkaTemplate;
  private final KafkaTemplate<Object, Object> avroKafkaTemplate;
  private final JsonAvroConverter converter = new JsonAvroConverter();

  public KafkaProducer(
      EndpointConfig endpointConfig,
      KafkaTemplate<Object, Object> rawKafkaTemplate,
      KafkaTemplate<Object, Object> avroKafkaTemplate) {
    this.endpointConfig = endpointConfig;
    this.rawKafkaTemplate = rawKafkaTemplate;
    this.avroKafkaTemplate = avroKafkaTemplate;
  }

  public DeferredResult<ResponseEntity<Map<String, Object>>> publishEvent(IngestEvent event) {
    Optional<Endpoint> endpoint = endpointConfig.getEndpointById(event.getEndpointId());

    log.debug(
        "processing request id = '{}', eventId = '{}', payload = '{}'",
        event.getId(),
        event.getEndpointId(),
        event.getPayload());

    if (endpoint.isPresent()) {
      return processEvent(event, endpoint.get());
    } else {
      log.warn("Endpoint with endpointId = '{}' does not exist.", event.getEndpointId());

      DeferredResult<ResponseEntity<Map<String, Object>>> result = new DeferredResult<>();
      result.setErrorResult(
          createErrorResponse(
              event,
              HttpStatus.NOT_FOUND,
              String.format(
                  "Endpoint with endpointId = '%s' does not exist.", event.getEndpointId())));

      return result;
    }
  }

  private DeferredResult<ResponseEntity<Map<String, Object>>> processEvent(
      IngestEvent event, Endpoint endpoint) {
    try {
      validate(event.getPayload());

      ListenableFuture<SendResult<Object, Object>> future = sendToKafka(event, endpoint);

      final DeferredResult<ResponseEntity<Map<String, Object>>> response = new DeferredResult<>();

      if (endpoint.isBlocking()) {
        future.addCallback(new DeferredResultCallback(event, response));
      } else {
        future.addCallback(new LoggingCallback(event));
        response.setResult(createResponse(event, HttpStatus.ACCEPTED));
      }

      return response;

    } catch (final JsonProcessingException ex) {
      log.warn(
          "Payload is not valid JSON for event = '{}' with payload = '{}'",
          event,
          event.getPayload(),
          ex);

      DeferredResult<ResponseEntity<Map<String, Object>>> errorResult = new DeferredResult<>();
      errorResult.setErrorResult(
          createErrorResponse(event, HttpStatus.BAD_REQUEST, getCauseAsString(ex)));
      return errorResult;
    } catch (AvroConversionException ex) {
      log.warn(
          "Failed to convert JSON to Avro for event = '{}' with payload = '{}'",
          event,
          event.getPayload(),
          ex);

      DeferredResult<ResponseEntity<Map<String, Object>>> errorResult = new DeferredResult<>();
      errorResult.setErrorResult(
          createErrorResponse(event, HttpStatus.BAD_REQUEST, getCauseAsString(ex)));
      return errorResult;
    } catch (SerializationException ex) {
      log.warn(
          "Failed to serialize Avro for event = '{}' with payload = '{}'",
          event,
          event.getPayload(),
          ex.getCause());

      DeferredResult<ResponseEntity<Map<String, Object>>> errorResult = new DeferredResult<>();

      errorResult.setErrorResult(
          createErrorResponse(event, HttpStatus.BAD_REQUEST, getCauseAsString(ex)));
      return errorResult;
    }
  }

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

  private ListenableFuture<SendResult<Object, Object>> sendToKafka(
      IngestEvent event, Endpoint endpoint) {
    if (endpoint.hasSchema()) {
      GenericData.Record record =
          converter.convertToGenericDataRecord(
              event.payloadAsBytes(), endpoint.getSchema().getAvro());
      return avroKafkaTemplate.send(endpoint.getTopic(), event.getId(), record);
    } else {
      return rawKafkaTemplate.send(endpoint.getTopic(), event.getId(), event.getPayload());
    }
  }
}
