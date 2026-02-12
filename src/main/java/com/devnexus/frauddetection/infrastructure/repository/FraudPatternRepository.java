package com.devnexus.frauddetection.infrastructure.repository;

import com.devnexus.frauddetection.domain.model.FraudPattern;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.VectorSearch;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudPatternRepository extends MongoRepository<FraudPattern, String> {

    // IMPORTANT: limit and numCandidates must be parseable ints on SD MongoDB 5.0.2
    @VectorSearch(
            indexName = "fraud_patterns_vector_index",
            limit = "10",
            numCandidates = "200"
    )
    SearchResults<FraudPattern> searchTopFraudPatternsByEmbeddingNear(Vector vector, Score score);
}
