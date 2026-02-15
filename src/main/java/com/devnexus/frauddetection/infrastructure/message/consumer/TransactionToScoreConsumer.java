package com.devnexus.frauddetection.infrastructure.message.consumer;

import com.devnexus.frauddetection.domain.model.ApprovedTransaction;
import com.devnexus.frauddetection.domain.model.FraudPattern;
import com.devnexus.frauddetection.domain.model.SuspiciousTransaction;
import com.devnexus.frauddetection.domain.model.Transaction;
import com.devnexus.frauddetection.infrastructure.embedding.voyage.EmbeddingService;
import com.devnexus.frauddetection.infrastructure.repository.ApprovedTransactionRepository;
import com.devnexus.frauddetection.infrastructure.repository.FraudPatternRepository;
import com.devnexus.frauddetection.infrastructure.repository.SuspiciousTransactionRepository;
import com.devnexus.frauddetection.infrastructure.streams.config.VectorFraudProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class TransactionToScoreConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionToScoreConsumer.class);

    private final EmbeddingService embeddingService;
    private final FraudPatternRepository fraudPatternRepository;
    private final VectorFraudProperties props;
    private final SuspiciousTransactionRepository suspiciousRepo;
    private final ApprovedTransactionRepository approvedRepo;

    public TransactionToScoreConsumer(
            EmbeddingService embeddingService,
            FraudPatternRepository fraudPatternRepository,
            VectorFraudProperties props,
            SuspiciousTransactionRepository suspiciousRepo,
            ApprovedTransactionRepository approvedRepo
    ) {
        this.embeddingService = embeddingService;
        this.fraudPatternRepository = fraudPatternRepository;
        this.props = props;
        this.suspiciousRepo = suspiciousRepo;
        this.approvedRepo = approvedRepo;
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

        Vector vector = embeddingService.embed(tx);

        SearchResults<FraudPattern> results =
                fraudPatternRepository.searchTopFraudPatternsByEmbeddingNear(
                        vector,
                        Score.of(props.similarityThreshold())
                );


        List<SearchResult<FraudPattern>> content = results.getContent();

        boolean similar = !content.isEmpty();
        double topScore = similar ? content.getFirst().getScore().getValue() : 0.0;

        if (similar) {
            List<SuspiciousTransaction.VectorMatch> matches = content.stream()
                    .map(r -> new SuspiciousTransaction.VectorMatch(r.getContent().id(), r.getScore().getValue()))
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

            log.warn(">>> VECTOR FLAGGED: txId={}, topScore={}, threshold={}, matches={}",
                    tx.transactionId(), topScore, props.similarityThreshold(), matches.size());
        } else {
            approvedRepo.save(new ApprovedTransaction(null, tx, Instant.now()));
            log.info(">>> VECTOR APPROVED: txId={}, topScore={}, threshold={}",
                    tx.transactionId(), topScore, props.similarityThreshold());

        }
    }
}
