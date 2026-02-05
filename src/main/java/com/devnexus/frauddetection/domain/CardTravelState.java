package com.devnexus.frauddetection.domain;

public record CardTravelState(
    Transaction lastTransaction,
    FraudAlert fraudAlert
) {
    public static CardTravelState empty() {
        return new CardTravelState(null, null);
    }

    public boolean hasFraudAlert() {
        return fraudAlert != null;
    }
}