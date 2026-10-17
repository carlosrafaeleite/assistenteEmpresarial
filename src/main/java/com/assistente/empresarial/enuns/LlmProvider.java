package com.assistente.empresarial.enuns;

public enum LlmProvider {
    OPENAI,
    GEMINI,
    OLLAMA;

    public static LlmProvider fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OPENAI;
        }
        for (LlmProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(value.trim())) {
                return provider;
            }
        }
        return OPENAI;
    }
}
