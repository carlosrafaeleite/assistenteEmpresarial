package com.assistente.empresarial.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;

@Configuration
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.chat-model:llama3.2}")
    private String chatModelName;

    @Value("${ollama.embedding-model:nomic-embed-text}")
    private String embeddingModelName;

    @Value("${ollama.embedding-dimension:768}")
    private int embeddingDimension;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource) {
        try {
            log.info("Tentando inicializar PgVectorEmbeddingStore no PostgreSQL (tabela: document_embeddings, dimensão: {})", embeddingDimension);
            return PgVectorEmbeddingStore.datasourceBuilder()
                    .datasource(dataSource)
                    .table("document_embeddings")
                    .dimension(embeddingDimension)
                    .createTable(true)
                    .useIndex(true)
                    .metadataStorageConfig(DefaultMetadataStorageConfig.defaultConfig())
                    .build();
        } catch (Exception e) {
            log.warn("Extensão pgvector não detectada ou erro ao iniciar PgVectorEmbeddingStore: {}. Ativando InMemoryEmbeddingStore como alternativa resiliente.", e.getMessage());
            return new InMemoryEmbeddingStore<>();
        }
    }
}
