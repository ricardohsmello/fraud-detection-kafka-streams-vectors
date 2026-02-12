package com.devnexus.frauddetection.domain.events;

import com.devnexus.frauddetection.domain.model.Transaction;

import java.time.Instant;

public record SuspiciousTransactionEvent(
        Transaction transaction,
        String ruleId,
        String description,
        Instant detectedAt
) {}
