package com.devnexus.frauddetection.infrastructure.embedding;

import com.devnexus.frauddetection.domain.model.Transaction;
import org.springframework.data.domain.Vector;
import org.springframework.stereotype.Component;


@Component
public class DummyTransactionEmbedder implements TransactionEmbedder {

    @Override
    public Vector embed(Transaction tx) {
        throw new UnsupportedOperationException("Implement embedding provider");
    }
}
