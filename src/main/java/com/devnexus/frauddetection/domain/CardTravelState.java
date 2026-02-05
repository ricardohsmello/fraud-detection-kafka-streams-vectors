package com.devnexus.frauddetection.domain;

public record CardTravelState(
    Transaction lastTransaction,
    boolean impossibleTravelDetected
) {
    public static CardTravelState empty() {
        return new CardTravelState(null, false);
    }
}