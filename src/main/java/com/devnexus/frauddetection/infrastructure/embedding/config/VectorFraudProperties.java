package com.devnexus.frauddetection.infrastructure.embedding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fraud.vector")
public record VectorFraudProperties(
        int topK,
        int numCandidates,
        double similarityThreshold
) {}
