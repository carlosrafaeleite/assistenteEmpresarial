package com.assistente.empresarial.service.cache;

import java.time.LocalDateTime;
import java.util.List;

public class CachedPromptResponse {

    private final String resposta;
    private final List<String> fontes;
    private final LocalDateTime cachedAt;
    private long hitCount;

    public CachedPromptResponse(String resposta, List<String> fontes) {
        this.resposta = resposta;
        this.fontes = fontes;
        this.cachedAt = LocalDateTime.now();
        this.hitCount = 0;
    }

    public String getResposta() {
        return resposta;
    }

    public List<String> getFontes() {
        return fontes;
    }

    public LocalDateTime getCachedAt() {
        return cachedAt;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void incrementHit() {
        this.hitCount++;
    }

    public boolean isExpired(long ttlMinutes) {
        return LocalDateTime.now().isAfter(cachedAt.plusMinutes(ttlMinutes));
    }
}
