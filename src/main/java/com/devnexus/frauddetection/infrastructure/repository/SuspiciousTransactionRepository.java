package com.devnexus.frauddetection.infrastructure.repository;

import com.devnexus.frauddetection.domain.model.SuspiciousTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuspiciousTransactionRepository extends MongoRepository<SuspiciousTransaction, String> {
}
