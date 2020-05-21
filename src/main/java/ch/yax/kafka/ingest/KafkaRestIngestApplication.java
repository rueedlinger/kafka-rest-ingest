package ch.yax.kafka.ingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaRestIngestApplication {

  public static void main(final String[] args) {
    SpringApplication.run(KafkaRestIngestApplication.class, args);
  }

}
