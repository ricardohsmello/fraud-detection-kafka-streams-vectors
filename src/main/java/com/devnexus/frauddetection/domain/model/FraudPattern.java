package com.devnexus.frauddetection.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Vector;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document("fraud_patterns")
public record FraudPattern(
        @Id String id,
        String patternName,
        String merchant,
        String city,
        BigDecimal typicalAmount,
        Vector embedding
) {}
