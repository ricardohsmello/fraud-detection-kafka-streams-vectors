package com.devnexus.frauddetection.infrastructure.message.config;

import com.devnexus.frauddetection.domain.Transaction;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTemplateConfig {

	@Bean
	@SuppressWarnings("unchecked")
	public KafkaTemplate<String, Transaction> transactionKafkaTemplate(ProducerFactory<?, ?> producerFactory) {
		return new KafkaTemplate<>((ProducerFactory<String, Transaction>) producerFactory);
	}

	@Bean
	public KafkaTemplate<String, byte[]> dlqKafkaTemplate(KafkaProperties kafkaProperties) {
		Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

		ProducerFactory<String, byte[]> factory = new DefaultKafkaProducerFactory<>(props);
		return new KafkaTemplate<>(factory);
	}
}
