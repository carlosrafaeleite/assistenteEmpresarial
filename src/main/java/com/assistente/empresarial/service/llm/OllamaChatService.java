package com.assistente.empresarial.service.llm;

import com.assistente.empresarial.enuns.LlmProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OllamaChatService implements LlmChatService {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatService.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.chat-model:llama3.2}")
    private String chatModelName;

    @Value("${ollama.temperature:0.3}")
    private Double temperature;

    @Value("${ollama.timeout-seconds:120}")
    private Integer timeoutSeconds;

    private volatile ChatLanguageModel chatLanguageModel;

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.OLLAMA;
    }

    @Override
    public String getModelName() {
        return chatModelName;
    }

    @Override
    public boolean isConfigured() {
        return ollamaBaseUrl != null && !ollamaBaseUrl.trim().isEmpty();
    }

    @Override
    public ChatLanguageModel getModel() {
        if (chatLanguageModel == null) {
            synchronized (this) {
                if (chatLanguageModel == null) {
                    log.info("Inicializando modelo Ollama local: {} em {} (timeout: {}s)",
                            chatModelName, ollamaBaseUrl, timeoutSeconds);
                    chatLanguageModel = OllamaChatModel.builder()
                            .baseUrl(ollamaBaseUrl)
                            .modelName(chatModelName)
                            .temperature(temperature != null ? temperature : 0.3)
                            .timeout(Duration.ofSeconds(timeoutSeconds != null ? timeoutSeconds : 120))
                            .build();
                }
            }
        }
        return chatLanguageModel;
    }
}
