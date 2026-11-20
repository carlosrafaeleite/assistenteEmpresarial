package com.assistente.empresarial.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de Embeddings 100% Local via Ollama (nomic-embed-text - 768 dimensões).
 * - Garante CUSTO ZERO de API tanto na ingestão de documentos/catálogos quanto na busca vetorial do chat.
 * - Mantém dimensão estritamente alinhada em 768 dimensões para o PostgreSQL pgvector,
 *   evitando conflito de dimensões (768 vs 1536) ao alternar provedores de LLM de chat (Gemini/OpenAI).
 * - Cache em memória para evitar reprocessamento de consultas idênticas.
 */
@Service
@Primary
public class DynamicEmbeddingService implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(DynamicEmbeddingService.class);

    @Value("${ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.embedding-model:nomic-embed-text}")
    private String ollamaEmbeddingModel;

    @Value("${ollama.embedding-dimension:768}")
    private int embeddingDimension = 768;

    private EmbeddingModel ollamaModel;

    private final Map<String, Embedding> embeddingCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 5000;

    @PostConstruct
    public void init() {
        try {
            this.ollamaModel = OllamaEmbeddingModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaEmbeddingModel)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            log.info("Motor de Embeddings Local inicializado com sucesso: Ollama ({}, {} dimensões) - Custo Zero de API",
                    ollamaEmbeddingModel, dimension());
        } catch (Exception e) {
            log.warn("Falha ao inicializar Ollama Embedding Model: {}", e.getMessage());
        }
    }

    @Override
    public Response<Embedding> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Response.from(criarVetorVazio());
        }

        String cacheKey = text.trim();
        Embedding cached = embeddingCache.get(cacheKey);
        if (cached != null) {
            return Response.from(cached);
        }

        Embedding embedding = gerarEmbeddingLocalOllama(text);
        if (embeddingCache.size() < MAX_CACHE_SIZE) {
            embeddingCache.put(cacheKey, embedding);
        }

        return Response.from(embedding);
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return embed(textSegment != null ? textSegment.text() : "");
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        if (textSegments == null || textSegments.isEmpty()) {
            return Response.from(Collections.emptyList());
        }
        List<Embedding> embeddings = new ArrayList<>(textSegments.size());
        for (TextSegment segment : textSegments) {
            embeddings.add(embed(segment.text()).content());
        }
        return Response.from(embeddings);
    }

    @Override
    public int dimension() {
        return embeddingDimension > 0 ? embeddingDimension : 768;
    }

    public String getEmbeddingModelName() {
        return ollamaEmbeddingModel != null ? ollamaEmbeddingModel : "nomic-embed-text";
    }

    private Embedding gerarEmbeddingLocalOllama(String text) {
        int targetDim = dimension();

        if (ollamaModel != null) {
            try {
                Embedding result = ollamaModel.embed(text).content();
                return alinharDimensao(result, targetDim);
            } catch (Exception e) {
                log.warn("Ollama local indisponível no momento para gerar embedding ({}). Acionando vetor determinístico de {} dimensões.",
                        e.getMessage(), targetDim);
            }
        }

        return gerarVetorDeterministico(text, targetDim);
    }

    /**
     * Garante que o vetor tenha exatamente a dimensão configurada no pgvector (768),
     * prevenindo erros de incompatibilidade dimensional no PostgreSQL.
     */
    public Embedding alinharDimensao(Embedding original, int targetDim) {
        if (original == null || original.vector() == null) {
            return criarVetorVazio();
        }
        float[] src = original.vector();
        if (src.length == targetDim) {
            return original;
        }

        float[] aligned = new float[targetDim];
        if (src.length > targetDim) {
            System.arraycopy(src, 0, aligned, 0, targetDim);
        } else {
            System.arraycopy(src, 0, aligned, 0, src.length);
        }
        normalizar(aligned);
        return Embedding.from(aligned);
    }

    private Embedding criarVetorVazio() {
        return Embedding.from(new float[dimension()]);
    }

    private Embedding gerarVetorDeterministico(String text, int dim) {
        float[] vector = new float[dim];
        int hash = text.hashCode();
        Random rnd = new Random(hash);
        for (int i = 0; i < dim; i++) {
            vector[i] = (float) rnd.nextGaussian();
        }
        normalizar(vector);
        return Embedding.from(vector);
    }

    private void normalizar(float[] vector) {
        float sum = 0f;
        for (float v : vector) {
            sum += v * v;
        }
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }
}
