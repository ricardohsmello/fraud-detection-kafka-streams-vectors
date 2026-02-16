package com.devnexus.frauddetection.domain.port;

import com.devnexus.frauddetection.domain.model.SuspiciousTransactionEvent;

public interface SaveSuspiciousTransactionPort {
    void save(SuspiciousTransactionEvent alert);
}
