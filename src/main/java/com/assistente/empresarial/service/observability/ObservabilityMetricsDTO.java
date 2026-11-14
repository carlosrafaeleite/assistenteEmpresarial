package com.assistente.empresarial.service.observability;

import java.util.Map;

public class ObservabilityMetricsDTO {

    private long totalRequests;
    private double avgLatencyMs;
    private double p95LatencyMs;
    private double cacheHitRatePercent;
    private double fallbackRatePercent;
    private long totalTokensConsumed;
    private double totalEstimatedCostUsd;
    private Map<String, Long> providerDistribution;

    public ObservabilityMetricsDTO() {
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public double getCacheHitRatePercent() {
        return cacheHitRatePercent;
    }

    public void setCacheHitRatePercent(double cacheHitRatePercent) {
        this.cacheHitRatePercent = cacheHitRatePercent;
    }

    public double getFallbackRatePercent() {
        return fallbackRatePercent;
    }

    public void setFallbackRatePercent(double fallbackRatePercent) {
        this.fallbackRatePercent = fallbackRatePercent;
    }

    public long getTotalTokensConsumed() {
        return totalTokensConsumed;
    }

    public void setTotalTokensConsumed(long totalTokensConsumed) {
        this.totalTokensConsumed = totalTokensConsumed;
    }

    public double getTotalEstimatedCostUsd() {
        return totalEstimatedCostUsd;
    }

    public void setTotalEstimatedCostUsd(double totalEstimatedCostUsd) {
        this.totalEstimatedCostUsd = totalEstimatedCostUsd;
    }

    public Map<String, Long> getProviderDistribution() {
        return providerDistribution;
    }

    public void setProviderDistribution(Map<String, Long> providerDistribution) {
        this.providerDistribution = providerDistribution;
    }
}
