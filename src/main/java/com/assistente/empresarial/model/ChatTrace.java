package com.assistente.empresarial.model;

import com.assistente.empresarial.enuns.LlmProvider;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "chat_traces")
public class ChatTrace {

    @Id
    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "assistente_id")
    private UUID assistenteId;

    @Column(name = "canal", length = 40)
    private String canal;

    @Column(name = "guardrail_time_ms")
    private long guardrailTimeMs;

    @Column(name = "cache_check_time_ms")
    private long cacheCheckTimeMs;

    @Column(name = "rag_retrieval_time_ms")
    private long ragRetrievalTimeMs;

    @Column(name = "llm_inference_time_ms")
    private long llmInferenceTimeMs;

    @Column(name = "tool_execution_time_ms")
    private long toolExecutionTimeMs;

    @Column(name = "total_latency_ms")
    private long totalLatencyMs;

    @Column(name = "input_tokens")
    private int inputTokens;

    @Column(name = "output_tokens")
    private int outputTokens;

    @Column(name = "total_tokens")
    private int totalTokens;

    @Column(name = "estimated_cost_usd")
    private double estimatedCostUsd;

    @Column(name = "cache_hit")
    private boolean cacheHit;

    @Column(name = "fallback_triggered")
    private boolean fallbackTriggered;

    @Column(name = "tools_used_raw", columnDefinition = "TEXT")
    private String toolsUsedRaw;

    @Column(name = "guardrail_violations_raw", columnDefinition = "TEXT")
    private String guardrailViolationsRaw;

    @Transient
    private List<String> toolsUsed = new ArrayList<>();

    @Transient
    private List<String> guardrailViolations = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_used", length = 30)
    private LlmProvider providerUsed;

    public ChatTrace() {
        this.traceId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    @PostLoad
    public void postLoad() {
        if (toolsUsedRaw != null && !toolsUsedRaw.isBlank()) {
            this.toolsUsed = Arrays.stream(toolsUsedRaw.split("\\|\\|"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        if (guardrailViolationsRaw != null && !guardrailViolationsRaw.isBlank()) {
            this.guardrailViolations = Arrays.stream(guardrailViolationsRaw.split("\\|\\|"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
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
        this.toolsUsed = toolsUsed != null ? toolsUsed : new ArrayList<>();
        this.toolsUsedRaw = this.toolsUsed.isEmpty() ? null : String.join("||", this.toolsUsed);
    }

    public List<String> getGuardrailViolations() {
        return guardrailViolations;
    }

    public void setGuardrailViolations(List<String> guardrailViolations) {
        this.guardrailViolations = guardrailViolations != null ? guardrailViolations : new ArrayList<>();
        this.guardrailViolationsRaw = this.guardrailViolations.isEmpty() ? null : String.join("||", this.guardrailViolations);
    }

    public LlmProvider getProviderUsed() {
        return providerUsed;
    }

    public void setProviderUsed(LlmProvider providerUsed) {
        this.providerUsed = providerUsed;
    }
}
