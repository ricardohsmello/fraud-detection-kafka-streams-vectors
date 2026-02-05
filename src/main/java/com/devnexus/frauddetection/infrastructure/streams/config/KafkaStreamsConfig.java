package com.devnexus.frauddetection.infrastructure.streams.config;

import com.devnexus.frauddetection.domain.rules.ImpossibleTravelValidator;
import com.devnexus.frauddetection.domain.rules.VelocityCheckValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

	private static final long MAX_TRANSACTIONS_PER_WINDOW = 3;
	private static final long WINDOW_MINUTES = 1;

	@Bean
	public ImpossibleTravelValidator impossibleTravelValidator() {
		return new ImpossibleTravelValidator();
	}

	@Bean
	public VelocityCheckValidator velocityCheckValidator() {
		return new VelocityCheckValidator(MAX_TRANSACTIONS_PER_WINDOW, WINDOW_MINUTES);
	}
}
