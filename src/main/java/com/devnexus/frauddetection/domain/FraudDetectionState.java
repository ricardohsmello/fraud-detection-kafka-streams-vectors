package com.devnexus.frauddetection.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record FraudDetectionState(
    Transaction lastTransaction,
    List<Instant> recentTransactionTimes,
    FraudAlert fraudAlert
) {
    private static final int MAX_RECENT_TRANSACTIONS = 100;

    public static FraudDetectionState empty() {
        return new FraudDetectionState(null, new ArrayList<>(), null);
    }

    public FraudDetectionState withTransaction(Transaction transaction, FraudAlert alert) {
        List<Instant> updatedTimes = new ArrayList<>(recentTransactionTimes);
        updatedTimes.add(transaction.transactionTime());

        if (updatedTimes.size() > MAX_RECENT_TRANSACTIONS) {
            updatedTimes = updatedTimes.subList(
                updatedTimes.size() - MAX_RECENT_TRANSACTIONS,
                updatedTimes.size()
            );
        }

        return new FraudDetectionState(transaction, updatedTimes, alert);
    }

    public long countTransactionsInWindow(long windowMinutes) {
        if (lastTransaction == null || lastTransaction.transactionTime() == null) {
            return 0;
        }

        Instant currentTime = lastTransaction.transactionTime();
        Instant windowStart = currentTime.minusSeconds(windowMinutes * 60);

        return recentTransactionTimes.stream()
            .filter(Objects::nonNull)
            .filter(time -> time.isAfter(windowStart))
            .count();
    }

    public boolean hasFraudAlert() {
        return fraudAlert != null;
    }
}