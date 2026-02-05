package com.devnexus.frauddetection.infrastructure.message.producer;

import com.devnexus.frauddetection.infrastructure.message.support.KafkaHeaders;
import com.devnexus.frauddetection.infrastructure.message.config.TopicsProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DlqProducer {

    private static final Logger log = LoggerFactory.getLogger(DlqProducer.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final TopicsProperties topics;

    public DlqProducer(KafkaTemplate<String, byte[]> kafkaTemplate, TopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    public void send(String sourceTopic, byte[] payload, Exception exception) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(
            topics.dlq(),
            null,
            null,
            null,
            payload,
            KafkaHeaders.forDlq(sourceTopic, exception)
        );

        kafkaTemplate.send(record)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send message to DLQ topic={}", topics.dlq(), ex);
                } else {
                    log.info("Message sent to DLQ topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
    }
}