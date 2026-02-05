package com.devnexus.frauddetection.infrastructure.streams.serde;

import com.devnexus.frauddetection.infrastructure.message.producer.DlqProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonSerde {
	private static final Logger log = LoggerFactory.getLogger(JsonSerde.class);

	private final ObjectMapper objectMapper = new ObjectMapper();
 	private final DlqProducer dlqProducer;

	public JsonSerde(DlqProducer dlqProducer) {
		this.dlqProducer = dlqProducer;
	}

	public <T> Serde<T> forClass(Class<T> clazz) {
		Serializer<T> serializer = (topic, data) -> {
			try {
				return objectMapper.writeValueAsBytes(data);
			} catch (Exception e) {
				throw new RuntimeException("Error serializing", e);
			}
		};

		Deserializer<T> deserializer = (topic, data) -> {
			try {
				return data == null ? null : objectMapper.readValue(data, clazz);
			} catch (Exception e) {
				log.error("Error deserializing from topic={}", topic, e);
				dlqProducer.send(topic, data, e);
				return null;
			}
		};

		return Serdes.serdeFrom(serializer, deserializer);
	}
}
