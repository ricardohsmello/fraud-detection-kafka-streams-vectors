package com.devnexus.frauddetection.application.usecase;

import com.devnexus.frauddetection.domain.model.SuspiciousTransactionEvent;
import com.devnexus.frauddetection.domain.port.TransactionPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveSuspiciousTransactionUseCase {

	private static final Logger logger = LoggerFactory.getLogger(SaveSuspiciousTransactionUseCase.class);

	private final TransactionPersistencePort persistence;

	public SaveSuspiciousTransactionUseCase(TransactionPersistencePort persistence) {
		this.persistence = persistence;
	}

	public void execute(SuspiciousTransactionEvent event) {
//		saveSuspiciousTransactionPort.save(alert);

		if (event == null || event.transaction() == null) {
			logger.warn(">>> Suspicious event: <null>");
			return;
		}

		var tx = event.transaction();

		logger.warn(">>> SUSPICIOUS (RULE): txId={}, userId={}, ruleId={}, desc={}",
				tx.transactionId(),
				tx.userId(),
				event.ruleId(),
				event.description()
		);

		persistence.saveSuspiciousFromRule(event);
	}
}
