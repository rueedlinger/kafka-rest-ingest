package ch.yax.kafka.ingest.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@ToString
@Slf4j
public class KafkaProducerConfig {

  @Value(value = "${kafka.bootstrap.servers}")
  private String bootstrapAddress;

  @Value(value = "${kafka.schema.registry.url}")
  private String schemaRegistryUrl;

  @Value(value = "${kafka.producer.acks:1}")
  private String producerAcks;

  @Value(value = "${kafka.producer.retries:10}")
  private int retries;

  @Value(value = "${kafka.producer.auto.register.schemas:false}")
  private boolean autoRegisterSchemas;

  // TODO: add other config options https://kafka.apache.org/documentation/#producerconfigs

  @PostConstruct
  public void init() {
    log.info("kafka producer config: {}", this);
  }

  @Bean(name = "rawProducerFactory")
  public ProducerFactory<Object, Object> rawProducerFactory() {
    Map<String, Object> configProps = createKafkaProducerDefaultConfig();
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean(name = "rawKafkaTemplate")
  public KafkaTemplate<Object, Object> rawKafkaTemplate(
      @Qualifier("rawProducerFactory") ProducerFactory<Object, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean(name = "avroProducerFactory")
  public ProducerFactory<Object, Object> avroProducerFactory() {
    Map<String, Object> configProps = createKafkaProducerDefaultConfig();
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    configProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    configProps.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, autoRegisterSchemas);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean(name = "avroKafkaTemplate")
  public KafkaTemplate<Object, Object> avroKafkaTemplate(
      @Qualifier("avroProducerFactory") ProducerFactory<Object, Object> avroProducerFactory) {
    return new KafkaTemplate<>(avroProducerFactory);
  }

  private Map<String, Object> createKafkaProducerDefaultConfig() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    configProps.put(ProducerConfig.ACKS_CONFIG, producerAcks);
    configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return configProps;
  }
}
