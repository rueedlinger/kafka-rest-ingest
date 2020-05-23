package ch.yax.kafka.ingest.kafka.callback;

import ch.yax.kafka.ingest.kafka.IngestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Slf4j
public class LoggingCallback implements ListenableFutureCallback<SendResult<Object, Object>> {

  private final IngestEvent event;

  public LoggingCallback(IngestEvent event) {
    this.event = event;
  }

  @Override
  public void onFailure(Throwable throwable) {
    log.error("Unable to send message (event = '{}')", event, throwable);
  }

  @Override
  public void onSuccess(SendResult<Object, Object> result) {
    log.debug(
        "Message sent to topic = '{}' with offset = '{}' and partition = '{}'. (event = '{}')",
        result.getRecordMetadata().topic(),
        result.getRecordMetadata().offset(),
        result.getRecordMetadata().partition(),
        event);
  }
}
