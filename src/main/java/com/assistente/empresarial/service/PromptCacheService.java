package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.CachedPromptResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serviço de Cache Semântico e de Prompts.
 * - Cache de respostas frequentes em memória com TTL e hash SHA-256.
 * - Estruturação de prefixos estáveis para maximizar o Prompt Caching dos provedores (OpenAI/Gemini).
 * - Monitoramento de hits, misses e taxa de acerto.
 */
@Service
public class PromptCacheService {

    private static final Logger log = LoggerFactory.getLogger(PromptCacheService.class);

    private static final int MAX_CACHE_ENTRIES = 2000;
    private static final long DEFAULT_TTL_MINUTES = 60; // 1 hora de TTL

    private final Map<String, CachedPromptResponse> cache = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);

    /**
     * Tenta obter resposta em cache para a combinação empresa, assistente e mensagem.
     */
    public CachedPromptResponse get(UUID empresaId, UUID assistenteId, String promptSistema, String mensagem) {
        totalRequests.incrementAndGet();
        String key = buildCacheKey(empresaId, assistenteId, promptSistema, mensagem);

        CachedPromptResponse cached = cache.get(key);
        if (cached != null) {
            if (cached.isExpired(DEFAULT_TTL_MINUTES)) {
                cache.remove(key);
                log.debug("Entrada de cache expirada para a chave {}", key);
                return null;
            }
            cached.incrementHit();
            cacheHits.incrementAndGet();
            log.info("Prompt Cache HIT! Chave={}, Hits={}", key.substring(0, Math.min(16, key.length())), cached.getHitCount());
            return cached;
        }

        return null;
    }

    /**
     * Armazena uma resposta calculada no cache.
     */
    public void put(UUID empresaId, UUID assistenteId, String promptSistema, String mensagem, String resposta, List<String> fontes) {
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            // Evicção simples de 10% das entradas mais antigas
            cache.keySet().stream().limit(MAX_CACHE_ENTRIES / 10).forEach(cache::remove);
        }

        String key = buildCacheKey(empresaId, assistenteId, promptSistema, mensagem);
        cache.put(key, new CachedPromptResponse(resposta, fontes));
        log.debug("Resposta gravada no Prompt Cache com sucesso. Total em cache: {}", cache.size());
    }

    /**
     * Invalida todo o cache de um assistente ou empresa quando a base de documentos muda.
     */
    public void invalidate(UUID empresaId, UUID assistenteId) {
        String prefix = (empresaId != null ? empresaId.toString() : "") + ":" + (assistenteId != null ? assistenteId.toString() : "");
        cache.keySet().removeIf(k -> k.startsWith(prefix));
        log.info("Cache invalidado para empresa={} e assistente={}", empresaId, assistenteId);
    }

    public void clear() {
        cache.clear();
        totalRequests.set(0);
        cacheHits.set(0);
        log.info("Prompt cache resetado completamente.");
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public double getHitRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }

    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Cria chave determinística com SHA-256 do prompt do sistema e da mensagem normalizada.
     */
    private String buildCacheKey(UUID empresaId, UUID assistenteId, String promptSistema, String mensagem) {
        String cleanMsg = mensagem != null ? mensagem.trim().toLowerCase().replaceAll("\\s+", " ") : "";
        String promptHash = sha256(promptSistema != null ? promptSistema : "");
        return (empresaId != null ? empresaId.toString() : "global") + ":"
                + (assistenteId != null ? assistenteId.toString() : "default") + ":"
                + promptHash + ":"
                + sha256(cleanMsg);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
