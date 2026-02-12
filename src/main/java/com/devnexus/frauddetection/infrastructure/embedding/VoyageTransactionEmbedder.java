package com.devnexus.frauddetection.infrastructure.embedding;

import com.devnexus.frauddetection.domain.model.Transaction;
import com.devnexus.frauddetection.infrastructure.embedding.config.VoyageEmbeddingProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Vector;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Primary
public class VoyageTransactionEmbedder implements TransactionEmbedder {

    private final VoyageEmbeddingProperties props;
    private final RestClient client;

    public VoyageTransactionEmbedder(VoyageEmbeddingProperties props) {
        this.props = props;
        this.client = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.apiKey())
                .build();
    }

    @Override
    public Vector embed(Transaction tx) {

        String input = TransactionEmbeddingText.toText(tx);

        VoyageEmbeddingResponse response =
                client.post()
                        .uri("/v1/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new VoyageEmbeddingRequest(
                                props.model(),
                                List.of(input)
                        ))
                        .retrieve()
                        .body(VoyageEmbeddingResponse.class);

        if (response == null
                || response.data() == null
                || response.data().isEmpty()) {
            throw new IllegalStateException("VoyageAI returned no embedding");
        }

        List<Double> embedding = response.data().getFirst().embedding();

        float[] floats = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            floats[i] = embedding.get(i).floatValue();
        }

        return Vector.of(floats);
    }

    public record VoyageEmbeddingRequest(
            String model,
            List<String> input
    ) {}

    public record VoyageEmbeddingResponse(
            List<EmbeddingItem> data
    ) {}

    public record EmbeddingItem(
            List<Double> embedding
    ) {}
}
