package com.devnexus.frauddetection.infrastructure.streams.topology;

import com.devnexus.frauddetection.domain.CardTravelState;
import com.devnexus.frauddetection.domain.FraudAlert;
import com.devnexus.frauddetection.domain.Transaction;
import com.devnexus.frauddetection.domain.rules.ImpossibleTravelValidator;
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
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FraudDetectionTopology {

	private static final Logger log = LoggerFactory.getLogger(FraudDetectionTopology.class);

	private final TopicsProperties topics;
	private final JsonSerde jsonSerde;
	private final ImpossibleTravelValidator validator;

	public FraudDetectionTopology(
			TopicsProperties topics,
			JsonSerde jsonSerde,
			ImpossibleTravelValidator validator
	) {
		this.topics = topics;
		this.jsonSerde = jsonSerde;
		this.validator = validator;
	}

	@Bean
	public KStream<String, Transaction> impossibleTravelStream(StreamsBuilder builder) {
		Serde<Transaction> transactionSerde = jsonSerde.forClass(Transaction.class);
		Serde<CardTravelState> cardTravelStateSerde = jsonSerde.forClass(CardTravelState.class);
		Serde<FraudAlert> fraudAlertSerde = jsonSerde.forClass(FraudAlert.class);

		KStream<String, Transaction> stream = builder.stream(
				topics.transactions(),
				Consumed.with(Serdes.String(), transactionSerde)
		);

		stream
				.filter((key, tx) -> tx != null)
				.selectKey((key, tx) -> tx.cardNumber())
				.groupByKey(Grouped.with(Serdes.String(), transactionSerde))
				.aggregate(
						CardTravelState::empty,
						(cardNumber, newTransaction, currentState) -> {
							Transaction previousTx = currentState.lastTransaction();

							FraudAlert alert = null;
							if (previousTx != null) {
								alert = validator.validate(previousTx, newTransaction).orElse(null);
							}

							return new CardTravelState(newTransaction, alert);
						},
						Materialized.<String, CardTravelState, KeyValueStore<Bytes, byte[]>>as("card-travel-store")
								.withKeySerde(Serdes.String())
								.withValueSerde(cardTravelStateSerde)
				)
				.toStream()
				.peek((cardNumber, state) -> log.info(">>> TRAVEL CHECK: card={}, fraud={}", cardNumber, state.hasFraudAlert()))
				.filter((cardNumber, state) -> state.hasFraudAlert())
				.mapValues(CardTravelState::fraudAlert)
				.peek((cardNumber, alert) -> log.info(">>> FRAUD DETECTED: {} - {}", alert.ruleId(), alert.description()))
				.to(topics.suspicious(), Produced.with(Serdes.String(), fraudAlertSerde));

		return stream;
	}

}
