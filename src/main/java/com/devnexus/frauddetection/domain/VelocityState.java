package com.devnexus.frauddetection.domain;

public record VelocityState(
    long count,
    Transaction lastTransaction
) {
    public static VelocityState empty() {
        return new VelocityState(0, null);
    }

    public VelocityState increment(Transaction transaction) {
        return new VelocityState(count + 1, transaction);
    }
}