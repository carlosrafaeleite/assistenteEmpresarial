package com.assistente.empresarial.service.observability;

import com.assistente.empresarial.enuns.LlmProvider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatTrace {

    private String traceId;
    private LocalDateTime timestamp;
    private UUID empresaId;
    private UUID assistenteId;
    private String canal;

    private long guardrailTimeMs;
    private long cacheCheckTimeMs;
    private long ragRetrievalTimeMs;
    private long llmInferenceTimeMs;
    private long toolExecutionTimeMs;
    private long totalLatencyMs;

    private int inputTokens;
    private int outputTokens;
    private int totalTokens;
    private double estimatedCostUsd;

    private boolean cacheHit;
    private boolean fallbackTriggered;
    private List<String> toolsUsed = new ArrayList<>();
    private List<String> guardrailViolations = new ArrayList<>();
    private LlmProvider providerUsed;

    public ChatTrace() {
        this.traceId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public long getGuardrailTimeMs() {
        return guardrailTimeMs;
    }

    public void setGuardrailTimeMs(long guardrailTimeMs) {
        this.guardrailTimeMs = guardrailTimeMs;
    }

    public long getCacheCheckTimeMs() {
        return cacheCheckTimeMs;
    }

    public void setCacheCheckTimeMs(long cacheCheckTimeMs) {
        this.cacheCheckTimeMs = cacheCheckTimeMs;
    }

    public long getRagRetrievalTimeMs() {
        return ragRetrievalTimeMs;
    }

    public void setRagRetrievalTimeMs(long ragRetrievalTimeMs) {
        this.ragRetrievalTimeMs = ragRetrievalTimeMs;
    }

    public long getLlmInferenceTimeMs() {
        return llmInferenceTimeMs;
    }

    public void setLlmInferenceTimeMs(long llmInferenceTimeMs) {
        this.llmInferenceTimeMs = llmInferenceTimeMs;
    }

    public long getToolExecutionTimeMs() {
        return toolExecutionTimeMs;
    }

    public void setToolExecutionTimeMs(long toolExecutionTimeMs) {
        this.toolExecutionTimeMs = toolExecutionTimeMs;
    }

    public long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(int inputTokens) {
        this.inputTokens = inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(int outputTokens) {
        this.outputTokens = outputTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public double getEstimatedCostUsd() {
        return estimatedCostUsd;
    }

    public void setEstimatedCostUsd(double estimatedCostUsd) {
        this.estimatedCostUsd = estimatedCostUsd;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public boolean isFallbackTriggered() {
        return fallbackTriggered;
    }

    public void setFallbackTriggered(boolean fallbackTriggered) {
        this.fallbackTriggered = fallbackTriggered;
    }

    public List<String> getToolsUsed() {
        return toolsUsed;
    }

    public void setToolsUsed(List<String> toolsUsed) {
        this.toolsUsed = toolsUsed;
    }

    public List<String> getGuardrailViolations() {
        return guardrailViolations;
    }

    public void setGuardrailViolations(List<String> guardrailViolations) {
        this.guardrailViolations = guardrailViolations;
    }

    public LlmProvider getProviderUsed() {
        return providerUsed;
    }

    public void setProviderUsed(LlmProvider providerUsed) {
        this.providerUsed = providerUsed;
    }
}
