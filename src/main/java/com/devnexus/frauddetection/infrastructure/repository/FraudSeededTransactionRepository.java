package com.devnexus.frauddetection.infrastructure.repository;

import com.devnexus.frauddetection.domain.model.FraudPattern;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FraudSeededTransactionRepository extends MongoRepository<FraudPattern, String> {
}
