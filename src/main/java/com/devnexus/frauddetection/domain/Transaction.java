package com.devnexus.frauddetection.domain;

import java.math.BigDecimal;

public record Transaction(
		String transactionId,
		String userId,
		String merchant,
		String city,
		BigDecimal transactionAmount,
		String transactionTime,
		String cardNumber,
		Double latitude,
		Double longitude
) {}


