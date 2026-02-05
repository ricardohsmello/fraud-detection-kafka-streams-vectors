package com.devnexus.frauddetection.infrastructure.streams.topology;

import com.devnexus.frauddetection.domain.CardTravelState;
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
		Serde<CardTravelState> cardTraveStoreSerde = jsonSerde.forClass(CardTravelState.class);

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

							boolean isImpossible = false;
							if (previousTx != null) {
								isImpossible = validator.isImpossibleTravel(previousTx, newTransaction);
							}

							return new CardTravelState(newTransaction, isImpossible);
						},
						Materialized.<String, CardTravelState, KeyValueStore<Bytes, byte[]>>as("card-travel-store")
								.withKeySerde(Serdes.String())
								.withValueSerde(cardTraveStoreSerde)
				)
				.toStream()
				.peek((cardNumber, state) -> log.info(">>> TRAVEL CHECK: card={}, impossible={}", cardNumber, state.impossibleTravelDetected()))
				.filter((cardNumber, state) -> state.impossibleTravelDetected())
				.mapValues(CardTravelState::lastTransaction)
				.peek((cardNumber, tx) -> log.info(">>> IMPOSSIBLE TRAVEL DETECTED: {}", tx))
				.to(topics.suspicious(), Produced.with(Serdes.String(), transactionSerde));

		return stream;
	}

}
