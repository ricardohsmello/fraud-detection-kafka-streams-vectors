package com.devnexus.frauddetection.domain.port;

import com.devnexus.frauddetection.domain.model.ScoringResult;

public interface FraudPatternSearchPort {
    ScoringResult searchSimilarPatterns(float[] embedding, double threshold);
}
