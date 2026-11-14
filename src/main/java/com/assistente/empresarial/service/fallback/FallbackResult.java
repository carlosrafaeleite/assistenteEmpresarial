package com.assistente.empresarial.service.fallback;

import com.assistente.empresarial.enuns.LlmProvider;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

public class FallbackResult {

    private final Response<AiMessage> response;
    private final LlmProvider providerUsed;
    private final boolean fallbackTriggered;
    private final String originalError;

    public FallbackResult(Response<AiMessage> response, LlmProvider providerUsed, boolean fallbackTriggered, String originalError) {
        this.response = response;
        this.providerUsed = providerUsed;
        this.fallbackTriggered = fallbackTriggered;
        this.originalError = originalError;
    }

    public Response<AiMessage> getResponse() {
        return response;
    }

    public LlmProvider getProviderUsed() {
        return providerUsed;
    }

    public boolean isFallbackTriggered() {
        return fallbackTriggered;
    }

    public String getOriginalError() {
        return originalError;
    }
}
