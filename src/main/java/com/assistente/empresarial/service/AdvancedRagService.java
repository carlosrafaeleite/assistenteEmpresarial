package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.RagRetrievalResult;
import com.assistente.empresarial.repository.VectorStoreRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Pipeline de RAG Avançado (Retrieval-Augmented Generation).
 * - Pré-processamento e normalização de consultas.
 * - Busca vetorial com limiar dinâmico e fallback adaptativo.
 * - Desduplicação de trechos e ordenação por relevância.
 * - Formatação estruturada de citações com rastreamento de fontes.
 */
@Service
public class AdvancedRagService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedRagService.class);

    private static final double PRIMARY_MIN_SCORE = 0.55;
    private static final double RELAXED_MIN_SCORE = 0.45;
    private static final int DEFAULT_TOP_K = 5;

    private final EmbeddingModel embeddingModel;
    private final VectorStoreRepository vectorStoreRepository;

    public AdvancedRagService(EmbeddingModel embeddingModel, VectorStoreRepository vectorStoreRepository) {
        this.embeddingModel = embeddingModel;
        this.vectorStoreRepository = vectorStoreRepository;
    }

    /**
     * Recupera contexto relevante de documentos para enriquecer o prompt da IA.
     */
    public RagRetrievalResult retrieveRelevantContext(UUID empresaId, UUID assistenteId, String query) {
        if (empresaId == null || query == null || query.trim().isEmpty()) {
            return RagRetrievalResult.empty();
        }

        String cleanedQuery = preprocessQuery(query);

        try {
            log.debug("Gerando embedding para consulta RAG: '{}'", cleanedQuery);
            Response<Embedding> embeddingResponse = embeddingModel.embed(cleanedQuery);
            Embedding queryVector = embeddingResponse.content();

            // 1. Busca estrita com score primário
            List<EmbeddingMatch<TextSegment>> matches = vectorStoreRepository.search(
                    queryVector, empresaId, assistenteId, DEFAULT_TOP_K, PRIMARY_MIN_SCORE
            );

            // 2. Se nenhum resultado satisfatório for encontrado, tenta com limiar mais flexível
            if (matches.isEmpty()) {
                log.debug("Nenhum match com score >= {}. Tentando limiar adaptativo >= {}", PRIMARY_MIN_SCORE, RELAXED_MIN_SCORE);
                matches = vectorStoreRepository.search(
                        queryVector, empresaId, assistenteId, DEFAULT_TOP_K, RELAXED_MIN_SCORE
                );
            }

            if (matches.isEmpty()) {
                log.info("Nenhum trecho documental relevante encontrado no RAG para a pergunta.");
                return RagRetrievalResult.empty();
            }

            // 3. Desduplicação e montagem do contexto estruturado
            StringBuilder contextBuilder = new StringBuilder();
            Set<String> fontesUnicas = new LinkedHashSet<>();
            Set<String> trechosProcessados = new HashSet<>();
            double highestScore = 0.0;
            int matchIndex = 1;

            for (EmbeddingMatch<TextSegment> match : matches) {
                if (match.score() > highestScore) {
                    highestScore = match.score();
                }

                TextSegment segment = match.embedded();
                String rawText = segment.text().trim();

                // Evita redundância de trechos idênticos
                if (trechosProcessados.contains(rawText)) {
                    continue;
                }
                trechosProcessados.add(rawText);

                String nomeArquivo = segment.metadata() != null ? segment.metadata().getString("nome_arquivo") : "Documento Institucional";
                if (nomeArquivo != null && !nomeArquivo.trim().isEmpty()) {
                    fontesUnicas.add(nomeArquivo);
                }

                contextBuilder.append(String.format("[Trecho %d - Fonte: %s (Relevância: %.2f)]\n%s\n\n",
                        matchIndex++, nomeArquivo, match.score(), rawText));
            }

            return new RagRetrievalResult(
                    contextBuilder.toString().trim(),
                    new ArrayList<>(fontesUnicas),
                    trechosProcessados.size(),
                    highestScore
            );

        } catch (Exception e) {
            log.error("Erro durante recuperação RAG: {}", e.getMessage(), e);
            return RagRetrievalResult.empty();
        }
    }

    private String preprocessQuery(String query) {
        // Remove pontuações repetidas e espaços excessivos
        return query.replaceAll("[?!.,;]+", " ").replaceAll("\\s+", " ").trim();
    }
}
