package com.assistente.empresarial.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prompt_cache_entries")
public class PromptCacheEntry {

    @Id
    @Column(name = "cache_key", length = 255)
    private String cacheKey;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "assistente_id")
    private UUID assistenteId;

    @Column(name = "resposta", columnDefinition = "TEXT", nullable = false)
    private String resposta;

    @Column(name = "fontes_raw", columnDefinition = "TEXT")
    private String fontesRaw;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt = LocalDateTime.now();

    @Column(name = "hit_count", nullable = false)
    private long hitCount = 0;

    public PromptCacheEntry() {
    }

    public PromptCacheEntry(String cacheKey, UUID empresaId, UUID assistenteId, String resposta, String fontesRaw) {
        this.cacheKey = cacheKey;
        this.empresaId = empresaId;
        this.assistenteId = assistenteId;
        this.resposta = resposta;
        this.fontesRaw = fontesRaw;
        this.cachedAt = LocalDateTime.now();
        this.hitCount = 0;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public UUID getAssistenteId() {
        return assistenteId;
    }

    public void setAssistenteId(UUID assistenteId) {
        this.assistenteId = assistenteId;
    }

    public String getResposta() {
        return resposta;
    }

    public void setResposta(String resposta) {
        this.resposta = resposta;
    }

    public String getFontesRaw() {
        return fontesRaw;
    }

    public void setFontesRaw(String fontesRaw) {
        this.fontesRaw = fontesRaw;
    }

    public LocalDateTime getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(LocalDateTime cachedAt) {
        this.cachedAt = cachedAt;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }
}
