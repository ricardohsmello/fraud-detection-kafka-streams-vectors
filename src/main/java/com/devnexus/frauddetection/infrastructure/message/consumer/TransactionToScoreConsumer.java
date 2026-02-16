package com.devnexus.frauddetection.infrastructure.message.consumer;

import com.devnexus.frauddetection.domain.model.ApprovedTransaction;
import com.devnexus.frauddetection.domain.model.FraudPattern;
import com.devnexus.frauddetection.domain.model.SuspiciousTransaction;
import com.devnexus.frauddetection.domain.model.Transaction;
import com.devnexus.frauddetection.infrastructure.embedding.config.VectorFraudProperties;
import com.devnexus.frauddetection.infrastructure.embedding.TransactionEmbedder;
import com.devnexus.frauddetection.infrastructure.repository.ApprovedTransactionRepository;
import com.devnexus.frauddetection.infrastructure.repository.FraudPatternsRepository;
import com.devnexus.frauddetection.infrastructure.repository.SuspiciousTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class TransactionToScoreConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionToScoreConsumer.class);

    private final TransactionEmbedder embedder;
    private final FraudPatternsRepository fraudPatternRepository;
    private final VectorFraudProperties props;
    private final SuspiciousTransactionRepository suspiciousRepo;
    private final ApprovedTransactionRepository approvedRepo;

    public TransactionToScoreConsumer(
            TransactionEmbedder embedder,
            FraudPatternsRepository fraudPatternRepository,
            VectorFraudProperties props,
            SuspiciousTransactionRepository suspiciousRepo,
            ApprovedTransactionRepository approvedRepo
    ) {
        this.embedder = embedder;
        this.fraudPatternRepository = fraudPatternRepository;
        this.props = props;
        this.suspiciousRepo = suspiciousRepo;
        this.approvedRepo = approvedRepo;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.to-score}",
            groupId = "fraud-detection-to-score-consumer",
            properties = {
                    "auto.offset.reset=earliest",
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
                    "spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.devnexus.frauddetection.domain.model.Transaction",
                    "spring.json.trusted.packages=com.devnexus.frauddetection.domain.model"
            }
    )
    public void onMessage(Transaction tx) {
        if (tx == null) {
            log.info(">>> TO SCORE: <null tx>");
            return;
        }

        Vector vector = embedder.embed(tx);
        log.info(">>> TO SCORE: tx={}, embedding size={}", tx, vector.size());

        SearchResults<FraudPattern> results =
                fraudPatternRepository.searchTopFraudPatternsByEmbeddingNear(
                        vector,
                        Score.of(props.similarityThreshold())
                );

        List<SearchResult<FraudPattern>> content = results.getContent();

        if (content.isEmpty()) {
            approvedRepo.save(new ApprovedTransaction(null, tx, Instant.now()));
            log.info(">>> VECTOR APPROVED: txId={}, no similar patterns found",
                    tx.transactionId());
            return;
        }

        // safe now
        double topScore = content.get(0).getScore().getValue();

        long fraudCount = content.stream()
                .filter(r -> r.getContent().fraud())
                .count();

        long notFraudCount = content.size() - fraudCount;

        boolean classifiedFraud = fraudCount >= notFraudCount;

        if (classifiedFraud) {
            List<SuspiciousTransaction.VectorMatch> matches = content.stream()
                    .map(r -> new SuspiciousTransaction.VectorMatch(
                            r.getContent().id(),
                            r.getScore().getValue()
                    ))
                    .toList();

            SuspiciousTransaction doc = new SuspiciousTransaction(
                    null,
                    tx,
                    SuspiciousTransaction.DetectionType.VECTOR,
                    null,
                    "Similar to known fraud patterns",
                    topScore,
                    props.similarityThreshold(),
                    matches,
                    Instant.now()
            );

            suspiciousRepo.save(doc);

            log.warn(">>> VECTOR FLAGGED: txId={}, topScore={}, threshold={}, matches={}, fraudVotes={}, safeVotes={}",
                    tx.transactionId(),
                    topScore,
                    props.similarityThreshold(),
                    matches.size(),
                    fraudCount,
                    notFraudCount);
        } else {
            approvedRepo.save(new ApprovedTransaction(null, tx, Instant.now()));

            log.info(">>> VECTOR APPROVED: txId={}, fraudVotes={}, safeVotes={}, threshold={}",
                    tx.transactionId(),
                    fraudCount,
                    notFraudCount,
                    props.similarityThreshold());
        }
    }
}
