package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.CachedPromptResponse;
import com.assistente.empresarial.model.PromptCacheEntry;
import com.assistente.empresarial.repository.PromptCacheEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Serviço de Cache Semântico e de Prompts Híbrido (Memória L1 + Banco PostgreSQL L2).
 * - Cache de respostas frequentes em memória com TTL e hash SHA-256.
 * - Persistência em banco PostgreSQL (prompt_cache_entries) para sobreviver a reinicializações.
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
    private final PromptCacheEntryRepository cacheRepository;

    @Autowired
    public PromptCacheService(@Autowired(required = false) PromptCacheEntryRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    public PromptCacheService() {
        this.cacheRepository = null;
    }

    /**
     * Tenta obter resposta em cache (L1 Memória -> L2 Banco de Dados).
     */
    public CachedPromptResponse get(UUID empresaId, UUID assistenteId, String promptSistema, String mensagem) {
        totalRequests.incrementAndGet();
        String key = buildCacheKey(empresaId, assistenteId, promptSistema, mensagem);

        CachedPromptResponse cached = cache.get(key);
        if (cached != null) {
            if (cached.isExpired(DEFAULT_TTL_MINUTES)) {
                cache.remove(key);
                removerDoBanco(key);
                return null;
            }
            cached.incrementHit();
            cacheHits.incrementAndGet();
            log.debug("Prompt Cache HIT (L1) para empresa={} assistente={} (hits={})", empresaId, assistenteId, cached.getHitCount());
            return cached;
        }

        // Consulta L2 no banco PostgreSQL
        if (cacheRepository != null) {
            try {
                Optional<PromptCacheEntry> dbOpt = cacheRepository.findById(key);
                if (dbOpt.isPresent()) {
                    PromptCacheEntry entry = dbOpt.get();
                    List<String> fontes = (entry.getFontesRaw() != null && !entry.getFontesRaw().isBlank())
                            ? Arrays.stream(entry.getFontesRaw().split("\\|\\|")).collect(Collectors.toList())
                            : Collections.emptyList();
                    CachedPromptResponse fromDb = new CachedPromptResponse(entry.getResposta(), fontes);
                    if (entry.getCachedAt() != null && entry.getCachedAt().plusMinutes(DEFAULT_TTL_MINUTES).isBefore(java.time.LocalDateTime.now())) {
                        cacheRepository.deleteById(key);
                        return null;
                    }
                    fromDb.incrementHit();
                    cache.put(key, fromDb);
                    cacheHits.incrementAndGet();
                    entry.setHitCount(entry.getHitCount() + 1);
                    cacheRepository.save(entry);
                    log.debug("Prompt Cache HIT (L2 DB) para empresa={} assistente={}", empresaId, assistenteId);
                    return fromDb;
                }
            } catch (Exception e) {
                log.debug("Falha ao consultar PromptCache no banco: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * Armazena uma nova resposta no cache L1 e persiste no banco L2.
     */
    public void put(UUID empresaId, UUID assistenteId, String promptSistema, String mensagem, String resposta, List<String> fontes) {
        if (resposta == null || resposta.trim().isEmpty()) {
            return;
        }

        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(DEFAULT_TTL_MINUTES));
            if (cache.size() >= MAX_CACHE_ENTRIES && !cache.isEmpty()) {
                String firstKey = cache.keySet().iterator().next();
                cache.remove(firstKey);
            }
        }

        String key = buildCacheKey(empresaId, assistenteId, promptSistema, mensagem);
        List<String> fontesSafe = fontes != null ? fontes : Collections.emptyList();
        cache.put(key, new CachedPromptResponse(resposta, fontesSafe));

        if (cacheRepository != null) {
            try {
                String fontesRaw = fontesSafe.isEmpty() ? null : String.join("||", fontesSafe);
                PromptCacheEntry entry = new PromptCacheEntry(key, empresaId, assistenteId, resposta, fontesRaw);
                cacheRepository.save(entry);
            } catch (Exception e) {
                log.debug("Não foi possível persistir entrada de cache no banco: {}", e.getMessage());
            }
        }
    }

    /**
     * Invalida o cache quando documentos ou prompts do assistente são atualizados.
     */
    public void invalidate(UUID empresaId, UUID assistenteId) {
        String prefix = (empresaId != null ? empresaId.toString() : "") + ":" + (assistenteId != null ? assistenteId.toString() : "");
        cache.keySet().removeIf(k -> k.startsWith(prefix));
        if (cacheRepository != null && empresaId != null) {
            try {
                if (assistenteId != null) {
                    cacheRepository.deleteByEmpresaIdAndAssistenteId(empresaId, assistenteId);
                } else {
                    cacheRepository.deleteByEmpresaId(empresaId);
                }
            } catch (Exception e) {
                log.debug("Falha ao invalidar cache no banco: {}", e.getMessage());
            }
        }
        log.info("Cache invalidado para empresa={} e assistente={}", empresaId, assistenteId);
    }

    public void clear() {
        cache.clear();
        totalRequests.set(0);
        cacheHits.set(0);
        if (cacheRepository != null) {
            try {
                cacheRepository.deleteAll();
            } catch (Exception ignored) {
            }
        }
        log.info("Prompt cache resetado completamente.");
    }

    private void removerDoBanco(String key) {
        if (cacheRepository != null) {
            try {
                cacheRepository.deleteById(key);
            } catch (Exception ignored) {
            }
        }
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
