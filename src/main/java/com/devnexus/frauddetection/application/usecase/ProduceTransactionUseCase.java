package com.devnexus.frauddetection.application.usecase;

import com.devnexus.frauddetection.application.request.TransactionRequest;
import com.devnexus.frauddetection.domain.port.TransactionProducerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProduceTransactionUseCase {

	private final Logger logger = LoggerFactory.getLogger(ProduceTransactionUseCase.class);

	private final TransactionProducerPort transactionProducerPort;

	public ProduceTransactionUseCase(TransactionProducerPort transactionProducerPort) {
		this.transactionProducerPort = transactionProducerPort;
	}

	public void execute(TransactionRequest transactionRequest) {
		logger.info("Sending transaction message: {}", transactionRequest);
		transactionProducerPort.send(transactionRequest.toTransaction());
	}
}
