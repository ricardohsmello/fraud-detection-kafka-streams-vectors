package com.devnexus.frauddetection.infrastructure.streams.config;

import com.devnexus.frauddetection.domain.rules.ImpossibleTravelValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

	@Bean
	public ImpossibleTravelValidator impossibleTravelValidator() {
		return new ImpossibleTravelValidator();
	}
}
