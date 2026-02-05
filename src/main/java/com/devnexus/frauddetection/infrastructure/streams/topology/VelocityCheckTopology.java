package com.devnexus.frauddetection.infrastructure.streams.topology;

import com.devnexus.frauddetection.domain.FraudAlert;
import com.devnexus.frauddetection.domain.Transaction;
import com.devnexus.frauddetection.domain.VelocityState;
import com.devnexus.frauddetection.domain.rules.VelocityCheckValidator;
import com.devnexus.frauddetection.infrastructure.message.config.TopicsProperties;
import com.devnexus.frauddetection.infrastructure.streams.serde.JsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class VelocityCheckTopology {

    private static final Logger log = LoggerFactory.getLogger(VelocityCheckTopology.class);

    private static final long MAX_TRANSACTIONS = 3;
    private static final long WINDOW_MINUTES = 1;

    private final TopicsProperties topics;
    private final JsonSerde jsonSerde;
    private final VelocityCheckValidator validator;

    public VelocityCheckTopology(TopicsProperties topics, JsonSerde jsonSerde, VelocityCheckValidator validator) {
        this.topics = topics;
        this.jsonSerde = jsonSerde;
        this.validator = validator;
    }

    @Bean
    public KStream<String, Transaction> velocityCheckStream(StreamsBuilder builder) {
        Serde<Transaction> transactionSerde = jsonSerde.forClass(Transaction.class);
        Serde<VelocityState> velocityStateSerde = jsonSerde.forClass(VelocityState.class);
        Serde<FraudAlert> fraudAlertSerde = jsonSerde.forClass(FraudAlert.class);

        KStream<String, Transaction> stream = builder.stream(
                topics.transactions(),
                Consumed.with(Serdes.String(), transactionSerde)
        );

        stream
                .filter((key, tx) -> tx != null)
                .selectKey((key, tx) -> tx.cardNumber())
                .groupByKey(Grouped.with(Serdes.String(), transactionSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(WINDOW_MINUTES)))
                .aggregate(
                        VelocityState::empty,
                        (cardNumber, transaction, state) -> state.increment(transaction),
                        Materialized.<String, VelocityState, WindowStore<Bytes, byte[]>>as("velocity-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(velocityStateSerde)
                )
                .toStream()
                .peek((windowedKey, state) -> log.info(">>> VELOCITY CHECK: card={}, count={}, window={}",
                        windowedKey.key(), state.count(), windowedKey.window()))
                .filter((windowedKey, state) -> state.count() > MAX_TRANSACTIONS)
                .mapValues((windowedKey, state) ->
                        validator.validate(state.count(), state.lastTransaction()).orElse(null))
                .filter((windowedKey, alert) -> alert != null)
                .selectKey((windowedKey, alert) -> windowedKey.key())
                .peek((cardNumber, alert) -> log.info(">>> FRAUD DETECTED: {} - {}", alert.ruleId(), alert.description()))
                .to(topics.suspicious(), Produced.with(Serdes.String(), fraudAlertSerde));

        return stream;
    }
}