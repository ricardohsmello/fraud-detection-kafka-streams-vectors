# Fraud Detection

Real-time fraud detection pipeline that scores card transactions using Kafka Streams, behavioral embeddings, and rule-based guardrails — without training a custom model.

## Tech Stack

- Java 21
- Spring Boot 4
- Kafka Streams
- MongoDB *(coming soon)*
- Voyage AI *(embedding generation)*

## How It Works

```
[Transaction] → [Kafka: transactions] → [Kafka Streams] → [Fraud Rules] → [Kafka: transactions-suspicious]
```

1. A transaction is sent via REST API
2. Kafka Streams processes and groups by card number
3. Rules detect anomalies (e.g., impossible travel)
4. Suspicious transactions are published to a separate topic

## Running

### 1. Start Kafka

```bash
docker run -d --name kafka -p 9092:9092 apache/kafka:latest
```

### 2. Create Topics

```bash
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic transactions --partitions 1

bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic transactions-suspicious --partitions 1
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

### 4. Send a Transaction

```bash
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN-001",
    "userId": "USR-123",
    "merchant": "Starbucks",
    "city": "Sao Paulo",
    "transactionAmount": 25.50,
    "transactionTime": "2025-01-15T10:30:00Z",
    "cardNumber": "4444-5555-6666-7777",
    "latitude": -23.5489,
    "longitude": -46.6388
  }'
```

### 5. Watch Suspicious Transactions

```bash
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic transactions-suspicious --from-beginning
```

## Testing Fraud Detection

To trigger an **impossible travel** alert, send two transactions with the same card from distant locations in quick succession (e.g., Sao Paulo → New York).

See `src/main/resources/http/fraud-detection.http` for example requests.