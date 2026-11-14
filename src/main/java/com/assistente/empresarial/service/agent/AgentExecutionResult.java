package com.assistente.empresarial.service.agent;

import java.util.ArrayList;
import java.util.List;

public class AgentExecutionResult {

    private final String finalResponse;
    private final List<String> executedTools;
    private final int iterations;
    private final long executionTimeMs;

    public AgentExecutionResult(String finalResponse, List<String> executedTools, int iterations, long executionTimeMs) {
        this.finalResponse = finalResponse;
        this.executedTools = executedTools != null ? executedTools : new ArrayList<>();
        this.iterations = iterations;
        this.executionTimeMs = executionTimeMs;
    }

    public String getFinalResponse() {
        return finalResponse;
    }

    public List<String> getExecutedTools() {
        return executedTools;
    }

    public int getIterations() {
        return iterations;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public boolean hasUsedTools() {
        return !executedTools.isEmpty();
    }
}
