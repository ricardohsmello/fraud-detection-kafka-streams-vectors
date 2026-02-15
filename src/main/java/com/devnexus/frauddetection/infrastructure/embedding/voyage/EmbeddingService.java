package com.devnexus.frauddetection.infrastructure.embedding.voyage;

import com.devnexus.frauddetection.domain.model.Transaction;
import com.devnexus.frauddetection.infrastructure.embedding.config.VoyageEmbeddingProperties;
import org.springframework.data.domain.Vector;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

@Service
public class EmbeddingService  {

	private final Logger logger = Logger.getLogger(EmbeddingService.class.getName());

	private final VoyageEmbeddingsClient client;
	private final VoyageEmbeddingProperties config;

	public EmbeddingService(VoyageEmbeddingsClient client, VoyageEmbeddingProperties config) {
		this.client = client;
		this.config = config;
	}

	public Vector embed(Transaction transaction) {
		String input = TransactionEmbeddingText.toText(transaction);

		logger.info("Generating embeddings .. ");

		var res = client.embed(new EmbeddingsRequest(
				List.of(input), config.model(), "query", config.outputDimension()));

		logger.info("Embeddings generated successfully!");

		var embedding = res.data().getFirst().embedding();

		float[] floats = new float[embedding.size()];
		for (int i = 0; i < embedding.size(); i++) {
			floats[i] = embedding.get(i).floatValue();
		}

		return Vector.of(floats);
	}
}