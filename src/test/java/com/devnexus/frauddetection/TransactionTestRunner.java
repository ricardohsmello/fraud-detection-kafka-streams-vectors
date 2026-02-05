package com.devnexus.frauddetection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

public class TransactionTestRunner {

    private static final String URL = "http://localhost:8081/api/transactions";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("Sending test transactions...\n");

        // 5 transações suspeitas (> 10.000)
        for (int i = 1; i <= 5; i++) {
            double amount = 10000 + random.nextInt(90000); // 10.001 a 100.000
            sendTransaction("TXN-" + i, amount);
            Thread.sleep(500);
        }

        // 5 transações normais (< 10.000)
        for (int i = 1; i <= 5; i++) {
            double amount = random.nextInt(9999) + 1; // 1 a 9.999
            sendTransaction("TXN-" + i, amount);
            Thread.sleep(500);
        }

        System.out.println("\nDone! ");
    }

    private static void sendTransaction(String id, double amount) throws Exception {
        String json = """
            {
                "transactionId": "%s",
                "transactionAmount": %.2f,
                "transactionTime": "%s",
                "cardNumber": "4532-%04d-%04d-%04d"
            }
            """.formatted(
                id,
                amount,
                LocalDateTime.now().toString(),
                random.nextInt(10000),
                random.nextInt(10000),
                random.nextInt(10000)
            );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String type = amount > 10000 ? "SUSPICIOUS" : "NORMAL";
        System.out.printf("%s | ID: %-15s | Amount: %10.2f | Status: %d%n",
            type, id, amount, response.statusCode());
    }
}
