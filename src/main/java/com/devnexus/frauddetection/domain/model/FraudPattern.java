package com.devnexus.frauddetection.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Vector;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("fraud_transactions_seeded")
public record FraudPattern(
        @Id String id,
        boolean seeded,
        Transaction transaction,
        String ruleId,
        String description,
        Instant detectedAt,
        Vector embedding
) {}
