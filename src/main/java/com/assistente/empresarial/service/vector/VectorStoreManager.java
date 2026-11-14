package com.assistente.empresarial.service.vector;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Gerenciador Avançado de Banco Vetorial (PgVector & In-Memory).
 * - Execução de buscas vetoriais com filtros de isolamento multitenant estrito.
 * - Health Check e diagnóstico de conectividade do banco vetorial.
 * - Métricas de vetores armazenados por empresa e assistente.
 */
@Service
public class VectorStoreManager {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreManager.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DataSource dataSource;

    public VectorStoreManager(EmbeddingStore<TextSegment> embeddingStore, DataSource dataSource) {
        this.embeddingStore = embeddingStore;
        this.dataSource = dataSource;
    }

    /**
     * Executa busca vetorial filtrando por empresa e assistente com limiar mínimo de similaridade.
     */
    public List<EmbeddingMatch<TextSegment>> search(Embedding queryEmbedding, UUID empresaId, UUID assistenteId, int maxResults, double minScore) {
        try {
            var filtro = MetadataFilterBuilder.metadataKey("empresa_id").isEqualTo(empresaId.toString())
                    .and(MetadataFilterBuilder.metadataKey("assistente_id").isEqualTo(assistenteId.toString()));

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(minScore)
                    .filter(filtro)
                    .build();

            EmbeddingSearchResult<TextSegment> resultado = embeddingStore.search(searchRequest);
            return resultado != null && resultado.matches() != null ? resultado.matches() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Erro ao buscar vetores para empresa {} e assistente {}: {}", empresaId, assistenteId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Exclui vetores pertencentes a um documento específico.
     */
    public void deleteByDocumentoId(UUID documentoId) {
        if (documentoId == null) return;
        try {
            Filter filter = MetadataFilterBuilder.metadataKey("documento_id").isEqualTo(documentoId.toString());
            embeddingStore.removeAll(filter);
            log.info("Vetores do documento {} removidos com sucesso.", documentoId);
        } catch (Exception e) {
            log.warn("Não foi possível excluir vetores pelo filtro metadata: {}", e.getMessage());
        }
    }

    /**
     * Diagnóstico e Health Check do banco vetorial.
     */
    public Map<String, Object> getHealthAndStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("tipoArmazenamento", embeddingStore.getClass().getSimpleName());

        boolean healthy = false;
        long totalVetoresEstimados = 0;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Checa se a extensão pgvector está ativa no PostgreSQL
            try (ResultSet rsExt = stmt.executeQuery("SELECT extname, extversion FROM pg_extension WHERE extname = 'vector'")) {
                if (rsExt.next()) {
                    stats.put("pgvectorExtensaoInstalada", true);
                    stats.put("pgvectorVersao", rsExt.getString("extversion"));
                    healthy = true;
                } else {
                    stats.put("pgvectorExtensaoInstalada", false);
                }
            }

            // Conta vetores na tabela document_embeddings se ela existir
            try (ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM document_embeddings")) {
                if (rsCount.next()) {
                    totalVetoresEstimados = rsCount.getLong(1);
                    stats.put("tabelaEmbeddingsExiste", true);
                    stats.put("totalVetoresNoBanco", totalVetoresEstimados);
                }
            } catch (Exception ignored) {
                stats.put("tabelaEmbeddingsExiste", false);
            }

        } catch (Exception e) {
            stats.put("erroConexaoPostgres", e.getMessage());
        }

        stats.put("statusSaudavel", healthy || embeddingStore.getClass().getSimpleName().contains("InMemory"));
        return stats;
    }

    public EmbeddingStore<TextSegment> getStore() {
        return embeddingStore;
    }
}
