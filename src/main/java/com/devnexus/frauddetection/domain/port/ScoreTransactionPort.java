package com.devnexus.frauddetection.domain.port;

import com.devnexus.frauddetection.domain.model.Transaction;

public interface ScoreTransactionPort {
    void score(Transaction transaction);
}
