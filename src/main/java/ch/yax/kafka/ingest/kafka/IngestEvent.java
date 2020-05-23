package ch.yax.kafka.ingest.kafka;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
public class IngestEvent {
  private String endpointId;
  @Builder.Default private String id = generateId();
  @ToString.Exclude private String payload;

  private static String generateId() {
    UUID uuid = UUID.randomUUID();
    return uuid.toString();
  }

  public byte[] payloadAsBytes() {
    if (payload != null) {
      return payload.getBytes();
    } else {
      return new byte[0];
    }
  }
}
