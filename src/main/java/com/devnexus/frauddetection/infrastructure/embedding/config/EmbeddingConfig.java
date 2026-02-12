package com.devnexus.frauddetection.infrastructure.embedding.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VoyageEmbeddingProperties.class)
class EmbeddingConfig {}
