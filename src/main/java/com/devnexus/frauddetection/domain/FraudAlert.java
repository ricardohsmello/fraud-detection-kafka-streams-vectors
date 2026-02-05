 package com.devnexus.frauddetection.domain;

public record FraudAlert(
		Transaction transaction,
		String ruleId,
		String description,
		String detectedAt
) {}
