package ch.yax.kafka.ingest.kafka.callback;

import static ch.yax.kafka.ingest.kafka.ResponseUtil.createErrorResponse;
import static ch.yax.kafka.ingest.kafka.ResponseUtil.createResponse;

import ch.yax.kafka.ingest.kafka.IngestEvent;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.request.async.DeferredResult;;

@Slf4j
public class DeferredResultCallback
    implements ListenableFutureCallback<SendResult<Object, Object>> {

  private final IngestEvent event;
  private final DeferredResult<ResponseEntity<Map<String, Object>>> response;

  public DeferredResultCallback(
      IngestEvent event, DeferredResult<ResponseEntity<Map<String, Object>>> response) {
    this.event = event;
    this.response = response;
  }

  @Override
  public void onFailure(Throwable throwable) {
    log.error("Unable to send message (event = '{}')", event, throwable);
    response.setErrorResult(
        createErrorResponse(event, HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage()));
  }

  @Override
  public void onSuccess(SendResult<Object, Object> result) {
    log.debug(
        "Message sent to topic = '{}' with offset = '{}' and partition = '{}.  (event = '{}')'",
        result.getRecordMetadata().topic(),
        result.getRecordMetadata().offset(),
        result.getRecordMetadata().partition(),
        event);
    response.setResult(createResponse(event, HttpStatus.OK));
  }
}
