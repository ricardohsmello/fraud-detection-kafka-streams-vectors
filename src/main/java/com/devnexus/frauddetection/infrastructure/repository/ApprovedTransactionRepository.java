package com.devnexus.frauddetection.infrastructure.repository;

import com.devnexus.frauddetection.domain.model.ApprovedTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovedTransactionRepository extends MongoRepository<ApprovedTransaction, String> {
}