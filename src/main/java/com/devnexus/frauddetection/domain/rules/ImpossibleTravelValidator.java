package com.devnexus.frauddetection.domain.rules;

import com.devnexus.frauddetection.domain.FraudAlert;
import com.devnexus.frauddetection.domain.Transaction;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class ImpossibleTravelValidator {

    private static final String RULE_ID = "IMPOSSIBLE_TRAVEL";
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MAX_TRAVEL_SPEED_KMH = 900.0;

    public Optional<FraudAlert> validate(Transaction previous, Transaction current) {
        if (!hasValidCoordinates(previous) || !hasValidCoordinates(current)) {
            return Optional.empty();
        }

        double distanceKm = calculateDistanceKm(
                previous.latitude(), previous.longitude(),
                current.latitude(), current.longitude()
        );

        Duration timeBetween = calculateTimeBetween(
                previous.transactionTime(),
                current.transactionTime()
        );

        double hoursElapsed = timeBetween.toMinutes() / 60.0;
        boolean isImpossible;

        if (hoursElapsed <= 0) {
            isImpossible = distanceKm > 1.0;
        } else {
            double requiredSpeedKmh = distanceKm / hoursElapsed;
            isImpossible = requiredSpeedKmh > MAX_TRAVEL_SPEED_KMH;
        }

        if (!isImpossible) {
            return Optional.empty();
        }

        String description = String.format(
                "%s to %s (%.0f km in %d min)",
                previous.city(),
                current.city(),
                distanceKm,
                timeBetween.toMinutes()
        );

        return Optional.of(new FraudAlert(
                current,
                RULE_ID,
                description,
                Instant.now().toString()
        ));
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private boolean hasValidCoordinates(Transaction tx) {
        return tx.latitude() != null && tx.longitude() != null;
    }

    private Duration calculateTimeBetween(String time1, String time2) {
        Instant instant1 = Instant.parse(time1);
        Instant instant2 = Instant.parse(time2);
        return Duration.between(instant1, instant2).abs();
    }
}