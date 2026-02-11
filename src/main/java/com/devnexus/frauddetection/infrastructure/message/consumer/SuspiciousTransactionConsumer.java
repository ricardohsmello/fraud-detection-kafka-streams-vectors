package com.devnexus.frauddetection.infrastructure.message.consumer;

import com.devnexus.frauddetection.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SuspiciousTransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(SuspiciousTransactionConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.to-score}",
            groupId = "fraud-detection-to-score-consumer",
            properties = {
                    "auto.offset.reset=earliest",
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
                    "spring.json.value.default.type=com.devnexus.frauddetection.domain.Transaction",
                    "spring.json.trusted.packages=com.devnexus.frauddetection.domain"
            }
    )
    public void onMessage(Transaction tx) {
        if (tx == null) {
            log.info(">>> TO SCORE: <null tx>");
            return;
        }

        log.info(">>> TO SCORE: txId={}, userId={}, amount={}, city={}, time={}",
                tx.transactionId(),
                tx.userId(),
                tx.transactionAmount(),
                tx.city(),
                tx.transactionTime()
        );
    }
}
