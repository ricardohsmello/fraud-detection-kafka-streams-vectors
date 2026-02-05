package com.devnexus.frauddetection.domain.rules;

import com.devnexus.frauddetection.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ImpossibleTravelValidatorTest {

    private ImpossibleTravelValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ImpossibleTravelValidator();
    }

    @Test
    void shouldDetectImpossibleTravel_SaoPauloToNewYork_In1Hour() {
        Transaction saoPaulo = new Transaction(
                "tx1", new BigDecimal("100.00"),
                "2024-01-15T10:00:00Z", "1234-5678-9012-3456",
                -23.5505, -46.6333
        );

        Transaction newYork = new Transaction(
                "tx2", new BigDecimal("200.00"),
                "2024-01-15T11:00:00Z", "1234-5678-9012-3456",
                40.7128, -74.0060
        );

        assertTrue(validator.isImpossibleTravel(saoPaulo, newYork));
    }

    @Test
    void shouldAllowTravel_SaoPauloToNewYork_In12Hours() {
        Transaction saoPaulo = new Transaction(
                "tx1", new BigDecimal("100.00"),
                "2024-01-15T06:00:00Z", "1234-5678-9012-3456",
                -23.5505, -46.6333
        );

        Transaction newYork = new Transaction(
                "tx2", new BigDecimal("200.00"),
                "2024-01-15T18:00:00Z", "1234-5678-9012-3456",
                40.7128, -74.0060
        );

        assertFalse(validator.isImpossibleTravel(saoPaulo, newYork));
    }

    @Test
    void shouldAllowTravel_SameCity_ShortTime() {
        Transaction downtown = new Transaction(
                "tx1", new BigDecimal("50.00"),
                "2024-01-15T10:00:00Z", "1234-5678-9012-3456",
                -23.5489, -46.6388
        );

        Transaction paulista = new Transaction(
                "tx2", new BigDecimal("75.00"),
                "2024-01-15T10:30:00Z", "1234-5678-9012-3456",
                -23.5632, -46.6542
        );

        assertFalse(validator.isImpossibleTravel(downtown, paulista));
    }

    @Test
    void shouldDetectImpossibleTravel_SameTimeDistantLocations() {
        Transaction saoPaulo = new Transaction(
                "tx1", new BigDecimal("100.00"),
                "2024-01-15T10:00:00Z", "1234-5678-9012-3456",
                -23.5505, -46.6333
        );

        Transaction rioDeJaneiro = new Transaction(
                "tx2", new BigDecimal("200.00"),
                "2024-01-15T10:00:00Z", "1234-5678-9012-3456",
                -22.9068, -43.1729
        );

        assertTrue(validator.isImpossibleTravel(saoPaulo, rioDeJaneiro));
    }

    @Test
    void shouldReturnFalse_WhenCoordinatesAreMissing() {
        Transaction withCoords = new Transaction(
                "tx1", new BigDecimal("100.00"),
                "2024-01-15T10:00:00Z", "1234-5678-9012-3456",
                -23.5505, -46.6333
        );

        Transaction withoutCoords = new Transaction(
                "tx2", new BigDecimal("200.00"),
                "2024-01-15T11:00:00Z", "1234-5678-9012-3456",
                null, null
        );

        assertFalse(validator.isImpossibleTravel(withCoords, withoutCoords));
    }

    @Test
    void shouldAllowTravel_SaoPauloToRio_In1Hour() {

        Transaction saoPaulo = new Transaction(
                "tx1", new BigDecimal("100.00"),
                "2024-01-15T10:00:00Z", "1234-5678-9012-3456",
                -23.5505, -46.6333
        );

        Transaction rio = new Transaction(
                "tx2", new BigDecimal("200.00"),
                "2024-01-15T11:00:00Z", "1234-5678-9012-3456",
                -22.9068, -43.1729
        );

        assertFalse(validator.isImpossibleTravel(saoPaulo, rio),
                "360 km in 1 hour (360 km/h) should be possible");
    }

    @Test
    void shouldDetectImpossibleTravel_SaoPauloToRio_In10Minutes() {

        Transaction saoPaulo = new Transaction(
                "tx1", new BigDecimal("100.00"),
                "2024-01-15T10:00:00Z", "1234-5678-9012-3456",
                -23.5505, -46.6333
        );

        Transaction rio = new Transaction(
                "tx2", new BigDecimal("200.00"),
                "2024-01-15T10:10:00Z", "1234-5678-9012-3456",
                -22.9068, -43.1729
        );

        assertTrue(validator.isImpossibleTravel(saoPaulo, rio),
                "360 km in 10 minutes (2160 km/h) should be impossible");
    }
}