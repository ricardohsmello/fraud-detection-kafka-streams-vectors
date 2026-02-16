package com.devnexus.frauddetection.domain.port;

import com.devnexus.frauddetection.domain.model.SuspiciousAlert;

public interface SaveSuspiciousTransactionPort {
    void save(SuspiciousAlert alert);
}
