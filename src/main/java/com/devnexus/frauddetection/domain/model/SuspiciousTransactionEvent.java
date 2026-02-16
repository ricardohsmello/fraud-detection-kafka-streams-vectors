package com.devnexus.frauddetection.domain.model;

import java.time.Instant;

public record SuspiciousTransactionEvent(
        Transaction transaction,
        String ruleId,
        String description,
        Instant detectedAt
) {}
