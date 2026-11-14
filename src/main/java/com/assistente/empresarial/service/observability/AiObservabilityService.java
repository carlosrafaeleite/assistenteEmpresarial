package com.assistente.empresarial.service.observability;

import com.assistente.empresarial.enuns.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Motor Central de Observabilidade de IA (AI Observability & APM).
 * - Rastreamento distribuído de requisições com Trace ID único.
 * - Decomposição detalhada de latência: Guardrails, Cache, RAG, LLM e Execução de Ferramentas.
 * - Contabilidade de tokens consumidos e estimativa financeira de custo (USD).
 * - Métricas agregadas (Percentil P95, Taxa de Cache Hit, Distribuição por Provedor).
 */
@Service
public class AiObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(AiObservabilityService.class);

    private static final int MAX_TRACES_HISTORY = 500;

    // Custos aproximados por 1 milhão de tokens (ex: OpenAI GPT-4o-mini: $0.15 input / $0.60 output)
    private static final double INPUT_COST_PER_MILLION = 0.15;
    private static final double OUTPUT_COST_PER_MILLION = 0.60;

    private final Deque<ChatTrace> traceBuffer = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalTokens = new AtomicLong(0);
    private final AtomicLong totalFallbacks = new AtomicLong(0);
    private final AtomicLong totalCacheHits = new AtomicLong(0);

    public void recordTrace(ChatTrace trace) {
        if (trace == null) return;

        // Calcula custo estimado
        double cost = (trace.getInputTokens() * INPUT_COST_PER_MILLION / 1_000_000.0)
                + (trace.getOutputTokens() * OUTPUT_COST_PER_MILLION / 1_000_000.0);
        trace.setEstimatedCostUsd(cost);

        totalRequests.incrementAndGet();
        totalTokens.addAndGet(trace.getTotalTokens());
        if (trace.isCacheHit()) totalCacheHits.incrementAndGet();
        if (trace.isFallbackTriggered()) totalFallbacks.incrementAndGet();

        traceBuffer.addFirst(trace);
        while (traceBuffer.size() > MAX_TRACES_HISTORY) {
            traceBuffer.pollLast();
        }

        log.info("TRACE [{}] Concluído em {}ms | Tokens: {} | Provedor: {} | CacheHit: {} | Fallback: {}",
                trace.getTraceId().substring(0, 8),
                trace.getTotalLatencyMs(),
                trace.getTotalTokens(),
                trace.getProviderUsed(),
                trace.isCacheHit(),
                trace.isFallbackTriggered());
    }

    public List<ChatTrace> getRecentTraces(int limit) {
        int count = Math.min(limit > 0 ? limit : 50, traceBuffer.size());
        List<ChatTrace> list = new ArrayList<>(count);
        int i = 0;
        for (ChatTrace t : traceBuffer) {
            if (i++ >= count) break;
            list.add(t);
        }
        return list;
    }

    public ObservabilityMetricsDTO getAggregatedMetrics() {
        ObservabilityMetricsDTO dto = new ObservabilityMetricsDTO();
        long reqCount = totalRequests.get();
        dto.setTotalRequests(reqCount);
        dto.setTotalTokensConsumed(totalTokens.get());

        if (reqCount == 0) {
            dto.setAvgLatencyMs(0.0);
            dto.setP95LatencyMs(0.0);
            dto.setCacheHitRatePercent(0.0);
            dto.setFallbackRatePercent(0.0);
            dto.setTotalEstimatedCostUsd(0.0);
            dto.setProviderDistribution(Collections.emptyMap());
            return dto;
        }

        dto.setCacheHitRatePercent(((double) totalCacheHits.get() / reqCount) * 100.0);
        dto.setFallbackRatePercent(((double) totalFallbacks.get() / reqCount) * 100.0);

        List<Long> latencies = new ArrayList<>();
        double totalCost = 0.0;
        Map<String, Long> providers = new HashMap<>();

        for (ChatTrace t : traceBuffer) {
            latencies.add(t.getTotalLatencyMs());
            totalCost += t.getEstimatedCostUsd();

            String prov = t.getProviderUsed() != null ? t.getProviderUsed().name() : "DESCONHECIDO";
            providers.put(prov, providers.getOrDefault(prov, 0L) + 1);
        }

        if (!latencies.isEmpty()) {
            Collections.sort(latencies);
            double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
            dto.setAvgLatencyMs(avg);

            int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;
            dto.setP95LatencyMs(latencies.get(Math.max(0, p95Index)));
        }

        dto.setTotalEstimatedCostUsd(totalCost);
        dto.setProviderDistribution(providers);
        return dto;
    }
}
