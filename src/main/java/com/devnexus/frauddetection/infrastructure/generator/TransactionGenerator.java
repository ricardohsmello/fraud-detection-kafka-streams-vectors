package com.devnexus.frauddetection.infrastructure.generator;

import com.devnexus.frauddetection.domain.model.Transaction;
import com.devnexus.frauddetection.infrastructure.message.producer.TransactionProducer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.generator.enabled", havingValue = "true")
public class TransactionGenerator {

    private static final Logger log = LoggerFactory.getLogger(TransactionGenerator.class);

    private static final List<CustomerProfile> CUSTOMERS = List.of(
            new CustomerProfile("USR-1001", "4111-1111-1111-1111", 24.50, 6.00),
            new CustomerProfile("USR-2002", "5555-2222-3333-4444", 120.00, 35.00),
            new CustomerProfile("USR-3003", "3782-822463-10005", 9000.00, 2500.00)
    );

    private static final List<MerchantLocation> MERCHANTS = List.of(
            new MerchantLocation("Starbucks", "Seattle", 47.6101, -122.2015),
            new MerchantLocation("Whole Foods", "Austin", 30.2672, -97.7431),
            new MerchantLocation("Apple Store", "San Francisco", 37.7858, -122.4064),
            new MerchantLocation("Target", "Chicago", 41.8781, -87.6298),
            new MerchantLocation("Tesco", "London", 51.5072, -0.1276),
            new MerchantLocation("Yodobashi Camera", "Tokyo", 35.6762, 139.6503)
    );

    private final TransactionProducer producer;
    private final Random random = new Random();
    private final long intervalMs;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "transaction-generator");
                thread.setDaemon(true);
                return thread;
            });

    public TransactionGenerator(TransactionProducer producer,
                                @Value("${app.generator.interval-ms:1000}") long intervalMs) {
        this.producer = producer;
        this.intervalMs = intervalMs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        scheduler.scheduleAtFixedRate(this::safeGenerate, 0, intervalMs, TimeUnit.MILLISECONDS);
        log.info("Transaction generator started with interval {} ms", intervalMs);
    }

    private void safeGenerate() {
        try {
            generateTransaction();
        } catch (Exception e) {
            log.warn("Transaction generator failed to create/send a transaction", e);
        }
    }

    private void generateTransaction() {
        CustomerProfile customer = CUSTOMERS.get(random.nextInt(CUSTOMERS.size()));
        MerchantLocation merchant = MERCHANTS.get(random.nextInt(MERCHANTS.size()));

        BigDecimal amount = generateAmount(customer);

        Transaction tx = new Transaction(
                "TXN-" + UUID.randomUUID(),
                customer.userId(),
                merchant.merchant(),
                merchant.city(),
                amount,
                Instant.now(),
                customer.cardNumber(),
                merchant.latitude(),
                merchant.longitude()
        );

        log.debug("Generated txId={} userId={} merchant={} amount={}",
                tx.transactionId(), tx.userId(), tx.merchant(), tx.transactionAmount());

        producer.send(tx);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    private BigDecimal generateAmount(CustomerProfile customer) {
        double raw = customer.mean() + customer.stdDev() * random.nextGaussian();
        double normalized = raw < 1.0 ? 1.0 : raw;
        return BigDecimal.valueOf(normalized).setScale(2, RoundingMode.HALF_UP);
    }

    private record CustomerProfile(String userId, String cardNumber, double mean, double stdDev) {}

    private record MerchantLocation(String merchant, String city, double latitude, double longitude) {}
}
