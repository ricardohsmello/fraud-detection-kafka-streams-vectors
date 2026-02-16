package com.devnexus.frauddetection.infrastructure.message.consumer;

import com.devnexus.frauddetection.application.usecase.SaveSuspiciousTransactionUseCase;
import com.devnexus.frauddetection.domain.model.SuspiciousAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SuspiciousTransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SuspiciousTransactionConsumer.class);

    private final SaveSuspiciousTransactionUseCase saveSuspiciousTransactionUseCase;

    public SuspiciousTransactionConsumer(SaveSuspiciousTransactionUseCase saveSuspiciousTransactionUseCase) {
        this.saveSuspiciousTransactionUseCase = saveSuspiciousTransactionUseCase;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.suspicious}",
            groupId = "${app.kafka.consumers.suspicious.group-id}",
            properties = {
                    "spring.json.value.default.type=${app.kafka.consumers.suspicious.value-type}"
            }
    )
    public void onMessage(SuspiciousAlert evt) {
        saveSuspiciousTransactionUseCase.execute(evt);
    }
}
