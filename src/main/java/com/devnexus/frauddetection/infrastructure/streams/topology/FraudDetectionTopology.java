package com.devnexus.frauddetection.infrastructure.streams.topology;

import com.devnexus.frauddetection.domain.FraudAlert;
import com.devnexus.frauddetection.domain.FraudDetectionState;
import com.devnexus.frauddetection.domain.Transaction;
import com.devnexus.frauddetection.domain.rules.ImpossibleTravelValidator;
import com.devnexus.frauddetection.domain.rules.VelocityCheckValidator;
import com.devnexus.frauddetection.infrastructure.message.config.TopicsProperties;
import com.devnexus.frauddetection.infrastructure.streams.serde.JsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FraudDetectionTopology {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionTopology.class);
    private static final long VELOCITY_WINDOW_MINUTES = 1;

    private final TopicsProperties topics;
    private final JsonSerde jsonSerde;
    private final ImpossibleTravelValidator impossibleTravelValidator;
    private final VelocityCheckValidator velocityCheckValidator;

    public FraudDetectionTopology(
            TopicsProperties topics,
            JsonSerde jsonSerde,
            ImpossibleTravelValidator impossibleTravelValidator,
            VelocityCheckValidator velocityCheckValidator
    ) {
        this.topics = topics;
        this.jsonSerde = jsonSerde;
        this.impossibleTravelValidator = impossibleTravelValidator;
        this.velocityCheckValidator = velocityCheckValidator;
    }

    @Bean
    public KStream<String, Transaction> fraudDetectionStream(StreamsBuilder builder) {
        Serde<Transaction> transactionSerde = jsonSerde.forClass(Transaction.class);
        Serde<FraudDetectionState> stateSerde = jsonSerde.forClass(FraudDetectionState.class);
        Serde<FraudAlert> fraudAlertSerde = jsonSerde.forClass(FraudAlert.class);

        KStream<String, Transaction> stream = builder.stream(
                topics.transactions(),
                Consumed.with(Serdes.String(), transactionSerde)
        );

        KStream<String, FraudDetectionState> processedStream = stream
                .filter((key, tx) -> tx != null)
                .selectKey((key, tx) -> tx.cardNumber())
                .groupByKey(Grouped.with(Serdes.String(), transactionSerde))
                .aggregate(
                        FraudDetectionState::empty,
                        (cardNumber, newTransaction, currentState) -> {
                            FraudAlert alert = null;


                            Transaction previousTx = currentState.lastTransaction();
                            if (previousTx != null && alert == null) {
                                alert = impossibleTravelValidator.validate(previousTx, newTransaction).orElse(null);
                            }

                            if (alert == null) {
                                long count = currentState.countTransactionsInWindow(VELOCITY_WINDOW_MINUTES) + 1;
                                alert = velocityCheckValidator.validate(count, newTransaction).orElse(null);
                            }

                            return currentState.withTransaction(newTransaction, alert);
                        },
                        Materialized.<String, FraudDetectionState, KeyValueStore<Bytes, byte[]>>as("fraud-detection-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(stateSerde)
                )
                .toStream()
                .peek((cardNumber, state) -> log.info(">>> FRAUD CHECK: card={}, hasFraud={}",
                        cardNumber, state.hasFraudAlert()));

        // Branch: fraud detected → suspicious, passed → to-score
        processedStream
                .split(Named.as("fraud-"))
                .branch(
                        (cardNumber, state) -> state.hasFraudAlert(),
                        Branched.withConsumer(fraudStream ->
                                fraudStream
                                        .mapValues(FraudDetectionState::fraudAlert)
                                        .peek((cardNumber, alert) -> log.info(">>> BLOCKED: {} - {}", alert.ruleId(), alert.description()))
                                        .to(topics.suspicious(), Produced.with(Serdes.String(), fraudAlertSerde))
                        )
                )
                .defaultBranch(
                        Branched.withConsumer(passedStream ->
                                passedStream
                                        .mapValues(FraudDetectionState::lastTransaction)
                                        .peek((cardNumber, tx) -> log.info(">>> APPROVED: card={}, txId={}", cardNumber, tx.transactionId()))
                                        .to(topics.toScore(), Produced.with(Serdes.String(), transactionSerde))
                        )
                );

        return stream;
    }
}