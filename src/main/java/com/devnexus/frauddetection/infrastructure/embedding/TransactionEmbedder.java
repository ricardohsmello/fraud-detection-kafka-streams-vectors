package com.devnexus.frauddetection.infrastructure.embedding;

import com.devnexus.frauddetection.domain.model.Transaction;
import org.springframework.data.domain.Vector;


public interface TransactionEmbedder {
    Vector embed(Transaction transaction);
}
