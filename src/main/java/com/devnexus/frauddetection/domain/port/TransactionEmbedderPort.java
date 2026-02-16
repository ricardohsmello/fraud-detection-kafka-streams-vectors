package com.devnexus.frauddetection.domain.port;

import com.devnexus.frauddetection.domain.model.Transaction;

public interface TransactionEmbedderPort {
    float[] embed(Transaction transaction);
}
