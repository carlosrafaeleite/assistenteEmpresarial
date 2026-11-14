package com.assistente.empresarial.service.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço Dinâmico e Resiliente de Embeddings.
 * - Suporta múltiplos provedores: OLLAMA, OPENAI e Fallback Local Determinístico.
 * - Cache LRU em memória para evitar chamadas de rede repetidas em consultas idênticas.
 */
@Service
@org.springframework.context.annotation.Primary
public class DynamicEmbeddingService implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(DynamicEmbeddingService.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.embedding-model:nomic-embed-text}")
    private String ollamaEmbeddingModel;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.embedding-model:text-embedding-3-small}")
    private String openAiEmbeddingModel;

    @Value("${ollama.embedding-dimension:768}")
    private int embeddingDimension;

    private EmbeddingModel primaryModel;
    private EmbeddingModel secondaryModel;

    // Cache simples de embeddings por hash de texto
    private final Map<String, Embedding> embeddingCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 5000;

    @PostConstruct
    public void init() {
        // Inicializa provedor primário Ollama
        try {
            this.primaryModel = OllamaEmbeddingModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaEmbeddingModel)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            log.info("Provedor primário de embeddings inicializado: Ollama ({})", ollamaEmbeddingModel);
        } catch (Exception e) {
            log.warn("Falha ao inicializar Ollama Embedding Model: {}", e.getMessage());
        }

        // Se chave OpenAI disponível, configura como secundário/alternativo
        if (openAiApiKey != null && !openAiApiKey.trim().isEmpty() && !openAiApiKey.equalsIgnoreCase("sua-chave-aqui")) {
            try {
                this.secondaryModel = OpenAiEmbeddingModel.builder()
                        .apiKey(openAiApiKey.trim())
                        .modelName(openAiEmbeddingModel)
                        .timeout(Duration.ofSeconds(60))
                        .build();
                log.info("Provedor secundário de embeddings configurado: OpenAI ({})", openAiEmbeddingModel);
            } catch (Exception e) {
                log.warn("Falha ao inicializar OpenAI Embedding Model: {}", e.getMessage());
            }
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

        Embedding embedding = gerarComFallback(text);
        if (embeddingCache.size() < MAX_CACHE_SIZE) {
            embeddingCache.put(cacheKey, embedding);
        }

        return Response.from(embedding);
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return embed(textSegment.text());
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
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

    private Embedding gerarComFallback(String text) {
        // 1. Tentar modelo primário (Ollama)
        if (primaryModel != null) {
            try {
                return primaryModel.embed(text).content();
            } catch (Exception e) {
                log.warn("Falha no modelo primário de embeddings: {}. Tentando secundário...", e.getMessage());
            }
        }

        // 2. Tentar modelo secundário (OpenAI)
        if (secondaryModel != null) {
            try {
                return secondaryModel.embed(text).content();
            } catch (Exception e) {
                log.warn("Falha no modelo secundário de embeddings: {}", e.getMessage());
            }
        }

        // 3. Fallback de contingência determinístico (evita quebrar a aplicação caso serviços externos estejam off)
        log.warn("Utilizando gerador de contingência determinístico de embeddings.");
        return gerarVetorDeterministico(text, dimension());
    }

    private Embedding criarVetorVazio() {
        float[] vector = new float[dimension()];
        return Embedding.from(vector);
    }

    private Embedding gerarVetorDeterministico(String text, int dim) {
        float[] vector = new float[dim];
        int hash = text.hashCode();
        Random rnd = new Random(hash);
        for (int i = 0; i < dim; i++) {
            vector[i] = (float) rnd.nextGaussian();
        }
        // Normalizar vetor
        float sum = 0f;
        for (float v : vector) sum += v * v;
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) {
                vector[i] /= norm;
            }
        }
        return Embedding.from(vector);
    }
}
