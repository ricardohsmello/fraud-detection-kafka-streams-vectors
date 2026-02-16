package com.devnexus.frauddetection.infrastructure.repository;

import com.devnexus.frauddetection.domain.model.FraudPattern;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.VectorSearch;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudSeededTransactionRepository extends MongoRepository<FraudPattern, String> {

    @VectorSearch(
            indexName = "fraud_patterns_vector_index",
            filter = "{'transaction.merchant': ?0}",
            limit = "10",
            numCandidates = "200"
    )
    SearchResults<FraudPattern> searchTopFraudPatternsByMerchantAndEmbeddingNear(
            String merchant,
            Vector vector,
            Score score
    );
}

