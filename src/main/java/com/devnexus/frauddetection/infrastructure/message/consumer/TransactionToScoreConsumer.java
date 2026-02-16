package com.devnexus.frauddetection.infrastructure.message.consumer;

import com.devnexus.frauddetection.application.usecase.VectorScoringUseCase;
import com.devnexus.frauddetection.domain.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionToScoreConsumer  {

    private static final Logger log = LoggerFactory.getLogger(TransactionToScoreConsumer.class);

    private final VectorScoringUseCase vectorScoringUseCase;

    public TransactionToScoreConsumer(VectorScoringUseCase vectorScoringUseCase) {
        this.vectorScoringUseCase = vectorScoringUseCase;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.to-score}",
            groupId = "fraud-detection-to-score-consumer"
    )
    public void onMessage(Transaction tx) {
        if (tx == null) {
            log.info(">>> TO SCORE: <null tx>");
            return;
        }

        vectorScoringUseCase.score(tx);
    }
}
