package com.devnexus.frauddetection.infrastructure.message.consumer;

import com.devnexus.frauddetection.domain.events.SuspiciousTransactionEvent;
import com.devnexus.frauddetection.domain.model.SuspiciousTransaction;
import com.devnexus.frauddetection.infrastructure.repository.SuspiciousTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SuspiciousTransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(SuspiciousTransactionConsumer.class);
    private final SuspiciousTransactionRepository repo;

    public SuspiciousTransactionConsumer(SuspiciousTransactionRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.suspicious}",
            groupId = "transaction-suspicious-consumer",
            properties = {
                    "auto.offset.reset=earliest",
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
                    "spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.devnexus.frauddetection.domain.events.SuspiciousTransactionEvent",
                    "spring.json.trusted.packages=com.devnexus.frauddetection.domain.events,com.devnexus.frauddetection.domain.model,com.devnexus.frauddetection.domain"
            }
    )
    public void onMessage(SuspiciousTransactionEvent evt) {
        if (evt == null || evt.transaction() == null) {
            log.warn(">>> Suspicious event: <null>");
            return;
        }

        var tx = evt.transaction();

        log.warn(">>> SUSPICIOUS (RULE): txId={}, userId={}, ruleId={}, desc={}",
                tx.transactionId(),
                tx.userId(),
                evt.ruleId(),
                evt.description()
        );

        SuspiciousTransaction doc = new SuspiciousTransaction(
                null,
                tx,
                SuspiciousTransaction.DetectionType.RULE,
                evt.ruleId(),
                evt.description(),
                null,
                null,
                null,
                evt.detectedAt() != null ? evt.detectedAt() : java.time.Instant.now()
        );

        repo.save(doc);
    }

}
