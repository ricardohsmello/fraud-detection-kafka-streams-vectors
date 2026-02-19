package com.devnexus.frauddetection.infrastructure.database;

import com.devnexus.frauddetection.domain.model.ScoringResult;
import com.devnexus.frauddetection.domain.model.VectorMatch;
import com.devnexus.frauddetection.domain.port.FraudPatternSearchPort;
import com.devnexus.frauddetection.infrastructure.database.document.FraudPattern;
import com.devnexus.frauddetection.infrastructure.database.repository.FraudPatternRepository;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MongoFraudPatternSearchAdapter implements FraudPatternSearchPort {

    private final FraudPatternRepository fraudPatternRepository;

    public MongoFraudPatternSearchAdapter(FraudPatternRepository fraudPatternRepository) {
        this.fraudPatternRepository = fraudPatternRepository;
    }

    @Override
    public ScoringResult searchSimilarPatterns(Vector embedding, double threshold) {

        SearchResults<FraudPattern> results =
                fraudPatternRepository.searchTopFraudPatternsByEmbeddingNear(
                        embedding,
                        Score.of(threshold)
                );

        List<SearchResult<FraudPattern>> content = results.getContent();

        if (content.isEmpty()) {
            return ScoringResult.noMatch();
        }

        double topScore = content.getFirst().getScore().getValue();

        List<VectorMatch> matches = content.stream()
                .map(r -> new VectorMatch(r.getContent().id(), r.getScore().getValue()))
                .toList();

        return ScoringResult.withMatches(topScore, matches);
    }
}
