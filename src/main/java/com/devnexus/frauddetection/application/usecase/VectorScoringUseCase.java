package com.devnexus.frauddetection.application.usecase;

import com.devnexus.frauddetection.domain.model.ScoringResult;
import com.devnexus.frauddetection.domain.model.Transaction;
import com.devnexus.frauddetection.domain.port.FraudPatternSearchPort;
import com.devnexus.frauddetection.domain.port.TransactionEmbedderPort;
import com.devnexus.frauddetection.domain.port.TransactionPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorScoringUseCase {

    private static final Logger logger = LoggerFactory.getLogger(VectorScoringUseCase.class);

    private final TransactionEmbedderPort embedder;
    private final FraudPatternSearchPort fraudPatternSearch;
    private final TransactionPersistencePort persistence;
    private final double similarityThreshold;

    public VectorScoringUseCase(
            TransactionEmbedderPort embedder,
            FraudPatternSearchPort fraudPatternSearch,
            TransactionPersistencePort persistence,
            double similarityThreshold
    ) {
        this.embedder = embedder;
        this.fraudPatternSearch = fraudPatternSearch;
        this.persistence = persistence;
        this.similarityThreshold = similarityThreshold;
    }

    public void score(Transaction transaction) {
        float[] embedding = embedder.embed(transaction);

        ScoringResult result = fraudPatternSearch.searchSimilarPatterns(embedding, similarityThreshold);

        if (result.matchFound()) {
            persistence.saveSuspiciousFromVector(transaction, result, similarityThreshold);

            logger.warn(">>> VECTOR FLAGGED: txId={}, topScore={}, threshold={}, matches={}",
                    transaction.transactionId(), result.topScore(), similarityThreshold, result.matches().size());
        } else {
            persistence.saveApproved(transaction);

            logger.info(">>> VECTOR APPROVED: txId={}, topScore={}, threshold={}",
                    transaction.transactionId(), result.topScore(), similarityThreshold);
        }
    }
}
