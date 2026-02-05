package com.devnexus.frauddetection.infrastructure.http;

import com.devnexus.frauddetection.domain.Transaction;
import com.devnexus.frauddetection.infrastructure.message.producer.TransactionProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	private final TransactionProducer transactionProducer;

	TransactionController(TransactionProducer transactionProducer) {
		this.transactionProducer = transactionProducer;
	}

	@PostMapping
	public ResponseEntity<String> create(@RequestBody Transaction transaction) {
		transactionProducer.send(transaction);
		return ResponseEntity.ok("Transaction sent to Kafka!");
	}
}