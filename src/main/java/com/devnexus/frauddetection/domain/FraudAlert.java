 package com.devnexus.frauddetection.domain;

import java.time.Instant;

public record FraudAlert(
		Transaction transaction,
		String ruleId,
		String description,
		String detectedAt
) {
	public static FraudAlert of(Transaction transaction, String ruleId, String description) {
		return new FraudAlert(transaction, ruleId, description, Instant.now().toString());
	}
}
