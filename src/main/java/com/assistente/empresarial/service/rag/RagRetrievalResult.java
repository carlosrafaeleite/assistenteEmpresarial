package com.assistente.empresarial.service.rag;

import java.util.ArrayList;
import java.util.List;

public class RagRetrievalResult {

    private final String contextText;
    private final List<String> sources;
    private final int totalMatches;
    private final double highestScore;

    public RagRetrievalResult(String contextText, List<String> sources, int totalMatches, double highestScore) {
        this.contextText = contextText;
        this.sources = sources != null ? sources : new ArrayList<>();
        this.totalMatches = totalMatches;
        this.highestScore = highestScore;
    }

    public static RagRetrievalResult empty() {
        return new RagRetrievalResult("", new ArrayList<>(), 0, 0.0);
    }

    public String getContextText() {
        return contextText;
    }

    public List<String> getSources() {
        return sources;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public double getHighestScore() {
        return highestScore;
    }

    public boolean hasContext() {
        return contextText != null && !contextText.trim().isEmpty();
    }
}
