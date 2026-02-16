package com.devnexus.frauddetection.infrastructure.embedding.voyage;

import com.devnexus.frauddetection.domain.model.Transaction;
import com.devnexus.frauddetection.domain.port.TransactionEmbedderPort;
import com.devnexus.frauddetection.infrastructure.embedding.config.VoyageEmbeddingProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

@Service
public class VoyageTransactionEmbedderPort implements TransactionEmbedderPort {

	private final Logger logger = Logger.getLogger(VoyageTransactionEmbedderPort.class.getName());

	private final VoyageEmbeddingsClient client;
	private final VoyageEmbeddingProperties config;

	public VoyageTransactionEmbedderPort(VoyageEmbeddingsClient client, VoyageEmbeddingProperties config) {
		this.client = client;
		this.config = config;
	}

	@Override
	public float[] embed(Transaction transaction) {
		String input = TransactionEmbeddingText.toText(transaction);

		logger.info("Generating embeddings .. ");

		var res = client.embed(new EmbeddingsRequest(
				List.of(input), config.model(), "query", config.outputDimension()));

		logger.info("Embeddings generated successfully!");

		var embedding = res.data().getFirst().embedding();

		float[] result = new float[embedding.size()];
		for (int i = 0; i < embedding.size(); i++) {
			result[i] = embedding.get(i).floatValue();
		}

		return result;
	}
}
